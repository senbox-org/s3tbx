package org.esa.s3tbx.c2rcc.modis;

import org.esa.snap.framework.datamodel.Band;
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

import java.io.IOException;

// todo (nf) - Add flags band and check for OOR of inputs and outputs of the NNs (https://github.com/bcdev/s3tbx-c2rcc/issues/2)
// todo (nf) - Add min/max values of NN inputs and outputs to metadata (https://github.com/bcdev/s3tbx-c2rcc/issues/3)

/**
 * The Case 2 Regional / CoastColour Operator for MODIS.
 * <p/>
 * Computes AC-reflectances and IOPs from MODIS L1C data products using
 * an neural-network approach.
 *
 * @author Norman Fomferra, Sabine Embacher
 */
@OperatorMetadata(alias = "modis.c2rcc", version = "0.2",
            authors = "Wolfgang Schoenfeld, Norman Fomferra (Brockmann Consult), Sabine Embacher (Brockmann Consult)",
            category = "Optical Processing/Thematic Water Processing",
            copyright = "Copyright (C) 2015 by Brockmann Consult",
            description = "Performs atmospheric correction and IOP retrieval on MODIS L1C data products.")
public class C2rccModisOperator extends PixelOperator {

    // Modis bands
    public static final int BAND_COUNT = C2rccModisAlgorithm.reflec_wavelengths.length;

    public static final int CONC_APIG_IX = BAND_COUNT;
    public static final int CONC_ADET_IX = BAND_COUNT + 1;
    public static final int CONC_AGELB_IX = BAND_COUNT + 2;
    public static final int CONC_BPART_IX = BAND_COUNT + 3;
    public static final int CONC_BWIT_IX = BAND_COUNT + 4;

    public static final int SUN_ZEN_IX = BAND_COUNT;
    public static final int SUN_AZI_IX = BAND_COUNT + 1;
    public static final int VIEW_ZEN_IX = BAND_COUNT + 2;
    public static final int VIEW_AZI_IX = BAND_COUNT + 3;
    public static final int ATM_PRESS_IX = BAND_COUNT + 4;
    public static final int OZONE_IX = BAND_COUNT + 5;

    @SourceProduct(label = "MODIS L1C product",
                description = "MODIS L1C source product.")
    private Product sourceProduct;

    @Parameter(label = "Valid-pixel expression",
                defaultValue = "",
                converter = BooleanExpressionConverter.class)
    private String validPixelExpression;

    @Parameter(defaultValue = "35.0", unit = "DU", interval = "(0, 100)")
    private double salinity;

    @Parameter(defaultValue = "15.0", unit = "C", interval = "(-50, 50)")
    private double temperature;

    private C2rccModisAlgorithm algorithm;

