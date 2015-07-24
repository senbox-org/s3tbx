package org.esa.s3tbx.c2rcc.meris;

import org.esa.snap.framework.datamodel.Band;
import org.esa.snap.framework.datamodel.FlagCoding;
import org.esa.snap.framework.datamodel.GeoPos;
import org.esa.snap.framework.datamodel.MetadataAttribute;
import org.esa.snap.framework.datamodel.PixelPos;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.datamodel.ProductData;
import org.esa.snap.framework.gpf.OperatorException;
import org.esa.snap.framework.gpf.OperatorSpi;
import org.esa.snap.framework.gpf.annotations.OperatorMetadata;
import org.esa.snap.framework.gpf.annotations.Parameter;
import org.esa.snap.framework.gpf.annotations.SourceProduct;
import org.esa.snap.framework.gpf.pointop.PixelOperator;
import org.esa.snap.framework.gpf.pointop.ProductConfigurer;
import org.esa.snap.framework.gpf.pointop.Sample;
import org.esa.snap.framework.gpf.pointop.SourceSampleConfigurer;
import org.esa.snap.framework.gpf.pointop.TargetSampleConfigurer;
import org.esa.snap.framework.gpf.pointop.WritableSample;
import org.esa.snap.util.ProductUtils;
import org.esa.snap.util.converters.BooleanExpressionConverter;

import java.awt.Color;
import java.io.IOException;

import static org.esa.s3tbx.c2rcc.meris.C2rccMerisAlgorithm.merband12_ix;

// todo (nf) - Add Thullier solar fluxes as default values to C2R-CC operator (https://github.com/bcdev/s3tbx-c2rcc/issues/1)
// todo (nf) - Add flags band and check for OOR of inputs and outputs of the NNs (https://github.com/bcdev/s3tbx-c2rcc/issues/2)
// todo (nf) - Add min/max values of NN inputs and outputs to metadata (https://github.com/bcdev/s3tbx-c2rcc/issues/3)

/**
 * The Case 2 Regional / CoastColour Operator for MERIS.
 * <p>
 * Computes AC-reflectances and IOPs from MERIS L1b data products using
 * an neural-network approach.
 *
 * @author Norman Fomferra
 */
@OperatorMetadata(alias = "meris.c2rcc", version = "0.5",
        authors = "Roland Doerffer, Norman Fomferra (Brockmann Consult)",
        category = "Optical Processing/Thematic Water Processing",
        copyright = "Copyright (C) 2015 by Brockmann Consult",
        description = "Performs atmospheric correction and IOP retrieval on MERIS L1b data products.")
public class C2rccMerisOperator extends PixelOperator {

    // MERIS sources
    public static final int BAND_COUNT = 15;
    public static final int DEM_ALT_IX = BAND_COUNT;
    public static final int SUN_ZEN_IX = BAND_COUNT + 1;
    public static final int SUN_AZI_IX = BAND_COUNT + 2;
    public static final int VIEW_ZEN_IX = BAND_COUNT + 3;
    public static final int VIEW_AZI_IX = BAND_COUNT + 4;
    public static final int ATM_PRESS_IX = BAND_COUNT + 5;
    public static final int OZONE_IX = BAND_COUNT + 6;

    // MERIS targets
    public static final int REFLEC_N = merband12_ix.length;

    public static final int REFLEC_1_IX = 0;
    public static final int CONC_APIG_IX = REFLEC_N;
    public static final int CONC_ADET_IX = REFLEC_N + 1;
    public static final int CONC_AGELB_IX = REFLEC_N + 2;
    public static final int CONC_BPART_IX = REFLEC_N + 3;
    public static final int CONC_BWIT_IX = REFLEC_N + 4;

    public static final int RTOSA_RATIO_MIN_IX = REFLEC_N + 5;
    public static final int RTOSA_RATIO_MAX_IX = REFLEC_N + 6;
    public static final int L2_FLAGS_IX = REFLEC_N + 7;

    public static final int RTOSA_IN_1_IX = REFLEC_N + 8;
    public static final int RTOSA_OUT_1_IX = RTOSA_IN_1_IX + REFLEC_N;

