package org.esa.s3tbx.c2rcc.meris;

import org.esa.s3tbx.c2rcc.C2rccCommons;
import org.esa.s3tbx.c2rcc.ancillary.AtmosphericAuxdataBuilder;
import org.esa.s3tbx.c2rcc.util.NNUtils;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.dataop.dem.ElevationModel;
import org.esa.snap.core.dataop.dem.ElevationModelRegistry;
import org.esa.snap.core.dataop.resamp.Resampling;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.pointop.Sample;
import org.esa.snap.core.gpf.pointop.SourceSampleConfigurer;
import org.esa.snap.core.gpf.pointop.WritableSample;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.converters.BooleanExpressionConverter;

import java.io.IOException;
import java.nio.file.Paths;

import static org.esa.s3tbx.c2rcc.ancillary.AncillaryCommons.fetchOzone;
import static org.esa.s3tbx.c2rcc.ancillary.AncillaryCommons.fetchSurfacePressure;

// todo (nf) - Add min/max values of NN inputs and outputs to metadata (https://github.com/bcdev/s3tbx-c2rcc/issues/3)

/**
 * The Case 2 Regional / CoastColour Operator for MERIS data of the 4th reprocessing.
 * <p/>
 * Computes AC-reflectances and IOPs from OLCI L1b data products using
 * an neural-network approach.
 */
@OperatorMetadata(alias = "c2rcc.meris4", version = "0.14",
        authors = "Roland Doerffer, Marco Peters (Brockmann Consult)",
        category = "Optical Processing/Thematic Water Processing",
        copyright = "Copyright (C) 2016 by Brockmann Consult",
        description = "Performs atmospheric correction and IOP retrieval with uncertainties on MERIS L1b data products from the 4th reprocessing.")
public class C2rccMeris4Operator extends C2rccCommonMerisOp {
    /*
        c2rcc ops have been removed from Graph Builder. In the layer xml they are disabled
        see https://senbox.atlassian.net/browse/SNAP-395
    */

    // MERIS sources
    private static final String RADIANCE_BANDNAME_PATTERN = "M%02d_radiance";
    static final String RASTER_NAME_QUALITY_FLAGS = "quality_flags";
    static final String RASTER_NAME_SUN_ZENITH = "SZA";
    static final String RASTER_NAME_SUN_AZIMUTH = "SAA";
    static final String RASTER_NAME_VIEWING_ZENITH = "OZA";
    static final String RASTER_NAME_VIEWING_AZIMUTH = "OAA";
    static final String RASTER_NAME_ALTITUDE = "altitude";

    private static final int RADIANCE_START_IX = 0;
    private static final int SOLAR_FLUX_START_IX = BAND_COUNT;
    private static final int DEM_ALT_IX = BAND_COUNT * 2;
    private static final int SUN_ZEN_IX = DEM_ALT_IX + 1;
    private static final int SUN_AZI_IX = DEM_ALT_IX + 2;
    private static final int VIEW_ZEN_IX = DEM_ALT_IX + 3;
    private static final int VIEW_AZI_IX = DEM_ALT_IX + 4;
    private static final int VALID_PIXEL_IX = DEM_ALT_IX + 5;

    private static final String SOLAR_FLUX_BANDNAME_PATTERN = "solar_flux_band_%d";

    @SourceProduct(label = "MERIS L1b 4th Reproc. product", description = "MERIS L1b source product.")
    private Product sourceProduct;

    @SourceProduct(description = "A second source product which is congruent to the L1b source product but contains cloud flags. " +
            "So the user can define a valid pixel expression referring both, the L1b and the cloud flag " +
            "containing source product. Expression example: '!quality_flags.invalid && !quality_flags.land && !$cloudProduct.l2_flags.CLOUD' ",
            optional = true,
            label = "Product with cloud flag")
    private Product cloudProduct;

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
            defaultValue = "!quality_flags.invalid && (!quality_flags.land || quality_flags.fresh_inland_water)",
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

    @Parameter(defaultValue = "1000", unit = "hPa", interval = "(800, 1040)", label = "Air Pressure",
            description = "The value used as air pressure if not provided by auxiliary data")
    private double press;

    @Parameter(defaultValue = "1.72", description = "Conversion factor bpart. (TSM = bpart * TSMfakBpart + bwit * TSMfakBwit)", label = "TSM factor bpart")
    private double TSMfakBpart;

