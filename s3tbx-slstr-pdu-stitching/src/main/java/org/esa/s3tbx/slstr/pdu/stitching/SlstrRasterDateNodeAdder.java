package org.esa.s3tbx.slstr.pdu.stitching;

import org.esa.snap.core.datamodel.*;
import org.esa.s3tbx.dataio.s3.util.ColorProvider;

import java.util.Arrays;

public class SlstrRasterDateNodeAdder {

    static void addRasterDataNodes(Product product, String fileName, ImageSize imageSize, ColorProvider colorProvider,
                                   ImageSize referenceImageSize, String referenceEnding) {
        String[] splitFileName = fileName.replace(".nc", "").split("_");
        if (splitFileName.length < 2) {
            return;
        }
        String start = splitFileName[0];
        String mainPart = splitFileName[splitFileName.length - 2];
        String ending = splitFileName[splitFileName.length - 1];
        String[] rasterDataNodeNames = getRasterDataNodeNames(start, mainPart, ending);
        int[] dataTypes = getDataTypes(mainPart, ending);
        short[] referenceResolutions = getResolutions(referenceEnding);
        for (int i = 0; i < rasterDataNodeNames.length; i++) {
            String rasterDataNodeName = rasterDataNodeNames[i];
            if (ending.startsWith("t")) {
                short[] sourceResolutions = getResolutions(ending);
                double subSamplingX = sourceResolutions[0] / referenceResolutions[0];
                double subSamplingY = sourceResolutions[1] / referenceResolutions[1];
                double offsetX = referenceImageSize.getTrackOffset() - imageSize.getTrackOffset() * subSamplingX;
                double offsetY = subSamplingY * imageSize.getStartOffset() - referenceImageSize.getStartOffset();
                double newOffsetX = offsetX % subSamplingX;
                int newWidth = (int) Math.ceil((referenceImageSize.getColumns() - newOffsetX) / subSamplingX);
                double newOffsetY = offsetY % subSamplingY;
                int newHeight = (int) Math.ceil((referenceImageSize.getRows() - newOffsetY) / subSamplingY);
                TiePointGrid tiePointGrid = new TiePointGrid(rasterDataNodeName, newWidth, newHeight,
                        newOffsetX, newOffsetY, subSamplingX, subSamplingY);
                product.addTiePointGrid(tiePointGrid);
            } else {
                Band band = new Band(rasterDataNodeName, dataTypes[i],
                        imageSize.getColumns(), imageSize.getRows());
                product.addBand(band);
                FlagCoding flagCoding = getFlagCoding(rasterDataNodeName);
                if (flagCoding != null) {
                    band.setSampleCoding(flagCoding);
                    product.getFlagCodingGroup().add(flagCoding);
                    for (String flagName : flagCoding.getFlagNames()) {
                        String maskName = String.format("%s_%s", rasterDataNodeName, flagName);
                        String expression = String.format("%s.%s", rasterDataNodeName, flagName);
                        product.addMask(maskName, expression, expression, colorProvider.getMaskColor(maskName), 0.5);
                    }
                }
            }

        }

    }

    private static short[] getResolutions(String gridIndex) {
        short[] resolutions;
        if (gridIndex.startsWith("i") || gridIndex.startsWith("f")) {
            resolutions = new short[]{1000, 1000};
        } else if (gridIndex.startsWith("t")) {
            resolutions = new short[]{16000, 1000};
        } else {
            resolutions = new short[]{500, 500};
        }
        return resolutions;
    }

    private static FlagCoding getFlagCoding(String bandName) {
        if (bandName.contains("exception")) {
            return createExceptionFlagCoding(bandName);
        } else if (bandName.contains("bayes")) {
            return createBayesFlagCoding(bandName);
        } else if (bandName.contains("cloud")) {
            return createCloudFlagCoding(bandName);
        } else if (bandName.contains("confidence")) {
            return createConfidenceFlagCoding(bandName);
        } else if (bandName.contains("pointing")) {
            return createPointingFlagCoding(bandName);
        }
        return null;
    }

