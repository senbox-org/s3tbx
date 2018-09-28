package org.esa.s3tbx.c2rcc.viirs;

import org.esa.s3tbx.c2rcc.C2rccCommons;
import org.esa.s3tbx.c2rcc.C2rccConfigurable;
import org.esa.s3tbx.c2rcc.ancillary.AtmosphericAuxdata;
import org.esa.s3tbx.c2rcc.ancillary.AtmosphericAuxdataBuilder;
import org.esa.s3tbx.c2rcc.util.RgbProfiles;
import org.esa.s3tbx.c2rcc.util.TargetProductPreparer;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.TimeCoding;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.pointop.PixelOperator;
import org.esa.snap.core.gpf.pointop.ProductConfigurer;
import org.esa.snap.core.gpf.pointop.Sample;
import org.esa.snap.core.gpf.pointop.SourceSampleConfigurer;
import org.esa.snap.core.gpf.pointop.TargetSampleConfigurer;
import org.esa.snap.core.gpf.pointop.WritableSample;
import org.esa.snap.core.util.BitSetter;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.converters.BooleanExpressionConverter;

import java.io.IOException;

import static org.esa.s3tbx.c2rcc.C2rccCommons.*;
import static org.esa.s3tbx.c2rcc.viirs.C2rccViirsAlgorithm.*;

// todo (nf) - Add Thullier solar fluxes as default values to C2RCC operator (https://github.com/bcdev/s3tbx-c2rcc/issues/1)
// todo (nf) - Add flags band and check for OOR of inputs and outputs of the NNs (https://github.com/bcdev/s3tbx-c2rcc/issues/2)
// todo (nf) - Add min/max values of NN inputs and outputs to metadata (https://github.com/bcdev/s3tbx-c2rcc/issues/3)

/**
 * The Case 2 Regional / CoastColour Operator for Viirs.
 * <p/>
 * Computes AC-reflectances and IOPs from Viirs L1C data products using
 * an neural-network approach.
 *
 * @author Marco Peters
 */
@OperatorMetadata(alias = "c2rcc.viirs", version = "1.0",
        authors = "Roland Doerffer, Marco Peters (Brockmann Consult)",
        category = "Optical/Thematic Water Processing",
        copyright = "Copyright (C) 2016 by Brockmann Consult",
        description = "Performs atmospheric correction and IOP retrieval on Viirs L1C data products.")
public class C2rccViirsOperator extends PixelOperator implements C2rccConfigurable {
    /*
        c2rcc ops have been removed from Graph Builder. In the layer xml they are disabled
        see https://senbox.atlassian.net/browse/SNAP-395
    */

    private static final int WL_BAND_COUNT = viirsWavelengths.length;
    private static final String RASTER_NAME_SOLAR_ZENITH = "solz";
    private static final String RASTER_NAME_SOLAR_AZIMUTH = "sola";
    private static final String RASTER_NAME_VIEW_AZIMUTH = "sena";
    private static final String RASTER_NAME_VIEW_ZENITH = "senz";
    static final String SOURCE_RADIANCE_NAME_PREFIX = "rhot_";
    static final String[] GEOMETRY_ANGLE_NAMES = {RASTER_NAME_SOLAR_ZENITH, RASTER_NAME_SOLAR_AZIMUTH,
            RASTER_NAME_VIEW_ZENITH, RASTER_NAME_VIEW_AZIMUTH};
    static final String RASTER_NAME_L2_FLAGS = "l2_flags";

    // sources
    private static final int SUN_ZEN_IX = WL_BAND_COUNT;
    private static final int SUN_AZI_IX = WL_BAND_COUNT + 1;
    private static final int VIEW_ZEN_IX = WL_BAND_COUNT + 2;
    private static final int VIEW_AZI_IX = WL_BAND_COUNT + 3;
    private static final int VALID_PIXEL_IX = WL_BAND_COUNT + 4;

