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
import org.esa.snap.core.util.ProductUtils;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Map;

/**
 * OLCI pixel classification operator.
 * Processes both land and water, so we get rid of separate land/water merge operator.
 * Land pixels are classified from NN approach + rho_toa threshold.
 * Water pixels are classified from NN approach + OC-CCI algorithm (sea ice, glint).
 * <p>
 * Current classification in more detail (see email OD --> group, 20171115):
 * * - OLCI water:
 * ## cloud_sure: nn_value in [1.1, 2.75] AND rho_toa[band_17] > 0.2
 * ## cloud_ambiguous:
 * +++ nn_value in [2.75, 3.5] für 'Glint-Pixel' gemäß L1-glint-flag AND rho_toa[band_17] > 0.08
 * +++ nn_value in [2.75, 3.75] für no-glint-Pixel AND rho_toa[band_17] > 0.08
 * <p>
 * - OLCI land:
 * ## cloud_sure: nn_value in [1.1, 2.75] AND rho_toa[band_3] > 0.3
 * ## cloud_ambiguous: nn_value in [2.75, 3.85] für no-glint-Pixel AND rho_toa[band_3] > 0.25
 *
 * @author olafd
 */
@OperatorMetadata(alias = "Idepix.Olci.Classification",
        version = "1.0",
        internal = true,
        authors = "Olaf Danne",
        copyright = "(c) 2016 by Brockmann Consult",
        description = "Idepix land pixel classification operator for OLCI.")
public class OlciClassificationOp extends Operator {

    @Parameter(defaultValue = "false",
            label = " Write NN value to the target product.",
            description = " If applied, write Schiller NN value to the target product ")
    private boolean outputSchillerNNValue;

    @Parameter(defaultValue = "false",
            description = "Check for sea/lake ice also outside Sea Ice Climatology area.",
            label = "Check for sea/lake ice also outside Sea Ice Climatology area"
    )
    private boolean ignoreSeaIceClimatology;

    @Parameter(defaultValue = "false",
            label = " Compute and write O2 corrected transmissions (experimental option)",
            description = " Computes and writes O2 corrected transmissions at bands 13-15 " +
                    "(experimental option, requires additional plugin)")
    private boolean computeO2CorrectedTransmissions;


    @SourceProduct(alias = "l1b", description = "The source product.")
    Product sourceProduct;

    @SourceProduct(alias = "rhotoa")
    private Product rad2reflProduct;
    @SourceProduct(alias = "waterMask")
    private Product waterMaskProduct;

    @SourceProduct(alias = "o2", optional = true)
    private Product o2Product;


    @TargetProduct(description = "The target product.")
    Product targetProduct;

    Band cloudFlagBand;

    private Band[] olciReflBands;
    private Band landWaterBand;

    private Band trans13Band;
    private Band altitudeBand;
    private TiePointGrid szaTpg;
    private TiePointGrid ozaTpg;

    private int[] cameraBounds;

    public static final String OLCI_ALL_NET_NAME = "11x10x4x3x2_207.9.net";

    private static final double THRESH_LAND_MINBRIGHT1 = 0.3;
    private static final double THRESH_LAND_MINBRIGHT2 = 0.25;  // test OD 20170411

    private static final double THRESH_WATER_MINBRIGHT1 = 0.2;
    private static final double THRESH_WATER_MINBRIGHT2 = 0.08; // CB 20170411

    ThreadLocal<SchillerNeuralNetWrapper> olciAllNeuralNet;

    private OlciCloudNNInterpreter nnInterpreter;
    private SeaIceClassifier seaIceClassifier;

    private static final double SEA_ICE_CLIM_THRESHOLD = 10.0;


