package org.esa.s3tbx.c2rcc.olci;

import org.esa.s3tbx.c2rcc.C2rccCommons;
import org.esa.s3tbx.c2rcc.C2rccConfigurable;
import org.esa.s3tbx.c2rcc.ancillary.AtmosphericAuxdata;
import org.esa.s3tbx.c2rcc.ancillary.AtmosphericAuxdataBuilder;
import org.esa.s3tbx.c2rcc.olci.C2rccOlciAlgorithm.Result;
import org.esa.s3tbx.c2rcc.util.NNUtils;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.datamodel.ProductNodeEvent;
import org.esa.snap.core.datamodel.ProductNodeListener;
import org.esa.snap.core.datamodel.ProductNodeListenerAdapter;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.dataop.dem.ElevationModel;
import org.esa.snap.core.dataop.dem.ElevationModelRegistry;
import org.esa.snap.core.dataop.resamp.Resampling;
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
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.converters.BooleanExpressionConverter;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Paths;

import static org.esa.s3tbx.c2rcc.ancillary.AncillaryCommons.fetchOzone;
import static org.esa.s3tbx.c2rcc.ancillary.AncillaryCommons.fetchSurfacePressure;
import static org.esa.s3tbx.c2rcc.olci.C2rccOlciAlgorithm.DEFAULT_OLCI_WAVELENGTH;
import static org.esa.s3tbx.c2rcc.olci.C2rccOlciAlgorithm.IDX_iop_rw;
import static org.esa.s3tbx.c2rcc.olci.C2rccOlciAlgorithm.IDX_iop_unciop;
import static org.esa.s3tbx.c2rcc.olci.C2rccOlciAlgorithm.IDX_iop_uncsumiop_unckd;
import static org.esa.s3tbx.c2rcc.olci.C2rccOlciAlgorithm.IDX_rtosa_aann;
import static org.esa.s3tbx.c2rcc.olci.C2rccOlciAlgorithm.IDX_rtosa_rpath;
import static org.esa.s3tbx.c2rcc.olci.C2rccOlciAlgorithm.IDX_rtosa_rw;
import static org.esa.s3tbx.c2rcc.olci.C2rccOlciAlgorithm.IDX_rtosa_trans;
import static org.esa.s3tbx.c2rcc.olci.C2rccOlciAlgorithm.IDX_rw_iop;
import static org.esa.s3tbx.c2rcc.olci.C2rccOlciAlgorithm.IDX_rw_kd;
import static org.esa.s3tbx.c2rcc.olci.C2rccOlciAlgorithm.IDX_rw_rwnorm;
import static org.esa.s3tbx.c2rcc.olci.C2rccOlciAlgorithm.olciband16_ix;
import static org.esa.s3tbx.c2rcc.olci.C2rccOlciAlgorithm.olciband21_ix;

// todo (nf) - Add min/max values of NN inputs and outputs to metadata (https://github.com/bcdev/s3tbx-c2rcc/issues/3)

/**
 * The Case 2 Regional / CoastColour Operator for OLCI.
 * <p/>
 * Computes AC-reflectances and IOPs from OLCI L1b data products using
 * an neural-network approach.
 *
 * @author Norman Fomferra
 */
@OperatorMetadata(alias = "olci.c2rcc", version = "0.9.6",
            authors = "Roland Doerffer, Sabine Embacher (Brockmann Consult)",
            category = "Optical Processing/Thematic Water Processing",
            copyright = "Copyright (C) 2015 by Brockmann Consult",
            description = "Performs atmospheric correction and IOP retrieval with uncertainties on OLCI L1b data products.")
public class C2rccOlciOperator extends PixelOperator implements C2rccConfigurable {
    /*
        c2rcc ops have been removed from Graph Builder. In the layer xml they are disabled
        see https://senbox.atlassian.net/browse/SNAP-395
    */

    // OLCI sources
    public static final int BAND_COUNT = 21;
    public static final int RADIANCE_START_IX = 0;
    public static final int SOLAR_FLUX_START_IX = BAND_COUNT;
    public static final int DEM_ALT_IX = BAND_COUNT * 2;
    public static final int SUN_ZEN_IX = DEM_ALT_IX + 1;
    public static final int SUN_AZI_IX = DEM_ALT_IX + 2;
    public static final int VIEW_ZEN_IX = DEM_ALT_IX + 3;
    public static final int VIEW_AZI_IX = DEM_ALT_IX + 4;
    public static final int VALID_PIXEL_IX = DEM_ALT_IX + 5;

    // OLCI targets
    public static final int BC_16 = olciband16_ix.length; // Band count 16
    public static final int BC_21 = olciband21_ix.length; // Band count 21
    public static final int SINGLE_IX = BC_21 + 7 * BC_16;

    public static final int RTOA_IX = 0;
    public static final int RTOSA_IX = BC_21;
    public static final int RTOSA_AANN_IX = BC_21 + BC_16;
    public static final int RPATH_IX = BC_21 + 2 * BC_16;
    public static final int TDOWN_IX = BC_21 + 3 * BC_16;
    public static final int TUP_IX = BC_21 + 4 * BC_16;
    public static final int AC_REFLEC_IX = BC_21 + 5 * BC_16;
    public static final int RHOWN_IX = BC_21 + 6 * BC_16;

    public static final int OOS_RTOSA_IX = SINGLE_IX;
    public static final int OOS_AC_REFLEC_IX = SINGLE_IX + 1;

    public static final int IOP_APIG_IX = SINGLE_IX + 2;
    public static final int IOP_ADET_IX = SINGLE_IX + 3;
    public static final int IOP_AGELB_IX = SINGLE_IX + 4;
    public static final int IOP_BPART_IX = SINGLE_IX + 5;
    public static final int IOP_BWIT_IX = SINGLE_IX + 6;

    //    public static final int IOP_ADG_IX = SINGLE_IX + 7;  // virtual band
//    public static final int IOP_ATOT_IX = SINGLE_IX + 8;  // virtual band
//    public static final int IOP_BTOT_IX = SINGLE_IX + 9;  // virtual band
    public static final int KD489_IX = SINGLE_IX + 7;
    public static final int KDMIN_IX = SINGLE_IX + 8;
//    public static final int KD_Z90MAX_IX = SINGLE_IX + 12;  // virtual band
//    public static final int CONC_CHL_IX = SINGLE_IX + 13;  // virtual band
//    public static final int CONC_TSM_IX = SINGLE_IX + 14;  // virtual band