    @SourceProduct(label = "MERIS L1b product",
            description = "MERIS L1b source product.")
    private Product sourceProduct;

    @Parameter(label = "Valid-pixel expression",
            defaultValue = "!l1_flags.INVALID && !l1_flags.LAND_OCEAN",
            converter = BooleanExpressionConverter.class)
    private String validPixelExpression;

    @Parameter(defaultValue = "35.0", unit = "DU", interval = "(0, 100)")
    private double salinity;

    @Parameter(defaultValue = "15.0", unit = "C", interval = "(-50, 50)")
    private double temperature;

    @Parameter(defaultValue = "false")
    private boolean useDefaultSolarFlux;

    @Parameter(defaultValue = "false", label = "Output top-of-standard-atmosphere (TOSA) reflectances")
    private boolean outputRtosa;

    private C2rccMerisAlgorithm algorithm;

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {

        double[] radiances = new double[BAND_COUNT];
        for (int i = 0; i < BAND_COUNT; i++) {
            radiances[i] = sourceSamples[i].getDouble();
        }

        GeoPos geoPos = sourceProduct.getGeoCoding().getGeoPos(new PixelPos(x + 0.5f, y + 0.5f), null);
        C2rccMerisAlgorithm.Result result = algorithm.processPixel(x, y, geoPos.getLat(), geoPos.getLon(),
                                                                   radiances,
                                                                   sourceSamples[SUN_ZEN_IX].getDouble(),
                                                                   sourceSamples[SUN_AZI_IX].getDouble(),
                                                                   sourceSamples[VIEW_ZEN_IX].getDouble(),
                                                                   sourceSamples[VIEW_AZI_IX].getDouble(),
                                                                   sourceSamples[DEM_ALT_IX].getDouble(),
                                                                   sourceSamples[ATM_PRESS_IX].getDouble(),
                                                                   sourceSamples[OZONE_IX].getDouble());

        for (int i = 0; i < result.rw.length; i++) {
            targetSamples[i].set(result.rw[i]);
        }

        for (int i = 0; i < result.iops.length; i++) {
            targetSamples[result.rw.length + i].set(result.iops[i]);
        }

        targetSamples[RTOSA_RATIO_MIN_IX].set(result.rtosa_ratio_min);
        targetSamples[RTOSA_RATIO_MAX_IX].set(result.rtosa_ratio_max);
        targetSamples[L2_FLAGS_IX].set(result.flags);

        if (outputRtosa) {
            for (int i = 0; i < result.rtosa_in.length; i++) {
                targetSamples[RTOSA_IN_1_IX + i].set(result.rtosa_in[i]);
            }
            for (int i = 0; i < result.rtosa_out.length; i++) {
                targetSamples[RTOSA_OUT_1_IX + i].set(result.rtosa_out[i]);
            }
        }
    }

    @Override
    protected void configureSourceSamples(SourceSampleConfigurer sc) throws OperatorException {
        sc.setValidPixelMask(validPixelExpression);
        for (int i = 0; i < BAND_COUNT; i++) {
            sc.defineSample(i, "radiance_" + (i + 1));
        }
        sc.defineSample(DEM_ALT_IX, "dem_alt");
        sc.defineSample(SUN_ZEN_IX, "sun_zenith");
        sc.defineSample(SUN_AZI_IX, "sun_azimuth");
        sc.defineSample(VIEW_ZEN_IX, "view_zenith");
        sc.defineSample(VIEW_AZI_IX, "view_azimuth");
        sc.defineSample(ATM_PRESS_IX, "atm_press");
        sc.defineSample(OZONE_IX, "ozone");
    }

