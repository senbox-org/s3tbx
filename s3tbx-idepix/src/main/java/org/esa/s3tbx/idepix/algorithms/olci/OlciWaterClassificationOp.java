package org.esa.s3tbx.idepix.algorithms.olci;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s3tbx.idepix.core.IdepixConstants;
import org.esa.s3tbx.idepix.core.seaice.SeaIceClassification;
import org.esa.s3tbx.idepix.core.seaice.SeaIceClassifier;
import org.esa.s3tbx.idepix.core.util.IdepixIO;
import org.esa.s3tbx.idepix.core.util.SchillerNeuralNetWrapper;
import org.esa.s3tbx.processor.rad2refl.Rad2ReflConstants;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.RectangleExtender;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;

/**
 * OLCI pixel classification operator.
 * Only water pixels are classified from NN approach (following MERIS approach for BEAM Cawa and OC-CCI algorithm).
 *
 * @author olafd
 */
@OperatorMetadata(alias = "Idepix.Olci.Water",
        version = "1.0",
        internal = true,
        authors = "Olaf Danne",
        copyright = "(c) 2016 by Brockmann Consult",
        description = "Idepix water pixel classification operator for OLCI.")
public class OlciWaterClassificationOp extends Operator {

    @Parameter(label = " Sea Ice Climatology Value", defaultValue = "false")
    private boolean ccOutputSeaIceClimatologyValue;

    @Parameter(defaultValue = "false",
            description = "Check for sea/lake ice also outside Sea Ice Climatology area.",
            label = "Check for sea/lake ice also outside Sea Ice Climatology area"
    )
    private boolean ignoreSeaIceClimatology;

    @Parameter(label = "Cloud screening 'ambiguous' threshold", defaultValue = "1.4")
    private double cloudScreeningAmbiguous;     // Schiller, used in previous approach only

    @Parameter(label = "Cloud screening 'sure' threshold", defaultValue = "1.8")
    private double cloudScreeningSure;         // Schiller, used in previous approach only

    @Parameter(defaultValue = "false",
            label = " Write NN value to the target product.",
            description = " If applied, write NN value to the target product ")
    private boolean outputSchillerNNValue;

    // We only have the All NN (mp/20170324)
//    @Parameter(defaultValue = "true",
//            label = " Use 'all' NN instead of separate land and water NNs.",
//            description = " If applied, 'all' NN instead of separate land and water NNs is used. ")
//    private boolean useSchillerNNAll;


    @SourceProduct(alias = "l1b")
    private Product l1bProduct;
    @SourceProduct(alias = "rhotoa")
    private Product rhoToaProduct;
    @SourceProduct(alias = "waterMask")
    private Product waterMaskProduct;

    @SuppressWarnings({"FieldCanBeLocal"})
    @TargetProduct
    private Product targetProduct;

    public static final String OLCI_ALL_NET_NAME = "11x10x4x3x2_207.9.net";

    private static final double THRESH_WATER_MINBRIGHT1 = 0.2;
//    private static final double THRESH_WATER_MINBRIGHT2 = 0.2;
    private static final double THRESH_WATER_MINBRIGHT2 = 0.08; // CB 20170411

    ThreadLocal<SchillerNeuralNetWrapper> olciAllNeuralNet;

    private static final double SEA_ICE_CLIM_THRESHOLD = 10.0;

    private Band cloudFlagBand;
    private SeaIceClassifier seaIceClassifier;
    private Band landWaterBand;
    private Band nnOutputBand;

    private RectangleExtender rectExtender;
    private OlciCloudNNInterpreter nnInterpreter;

    @Override
    public void initialize() throws OperatorException {
        readSchillerNets();
        nnInterpreter = OlciCloudNNInterpreter.create();
        createTargetProduct();

        initSeaIceClassifier();

        landWaterBand = waterMaskProduct.getBand("land_water_fraction");

        rectExtender = new RectangleExtender(new Rectangle(l1bProduct.getSceneRasterWidth(),
                                                           l1bProduct.getSceneRasterHeight()), 1, 1);
    }

    private void readSchillerNets() {
        try (InputStream isAll = getClass().getResourceAsStream(OLCI_ALL_NET_NAME)) {
            olciAllNeuralNet = SchillerNeuralNetWrapper.create(isAll);
        } catch (IOException e) {
            throw new OperatorException("Cannot read Neural Nets: " + e.getMessage());
        }
    }

