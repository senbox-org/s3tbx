package org.esa.s3tbx.mph_chl;

import org.esa.snap.core.datamodel.*;
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

@OperatorMetadata(alias = "MphChlOlci",
        version = "1.0",
        internal = true,
        authors = "Mark William Matthews, Daniel Odermatt, Tom Block, Olaf Danne",
        copyright = "(c) 2013, 2014, 2017 by Brockmann Consult",
        description = "Computes maximum peak height of chlorophyll for OLCI")
public class MphChlOlciOp extends PixelOperator {

    private static final int REFL_6_IDX = 0;
    private static final int REFL_7_IDX = 1;
    private static final int REFL_8_IDX = 2;
    private static final int REFL_9_IDX = 3;
    private static final int REFL_10_IDX = 4;
    private static final int REFL_14_IDX = 5;

    private final float[] olciWvls = MphChlConstants.OLCI_WAVELENGHTS;

    @SourceProduct(alias = "Name")
    private Product sourceProduct;
    @Parameter(defaultValue = "not (quality_flags.land or quality_flags.invalid)",
            description = "Expression defining pixels considered for processing.",
            converter = BooleanExpressionConverter.class)
    private String validPixelExpression;

    @Parameter(defaultValue = "1000.0",
            description = "Maximum chlorophyll, arithmetically higher values are capped.")
    private double cyanoMaxValue;

    @Parameter(defaultValue = "350.0",
            description = "Chlorophyll threshold, above which all cyanobacteria dominated waters are 'float.")
    private double chlThreshForFloatFlag;

    @Parameter(defaultValue = "false",
            description = "Switch to true to write 'mph' band.")
    boolean exportMph;

    // package access for testing only tb 2014-03-27
    VirtualBandOpImage invalidOpImage;
    private double ratioP;
    private double ratioC;
    private double ratioB;


    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {
        if (!isSampleValid(x, y)) {
            MphChlUtils.setToInvalid(targetSamples, exportMph);
            return;
        }

        final double r_6 = sourceSamples[REFL_6_IDX].getDouble();
        final double r_7 = sourceSamples[REFL_7_IDX].getDouble();
        final double r_8 = sourceSamples[REFL_8_IDX].getDouble();
        final double r_9 = sourceSamples[REFL_9_IDX].getDouble();
        final double r_10 = sourceSamples[REFL_10_IDX].getDouble();
        final double r_14 = sourceSamples[REFL_14_IDX].getDouble();

        double maxBrr_0 = r_8;
        double maxLambda_0 = olciWvls[9];     // 681
        if (r_9 > maxBrr_0) {
            maxBrr_0 = r_9;
            maxLambda_0 = olciWvls[10];        // 709
        }

        double maxBrr_1 = maxBrr_0;
        double maxLambda_1 = maxLambda_0;
        if (r_10 > maxBrr_1) {
            maxBrr_1 = r_10;
            maxLambda_1 = olciWvls[11];      // 753
        }

        final double ndvi = (r_14 - r_7) / (r_14 + r_7);
        final double SIPF_peak = r_7 - r_6 - ((r_8 - r_6) * ratioP);
        final double SICF_peak = r_8 - r_7 - ((r_9 - r_7) * ratioC);
        final double BAIR_peak = r_9 - r_7 - ((r_14 - r_7) * ratioB);

        double mph_0 = MphChlUtils.computeMph(maxBrr_0, r_7, r_14, maxLambda_0,
                                              olciWvls[7],         // 664
                                              olciWvls[16]);     // 885
        double mph_1 = MphChlUtils.computeMph(maxBrr_1, r_7, r_14, maxLambda_1,
                                              olciWvls[7],         // 664
                                              olciWvls[16]);     // 885

        boolean floating_flag = false;
        boolean adj_flag = false;
        boolean cyano_flag = false;

        int immersed_cyano = 0;
        int floating_cyano = 0;
        int floating_vegetation = 0;

        boolean calculatePolynomial = false;
        boolean calculateExponential = false;

        if (maxLambda_1 != olciWvls[11]) {       // 753
            if (MphChlUtils.isCyano(SICF_peak, SIPF_peak, BAIR_peak)) {
                cyano_flag = true;
                calculateExponential = true;
            } else {
                calculatePolynomial = true;
            }
        } else {
            if (mph_1 >= 0.02 || ndvi >= 0.2) {
                floating_flag = true;
                adj_flag = false;
                if (MphChlUtils.isCyano(SICF_peak, SIPF_peak)) {
                    cyano_flag = true;
                    calculateExponential = true;
                } else {
                    cyano_flag = false;
                    floating_vegetation = 1;
                }
            }
            if (mph_1 < 0.02 && ndvi < 0.2) {
                floating_flag = false;
                adj_flag = true;
                cyano_flag = false;
                calculatePolynomial = true;
            }
        }

        double mph_chl = Double.NaN;
        if (calculatePolynomial) {
            mph_chl = MphChlUtils.computeChlPolynomial(mph_0);
        }

        if (calculateExponential) {
            mph_chl = MphChlUtils.computeChlExponential(mph_1);
            if (mph_chl < chlThreshForFloatFlag) {
                immersed_cyano = 1;
            } else {
                floating_flag = true;
                floating_cyano = 1;
            }
        }

        if (mph_chl > cyanoMaxValue) {
            mph_chl = cyanoMaxValue;
        }

        targetSamples[0].set(mph_chl);
        targetSamples[1].set(MphChlUtils.encodeFlags(cyano_flag, floating_flag, adj_flag));
        targetSamples[2].set(immersed_cyano);
        targetSamples[3].set(floating_cyano);
        targetSamples[4].set(floating_vegetation);
        if (exportMph) {
            targetSamples[5].set(mph_0);
        }
    }

    @Override
    protected void configureSourceSamples(SourceSampleConfigurer sampleConfigurer) throws OperatorException {
        sampleConfigurer.defineSample(0, "rBRR_07");   //  619
        sampleConfigurer.defineSample(1, "rBRR_08");    // 664
        sampleConfigurer.defineSample(2, "rBRR_10");    // 681
        sampleConfigurer.defineSample(3, "rBRR_11");    // 709
        sampleConfigurer.defineSample(4, "rBRR_12");   // 753
        sampleConfigurer.defineSample(5, "rBRR_18");   // 885
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
    protected void prepareInputs() throws OperatorException {
        ratioP = (olciWvls[7] - olciWvls[6]) / (olciWvls[9] - olciWvls[6]);
        ratioC = (olciWvls[9] - olciWvls[7]) / (olciWvls[10] - olciWvls[7]);
        ratioB = (olciWvls[10] - olciWvls[7]) / (olciWvls[17] - olciWvls[7]);

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

    private boolean isSampleValid(int x, int y) {
        return invalidOpImage == null || invalidOpImage.getData(new Rectangle(x, y, 1, 1)).getSample(x, y, 0) != 0;
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(MphChlOlciOp.class);
        }
    }
}