    @Parameter(defaultValue = "3.1", description = "Conversion factor bwit. (TSM = bpart * TSMfakBpart + bwit * TSMfakBwit)", label = "TSM factor bwit")
    private double TSMfakBwit;

    @Parameter(defaultValue = "1.04", description = "Chlorophyl exponent ( CHL = iop-apig^CHLexp * CHLfak ) ", label = "CHL exponent")
    private double CHLexp;

    @Parameter(defaultValue = "21.0", description = "Chlorophyl factor ( CHL = iop-apig^CHLexp * CHLfak ) ", label = "CHL factor")
    private double CHLfak;

    @Parameter(defaultValue = "0.05", description = "Threshold for out of scope of nn training dataset flag for gas corrected top-of-atmosphere reflectances",
            label = "Threshold rtosa OOS")
    private double thresholdRtosaOOS;

    @Parameter(defaultValue = "0.1", description = "Threshold for out of scope of nn training dataset flag for atmospherically corrected reflectances",
            label = "Threshold AC reflectances OOS")
    private double thresholdAcReflecOos;

    @Parameter(defaultValue = "0.955", description = "Threshold for cloud test based on downwelling transmittance @865",
            label = "Threshold for cloud flag on transmittance down @865")
    private double thresholdCloudTDown865;

    @Parameter(description = "Path to the atmospheric auxiliary data directory. Use either this or the specific products. " +
            "If the auxiliary data needed for interpolation is not available in this path, the data will automatically downloaded.")
    private String atmosphericAuxDataPath;

    @Parameter(description = "Path to an alternative set of neuronal nets. Use this to replace the standard " +
            "set of neuronal nets with the ones in the given directory.",
            label = "Alternative NN Path")
    private String alternativeNNPath;

    private final String[] availableNetSets = new String[]{"C2RCC-Nets", "C2X-Nets"};

    @Parameter(valueSet = {"C2RCC-Nets", "C2X-Nets"},
            description = "Set of neuronal nets for algorithm.",
            defaultValue = "C2RCC-Nets",
            label = "Set of neuronal nets")
    private String netSet = "C2RCC-Nets";

    @Parameter(defaultValue = "false", description =
            "Reflectance values in the target product shall be either written as remote sensing or water leaving reflectances",
            label = "Output AC reflectances as rrs instead of rhow")
    protected boolean outputAsRrs;

    @Parameter(defaultValue = "true", description =
            "If selected, the ECMWF auxiliary data (total_ozone, sea_level_pressure) of the source product is used",
            label = "Use ECMWF aux data of source product")
    protected boolean useEcmwfAuxData;

    @Parameter(defaultValue = "true", label = "Output TOA reflectances")
    protected boolean outputRtoa;

    @Parameter(defaultValue = "false", label = "Output gas corrected TOSA reflectances")
    protected boolean outputRtosaGc;

    @Parameter(defaultValue = "false", label = "Output gas corrected TOSA reflectances of auto nn")
    protected boolean outputRtosaGcAann;

    @Parameter(defaultValue = "false", label = "Output path radiance reflectances")
    protected boolean outputRpath;

    @Parameter(defaultValue = "false", label = "Output downward transmittance")
    protected boolean outputTdown;

    @Parameter(defaultValue = "false", label = "Output upward transmittance")
    protected boolean outputTup;

    @Parameter(defaultValue = "true", label = "Output atmospherically corrected angular dependent reflectances")
    protected boolean outputAcReflectance;

    @Parameter(defaultValue = "true", label = "Output normalized water leaving reflectances")
    protected boolean outputRhown;

    @Parameter(defaultValue = "false", label = "Output of out of scope values")
    protected boolean outputOos;

    @Parameter(defaultValue = "true", label = "Output of irradiance attenuation coefficients")
    protected boolean outputKd;

    @Parameter(defaultValue = "true", label = "Output uncertainties")
    protected boolean outputUncertainties;

    private boolean useSnapDem;
    private ElevationModel elevationModel;