    @Override
    protected void configureTargetSamples(TargetSampleConfigurer sc) throws OperatorException {

        for (int i = 0; i < merband12_ix.length; i++) {
            int bi = merband12_ix[i];
            sc.defineSample(REFLEC_1_IX + i, "reflec_" + bi);
        }

        sc.defineSample(CONC_APIG_IX, "conc_apig");
        sc.defineSample(CONC_ADET_IX, "conc_adet");
        sc.defineSample(CONC_AGELB_IX, "conc_agelb");
        sc.defineSample(CONC_BPART_IX, "conc_bpart");
        sc.defineSample(CONC_BWIT_IX, "conc_bwit");
        sc.defineSample(RTOSA_RATIO_MIN_IX, "rtosa_ratio_min");
        sc.defineSample(RTOSA_RATIO_MAX_IX, "rtosa_ratio_max");
        sc.defineSample(L2_FLAGS_IX, "l2_flags");

        if (outputRtosa) {
            for (int i = 0; i < merband12_ix.length; i++) {
                int bi = merband12_ix[i];
                sc.defineSample(RTOSA_IN_1_IX + i, "rtosa_in_" + bi);
            }
            for (int i = 0; i < merband12_ix.length; i++) {
                int bi = merband12_ix[i];
                sc.defineSample(RTOSA_OUT_1_IX + i, "rtosa_out_" + bi);
            }
        }
    }

    @Override
    protected void configureTargetProduct(ProductConfigurer productConfigurer) {
        super.configureTargetProduct(productConfigurer);
        productConfigurer.copyMetadata();

        Product targetProduct = productConfigurer.getTargetProduct();

        ProductUtils.copyFlagBands(sourceProduct, targetProduct, true);

        for (int index : merband12_ix) {
            Band reflecBand = targetProduct.addBand("reflec_" + index, ProductData.TYPE_FLOAT32);
            ProductUtils.copySpectralBandProperties(sourceProduct.getBand("radiance_" + index), reflecBand);
            reflecBand.setUnit("1");
        }

        //output  1 is log_conc_apig in [-13.170000,1.671000]
        //output  2 is log_conc_adet in [-9.903000,1.782000]
        //output  3 is log_conc_agelb in [-9.903000,0.000000]
        //output  4 is log_conc_bpart in [-6.908000,4.081000]
        //output  5 is log_conc_bwit in [-6.908000,4.076000]
        addBand(targetProduct, "conc_apig", "m^-1", "Pigment absorption coefficient");
        addBand(targetProduct, "conc_adet", "m^-1", "Pigment absorption");
        addBand(targetProduct, "conc_agelb", "m^-1", "Yellow substance absorption coefficient");
        addBand(targetProduct, "conc_bpart", "m^-1", "");
        addBand(targetProduct, "conc_bwit", "m^-1", "Backscattering of suspended particulate matter");

        addVirtualBand(targetProduct, "tsm", "(conc_bpart + conc_bwit) * 1.7", "g m^-3", "Total suspended matter dry weight concentration");
        addVirtualBand(targetProduct, "atot", "conc_apig + conc_adet + conc_agelb", "m^-1", "Total absorption coefficient of all water constituents");
        addVirtualBand(targetProduct, "chl", "pow(conc_apig, 1.04) * 20.0", "m^-1", "Chlorophyll concentration");

        addBand(targetProduct, "rtosa_ratio_min", "1", "Minimum of rtosa_out:rtosa_in ratios");
        addBand(targetProduct, "rtosa_ratio_max", "1", "Maximum of rtosa_out:rtosa_in ratios");
        Band l2_flags = targetProduct.addBand("l2_flags", ProductData.TYPE_UINT32);
        l2_flags.setDescription("Quality flags");

        FlagCoding flagCoding = new FlagCoding("l2_flags");
        flagCoding.addFlag("AC_NN_IN_ALIEN", 0x01, "The input spectrum to atmospheric correction neural net was unknown");
        flagCoding.addFlag("AC_NN_IN_OOR", 0x02, "One of the inputs to the atmospheric correction neural net was out of range");
        flagCoding.addFlag("IOP_NN_IN_OOR", 0x04, "One of the inputs to the IOP retrieval neural net was out of range");
        targetProduct.getFlagCodingGroup().add(flagCoding);
        l2_flags.setSampleCoding(flagCoding);

        Color[] maskColors = {Color.RED, Color.ORANGE, Color.YELLOW, Color.BLUE, Color.GREEN, Color.PINK, Color.MAGENTA, Color.CYAN};
        String[] flagNames = flagCoding.getFlagNames();
        for (int i = 0; i < flagNames.length; i++) {
            String flagName = flagNames[i];
            MetadataAttribute flag = flagCoding.getFlag(flagName);
            targetProduct.addMask(flagName, "l2_flags." + flagName, flag.getDescription(), maskColors[i % maskColors.length], 0.5);
        }

        if (outputRtosa) {
            for (int bi : merband12_ix) {
                Band rtosaInBand = addBand(targetProduct, "rtosa_in_" + bi, "1", "Top-of-standard-atmosphere reflectances, input to AC");
                ProductUtils.copySpectralBandProperties(sourceProduct.getBand("radiance_" + bi), rtosaInBand);
            }
            for (int bi : merband12_ix) {
                Band rtosaOutBand = addBand(targetProduct, "rtosa_out_" + bi, "1", "Top-of-standard-atmosphere reflectances, output from ANN");
                ProductUtils.copySpectralBandProperties(sourceProduct.getBand("radiance_" + bi), rtosaOutBand);
            }
            targetProduct.setAutoGrouping("reflec:rtosa_in:rtosa_out");
        } else {
            targetProduct.setAutoGrouping("reflec");
        }
    }

