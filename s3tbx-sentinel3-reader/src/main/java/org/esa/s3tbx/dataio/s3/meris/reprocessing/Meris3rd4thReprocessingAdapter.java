package org.esa.s3tbx.dataio.s3.meris.reprocessing;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glevel.MultiLevelImage;
import org.esa.s3tbx.dataio.s3.ReprocessingAdapter;
import org.esa.snap.core.dataio.geocoding.ComponentFactory;
import org.esa.snap.core.dataio.geocoding.ComponentGeoCoding;
import org.esa.snap.core.dataio.geocoding.GeoRaster;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.util.BitSetter;
import org.esa.snap.core.util.ProductUtils;

import javax.media.jai.RenderedOp;
import javax.media.jai.operator.MeanDescriptor;
import java.awt.*;
import java.awt.image.renderable.ParameterBlock;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Class providing the adaptation of a MERIS L1b product from fourth to third reprocessing version.
 *
 * @author olafd
 */
public class Meris3rd4thReprocessingAdapter implements ReprocessingAdapter {

    static String MERIS_DETECTOR_INDEX_DS_NAME = "detector_index";
    static String MERIS_L1B_RADIANCE_BAND_NAME_PREFIX = "radiance";
    static int MERIS_L1B_NUM_SPECTRAL_BANDS = 15;

    private final Map<Integer, Integer> qualityToL1FlagMap = new HashMap<>();

    public Meris3rd4thReprocessingAdapter() {
        setupQualityToL1FlagMap();
    }