    public static boolean isValidInput(Product product) {
        for (int i = 1; i <= BAND_COUNT; i++) {
            if (!product.containsBand(String.format(RADIANCE_BANDNAME_PATTERN, i))
                    || !product.containsBand(getSolarFluxBandname(i))) {
                return false;
            }
        }

        return product.containsBand(RASTER_NAME_QUALITY_FLAGS)
                && product.containsRasterDataNode(RASTER_NAME_SUN_ZENITH)
                && product.containsRasterDataNode(RASTER_NAME_SUN_AZIMUTH)
                && product.containsRasterDataNode(RASTER_NAME_VIEWING_ZENITH)
                && product.containsRasterDataNode(RASTER_NAME_VIEWING_AZIMUTH);
    }

    @Override
    public void setAtmosphericAuxDataPath(String atmosphericAuxDataPath) {
        this.atmosphericAuxDataPath = atmosphericAuxDataPath;
    }

    @Override
    public String getAtmosphericAuxDataPath() {
        return atmosphericAuxDataPath;
    }

    @Override
    public void setTomsomiStartProduct(Product tomsomiStartProduct) {
        this.tomsomiStartProduct = tomsomiStartProduct;
    }

    @Override
    public Product getTomsomiStartProduct() {
        return tomsomiStartProduct;
    }

    @Override
    public void setTomsomiEndProduct(Product tomsomiEndProduct) {
        this.tomsomiEndProduct = tomsomiEndProduct;
    }

    @Override
    public Product getTomsomiEndProduct() {
        return tomsomiEndProduct;
    }

    @Override
    public void setNcepStartProduct(Product ncepStartProduct) {
        this.ncepStartProduct = ncepStartProduct;
    }

    @Override
    public Product getNcepStartProduct() {
        return ncepStartProduct;
    }

    @Override
    public void setNcepEndProduct(Product ncepEndProduct) {
        this.ncepEndProduct = ncepEndProduct;
    }