    // targets
    private static final int REFLEC_1_IX = 0;
    private static final int IOP_APIG_IX = WL_BAND_COUNT;
    private static final int IOP_ADET_IX = WL_BAND_COUNT + 1;
    private static final int IOP_AGELB_IX = WL_BAND_COUNT + 2;
    private static final int IOP_BPART_IX = WL_BAND_COUNT + 3;
    private static final int IOP_BWIT_IX = WL_BAND_COUNT + 4;

    private static final int RTOSA_RATIO_MIN_IX = WL_BAND_COUNT + 5;
    private static final int RTOSA_RATIO_MAX_IX = WL_BAND_COUNT + 6;
    private static final int C2RCC_FLAGS_IX = WL_BAND_COUNT + 7;

    private static final int RTOSA_IN_1_IX = WL_BAND_COUNT + 8;
    private static final int RTOSA_OUT_1_IX = RTOSA_IN_1_IX + WL_BAND_COUNT;

    private static final String PRODUCT_TYPE = "C2RCC_VIIRS";

    /*
     * Source product type has been changed from L1B to L1C in commit
     * https://github.com/bcdev/s3tbx-c2rcc/commit/dcf85caa793c08a52d46f79f3004841be57ab04c
     *
     * TODO (2016-06-01/mp - Actually we can support both types)
     */
    @SourceProduct(label = "VIIRS L1C product",
            description = "VIIRS L1C source product.")
    private Product sourceProduct;

    @SourceProduct(description = "The first product providing ozone values for ozone interpolation. " +
            "Use either the TOMSOMI and NCEP products or the atmosphericAuxdataPath to as source for ozone and air pressure.",
            optional = true,
            label = "Ozone interpolation start product (TOMSOMI)")
    private Product tomsomiStartProduct;

    @SourceProduct(description = "The second product providing ozone values for ozone interpolation. " +
            "Use either the TOMSOMI and NCEP products or the atmosphericAuxdataPath to as source for ozone and air pressure.",
            optional = true,
            label = "Ozone interpolation end product (TOMSOMI)")
    private Product tomsomiEndProduct;

    @SourceProduct(description = "The first product providing air pressure values for pressure interpolation. " +
            "Use either the TOMSOMI and NCEP products or the atmosphericAuxdataPath to as source for ozone and air pressure.",
            optional = true,
            label = "Air pressure interpolation start product (NCEP)")
    private Product ncepStartProduct;

    @SourceProduct(description = "The second product providing air pressure values for pressure interpolation. " +
            "Use either the TOMSOMI and NCEP products or the atmosphericAuxdataPath to as source for ozone and air pressure.",
            optional = true,
            label = "Air pressure interpolation end product (NCEP)")
    private Product ncepEndProduct;

    @Parameter(label = "Valid-pixel expression",
            defaultValue = "!(l2_flags.LAND || rhot_862 > 0.25)",
            description = "Defines the pixels which are valid for processing",
            converter = BooleanExpressionConverter.class)
    private String validPixelExpression;

    @Parameter(defaultValue = "35.0", unit = "PSU", interval = "(0.000028, 43)",
            description = "The value used as salinity for the scene")
    private double salinity;

    @Parameter(defaultValue = "15.0", unit = "C", interval = "(0.000111, 36)",
            description = "The value used as temperature for the scene")
    private double temperature;

    @Parameter(defaultValue = "330", unit = "DU", interval = "(0, 1000)",
            description = "The value used as ozone if not provided by auxiliary data")
    private double ozone;

    @Parameter(defaultValue = "1000", unit = "hPa", interval = "(800, 1040)", label = "Air Pressure at Sea Level",
            description = "The surface air pressure at sea level if not provided by auxiliary data")
    private double press;

    @Parameter(description = "Path to the atmospheric auxiliary data directory. Use either this or tomsomiStartProduct, " +
            "tomsomiEndProduct, ncepStartProduct and ncepEndProduct to use ozone and air pressure aux data " +
            "for calculations. If the auxiliary data needed for interpolation not available in this " +
            "path, the data will automatically downloaded.")
    private String atmosphericAuxDataPath;