    private static FlagCoding createExceptionFlagCoding(String bandName) {
        FlagCoding exceptionFlagcoding = new FlagCoding(bandName);
        exceptionFlagcoding.addFlag("ISP_absent", 1, "");
        exceptionFlagcoding.addFlag("pixel_absent", 2, "");
        exceptionFlagcoding.addFlag("not_decompressed", 4, "");
        exceptionFlagcoding.addFlag("no_signal", 8, "");
        exceptionFlagcoding.addFlag("saturation", 16, "");
        exceptionFlagcoding.addFlag("invalid_radiance", 32, "");
        exceptionFlagcoding.addFlag("no_parameters", 64, "");
        exceptionFlagcoding.addFlag("unfilled_pixel", 128, "");
        return exceptionFlagcoding;
    }

    private static FlagCoding createBayesFlagCoding(String bandName) {
        FlagCoding exceptionFlagcoding = new FlagCoding(bandName);
        exceptionFlagcoding.addFlag("single_low", 1, "");
        exceptionFlagcoding.addFlag("single_moderate", 2, "");
        exceptionFlagcoding.addFlag("dual_low", 4, "");
        exceptionFlagcoding.addFlag("dual_moderate", 8, "");
        exceptionFlagcoding.addFlag("spare_1", 16, "");
        exceptionFlagcoding.addFlag("spare_2", 32, "");
        exceptionFlagcoding.addFlag("spare_3", 64, "");
        exceptionFlagcoding.addFlag("spare_4", 128, "");
        return exceptionFlagcoding;
    }

    private static FlagCoding createCloudFlagCoding(String bandName) {
        FlagCoding exceptionFlagcoding = new FlagCoding(bandName);
        exceptionFlagcoding.addFlag("visible", 1, "");
        exceptionFlagcoding.addFlag("1_37_threshold", 2, "");
        exceptionFlagcoding.addFlag("1_6_small_histogram", 4, "");
        exceptionFlagcoding.addFlag("1_6_large_histogram", 8, "");
        exceptionFlagcoding.addFlag("2_25_small_histogram", 16, "");
        exceptionFlagcoding.addFlag("2_25_large_histogram", 32, "");
        exceptionFlagcoding.addFlag("11_spatial_coherence", 64, "");
        exceptionFlagcoding.addFlag("gross_cloud", 128, "");
        exceptionFlagcoding.addFlag("thin_cirrus", 256, "");
        exceptionFlagcoding.addFlag("medium_high", 512, "");
        exceptionFlagcoding.addFlag("fog_low_stratus", 1024, "");
        exceptionFlagcoding.addFlag("11_12_view_difference", 2048, "");
        exceptionFlagcoding.addFlag("3_7_11_view_difference", 4096, "");
        exceptionFlagcoding.addFlag("thermal_histogram", 8192, "");
        exceptionFlagcoding.addFlag("spare_1", 16384, "");
        exceptionFlagcoding.addFlag("spare_2", 32768, "");
        return exceptionFlagcoding;
    }

    private static FlagCoding createConfidenceFlagCoding(String bandName) {
        FlagCoding exceptionFlagcoding = new FlagCoding(bandName);
        exceptionFlagcoding.addFlag("coastline", 1, "");
        exceptionFlagcoding.addFlag("ocean", 2, "");
        exceptionFlagcoding.addFlag("tidal", 4, "");
        exceptionFlagcoding.addFlag("land", 8, "");
        exceptionFlagcoding.addFlag("inland_water", 16, "");
        exceptionFlagcoding.addFlag("unfilled", 32, "");
        exceptionFlagcoding.addFlag("spare_1", 64, "");
        exceptionFlagcoding.addFlag("spare_2", 128, "");
        exceptionFlagcoding.addFlag("cosmetic", 256, "");
        exceptionFlagcoding.addFlag("duplicate", 512, "");
        exceptionFlagcoding.addFlag("day", 1024, "");
        exceptionFlagcoding.addFlag("twilight", 2048, "");
        exceptionFlagcoding.addFlag("sun_glint", 4096, "");
        exceptionFlagcoding.addFlag("snow", 8192, "");
        exceptionFlagcoding.addFlag("summary_cloud", 16384, "");
        exceptionFlagcoding.addFlag("summary_pointing", 32768, "");
        return exceptionFlagcoding;
    }

