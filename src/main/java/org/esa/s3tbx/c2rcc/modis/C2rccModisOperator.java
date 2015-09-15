package org.esa.s3tbx.c2rcc.modis;

import static org.esa.s3tbx.c2rcc.C2rccConstants.ANC_DATA_URI;
import static org.esa.s3tbx.c2rcc.C2rccConstants.createOzoneFormat;
import static org.esa.s3tbx.c2rcc.C2rccConstants.createPressureFormat;
import static org.esa.s3tbx.c2rcc.C2rccConstants.fetchOzone;
import static org.esa.s3tbx.c2rcc.C2rccConstants.fetchSurfacePressure;
import static org.esa.s3tbx.c2rcc.modis.C2rccModisAlgorithm.reflec_wavelengths;
import static org.esa.s3tbx.c2rcc.seawifs.C2rccSeaWiFSAlgorithm.ozone_default;
import static org.esa.s3tbx.c2rcc.seawifs.C2rccSeaWiFSAlgorithm.pressure_default;

import org.esa.s3tbx.c2rcc.anc.AncDataFormat;
import org.esa.s3tbx.c2rcc.anc.AncDownloader;
import org.esa.s3tbx.c2rcc.anc.AncRepository;
import org.esa.s3tbx.c2rcc.anc.AtmosphericAuxdata;
import org.esa.s3tbx.c2rcc.anc.AtmosphericAuxdataDynamic;
import org.esa.s3tbx.c2rcc.anc.AtmosphericAuxdataStatic;
import org.esa.s3tbx.c2rcc.util.TargetProductPreparer;
import org.esa.snap.framework.datamodel.GeoCoding;
import org.esa.snap.framework.datamodel.GeoPos;
import org.esa.snap.framework.datamodel.PixelPos;
import org.esa.snap.framework.datamodel.Product;
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
import org.esa.snap.util.StringUtils;
import org.esa.snap.util.converters.BooleanExpressionConverter;

import java.io.File;
import java.io.IOException;

// todo (nf) - Add flags band and check for OOR of inputs and outputs of the NNs (https://github.com/bcdev/s3tbx-c2rcc/issues/2)
// todo (nf) - Add min/max values of NN inputs and outputs to metadata (https://github.com/bcdev/s3tbx-c2rcc/issues/3)

/**
 * The Case 2 Regional / CoastColour Operator for MODIS.
 * <p/>
 * Computes AC-reflectances and IOPs from MODIS L1C_LAC data products using
 * an neural-network approach.
 *
 * @author Norman Fomferra, Sabine Embacher
 */
@OperatorMetadata(alias = "modis.c2rcc", version = "0.2",
            authors = "Wolfgang Schoenfeld, Norman Fomferra (Brockmann Consult), Sabine Embacher (Brockmann Consult)",
            category = "Optical Processing/Thematic Water Processing",
            copyright = "Copyright (C) 2015 by Brockmann Consult",
            description = "Performs atmospheric correction and IOP retrieval on MODIS L1C_LAC data products.")
public class C2rccModisOperator extends PixelOperator {

    // Modis bands
    public static final int SOURCE_BAND_COUNT = reflec_wavelengths.length;
    public static final int SUN_ZEN_IX = SOURCE_BAND_COUNT;
    public static final int SUN_AZI_IX = SOURCE_BAND_COUNT + 1;
    public static final int VIEW_ZEN_IX = SOURCE_BAND_COUNT + 2;
    public static final int VIEW_AZI_IX = SOURCE_BAND_COUNT + 3;
    public static final int ATM_PRESS_IX = SOURCE_BAND_COUNT + 4;
    public static final int OZONE_IX = SOURCE_BAND_COUNT + 5;

    // Modis Targets
    public static final int REFLEC_BAND_COUNT = reflec_wavelengths.length;

    public static final int REFLEC_1_IX = 0;
    public static final int IOP_APIG_IX = REFLEC_BAND_COUNT;
    public static final int IOP_ADET_IX = REFLEC_BAND_COUNT + 1;
    public static final int IOP_AGELB_IX = REFLEC_BAND_COUNT + 2;
    public static final int IOP_BPART_IX = REFLEC_BAND_COUNT + 3;
    public static final int IOP_BWIT_IX = REFLEC_BAND_COUNT + 4;

    public static final int RTOSA_RATIO_MIN_IX = REFLEC_BAND_COUNT + 5;
    public static final int RTOSA_RATIO_MAX_IX = REFLEC_BAND_COUNT + 6;
    public static final int L2_FLAGS_IX = REFLEC_BAND_COUNT + 7;