    private void initSeaIceClassifier() {
        final ProductData.UTC startTime = getSourceProduct().getStartTime();
        final int monthIndex = startTime.getAsCalendar().get(Calendar.MONTH);
        try {
            seaIceClassifier = new SeaIceClassifier(monthIndex + 1);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createTargetProduct() {
        targetProduct = IdepixIO.createCompatibleTargetProduct(l1bProduct, "MER", "MER_L2", true);

        cloudFlagBand = targetProduct.addBand(IdepixConstants.CLASSIF_BAND_NAME, ProductData.TYPE_INT16);
        FlagCoding flagCoding = OlciUtils.createOlciFlagCoding(IdepixConstants.CLASSIF_BAND_NAME);
        cloudFlagBand.setSampleCoding(flagCoding);
        targetProduct.getFlagCodingGroup().add(flagCoding);

        if (outputSchillerNNValue) {
            nnOutputBand = targetProduct.addBand(IdepixConstants.NN_OUTPUT_BAND_NAME,
                                                 ProductData.TYPE_FLOAT32);
        }
    }

    @Override
    public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        Rectangle targetRectangle = targetTile.getRectangle();
        try {
            final Rectangle sourceRectangle = rectExtender.extend(targetRectangle);

            Tile[] rhoToaTiles = new Tile[Rad2ReflConstants.OLCI_REFL_BAND_NAMES.length];
            for (int i = 0; i < Rad2ReflConstants.OLCI_REFL_BAND_NAMES.length; i++) {
                final int suffixStart = Rad2ReflConstants.OLCI_REFL_BAND_NAMES[i].indexOf("_");
                final String reflBandname = Rad2ReflConstants.OLCI_REFL_BAND_NAMES[i].substring(0, suffixStart);
                final Band rhoToaBand = rhoToaProduct.getBand(reflBandname + "_reflectance");
                rhoToaTiles[i] = getSourceTile(rhoToaBand, sourceRectangle);
            }

            Tile l1FlagsTile = getSourceTile(l1bProduct.getBand(OlciConstants.OLCI_QUALITY_FLAGS_BAND_NAME),
                                             sourceRectangle);
            Tile waterFractionTile = getSourceTile(landWaterBand, sourceRectangle);

            for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
                checkForCancellation();
                for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {
                    if (!l1FlagsTile.getSampleBit(x, y, OlciConstants.L1_F_INVALID)) {
                        final int waterFraction = waterFractionTile.getSampleInt(x, y);

                        if (isLandPixel(x, y, l1FlagsTile, waterFraction)) {
                            if (band == cloudFlagBand) {
                                targetTile.setSample(x, y, OlciConstants.L1_F_LAND, true);
                            } else {
                                targetTile.setSample(x, y, Float.NaN);
                            }
                        } else {
                            if (band == cloudFlagBand) {
                                classifyCloud(x, y, l1FlagsTile, rhoToaTiles, targetTile, waterFraction);
                            }
                            if (outputSchillerNNValue && band == nnOutputBand) {
                                final double[] nnOutput = getOlciNNOutput(x, y, rhoToaTiles);
                                targetTile.setSample(x, y, nnOutput[0]);
                            }
                        }
                    } else {
                        targetTile.setSample(x, y, IdepixConstants.IDEPIX_INVALID, true);
                    }
                }
            }
        } catch (Exception e) {
            throw new OperatorException(e);
        }
    }

    private boolean isLandPixel(int x, int y, Tile l1FlagsTile, int waterFraction) {
        // the water mask ends at 59 Degree south, stop earlier to avoid artefacts
        if (getGeoPos(x, y).lat > -58f) {
            // values bigger than 100 indicate no data
            if (waterFraction <= 100) {
                // todo: this does not work if we have a PixelGeocoding. In that case, waterFraction
                // is always 0 or 100!! (TS, OD, 20140502)
                return waterFraction == 0;
            } else {
                return l1FlagsTile.getSampleBit(x, y, OlciConstants.L1_F_LAND);
            }
        } else {
            return l1FlagsTile.getSampleBit(x, y, OlciConstants.L1_F_LAND);
        }
    }

    private boolean isGlintPixel(int x, int y, Tile l1FlagsTile) {
        return l1FlagsTile.getSampleBit(x, y, OlciConstants.L1_F_GLINT);
    }

    private boolean isCoastlinePixel(int x, int y, int waterFraction) {
        // the water mask ends at 59 Degree south, stop earlier to avoid artefacts
        // values bigger than 100 indicate no data
        // todo: this does not work if we have a PixelGeocoding. In that case, waterFraction
        // is always 0 or 100!! (TS, OD, 20140502)
        return getGeoPos(x, y).lat > -58f && waterFraction <= 100 && waterFraction < 100 && waterFraction > 0;
    }