    @Parameter(defaultValue = "false", label = "Output TOSA reflectances")
    private boolean outputRtosa;

    @Parameter(defaultValue = "false", description =
            "Reflectance values in the target product shall be either written as remote sensing or water leaving reflectances",
            label = "Output AC reflectances as rrs instead of rhow")
    private boolean outputAsRrs;


    private C2rccViirsAlgorithm algorithm;
    private AtmosphericAuxdata atmosphericAuxdata;
    private TimeCoding timeCoding;

    @Override
    public void setAtmosphericAuxDataPath(String atmosphericAuxDataPath) {
        this.atmosphericAuxDataPath = atmosphericAuxDataPath;
    }

    @Override
    public void setTomsomiStartProduct(Product tomsomiStartProduct) {
        this.tomsomiStartProduct = tomsomiStartProduct;
    }

    @Override
    public void setTomsomiEndProduct(Product tomsomiEndProduct) {
        this.tomsomiEndProduct = tomsomiEndProduct;
    }

    @Override
    public void setNcepStartProduct(Product ncepStartProduct) {
        this.ncepStartProduct = ncepStartProduct;
    }

    @Override
    public void setNcepEndProduct(Product ncepEndProduct) {
        this.ncepEndProduct = ncepEndProduct;
    }

    @Override
    public void setOzone(double ozone) {
        this.ozone = ozone;
    }

    @Override
    public void setPress(double press) {
        this.press = press;
    }

    @Override
    public void setOutputAsRrs(boolean asRadianceRefl) {
        outputAsRrs = asRadianceRefl;
    }


    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {
        boolean samplesValid = C2rccCommons.areSamplesValid(sourceSamples, x, y);
        if (sourceSamples[VALID_PIXEL_IX].getBoolean() && samplesValid) {
            double[] toa_ref = new double[WL_BAND_COUNT];
            for (int i = 0; i < WL_BAND_COUNT; i++) {
                toa_ref[i] = sourceSamples[i].getDouble();
            }

            final PixelPos pixelPos = new PixelPos(x + 0.5f, y + 0.5f);
            GeoPos geoPos = sourceProduct.getSceneGeoCoding().getGeoPos(pixelPos, null);
            final double mjd = timeCoding.getMJD(pixelPos);
            final double lat = geoPos.getLat();
            final double lon = geoPos.getLon();

            final double sun_zeni = sourceSamples[SUN_ZEN_IX].getDouble();
            final double sun_azi = sourceSamples[SUN_AZI_IX].getDouble();
            final double view_zeni = sourceSamples[VIEW_ZEN_IX].getDouble();
            final double view_azi = sourceSamples[VIEW_AZI_IX].getDouble();
            final double dem_alt = 0.0;  // todo to be replaced by a real value
            final double atm_press = fetchSurfacePressure(atmosphericAuxdata, mjd, x, y, lat, lon);
            final double ozone = fetchOzone(atmosphericAuxdata, mjd, x, y, lat, lon);


            C2rccViirsAlgorithm.Result result = algorithm.processPixel(
                    toa_ref,
                    sun_zeni, sun_azi,
                    view_zeni, view_azi,
                    dem_alt,
                    atm_press, ozone
            );

            for (int i = 0; i < result.rw.length; i++) {
                targetSamples[i].set(outputAsRrs ? result.rw[i] / Math.PI : result.rw[i]);
            }

            for (int i = 0; i < result.iops.length; i++) {
                targetSamples[result.rw.length + i].set(result.iops[i]);
            }

            targetSamples[RTOSA_RATIO_MIN_IX].set(result.rtosa_ratio_min);
            targetSamples[RTOSA_RATIO_MAX_IX].set(result.rtosa_ratio_max);

            int flags = BitSetter.setFlag(result.flags, C2rccViirsAlgorithm.FLAG_INDEX_VALID_PE, true);
            targetSamples[C2RCC_FLAGS_IX].set(flags);

            if (outputRtosa) {
                for (int i = 0; i < result.rtosa_in.length; i++) {
                    targetSamples[RTOSA_IN_1_IX + i].set(result.rtosa_in[i]);
                }
                for (int i = 0; i < result.rtosa_out.length; i++) {
                    targetSamples[RTOSA_OUT_1_IX + i].set(result.rtosa_out[i]);
                }
            }
        } else {
            setInvalid(targetSamples);
            int flags = BitSetter.setFlag(0, C2rccViirsAlgorithm.FLAG_INDEX_VALID_PE, false);
            targetSamples[C2RCC_FLAGS_IX].set(flags);
        }
    }