    @Override
    public Product getNcepEndProduct() {
        return ncepEndProduct;
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
    public void setOzone(double ozone) {
        this.ozone = ozone;
    }

    @Override
    public double getOzone() {
        return ozone;
    }

    @Override
    public void setPress(double press) {
        this.press = press;
    }

    @Override
    public double getPress() {
        return press;
    }

    @Override
    public double getTSMfakBpart() {
        return TSMfakBpart;
    }

    @Override
    public double getTSMfakBwit() {
        return TSMfakBwit;
    }

    @Override
    public double getCHLexp() {
        return CHLexp;
    }

    @Override
    public double getCHLfak() {
        return CHLfak;
    }

    @Override
    public void setUseEcmwfAuxData(boolean useEcmwfAuxData) {
        this.useEcmwfAuxData = useEcmwfAuxData;
    }

    @Override
    public void setOutputRtosa(boolean outputRtosa) {
        this.outputRtosaGc = outputRtosa;
    }

    @Override
    public void setOutputAsRrs(boolean asRadianceRefl) {
        this.outputAsRrs = asRadianceRefl;
    }

    @Override
    protected boolean getUseEcmwfAuxData() {
        return useEcmwfAuxData;
    }

    @Override
    protected void setOutputKd(boolean outputKd) {
        this.outputKd = outputKd;
    }

    @Override
    protected boolean getOutputKd() {
        return outputKd;
    }

    @Override
    protected void setOutputOos(boolean outputOos) {
        this.outputOos = outputOos;
    }

    @Override
    protected boolean getOutputOos() {
        return outputOos;
    }

    @Override
    protected void setOutputRpath(boolean outputRpath) {
        this.outputRpath = outputRpath;
    }

    @Override
    protected boolean getOutputRpath() {
        return outputRpath;
    }

    @Override
    protected void setOutputRtoa(boolean outputRtoa) {
        this.outputRtoa = outputRtoa;
    }

    @Override
    protected boolean getOutputRtoa() {
        return outputRtoa;
    }

    @Override
    protected boolean getOutputRtosa() {
        return outputRtosaGc;
    }

    @Override
    protected void setOutputRtosaGcAann(boolean outputRtosaGcAann) {
        this.outputRtosaGcAann = outputRtosaGcAann;
    }

    @Override
    protected boolean getOutputRtosaGcAann() {
        return outputRtosaGcAann;
    }

    @Override
    protected void setOutputAcReflec(boolean outputAcReflec) {
        this.outputAcReflectance = outputAcReflec;
    }

    @Override
    protected boolean getOutputAcReflec() {
        return outputAcReflectance;
    }

    @Override
    protected void setOutputRhown(boolean outputRhown) {
        this.outputRhown = outputRhown;
    }

    @Override
    protected boolean getOutputRhown() {
        return outputRhown;
    }

    @Override
    protected void setOutputTdown(boolean outputTdown) {
        this.outputTdown = outputTdown;
    }

    @Override
    protected boolean getOutputTdown() {
        return this.outputTdown;
    }

    @Override
    protected void setOutputTup(boolean outputTup) {
        this.outputTup = outputTup;
    }

    @Override
    protected boolean getOutputTup() {
        return outputTup;
    }

    @Override
    protected void setOutputUncertainties(boolean outputUncertainties) {
        this.outputUncertainties = outputUncertainties;
    }

    @Override
    protected boolean getOutputUncertainties() {
        return outputUncertainties;
    }

    @Override
    protected boolean getOutputAsRrs() {
        return outputAsRrs;
    }

    @Override
    public void setValidPixelExpression(String validPixelExpression) {
        this.validPixelExpression = validPixelExpression;
    }


    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {
        boolean samplesValid = C2rccCommons.areSamplesValid(sourceSamples, x, y);
        if (!samplesValid) {
            setInvalid(targetSamples);
            return;
        }

        final double[] radiances = new double[BAND_COUNT];
        final double[] solflux = new double[BAND_COUNT];
        for (int i = 0; i < BAND_COUNT; i++) {
            radiances[i] = sourceSamples[i].getDouble();
            solflux[i] = sourceSamples[i + SOLAR_FLUX_START_IX].getDouble();
        }

        final PixelPos pixelPos = new PixelPos(x + 0.5f, y + 0.5f);
        final double mjd = timeCoding.getMJD(pixelPos);

        GeoPos geoPos = sourceProduct.getSceneGeoCoding().getGeoPos(pixelPos, null);
        double lat = geoPos.getLat();
        double lon = geoPos.getLon();
        double atmPress = fetchSurfacePressure(atmosphericAuxdata, mjd, x, y, lat, lon);
        double ozone = fetchOzone(atmosphericAuxdata, mjd, x, y, lat, lon);
        final double altitude;
        if (useSnapDem) {
            try {
                altitude = elevationModel.getElevation(geoPos);
            } catch (Exception e) {
                throw new OperatorException("Unable to compute altitude.", e);
            }
        } else {
            altitude = sourceSamples[DEM_ALT_IX].getDouble();
        }

        C2rccMerisAlgorithm.Result result = algorithm.processPixel(x, y, lat, lon,
                                                                   radiances,
                                                                   solflux,
                                                                   sourceSamples[SUN_ZEN_IX].getDouble(),
                                                                   sourceSamples[SUN_AZI_IX].getDouble(),
                                                                   sourceSamples[VIEW_ZEN_IX].getDouble(),
                                                                   sourceSamples[VIEW_AZI_IX].getDouble(),
                                                                   altitude,
                                                                   sourceSamples[VALID_PIXEL_IX].getBoolean(),
                                                                   atmPress,
                                                                   ozone);

        fillTargetSamples(targetSamples, result);
    }

    @Override
    protected void configureSourceSamples(SourceSampleConfigurer sc) throws OperatorException {
        for (int i = 0; i < BAND_COUNT; i++) {
            sc.defineSample(i + RADIANCE_START_IX, getRadianceBandName(i + 1));
            sc.defineSample(i + SOLAR_FLUX_START_IX, getSolarFluxBandname(i + 1));
        }
        if (!useSnapDem) {
            sc.defineSample(DEM_ALT_IX, RASTER_NAME_ALTITUDE);
        }
        sc.defineSample(SUN_ZEN_IX, RASTER_NAME_SUN_ZENITH);
        sc.defineSample(SUN_AZI_IX, RASTER_NAME_SUN_AZIMUTH);
        sc.defineSample(VIEW_ZEN_IX, RASTER_NAME_VIEWING_ZENITH);
        sc.defineSample(VIEW_AZI_IX, RASTER_NAME_VIEWING_AZIMUTH);
        if (StringUtils.isNotNullAndNotEmpty(validPixelExpression)) {
            sc.defineComputedSample(VALID_PIXEL_IX, ProductData.TYPE_UINT8, validPixelExpression);
        } else {
            sc.defineComputedSample(VALID_PIXEL_IX, ProductData.TYPE_UINT8, "true");
        }

    }

    @Override
    protected void prepareInputs() throws OperatorException {
        for (int i = 1; i <= BAND_COUNT; i++) {
            assertSourceBand(getRadianceBandName(i));
            assertSourceBand(getSolarFluxBandname(i));
        }
        useSnapDem = !sourceProduct.containsRasterDataNode(RASTER_NAME_ALTITUDE);
        if (useSnapDem) {
            elevationModel = ElevationModelRegistry.getInstance().getDescriptor("GETASSE30").createDem(Resampling.BILINEAR_INTERPOLATION);
        }

        sourceProduct.isCompatibleBandArithmeticExpression(validPixelExpression);

        if (sourceProduct.getSceneGeoCoding() == null) {
            throw new OperatorException("The source product must be geo-coded.");
        }

        assertSourceRaster(RASTER_NAME_SUN_ZENITH);
        assertSourceRaster(RASTER_NAME_SUN_AZIMUTH);
        assertSourceRaster(RASTER_NAME_VIEWING_ZENITH);
        assertSourceRaster(RASTER_NAME_VIEWING_AZIMUTH);

        try {
            final String[] nnFilePaths;
            final boolean loadFromResources = StringUtils.isNullOrEmpty(alternativeNNPath);
            if (loadFromResources) {
                if (availableNetSets[0].equalsIgnoreCase(netSet)) {
                    nnFilePaths = c2rccNNResourcePaths;
                } else {
                    nnFilePaths = c2xNNResourcePaths;
                }
            } else {
                nnFilePaths = NNUtils.getNNFilePaths(Paths.get(alternativeNNPath), alternativeNetDirNames);
            }
            algorithm = new C2rccMerisAlgorithm(nnFilePaths, loadFromResources);
        } catch (IOException e) {
            throw new OperatorException(e);
        }

        algorithm.setTemperature(temperature);
        algorithm.setSalinity(salinity);
        algorithm.setThresh_absd_log_rtosa(thresholdRtosaOOS);
        algorithm.setThresh_rwlogslope(thresholdAcReflecOos);
        algorithm.setThresh_cloudTransD(thresholdCloudTDown865);

        algorithm.setOutputRtosaGcAann(outputRtosaGcAann);
        algorithm.setOutputRpath(outputRpath);
        algorithm.setOutputTdown(outputTdown);
        algorithm.setOutputTup(outputTup);
        algorithm.setOutputRhow(outputAcReflectance);
        algorithm.setOutputRhown(outputRhown);
        algorithm.setOutputOos(outputOos);
        algorithm.setOutputKd(outputKd);
        algorithm.setOutputUncertainties(outputUncertainties);

        timeCoding = C2rccCommons.getTimeCoding(sourceProduct);
        initAtmosphericAuxdata();
    }

    @Override
    protected String getRadianceBandName(int index) {
        return String.format(RADIANCE_BANDNAME_PATTERN, index);
    }

    private static String getSolarFluxBandname(int index) {
        return String.format(SOLAR_FLUX_BANDNAME_PATTERN, index);
    }

    @Override
    protected RasterDataNode getPressureRaster() {
        return sourceProduct.getRasterDataNode(RASTER_NAME_SEA_LEVEL_PRESSURE);
    }

    @Override
    protected RasterDataNode getOzoneRaster() {
        VirtualBand ozoneInDu = new VirtualBand("__ozone_in_du_",
                                                ProductData.TYPE_FLOAT32,
                                                getSourceProduct().getSceneRasterWidth(),
                                                getSourceProduct().getSceneRasterHeight(),
                                                RASTER_NAME_TOTAL_OZONE + " * 46698");
        ozoneInDu.setOwner(sourceProduct);
        return ozoneInDu;
    }

    private void assertSourceRaster(String name) {
        if (!sourceProduct.containsRasterDataNode(name)) {
            throw new OperatorException("Invalid source product, raster '" + name + "' required");
        }
    }

    private void assertSourceBand(String name) {
        if (!sourceProduct.containsBand(name)) {
            throw new OperatorException("Invalid source product, band '" + name + "' required");
        }
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(C2rccMeris4Operator.class);
        }
    }
}
