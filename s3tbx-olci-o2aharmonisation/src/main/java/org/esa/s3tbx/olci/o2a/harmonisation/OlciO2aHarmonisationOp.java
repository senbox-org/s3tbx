package org.esa.s3tbx.olci.o2a.harmonisation;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.math.MathUtils;
import org.json.simple.parser.ParseException;
import smile.neighbor.KDTree;

import java.awt.Rectangle;
import java.io.IOException;
import java.nio.file.Path;
import java.util.regex.Pattern;

/**
 * Performs O2A band harmonisation on OLCI L1b product.
 * Implements last update v4 provided by R.Preusker, June 2020.
 * Authors: R.Preusker (algorithm, Python breadboard), O.Danne, M.Peters (Java conversion), 2018-2019
 * <p/>
 *
 * @author Rene Preusker
 * @author Olaf Danne
 * @author Marco Peters
 */
@OperatorMetadata(alias = "OlciO2aHarmonisation", version = "1.2",
        authors = "R.Preusker, O.Danne, M.Peters",
        category = "Optical/Preprocessing",
        copyright = "Copyright (C) 2018-2020 by Brockmann Consult",
        description = "Performs O2A band harmonisation on OLCI L1b product. Implements update v4 of R.Preusker, June 2020.")
public class OlciO2aHarmonisationOp extends Operator {