    private static FlagCoding createPointingFlagCoding(String bandName) {
        FlagCoding exceptionFlagcoding = new FlagCoding(bandName);
        exceptionFlagcoding.addFlag("FlipMirrorAbsoluteError", 1, "");
        exceptionFlagcoding.addFlag("FlipMirrorIntegratedError", 2, "");
        exceptionFlagcoding.addFlag("FlipMirrorRMSError", 4, "");
        exceptionFlagcoding.addFlag("ScanMirrorAbsoluteError", 8, "");
        exceptionFlagcoding.addFlag("ScanMirrorIntegratedError", 16, "");
        exceptionFlagcoding.addFlag("ScanMirrorRMSError", 32, "");
        exceptionFlagcoding.addFlag("ScanTimeError", 64, "");
        exceptionFlagcoding.addFlag("Platform_Mode", 128, "");
        return exceptionFlagcoding;
    }

    private static String[] getRasterDataNodeNames(String start, String mainPart, String ending) {
        switch (mainPart) {
            case "radiance": {
                return getRadianceBandNames(start, ending);
            }
            case "BT": {
                return getBtBandNames(start, ending);
            }
            case "geometry": {
                return getGeometryBandNames(ending);
            }
            case "geodetic": {
                return getGeodeticBandNames(ending);
            }
            case "cartesian": {
                return getCartesianBandNames(ending);
            }
            case "indices": {
                return getIndicesBandNames(ending);
            }
            case "flags": {
                return getFlagBandNames(ending);
            }
            case "met": {
                return getMetTxNames();
            }
        }
        return new String[0];
    }

    private static int[] getDataTypes(String mainPart, String ending) {
        switch (mainPart) {
            case "radiance": {
                return getRadianceDataTypes();
            }
            case "BT": {
                return getBtDataTypes();
            }
            case "geometry": {
                return getGeometryDataTypes();
            }
            case "geodetic": {
                return getGeodeticDataTypes(ending);
            }
            case "cartesian": {
                return getCartesianDataTypes(ending);
            }
            case "indices": {
                return getIndicesDataTypes();
            }
            case "flags": {
                return getFlagDataTypes();
            }
            case "met": {
                return getMetTxDataTypes();
            }
        }
        return new int[0];
    }

    private static String[] getCartesianBandNames(String ending) {
        return new String[]{String.format("x_%s", ending), String.format("y_%s", ending)};
    }

    private static int[] getCartesianDataTypes(String ending) {
        if (ending.equals("tx")) {
            return new int[]{ProductData.TYPE_FLOAT64, ProductData.TYPE_FLOAT64};
        }
        return new int[]{ProductData.TYPE_INT32, ProductData.TYPE_INT32};
    }

    private static String[] getFlagBandNames(String ending) {
        return new String[]{
                String.format("bayes_%s", ending),
                String.format("cloud_%s", ending),
                String.format("confidence_%s", ending),
                String.format("pointing_%s", ending)};
    }

    private static int[] getFlagDataTypes() {
        return new int[]{ProductData.TYPE_UINT8, ProductData.TYPE_UINT16, ProductData.TYPE_UINT16, ProductData.TYPE_UINT8};
    }

    private static String[] getGeodeticBandNames(String ending) {
        if (ending.equals("tx")) {
            return new String[]{"latitude_tx", "longitude_tx"};
        }
        return new String[]{
                String.format("elevation_%s", ending),
                String.format("latitude_%s", ending),
                String.format("longitude_%s", ending)};
    }