    @Override
    protected void prepareInputs() throws OperatorException {
        super.prepareInputs();

        for (int i = 0; i < BAND_COUNT; i++) {
            assertSourceBand("radiance_" + (i + 1));
        }
        assertSourceBand("l1_flags");

        if (sourceProduct.getGeoCoding() == null) {
            throw new OperatorException("The source product must be geo-coded.");
        }

        try {
            algorithm = new C2rccMerisAlgorithm();
        } catch (IOException e) {
            throw new OperatorException(e);
        }

        algorithm.setTemperature(temperature);
        algorithm.setSalinity(salinity);
        if (!useDefaultSolarFlux) {
            double[] solfluxFromL1b = new double[BAND_COUNT];
            for (int i = 0; i < BAND_COUNT; i++) {
                solfluxFromL1b[i] = sourceProduct.getBand("radiance_" + (i + 1)).getSolarFlux();
            }
            if (isSolfluxValid(solfluxFromL1b)) {
                algorithm.setSolflux(solfluxFromL1b);
            }
        }
    }

    private void assertSourceBand(String name) {
        if (sourceProduct.getBand(name) == null) {
            throw new OperatorException("Invalid source product, band '" + name + "' required");
        }
    }

    private Band addBand(Product targetProduct, String name, String unit, String description) {
        Band targetBand = targetProduct.addBand(name, ProductData.TYPE_FLOAT32);
        targetBand.setUnit(unit);
        targetBand.setDescription(description);
        targetBand.setGeophysicalNoDataValue(Double.NaN);
        targetBand.setNoDataValueUsed(true);
        return targetBand;
    }

    private void addVirtualBand(Product targetProduct, String name, String expression, String unit, String description) {
        Band band = targetProduct.addBand(name, expression);
        band.setUnit(unit);
        band.setDescription(description);
        band.getSourceImage(); // trigger source image creation
        band.setGeophysicalNoDataValue(Double.NaN);
        band.setNoDataValueUsed(true);
    }

    private static boolean isSolfluxValid(double[] solflux) {
        for (double v : solflux) {
            if (v <= 0.0) {
                return false;
            }
        }
        return true;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public void setSalinity(double salinity) {
        this.salinity = salinity;
    }

    public void setUseDefaultSolarFlux(boolean useDefaultSolarFlux) {
        this.useDefaultSolarFlux = useDefaultSolarFlux;
    }

    public void setValidPixelExpression(String validPixelExpression) {
        this.validPixelExpression = validPixelExpression;
    }

    public void setOutputRtosa(boolean outputRtosa) {
        this.outputRtosa = outputRtosa;
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(C2rccMerisOperator.class);
        }
    }
}
