package org.esa.s3tbx.c2rcc.meris;

import static org.esa.s3tbx.c2rcc.anc.AncillaryCommons.ANC_DATA_URI;
import static org.esa.s3tbx.c2rcc.anc.AncillaryCommons.createOzoneFormat;
import static org.esa.s3tbx.c2rcc.anc.AncillaryCommons.createPressureFormat;
import static org.esa.s3tbx.c2rcc.anc.AncillaryCommons.fetchOzone;
import static org.esa.s3tbx.c2rcc.anc.AncillaryCommons.fetchSurfacePressure;
import static org.esa.s3tbx.c2rcc.meris.C2rccMerisAlgorithm.DEFAULT_SOLAR_FLUX;
import static org.esa.s3tbx.c2rcc.meris.C2rccMerisAlgorithm.merband12_ix;
import static org.esa.s3tbx.c2rcc.seawifs.C2rccSeaWiFSAlgorithm.ozone_default;
import static org.esa.s3tbx.c2rcc.seawifs.C2rccSeaWiFSAlgorithm.pressure_default;

import org.esa.s3tbx.c2rcc.anc.AncDataFormat;
import org.esa.s3tbx.c2rcc.anc.AncDownloader;
import org.esa.s3tbx.c2rcc.anc.AncRepository;
import org.esa.s3tbx.c2rcc.anc.AtmosphericAuxdata;
import org.esa.s3tbx.c2rcc.anc.AtmosphericAuxdataDynamic;
import org.esa.s3tbx.c2rcc.anc.AtmosphericAuxdataStatic;
import org.esa.s3tbx.c2rcc.util.SolarFluxLazyLookup;
import org.esa.s3tbx.c2rcc.util.TargetProductPreparer;
import org.esa.snap.framework.datamodel.GeoPos;
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
import org.esa.snap.util.StringUtils;
import org.esa.snap.util.converters.BooleanExpressionConverter;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;

// todo (nf) - Add Thullier solar fluxes as default values to C2R-CC operator (https://github.com/bcdev/s3tbx-c2rcc/issues/1)
// todo (nf) - Add flags band and check for OOR of inputs and outputs of the NNs (https://github.com/bcdev/s3tbx-c2rcc/issues/2)
// todo (nf) - Add min/max values of NN inputs and outputs to metadata (https://github.com/bcdev/s3tbx-c2rcc/issues/3)