    public static final int UNC_APIG_IX = SINGLE_IX + 9;
    public static final int UNC_ADET_IX = SINGLE_IX + 10;
    public static final int UNC_AGELB_IX = SINGLE_IX + 11;
    public static final int UNC_BPART_IX = SINGLE_IX + 12;
    public static final int UNC_BWIT_IX = SINGLE_IX + 13;
    public static final int UNC_ADG_IX = SINGLE_IX + 14;
    public static final int UNC_ATOT_IX = SINGLE_IX + 15;
    public static final int UNC_BTOT_IX = SINGLE_IX + 16;
    //    public static final int UNC_TSM_IX = SINGLE_IX + 24;  // virtual band
//    public static final int UNC_CHL_IX = SINGLE_IX + 23;  // virtual band
    public static final int UNC_KD489_IX = SINGLE_IX + 17;
    public static final int UNC_KDMIN_IX = SINGLE_IX + 18;
//    public static final int UNC_Z90MAX_IX = SINGLE_IX + 27;  // virtual band

    public static final int C2RCC_FLAGS_IX = SINGLE_IX + 19;


    public static final String[] alternativeNetDirNames = new String[]{
            "rtosa_aann",
            "rtosa_rw",
            "rw_iop",
            "iop_rw",
            "rw_kd",
            "iop_unciop",
            "iop_uncsumiop_unckd",
            "rw_rwnorm",
            "rtosa_trans",
            "rtosa_rpath"
    };

    public static final String[] c2rccNNResourcePaths = new String[10];
    public static final String RASTER_NAME_QUALITY_FLAGS = "quality_flags";
    public static final String RASTER_NAME_SEA_LEVEL_PRESSURE = "sea_level_pressure";
    public static final String RASTER_NAME_TOTAL_OZONE = "total_ozone";
    public static final String RASTER_NAME_SUN_ZENITH = "SZA";
    public static final String RASTER_NAME_SUN_AZIMUTH = "SAA";
    public static final String RASTER_NAME_VIEWING_ZENITH = "OZA";
    public static final String RASTER_NAME_VIEWING_AZIMUTH = "OAA";
    public static final String RASTER_NAME_ALTITUDE = "altitude";

    static {
        c2rccNNResourcePaths[IDX_iop_rw] = "olci/iop_rw/17x97x47_464.3.net";
        c2rccNNResourcePaths[IDX_iop_unciop] = "olci/iop_unciop/17x77x37_11486.7.net";
        c2rccNNResourcePaths[IDX_iop_uncsumiop_unckd] = "olci/iop_uncsumiop_unckd/17x77x37_9113.1.net";
        c2rccNNResourcePaths[IDX_rtosa_aann] = "olci/rtosa_aann/31x7x31_1078.2.net";
        c2rccNNResourcePaths[IDX_rtosa_rpath] = "olci/rtosa_rpath/31x77x57x37_2430.4.net";
        c2rccNNResourcePaths[IDX_rtosa_rw] = "olci/rtosa_rw/33x73x53x33_930865.5.net";
        c2rccNNResourcePaths[IDX_rtosa_trans] = "olci/rtosa_trans/31x77x57x37_30152.6.net";
        c2rccNNResourcePaths[IDX_rw_iop] = "olci/rw_iop/97x77x37_20196.0.net";
        c2rccNNResourcePaths[IDX_rw_kd] = "olci/rw_kd/97x77x7_389.5.net";
        c2rccNNResourcePaths[IDX_rw_rwnorm] = "olci/rw_rwnorm/37x57x17_69.1.net";
    }

    @SourceProduct(label = "OLCI L1b product", description = "OLCI L1b source product.")
    private Product sourceProduct;

    @SourceProduct(description = "A second source product which is congruent to the L1b source product but contains cloud flags. " +
            "So the user can define a valid pixel expression referring both, the L1b and the cloud flag " +
            "containing source product. Expression example: '!l1_flags.INVALID && !l1_flags.LAND_OCEAN! && !$cloudProduct.l2_flags.CLOUD' ",
            optional = true,
            label = "Product with cloud flag")
    private Product cloudProduct;

    @SourceProduct(description = "The first product providing ozone values for ozone interpolation. " +
            "Use either this in combination with other start- and end-products (tomsomiEndProduct, " +
            "ncepStartProduct, ncepEndProduct) or atmosphericAuxdataPath to use ozone and air pressure " +
            "auxiliary data for calculations.",
            optional = true,
            label = "Ozone interpolation start product (TOMSOMI)")
    private Product tomsomiStartProduct;

    @SourceProduct(description = "The second product providing ozone values for ozone interpolation. " +
            "Use either this in combination with other start- and end-products (tomsomiStartProduct, " +
            "ncepStartProduct, ncepEndProduct) or atmosphericAuxdataPath to use ozone and air pressure " +
            "auxiliary data for calculations.",
            optional = true,
            label = "Ozone interpolation end product (TOMSOMI)")
    private Product tomsomiEndProduct;

    @SourceProduct(description = "The first product providing air pressure values for pressure interpolation. " +
            "Use either this in combination with other start- and end-products (tomsomiStartProduct, " +
            "tomsomiEndProduct, ncepEndProduct) or atmosphericAuxdataPath to use ozone and air pressure " +
            "auxiliary data for calculations.",
            optional = true,
            label = "Air pressure interpolation start product (NCEP)")
    private Product ncepStartProduct;

    @SourceProduct(description = "The second product providing air pressure values for pressure interpolation. " +
            "Use either this in combination with other start- and end-products (tomsomiStartProduct, " +
            "tomsomiEndProduct, ncepStartProduct) or atmosphericAuxdataPath to use ozone and air pressure " +
            "auxiliary data for calculations.",
            optional = true,
            label = "Air pressure interpolation end product (NCEP)")
    private Product ncepEndProduct;

    @Parameter(label = "Valid-pixel expression",
            defaultValue = "!quality_flags.invalid && !quality_flags.land",
            converter = BooleanExpressionConverter.class)
    private String validPixelExpression;

    @Parameter(defaultValue = "35.0", unit = "PSU", interval = "(0.1, 43)")
    private double salinity;