    @Override
    public void initialize() throws OperatorException {
        setBands();
        nnInterpreter = OlciCloudNNInterpreter.create();
        readSchillerNeuralNets();
        createTargetProduct();

        initSeaIceClassifier();

        landWaterBand = waterMaskProduct.getBand("land_water_fraction");

        if (computeO2CorrectedTransmissions) {
            trans13Band = o2Product.getBand("trans_13");
            altitudeBand = sourceProduct.getBand("altitude");
            szaTpg = sourceProduct.getTiePointGrid("SZA");
            ozaTpg = sourceProduct.getTiePointGrid("OZA");
        }

        if (sourceProduct.getName().contains("_EFR") || sourceProduct.getProductType().contains("_EFR")) {
            cameraBounds = OlciConstants.CAMERA_BOUNDS_FR;
        } else if (sourceProduct.getName().contains("_ERR") || sourceProduct.getProductType().contains("_ERR")) {
            cameraBounds = OlciConstants.CAMERA_BOUNDS_RR;
        } else {
            throw new OperatorException(IdepixConstants.INPUT_INCONSISTENCY_ERROR_MESSAGE);
        }
    }

    private void readSchillerNeuralNets() {
        InputStream olciAllIS = getClass().getResourceAsStream(OLCI_ALL_NET_NAME);
        olciAllNeuralNet = SchillerNeuralNetWrapper.create(olciAllIS);
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

    public void setBands() {
        olciReflBands = new Band[Rad2ReflConstants.OLCI_REFL_BAND_NAMES.length];
        for (int i = 0; i < Rad2ReflConstants.OLCI_REFL_BAND_NAMES.length; i++) {
            final int suffixStart = Rad2ReflConstants.OLCI_REFL_BAND_NAMES[i].indexOf("_");
            final String reflBandname = Rad2ReflConstants.OLCI_REFL_BAND_NAMES[i].substring(0, suffixStart);
            olciReflBands[i] = rad2reflProduct.getBand(reflBandname + "_reflectance");
        }
    }

    void createTargetProduct() throws OperatorException {
        int sceneWidth = sourceProduct.getSceneRasterWidth();
        int sceneHeight = sourceProduct.getSceneRasterHeight();

        targetProduct = new Product(sourceProduct.getName(), sourceProduct.getProductType(), sceneWidth, sceneHeight);

        // shall be the only target band!!
        cloudFlagBand = targetProduct.addBand(IdepixConstants.CLASSIF_BAND_NAME, ProductData.TYPE_INT16);
        FlagCoding flagCoding = OlciUtils.createOlciFlagCoding(IdepixConstants.CLASSIF_BAND_NAME);
        cloudFlagBand.setSampleCoding(flagCoding);
        targetProduct.getFlagCodingGroup().add(flagCoding);

        ProductUtils.copyTiePointGrids(sourceProduct, targetProduct);

        ProductUtils.copyGeoCoding(sourceProduct, targetProduct);
        targetProduct.setStartTime(sourceProduct.getStartTime());
        targetProduct.setEndTime(sourceProduct.getEndTime());
        ProductUtils.copyMetadata(sourceProduct, targetProduct);

        if (outputSchillerNNValue) {
            targetProduct.addBand(IdepixConstants.NN_OUTPUT_BAND_NAME, ProductData.TYPE_FLOAT32);
        }

        if (computeO2CorrectedTransmissions) {
            targetProduct.addBand("trans13_baseline", ProductData.TYPE_FLOAT32);
            targetProduct.addBand("trans13_baseline_AMFcorr", ProductData.TYPE_FLOAT32);
            targetProduct.addBand("trans13_excess", ProductData.TYPE_FLOAT32);
        }
    }


    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle rectangle, ProgressMonitor pm) throws OperatorException {
        // MERIS variables
        final Tile waterFractionTile = getSourceTile(landWaterBand, rectangle);

        Tile trans13Tile = null;
        Tile altitudeTile = null;
        Tile szaTile = null;
        Tile ozaTile = null;
        Tile trans13BaselineTargetTile = null;
        Tile trans13BaselineAMFCorrTargetTile = null;
        Tile trans13ExcessTargetTile = null;
        if (computeO2CorrectedTransmissions) {
            trans13Tile = getSourceTile(trans13Band, rectangle);
            altitudeTile = getSourceTile(altitudeBand, rectangle);
            szaTile = getSourceTile(szaTpg, rectangle);
            ozaTile = getSourceTile(ozaTpg, rectangle);
            trans13BaselineTargetTile = targetTiles.get(targetProduct.getBand("trans13_baseline"));
            trans13BaselineAMFCorrTargetTile = targetTiles.get(targetProduct.getBand("trans13_baseline_AMFcorr"));
            trans13ExcessTargetTile = targetTiles.get(targetProduct.getBand("trans13_excess"));
        }

        final Band olciQualityFlagBand = sourceProduct.getBand(OlciConstants.OLCI_QUALITY_FLAGS_BAND_NAME);
        final Tile olciQualityFlagTile = getSourceTile(olciQualityFlagBand, rectangle);

        Tile[] olciReflectanceTiles = new Tile[Rad2ReflConstants.OLCI_REFL_BAND_NAMES.length];
        float[] olciReflectance = new float[Rad2ReflConstants.OLCI_REFL_BAND_NAMES.length];
        for (int i = 0; i < Rad2ReflConstants.OLCI_REFL_BAND_NAMES.length; i++) {
            olciReflectanceTiles[i] = getSourceTile(olciReflBands[i], rectangle);
        }

        final Band cloudFlagTargetBand = targetProduct.getBand(IdepixConstants.CLASSIF_BAND_NAME);
        final Tile cloudFlagTargetTile = targetTiles.get(cloudFlagTargetBand);

        Tile nnTargetTile = null;
        if (outputSchillerNNValue) {
            nnTargetTile = targetTiles.get(targetProduct.getBand(IdepixConstants.NN_OUTPUT_BAND_NAME));
        }
        try {
            for (int y = rectangle.y; y < rectangle.y + rectangle.height; y++) {
                checkForCancellation();
                for (int x = rectangle.x; x < rectangle.x + rectangle.width; x++) {
                    final int waterFraction = waterFractionTile.getSampleInt(x, y);
                    initCloudFlag(olciQualityFlagTile, targetTiles.get(cloudFlagTargetBand), olciReflectance, y, x);
                    if (isOlciLandPixel(x, y, olciQualityFlagTile, waterFraction)) {
                        // only use Schiller NN approach...
                        classifyOverLand(trans13Tile, altitudeTile, szaTile, ozaTile,
                                         trans13BaselineTargetTile, trans13BaselineAMFCorrTargetTile, trans13ExcessTargetTile,
                                         olciQualityFlagTile, olciReflectanceTiles, olciReflectance, cloudFlagTargetTile,
                                         nnTargetTile, y, x);
                    } else {
                        classifyOverWater(trans13Tile, altitudeTile, szaTile, ozaTile,
                                          trans13BaselineTargetTile, trans13BaselineAMFCorrTargetTile,
                                          trans13ExcessTargetTile, olciQualityFlagTile, olciReflectanceTiles,
                                          cloudFlagTargetTile, nnTargetTile, y, x, waterFraction);
                    }
                }
            }
        } catch (Exception e) {
            throw new OperatorException("Failed to provide GA cloud screening:\n" + e.getMessage(), e);
        }
    }