/**
 * The Case 2 Regional / CoastColour Operator for MERIS.
 * <p/>
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
    public static final int IOP_APIG_IX = REFLEC_N;
    public static final int IOP_ADET_IX = REFLEC_N + 1;
    public static final int IOP_AGELB_IX = REFLEC_N + 2;
    public static final int IOP_BPART_IX = REFLEC_N + 3;
    public static final int IOP_BWIT_IX = REFLEC_N + 4;

    public static final int RTOSA_RATIO_MIN_IX = REFLEC_N + 5;
    public static final int RTOSA_RATIO_MAX_IX = REFLEC_N + 6;
    public static final int L2_QFLAGS_IX = REFLEC_N + 7;

    public static final int RTOSA_IN_1_IX = REFLEC_N + 8;
    public static final int RTOSA_OUT_1_IX = RTOSA_IN_1_IX + REFLEC_N;

    @SourceProduct(label = "MERIS L1b product",
                description = "MERIS L1b source product.")
    private Product sourceProduct;

    @SourceProduct(description = "The first product providing ozone values for ozone interpolation. " +
                                 "Use either this in combination with other start- and end-products (tomsomiEndProduct, " +
                                 "ncepStartProduct, ncepEndProduct) or atmosphericAuxdataPath to use ozone and air pressure " +
                                 "aux data for calculations.",
                optional = true)
    private Product tomsomiStartProduct;

    @SourceProduct(description = "The second product providing ozone values for ozone interpolation. " +
                                 "Use either this in combination with other start- and end-products (tomsomiStartProduct, " +
                                 "ncepStartProduct, ncepEndProduct) or atmosphericAuxdataPath to use ozone and air pressure " +
                                 "aux data for calculations.",
                optional = true)
    private Product tomsomiEndProduct;

    @SourceProduct(description = "The first product providing air pressure values for pressure interpolation. " +
                                 "Use either this in combination with other start- and end-products (tomsomiStartProduct, " +
                                 "tomsomiEndProduct, ncepEndProduct) or atmosphericAuxdataPath to use ozone and air pressure " +
                                 "aux data for calculations.",
                optional = true)
    private Product ncepStartProduct;

    @SourceProduct(description = "The second product providing air pressure values for pressure interpolation. " +
                                 "Use either this in combination with other start- and end-products (tomsomiStartProduct, " +
                                 "tomsomiEndProduct, ncepStartProduct) or atmosphericAuxdataPath to use ozone and air pressure " +
                                 "aux data for calculations.",
                optional = true)
    private Product ncepEndProduct;

    @Parameter(label = "Valid-pixel expression",
                defaultValue = "!l1_flags.INVALID && !l1_flags.LAND_OCEAN",
                converter = BooleanExpressionConverter.class)
    private String validPixelExpression;

    @Parameter(defaultValue = "35.0", unit = "DU", interval = "(0, 100)")
    private double salinity;

    @Parameter(defaultValue = "15.0", unit = "C", interval = "(-50, 50)")
    private double temperature;

    @Parameter(defaultValue = "330", unit = "DU", interval = "(0, 1000)")
    private double ozone;

    @Parameter(defaultValue = "1000", unit = "hPa", interval = "(0, 2000)")
    private double press;

    @Parameter(description = "Path to the atmospheric auxiliary data directory. Use either this or tomsomiStartProduct, " +
                             "tomsomiEndProduct, ncepStartProduct and ncepEndProduct to use ozone and air pressure aux data " +
                             "for calculations. If the auxiliary data needed for interpolation not available in this " +
                             "path, the data will automatically downloaded.")
    private String atmosphericAuxDataPath;

    @Parameter(defaultValue = "false", label = "Output top-of-standard-atmosphere (TOSA) reflectances")
    private boolean outputRtosa;

    @Parameter(defaultValue = "false")
    private boolean useDefaultSolarFlux;

    @Parameter(defaultValue = "false", description =
                "If selected, the ecmwf auxiliary data (ozon, air pressure) of the source product is used")
    private boolean useEcmwfAuxData;

    private C2rccMerisAlgorithm algorithm;
    private SolarFluxLazyLookup solarFluxLazyLookup;
    private AtmosphericAuxdata atmosphericAuxdata;

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

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public void setSalinity(double salinity) {
        this.salinity = salinity;
    }

    public void setOzone(double ozone) {
        this.ozone = ozone;
    }

    public void setPress(double press) {
        this.press = press;
    }

    public void setUseDefaultSolarFlux(boolean useDefaultSolarFlux) {
        this.useDefaultSolarFlux = useDefaultSolarFlux;
    }

    public void setUseEcmwfAuxData(boolean useEcmwfAuxData) {
        this.useEcmwfAuxData = useEcmwfAuxData;
    }

    public void setValidPixelExpression(String validPixelExpression) {
        this.validPixelExpression = validPixelExpression;
    }

    public void setOutputRtosa(boolean outputRtosa) {
        this.outputRtosa = outputRtosa;
    }

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {

        double[] radiances = new double[BAND_COUNT];
        for (int i = 0; i < BAND_COUNT; i++) {
            radiances[i] = sourceSamples[i].getDouble();
        }

        final PixelPos pixelPos = new PixelPos(x + 0.5f, y + 0.5f);
        final double mjd = sourceProduct.getTimeCoding().getMJD(pixelPos);
        if (useDefaultSolarFlux) {
            ProductData.UTC utc = new ProductData.UTC(mjd);
            Calendar calendar = utc.getAsCalendar();
            final int doy = calendar.get(Calendar.DAY_OF_YEAR);
            final int year = calendar.get(Calendar.YEAR);
            double[] correctedSolFlux = solarFluxLazyLookup.getCorrectedFluxFor(doy, year);
            algorithm.setSolflux(correctedSolFlux);
        }

        // use real geocoding if needed
//        GeoPos geoPos = new GeoPos(0, 0);
        GeoPos geoPos = sourceProduct.getGeoCoding().getGeoPos(pixelPos, null);
        double lat = geoPos.getLat();
        double lon = geoPos.getLon();
        double atmPress;
        double ozone;
        if (useEcmwfAuxData) {
            atmPress = sourceSamples[ATM_PRESS_IX].getDouble();
            ozone = sourceSamples[OZONE_IX].getDouble();
        } else {
            ozone = fetchOzone(atmosphericAuxdata, mjd, lat, lon);
            atmPress = fetchSurfacePressure(atmosphericAuxdata, mjd, lat, lon);
        }
        C2rccMerisAlgorithm.Result result = algorithm.processPixel(x, y, lat, lon,
                                                                   radiances,
                                                                   sourceSamples[SUN_ZEN_IX].getDouble(),
                                                                   sourceSamples[SUN_AZI_IX].getDouble(),
                                                                   sourceSamples[VIEW_ZEN_IX].getDouble(),
                                                                   sourceSamples[VIEW_AZI_IX].getDouble(),
                                                                   sourceSamples[DEM_ALT_IX].getDouble(),
                                                                   atmPress,
                                                                   ozone);

        for (int i = 0; i < result.rw.length; i++) {
            targetSamples[i].set(result.rw[i]);
        }

        for (int i = 0; i < result.iops.length; i++) {
            targetSamples[result.rw.length + i].set(result.iops[i]);
        }

        targetSamples[RTOSA_RATIO_MIN_IX].set(result.rtosa_ratio_min);
        targetSamples[RTOSA_RATIO_MAX_IX].set(result.rtosa_ratio_max);
        targetSamples[L2_QFLAGS_IX].set(result.flags);

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

        sc.defineSample(IOP_APIG_IX, "iop_apig");
        sc.defineSample(IOP_ADET_IX, "iop_adet");
        sc.defineSample(IOP_AGELB_IX, "iop_agelb");
        sc.defineSample(IOP_BPART_IX, "iop_bpart");
        sc.defineSample(IOP_BWIT_IX, "iop_bwit");
        sc.defineSample(RTOSA_RATIO_MIN_IX, "rtosa_ratio_min");
        sc.defineSample(RTOSA_RATIO_MAX_IX, "rtosa_ratio_max");
        sc.defineSample(L2_QFLAGS_IX, "l2_qflags");

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
        TargetProductPreparer.prepareTargetProduct(targetProduct, sourceProduct, "radiance_", merband12_ix, outputRtosa);
    }

    @Override
    protected void prepareInputs() throws OperatorException {
        for (int i = 0; i < BAND_COUNT; i++) {
            assertSourceBand("radiance_" + (i + 1));
        }
        assertSourceBand("l1_flags");

        // todo must be reactivated later
//        if (sourceProduct.getGeoCoding() == null) {
//            throw new OperatorException("The source product must be geo-coded.");
//        }

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
            } else {
                throw new OperatorException("Invalid solar flux in source product!");
            }
        } else {
            solarFluxLazyLookup = new SolarFluxLazyLookup(DEFAULT_SOLAR_FLUX);
        }
        if (!useEcmwfAuxData) {
            initAtmosphericAuxdata();
        }
    }

    private void initAtmosphericAuxdata() {
        if (StringUtils.isNullOrEmpty(atmosphericAuxDataPath)) {
            try {
                atmosphericAuxdata = new AtmosphericAuxdataStatic(tomsomiStartProduct, tomsomiEndProduct, "ozone", ozone,
                                                                  ncepStartProduct, ncepEndProduct, "press", press);
            } catch (IOException e) {
                throw new OperatorException("Unable to create provider for atmospheric ancillary data.", e);
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

    private static boolean isSolfluxValid(double[] solflux) {
        for (double v : solflux) {
            if (v <= 0.0) {
                return false;
            }
        }
        return true;
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(C2rccMerisOperator.class);
        }
    }
}