    public static final int RTOSA_IN_1_IX = REFLEC_BAND_COUNT + 8;
    public static final int RTOSA_OUT_1_IX = RTOSA_IN_1_IX + REFLEC_BAND_COUNT;


    @SourceProduct(label = "MODIS L1C product",
                description = "MODIS L1C source product.")
    private Product sourceProduct;

    @Parameter(label = "Valid-pixel expression",
                defaultValue = "!(l2_flags.LAND ||  max(rhot_412,max(rhot_443,max(rhot_488,max(rhot_531,max(rhot_547,max(rhot_555,max(rhot_667,max(rhot_678,max(rhot_748,rhot_869)))))))))>0.25)",
                converter = BooleanExpressionConverter.class)
    private String validPixelExpression;

    @Parameter(defaultValue = "35.0", unit = "DU", interval = "(0, 100)")
    private double salinity;

    @Parameter(defaultValue = "15.0", unit = "C", interval = "(-50, 50)")
    private double temperature;

    @Parameter(defaultValue = "false", label = "Output top-of-standard-atmosphere (TOSA) reflectances")
    private boolean outputRtosa;

    @Parameter(description = "Path to the atmospheric auxiliary data directory.Use either this or tomsomiStartProduct, " +
                             "tomsomiEndProduct, ncepStartProduct, and ncepEndProduct to use ozone auxiliary data.")
    private String atmosphericAuxDataPath;

    @SourceProduct(description = "A product which is used for derivation of ozone values. Use either this and tomsomiEndProduct," +
                                 "ncepStartProduct, and ncepEndProduct or atmosphericAuxdataPath to use ozone auxiliary data.",
                optional = true)
    private Product tomsomiStartProduct;

    @SourceProduct(description = "A product which is used for derivation of ozone values. Use either this and " +
                                 "tomsomiStartProduct, ncepStartProduct, and ncepEndProduct or atmosphericAuxdataPath to use ozone auxiliary data.",
                optional = true)
    private Product tomsomiEndProduct;

    @SourceProduct(description = "A product which is used for derivation of ozone values. Use either this and tomsomiStartProduct, " +
                                 "tomsomiEndProduct, and ncepEndProduct or atmosphericAuxdataPath to use ozone auxiliary data.",
                optional = true)
    private Product ncepStartProduct;

    @SourceProduct(description = "A product which is used for derivation of ozone values. Use either this and tomsomiStartProduct, " +
                                 "tomsomiEndProduct, and ncepStartProduct or atmosphericAuxdataPath to use ozone auxiliary data.",
                optional = true)
    private Product ncepEndProduct;

    @Parameter(defaultValue = "330", unit = "DU", interval = "(0, 1000)")
    private double ozone;

    @Parameter(defaultValue = "1000", unit = "hPa", interval = "(0, 2000)")
    private double press;

    private C2rccModisAlgorithm algorithm;
    private AtmosphericAuxdata atmosphericAuxdata;