    @Override
    protected void configureSourceSamples(SourceSampleConfigurer sc) throws OperatorException {
        for (int i = 0; i < WL_BAND_COUNT; i++) {
            final int wavelength = viirsWavelengths[i];
            sc.defineSample(i, SOURCE_RADIANCE_NAME_PREFIX + wavelength);
        }
        sc.defineSample(SUN_ZEN_IX, RASTER_NAME_SOLAR_ZENITH);
        sc.defineSample(SUN_AZI_IX, RASTER_NAME_SOLAR_AZIMUTH);
        sc.defineSample(VIEW_ZEN_IX, RASTER_NAME_VIEW_ZENITH);
        sc.defineSample(VIEW_AZI_IX, RASTER_NAME_VIEW_AZIMUTH);
        if (StringUtils.isNotNullAndNotEmpty(validPixelExpression)) {
            sc.defineComputedSample(VALID_PIXEL_IX, ProductData.TYPE_UINT8, validPixelExpression);
        } else {
            sc.defineComputedSample(VALID_PIXEL_IX, ProductData.TYPE_UINT8, "true");
        }
    }

    @Override
    protected void configureTargetSamples(TargetSampleConfigurer sc) throws OperatorException {

        for (int i = 0; i < viirsWavelengths.length; i++) {
            int wl = viirsWavelengths[i];
            if (outputAsRrs) {
                sc.defineSample(REFLEC_1_IX + i, "rrs_" + wl);
            } else {
                sc.defineSample(REFLEC_1_IX + i, "rhow_" + wl);
            }
        }

        sc.defineSample(IOP_APIG_IX, "iop_apig");
        sc.defineSample(IOP_ADET_IX, "iop_adet");
        sc.defineSample(IOP_AGELB_IX, "iop_agelb");
        sc.defineSample(IOP_BPART_IX, "iop_bpart");
        sc.defineSample(IOP_BWIT_IX, "iop_bwit");
        sc.defineSample(RTOSA_RATIO_MIN_IX, "rtosa_ratio_min");
        sc.defineSample(RTOSA_RATIO_MAX_IX, "rtosa_ratio_max");
        sc.defineSample(C2RCC_FLAGS_IX, "c2rcc_flags");

        if (outputRtosa) {
            for (int i = 0; i < viirsWavelengths.length; i++) {
                int wl = viirsWavelengths[i];
                sc.defineSample(RTOSA_IN_1_IX + i, "rtosa_in_" + wl);
            }
            for (int i = 0; i < viirsWavelengths.length; i++) {
                int wl = viirsWavelengths[i];
                sc.defineSample(RTOSA_OUT_1_IX + i, "rtosa_out_" + wl);
            }
        }
    }

    @Override
    protected void configureTargetProduct(ProductConfigurer productConfigurer) {
        super.configureTargetProduct(productConfigurer);
        productConfigurer.copyMetadata();
        Product targetProduct = productConfigurer.getTargetProduct();
        targetProduct.setProductType(PRODUCT_TYPE);
        TargetProductPreparer.prepareTargetProduct(targetProduct, sourceProduct, SOURCE_RADIANCE_NAME_PREFIX, viirsWavelengths,
                                                   outputRtosa, outputAsRrs);
        C2rccCommons.ensureTimeInformation(targetProduct, sourceProduct.getStartTime(), sourceProduct.getEndTime(), timeCoding);
    }

