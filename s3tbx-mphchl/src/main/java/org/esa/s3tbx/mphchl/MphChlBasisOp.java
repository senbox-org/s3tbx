package org.esa.s3tbx.mphchl;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.pointop.*;
import org.esa.snap.core.image.ResolutionLevel;
import org.esa.snap.core.image.VirtualBandOpImage;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.converters.BooleanExpressionConverter;

import java.awt.*;

@OperatorMetadata(alias = "MphChlBasis",
        version = "1.0",
        internal = true,
        authors = "Mark William Matthews, Daniel Odermatt, Tom Block, Olaf Danne",
        copyright = "(c) 2013, 2014, 2017 by Brockmann Consult",
        description = "Computes maximum peak height of chlorophyll. Basis class, contains sensor-independent parts.")
public class MphChlBasisOp extends PixelOperator {

    @SourceProduct(alias = "Name")
    Product sourceProduct;


    @Parameter(description = "Expression defining pixels considered for processing.",
            converter = BooleanExpressionConverter.class)
    String validPixelExpression;

    @Parameter(defaultValue = "1000.0",
            description = "Maximum chlorophyll, arithmetically higher values are capped.")
    double cyanoMaxValue;

    @Parameter(defaultValue = "350.0",
            description = "Chlorophyll threshold, above which all cyanobacteria dominated waters are 'float.")
    double chlThreshForFloatFlag;

    @Parameter(defaultValue = "false",
            description = "Switch to true to write 'mph' band.")
    boolean exportMph;


    float[] sensorWvls;

    VirtualBandOpImage invalidOpImage;

    double ratioP;
    double ratioC;
    double ratioB;


    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {
    }

    @Override
    protected void configureTargetSamples(TargetSampleConfigurer sampleConfigurer) throws OperatorException {
        sampleConfigurer.defineSample(0, "chl");
        sampleConfigurer.defineSample(1, "mph_chl_flags");
        sampleConfigurer.defineSample(2, "immersed_cyanobacteria");
        sampleConfigurer.defineSample(3, "floating_cyanobacteria");
        sampleConfigurer.defineSample(4, "floating_vegetation");
        if (exportMph) {
            sampleConfigurer.defineSample(5, "mph");
        }
    }

    @Override
    protected void configureTargetProduct(ProductConfigurer productConfigurer) {
        final Band chlBand = productConfigurer.addBand("chl", ProductData.TYPE_FLOAT32);
        chlBand.setUnit("mg/m^3");
        chlBand.setGeophysicalNoDataValue(Double.NaN);
        chlBand.setNoDataValue(Double.NaN);
        chlBand.setNoDataValueUsed(true);

        final Band immersedCyanoBand = productConfigurer.addBand("immersed_cyanobacteria", ProductData.TYPE_INT8);
        immersedCyanoBand.setNoDataValue(0);
        immersedCyanoBand.setNoDataValueUsed(true);
        productConfigurer.addBand("floating_cyanobacteria", ProductData.TYPE_INT8);
        productConfigurer.addBand("floating_vegetation", ProductData.TYPE_INT8);

        if (exportMph) {
            final Band mphBand = productConfigurer.addBand("mph", ProductData.TYPE_FLOAT32);
            mphBand.setUnit("dl");
            mphBand.setGeophysicalNoDataValue(Double.NaN);
            mphBand.setNoDataValue(Double.NaN);
            mphBand.setNoDataValueUsed(true);
        }
        final Band flagBand = productConfigurer.addBand("mph_chl_flags", ProductData.TYPE_INT8);

        super.configureTargetProduct(productConfigurer);

        final Product targetProduct = productConfigurer.getTargetProduct();
        final FlagCoding flagCoding = MphChlUtils.createMphChlFlagCoding("mph_chl_flags");
        targetProduct.getFlagCodingGroup().add(flagCoding);
        flagBand.setSampleCoding(flagCoding);

        MphChlUtils.setupMphChlBitmask(targetProduct);

        final Product sourceProduct = productConfigurer.getSourceProduct();
        ProductUtils.copyFlagBands(sourceProduct, targetProduct, true);
    }

    @Override
    protected void configureSourceSamples(SourceSampleConfigurer sampleConfigurer) throws OperatorException {
    }

    @Override
    protected void prepareInputs() throws OperatorException {

        if (StringUtils.isNullOrEmpty(validPixelExpression)) {
            return;
        }

        if (!sourceProduct.isCompatibleBandArithmeticExpression(validPixelExpression)) {
            final String message = String.format("The given expression '%s' is not compatible with the source product.", validPixelExpression);
            throw new OperatorException(message);
        }

        invalidOpImage = VirtualBandOpImage.builder(validPixelExpression, sourceProduct)
                .dataType(ProductData.TYPE_FLOAT32)
                .fillValue(0.0f)
                .tileSize(sourceProduct.getPreferredTileSize())
                .mask(false)
                .level(ResolutionLevel.MAXRES)
                .create();
    }

    boolean isSampleValid(int x, int y) {
        return invalidOpImage == null || invalidOpImage.getData(new Rectangle(x, y, 1, 1)).getSample(x, y, 0) != 0;
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(MphChlBasisOp.class);
        }
    }
}