    @Override
    public Product convertToLowerVersion(Product inputProduct) {
        Product thirdReproProduct = new Product(inputProduct.getName(),
                inputProduct.getProductType(),
                inputProduct.getSceneRasterWidth(),
                inputProduct.getSceneRasterHeight());

        set3rdReprocessingGeoCoding(inputProduct, thirdReproProduct);

        adaptProductInformationToThirdRepro(inputProduct, thirdReproProduct);

        // adapt band names:
        // ## M%02d_radiance --> radiance_%d
        // ## detector_index --> detector_index
        adaptBandNamesToThirdRepro(inputProduct, thirdReproProduct);

        // adapt tie point grids:
        // TP_latitude --> latitude
        // TP_longitude --> longitude
        // TP_altitude --> dem_alt  // todo: clarify this!
        // n.a. --> dem_rough  // todo: clarify this!
        // n.a. --> lat_corr  // todo: clarify this!
        // n.a. --> lon_corr  // todo: clarify this!
        // SZA --> sun_zenith
        // OZA --> view_zenith
        // SAA --> sun_azimuth
        // OAA --> view_azimuth
        // horiz_wind_vector_1 --> zonal_wind
        // horiz_wind_vector_2 --> merid_wind
        // sea_level_pressure --> atm_press
        // total_ozone --> ozone , convert units
        // humidity_pressure_level_14 --> rel_hum  // relative humidity at 850 hPa
        adaptTiePointGridsToThirdRepro(inputProduct, thirdReproProduct);

        // adapt flag band and coding:
        // quality_flags --> l1_flags
        // ##  cosmetic (2^24) --> COSMETIC (1)
        // ##  duplicated (2^23) --> DUPLICATED (2)
        // ##  sun_glint_risk (2^22) --> GLINT_RISK (4)
        // ##  dubious (2^21) --> SUSPECT (8)
        // ##  land (2^31) --> LAND_OCEAN (16)
        // ##  bright (2^27) --> BRIGHT (32)
        // ##  coastline (2^30) --> COASTLINE (64)
        // ##  invalid (2^2^25) --> INVALID (128)
        try {
            adaptFlagBandToThirdRepro(inputProduct, thirdReproProduct);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // adapt metadata:
        // Idepix MERIS does not use any product metadata
        // --> for the moment, copy just a few elements from element metadataSection:
        Meris3rd4thReprocessingMetadata.fillMetadataInThirdRepro(inputProduct, thirdReproProduct);

        return thirdReproProduct;
    }

    @Override
    public Product convertToHigherVersion(Product inputProduct) {
        // not yet implemented
        return null;
    }

    /* package local for testing */
    int convertQualityToL1FlagValue(long qualityFlagValue) {
        int l1FlagValue = 0;
        for (int i = 0; i < Integer.SIZE; i++) {
            if (qualityToL1FlagMap.containsKey(i) && BitSetter.isFlagSet(qualityFlagValue, i)) {
                l1FlagValue += qualityToL1FlagMap.get(i);
            }
        }
        return l1FlagValue;
    }

    /* package local for testing */
    static float getMeanSolarFluxFrom4thReprocessing(Band solarFluxBand) {
        final MultiLevelImage sourceImage = solarFluxBand.getSourceImage();
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(sourceImage);// The source image
        pb.add(null);    // null ROI means whole image
        pb.add(1);       // check every pixel horizontally
        pb.add(1);       // check every pixel vertically

        // Perform the mean operation on the source image.
        final RenderedOp meanImage = MeanDescriptor.create(sourceImage, null, 1, 1, null);
        double[] mean = (double[]) meanImage.getProperty("mean");

        return (float) mean[0];
    }

    private static boolean isMerisRR(Product inputProduct) {
        return inputProduct.getProductType().startsWith("ME_1_R");
    }

    private static void copyGeneralBandProperties(Band sourceBand, Band targetBand) {
        targetBand.setUnit(sourceBand.getUnit());
    }

    private static void copySpectralBandProperties(Band sourceBand, Band targetBand) {
        targetBand.setDescription("TOA radiance band " + sourceBand.getSpectralBandIndex());
        targetBand.setSpectralBandIndex(sourceBand.getSpectralBandIndex());
        targetBand.setSpectralWavelength(sourceBand.getSpectralWavelength());
        targetBand.setSpectralBandwidth(sourceBand.getSpectralBandwidth());
        targetBand.setNoDataValueUsed(false);
        targetBand.setNoDataValue(0.0);
    }

    private static Band createL1bFlagBand(Product thirdReproProduct) {

        Band l1FlagBand = new Band("l1_flags", ProductData.TYPE_INT32,
                thirdReproProduct.getSceneRasterWidth(), thirdReproProduct.getSceneRasterHeight());
        l1FlagBand.setDescription("Level 1b classification and quality flags");

        FlagCoding l1FlagCoding = new FlagCoding("l1_flags");
        l1FlagCoding.addFlag("COSMETIC", BitSetter.setFlag(0, 0), "Pixel is cosmetic");
        l1FlagCoding.addFlag("DUPLICATED", BitSetter.setFlag(0, 1), "Pixel has been duplicated (filled in)");
        l1FlagCoding.addFlag("GLINT_RISK", BitSetter.setFlag(0, 2), "Pixel has glint risk");
        l1FlagCoding.addFlag("SUSPECT", BitSetter.setFlag(0, 3), "Pixel is suspect");
        l1FlagCoding.addFlag("LAND_OCEAN", BitSetter.setFlag(0, 4), "Pixel is over land, not ocean");
        l1FlagCoding.addFlag("BRIGHT", BitSetter.setFlag(0, 5), "Pixel is bright");
        l1FlagCoding.addFlag("COASTLINE", BitSetter.setFlag(0, 6), "Pixel is part of a coastline");
        l1FlagCoding.addFlag("INVALID", BitSetter.setFlag(0, 7), "Pixel is invalid");

        l1FlagBand.setSampleCoding(l1FlagCoding);
        thirdReproProduct.getFlagCodingGroup().add(l1FlagCoding);

        return l1FlagBand;
    }

    private static void setupL1FlagsBitmask(Product thirdReproProduct) {

        int index = 0;
        int w = thirdReproProduct.getSceneRasterWidth();
        int h = thirdReproProduct.getSceneRasterHeight();
        Mask mask;

        mask = Mask.BandMathsType.create("cosmetic", "Pixel is cosmetic", w, h,
                "l1_flags.COSMETIC", new Color(204, 153, 255), 0.5f);
        thirdReproProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("duplicated", "Pixel has been duplicated (filled in)", w, h,
                "l1_flags.DUPLICATED", new Color(255, 200, 0), 0.5f);
        thirdReproProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("glint_risk", "Pixel has glint risk", w, h,
                "l1_flags.GLINT_RISK", new Color(255, 0, 255), 0.5f);
        thirdReproProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("suspect", "Pixel is suspect", w, h,
                "l1_flags.SUSPECT", new Color(204, 102, 255), 0.5f);
        thirdReproProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("land", "Pixel is over land, not ocean", w, h,
                "l1_flags.LAND_OCEAN", new Color(51, 153, 0), 0.5f);
        thirdReproProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("water", "Pixel is over ocean, not land", w, h,
                "not l1_flags.LAND_OCEAN", new Color(153, 153, 255), 0.5f);
        thirdReproProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("bright", "Pixel is bright", w, h,
                "l1_flags.BRIGHT", new Color(255, 255, 0), 0.5f);
        thirdReproProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("coastline", "Pixel is part of a coastline", w, h,
                "l1_flags.COASTLINE", Color.GREEN, 0.5f);
        thirdReproProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("invalid", "Pixel is invaid", w, h,
                "l1_flags.INVALID", Color.RED, 0.5f);
        thirdReproProduct.getMaskGroup().add(index, mask);
    }

    private static void set3rdReprocessingGeoCoding(Product inputProduct, Product thirdReproProduct) {
        final TiePointGrid latTPG = inputProduct.getTiePointGrid("TP_latitude");
        final float[] latTiePoints = latTPG.getTiePoints();
        final double[] latTiePointsD = new double[latTiePoints.length];
        for (int i = 0; i < latTiePoints.length; i++) {
            latTiePointsD[i] = latTiePoints[i];
        }
        final TiePointGrid lonTPG = inputProduct.getTiePointGrid("TP_longitude");
        final float[] lonTiePoints = lonTPG.getTiePoints();
        final double[] lonTiePointsD = new double[lonTiePoints.length];
        for (int i = 0; i < lonTiePoints.length; i++) {
            lonTiePointsD[i] = lonTiePoints[i];
        }

        GeoRaster geoRaster;
        if (isMerisRR(inputProduct)) {
            geoRaster = new GeoRaster(lonTiePointsD, latTiePointsD, "longitude", "latitude",
                    lonTPG.getRasterWidth(), latTPG.getRasterHeight(),
                    thirdReproProduct.getSceneRasterWidth(), thirdReproProduct.getSceneRasterHeight(),
                    1.2, 0.5, 0.5, 16.0, 16.0);
        } else {
            geoRaster = new GeoRaster(lonTiePointsD, latTiePointsD, "longitude", "latitude",
                    lonTPG.getRasterWidth(), latTPG.getRasterHeight(),
                    thirdReproProduct.getSceneRasterWidth(), thirdReproProduct.getSceneRasterHeight(),
                    0.3, 0.5, 0.5, 64.0, 64.0);
        }

        thirdReproProduct.setSceneGeoCoding(new ComponentGeoCoding(geoRaster,
                ComponentFactory.getForward("FWD_TIE_POINT_BILINEAR"),
                ComponentFactory.getInverse("INV_TIE_POINT")));
    }

    private void adaptProductInformationToThirdRepro(Product inputProduct, Product thirdReproProduct) {
        // we often need the start/stop times for downstream processors...
        thirdReproProduct.setStartTime(inputProduct.getStartTime());
        thirdReproProduct.setEndTime(inputProduct.getEndTime());
        // todo: discuss what else we want to set back to 3RP here
        thirdReproProduct.setPreferredTileSize(inputProduct.getPreferredTileSize());

        if (isMerisRR(inputProduct)) {
            // we often need the 3RP product type for downstream processors (e.g. for loading MER Auxdata)
            thirdReproProduct.setProductType("MER_RR__1P");
        } else if (isMerisFR(inputProduct)) {
            thirdReproProduct.setProductType("MER_FRG_1P");
        }
    }

    private boolean isMerisFR(Product inputProduct) {
        return inputProduct.getProductType().startsWith("ME_1_F");
    }

    private void adaptFlagBandToThirdRepro(Product inputProduct, Product thirdReproProduct) throws IOException {

        Band l1FlagBand = createL1bFlagBand(thirdReproProduct);
        setupL1FlagsBitmask(thirdReproProduct);

        final Band qualityFlagBand = inputProduct.getBand("quality_flags");
        final int width = inputProduct.getSceneRasterWidth();
        final int height = inputProduct.getSceneRasterHeight();
        int[] qualityFlagData = new int[width * height];
        int[] l1FlagData = new int[width * height];
        qualityFlagBand.readPixels(0, 0, width, height, qualityFlagData, ProgressMonitor.NULL);
        for (int i = 0; i < width * height; i++) {
            l1FlagData[i] = convertQualityToL1FlagValue(qualityFlagData[i]);
        }

        l1FlagBand.ensureRasterData();
        l1FlagBand.setPixels(0, 0, width, height, l1FlagData);
        l1FlagBand.setSourceImage(l1FlagBand.getSourceImage());
        thirdReproProduct.addBand(l1FlagBand);
    }

    private void adaptTiePointGridsToThirdRepro(Product inputProduct, Product thirdReproProduct) {
        // adapt tie point grids:
        // TP_latitude --> latitude
        // TP_longitude --> longitude
        // TP_altitude --> dem_alt  // todo: clarify this!
        // n.a. --> dem_rough  // todo: clarify this!
        // n.a. --> lat_corr  // todo: clarify this!
        // n.a. --> lon_corr  // todo: clarify this!
        // SZA --> sun_zenith
        // OZA --> view_zenith
        // SAA --> sun_azimuth
        // OAA --> view_azimuth
        // horizontal_wind_vector_1 --> zonal_wind
        // horizontal_wind_vector_2 --> merid_wind
        // sea_level_pressure --> atm_press
        // total_ozone --> ozone
        // humidity_pressure_level_14 --> rel_hum  // relative humidity at 850 hPa

        if (!thirdReproProduct.containsTiePointGrid("TP_latitude")) {
            ProductUtils.copyTiePointGrid("TP_latitude", inputProduct, thirdReproProduct);
        }
        final TiePointGrid latitudeTargetTPG = thirdReproProduct.getTiePointGrid("TP_latitude");
        latitudeTargetTPG.setName("latitude");
        latitudeTargetTPG.setDescription("Latitude of the tie points (WGS-84), positive N");
        latitudeTargetTPG.setUnit("deg");

        if (!thirdReproProduct.containsTiePointGrid("TP_longitude")) {
            ProductUtils.copyTiePointGrid("TP_longitude", inputProduct, thirdReproProduct);
        }
        final TiePointGrid longitudeTargetTPG = thirdReproProduct.getTiePointGrid("TP_longitude");
        longitudeTargetTPG.setName("longitude");
        longitudeTargetTPG.setDescription("Longitude of the tie points (WGS-84), Greenwich origin, positive E");
        longitudeTargetTPG.setUnit("deg");

        final TiePointGrid altitudeTargetTPG =
                ProductUtils.copyTiePointGrid("TP_altitude", inputProduct, thirdReproProduct);
        if (altitudeTargetTPG != null) {
            altitudeTargetTPG.setName("dem_alt");
            altitudeTargetTPG.setDescription("Digital elevation model altitude");
        }

        final TiePointGrid szaTargetTPG =
                ProductUtils.copyTiePointGrid("SZA", inputProduct, thirdReproProduct);
        if (szaTargetTPG != null) {
            szaTargetTPG.setName("sun_zenith");
            szaTargetTPG.setDescription("Viewing zenith angles");
            szaTargetTPG.setUnit("deg");
        }

        final TiePointGrid saaTargetTPG =
                ProductUtils.copyTiePointGrid("SAA", inputProduct, thirdReproProduct);
        if (saaTargetTPG != null) {
            saaTargetTPG.setName("sun_azimuth");
            saaTargetTPG.setDescription("Sun azimuth angles");
            saaTargetTPG.setUnit("deg");
            // SAA: 4th --> 3rd: if (saa <= 0.0) then saa += 360.0
            final float[] saaTiePoints = saaTargetTPG.getTiePoints();
            for (int i = 0; i < saaTiePoints.length; i++) {
                if (saaTiePoints[i] <= 0.0) {
                    saaTiePoints[i] += 360.0;
                }
            }
        }

        final TiePointGrid vzaTargetTPG =
                ProductUtils.copyTiePointGrid("OZA", inputProduct, thirdReproProduct);
        if (vzaTargetTPG != null) {
            vzaTargetTPG.setName("view_zenith");
        }

        final TiePointGrid vaaTargetTPG =
                ProductUtils.copyTiePointGrid("OAA", inputProduct, thirdReproProduct);
        if (vaaTargetTPG != null) {
            vaaTargetTPG.setName("view_azimuth");
            vaaTargetTPG.setDescription("Viewing azimuth angles");
            vaaTargetTPG.setUnit("deg");
            // OAA: 4th --> 3rd: if (oaa <= 0.0) then oaa += 360.0
            final float[] vaaTiePoints = vaaTargetTPG.getTiePoints();
            for (int i = 0; i < vaaTiePoints.length; i++) {
                if (vaaTiePoints[i] <= 0.0) {
                    vaaTiePoints[i] += 360.0;
                }
            }
        }

        final TiePointGrid zonalWindTPG =
                ProductUtils.copyTiePointGrid("horizontal_wind_vector_1", inputProduct, thirdReproProduct);
        if (zonalWindTPG != null) {
            zonalWindTPG.setName("zonal_wind");
            zonalWindTPG.setDescription("Zonal wind");
        }

        final TiePointGrid meridionalWindTPG =
                ProductUtils.copyTiePointGrid("horizontal_wind_vector_2", inputProduct, thirdReproProduct);
        if (meridionalWindTPG != null) {
            meridionalWindTPG.setName("merid_wind");
            meridionalWindTPG.setDescription("Meridional wind");
        }

        final TiePointGrid atmPressTPG =
                ProductUtils.copyTiePointGrid("sea_level_pressure", inputProduct, thirdReproProduct);
        if (atmPressTPG != null) {
            atmPressTPG.setName("atm_press");
        }

        final TiePointGrid ozoneTPG =
                ProductUtils.copyTiePointGrid("total_ozone", inputProduct, thirdReproProduct);
        if (ozoneTPG != null) {
            ozoneTPG.setName("ozone");
            ozoneTPG.setDescription("Total ozone");
            ozoneTPG.setUnit("DU");
            final double conversionFactor = 1.0 / 2.1415e-5;  // convert from kg/m2 to DU, https://sacs.aeronomie.be/info/dobson.php
            final float[] ozoneTiePoints = ozoneTPG.getTiePoints();
            for (int i = 0; i < ozoneTiePoints.length; i++) {
                ozoneTiePoints[i] *= conversionFactor;
            }
        }

        // relative humidity: take humidity_pressure_level_14 which
        //  corresponds to rel_hum in 3RP, which is at 850hPa according to MERIS Product Handbook v2.1, page 52,
        //  https://earth.esa.int/pub/ESA_DOC/ENVISAT/MERIS/meris.ProductHandbook.2_1.pdf)
        final TiePointGrid relHumTPG =
                ProductUtils.copyTiePointGrid("humidity_pressure_level_14", inputProduct, thirdReproProduct);
        if (relHumTPG != null) {
            relHumTPG.setName("rel_hum");
            relHumTPG.setDescription("Relative humidity");
        }
    }

    private void adaptBandNamesToThirdRepro(Product inputProduct, Product thirdReproProduct) {
        // ## detector_index --> detector_index
        // ## M%02d_radiance --> radiance_%d
        Band detectorIndexTargetBand =
                ProductUtils.copyBand(MERIS_DETECTOR_INDEX_DS_NAME,
                        inputProduct, thirdReproProduct, true);
        final Band detectorIndexSourceBand = inputProduct.getBand(MERIS_DETECTOR_INDEX_DS_NAME);
        detectorIndexTargetBand.setDescription(detectorIndexSourceBand.getDescription());
        detectorIndexTargetBand.setNoDataValueUsed(false);
        detectorIndexTargetBand.setNoDataValue(0.0);
        detectorIndexTargetBand.setValidPixelExpression(null);

        for (int i = 0; i < MERIS_L1B_NUM_SPECTRAL_BANDS; i++) {
            final String inputBandName = "M" + String.format("%02d", i + 1) + "_radiance";
            final String thirdReproBandName = MERIS_L1B_RADIANCE_BAND_NAME_PREFIX + "_" + (i + 1);
            final Band inputRadianceBand = inputProduct.getBand(inputBandName);
            final Band targetRadianceBand = ProductUtils.copyBand(inputBandName, inputProduct,
                    thirdReproBandName, thirdReproProduct, true);
            if (targetRadianceBand != null) {
                copyGeneralBandProperties(inputRadianceBand, targetRadianceBand);
                copySpectralBandProperties(inputRadianceBand, targetRadianceBand);

                // set solar flux:
                final Band solarFluxBand = inputProduct.getBand("solar_flux_band_" + (i + 1));
                // todo: solar flux is not needed for Idepix, but later for general adapter. Discuss how to set.
                // For the moment, compute the mean of the corresponding solar flux band image.
                // Later, implement GK suggestion
                targetRadianceBand.setSolarFlux(getMeanSolarFluxFrom4thReprocessing(solarFluxBand));

                // set valid pixel expression:
                targetRadianceBand.setValidPixelExpression("!l1_flags.INVALID");
            }
        }
    }

    private void setupQualityToL1FlagMap() {
        // quality_flags --> l1_flags
        // ##  cosmetic (2^24) --> COSMETIC (1)
        // ##  duplicated (2^23) --> DUPLICATED (2)
        // ##  sun_glint_risk (2^22) --> GLINT_RISK (4)
        // ##  dubious (2^21) --> SUSPECT (8)
        // ##  land (2^31) --> LAND_OCEAN (16)
        // ##  bright (2^27) --> BRIGHT (32)
        // ##  coastline (2^30) --> COASTLINE (64)
        // ##  invalid (2^25) --> INVALID (128)
        qualityToL1FlagMap.put(24, 1);
        qualityToL1FlagMap.put(23, 2);
        qualityToL1FlagMap.put(22, 4);
        qualityToL1FlagMap.put(21, 8);
        qualityToL1FlagMap.put(31, 16);
        qualityToL1FlagMap.put(27, 32);
        qualityToL1FlagMap.put(30, 64);
        qualityToL1FlagMap.put(25, 128);
    }

}
