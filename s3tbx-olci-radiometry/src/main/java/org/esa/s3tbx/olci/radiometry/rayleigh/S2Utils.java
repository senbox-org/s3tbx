package org.esa.s3tbx.olci.radiometry.rayleigh;

import org.esa.s3tbx.olci.radiometry.SensorConstants;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.util.ProductUtils;

import javax.media.jai.BorderExtender;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.ScaleDescriptor;
import java.awt.*;

/**
 * Utility class for S2 MSI Rayleigh Correction (i.e. methods for rescaling)
 *
 * @author olafd
 */
public class S2Utils {

    /**
     * Rescales S2 input source images to common resolution of 20m and returns a new product
     * containing the selected spectral bands and the geometry bands, all with the rescaled source images
     *
     * @param sourceProduct   - the source product
     * @param sourceBandNames - the selected input spectral S2 bands
     *
     * @param s2MsiTargetResolution
     * @return s2RescaledProduct
     */
    public static Product rescaleS2SourceImages(Product sourceProduct,
                                                String[] sourceBandNames,
                                                int s2MsiTargetResolution) {
        int s2ScaleWidth;
        int s2ScaleHeight;
        GeoCoding s2GeoCoding;
        Band referenceBand;
        switch (s2MsiTargetResolution) {
            case 10:
                referenceBand = sourceProduct.getBand(SensorConstants.S2_BANDS_TO_COPY_10[0]);
                break;
            case 20:
                referenceBand = sourceProduct.getBand(SensorConstants.S2_BANDS_TO_COPY_20[0]);
                break;
            case 60:
                referenceBand = sourceProduct.getBand(SensorConstants.S2_BANDS_TO_COPY_60[0]);
                break;
            default:
                throw new OperatorException("Target resolution " + s2MsiTargetResolution + " not supported.");
        }

        s2ScaleWidth = referenceBand.getRasterWidth();
        s2ScaleHeight = referenceBand.getRasterHeight();
        s2GeoCoding = referenceBand.getGeoCoding();

        Product s2RescaledProduct = new Product(sourceProduct.getName() + "_rayleigh", sourceProduct.getProductType(),
                                                s2ScaleWidth, s2ScaleHeight);
        s2RescaledProduct.setSceneGeoCoding(s2GeoCoding);
        s2RescaledProduct.setStartTime(sourceProduct.getStartTime());
        s2RescaledProduct.setEndTime(sourceProduct.getEndTime());

        for (String sourceBandName : sourceBandNames) {
            // B5, 6, 7, 8A are already at 20m. B10-12 are currently ignored (no RC).
            // --> remaining bands are: B1, B9 (60m); B2, B3, B4, B8 (10m)
            downscaleS2Bands(s2RescaledProduct, sourceProduct, sourceBandName, s2MsiTargetResolution);
            upscaleS2Bands(s2RescaledProduct, sourceProduct, sourceBandName, s2MsiTargetResolution);
            copyS2Bands(s2RescaledProduct, sourceProduct, sourceBandName, s2MsiTargetResolution);
        }

        for (String geomBandName : SensorConstants.S2_GEOMETRY_BANDS) {
            upscaleS2Bands(s2RescaledProduct, sourceProduct, geomBandName, s2MsiTargetResolution);
        }

        return s2RescaledProduct;
    }

    /**
     * Returns spectral band index of given S2 band.
     * Needs to consider the nasty 'B8A' special case.
     *
     * @param sourceBand - the S2 source band
     * @return spectralBandIndex
     */
    public static int getS2SpectralBandIndex(Band sourceBand) {
        // S2 spectralBandIndex:
        // bands B1..B8: spectralBandIndex = 0..7
        // band B8A: spectralBandIndex = 8
        // bands B9: spectralBandIndex = 9
        // bands B10..B12: ignored
        final String bandName = sourceBand.getName();
        if (bandName.equals("B8A")) {
            return 8;
        } else {
            final int bandNumber = Integer.parseInt(bandName.substring(1, bandName.length()));
            return bandNumber < 9 ? bandNumber - 1 : bandNumber;
        }
    }

    /**
     * Returns S2 target band name for given band category and source band name.
     * Needs to consider the nasty 'B8A' special case.
     *
     * @param bandCategory - bandCategory, e.g. "rBRR_%02d"
     * @param sourceBand   - the S2 source band
     * @return targetBandName
     */
    public static String getS2TargetBandName(String bandCategory, Band sourceBand) {
        // bandCategory e.g. "rBRR_%02d"
        final String bandCategoryPrefix = bandCategory.substring(0, bandCategory.length() - 4); // e.g. "rBRR_"

        final String bandName = sourceBand.getName();
        if (bandName.equals("B8A")) {
            return bandCategoryPrefix + "B8A";
        } else {
            final int spectralBandIndex = getS2SpectralBandIndex(sourceBand);
            if (spectralBandIndex < 9) {
                return bandCategoryPrefix + "B" + (spectralBandIndex + 1);
            } else {
                return bandCategoryPrefix + "B" + spectralBandIndex;
            }
        }
    }