    @Parameter(defaultValue = "15.0", unit = "C", interval = "(0.1, 36)")
    private double temperature;

    @Parameter(defaultValue = "330", unit = "DU", interval = "(0, 1000)")
    private double ozone;

    @Parameter(defaultValue = "1000", unit = "hPa", interval = "(800, 1040)", label = "Air Pressure")
    private double press;

    @Parameter(defaultValue = "1.72", description = "Conversion factor bpart. (TSM = bpart * TSMfakBpart + bwit * TSMfakBwit)", label = "TSM factor bpart")
    private double TSMfakBpart;

    @Parameter(defaultValue = "6.2", description = "Conversion factor bwit. (TSM = bpart * TSMfakBpart + bwit * TSMfakBwit)", label = "TSM factor bwit")
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

    @Parameter(description = "Path to the atmospheric auxiliary data directory. Use either this or tomsomiStartProduct, " +
            "tomsomiEndProduct, ncepStartProduct and ncepEndProduct to use ozone and air pressure aux data " +
            "for calculations. If the auxiliary data needed for interpolation not available in this " +
            "path, the data will automatically downloaded.")
    private String atmosphericAuxDataPath;

    @Parameter(description = "Path to an alternative set of neuronal nets. Use this to replace the standard set of neuronal nets with the nets " +
            "available in the given directory. The directory must strictly be organized in the following way to be a valid set " +
            "of neuronal nets. The path must contain the subdirectories 'rtosa_aann', 'rtosa_rw', 'rw_iop', 'iop_rw', 'rw_kd', " +
            "'iop_unciop', 'iop_uncsumiop_unckd', 'rw_rwnorm', 'rtosa_trans', 'rtosa_rpath' and inside the subdirectories " +
            "only one *.net file.",
            label = "Alternative NN Path")
    private String alternativeNNPath;

    @Parameter(defaultValue = "false", description =
            "Reflectance values in the target product shall be either written as remote sensing or water leaving reflectances",
            label = "Output AC reflectances as remote sensing or water leaving reflectances")
    private boolean outputAsRrs;


//    private final String[] availableNetSets = new String[]{"C2RCC-Nets", "C2X-Nets"};
//    @Parameter(valueSet = {"C2RCC-Nets", "C2X-Nets"},
//            description = "Set of neuronal nets for algorithm.",
//            defaultValue = "C2RCC-Nets",
//            label = "Set of neuronal nets")
//    private String netSet = "C2RCC-Nets";

    // @todo discuss with Carsten and Roland
//    @Parameter(defaultValue = "false")
//    private boolean useDefaultSolarFlux;

    @Parameter(defaultValue = "true", description =
            "If selected, the ECMWF auxiliary data (total_ozone, sea_level_pressure) of the source product is used",
            label = "Use ECMWF aux data of source product")
    private boolean useEcmwfAuxData;

    @Parameter(defaultValue = "false", label = "Output top-of-atmosphere (TOA) reflectances")
    private boolean outputRtoa;

    @Parameter(defaultValue = "false", label = "Output gas corrected top-of-atmosphere (TOSA) reflectances")
    private boolean outputRtosaGc;

    @Parameter(defaultValue = "false", label = "Output of auto nn, reflectances")
    private boolean outputRtosaGcAann;

    @Parameter(defaultValue = "false", label = "Output path radiance reflectances")
    private boolean outputRpath;

    @Parameter(defaultValue = "false", label = "Output downward transmittance")
    private boolean outputTdown;

    @Parameter(defaultValue = "false", label = "Output upward transmittance")
    private boolean outputTup;

    @Parameter(defaultValue = "true", label = "Output atmospherically corrected angular dependent reflectances")
    private boolean outputAcReflectance;

    @Parameter(defaultValue = "true", label = "Output normalized water leaving reflectances")
    private boolean outputRhown;

    @Parameter(defaultValue = "false", label = "Output of out of scope values")
    private boolean outputOos;

    @Parameter(defaultValue = "true", label = "Output of irradiance attenuation coefficients")
    private boolean outputKd;

    @Parameter(defaultValue = "false", label = "Output uncertainties")
    private boolean outputUncertainties;

    private C2rccOlciAlgorithm algorithm;
    // @todo discuss with Carsten and Roland
//    private SolarFluxLazyLookup solarFluxLazyLookup;
//    private double[] constantSolarFlux;
    private AtmosphericAuxdata atmosphericAuxdata;
    private boolean useSnapDem;
    private ElevationModel elevationModel;

    public static boolean isValidInput(Product product) {
        for (int i = 0; i < BAND_COUNT; i++) {
            if (!product.containsBand(getRadianceBandName(i))
                    || !product.containsBand(getSolarFluxBandname(i))) {
                return false;
            }
        }

        return product.containsBand(RASTER_NAME_QUALITY_FLAGS)
                && product.containsRasterDataNode(RASTER_NAME_SUN_ZENITH)
                && product.containsRasterDataNode(RASTER_NAME_SUN_ZENITH)
                && product.containsRasterDataNode(RASTER_NAME_SUN_ZENITH)
                && product.containsRasterDataNode(RASTER_NAME_SUN_ZENITH);
    }

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
    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    @Override
    public void setSalinity(double salinity) {
        this.salinity = salinity;
    }

    // @todo discuss with Carsten and Roland
//    public void setUseDefaultSolarFlux(boolean useDefaultSolarFlux) {
//        this.useDefaultSolarFlux = useDefaultSolarFlux;
//    }

    @Override
    public void setOzone(double ozone) {
        this.ozone = ozone;
    }

    @Override
    public void setPress(double press) {
        this.press = press;
    }

    public void setUseEcmwfAuxData(boolean useEcmwfAuxData) {
        this.useEcmwfAuxData = useEcmwfAuxData;
    }

    @Override
    public void setValidPixelExpression(String validPixelExpression) {
        this.validPixelExpression = validPixelExpression;
    }

    @Override
    public void setOutputRtosa(boolean outputRtosa) {
        this.outputRtosaGc = outputRtosa;
    }

    @Override
    public void setOutputAsRrs(boolean asRrs) {
        outputAsRrs = asRrs;
    }

    public void setOutputKd(boolean outputKd) {
        this.outputKd = outputKd;
    }