    @SourceProduct(description = "OLCI L1b or fully compatible product. " +
            "May contain an optional altitude band, i.e. introduced from an external DEM.",
            label = "OLCI L1b product")
    private Product l1bProduct;

    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "Name of alternative altitude band in source product " +
            "(i.e. introduced from an external DEM). Altitude is expected in meters.",
            label = "Name of alternative altitude band")
    private String alternativeAltitudeBandName;

    @Parameter(defaultValue = "true",
            description = "If set to true, only band 13 needed for cloud detection will be processed, otherwise bands 13-15.",
            label = "Only process OLCI band 13 (761.25 nm)")
    private boolean processOnlyBand13;

    @Parameter(defaultValue = "true",
            label = "Write harmonised radiances",
            description = "If set to true, harmonised radiances of processed band(s) will be written to target product.")
    private boolean writeHarmonisedRadiances;

    private int lastBandToProcess;
    private int numBandsToProcess;

    private TiePointGrid szaBand;
    private TiePointGrid ozaBand;

    private RasterDataNode demAltitudeBand;
    private RasterDataNode altitudeBand;
    private RasterDataNode slpBand;
    private Band detectorIndexBand;

    private Band[] radianceBands;
    private Band[] solarFluxBands;

    private KDTree<double[]>[] desmileKdTrees;
    private DesmileLut[] desmileLuts;
    private OlciO2aHarmonisationIO.SpectralCharacteristics specChar;
    private double[][] dwlCorrOffsets;

    @Override
    public void initialize() throws OperatorException {
        lastBandToProcess = processOnlyBand13 ? 13 : 15;
        numBandsToProcess = lastBandToProcess - 13 + 1;
        OlciO2aHarmonisationIO.validateSourceProduct(l1bProduct);
        if (StringUtils.isNotNullAndNotEmpty(alternativeAltitudeBandName)) {
            if (l1bProduct.containsBand(alternativeAltitudeBandName)) {
                demAltitudeBand = l1bProduct.getBand(alternativeAltitudeBandName);
            } else {
                String message = String.format("Specified alternative altitude band '%s' is not contained in OLCI L1B product.",
                        alternativeAltitudeBandName);
                throw new OperatorException(message);
            }
        }

        int orbitNumber = OlciO2aHarmonisationIO.getOrbitNumber(l1bProduct);
        final String platform = OlciO2aHarmonisationIO.getPlatform(l1bProduct);
        Product modelProduct;
        try {
            modelProduct = OlciO2aHarmonisationIO.getModelProduct(platform);
            specChar = OlciO2aHarmonisationIO.getSpectralCharacteristics(orbitNumber, modelProduct);
            dwlCorrOffsets = OlciO2aHarmonisationIO.getDwlCorrOffsets(platform);
        } catch (IOException e) {
            e.printStackTrace();
        }

        createTargetProduct();
    }

    @Override
    public void doExecute(ProgressMonitor pm) throws OperatorException {
        pm.beginTask("Initializing Desmile Auxiliary Data", (numBandsToProcess * 2) + 2);
        try {
            initDesmileAuxdata(pm);
            szaBand = l1bProduct.getTiePointGrid("SZA");
            ozaBand = l1bProduct.getTiePointGrid("OZA");
            slpBand = l1bProduct.getTiePointGrid("sea_level_pressure");
            detectorIndexBand = l1bProduct.getBand("detector_index");
            altitudeBand = l1bProduct.getBand("altitude");
            radianceBands = new Band[5];
            solarFluxBands = new Band[5];
            for (int i = 12; i < 17; i++) {
                radianceBands[i - 12] = l1bProduct.getBand("Oa" + i + "_radiance");
                solarFluxBands[i - 12] = l1bProduct.getBand("solar_flux_band_" + i);
            }
            pm.worked(1);
        } catch (IOException | ParseException e) {
            throw new OperatorException("Cannot initialize auxdata for desmile of transmissions - exiting.", e);
        } finally {
            pm.done();
        }
    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        final Rectangle targetRectangle = targetTile.getRectangle();
        final String targetBandName = targetBand.getName();

        final Tile szaTile = getSourceTile(szaBand, targetRectangle);
        final Tile ozaTile = getSourceTile(ozaBand, targetRectangle);
        final Tile altitudeTile = getSourceTile(altitudeBand, targetRectangle);
        Tile demAltitudeTile = null;
        if (demAltitudeBand != null) {
            demAltitudeTile = getSourceTile(demAltitudeBand, targetRectangle);
        }
        final Tile slpTile = getSourceTile(slpBand, targetRectangle);
        final Tile detectorIndexTile = getSourceTile(detectorIndexBand, targetRectangle);
        final Tile l1FlagsTile = getSourceTile(l1bProduct.getRasterDataNode("quality_flags"), targetRectangle);

        Tile[] radianceTiles = new Tile[5];
        Tile[] solarFluxTiles = new Tile[5];
        for (int i = 0; i < 5; i++) {
            radianceTiles[i] = getSourceTile(radianceBands[i], targetRectangle);
            solarFluxTiles[i] = getSourceTile(solarFluxBands[i], targetRectangle);
        }

        for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
            checkForCancellation();
            for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {
                final boolean pixelIsValid = !l1FlagsTile.getSampleBit(x, y, OlciO2aHarmonisationConstants.OLCI_INVALID_BIT);
                if (pixelIsValid) {
                    // Preparing input data...
                    final double sza = szaTile.getSampleDouble(x, y);
                    final double oza = ozaTile.getSampleDouble(x, y);
                    double altitude;
                    // if all info from DEM is present, use DEM altitude:
                    if (demAltitudeTile != null) {
                        altitude = demAltitudeTile.getSampleDouble(x, y);
                    } else {
                        altitude = altitudeTile.getSampleDouble(x, y);
                    }

                    final double slp = slpTile.getSampleDouble(x, y);
                    double surfacePress = OlciO2aHarmonisationAlgorithm.height2press(altitude, slp);
                    final float detectorIndex = detectorIndexTile.getSampleFloat(x, y);

                    final double amf = (1.0 / Math.cos(sza * MathUtils.DTOR) + 1.0 / Math.cos(oza * MathUtils.DTOR));

                    double[] radiance = new double[5];
                    double[] r = new double[5];
                    double[] cwl = new double[5];
                    double[] fwhm = new double[5];
                    double[] solarFlux = new double[5];

                    for (int i = 0; i < 5; i++) {    // 12, 13, 14, 15, 16
                        radiance[i] = radianceTiles[i].getSampleDouble(x, y);
                        cwl[i] = specChar.getCwvl()[i][(int) detectorIndex];
                        fwhm[i] = specChar.getFwhm()[i][(int) detectorIndex];
                        solarFlux[i] = solarFluxTiles[i].getSampleDouble(x, y);
                        r[i] = radiance[i] / solarFlux[i];
                    }

                    final double dlam = cwl[4] - cwl[0];
                    final double drad = r[4] - r[0];
                    final double grad = drad / dlam;
                    double[] trans = new double[5];
                    double[] radianceAbsFree = new double[5];

                    final int camera = (int) (detectorIndex / 740);
                    for (int i = 0; i < 3; i++) {   // 13, 14, 15 !!
                        if (dlam > 0.0001) {
                            radianceAbsFree[i + 1] = r[0] + grad * (cwl[i + 1] - cwl[0]);
                        } else {
                            radianceAbsFree[i + 1] = Float.NaN;
                        }
                        trans[i + 1] = r[i + 1] / radianceAbsFree[i + 1];
                        cwl[i + 1] += dwlCorrOffsets[i][camera];
                    }

                    // Processing data...

                    //  bands 13, 14, or 15 will get bandIndex 0, 1 or 2
                    final int bandIndex = Integer.parseInt(targetBandName.split(Pattern.quote("_"))[1]) - 13;
                    final double dwl = cwl[bandIndex + 1] - OlciO2aHarmonisationConstants.cwvl[bandIndex];
                    final double transDesmiled = OlciO2aHarmonisationAlgorithm.desmileTransmission(dwl, fwhm[bandIndex + 1],
                            amf,
                            trans[bandIndex + 1],
                            desmileKdTrees[bandIndex],
                            desmileLuts[bandIndex]);
                    final double transDesmiledRectified =
                            OlciO2aHarmonisationAlgorithm.rectifyDesmiledTransmission(transDesmiled, amf, bandIndex + 13);

                    if (targetBandName.startsWith("trans_1")) {
                        targetTile.setSample(x, y, transDesmiledRectified);
                    } else if (targetBandName.startsWith("transDesmiled_1")) {
                        targetTile.setSample(x, y, transDesmiled);
                    } else if (targetBandName.startsWith("press")) {
                        final double transPress = OlciO2aHarmonisationAlgorithm.trans2Press(transDesmiledRectified, bandIndex + 13);
                        targetTile.setSample(x, y, transPress);
                    } else if (targetBandName.startsWith("surface")) {
                        final double transSurface = OlciO2aHarmonisationAlgorithm.press2Trans(surfacePress, bandIndex + 13);
                        targetTile.setSample(x, y, transSurface);
                    } else if (targetBandName.startsWith("radiance")) {
                        // radiance
                        final double harmonisedRadiance =
                                radianceAbsFree[bandIndex + 1] * solarFlux[bandIndex + 1] * transDesmiledRectified;
                        targetTile.setSample(x, y, harmonisedRadiance);
                    } else {
                        throw new OperatorException("Unexpected target band name: '" +
                                targetBandName + "' - exiting.");
                    }
                } else {
                    targetTile.setSample(x, y, Float.NaN);
                }
            }
        }
    }

    private void initDesmileAuxdata(ProgressMonitor pm) throws IOException, ParseException {
        final Path auxdataPath = OlciO2aHarmonisationIO.installAuxdata();
        pm.worked(1);
        desmileLuts = new DesmileLut[numBandsToProcess];
        desmileKdTrees = new KDTree[numBandsToProcess];
        for (int i = 13; i <= lastBandToProcess; i++) {
            desmileLuts[i - 13] = OlciO2aHarmonisationIO.createDesmileLut(auxdataPath, i);
            pm.worked(1);
            desmileKdTrees[i - 13] = OlciO2aHarmonisationIO.createKDTreeForDesmileInterpolation(desmileLuts[i - 13]);
            pm.worked(1);
        }
    }

    private void createTargetProduct() {
        targetProduct = new Product("HARMONIZED", "HARMONIZED",
                l1bProduct.getSceneRasterWidth(), l1bProduct.getSceneRasterHeight());
        targetProduct.setDescription("Harmonisation product");
        targetProduct.setStartTime(l1bProduct.getStartTime());
        targetProduct.setEndTime(l1bProduct.getEndTime());

        for (int i = 13; i <= lastBandToProcess; i++) {
            Band transBand = targetProduct.addBand("trans_" + i, ProductData.TYPE_FLOAT32);
            transBand.setUnit("dl");
            Band transDesmiledBand = targetProduct.addBand("transDesmiled_" + i, ProductData.TYPE_FLOAT32);
            transDesmiledBand.setUnit("dl");
            Band pressBand = targetProduct.addBand("press_" + i, ProductData.TYPE_FLOAT32);
            pressBand.setUnit("hPa");
            Band surfaceBand = targetProduct.addBand("surface_" + i, ProductData.TYPE_FLOAT32);
            surfaceBand.setUnit("dl");
            if (writeHarmonisedRadiances) {
                Band radianceBand = targetProduct.addBand("radiance_" + i, ProductData.TYPE_FLOAT32);
                final String unit = l1bProduct.getBand("OA12_radiance").getUnit();
                radianceBand.setUnit(unit);
            }
        }

        for (int i = 0; i < targetProduct.getNumBands(); i++) {
            targetProduct.getBandAt(i).setNoDataValue(Float.NaN);
            targetProduct.getBandAt(i).setNoDataValueUsed(true);
        }
        ProductUtils.copyTiePointGrids(l1bProduct, targetProduct);
        ProductUtils.copyGeoCoding(l1bProduct, targetProduct);

        setTargetProduct(targetProduct);
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(OlciO2aHarmonisationOp.class);
        }
    }
}