    /**
     * Checks if a S2 target band name matches a given pattern
     * Needs to consider the nasty 'B8A' special case.
     *
     * @param targetBandName - the target band name
     * @param pattern        - the pattern
     * @return boolean
     */
    public static boolean targetS2BandNameMatches(String targetBandName, String pattern) {
        // pattern e.g. "rtoa_\\d{2}"
        String s2Pattern;
        if (targetBandName.indexOf("_B") == targetBandName.length() - 3) {
            s2Pattern = pattern.replace("\\d{2}", "B\\d{1}");  // e.g. rBRR_B7
        } else {
            s2Pattern = pattern.replace("\\d{2}", "B\\d{2}");  // e.g. rBRR_B12
        }
        final String patternPrefix = pattern.substring(0, pattern.length() - 5); // e.g. "rtoa_"
        if (targetBandName.endsWith("8A")) {
            return targetBandName.equals(patternPrefix + "B8A");
        } else {
            return targetBandName.matches(s2Pattern);
        }
    }

    /**
     * Provides unique source S2 band index on interval [1.13] for given target band name,
     * i.e. corrects for the duplicated index '8' for bands B8 and B8A
     *
     * @param sourceBandIndex - the original source band index
     * @param targetBandName - the target band name
     *
     * @return s2SourceBandIndex
     */
    public static int getS2SourceBandIndex(int sourceBandIndex, String targetBandName) {
        // input sourceBandIndex from RayleighCorrectionOp is [1..12], but 8 for both B8 and B8A,
        // so we map it onto [1..13]
        if (sourceBandIndex >= 9 || targetBandName.endsWith("8A")) {
            return sourceBandIndex + 1;
        } else {
            return sourceBandIndex;
        }
    }

    private static void copyS2Bands(Product s2RescaledProduct,
                                    Product sourceProduct,
                                    String sourceBandName,
                                    int s2MsiTargetResolution) {
        String[] bandsToCopy;
        switch (s2MsiTargetResolution) {
            case 10:
                bandsToCopy = SensorConstants.S2_BANDS_TO_COPY_10;
                break;
            case 20:
                bandsToCopy = SensorConstants.S2_BANDS_TO_COPY_20;
                break;
            case 60:
                bandsToCopy = SensorConstants.S2_BANDS_TO_COPY_60;
                break;
            default:
                throw new OperatorException("Target resolution " + s2MsiTargetResolution + " not supported.");
        }
        for (String bandToCopy : bandsToCopy) {
            // already target resolution
            if (sourceBandName.equals(bandToCopy)) {
                ProductUtils.copyBand(sourceBandName, sourceProduct, s2RescaledProduct, true);
                copyS2SourceBandProperties(sourceProduct.getBand(sourceBandName),
                                           s2RescaledProduct.getBand(sourceBandName));
            }
        }
    }

    private static void downscaleS2Bands(Product s2RescaledProduct,
                                         Product sourceProduct,
                                         String sourceBandName,
                                         int s2MsiTargetResolution) {

        String[] bandsToDownscale;
        int s2ScaleWidth;
        int s2ScaleHeight;
        switch (s2MsiTargetResolution) {
            case 10:
                bandsToDownscale = SensorConstants.S2_BANDS_TO_DOWNSCALE_10;
                s2ScaleWidth = sourceProduct.getBand(SensorConstants.S2_BANDS_TO_COPY_10[0]).getRasterWidth();
                s2ScaleHeight = sourceProduct.getBand(SensorConstants.S2_BANDS_TO_COPY_10[0]).getRasterHeight();
                break;
            case 20:
                bandsToDownscale = SensorConstants.S2_BANDS_TO_DOWNSCALE_20;
                s2ScaleWidth = sourceProduct.getBand(SensorConstants.S2_BANDS_TO_COPY_20[0]).getRasterWidth();
                s2ScaleHeight = sourceProduct.getBand(SensorConstants.S2_BANDS_TO_COPY_20[0]).getRasterHeight();
                break;
            case 60:
                bandsToDownscale = SensorConstants.S2_BANDS_TO_DOWNSCALE_60;
                s2ScaleWidth = sourceProduct.getBand(SensorConstants.S2_BANDS_TO_COPY_60[0]).getRasterWidth();
                s2ScaleHeight = sourceProduct.getBand(SensorConstants.S2_BANDS_TO_COPY_60[0]).getRasterHeight();
                break;
            default:
                throw new OperatorException("Target resolution " + s2MsiTargetResolution + " not supported.");
        }
        for (String bandToDownscale : bandsToDownscale) {
            if (sourceBandName.equals(bandToDownscale)) {
                Band sourceBand = sourceProduct.getBand(sourceBandName);
                final float xScale = sourceBand.getRasterWidth() * 1.0f / s2ScaleWidth;
                final float yScale = sourceBand.getRasterHeight() * 1.0f / s2ScaleHeight;
                createS2ScaledImage(s2RescaledProduct, sourceBandName, xScale, yScale, sourceBand);
            }
        }
    }