    public void setOutputOos(boolean outputOos) {
        this.outputOos = outputOos;
    }

    public void setOutputRpath(boolean outputRpath) {
        this.outputRpath = outputRpath;
    }

    public void setOutputRtoa(boolean outputRtoa) {
        this.outputRtoa = outputRtoa;
    }

    public void setOutputRtosaGcAann(boolean outputRtosaGcAann) {
        this.outputRtosaGcAann = outputRtosaGcAann;
    }

    public void setOutputAcReflec(boolean outputAcReflec) {
        this.outputAcReflectance = outputAcReflec;
    }

    public void setOutputRhown(boolean outputRhown) {
        this.outputRhown = outputRhown;
    }

    public void setOutputTdown(boolean outputTdown) {
        this.outputTdown = outputTdown;
    }

    public void setOutputTup(boolean outputTup) {
        this.outputTup = outputTup;
    }

    public void setOutputUncertainties(boolean outputUncertainties) {
        this.outputUncertainties = outputUncertainties;
    }

    @Override
    public void dispose() {
        super.dispose();
        if (atmosphericAuxdata != null) {
            atmosphericAuxdata.dispose();
            atmosphericAuxdata = null;
        }
    }

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {

        final double[] radiances = new double[BAND_COUNT];
        // @todo discuss with Carsten and Roland
        final double[] solflux = new double[BAND_COUNT];
        for (int i = 0; i < BAND_COUNT; i++) {
            radiances[i] = sourceSamples[i].getDouble();
            solflux[i] = sourceSamples[i + SOLAR_FLUX_START_IX].getDouble();
        }

        final PixelPos pixelPos = new PixelPos(x + 0.5f, y + 0.5f);
        final double mjd = sourceProduct.getSceneTimeCoding().getMJD(pixelPos);

        // @todo discuss with Carsten and Roland
//        if (useDefaultSolarFlux) {
//            ProductData.UTC utc = new ProductData.UTC(mjd);
//            Calendar calendar = utc.getAsCalendar();
//            final int doy = calendar.get(Calendar.DAY_OF_YEAR);
//            final int year = calendar.get(Calendar.YEAR);
//            solflux = solarFluxLazyLookup.getCorrectedFluxFor(doy, year);
//        } else {
//        solflux = constantSolarFlux;
//        }

        GeoPos geoPos = sourceProduct.getSceneGeoCoding().getGeoPos(pixelPos, null);
        double lat = geoPos.getLat();
        double lon = geoPos.getLon();
        double atmPress = fetchSurfacePressure(atmosphericAuxdata, mjd, lat, lon);
        double ozone = fetchOzone(atmosphericAuxdata, mjd, lat, lon);
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
        Result result = algorithm.processPixel(x, y, lat, lon,
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

        if (outputRtoa) {
            for (int i = 0; i < result.r_toa.length; i++) {
                targetSamples[RTOA_IX + i].set(result.r_toa[i]);
            }
        }

        if (outputRtosaGc) {
            for (int i = 0; i < result.r_tosa.length; i++) {
                targetSamples[RTOSA_IX + i].set(result.r_tosa[i]);
            }
        }

        if (outputRtosaGcAann) {
            for (int i = 0; i < result.rtosa_aann.length; i++) {
                targetSamples[RTOSA_AANN_IX + i].set(result.rtosa_aann[i]);
            }
        }

        if (outputRpath) {
            for (int i = 0; i < result.rpath_nn.length; i++) {
                targetSamples[RPATH_IX + i].set(result.rpath_nn[i]);
            }
        }

        if (outputTdown) {
            for (int i = 0; i < result.transd_nn.length; i++) {
                targetSamples[TDOWN_IX + i].set(result.transd_nn[i]);
            }
        }

        if (outputTup) {
            for (int i = 0; i < result.transu_nn.length; i++) {
                targetSamples[TUP_IX + i].set(result.transu_nn[i]);
            }
        }

        if (outputAcReflectance) {
            for (int i = 0; i < result.rwa.length; i++) {
                targetSamples[AC_REFLEC_IX + i].set(outputAsRrs ? result.rwa[i] / Math.PI : result.rwa[i]);
            }
        }

        if (outputRhown) {
            for (int i = 0; i < result.rwn.length; i++) {
                targetSamples[RHOWN_IX + i].set(result.rwn[i]);
            }
        }

        if (outputOos) {
            targetSamples[OOS_RTOSA_IX].set(result.rtosa_oos);
            targetSamples[OOS_AC_REFLEC_IX].set(result.rwa_oos);
        }

        for (int i = 0; i < result.iops_nn.length; i++) {
            targetSamples[IOP_APIG_IX + i].set(result.iops_nn[i]);
        }

        if (outputKd) {
            targetSamples[KD489_IX].set(result.kd489_nn);
            targetSamples[KDMIN_IX].set(result.kdmin_nn);
        }

        if (outputUncertainties) {
            for (int i = 0; i < result.unc_iop_abs.length; i++) {
                targetSamples[UNC_APIG_IX + i].set(result.unc_iop_abs[i]);
            }
            targetSamples[UNC_ADG_IX].set(result.unc_abs_adg);
            targetSamples[UNC_ATOT_IX].set(result.unc_abs_atot);
            targetSamples[UNC_BTOT_IX].set(result.unc_abs_btot);
            if (outputKd) {
                targetSamples[UNC_KD489_IX].set(result.unc_abs_kd489);
                targetSamples[UNC_KDMIN_IX].set(result.unc_abs_kdmin);
            }
        }

        targetSamples[C2RCC_FLAGS_IX].set(result.flags);
    }

    @Override
    protected void configureSourceSamples(SourceSampleConfigurer sc) throws OperatorException {
//        sc.setValidPixelMask(validPixelExpression);
        for (int i = 0; i < BAND_COUNT; i++) {
            sc.defineSample(i + RADIANCE_START_IX, getRadianceBandName(i));
            sc.defineSample(i + SOLAR_FLUX_START_IX, getSolarFluxBandname(i));
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
    protected void configureTargetSamples(TargetSampleConfigurer tsc) throws OperatorException {

        if (outputRtoa) {
            for (int i = 0; i < olciband21_ix.length; i++) {
                tsc.defineSample(RTOA_IX + i, "rtoa_" + olciband21_ix[i]);
            }
        }

        if (outputRtosaGc) {
            for (int i = 0; i < olciband16_ix.length; i++) {
                tsc.defineSample(RTOSA_IX + i, "rtosa_gc_" + olciband16_ix[i]);
            }
        }

        if (outputRtosaGcAann) {
            for (int i = 0; i < olciband16_ix.length; i++) {
                tsc.defineSample(RTOSA_AANN_IX + i, "rtosagc_aann_" + olciband16_ix[i]);
            }
        }

        if (outputRpath) {
            for (int i = 0; i < olciband16_ix.length; i++) {
                tsc.defineSample(RPATH_IX + i, "rpath_" + olciband16_ix[i]);
            }
        }

        if (outputTdown) {
            for (int i = 0; i < olciband16_ix.length; i++) {
                tsc.defineSample(TDOWN_IX + i, "tdown_" + olciband16_ix[i]);
            }
        }

        if (outputTup) {
            for (int i = 0; i < olciband16_ix.length; i++) {
                tsc.defineSample(TUP_IX + i, "tup_" + olciband16_ix[i]);
            }
        }

        if (outputAcReflectance) {
            for (int i = 0; i < olciband16_ix.length; i++) {
                if (outputAsRrs) {
                    tsc.defineSample(AC_REFLEC_IX + i, "rrs_" + olciband16_ix[i]);
                } else {
                    tsc.defineSample(AC_REFLEC_IX + i, "rhow_" + olciband16_ix[i]);
                }
            }
        }

        if (outputRhown) {
            for (int i = 0; i < olciband16_ix.length; i++) {
                tsc.defineSample(RHOWN_IX + i, "rhown_" + olciband16_ix[i]);
            }
        }

        if (outputOos) {
            tsc.defineSample(OOS_RTOSA_IX, "oos_rtosa");
            if (outputAsRrs) {
                tsc.defineSample(OOS_AC_REFLEC_IX, "oos_rrs");
            } else {
                tsc.defineSample(OOS_AC_REFLEC_IX, "oos_rhow");
            }
        }

        tsc.defineSample(IOP_APIG_IX, "iop_apig");
        tsc.defineSample(IOP_ADET_IX, "iop_adet");
        tsc.defineSample(IOP_AGELB_IX, "iop_agelb");
        tsc.defineSample(IOP_BPART_IX, "iop_bpart");
        tsc.defineSample(IOP_BWIT_IX, "iop_bwit");

        if (outputKd) {
            tsc.defineSample(KD489_IX, "kd489");
            tsc.defineSample(KDMIN_IX, "kdmin");
        }

        if (outputUncertainties) {
            tsc.defineSample(UNC_APIG_IX, "unc_apig");
            tsc.defineSample(UNC_ADET_IX, "unc_adet");
            tsc.defineSample(UNC_AGELB_IX, "unc_agelb");
            tsc.defineSample(UNC_BPART_IX, "unc_bpart");
            tsc.defineSample(UNC_BWIT_IX, "unc_bwit");

            tsc.defineSample(UNC_ADG_IX, "unc_adg");
            tsc.defineSample(UNC_ATOT_IX, "unc_atot");
            tsc.defineSample(UNC_BTOT_IX, "unc_btot");
            if (outputKd) {
                tsc.defineSample(UNC_KD489_IX, "unc_kd489");
                tsc.defineSample(UNC_KDMIN_IX, "unc_kdmin");
            }
        }

        tsc.defineSample(C2RCC_FLAGS_IX, "c2rcc_flags");
    }

    @Override
    protected void configureTargetProduct(ProductConfigurer productConfigurer) {
        super.configureTargetProduct(productConfigurer);
        productConfigurer.copyMetadata();

        final Product targetProduct = productConfigurer.getTargetProduct();

        targetProduct.setPreferredTileSize(128, 128);
        ProductUtils.copyFlagBands(sourceProduct, targetProduct, true);

        final StringBuilder autoGrouping = new StringBuilder("iop");
        autoGrouping.append(":conc");

        if (outputRtoa) {
            for (int i : olciband21_ix) {
                final Band band = addBand(targetProduct, "rtoa_" + i, "1", "Top-of-atmosphere reflectance");
                ensureSpectralProperties(band, i);
            }
            autoGrouping.append(":rtoa");
        }
        final String validPixelExpression = "c2rcc_flags.Valid_PE";
        if (outputRtosaGc) {
            for (int bi : olciband16_ix) {
                Band band = addBand(targetProduct, "rtosa_gc_" + bi, "1", "Gas corrected top-of-atmosphere reflectance, input to AC");
                ensureSpectralProperties(band, bi);
                band.setValidPixelExpression(validPixelExpression);
            }
            autoGrouping.append(":rtosa_gc");
        }
        if (outputRtosaGcAann) {
            for (int bi : olciband16_ix) {
                Band band = addBand(targetProduct, "rtosagc_aann_" + bi, "1", "Gas corrected top-of-atmosphere reflectance, output from AANN");
                ensureSpectralProperties(band, bi);
                band.setValidPixelExpression(validPixelExpression);
            }
            autoGrouping.append(":rtosagc_aann");
        }

        if (outputRpath) {
            for (int bi : olciband16_ix) {
                Band band = addBand(targetProduct, "rpath_" + bi, "1", "Path-radiance reflectances");
                ensureSpectralProperties(band, bi);
                band.setValidPixelExpression(validPixelExpression);
            }
            autoGrouping.append(":rpath");
        }

        if (outputTdown) {
            for (int bi : olciband16_ix) {
                Band band = addBand(targetProduct, "tdown_" + bi, "1", "Transmittance of downweling irradiance");
                ensureSpectralProperties(band, bi);
                band.setValidPixelExpression(validPixelExpression);
            }
            autoGrouping.append(":tdown");
        }

        if (outputTup) {
            for (int bi : olciband16_ix) {
                Band band = addBand(targetProduct, "tup_" + bi, "1", "Transmittance of upweling irradiance");
                ensureSpectralProperties(band, bi);
                band.setValidPixelExpression(validPixelExpression);
            }
            autoGrouping.append(":tup");
        }

        if (outputAcReflectance) {
            for (int index : olciband16_ix) {
                final Band band;
                if (outputAsRrs) {
                    band = addBand(targetProduct, "rrs_" + index, "sr^-1", "Atmospherically corrected Angular dependent remote sensing reflectances");
                } else {
                    band = addBand(targetProduct, "rhow_" + index, "1", "Atmospherically corrected Angular dependent water leaving reflectances");
                }
                ensureSpectralProperties(band, index);
                band.setValidPixelExpression(validPixelExpression);
            }
            if (outputAsRrs) {
                autoGrouping.append(":rrs");
            }else {
                autoGrouping.append(":rhow");
            }
        }

        if (outputRhown) {
            for (int index : olciband16_ix) {
                final Band band = addBand(targetProduct, "rhown_" + index, "1", "Normalized water leaving reflectances");
                ensureSpectralProperties(band, index);
                band.setValidPixelExpression(validPixelExpression);
            }
            autoGrouping.append(":rhown");
        }

        if (outputOos) {
            final Band oos_rtosa = addBand(targetProduct, "oos_rtosa", "1", "Gas corrected top-of-atmosphere reflectances are out of scope of nn training dataset");
            oos_rtosa.setValidPixelExpression(validPixelExpression);
            if (outputAsRrs) {
                final Band oos_rrs = addBand(targetProduct, "oos_rrs", "1", "Remote sensing reflectance are out of scope of nn training dataset");
                oos_rrs.setValidPixelExpression(validPixelExpression);
            } else {
                final Band oos_rhow = addBand(targetProduct, "oos_rhow", "1", "Water leaving reflectances are out of scope of nn training dataset");
                oos_rhow.setValidPixelExpression(validPixelExpression);
            }

            autoGrouping.append(":oos");
        }

        Band iop_apig = addBand(targetProduct, "iop_apig", "m^-1", "Absorption coefficient of phytoplankton pigments at 443 nm");
        Band iop_adet = addBand(targetProduct, "iop_adet", "m^-1", "Absorption coefficient of detritus at 443 nm");
        Band iop_agelb = addBand(targetProduct, "iop_agelb", "m^-1", "Absorption coefficient of gelbstoff at 443 nm");
        Band iop_bpart = addBand(targetProduct, "iop_bpart", "m^-1", "Scattering coefficient of marine paticles at 443 nm");
        Band iop_bwit = addBand(targetProduct, "iop_bwit", "m^-1", "Scattering coefficient of white particles at 443 nm");
        Band iop_adg = addVirtualBand(targetProduct, "iop_adg", "iop_adet + iop_agelb", "m^-1", "Detritus + gelbstoff absorption at 443 nm");
        Band iop_atot = addVirtualBand(targetProduct, "iop_atot", "iop_apig + iop_adet + iop_agelb", "m^-1", "phytoplankton + detritus + gelbstoff absorption at 443 nm");
        Band iop_btot = addVirtualBand(targetProduct, "iop_btot", "iop_bpart + iop_bwit", "m^-1", "total particle scattering at 443 nm");

        iop_apig.setValidPixelExpression(validPixelExpression);
        iop_adet.setValidPixelExpression(validPixelExpression);
        iop_agelb.setValidPixelExpression(validPixelExpression);
        iop_bpart.setValidPixelExpression(validPixelExpression);
        iop_bwit.setValidPixelExpression(validPixelExpression);
        iop_adg.setValidPixelExpression(validPixelExpression);
        iop_atot.setValidPixelExpression(validPixelExpression);
        iop_btot.setValidPixelExpression(validPixelExpression);

        Band kd489 = null;
        Band kdmin = null;
        Band kd_z90max = null;
        if (outputKd) {
            kd489 = addBand(targetProduct, "kd489", "m^-1", "Irradiance attenuation coefficient at 489 nm");
            kdmin = addBand(targetProduct, "kdmin", "m^-1", "Mean irradiance attenuation coefficient at the three bands with minimum kd");
            kd_z90max = addVirtualBand(targetProduct, "kd_z90max", "1 / kdmin", "m", "Depth of the water column from which 90% of the water leaving irradiance comes from");

            kd489.setValidPixelExpression(validPixelExpression);
            kdmin.setValidPixelExpression(validPixelExpression);
            kd_z90max.setValidPixelExpression(validPixelExpression);

            autoGrouping.append(":kd");
        }

        Band conc_tsm = addVirtualBand(targetProduct, "conc_tsm", "iop_bpart * " + TSMfakBpart + " + iop_bwit * " + TSMfakBwit, "g m^-3", "Total suspended matter dry weight concentration");
        Band conc_chl = addVirtualBand(targetProduct, "conc_chl", "pow(iop_apig, " + CHLexp + ") * " + CHLfak, "mg m^-3", "Chlorophyll concentration");

        conc_tsm.setValidPixelExpression(validPixelExpression);
        conc_chl.setValidPixelExpression(validPixelExpression);

        if (outputUncertainties) {
            Band unc_apig = addBand(targetProduct, "unc_apig", "m^-1", "uncertainty of pigment absorption coefficient");
            Band unc_adet = addBand(targetProduct, "unc_adet", "m^-1", "uncertainty of detritus absorption coefficient");
            Band unc_agelb = addBand(targetProduct, "unc_agelb", "m^-1", "uncertainty of dissolved gelbstoff absorption coefficient");
            Band unc_bpart = addBand(targetProduct, "unc_bpart", "m^-1", "uncertainty of particle scattering coefficient");
            Band unc_bwit = addBand(targetProduct, "unc_bwit", "m^-1", "uncertainty of white particle scattering coefficient");
            Band unc_adg = addBand(targetProduct, "unc_adg", "m^-1", "uncertainty of total gelbstoff absorption coefficient");
            Band unc_atot = addBand(targetProduct, "unc_atot", "m^-1", "uncertainty of total water constituent absorption coefficient");
            Band unc_btot = addBand(targetProduct, "unc_btot", "m^-1", "uncertainty of total water constituent scattering coefficient");

            iop_apig.addAncillaryVariable(unc_apig, "uncertainty");
            iop_adet.addAncillaryVariable(unc_adet, "uncertainty");
            iop_agelb.addAncillaryVariable(unc_agelb, "uncertainty");
            iop_bpart.addAncillaryVariable(unc_bpart, "uncertainty");
            iop_bwit.addAncillaryVariable(unc_bwit, "uncertainty");
            iop_adg.addAncillaryVariable(unc_adg, "uncertainty");
            iop_atot.addAncillaryVariable(unc_atot, "uncertainty");
            iop_btot.addAncillaryVariable(unc_btot, "uncertainty");

            unc_apig.setValidPixelExpression(validPixelExpression);
            unc_adet.setValidPixelExpression(validPixelExpression);
            unc_agelb.setValidPixelExpression(validPixelExpression);
            unc_bpart.setValidPixelExpression(validPixelExpression);
            unc_bwit.setValidPixelExpression(validPixelExpression);
            unc_adg.setValidPixelExpression(validPixelExpression);
            unc_atot.setValidPixelExpression(validPixelExpression);
            unc_btot.setValidPixelExpression(validPixelExpression);

            Band unc_tsm = addVirtualBand(targetProduct, "unc_tsm", "unc_btot * " + TSMfakBpart, "g m^-3", "uncertainty of total suspended matter (TSM) dry weight concentration");
            Band unc_chl = addVirtualBand(targetProduct, "unc_chl", "pow(unc_apig, " + CHLexp + ") * " + CHLfak, "mg m^-3", "uncertainty of chlorophyll concentration");

            conc_tsm.addAncillaryVariable(unc_tsm, "uncertainty");
            conc_chl.addAncillaryVariable(unc_chl, "uncertainty");

            unc_tsm.setValidPixelExpression(validPixelExpression);
            unc_chl.setValidPixelExpression(validPixelExpression);

            if (outputKd) {
                Band unc_kd489 = addBand(targetProduct, "unc_kd489", "m^-1", "uncertainty of irradiance attenuation coefficient");
                Band unc_kdmin = addBand(targetProduct, "unc_kdmin", "m^-1", "uncertainty of mean irradiance attenuation coefficient");
                Band unc_kd_z90max = addVirtualBand(targetProduct, "unc_kd_z90max", "abs(kd_z90max - 1.0 / abs(kdmin - unc_kdmin))", "m", "uncertainty of depth of the water column from which 90% of the water leaving irradiance comes from");

                kd489.addAncillaryVariable(unc_kd489, "uncertainty");
                kdmin.addAncillaryVariable(unc_kdmin, "uncertainty");
                kd_z90max.addAncillaryVariable(unc_kd_z90max, "uncertainty");

                unc_kd489.setValidPixelExpression(validPixelExpression);
                unc_kdmin.setValidPixelExpression(validPixelExpression);
                unc_kd_z90max.setValidPixelExpression(validPixelExpression);
            }

            autoGrouping.append(":unc");
        }

        Band c2rcc_flags = targetProduct.addBand("c2rcc_flags", ProductData.TYPE_UINT32);
        c2rcc_flags.setDescription("C2RCC quality flags");

        FlagCoding flagCoding = new FlagCoding("c2rcc_flags");
        //0
        flagCoding.addFlag("Rtosa_OOS", 0x01, "The input spectrum to atmospheric correction neural net was unknown");
        flagCoding.addFlag("Rtosa_OOR", 0x02, "The input spectrum to atmospheric correction neural net out of training range");
        flagCoding.addFlag("Iop_OOR", 0x04, "One of the IOPs is out of range");
        flagCoding.addFlag("Rhow_OOR", 0x08, "One of the inputs to the IOP retrieval neural net is out of training range");
        flagCoding.addFlag("Apig_at_max", 0x010, "Apig output of the IOP retrieval neural net is at its maximum");
        //5
        flagCoding.addFlag("Adet_at_max", 0x020, "Adet output of the IOP retrieval neural net is at its maximum");
        flagCoding.addFlag("Agelb_at_max", 0x040, "Agelb output of the IOP retrieval neural net is at its maximum");
        flagCoding.addFlag("Bpart_at_max", 0x080, "Bpart output of the IOP retrieval neural net is at its maximum");
        flagCoding.addFlag("Bwit_at_max", 0x0100, "Bwit output of the IOP retrieval neural net is at its maximum");
        flagCoding.addFlag("Apig_at_min", 0x0200, "Apig output of the IOP retrieval neural net is at its maximum");
        //10
        flagCoding.addFlag("Adet_at_min", 0x0400, "Adet output of the IOP retrieval neural net is at its maximum");
        flagCoding.addFlag("Agelb_at_min", 0x0800, "Agelb output of the IOP retrieval neural net is at its maximum");
        flagCoding.addFlag("Bpart_at_min", 0x01000, "Bpart output of the IOP retrieval neural net is at its maximum");
        flagCoding.addFlag("Bwit_at_min", 0x02000, "Bwit output of the IOP retrieval neural net is at its maximum");
        flagCoding.addFlag("Rhow_OOS", 0x04000, "The Rhow input spectrum to IOP neural net is unknown");
        //15
        flagCoding.addFlag("Kd489_OOR", 0x08000, "kd489 is out of range");
        flagCoding.addFlag("Kdmin_OOR", 0x010000, "kdmin is out of range");
        flagCoding.addFlag("Kd489_at_max", 0x020000, "kdmin is at max");
        flagCoding.addFlag("Kdmin_at_max", 0x040000, "kdmin is at max");
        flagCoding.addFlag("Valid_PE", 0x080000, "The operators valid pixel expression has resolved to true");

        targetProduct.getFlagCodingGroup().add(flagCoding);
        c2rcc_flags.setSampleCoding(flagCoding);

        Color[] maskColors = {Color.RED, Color.ORANGE, Color.YELLOW, Color.BLUE, Color.GREEN, Color.PINK, Color.MAGENTA, Color.CYAN, Color.GRAY};
        String[] flagNames = flagCoding.getFlagNames();
        for (int i = 0; i < flagNames.length; i++) {
            String flagName = flagNames[i];
            MetadataAttribute flag = flagCoding.getFlag(flagName);
            targetProduct.addMask(flagName, "c2rcc_flags." + flagName, flag.getDescription(), maskColors[i % maskColors.length], 0.5);
        }
        targetProduct.setAutoGrouping(autoGrouping.toString());

        targetProduct.addProductNodeListener(getNnNamesMetadataAppender());
    }

    @Override
    protected void prepareInputs() throws OperatorException {
        for (int i = 0; i < BAND_COUNT; i++) {
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
            final boolean loadFromResources = alternativeNNPath == null || alternativeNNPath.trim().length() == 0;
            if (loadFromResources) {
                nnFilePaths = c2rccNNResourcePaths;
            } else {
                nnFilePaths = NNUtils.getNNFilePaths(Paths.get(alternativeNNPath), alternativeNetDirNames);
            }
            algorithm = new C2rccOlciAlgorithm(nnFilePaths, loadFromResources);
        } catch (IOException e) {
            throw new OperatorException(e);
        }

        algorithm.setTemperature(temperature);
        algorithm.setSalinity(salinity);
        algorithm.setThresh_absd_log_rtosa(thresholdRtosaOOS);
        algorithm.setThresh_rwlogslope(thresholdAcReflecOos);

        algorithm.setOutputRtoaGcAann(outputRtosaGcAann);
        algorithm.setOutputRpath(outputRpath);
        algorithm.setOutputTdown(outputTdown);
        algorithm.setOutputTup(outputTup);
        algorithm.setOutputRhow(outputAcReflectance);
        algorithm.setOutputRhown(outputRhown);
        algorithm.setOutputOos(outputOos);
        algorithm.setOutputKd(outputKd);
        algorithm.setOutputUncertainties(outputUncertainties);

        // @todo discuss with Carsten and Roland
//        if (useDefaultSolarFlux) {  // not the sol flux values from the input product
//            solarFluxLazyLookup = new SolarFluxLazyLookup(DEFAULT_SOLAR_FLUX);
//        } else {
//            double[] solfluxFromL1b = new double[BAND_COUNT];
//            for (int i = 0; i < BAND_COUNT; i++) {
//                solfluxFromL1b[i] = sourceProduct.getBand("radiance_" + (i + 1)).getSolarFlux();
//            }
//            if (isSolfluxValid(solfluxFromL1b)) {
//                constantSolarFlux = solfluxFromL1b;
//            } else {
//                throw new OperatorException("Invalid solar flux in source product!");
//            }
//        }

        C2rccCommons.ensureTimeCoding_Fallback(sourceProduct);
        initAtmosphericAuxdata();
    }

    private static String getRadianceBandName(int i) {
        return String.format("Oa%02d_radiance", i + 1);
    }

    private static String getSolarFluxBandname(int i) {
        return String.format("solar_flux_band_%d", i + 1);
    }

//    private static boolean isSolfluxValid(double[] solflux) {
//        for (double v : solflux) {
//            if (v <= 0.0) {
//                return false;
//            }
//        }
//        return true;
//    }

    private void ensureSpectralProperties(Band band, int i) {
        ProductUtils.copySpectralBandProperties(sourceProduct.getBand(String.format("Oa%02d_radiance", i)), band);
        if (band.getSpectralWavelength() == 0) {
            band.setSpectralWavelength(DEFAULT_OLCI_WAVELENGTH[i - 1]);
            band.setSpectralBandIndex(i);
        }

    }

    private ProductNodeListener getNnNamesMetadataAppender() {
        final String processingGraphName = "Processing_Graph";
        final String[] nnNames = algorithm.getUsedNeuronalNetNames();
        final String alias = getSpi().getOperatorAlias();
        return new ProductNodeListenerAdapter() {

            private MetadataElement operatorNode;

            @Override
            public void nodeAdded(ProductNodeEvent event) {
                final ProductNode sourceNode = event.getSourceNode();
                if (!(sourceNode instanceof MetadataAttribute)) {
                    return;
                }
                final MetadataAttribute ma = (MetadataAttribute) sourceNode;
                final MetadataElement pe = ma.getParentElement();
                if ("operator".equals(ma.getName())
                        && pe.getName().startsWith("node")
                        && processingGraphName.equals(pe.getParentElement().getName())) {
                    if (operatorNode == null) {
                        if (alias.equals(ma.getData().getElemString())) {
                            operatorNode = pe;
                        }
                    } else {
                        sourceNode.getProduct().removeProductNodeListener(this);
                        final MetadataElement neuronalNetsElem = new MetadataElement("neuronalNets");
                        operatorNode.addElement(neuronalNetsElem);
                        for (String nnName : nnNames) {
                            neuronalNetsElem.addAttribute(new MetadataAttribute("usedNeuralNet", ProductData.createInstance(nnName), true));
                        }
                    }
                }
            }
        };
    }

    private void initAtmosphericAuxdata() {
        AtmosphericAuxdataBuilder auxdataBuilder = new AtmosphericAuxdataBuilder();
        auxdataBuilder.setOzone(ozone);
        auxdataBuilder.setSurfacePressure(press);
        auxdataBuilder.useAtmosphericAuxDataPath(atmosphericAuxDataPath);
        auxdataBuilder.useTomsomiProducts(tomsomiStartProduct, tomsomiEndProduct);
        auxdataBuilder.useNcepProducts(ncepStartProduct, ncepEndProduct);
        if(useEcmwfAuxData) {
            VirtualBand ozoneInDu = new VirtualBand("__ozone_in_du_",
                                                      ProductData.TYPE_FLOAT32,
                                                      getSourceProduct().getSceneRasterWidth(),
                                                      getSourceProduct().getSceneRasterHeight(),
                                                      RASTER_NAME_TOTAL_OZONE + " * 46698");
            ozoneInDu.setOwner(sourceProduct);
            auxdataBuilder.useAtmosphericRaster(ozoneInDu,
                                                sourceProduct.getRasterDataNode(RASTER_NAME_SEA_LEVEL_PRESSURE));
        }

        try {
            atmosphericAuxdata = auxdataBuilder.create();
        } catch (Exception e) {
            throw new OperatorException("Could not create provider for atmospheric auxdata", e);
        }
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

    private void assertFlagCoding(String name) {
        assertSourceBand(name);
        if (sourceProduct.getBand(name).getFlagCoding() == null) {
            throw new OperatorException("Invalid source product, flag coding '" + name + "' required");
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

    private Band addVirtualBand(Product targetProduct, String name, String expression, String unit, String description) {
        Band band = targetProduct.addBand(name, expression);
        band.setUnit(unit);
        band.setDescription(description);
        band.getSourceImage(); // trigger source image creation
        band.setGeophysicalNoDataValue(Double.NaN);
        band.setNoDataValueUsed(true);
        return band;
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(C2rccOlciOperator.class);
        }
    }
}
