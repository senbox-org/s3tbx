package org.esa.s3tbx.olci.radiometry.rayleigh;

import org.esa.s3tbx.olci.radiometry.SensorConstants;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
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
     *
     *
     * @param sourceProduct
     * @param sourceBandNames
     * @return
     */
    public static Product rescaleS2SourceImages(Product sourceProduct, String[] sourceBandNames) {
        final int scaleWidth = sourceProduct.getBand("B5").getRasterWidth();
        final int scaleHeight = sourceProduct.getBand("B5").getRasterHeight();

        Product s2RescaledProduct = new Product(sourceProduct.getName() + "_rayleigh", sourceProduct.getProductType(),
                                        scaleWidth, scaleHeight);
        s2RescaledProduct.setSceneGeoCoding(sourceProduct.getBand("B5").getGeoCoding());    // S2 !!
        s2RescaledProduct.setStartTime(sourceProduct.getStartTime());
        s2RescaledProduct.setEndTime(sourceProduct.getEndTime());

        for (String sourceBandName : sourceBandNames) {
            // B5, 6, 7, 8A are already at 20m. B10-12 are currently ignored (no RC).
            // --> remaining bands are: B1, B9 (60m); B2, B3, B4, B8 (10m)
            downscaleS2Bands(s2RescaledProduct, sourceProduct, sourceBandName);
            upscaleS2Bands(s2RescaledProduct, sourceProduct, sourceBandName);
            copyS2Bands(s2RescaledProduct, sourceProduct, sourceBandName);
        }

        for (String geomBandName : SensorConstants.S2_GEOMETRY_BANDS) {
            upscaleS2Bands(s2RescaledProduct, sourceProduct, geomBandName);
        }

        return s2RescaledProduct;
    }

    private static void copyS2Bands(Product s2RescaledProduct, Product sourceProduct, String sourceBandName) {
        for (String bandToCopy : SensorConstants.S2_BANDS_TO_COPY) {
            // already 20m
            if (sourceBandName.equals(bandToCopy)) {
                ProductUtils.copyBand(sourceBandName, sourceProduct, s2RescaledProduct, true);
                copyS2SourceBandProperties(sourceProduct.getBand(sourceBandName),
                                           s2RescaledProduct.getBand(sourceBandName));
            }
        }
    }

    private static void downscaleS2Bands(Product s2RescaledProduct, Product sourceProduct, String sourceBandName) {
        for (String bandToDownscale : SensorConstants.S2_BANDS_TO_DOWNSCALE) {
            // 10m --> 20m
            final float xScale = 0.5f;
            final float yScale = 0.5f;
            if (sourceBandName.equals(bandToDownscale)) {
                Band sourceBand = sourceProduct.getBand(sourceBandName);
                createS2ScaledImage(s2RescaledProduct, sourceBandName, xScale, yScale, sourceBand);
            }
        }
    }

    private static void upscaleS2Bands(Product s2RescaledProduct, Product sourceProduct, String sourceBandName) {
        // 60m --> 20m; geom --> 20m
        for (String bandToUpscale : SensorConstants.S2_BANDS_TO_UPSCALE) {
            // B5 is the first 20m band
            final int s2ScaleWidth = sourceProduct.getBand("B5").getRasterWidth();
            final int s2ScaleHeight = sourceProduct.getBand("B5").getRasterHeight();
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
                                                        Interpolation.getInstance(Interpolation.INTERP_BILINEAR),
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