    @Override
    protected void prepareInputs() throws OperatorException {
        for (int i = 0; i < WL_BAND_COUNT; i++) {
            final int wavelength = viirsWavelengths[i];
            assertSourceBand(SOURCE_RADIANCE_NAME_PREFIX + wavelength);
        }
        assertSourceBand(RASTER_NAME_L2_FLAGS);
        assertSourceBandAndRemoveValidExpression(RASTER_NAME_SOLAR_ZENITH);
        assertSourceBandAndRemoveValidExpression(RASTER_NAME_SOLAR_AZIMUTH);
        assertSourceBandAndRemoveValidExpression(RASTER_NAME_VIEW_ZENITH);
        assertSourceBandAndRemoveValidExpression(RASTER_NAME_VIEW_AZIMUTH);

        if (sourceProduct.getSceneGeoCoding() == null) {
            throw new OperatorException("The source product must be geo-coded.");
        }

        try {
            algorithm = new C2rccViirsAlgorithm();
        } catch (IOException e) {
            throw new OperatorException(e);
        }

        algorithm.setTemperature(temperature);
        algorithm.setSalinity(salinity);

        timeCoding = getTimeCoding(sourceProduct);
        initAtmosphericAuxdata();

    }

    public static boolean isValidInput(Product product) {
        for (int i = 0; i < WL_BAND_COUNT; i++) {
            final int wl = viirsWavelengths[i];
            if (!product.containsBand("rhot_" + wl)) {
                return false;
            }
        }
        if (!product.containsBand("l2_flags")) {
            return false;
        }
        if (!product.containsBand("solz")) {
            return false;
        }
        if (!product.containsBand("sola")) {
            return false;
        }
        if (!product.containsBand("senz")) {
            return false;
        }
        if (!product.containsBand("sena")) {
            return false;
        }
        return true;
    }

    private void initAtmosphericAuxdata() {
        AtmosphericAuxdataBuilder auxdataBuilder = new AtmosphericAuxdataBuilder();
        auxdataBuilder.setOzone(ozone);
        auxdataBuilder.setSurfacePressure(press);
        auxdataBuilder.useAtmosphericAuxDataPath(atmosphericAuxDataPath);
        auxdataBuilder.useTomsomiProducts(tomsomiStartProduct, tomsomiEndProduct);
        auxdataBuilder.useNcepProducts(ncepStartProduct, ncepEndProduct);
        try {
            atmosphericAuxdata = auxdataBuilder.create();
        } catch (Exception e) {
            throw new OperatorException("Could not create provider for atmospheric auxdata", e);
        }
    }

    private void assertSourceBandAndRemoveValidExpression(String bandname) {
        assertSourceBand(bandname);
        final Band band = sourceProduct.getBand(bandname);
        band.setValidPixelExpression("");
    }

    private void assertSourceBand(String name) {
        if (sourceProduct.getBand(name) == null) {
            throw new OperatorException("Invalid source product, band '" + name + "' required");
        }
    }

    @Override
    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    @Override
    public void setSalinity(double salinity) {
        this.salinity = salinity;
    }

    @Override
    public void setValidPixelExpression(String validPixelExpression) {
        this.validPixelExpression = validPixelExpression;
    }

    @Override
    public void setOutputRtosa(boolean outputRtosa) {
        this.outputRtosa = outputRtosa;
    }

    @Override
    public void dispose() {
        super.dispose();
        if (atmosphericAuxdata != null) {
            atmosphericAuxdata.dispose();
            atmosphericAuxdata = null;
        }
    }

    public static class Spi extends OperatorSpi {
        static{
            RgbProfiles.installViirsRgbProfiles();
        }

        public Spi() {
            super(C2rccViirsOperator.class);
        }
    }
}
