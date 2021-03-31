package org.esa.s3tbx.dos;

import com.bc.ceres.core.ProgressMonitor;

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
import org.esa.snap.core.util.converters.BooleanExpressionConverter;

import javax.media.jai.Histogram;
import java.awt.*;
import java.util.Arrays;

/**
 * Performs dark object subtraction for spectral bands in source product.
 */
@OperatorMetadata(alias = "DarkObjectSubtraction",
        version = "1.0",
        category = "Optical/Preprocessing",
        authors = "Olaf Danne, Roman Shevchuk",
        copyright = "(c) 2019 by Brockmann Consult",
        description = "Performs dark object subtraction for spectral bands in source product.")
public class DarkObjectSubtractionOp extends Operator {

    @SourceProduct(description = "Source product containing spectral bands.")
    private Product sourceProduct;

    @TargetProduct
    private Product targetProduct;

    @Parameter(label = "Bands to copy",
            description = "Bands to be copied to the target. DOS will be applied on spectral bands only.",
            rasterDataNodeType = Band.class)
    private String[] sourceBandNames;

    @Parameter(label = "Dark object search area", converter = BooleanExpressionConverter.class,
            description = "Mask expression defining search area for dark object.")
    private String maskExpression;

    @Parameter(label = "Percentile of minimum in image data", valueSet = {"0", "1", "5"},
            description = "Percentile of minimum in image data in percent " +
                    "(the number means how many percent of the image data are lower than detected minimum.")
    private int histogramMinimumPercentile;


    private final static String DARK_OBJECT_METADATA_GROUP_NAME = "Dark Object Spectral Values";

    private final static String TARGET_PRODUCT_NAME = "Dark-Object-Subtraction";
    private final static String TARGET_PRODUCT_TYPE = "dark-object-subtraction";

    private double[] darkObjectValues;

    @Override
    public void initialize() throws OperatorException {
        // validation
        if (sourceProduct.isMultiSize()) {
            throw new OperatorException("Cannot (yet) handle multi-size products. Consider resampling the product first.");
        }
        if (this.sourceBandNames == null || this.sourceBandNames.length == 0) {
            throw new OperatorException("Please select at least one source band.");
        }
        boolean spectralBandFound = false;
        for (String sourceBandName : sourceBandNames) {
            final Band band = sourceProduct.getBand(sourceBandName);
            if (band.getSpectralWavelength() > 0) {
                spectralBandFound = true;
                break;
            }
        }
        if (!spectralBandFound) {
            throw new OperatorException("Source product does not contain spectral bands. DOS cannot be applied.");
        }

        darkObjectValues = new double[sourceBandNames.length];

        // set up target product
        targetProduct = createTargetProduct();

        setTargetProduct(targetProduct);
    }

    // todo:
    // The setup with applying the DOS in doExecute with JAI imaging does not yet work reliably.
    // The reason is that we have not implemented computeTile nor computeTileStack here because it is not needed,
    // but the framework calls computeTile although it should not do so, and at the time of the call the
    // source image of the target tile might not yet be set, so we may have empty tiles in the target product.

    // The issue must be fixed in GPF. For the time being, the 'applyDarkObjectSubtraction' method is just moved into
    // initialize, which works fine. A cleaner way would be just to retrieve the DOS constants in doExecute,
    // and to implement computeTile and do the subtraction manually there.

    // todo(mp, MAR2021) - when all other todos are done, then the above should be implemented. The computation of
    // todo(mp, MAR2021) - the subtraction value can be done in the doExecute and the subtraction itself
    // todo(mp, MAR2021) - can be done in computeTile and not in the JAI images.