    public void setSalinity(double salinity) {
        this.salinity = salinity;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public void setValidPixelExpression(String validPixelExpression) {
        this.validPixelExpression = validPixelExpression;
    }

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {

        double[] toa_ref = new double[BAND_COUNT];
        for (int i = 0; i < BAND_COUNT; i++) {
            toa_ref[i] = sourceSamples[i].getDouble();
        }

        C2rccModisAlgorithm.Result result = algorithm.processPixel(
                    toa_ref,
                    sourceSamples[SUN_ZEN_IX].getDouble(),
                    sourceSamples[SUN_AZI_IX].getDouble(),
                    sourceSamples[VIEW_ZEN_IX].getDouble(),
                    sourceSamples[VIEW_AZI_IX].getDouble(),
                    C2rccModisAlgorithm.pressure_default,  // todo to be replaced by a real value
//                    sourceSamples[ATM_PRESS_IX].getDouble(),
                    C2rccModisAlgorithm.ozone_default      // todo to be replaced by a real value
//                    sourceSamples[OZONE_IX].getDouble()
        );

        for (int i = 0; i < result.rw.length; i++) {
            targetSamples[i].set(result.rw[i]);
        }

        for (int i = 0; i < result.iops.length; i++) {
            targetSamples[result.rw.length + i].set(result.iops[i]);
        }
    }

    @Override
    protected void configureSourceSamples(SourceSampleConfigurer sc) throws OperatorException {
        sc.setValidPixelMask(validPixelExpression);
        for (int i = 0; i < C2rccModisAlgorithm.reflec_wavelengths.length; i++) {
            int wl = C2rccModisAlgorithm.reflec_wavelengths[i];
            sc.defineSample(i, "rhot_" + wl);
        }
        sc.defineSample(SUN_ZEN_IX, "solz");
        sc.defineSample(SUN_AZI_IX, "sola");
        sc.defineSample(VIEW_ZEN_IX, "senz");
        sc.defineSample(VIEW_AZI_IX, "sena");
//        sc.defineSample(ATM_PRESS_IX, "atm_press");
//        sc.defineSample(OZONE_IX, "ozone");
    }

    @Override
    protected void configureTargetSamples(TargetSampleConfigurer sc) throws OperatorException {
        for (int i = 0; i < C2rccModisAlgorithm.reflec_wavelengths.length; i++) {
            sc.defineSample(i, "reflec_" + C2rccModisAlgorithm.reflec_wavelengths[i]);
        }
        sc.defineSample(CONC_APIG_IX, "conc_apig");
        sc.defineSample(CONC_ADET_IX, "conc_adet");
        sc.defineSample(CONC_AGELB_IX, "conc_agelb");
        sc.defineSample(CONC_BPART_IX, "conc_bpart");
        sc.defineSample(CONC_BWIT_IX, "conc_bwit");
    }


    @Override
    protected void configureTargetProduct(ProductConfigurer productConfigurer) {
        super.configureTargetProduct(productConfigurer);
        productConfigurer.copyMetadata();

        Product targetProduct = productConfigurer.getTargetProduct();

        ProductUtils.copyFlagBands(sourceProduct, targetProduct, true);

        for (int index : C2rccModisAlgorithm.reflec_wavelengths) {
            Band reflecBand = targetProduct.addBand("reflec_" + index, ProductData.TYPE_FLOAT32);
            ProductUtils.copySpectralBandProperties(sourceProduct.getBand("rhot_" + index), reflecBand);
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

        targetProduct.setAutoGrouping("reflec:conc");
    }

    @Override
    protected void prepareInputs() throws OperatorException {
        super.prepareInputs();

        for (int i = 0; i < C2rccModisAlgorithm.reflec_wavelengths.length; i++) {
            int wl = C2rccModisAlgorithm.reflec_wavelengths[i];
            assertSourceBand("rhot_" + wl);
        }

        try {
            algorithm = new C2rccModisAlgorithm();
        } catch (IOException e) {
            throw new OperatorException(e);
        }

        algorithm.setTemperature(temperature);
        algorithm.setSalinity(salinity);
    }

    private void assertSourceBand(String name) {
        if (sourceProduct.getBand(name) == null) {
            throw new OperatorException("Invalid source product, band '" + name + "' required");
        }
    }

    private void addBand(Product targetProduct, String name, String unit, String description) {
        Band targetBand = targetProduct.addBand(name, ProductData.TYPE_FLOAT32);
        targetBand.setUnit(unit);
        targetBand.setDescription(description);
        targetBand.setGeophysicalNoDataValue(Double.NaN);
        targetBand.setNoDataValueUsed(true);
    }

    private void addVirtualBand(Product targetProduct, String name, String expression, String unit, String description) {
        Band band = targetProduct.addBand(name, expression);
        band.setUnit(unit);
        band.setDescription(description);
        band.getSourceImage(); // trigger source image creation
        band.setGeophysicalNoDataValue(Double.NaN);
        band.setNoDataValueUsed(true);
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(C2rccModisOperator.class);
        }
    }
}
