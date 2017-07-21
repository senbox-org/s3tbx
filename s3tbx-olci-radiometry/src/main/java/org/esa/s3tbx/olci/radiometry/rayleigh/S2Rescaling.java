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
 * Class providing methods for specific rescaling of S2-MSI bands
 *
 * @author olafd
 */
public class S2Rescaling {

    /**
     * Rescales S2 input source images to common resolution of 20m and returns a new product
     * containing the selected spectral bands and the geometry bands, all with the rescaled source images
     * <p>
     * todo: this might become a separate operator for more general use in S2 processors,
     * as it avoids rescaling of the whole input product if only certain bands are needed.
     *
     * @param sourceProduct         - the source product
     * @param sourceBandNames       - the selected input spectral S2 bands
     * @param s2MsiTargetResolution - the target resolution (10, 20 or 60m)
     * @return s2RescaledProduct
     */
    static Product getS2ProductWithRescaledSourceBands(Product sourceProduct,
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
            downscaleS2Band(s2RescaledProduct, sourceProduct, sourceBandName, s2MsiTargetResolution);
            upscaleS2Band(s2RescaledProduct, sourceProduct, sourceBandName, s2MsiTargetResolution);
            copyS2Band(s2RescaledProduct, sourceProduct, sourceBandName, s2MsiTargetResolution);
        }

        for (String geomBandName : SensorConstants.S2_GEOMETRY_BANDS) {
            upscaleS2Band(s2RescaledProduct, sourceProduct, geomBandName, s2MsiTargetResolution);
        }

        return s2RescaledProduct;
    }

    private static void copyS2Band(Product s2RescaledProduct,
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

    private static void downscaleS2Band(Product s2RescaledProduct,
                                        Product sourceProduct,
                                        String sourceBandName,
                                        int s2MsiTargetResolution) {

        String[] bandsToRescale;
        int s2ScaleWidth;
        int s2ScaleHeight;
        switch (s2MsiTargetResolution) {
            case 10:
                bandsToRescale = SensorConstants.S2_BANDS_TO_DOWNSCALE_10;
                s2ScaleWidth = sourceProduct.getBand(SensorConstants.S2_BANDS_TO_COPY_10[0]).getRasterWidth();
                s2ScaleHeight = sourceProduct.getBand(SensorConstants.S2_BANDS_TO_COPY_10[0]).getRasterHeight();
                break;
            case 20:
                bandsToRescale = SensorConstants.S2_BANDS_TO_DOWNSCALE_20;
                s2ScaleWidth = sourceProduct.getBand(SensorConstants.S2_BANDS_TO_COPY_20[0]).getRasterWidth();
                s2ScaleHeight = sourceProduct.getBand(SensorConstants.S2_BANDS_TO_COPY_20[0]).getRasterHeight();
                break;
            case 60:
                bandsToRescale = SensorConstants.S2_BANDS_TO_DOWNSCALE_60;
                s2ScaleWidth = sourceProduct.getBand(SensorConstants.S2_BANDS_TO_COPY_60[0]).getRasterWidth();
                s2ScaleHeight = sourceProduct.getBand(SensorConstants.S2_BANDS_TO_COPY_60[0]).getRasterHeight();
                break;
            default:
                throw new OperatorException("Target resolution " + s2MsiTargetResolution + " not supported.");
        }
        setupRescaledBand(s2RescaledProduct, sourceProduct, sourceBandName, bandsToRescale, s2ScaleWidth, s2ScaleHeight);
    }

    private static void upscaleS2Band(Product s2RescaledProduct,
                                      Product sourceProduct,
                                      String sourceBandName,
                                      int s2MsiTargetResolution) {
        String[] bandsToRescale;
        int s2ScaleWidth;
        int s2ScaleHeight;
        switch (s2MsiTargetResolution) {
            case 10:
                bandsToRescale = SensorConstants.S2_BANDS_TO_UPSCALE_10;
                s2ScaleWidth = sourceProduct.getBand(SensorConstants.S2_BANDS_TO_COPY_10[0]).getRasterWidth();
                s2ScaleHeight = sourceProduct.getBand(SensorConstants.S2_BANDS_TO_COPY_10[0]).getRasterHeight();
                break;
            case 20:
                bandsToRescale = SensorConstants.S2_BANDS_TO_UPSCALE_20;
                s2ScaleWidth = sourceProduct.getBand(SensorConstants.S2_BANDS_TO_COPY_20[0]).getRasterWidth();
                s2ScaleHeight = sourceProduct.getBand(SensorConstants.S2_BANDS_TO_COPY_20[0]).getRasterHeight();
                break;
            case 60:
                bandsToRescale = SensorConstants.S2_BANDS_TO_UPSCALE_60;
                s2ScaleWidth = sourceProduct.getBand(SensorConstants.S2_BANDS_TO_COPY_60[0]).getRasterWidth();
                s2ScaleHeight = sourceProduct.getBand(SensorConstants.S2_BANDS_TO_COPY_60[0]).getRasterHeight();
                break;
            default:
                throw new OperatorException("Target resolution " + s2MsiTargetResolution + " not supported.");
        }
        setupRescaledBand(s2RescaledProduct, sourceProduct, sourceBandName, bandsToRescale, s2ScaleWidth, s2ScaleHeight);
    }

    private static void setupRescaledBand(Product s2RescaledProduct,
                                          Product sourceProduct,
                                          String sourceBandName,
                                          String[] bandsToRescale,
                                          int s2ScaleWidth,
                                          int s2ScaleHeight) {
        for (String bandToRescale : bandsToRescale) {
            if (sourceBandName.equals(bandToRescale)) {
                Band sourceBand = sourceProduct.getBand(sourceBandName);
                final float xScale = s2ScaleWidth * 1.0f / sourceBand.getRasterWidth();
                final float yScale = s2ScaleHeight * 1.0f / sourceBand.getRasterHeight();
                RenderedOp s2ScaledImage = createS2ScaledImage(xScale, yScale, sourceBand);

                if (!s2RescaledProduct.containsBand(sourceBandName)) {
                    final Band sourceBandRescaled = s2RescaledProduct.addBand(sourceBandName, sourceBand.getDataType());
                    sourceBandRescaled.setSourceImage(s2ScaledImage);
                    copyS2SourceBandProperties(sourceBand, sourceBandRescaled);
                }
            }
        }
    }

    private static RenderedOp createS2ScaledImage(float xScale, float yScale, Band sourceBand) {
        BorderExtender extender = BorderExtender.createInstance(BorderExtender.BORDER_COPY);
        RenderingHints hints = new RenderingHints(JAI.KEY_BORDER_EXTENDER, extender);
        return ScaleDescriptor.create(sourceBand.getSourceImage(),
                                      xScale,
                                      yScale,
                                      0.0f, 0.0f,
//                                      Interpolation.getInstance(Interpolation.INTERP_NEAREST),
                                      Interpolation.getInstance(Interpolation.INTERP_BILINEAR),
                                      hints);
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
