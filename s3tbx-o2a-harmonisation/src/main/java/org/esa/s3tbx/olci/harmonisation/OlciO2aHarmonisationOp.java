package org.esa.s3tbx.olci.harmonisation;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.collocation.CollocateOp;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.PixelPos;
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
import org.esa.snap.core.util.math.MathUtils;
import org.json.simple.parser.ParseException;
import smile.neighbor.KDTree;

import java.awt.Rectangle;
import java.io.IOException;
import java.nio.file.Path;
import java.util.regex.Pattern;

/**
 * Performs Harmonisation on OLCI L1b product.
 * Authors: R.Preusker (algorithm, Python breadboard), O.Danne (Java conversion), May 2018
 * <p/>
 *
 * @author Rene Preusker
 * @author Olaf Danne
 */
@OperatorMetadata(alias = "OlciO2aHarmonisation", version = "1.1",
        authors = "R.Preusker, O.Danne",
        category = "Optical/Preprocessing",
        copyright = "Copyright (C) 2018 by Brockmann Consult",
        description = "Performs O2A band harmonisation on OLCI L1b product.")
public class OlciO2aHarmonisationOp extends Operator {

    @SourceProduct(description = "OLCI L1b product",
            label = "OLCI L1b product")
    private Product l1bProduct;

    @SourceProduct(description = "DEM product (possible improvement compared to OLCI altitude band)",
            optional = true,
            label = "DEM product")
    private Product demProduct;

    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "Name of altitude band in optional DEM file. Altitude is expected in meters.",
            label = "Name of DEM altitude band")
    private String demAltitudeBandName;

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
    private RasterDataNode collocationFlagsBand;
    private RasterDataNode altitudeBand;
    private RasterDataNode slpBand;
    private Band detectorIndexBand;

    private Band[] radianceBands;
    private Band[] cwlBands;
    private Band[] fwhmBands;
    private Band[] solarFluxBands;

    private KDTree<double[]>[] desmileKdTrees;
    private DesmileLut[] desmileLuts;

    private Product collocatedDemProduct;

    @Override
    public void initialize() throws OperatorException {
        lastBandToProcess = processOnlyBand13 ? 13 : 15;
        numBandsToProcess = lastBandToProcess - 13 + 1;

        OlciHarmonisationIO.validateSourceProduct(l1bProduct);

        try {
            initDesmileAuxdata();
        } catch (IOException | ParseException e) {
            e.printStackTrace();
            throw new OperatorException("Cannit initialize auxdata for desmile of transmissions - exiting.");
        }

        szaBand = l1bProduct.getTiePointGrid("SZA");
        ozaBand = l1bProduct.getTiePointGrid("OZA");
        slpBand = l1bProduct.getTiePointGrid("sea_level_pressure");
        detectorIndexBand = l1bProduct.getBand("detector_index");

        altitudeBand = l1bProduct.getBand("altitude");
        if (demProduct != null) {
            OlciHarmonisationIO.validateDemProduct(demProduct, demAltitudeBandName);
            collocatedDemProduct = demProduct;
            if (!isDemProductCollocated()) {
                collocatedDemProduct = collocateDemProduct();
            }
            demAltitudeBand = collocatedDemProduct.getRasterDataNode(demAltitudeBandName);
            collocationFlagsBand = collocatedDemProduct.getRasterDataNode("collocation_flags");
        }
//        setTargetProduct(collocatedDemProduct);  //  test

        radianceBands = new Band[5];
        cwlBands = new Band[5];
        fwhmBands = new Band[5];
        solarFluxBands = new Band[5];
        for (int i = 12; i < 17; i++) {
            radianceBands[i - 12] = l1bProduct.getBand("Oa" + i + "_radiance");
            cwlBands[i - 12] = l1bProduct.getBand("lambda0_band_" + i);
            fwhmBands[i - 12] = l1bProduct.getBand("FWHM_band_" + i);
            solarFluxBands[i - 12] = l1bProduct.getBand("solar_flux_band_" + i);
        }

        createTargetProduct();
    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        final Rectangle targetRectangle = targetTile.getRectangle();
        final String targetBandName = targetBand.getName();

        final Tile szaTile = getSourceTile(szaBand, targetRectangle);
        final Tile ozaTile = getSourceTile(ozaBand, targetRectangle);
        final Tile altitudeTile = getSourceTile(altitudeBand, targetRectangle);
        Tile demAltitudeTile = null;
        Tile collocationFlagsTile = null;
        if (demAltitudeBand != null) {
            demAltitudeTile = getSourceTile(demAltitudeBand, targetRectangle);
            collocationFlagsTile = getSourceTile(collocationFlagsBand, targetRectangle);
        }
        final Tile slpTile = getSourceTile(slpBand, targetRectangle);
        final Tile detectorIndexTile = getSourceTile(detectorIndexBand, targetRectangle);
        final Tile l1FlagsTile = getSourceTile(l1bProduct.getRasterDataNode("quality_flags"), targetRectangle);

        Tile[] radianceTiles = new Tile[5];
        Tile[] cwlTiles = new Tile[5];
        Tile[] fwhmTiles = new Tile[5];
        Tile[] solarFluxTiles = new Tile[5];
        for (int i = 0; i < 5; i++) {
            radianceTiles[i] = getSourceTile(radianceBands[i], targetRectangle);
            cwlTiles[i] = getSourceTile(cwlBands[i], targetRectangle);
            fwhmTiles[i] = getSourceTile(fwhmBands[i], targetRectangle);
            solarFluxTiles[i] = getSourceTile(solarFluxBands[i], targetRectangle);
        }

        for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
            checkForCancellation();
            for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {
                final boolean pixelIsValid = !l1FlagsTile.getSampleBit(x, y, OlciHarmonisationConstants.OLCI_INVALID_BIT);
                if (pixelIsValid) {
                    // Preparing input data...
                    final double sza = szaTile.getSampleDouble(x, y);
                    final double oza = ozaTile.getSampleDouble(x, y);
                    double altitude = altitudeTile.getSampleDouble(x, y);
                    // if all info from DEM is present, use DEM altitude:
                    if (demAltitudeTile != null && collocationFlagsTile != null &&
                            collocationFlagsTile.getSampleInt(x, y) == 1) {
                        altitude = demAltitudeTile.getSampleDouble(x, y);
                    }

                    final double slp = slpTile.getSampleDouble(x, y);
                    double surfacePress = OlciHarmonisationAlgorithm.height2press(altitude, slp);
                    final float detectorIndex = detectorIndexTile.getSampleFloat(x, y);

                    final double amf = (1.0 / Math.cos(sza * MathUtils.DTOR) + 1.0 / Math.cos(oza * MathUtils.DTOR));

                    double[] radiance = new double[5];
                    double[] r = new double[5];
                    double[] cwl = new double[5];
                    double[] fwhm = new double[5];
                    double[] solarFlux = new double[5];
                    for (int i = 0; i < 5; i++) {    // 12, 13, 14, 15, 16
                        radiance[i] = radianceTiles[i].getSampleDouble(x, y);
                        cwl[i] = cwlTiles[i].getSampleDouble(x, y);
                        fwhm[i] = fwhmTiles[i].getSampleDouble(x, y);
                        solarFlux[i] = solarFluxTiles[i].getSampleDouble(x, y);
                        r[i] = radiance[i] / solarFlux[i];
                    }

                    final double dlam = cwl[4] - cwl[0];
                    final double drad = r[4] - r[0];
                    double[] trans = new double[5];
                    double[] radianceAbsFree = new double[5];
                    for (int i = 0; i < 3; i++) {   // 13, 14, 15 !!
                        if (dlam > 0.0001) {
                            final double grad = drad / dlam;
                            radianceAbsFree[i + 1] = r[0] + grad * (cwl[i + 1] - cwl[0]);
                        } else {
                            radianceAbsFree[i + 1] = Float.NaN;
                        }
                        trans[i + 1] = r[i + 1] / radianceAbsFree[i + 1];
                        cwl[i + 1] += OlciHarmonisationAlgorithm.overcorrectLambda(detectorIndex,
                                                                                   OlciHarmonisationConstants.DWL_CORR_OFFSET[i]);
                    }

                    // Processing data...

                    //  bands 13, 14, or 15 will get bandIndex 0, 1 or 2
                    final int bandIndex = Integer.parseInt(targetBandName.split(Pattern.quote("_"))[1]) - 13;
                    final double dwl = cwl[bandIndex + 1] - OlciHarmonisationConstants.cwvl[bandIndex];
                    final double transDesmiled = OlciHarmonisationAlgorithm.desmileTransmission(dwl, fwhm[bandIndex + 1],
                                                                                                amf,
                                                                                                trans[bandIndex + 1],
                                                                                                desmileKdTrees[bandIndex],
                                                                                                desmileLuts[bandIndex]);
                    final double transDesmiledRectified =
                            OlciHarmonisationAlgorithm.rectifyDesmiledTransmission(transDesmiled, amf, bandIndex + 13);

                    if (targetBandName.startsWith("trans")) {
                        targetTile.setSample(x, y, transDesmiledRectified);
                    } else if (targetBandName.startsWith("press")) {
                        final double transPress = OlciHarmonisationAlgorithm.trans2Press(transDesmiledRectified, bandIndex + 13);
                        targetTile.setSample(x, y, transPress);
                    } else if (targetBandName.startsWith("surface")) {
                        final double transSurface = OlciHarmonisationAlgorithm.press2Trans(surfacePress, bandIndex + 13);
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

    private void initDesmileAuxdata() throws IOException, ParseException {
        final Path auxdataPath = OlciHarmonisationIO.installAuxdata();
        desmileLuts = new DesmileLut[numBandsToProcess];
        desmileKdTrees = new KDTree[numBandsToProcess];
        for (int i = 13; i <= lastBandToProcess; i++) {
            desmileLuts[i - 13] = OlciHarmonisationIO.createDesmileLut(auxdataPath, i);
            desmileKdTrees[i - 13] = OlciHarmonisationIO.createKDTreeForDesmileInterpolation(desmileLuts[i - 13]);
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
            Band pressBand = targetProduct.addBand("press_" + i, ProductData.TYPE_FLOAT32);
            pressBand.setUnit("hPa");
            Band surfaceBand = targetProduct.addBand("surface_" + i, ProductData.TYPE_FLOAT32);
            surfaceBand.setUnit("dl");
            if (writeHarmonisedRadiances) {
                Band radianceBand = targetProduct.addBand("radiance_" + i, ProductData.TYPE_FLOAT32);
                final String unit = l1bProduct.getBand("OA01_radiance").getUnit();
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

    private boolean isDemProductCollocated() {
        final int w1 = l1bProduct.getSceneRasterWidth();
        final int h1 = l1bProduct.getSceneRasterHeight();
        final int w2 = demProduct.getSceneRasterWidth();
        final int h2 = demProduct.getSceneRasterHeight();
        if (w1 != w2 || h1 != h2) {
            return false;
        }

        final GeoCoding gc1 = l1bProduct.getSceneGeoCoding();
        final GeoCoding gc2 = demProduct.getSceneGeoCoding();
        if (gc1.getGeoPos(new PixelPos(0, 0), null).getLat() != gc2.getGeoPos(new PixelPos(0, 0), null).getLat()) {
            return false;
        }
        if (gc1.getGeoPos(new PixelPos(0, 0), null).getLon() != gc2.getGeoPos(new PixelPos(0, 0), null).getLon()) {
            return false;
        }
        if (gc1.getGeoPos(new PixelPos(w1 - 1, h1 - 1), null).getLat() != gc2.getGeoPos(new PixelPos(w2 - 1, h2 - 1), null).getLat()) {
            return false;
        }
        if (gc1.getGeoPos(new PixelPos(w1 - 1, h1 - 1), null).getLon() != gc2.getGeoPos(new PixelPos(w2 - 1, h2 - 1), null).getLon()) {
            return false;
        }

        return true;
    }

    private Product collocateDemProduct() {
        CollocateOp op = new CollocateOp();
        op.setParameterDefaultValues();
        op.setMasterProduct(l1bProduct);
        op.setSlaveProduct(demProduct);
        op.setParameter("renameMasterComponents", false);
        op.setParameter("renameSlaveComponents", false);
        return op.getTargetProduct();
    }


    public static class Spi extends OperatorSpi {

        public Spi() {
            super(OlciO2aHarmonisationOp.class);
        }
    }
}