    private static int[] getGeodeticDataTypes(String ending) {
        if (ending.equals("tx")) {
            return new int[]{ProductData.TYPE_FLOAT64, ProductData.TYPE_FLOAT64};
        }
        return new int[]{ProductData.TYPE_INT16, ProductData.TYPE_INT32, ProductData.TYPE_INT32};
    }


    private static String[] getIndicesBandNames(String ending) {
        return new String[]{
                String.format("detector_%s", ending),
                String.format("pixel_%s", ending),
                String.format("scan_%s", ending)};
    }

    private static int[] getIndicesDataTypes() {
        return new int[]{ProductData.TYPE_UINT8, ProductData.TYPE_UINT16, ProductData.TYPE_UINT16};
    }

    private static String[] getMetTxNames() {
        String[] metTxNames = new String[101];
        int index = 0;
        for (int i = 1; i <= 25; i++) {
            metTxNames[index++] = String.format("specificic_humidity_tx_pressure_level_%d_tx", i);
            metTxNames[index++] = String.format("temperature_profile_tx_pressure_level_%d_tx", i);
        }
        for (int i = 1; i <= 5; i++) {
            metTxNames[index++] = String.format("east_west_stress_tx_time_%d_tx", i);
            metTxNames[index++] = String.format("latent_heat_tx_time_%d_tx", i);
            metTxNames[index++] = String.format("north_south_tx_time_%d_tx", i);
            metTxNames[index++] = String.format("sensible_heat_tx_time_%d_tx", i);
            metTxNames[index++] = String.format("solar_radiation_tx_time_%d_tx", i);
            metTxNames[index++] = String.format("thermal_radiation_tx_time_%d_tx", i);
            metTxNames[index++] = String.format("u_wind_tx_time_%d_tx", i);
            metTxNames[index++] = String.format("v_wind_tx_time_%d_tx", i);
        }
        metTxNames[index++] = "cloud_fraction_tx";
        metTxNames[index++] = "dew_point_tx";
        metTxNames[index++] = "sea_ice_fraction_tx";
        metTxNames[index++] = "sea_surface_temperature_tx";
        metTxNames[index++] = "skin_temperature_tx";
        metTxNames[index++] = "snow_depth_tx";
        metTxNames[index++] = "soil_wetness_tx";
        metTxNames[index++] = "surface_pressure_tx";
        metTxNames[index++] = "temperature_tx";
        metTxNames[index++] = "total_column_ozone_tx";
        metTxNames[index] = "total_column_water_vapour_tx";
        return metTxNames;
    }

    private static int[] getMetTxDataTypes() {
        int[] metTxDataTypes = new int[101];
        Arrays.fill(metTxDataTypes, ProductData.TYPE_FLOAT32);
        return metTxDataTypes;
    }

    private static String[] getBtBandNames(String start, String ending) {
        return new String[]{
                String.format("%s_BT_%s", start, ending),
                String.format("%s_BT_exception_%s", start, ending)
        };
    }

    private static int[] getBtDataTypes() {
        return new int[]{ProductData.TYPE_INT16, ProductData.TYPE_UINT8};
    }

    private static String[] getGeometryBandNames(String ending) {
        return new String[]{
                String.format("sat_azimuth_%s", ending),
                String.format("sat_path_%s", ending),
                String.format("sat_zenith_%s", ending),
                String.format("solar_azimuth_%s", ending),
                String.format("solar_path_%s", ending),
                String.format("solar_zenith_%s", ending)
        };
    }

    private static int[] getGeometryDataTypes() {
        return new int[]{ProductData.TYPE_FLOAT64, ProductData.TYPE_FLOAT64};
    }

    private static String[] getRadianceBandNames(String start, String ending) {
        return new String[]{
                String.format("%s_radiance_%s", start, ending),
                String.format("%s_exception_%s", start, ending)
        };
    }

    private static int[] getRadianceDataTypes() {
        return new int[]{ProductData.TYPE_INT16, ProductData.TYPE_UINT8};
    }

}