    private void classifyCloud(int x, int y, Tile l1FlagsTile, Tile[] rhoToaTiles, Tile targetTile, int waterFraction) {

        final boolean isCoastline = isCoastlinePixel(x, y, waterFraction);
        targetTile.setSample(x, y, IdepixConstants.IDEPIX_COASTLINE, isCoastline);

        boolean checkForSeaIce = false;
        if (!isCoastline) {
            // over water
            final GeoPos geoPos = getGeoPos(x, y);

            checkForSeaIce = ignoreSeaIceClimatology || isPixelClassifiedAsSeaice(geoPos);
        }

        double nnOutput = getOlciNNOutput(x, y, rhoToaTiles)[0];
        if (!targetTile.getSampleBit(x, y, IdepixConstants.IDEPIX_INVALID)) {
            targetTile.setSample(x, y, IdepixConstants.IDEPIX_CLOUD_AMBIGUOUS, false);
            targetTile.setSample(x, y, IdepixConstants.IDEPIX_CLOUD_SURE, false);
            targetTile.setSample(x, y, IdepixConstants.IDEPIX_CLOUD, false);
            targetTile.setSample(x, y, IdepixConstants.IDEPIX_SNOW_ICE, false);

            final boolean isGlint = isGlintPixel(x, y, l1FlagsTile);
            // CB 20170406:
            final boolean cloudSure = rhoToaTiles[16].getSampleFloat(x, y) > THRESH_WATER_MINBRIGHT1 &&
                    nnInterpreter.isCloudSure(nnOutput);
            final boolean cloudAmbiguous = rhoToaTiles[16].getSampleFloat(x, y) > THRESH_WATER_MINBRIGHT2 &&
                    nnInterpreter.isCloudAmbiguous(nnOutput, false, isGlint);

            targetTile.setSample(x, y, IdepixConstants.IDEPIX_CLOUD_AMBIGUOUS, cloudAmbiguous);
            targetTile.setSample(x, y, IdepixConstants.IDEPIX_CLOUD_SURE, cloudSure);
            targetTile.setSample(x, y, IdepixConstants.IDEPIX_CLOUD, cloudAmbiguous || cloudSure);

            if (checkForSeaIce && nnInterpreter.isSnowIce(nnOutput)) {
                targetTile.setSample(x, y, IdepixConstants.IDEPIX_SNOW_ICE, true);
                targetTile.setSample(x, y, IdepixConstants.IDEPIX_CLOUD_SURE, false);
                targetTile.setSample(x, y, IdepixConstants.IDEPIX_CLOUD, false);
            }
        }
    }

    private double[] getOlciNNOutput(int x, int y, Tile[] rhoToaTiles) {
        SchillerNeuralNetWrapper nnWrapper = olciAllNeuralNet.get();
        double[] nnInput = nnWrapper.getInputVector();
        for (int i = 0; i < nnInput.length; i++) {
            nnInput[i] = Math.sqrt(rhoToaTiles[i].getSampleFloat(x, y));
        }
        return nnWrapper.getNeuralNet().calc(nnInput);
    }

    private boolean isPixelClassifiedAsSeaice(GeoPos geoPos) {
        // check given pixel, but also neighbour cell from 1x1 deg sea ice climatology...
        final double maxLon = 360.0;
        final double minLon = 0.0;
        final double maxLat = 180.0;
        final double minLat = 0.0;

        for (int y = -1; y <= 1; y++) {
            for (int x = -1; x <= 1; x++) {
                // for sea ice climatology indices, we need to shift lat/lon onto [0,180]/[0,360]...
                double lon = geoPos.lon + 180.0 + x * 1.0;
                double lat = 90.0 - geoPos.lat + y * 1.0;
                lon = Math.max(lon, minLon);
                lon = Math.min(lon, maxLon);
                lat = Math.max(lat, minLat);
                lat = Math.min(lat, maxLat);
                final SeaIceClassification classification = seaIceClassifier.getClassification(lat, lon);
                if (classification.max >= SEA_ICE_CLIM_THRESHOLD) {
                    return true;
                }
            }
        }
        return false;
    }

    private GeoPos getGeoPos(int x, int y) {
        final GeoPos geoPos = new GeoPos();
        final GeoCoding geoCoding = getSourceProduct().getSceneGeoCoding();
        final PixelPos pixelPos = new PixelPos(x, y);
        geoCoding.getGeoPos(pixelPos, geoPos);
        return geoPos;
    }

    public static class Spi extends OperatorSpi {
        public Spi() {
            super(OlciWaterClassificationOp.class);
        }
    }

}