    private void classifyOverWater(Tile trans13Tile, Tile altitudeTile, Tile szaTile, Tile ozaTile,
                                   Tile trans13BaselineTargetTile, Tile trans13BaselineAMFCorrTargetTile,
                                   Tile trans13ExcessTargetTile, Tile olciQualityFlagTile, Tile[] olciReflectanceTiles,
                                   Tile cloudFlagTargetTile, Tile nnTargetTile, int y, int x, int waterFraction) {
        if (computeO2CorrectedTransmissions) {
            final double trans13 = trans13Tile.getSampleDouble(x, y);
            final double altitude = altitudeTile.getSampleDouble(x, y);
            final double sza = szaTile.getSampleDouble(x, y);
            final double oza = ozaTile.getSampleDouble(x, y);
            final boolean isBright =
                    olciQualityFlagTile.getSampleBit(x, y, OlciConstants.L1_F_BRIGHT);

            final O2Correction o2Corr =
                    O2Correction.computeO2CorrectionTerms(cameraBounds, x, trans13, altitude,
                                                          sza, oza, isBright);
            classifyCloud(x, y, olciQualityFlagTile, olciReflectanceTiles, cloudFlagTargetTile, waterFraction, true,
                          o2Corr.isO2Cloud());
            trans13BaselineTargetTile.setSample(x, y, o2Corr.getTrans13Baseline());
            trans13BaselineAMFCorrTargetTile.setSample(x, y, o2Corr.getTrans13BaselineAmfCorr());
            trans13ExcessTargetTile.setSample(x, y, o2Corr.getTrans13Excess());
        }
        classifyCloud(x, y, olciQualityFlagTile, olciReflectanceTiles, cloudFlagTargetTile, waterFraction);
        if (outputSchillerNNValue) {
            final double[] nnOutput = getOlciNNOutput(x, y, olciReflectanceTiles);
            nnTargetTile.setSample(x, y, nnOutput[0]);
        }
    }