    private static void upscaleS2Bands(Product s2RescaledProduct,
                                       Product sourceProduct,
                                       String sourceBandName,
                                       int s2MsiTargetResolution) {
        String[] bandsToUpscale;
        int s2ScaleWidth;
        int s2ScaleHeight;
        switch (s2MsiTargetResolution) {
            case 10:
                bandsToUpscale = SensorConstants.S2_BANDS_TO_UPSCALE_10;
                s2ScaleWidth = sourceProduct.getBand(SensorConstants.S2_BANDS_TO_COPY_10[0]).getRasterWidth();
                s2ScaleHeight = sourceProduct.getBand(SensorConstants.S2_BANDS_TO_COPY_10[0]).getRasterHeight();
                break;
            case 20:
                bandsToUpscale = SensorConstants.S2_BANDS_TO_UPSCALE_20;
                s2ScaleWidth = sourceProduct.getBand(SensorConstants.S2_BANDS_TO_COPY_20[0]).getRasterWidth();
                s2ScaleHeight = sourceProduct.getBand(SensorConstants.S2_BANDS_TO_COPY_20[0]).getRasterHeight();
                break;
            case 60:
                bandsToUpscale = SensorConstants.S2_BANDS_TO_UPSCALE_60;
                s2ScaleWidth = sourceProduct.getBand(SensorConstants.S2_BANDS_TO_COPY_60[0]).getRasterWidth();
                s2ScaleHeight = sourceProduct.getBand(SensorConstants.S2_BANDS_TO_COPY_60[0]).getRasterHeight();
                break;
            default:
                throw new OperatorException("Target resolution " + s2MsiTargetResolution + " not supported.");
        }
        for (String bandToUpscale : bandsToUpscale) {
            if (sourceBandName.equals(bandToUpscale)) {
                Band sourceBand = sourceProduct.getBand(sourceBandName);
                final float xScale = s2ScaleWidth * 1.0f / sourceBand.getRasterWidth();
                final float yScale = s2ScaleHeight * 1.0f / sourceBand.getRasterHeight();
                createS2ScaledImage(s2RescaledProduct, sourceBandName, xScale, yScale, sourceBand);
            }
        }
    }

    private static void createS2ScaledImage(Product s2RescaledProduct, String sourceBandName, float xScale, float yScale, Band sourceBand) {
        BorderExtender extender = BorderExtender.createInstance(BorderExtender.BORDER_COPY);
        RenderingHints hints = new RenderingHints(JAI.KEY_BORDER_EXTENDER, extender);
        RenderedOp scaledImage = ScaleDescriptor.create(sourceBand.getSourceImage(),
                                                        xScale,
                                                        yScale,
                                                        0.0f, 0.0f,
//                                                        Interpolation.getInstance(Interpolation.INTERP_BILINEAR),
                                                        Interpolation.getInstance(Interpolation.INTERP_NEAREST),
                                                        hints);

        if (!s2RescaledProduct.containsBand(sourceBandName)) {
            final Band sourceBandRescaled = s2RescaledProduct.addBand(sourceBandName, sourceBand.getDataType());
            sourceBandRescaled.setSourceImage(scaledImage);
            copyS2SourceBandProperties(sourceBand, sourceBandRescaled);
        }
    }

    private static void copyS2SourceBandProperties(Band sourceBand, Band sourceBandRescaled) {
        sourceBandRescaled.setUnit(sourceBand.getUnit());
        sourceBandRescaled.setScalingFactor(sourceBand.getScalingFactor());
        sourceBandRescaled.setScalingOffset(sourceBand.getScalingOffset());
        sourceBandRescaled.setNoDataValue(sourceBand.getNoDataValue());
        sourceBandRescaled.setNoDataValueUsed(sourceBand.isNoDataValueUsed());
        sourceBandRescaled.setValidPixelExpression(sourceBand.getValidPixelExpression());
        sourceBandRescaled.setSpectralWavelength(sourceBand.getSpectralWavelength());
        sourceBandRescaled.setSpectralBandwidth(sourceBand.getSpectralBandwidth());
        sourceBandRescaled.setSpectralBandIndex(sourceBand.getSpectralBandIndex());
    }

}