    public void setSalinity(double salinity) {
        this.salinity = salinity;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public void setValidPixelExpression(String validPixelExpression) {
        this.validPixelExpression = validPixelExpression;
    }

    public void setAtmosphericAuxDataPath(String atmosphericAuxDataPath) {
        this.atmosphericAuxDataPath = atmosphericAuxDataPath;
    }

    public void setTomsomiStartProduct(Product tomsomiStartProduct) {
        this.tomsomiStartProduct = tomsomiStartProduct;
    }

    public void setTomsomiEndProduct(Product tomsomiEndProduct) {
        this.tomsomiEndProduct = tomsomiEndProduct;
    }

    public void setNcepStartProduct(Product ncepStartProduct) {
        this.ncepStartProduct = ncepStartProduct;
    }

    public void setNcepEndProduct(Product ncepEndProduct) {
        this.ncepEndProduct = ncepEndProduct;
    }

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {

        double[] toa_ref = new double[SOURCE_BAND_COUNT];
        for (int i = 0; i < SOURCE_BAND_COUNT; i++) {
            toa_ref[i] = sourceSamples[i].getDouble();
        }

        GeoCoding geoCoding = sourceProduct.getGeoCoding();
        PixelPos pixelPos = new PixelPos(x + 0.5, y + 0.5);
        double mjd = sourceProduct.getTimeCoding().getMJD(pixelPos);
        GeoPos geoPos = geoCoding.getGeoPos(pixelPos, new GeoPos());

        double ozone = fetchOzone(atmosphericAuxdata, mjd, geoPos.lat, geoPos.lon);
        double atmPress = fetchSurfacePressure(atmosphericAuxdata, mjd, geoPos.lat, geoPos.lon);
        C2rccModisAlgorithm.Result result = algorithm.processPixel(
                    toa_ref,
                    sourceSamples[SUN_ZEN_IX].getDouble(),
                    sourceSamples[SUN_AZI_IX].getDouble(),
                    sourceSamples[VIEW_ZEN_IX].getDouble(),
                    sourceSamples[VIEW_AZI_IX].getDouble(),
                    atmPress,
                    ozone
        );

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
        for (int i = 0; i < reflec_wavelengths.length; i++) {
            int wl = reflec_wavelengths[i];
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
        for (int i = 0; i < reflec_wavelengths.length; i++) {
            sc.defineSample(i, "reflec_" + reflec_wavelengths[i]);
        }
        sc.defineSample(IOP_APIG_IX, "iop_apig");
        sc.defineSample(IOP_ADET_IX, "iop_adet");
        sc.defineSample(IOP_AGELB_IX, "iop_agelb");
        sc.defineSample(IOP_BPART_IX, "iop_bpart");
        sc.defineSample(IOP_BWIT_IX, "iop_bwit");

        sc.defineSample(RTOSA_RATIO_MIN_IX, "rtosa_ratio_min");
        sc.defineSample(RTOSA_RATIO_MAX_IX, "rtosa_ratio_max");
        sc.defineSample(L2_FLAGS_IX, "l2_qflags");

        if (outputRtosa) {
            for (int i = 0; i < reflec_wavelengths.length; i++) {
                int wl = reflec_wavelengths[i];
                sc.defineSample(RTOSA_IN_1_IX + i, "rtosa_in_" + wl);
            }
            for (int i = 0; i < reflec_wavelengths.length; i++) {
                int wl = reflec_wavelengths[i];
                sc.defineSample(RTOSA_OUT_1_IX + i, "rtosa_out_" + wl);
            }
        }
    }

    @Override
    protected void configureTargetProduct(ProductConfigurer productConfigurer) {
        super.configureTargetProduct(productConfigurer);
        productConfigurer.copyMetadata();
        Product targetProduct = productConfigurer.getTargetProduct();
        TargetProductPreparer.prepareTargetProduct(targetProduct, sourceProduct, "rhot_", reflec_wavelengths, outputRtosa);
    }

    @Override
    protected void prepareInputs() throws OperatorException {
        for (int wl : reflec_wavelengths) {
            assertSourceBand("rhot_" + wl);
        }
        assertSourceBand("l2_flags");

//        if (source.getGeoCoding() == null) {
//            throw new OperatorException("The source product must be geo-coded.");
//        }

        try {
            algorithm = new C2rccModisAlgorithm();
        } catch (IOException e) {
            throw new OperatorException(e);
        }

        algorithm.setTemperature(temperature);
        algorithm.setSalinity(salinity);

        initAtmosphericAuxdata();
    }

    private void initAtmosphericAuxdata() {
        if (StringUtils.isNullOrEmpty(atmosphericAuxDataPath)) {
            try {
                atmosphericAuxdata = new AtmosphericAuxdataStatic(tomsomiStartProduct, tomsomiEndProduct, "ozone", ozone,
                                                                  ncepStartProduct, ncepEndProduct, "press", press);
            } catch (IOException e) {
                getLogger().severe("Unable to create provider for atmospheric ancillary data.");
                getLogger().severe(e.getMessage());
            }
        } else {
            final AncDownloader ancDownloader = new AncDownloader(ANC_DATA_URI);
            final AncRepository ancRepository = new AncRepository(new File(atmosphericAuxDataPath), ancDownloader);
            AncDataFormat ozoneFormat = createOzoneFormat(ozone_default);
            AncDataFormat pressureFormat = createPressureFormat(pressure_default);
            atmosphericAuxdata = new AtmosphericAuxdataDynamic(ancRepository, ozoneFormat, pressureFormat);
        }
    }

    private void assertSourceBand(String name) {
        if (sourceProduct.getBand(name) == null) {
            throw new OperatorException("Invalid source product, band '" + name + "' required");
        }
    }

    public void setOutputRtosa(boolean outputRtosa) {
        this.outputRtosa = outputRtosa;
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(C2rccModisOperator.class);
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        atmosphericAuxdata.dispose();
    }
}