    private void classifyOverLand(Tile trans13Tile, Tile altitudeTile, Tile szaTile, Tile ozaTile,
                                  Tile trans13BaselineTargetTile, Tile trans13BaselineAMFCorrTargetTile,
                                  Tile trans13ExcessTargetTile, Tile olciQualityFlagTile, Tile[] olciReflectanceTiles,
                                  float[] olciReflectance, Tile cloudFlagTargetTile, Tile nnTargetTile, int y, int x) {
        for (int i = 0; i < Rad2ReflConstants.OLCI_REFL_BAND_NAMES.length; i++) {
            olciReflectance[i] = olciReflectanceTiles[i].getSampleFloat(x, y);
        }

        SchillerNeuralNetWrapper nnWrapper = olciAllNeuralNet.get();
        double[] inputVector = nnWrapper.getInputVector();
        for (int i = 0; i < inputVector.length; i++) {
            inputVector[i] = Math.sqrt(olciReflectance[i]);
        }

        final double nnOutput = nnWrapper.getNeuralNet().calc(inputVector)[0];

        if (!cloudFlagTargetTile.getSampleBit(x, y, IdepixConstants.IDEPIX_INVALID)) {
            cloudFlagTargetTile.setSample(x, y, IdepixConstants.IDEPIX_CLOUD_AMBIGUOUS, false);
            cloudFlagTargetTile.setSample(x, y, IdepixConstants.IDEPIX_CLOUD_SURE, false);
            cloudFlagTargetTile.setSample(x, y, IdepixConstants.IDEPIX_CLOUD, false);
            cloudFlagTargetTile.setSample(x, y, IdepixConstants.IDEPIX_SNOW_ICE, false);

            // CB 20170406:
            final boolean cloudSure = olciReflectance[2] > THRESH_LAND_MINBRIGHT1 &&
                    nnInterpreter.isCloudSure(nnOutput);
            final boolean cloudAmbiguous = olciReflectance[2] > THRESH_LAND_MINBRIGHT2 &&
                    nnInterpreter.isCloudAmbiguous(nnOutput, true, false);

            if (computeO2CorrectedTransmissions) {
                final double trans13 = trans13Tile.getSampleDouble(x, y);
                final double altitude = altitudeTile.getSampleDouble(x, y);
                final double sza = szaTile.getSampleDouble(x, y);
                final double oza = ozaTile.getSampleDouble(x, y);
                final boolean isBright =
                        olciQualityFlagTile.getSampleBit(x, y, OlciConstants.L1_F_BRIGHT);

                final O2Correction o2Corr =
                        O2Correction.computeO2CorrectionTerms(cameraBounds, x, trans13, altitude,
                                                              sza, oza, isBright);

                cloudFlagTargetTile.setSample(x, y, IdepixConstants.IDEPIX_CLOUD, o2Corr.isO2Cloud());
                trans13BaselineTargetTile.setSample(x, y, o2Corr.getTrans13Baseline());
                trans13BaselineAMFCorrTargetTile.setSample(x, y, o2Corr.getTrans13BaselineAmfCorr());
                trans13ExcessTargetTile.setSample(x, y, o2Corr.getTrans13Excess());

            } else {
                cloudFlagTargetTile.setSample(x, y, IdepixConstants.IDEPIX_CLOUD_AMBIGUOUS, cloudAmbiguous);
                cloudFlagTargetTile.setSample(x, y, IdepixConstants.IDEPIX_CLOUD_SURE, cloudSure);
                cloudFlagTargetTile.setSample(x, y, IdepixConstants.IDEPIX_CLOUD, cloudAmbiguous || cloudSure);
            }
            cloudFlagTargetTile.setSample(x, y, IdepixConstants.IDEPIX_SNOW_ICE, nnInterpreter.isSnowIce(nnOutput));
        }

        if (nnTargetTile != null) {
            nnTargetTile.setSample(x, y, nnOutput);
        }
    }