    @Override
    public void doExecute(ProgressMonitor pm) throws OperatorException {
        try {
            pm.beginTask("Executing dark object subtraction...", 0);
            calculateDarkObjectSubtraction(pm);
            final MetadataElement darkObjectSpectralValueMetadataElement = new MetadataElement(DARK_OBJECT_METADATA_GROUP_NAME);
            targetProduct.getMetadataRoot().addElement(darkObjectSpectralValueMetadataElement);
            for (int i = 0; i < sourceBandNames.length; i++) {
                final String sourceBandName = sourceBandNames[i];
                //Band sourceBand = sourceProduct.getBand(sourceBandName);
                final MetadataAttribute dosAttr = new MetadataAttribute(sourceBandName,
                        ProductData.createInstance(new double[]{darkObjectValues[i]}), true);
                targetProduct.getMetadataRoot().getElement(DARK_OBJECT_METADATA_GROUP_NAME).addAttribute(dosAttr);
            }
       } catch (Exception e) {
           throw new OperatorException(e.getMessage(), e);
       } finally {
            pm.done();
        }
   }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        int bandIndex =  Arrays.asList(sourceBandNames).indexOf(targetBand.getName());
        String sourceBandName = targetBand.getName();
        Band sourceBand = sourceProduct.getBand(sourceBandName);
        Rectangle targetRectangle = targetTile.getRectangle();
        double subtraction = darkObjectValues[bandIndex];
        if (sourceBand.getSpectralWavelength() > 0) {
            for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
                for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {
                    double value = sourceBand.getSampleFloat(x,y) - subtraction;
                    targetTile.setSample(x, y,value);
                }
            }
        }
    }

    static double getHistogramMinimum(Stx stx) {
        final Histogram h = stx.getHistogram();
        return h.getLowValue()[0];
    }

    static double getHistogramMaximum(Stx stx) {
        final Histogram h = stx.getHistogram();
        return h.getHighValue()[0];
    }

    static double getHistogramMinAtPercentile(Stx stx, int percentile) {
        final Histogram h = stx.getHistogram();
        final double highValue = h.getHighValue()[0];
        final double lowValue = h.getLowValue()[0];
        final int numBins = h.getNumBins(0);

        double sum = 0.0;
        for (int i = 0; i < numBins; i++) {
            final double binValue = lowValue + i * (highValue - lowValue) / (numBins - 1);
            sum += h.getBins()[0][i];
            if (sum >= percentile * h.getTotals()[0] / 100.0) {
                return binValue;
            }
        }
        return 0;
    }

    //This calculates darkObjectValues
    //todo : add this to doExecute
    private void calculateDarkObjectSubtraction(ProgressMonitor pm){
        pm.beginTask("Calculating DOS...", sourceBandNames.length);
        Mask mask = new Mask("m",0,0, Mask.BandMathsType.INSTANCE);
        if (! (maskExpression == null || maskExpression.isEmpty() )) {
            if (sourceBandNames.length > 0) {
                int width = sourceProduct.getBand(sourceBandNames[0]).getRasterWidth();
                int height = sourceProduct.getBand(sourceBandNames[0]).getRasterHeight();
                mask = new Mask("m", width, height, Mask.BandMathsType.INSTANCE);
                Mask.BandMathsType.setExpression(mask, maskExpression);
            }
        }
        for (int i = 0; i < sourceBandNames.length; i++) {
            checkForCancellation();
            final String sourceBandName = sourceBandNames[i];
            pm.setSubTaskName(String.format("Calculating DOS to band '%s'", sourceBandName));
            Band sourceBand = sourceProduct.getBand(sourceBandName);
            if (sourceBand.getSpectralWavelength() > 0) {
                Stx stx;
                if (maskExpression == null || maskExpression.isEmpty()) {
                    stx = new StxFactory().create(sourceBand, ProgressMonitor.NULL);
                } else {
                    //mask = new Mask("m", width, height, Mask.BandMathsType.INSTANCE);
                    Mask.BandMathsType.setExpression(mask, maskExpression);
                    Mask.BandMathsType.setExpression(mask, maskExpression);
                    mask.setOwner(sourceProduct);

                    stx = new StxFactory().withRoiMask(mask).create(sourceBand, ProgressMonitor.NULL);
                }
                darkObjectValues[i] = getHistogramMinAtPercentile(stx, histogramMinimumPercentile);
            }
        }
    }

    private Product createTargetProduct() {
        final int sceneWidth = sourceProduct.getSceneRasterWidth();
        final int sceneHeight = sourceProduct.getSceneRasterHeight();
        Product targetProduct = new Product(TARGET_PRODUCT_NAME, TARGET_PRODUCT_TYPE, sceneWidth, sceneHeight);
        ProductUtils.copyGeoCoding(sourceProduct, targetProduct);
        ProductUtils.copyFlagCodings(sourceProduct, targetProduct);
        ProductUtils.copyTiePointGrids(sourceProduct, targetProduct);
        ProductUtils.copyMetadata(sourceProduct, targetProduct);
        ProductUtils.copyMasks(sourceProduct, targetProduct);
        targetProduct.setStartTime(sourceProduct.getStartTime());
        targetProduct.setEndTime(sourceProduct.getEndTime());

        for (String sourceBandName : sourceBandNames) {
            Band sourceBand = sourceProduct.getBand(sourceBandName);
            if (sourceBand.getSpectralWavelength() > 0) {
                final Band targetBand = new Band(sourceBand.getName(), ProductData.TYPE_FLOAT32, sceneWidth, sceneHeight);
                targetProduct.addBand(targetBand);
                ProductUtils.copySpectralBandProperties(sourceBand, targetBand);
                ProductUtils.copyGeoCoding(sourceBand, targetBand);
                targetBand.setDescription(sourceBand.getDescription());
                targetBand.setUnit(sourceBand.getUnit());
                targetBand.setNoDataValueUsed(sourceBand.isNoDataValueUsed());
                targetBand.setNoDataValue(sourceBand.getGeophysicalNoDataValue());
                targetBand.setValidPixelExpression(sourceBand.getValidPixelExpression());

            } else {
                ProductUtils.copyBand(sourceBand.getName(), sourceProduct, targetProduct, true);
            }
        }
        return targetProduct;
    }

    public static class Spi extends OperatorSpi {
        public Spi() {
            super(DarkObjectSubtractionOp.class);
        }
    }
}