    private boolean isOlciLandPixel(int x, int y, Tile olciL1bFlagTile, int waterFraction) {
        // the water mask ends at 59 Degree south, stop earlier to avoid artefacts
        if (getGeoPos(x, y).lat > -58f) {
            // values bigger than 100 indicate no data
            if (waterFraction <= 100) {
                // todo: this does not work if we have a PixelGeocoding. In that case, waterFraction
                // is always 0 or 100!! (TS, OD, 20140502)
                return waterFraction == 0;
            } else {
                return olciL1bFlagTile.getSampleBit(x, y, OlciConstants.L1_F_LAND);
            }
        } else {
            return olciL1bFlagTile.getSampleBit(x, y, OlciConstants.L1_F_LAND);
        }
    }

    private void classifyCloud(int x, int y, Tile l1FlagsTile, Tile[] rhoToaTiles, Tile targetTile,
                               int waterFraction) {
        classifyCloud(x, y, l1FlagsTile, rhoToaTiles, targetTile, waterFraction, false, false);
    }

    private void classifyCloud(int x, int y, Tile l1FlagsTile, Tile[] rhoToaTiles, Tile targetTile,
                               int waterFraction, boolean computeO2CorrectedTransmissions, boolean isO2Cloud) {

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

            if (computeO2CorrectedTransmissions) {
                targetTile.setSample(x, y, IdepixConstants.IDEPIX_CLOUD, isO2Cloud);
                if (checkForSeaIce && nnInterpreter.isSnowIce(nnOutput)) {
                    targetTile.setSample(x, y, IdepixConstants.IDEPIX_SNOW_ICE, true);
                }
            } else {
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

    private GeoPos getGeoPos(int x, int y) {
        final GeoPos geoPos = new GeoPos();
        final GeoCoding geoCoding = getSourceProduct().getSceneGeoCoding();
        final PixelPos pixelPos = new PixelPos(x, y);
        geoCoding.getGeoPos(pixelPos, geoPos);
        return geoPos;
    }

    void initCloudFlag(Tile olciL1bFlagTile, Tile targetTile, float[] olciReflectances, int y, int x) {
        // for given instrument, compute boolean pixel properties and write to cloud flag band
        final boolean l1Invalid = olciL1bFlagTile.getSampleBit(x, y, OlciConstants.L1_F_INVALID);
        final boolean reflectancesValid = IdepixIO.areAllReflectancesValid(olciReflectances);

        targetTile.setSample(x, y, IdepixConstants.IDEPIX_INVALID, l1Invalid || !reflectancesValid);
        targetTile.setSample(x, y, IdepixConstants.IDEPIX_CLOUD, false);
        targetTile.setSample(x, y, IdepixConstants.IDEPIX_CLOUD_SURE, false);
        targetTile.setSample(x, y, IdepixConstants.IDEPIX_CLOUD_AMBIGUOUS, false);
        targetTile.setSample(x, y, IdepixConstants.IDEPIX_SNOW_ICE, false);
        targetTile.setSample(x, y, IdepixConstants.IDEPIX_CLOUD_BUFFER, false);
        targetTile.setSample(x, y, IdepixConstants.IDEPIX_CLOUD_SHADOW, false);
        targetTile.setSample(x, y, IdepixConstants.IDEPIX_COASTLINE, false);
        targetTile.setSample(x, y, IdepixConstants.IDEPIX_LAND, true);   // already checked
    }

    public static class Spi extends OperatorSpi {
        public Spi() {
            super(OlciClassificationOp.class);
        }
    }
}
