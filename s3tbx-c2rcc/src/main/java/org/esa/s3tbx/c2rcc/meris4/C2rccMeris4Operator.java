package org.esa.s3tbx.c2rcc.meris4;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s3tbx.c2rcc.C2rccCommons;
import org.esa.s3tbx.c2rcc.C2rccConfigurable;
import org.esa.s3tbx.c2rcc.ancillary.AtmosphericAuxdata;
import org.esa.s3tbx.c2rcc.ancillary.AtmosphericAuxdataBuilder;
import org.esa.s3tbx.c2rcc.meris4.C2rccMeris4Algorithm.Result;
import org.esa.s3tbx.c2rcc.util.NNUtils;
import org.esa.s3tbx.c2rcc.util.RgbProfiles;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.datamodel.ProductNodeEvent;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.datamodel.ProductNodeListener;
import org.esa.snap.core.datamodel.ProductNodeListenerAdapter;
import org.esa.snap.core.datamodel.TimeCoding;
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
import java.util.HashMap;
import java.util.Map;

import static org.esa.s3tbx.c2rcc.C2rccCommons.addBand;
import static org.esa.s3tbx.c2rcc.C2rccCommons.addVirtualBand;
import static org.esa.s3tbx.c2rcc.C2rccCommons.fetchOzone;
import static org.esa.s3tbx.c2rcc.C2rccCommons.fetchSurfacePressure;
import static org.esa.s3tbx.c2rcc.meris4.C2rccMeris4Algorithm.DEFAULT_MERIS_WAVELENGTH;
import static org.esa.s3tbx.c2rcc.meris4.C2rccMeris4Algorithm.FLAG_INDEX_ADET_AT_MAX;
import static org.esa.s3tbx.c2rcc.meris4.C2rccMeris4Algorithm.FLAG_INDEX_ADET_AT_MIN;
import static org.esa.s3tbx.c2rcc.meris4.C2rccMeris4Algorithm.FLAG_INDEX_AGELB_AT_MAX;
import static org.esa.s3tbx.c2rcc.meris4.C2rccMeris4Algorithm.FLAG_INDEX_AGELB_AT_MIN;
import static org.esa.s3tbx.c2rcc.meris4.C2rccMeris4Algorithm.FLAG_INDEX_APIG_AT_MAX;
import static org.esa.s3tbx.c2rcc.meris4.C2rccMeris4Algorithm.FLAG_INDEX_APIG_AT_MIN;
import static org.esa.s3tbx.c2rcc.meris4.C2rccMeris4Algorithm.FLAG_INDEX_BPART_AT_MAX;
import static org.esa.s3tbx.c2rcc.meris4.C2rccMeris4Algorithm.FLAG_INDEX_BPART_AT_MIN;
import static org.esa.s3tbx.c2rcc.meris4.C2rccMeris4Algorithm.FLAG_INDEX_BWIT_AT_MAX;
import static org.esa.s3tbx.c2rcc.meris4.C2rccMeris4Algorithm.FLAG_INDEX_BWIT_AT_MIN;
import static org.esa.s3tbx.c2rcc.meris4.C2rccMeris4Algorithm.FLAG_INDEX_CLOUD;
import static org.esa.s3tbx.c2rcc.meris4.C2rccMeris4Algorithm.FLAG_INDEX_IOP_OOR;
import static org.esa.s3tbx.c2rcc.meris4.C2rccMeris4Algorithm.FLAG_INDEX_KD489_AT_MAX;
import static org.esa.s3tbx.c2rcc.meris4.C2rccMeris4Algorithm.FLAG_INDEX_KD489_OOR;
import static org.esa.s3tbx.c2rcc.meris4.C2rccMeris4Algorithm.FLAG_INDEX_KDMIN_AT_MAX;
import static org.esa.s3tbx.c2rcc.meris4.C2rccMeris4Algorithm.FLAG_INDEX_KDMIN_OOR;
import static org.esa.s3tbx.c2rcc.meris4.C2rccMeris4Algorithm.FLAG_INDEX_RHOW_OOR;
import static org.esa.s3tbx.c2rcc.meris4.C2rccMeris4Algorithm.FLAG_INDEX_RHOW_OOS;
import static org.esa.s3tbx.c2rcc.meris4.C2rccMeris4Algorithm.FLAG_INDEX_RTOSA_OOR;
import static org.esa.s3tbx.c2rcc.meris4.C2rccMeris4Algorithm.FLAG_INDEX_RTOSA_OOS;
import static org.esa.s3tbx.c2rcc.meris4.C2rccMeris4Algorithm.FLAG_INDEX_VALID_PE;
import static org.esa.s3tbx.c2rcc.meris4.C2rccMeris4Algorithm.merband12_ix;
import static org.esa.s3tbx.c2rcc.meris4.C2rccMeris4Algorithm.merband15_ix;

// todo (nf) - Add min/max values of NN inputs and outputs to metadata (https://github.com/bcdev/s3tbx-c2rcc/issues/3)

/**
 * The Case 2 Regional / CoastColour Operator for MERIS data of the 4th reprocessing.
 * <p/>
 * Computes AC-reflectances and IOPs from OLCI L1b data products using
 * an neural-network approach.
 */
@OperatorMetadata(alias = "c2rcc.meris4", version = "1.0",
        authors = "Roland Doerffer, Sabine Embacher (Brockmann Consult)",
        category = "Optical/Thematic Water Processing",
        copyright = "Copyright (C) 2016 by Brockmann Consult",
        description = "Performs atmospheric correction and IOP retrieval with uncertainties on MERIS L1b data products from the 4th reprocessing.")
public class C2rccMeris4Operator extends PixelOperator implements C2rccConfigurable {
    /*
        c2rcc ops have been removed from Graph Builder. In the layer xml they are disabled
        see https://senbox.atlassian.net/browse/SNAP-395
    */

    static final String RASTER_NAME_QUALITY_FLAGS = "quality_flags";
    static final String RASTER_NAME_SEA_LEVEL_PRESSURE = "sea_level_pressure";
    static final String RASTER_NAME_TOTAL_OZONE = "total_ozone";
    static final String RASTER_NAME_SUN_ZENITH = "SZA";
    static final String RASTER_NAME_SUN_AZIMUTH = "SAA";
    static final String RASTER_NAME_VIEWING_ZENITH = "OZA";
    static final String RASTER_NAME_VIEWING_AZIMUTH = "OAA";
    static final String RASTER_NAME_ALTITUDE = "altitude";

    // MERIS4 sources
    static final int BAND_COUNT = 15;
    private static final int RADIANCE_START_IX = 0;
    private static final int SOLAR_FLUX_START_IX = BAND_COUNT;
    private static final int DEM_ALT_IX = BAND_COUNT * 2;
    private static final int SUN_ZEN_IX = DEM_ALT_IX + 1;
    private static final int SUN_AZI_IX = DEM_ALT_IX + 2;
    private static final int VIEW_ZEN_IX = DEM_ALT_IX + 3;
    private static final int VIEW_AZI_IX = DEM_ALT_IX + 4;
    private static final int VALID_PIXEL_IX = DEM_ALT_IX + 5;

    // MERIS4 targets
    private static final int BC_12 = merband12_ix.length; // Band count 12
    private static final int BC_15 = merband15_ix.length; // Band count 15
    private static final int SINGLE_IX = BC_15 + 7 * BC_12;

    private static final int RTOA_IX = 0;
    private static final int RTOSA_IX = BC_15;
    private static final int RTOSA_AANN_IX = BC_15 + BC_12;
    private static final int RPATH_IX = BC_15 + 2 * BC_12;
    private static final int TDOWN_IX = BC_15 + 3 * BC_12;
    private static final int TUP_IX = BC_15 + 4 * BC_12;
    private static final int AC_REFLEC_IX = BC_15 + 5 * BC_12;
    private static final int RHOWN_IX = BC_15 + 6 * BC_12;

    private static final int OOS_RTOSA_IX = SINGLE_IX;
    private static final int OOS_AC_REFLEC_IX = SINGLE_IX + 1;

    private static final int IOP_APIG_IX = SINGLE_IX + 2;
    private static final int IOP_ADET_IX = SINGLE_IX + 3;
    private static final int IOP_AGELB_IX = SINGLE_IX + 4;
    private static final int IOP_BPART_IX = SINGLE_IX + 5;
    private static final int IOP_BWIT_IX = SINGLE_IX + 6;
    private static final int KD489_IX = SINGLE_IX + 7;
    private static final int KDMIN_IX = SINGLE_IX + 8;
    private static final int UNC_APIG_IX = SINGLE_IX + 9;
    private static final int UNC_ADET_IX = SINGLE_IX + 10;
    private static final int UNC_AGELB_IX = SINGLE_IX + 11;
    private static final int UNC_BPART_IX = SINGLE_IX + 12;
    private static final int UNC_BWIT_IX = SINGLE_IX + 13;
    private static final int UNC_ADG_IX = SINGLE_IX + 14;
    private static final int UNC_ATOT_IX = SINGLE_IX + 15;
    private static final int UNC_BTOT_IX = SINGLE_IX + 16;
    private static final int UNC_KD489_IX = SINGLE_IX + 17;
    private static final int UNC_KDMIN_IX = SINGLE_IX + 18;

    private static final int C2RCC_FLAGS_IX = SINGLE_IX + 19;

    private static final String RADIANCE_BANDNAME_PATTERN = "M%02d_radiance";
    private static final String SOLAR_FLUX_BANDNAME_PATTERN = "solar_flux_band_%d";

    private static final String PRODUCT_TYPE = "C2RCC_MERIS4";

    private static final String STANDARD_NETS = "C2RCC-Nets";
    private static final String EXTREME_NETS = "C2X-Nets";
    private static final Map<String, String[]> c2rccNetSetMap = new HashMap<>();

    static {
        String[] standardNets = new String[10];
        standardNets[C2rccMeris4Algorithm.IDX_rtosa_aann] = "meris/coastcolour_midtsm_20161012/atmo_midtsm/rtosa_aann/31x7x31_786.7.net";
        standardNets[C2rccMeris4Algorithm.IDX_rtosa_rpath] = "meris/coastcolour_midtsm_20161012/atmo_midtsm/rtosa_rpath/31x37_2058.3.net";
        standardNets[C2rccMeris4Algorithm.IDX_rtosa_rw] = "meris/coastcolour_midtsm_20161012/atmo_midtsm/rtosa_rw/37x77x57x37_727927.1.net";
        standardNets[C2rccMeris4Algorithm.IDX_rtosa_trans] = "meris/coastcolour_midtsm_20161012/atmo_midtsm/rtosa_trans/31x37_39553.7.net";
        standardNets[C2rccMeris4Algorithm.IDX_iop_rw] = "meris/coastcolour_midtsm_20161012/water_midtsm/iop_rw/17x97x47_490.7.net";
        standardNets[C2rccMeris4Algorithm.IDX_iop_unciop] = "meris/coastcolour_midtsm_20161012/water_midtsm/iop_unciop/17x77x37_11486.7.net";
        standardNets[C2rccMeris4Algorithm.IDX_iop_uncsumiop_unckd] = "meris/coastcolour_midtsm_20161012/water_midtsm/iop_uncsumiop_unckd/17x77x37_9113.1.net";
        standardNets[C2rccMeris4Algorithm.IDX_rw_iop] = "meris/coastcolour_midtsm_20161012/water_midtsm/rw_iop/97x77x37_22393.1.net";
        standardNets[C2rccMeris4Algorithm.IDX_rw_kd] = "meris/coastcolour_midtsm_20161012/water_midtsm/rw_kd/97x77x7_376.3.net";
        standardNets[C2rccMeris4Algorithm.IDX_rw_rwnorm] = "meris/coastcolour_midtsm_20161012/water_midtsm/rw_rwnorm/37x57x17_76.8.net";
        c2rccNetSetMap.put(STANDARD_NETS, standardNets);
    }

    static {
        String[] extremeNets = new String[10];
        extremeNets[C2rccMeris4Algorithm.IDX_rtosa_aann] = "meris/c2x/nn4snap_meris_hitsm_20151128/rtosa_aann/31x7x31_1244.3.net";
        extremeNets[C2rccMeris4Algorithm.IDX_rtosa_rw] = "meris/c2x/nn4snap_meris_hitsm_20151128/rtosa_rw/17x27x27x17_677356.6.net";
        extremeNets[C2rccMeris4Algorithm.IDX_rw_iop] = "meris/c2x/nn4snap_meris_hitsm_20151128/rw_iop/27x97x77x37_14746.2.net";
        extremeNets[C2rccMeris4Algorithm.IDX_iop_rw] = "meris/c2x/nn4snap_meris_hitsm_20151128/iop_rw/17x37x97x47_500.0.net";
        extremeNets[C2rccMeris4Algorithm.IDX_rw_kd] = "meris/c2x/nn4snap_meris_hitsm_20151128/rw_kd/97x77x7_232.4.net";
        extremeNets[C2rccMeris4Algorithm.IDX_iop_unciop] = "meris/c2x/nn4snap_meris_hitsm_20151128/iop_unciop/17x77x37_11486.7.net";
        extremeNets[C2rccMeris4Algorithm.IDX_iop_uncsumiop_unckd] = "meris/c2x/nn4snap_meris_hitsm_20151128/iop_uncsumiop_unckd/17x77x37_9113.1.net";
        extremeNets[C2rccMeris4Algorithm.IDX_rw_rwnorm] = "meris/c2x/nn4snap_meris_hitsm_20151128/rw_rwnorm/37x57x17_76.8.net";
        extremeNets[C2rccMeris4Algorithm.IDX_rtosa_trans] = "meris/c2x/nn4snap_meris_hitsm_20151128/rtosa_trans/31x77x57x37_45461.2.net";
        extremeNets[C2rccMeris4Algorithm.IDX_rtosa_rpath] = "meris/c2x/nn4snap_meris_hitsm_20151128/rtosa_rpath/31x77x57x37_4701.4.net";
        c2rccNetSetMap.put(EXTREME_NETS, extremeNets);
    }

    @SourceProduct(label = "MERIS L1b 4th reproc. product", description = "MERIS L1b source product.")
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
            defaultValue = "!quality_flags.invalid && (!quality_flags.land || quality_flags.fresh_inland_water)",
            description = "Defines the pixels which are valid for processing.",
            converter = BooleanExpressionConverter.class)
    private String validPixelExpression;

    @Parameter(defaultValue = "35.0", unit = "PSU", interval = "(0.000028, 43)",
            description = "The value used as salinity for the scene.")
    private double salinity;

    @Parameter(defaultValue = "15.0", unit = "C", interval = "(0.000111, 36)",
            description = "The value used as temperature for the scene.")
    private double temperature;

    @Parameter(defaultValue = "330", unit = "DU", interval = "(0, 1000)",
            description = "The value used as ozone if not provided by auxiliary data.")
    private double ozone;

    @Parameter(defaultValue = "1000", unit = "hPa", interval = "(800, 1040)", label = "Air Pressure at Sea Level",
            description = "The surface air pressure at sea level if not provided by auxiliary data.")
    private double press;

    @Parameter(alias = "TSMfac", defaultValue = "1.72", description = "TSM factor (TSM = TSMfac * iop_btot^TSMexp).", label = "TSM factor")
    private double TSMfakBpart;

    @Parameter(alias = "TSMexp", defaultValue = "3.1", description = "TSM exponent (TSM = TSMfac * iop_btot^TSMexp).", label = "TSM exponent")
    private double TSMfakBwit;

    @Parameter(alias = "CHLexp", defaultValue = "1.04", description = "Chlorophyll exponent ( CHL = iop_apig^CHLexp * CHLfac).", label = "CHL exponent")
    private double CHLexp;

    @Parameter(alias = "CHLfac", defaultValue = "21.0", description = "Chlorophyll factor ( CHL = iop_apig^CHLexp * CHLfac).", label = "CHL factor")
    private double CHLfak;

    //RD20161103 parameter 0.05 -> 0.003 for sum of differences of MERIS12 bands 9-12
    @Parameter(defaultValue = "0.003", description = "Threshold for out of scope of nn training dataset flag for gas corrected top-of-atmosphere reflectances.",
            label = "Threshold rtosa OOS")
    private double thresholdRtosaOOS;

    @Parameter(defaultValue = "0.1", description = "Threshold for out of scope of nn training dataset flag for atmospherically corrected reflectances.",
            label = "Threshold AC reflectances OOS")
    private double thresholdAcReflecOos;

    @Parameter(defaultValue = "0.955", description = "Threshold for cloud test based on downwelling transmittance @865.",
            label = "Threshold for cloud flag on down transmittance @865")
    private double thresholdCloudTDown865;

    @Parameter(description = "Path to the atmospheric auxiliary data directory. Use either this or the specified products on the I/O Parameters tab. " +
            "If the auxiliary data is not available at this path, the data will automatically be downloaded.")
    private String atmosphericAuxDataPath;

    @Parameter(description = "Path to an alternative set of neuronal nets. Use this to replace the standard " +
            "set of neuronal nets with the ones in the given directory.",
            label = "Alternative NN Path")
    private String alternativeNNPath;

    @Parameter(valueSet = {STANDARD_NETS, EXTREME_NETS},
            description = "Set of neuronal nets for algorithm.",
            defaultValue = STANDARD_NETS,
            label = "Set of neuronal nets")
    private String netSet = STANDARD_NETS;

    @Parameter(defaultValue = "false", description = "Write remote sensing reflectances instead of water leaving reflectances.",
            label = "Output AC reflectances as rrs instead of rhow")
    private boolean outputAsRrs;

    @Parameter(defaultValue = "false", description = "Alternative way of calculating water reflectance. Still experimental.",
            label = "Derive water reflectance from path radiance and transmittance")
    private boolean deriveRwFromPathAndTransmittance;

    @Parameter(defaultValue = "true", description =
            "If selected, the ECMWF auxiliary data (total_ozone, sea_level_pressure) of the source product is used",
            label = "Use ECMWF aux data of source product")
    private boolean useEcmwfAuxData;

    @Parameter(defaultValue = "true", label = "Output TOA reflectances")
    private boolean outputRtoa;

    @Parameter(defaultValue = "false", label = "Output gas corrected TOSA reflectances")
    private boolean outputRtosaGc;

    @Parameter(defaultValue = "false", label = "Output gas corrected TOSA reflectances of auto nn")
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

    @Parameter(defaultValue = "true", label = "Output uncertainties")
    private boolean outputUncertainties;

    private C2rccMeris4Algorithm algorithm;
    private AtmosphericAuxdata atmosphericAuxdata;
    private boolean useSnapDem;
    private ElevationModel elevationModel;
    private TimeCoding timeCoding;

    public static boolean isValidInput(Product product) {
        for (int i = 1; i <= BAND_COUNT; i++) {
            if (!product.containsBand(getRadianceBandName(i))
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

    public void setDeriveRwFromPathAndTransmittance(boolean deriveRwFromPathAndTransmittance) {
        this.deriveRwFromPathAndTransmittance = deriveRwFromPathAndTransmittance;
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
        final double[] solflux = new double[BAND_COUNT];
        for (int i = 0; i < BAND_COUNT; i++) {
            radiances[i] = sourceSamples[i].getDouble();
            Sample solFluxSample = sourceSamples[i + SOLAR_FLUX_START_IX];
            solflux[i] = solFluxSample.getNode().isPixelValid(x, y) ? solFluxSample.getDouble() : Double.NaN;
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
    protected void configureTargetSamples(TargetSampleConfigurer tsc) throws OperatorException {

        if (outputRtoa) {
            for (int i = 0; i < merband15_ix.length; i++) {
                tsc.defineSample(RTOA_IX + i, "rtoa_" + merband15_ix[i]);
            }
        }

        if (outputRtosaGc) {
            for (int i = 0; i < merband12_ix.length; i++) {
                tsc.defineSample(RTOSA_IX + i, "rtosa_gc_" + merband12_ix[i]);
            }
        }

        if (outputRtosaGcAann) {
            for (int i = 0; i < merband12_ix.length; i++) {
                tsc.defineSample(RTOSA_AANN_IX + i, "rtosagc_aann_" + merband12_ix[i]);
            }
        }

        if (outputRpath) {
            for (int i = 0; i < merband12_ix.length; i++) {
                tsc.defineSample(RPATH_IX + i, "rpath_" + merband12_ix[i]);
            }
        }

        if (outputTdown) {
            for (int i = 0; i < merband12_ix.length; i++) {
                tsc.defineSample(TDOWN_IX + i, "tdown_" + merband12_ix[i]);
            }
        }

        if (outputTup) {
            for (int i = 0; i < merband12_ix.length; i++) {
                tsc.defineSample(TUP_IX + i, "tup_" + merband12_ix[i]);
            }
        }

        if (outputAcReflectance) {
            for (int i = 0; i < merband12_ix.length; i++) {
                if (outputAsRrs) {
                    tsc.defineSample(AC_REFLEC_IX + i, "rrs_" + merband12_ix[i]);
                } else {
                    tsc.defineSample(AC_REFLEC_IX + i, "rhow_" + merband12_ix[i]);
                }
            }
        }

        if (outputRhown) {
            for (int i = 0; i < merband12_ix.length; i++) {
                tsc.defineSample(RHOWN_IX + i, "rhown_" + merband12_ix[i]);
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
        targetProduct.setProductType(PRODUCT_TYPE);
        C2rccCommons.ensureTimeInformation(targetProduct, sourceProduct.getStartTime(), sourceProduct.getEndTime(), timeCoding);

        targetProduct.setPreferredTileSize(128, 128);
        ProductUtils.copyFlagBands(sourceProduct, targetProduct, true);
        ProductNodeGroup<Mask> maskGroup = targetProduct.getMaskGroup();
        for (int i = 0; i < maskGroup.getNodeCount(); i++) {
            Mask mask = maskGroup.get(i);
            mask.setDescription("Copied from OLCI L1b input product");
        }

        final StringBuilder autoGrouping = new StringBuilder("iop");
        autoGrouping.append(":conc");

        if (outputRtoa) {
            for (int i : merband15_ix) {
                final Band band = addBand(targetProduct, "rtoa_" + i, "1", "Top-of-atmosphere reflectance");
                ensureSpectralProperties(band, i);
            }
            autoGrouping.append(":rtoa");
        }
        final String validPixelExpression = "c2rcc_flags.Valid_PE";
        if (outputRtosaGc) {
            for (int bi : merband12_ix) {
                Band band = addBand(targetProduct, "rtosa_gc_" + bi, "1", "Gas corrected top-of-atmosphere reflectance, input to AC");
                ensureSpectralProperties(band, bi);
                band.setValidPixelExpression(validPixelExpression);
            }
            autoGrouping.append(":rtosa_gc");
        }
        if (outputRtosaGcAann) {
            for (int bi : merband12_ix) {
                Band band = addBand(targetProduct, "rtosagc_aann_" + bi, "1", "Gas corrected top-of-atmosphere reflectance, output from AANN");
                ensureSpectralProperties(band, bi);
                band.setValidPixelExpression(validPixelExpression);
            }
            autoGrouping.append(":rtosagc_aann");
        }

        if (outputRpath) {
            for (int bi : merband12_ix) {
                Band band = addBand(targetProduct, "rpath_" + bi, "1", "Path-radiance reflectances");
                ensureSpectralProperties(band, bi);
                band.setValidPixelExpression(validPixelExpression);
            }
            autoGrouping.append(":rpath");
        }

        if (outputTdown) {
            for (int bi : merband12_ix) {
                Band band = addBand(targetProduct, "tdown_" + bi, "1", "Transmittance of downweling irradiance");
                ensureSpectralProperties(band, bi);
                band.setValidPixelExpression(validPixelExpression);
            }
            autoGrouping.append(":tdown");
        }

        if (outputTup) {
            for (int bi : merband12_ix) {
                Band band = addBand(targetProduct, "tup_" + bi, "1", "Transmittance of upweling irradiance");
                ensureSpectralProperties(band, bi);
                band.setValidPixelExpression(validPixelExpression);
            }
            autoGrouping.append(":tup");
        }

        if (outputAcReflectance) {
            for (int index : merband12_ix) {
                final Band band;
                if (outputAsRrs) {
                    band = addBand(targetProduct, "rrs_" + index, "sr^-1", "Atmospherically corrected angular dependent remote sensing reflectances");
                } else {
                    band = addBand(targetProduct, "rhow_" + index, "1", "Atmospherically corrected angular dependent water leaving reflectances");
                }
                ensureSpectralProperties(band, index);
                band.setValidPixelExpression(validPixelExpression);
            }
            if (outputAsRrs) {
                autoGrouping.append(":rrs");
            } else {
                autoGrouping.append(":rhow");
            }
        }

        if (outputRhown) {
            for (int index : merband12_ix) {
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
            Band unc_apig = addBand(targetProduct, "unc_apig", "m^-1", "Uncertainty of pigment absorption coefficient");
            Band unc_adet = addBand(targetProduct, "unc_adet", "m^-1", "Uncertainty of detritus absorption coefficient");
            Band unc_agelb = addBand(targetProduct, "unc_agelb", "m^-1", "Uncertainty of dissolved gelbstoff absorption coefficient");
            Band unc_bpart = addBand(targetProduct, "unc_bpart", "m^-1", "Uncertainty of particle scattering coefficient");
            Band unc_bwit = addBand(targetProduct, "unc_bwit", "m^-1", "Uncertainty of white particle scattering coefficient");
            Band unc_adg = addBand(targetProduct, "unc_adg", "m^-1", "Uncertainty of total gelbstoff absorption coefficient");
            Band unc_atot = addBand(targetProduct, "unc_atot", "m^-1", "Uncertainty of total water constituent absorption coefficient");
            Band unc_btot = addBand(targetProduct, "unc_btot", "m^-1", "Uncertainty of total water constituent scattering coefficient");

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

            Band unc_tsm = addVirtualBand(targetProduct, "unc_tsm", "unc_btot * " + TSMfakBpart, "g m^-3", "Uncertainty of total suspended matter (TSM) dry weight concentration");
            Band unc_chl = addVirtualBand(targetProduct, "unc_chl", "pow(unc_apig, " + CHLexp + ") * " + CHLfak, "mg m^-3", "Uncertainty of chlorophyll concentration");

            conc_tsm.addAncillaryVariable(unc_tsm, "uncertainty");
            conc_chl.addAncillaryVariable(unc_chl, "uncertainty");

            unc_tsm.setValidPixelExpression(validPixelExpression);
            unc_chl.setValidPixelExpression(validPixelExpression);

            if (outputKd) {
                Band unc_kd489 = addBand(targetProduct, "unc_kd489", "m^-1", "Uncertainty of irradiance attenuation coefficient");
                Band unc_kdmin = addBand(targetProduct, "unc_kdmin", "m^-1", "Uncertainty of mean irradiance attenuation coefficient");
                Band unc_kd_z90max = addVirtualBand(targetProduct, "unc_kd_z90max", "abs(kd_z90max - 1.0 / abs(kdmin - unc_kdmin))", "m", "Uncertainty of depth of the water column from which 90% of the water leaving irradiance comes from");

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
        flagCoding.addFlag("Rtosa_OOS", 0x01 << FLAG_INDEX_RTOSA_OOS, "The input spectrum to the atmospheric correction neural net was out of the scope of the training range and the inversion is likely to be wrong");
        flagCoding.addFlag("Rtosa_OOR", 0x01 << FLAG_INDEX_RTOSA_OOR, "The input spectrum to the atmospheric correction neural net out of training range");
        flagCoding.addFlag("Rhow_OOR", 0x01 << FLAG_INDEX_RHOW_OOR, "One of the inputs to the IOP retrieval neural net is out of training range");
        flagCoding.addFlag("Cloud_risk", 0x01 << FLAG_INDEX_CLOUD, "High downwelling transmission is indicating cloudy conditions");
        flagCoding.addFlag("Iop_OOR", 0x01 << FLAG_INDEX_IOP_OOR, "One of the IOPs is out of range");
        flagCoding.addFlag("Apig_at_max", 0x01 << FLAG_INDEX_APIG_AT_MAX, "Apig output of the IOP retrieval neural net is at its maximum. This means that the true value is this value or higher.");
        //5
        flagCoding.addFlag("Adet_at_max", 0x01 << FLAG_INDEX_ADET_AT_MAX, "Adet output of the IOP retrieval neural net is at its maximum. This means that the true value is this value or higher.");
        flagCoding.addFlag("Agelb_at_max", 0x01 << FLAG_INDEX_AGELB_AT_MAX, "Agelb output of the IOP retrieval neural net is at its maximum. This means that the true value is this value or higher.");
        flagCoding.addFlag("Bpart_at_max", 0x01 << FLAG_INDEX_BPART_AT_MAX, "Bpart output of the IOP retrieval neural net is at its maximum. This means that the true value is this value or higher.");
        flagCoding.addFlag("Bwit_at_max", 0x01 << FLAG_INDEX_BWIT_AT_MAX, "Bwit output of the IOP retrieval neural net is at its maximum. This means that the true value is this value or higher.");
        flagCoding.addFlag("Apig_at_min", 0x01 << FLAG_INDEX_APIG_AT_MIN, "Apig output of the IOP retrieval neural net is at its minimum. This means that the true value is this value or lower.");
        //10
        flagCoding.addFlag("Adet_at_min", 0x01 << FLAG_INDEX_ADET_AT_MIN, "Adet output of the IOP retrieval neural net is at its minimum. This means that the true value is this value or lower.");
        flagCoding.addFlag("Agelb_at_min", 0x01 << FLAG_INDEX_AGELB_AT_MIN, "Agelb output of the IOP retrieval neural net is at its minimum. This means that the true value is this value or lower.");
        flagCoding.addFlag("Bpart_at_min", 0x01 << FLAG_INDEX_BPART_AT_MIN, "Bpart output of the IOP retrieval neural net is at its minimum. This means that the true value is this value or lower.");
        flagCoding.addFlag("Bwit_at_min", 0x01 << FLAG_INDEX_BWIT_AT_MIN, "Bwit output of the IOP retrieval neural net is at its minimum. This means that the true value is this value or lower.");
        flagCoding.addFlag("Rhow_OOS", 0x01 << FLAG_INDEX_RHOW_OOS, "The Rhow input spectrum to IOP neural net is probably not within the training range of the neural net, and the inversion is likely to be wrong.");
        //15
        flagCoding.addFlag("Kd489_OOR", 0x01 << FLAG_INDEX_KD489_OOR, "Kd489 is out of range");
        flagCoding.addFlag("Kdmin_OOR", 0x01 << FLAG_INDEX_KDMIN_OOR, "Kdmin is out of range");
        flagCoding.addFlag("Kd489_at_max", 0x01 << FLAG_INDEX_KD489_AT_MAX, "Kdmin is at max");
        flagCoding.addFlag("Kdmin_at_max", 0x01 << FLAG_INDEX_KDMIN_AT_MAX, "Kdmin is at max");
        flagCoding.addFlag("Valid_PE", (int) (0x01L << FLAG_INDEX_VALID_PE), "The operators valid pixel expression has resolved to true");

        targetProduct.getFlagCodingGroup().add(flagCoding);
        c2rcc_flags.setSampleCoding(flagCoding);

        Color[] maskColors = {Color.RED, Color.ORANGE, Color.YELLOW, Color.BLUE, Color.GREEN, Color.PINK, Color.MAGENTA, Color.CYAN, Color.GRAY};
        String[] flagNames = flagCoding.getFlagNames();
        for (int i = 0; i < flagNames.length; i++) {
            String flagName = flagNames[i];
            MetadataAttribute flag = flagCoding.getFlag(flagName);
            double transparency = flagCoding.getFlagMask(flagName) == 0x01 << FLAG_INDEX_CLOUD ? 0.0 : 0.5;
            Color color = flagCoding.getFlagMask(flagName) == 0x01 << FLAG_INDEX_CLOUD ? Color.lightGray : maskColors[i % maskColors.length];
            targetProduct.addMask(flagName, "c2rcc_flags." + flagName, flag.getDescription(), color, transparency);
        }
        targetProduct.setAutoGrouping(autoGrouping.toString());
    }

    @Override
    protected void prepareInputs() throws OperatorException {
        String missingRasterMsgFormat = "Invalid source product, raster '%s' required";
        for (int i = 1; i <= BAND_COUNT; i++) {
            assertSourceRaster(getRadianceBandName(i), missingRasterMsgFormat);
            assertSourceRaster(getSolarFluxBandname(i), missingRasterMsgFormat);
        }

        assertSourceRaster(RASTER_NAME_SUN_ZENITH, missingRasterMsgFormat);
        assertSourceRaster(RASTER_NAME_SUN_AZIMUTH, missingRasterMsgFormat);
        assertSourceRaster(RASTER_NAME_VIEWING_ZENITH, missingRasterMsgFormat);
        assertSourceRaster(RASTER_NAME_VIEWING_AZIMUTH, missingRasterMsgFormat);

        if (useEcmwfAuxData) {
            String ecmwfMsg = "For ECMWF usage a '%s' raster is required";
            assertSourceRaster(RASTER_NAME_SEA_LEVEL_PRESSURE, ecmwfMsg);
            assertSourceRaster(RASTER_NAME_TOTAL_OZONE, ecmwfMsg);
        }

        sourceProduct.isCompatibleBandArithmeticExpression(validPixelExpression);

        useSnapDem = !sourceProduct.containsRasterDataNode(RASTER_NAME_ALTITUDE);
        if (useSnapDem) {
            elevationModel = ElevationModelRegistry.getInstance().getDescriptor("GETASSE30").createDem(Resampling.BILINEAR_INTERPOLATION);
        }

        if (sourceProduct.getSceneGeoCoding() == null) {
            throw new OperatorException("The source product must be geo-coded.");
        }
        timeCoding = C2rccCommons.getTimeCoding(sourceProduct);
    }

    @Override
    public void doExecute(ProgressMonitor pm) throws OperatorException {
        pm.beginTask("Preparing computation", 2);
        try {
            pm.setSubTaskName("Defining algorithm ...");
            if (StringUtils.isNotNullAndNotEmpty(alternativeNNPath)) {
                String[] nnFilePaths = NNUtils.getNNFilePaths(Paths.get(alternativeNNPath),
                        NNUtils.ALTERNATIVE_NET_DIR_NAMES);
                algorithm = new C2rccMeris4Algorithm(nnFilePaths, false);
            } else {
                String[] nnFilePaths = c2rccNetSetMap.get(netSet);
                if (nnFilePaths == null) {
                    throw new OperatorException(String.format("Unknown set '%s' of neural nets specified.", netSet));
                }
                algorithm = new C2rccMeris4Algorithm(nnFilePaths, true);
            }
            algorithm.setTemperature(temperature);
            algorithm.setSalinity(salinity);
            algorithm.setThresh_absd_log_rtosa(thresholdRtosaOOS);
            algorithm.setThresh_rwlogslope(thresholdAcReflecOos);
            algorithm.setThresh_cloudTransD(thresholdCloudTDown865);
            algorithm.setOutputRtoaGcAann(outputRtosaGcAann);
            algorithm.setOutputRpath(outputRpath);
            algorithm.setOutputTdown(outputTdown);
            algorithm.setOutputTup(outputTup);
            algorithm.setOutputRhow(outputAcReflectance);
            algorithm.setOutputRhown(outputRhown);
            algorithm.setOutputOos(outputOos);
            algorithm.setOutputKd(outputKd);
            algorithm.setOutputUncertainties(outputUncertainties);
            algorithm.setDeriveRwFromPathAndTransmittance(deriveRwFromPathAndTransmittance);
            addNnNamesMetadata();
            pm.worked(1);
            pm.setSubTaskName("Initialising atmospheric auxiliary data");
            initAtmosphericAuxdata();
            pm.worked(1);
        } catch (IOException e) {
            throw new OperatorException(e);
        } finally {
            pm.done();
        }
    }

    private static String getRadianceBandName(int index) {
        return String.format(RADIANCE_BANDNAME_PATTERN, index);
    }

    private static String getSolarFluxBandname(int index) {
        return String.format(SOLAR_FLUX_BANDNAME_PATTERN, index);
    }

    private void ensureSpectralProperties(Band band, int i) {
        ProductUtils.copySpectralBandProperties(sourceProduct.getBand(getRadianceBandName(i)), band);
        if (band.getSpectralWavelength() == 0) {
            band.setSpectralWavelength(DEFAULT_MERIS_WAVELENGTH[i - 1]);
            band.setSpectralBandIndex(i);
        }

    }

    private void addNnNamesMetadata() {
        final String[] nnNames = algorithm.getUsedNeuronalNetNames();
        final String alias = getSpi().getOperatorAlias();
        MetadataElement pgElement = getTargetProduct().getMetadataRoot().getElement("Processing_Graph");
        if (pgElement == null) {
            return;
        }
        for (MetadataElement nodeElement : pgElement.getElements()) {
            if (nodeElement.containsAttribute("operator") && alias.equals(nodeElement.getAttributeString("operator"))) {
                final MetadataElement neuronalNetsElem = new MetadataElement("neuronalNets");
                nodeElement.addElement(neuronalNetsElem);
                for (String nnName : nnNames) {
                    neuronalNetsElem.addAttribute(new MetadataAttribute("usedNeuralNet", ProductData.createInstance(nnName), true));
                }
                return;
            }
        }
    }

    private void initAtmosphericAuxdata() {
        AtmosphericAuxdataBuilder auxdataBuilder = new AtmosphericAuxdataBuilder();
        auxdataBuilder.setOzone(ozone);
        auxdataBuilder.setSurfacePressure(press);
        auxdataBuilder.useAtmosphericAuxDataPath(atmosphericAuxDataPath);
        auxdataBuilder.useTomsomiProducts(tomsomiStartProduct, tomsomiEndProduct);
        auxdataBuilder.useNcepProducts(ncepStartProduct, ncepEndProduct);
        if (useEcmwfAuxData) {
            String toDopsenExpr = String.format("%1$s < 1 ? %1$s * 46698 : %1$s", RASTER_NAME_TOTAL_OZONE);
            VirtualBand ozoneInDu = new VirtualBand("__ozone_in_du_",
                    ProductData.TYPE_FLOAT32,
                    getSourceProduct().getSceneRasterWidth(),
                    getSourceProduct().getSceneRasterHeight(),
                    toDopsenExpr);
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

    private void assertSourceRaster(String name, String msgFormat) {
        if (!sourceProduct.containsRasterDataNode(name)) {
            throw new OperatorException(String.format(msgFormat, name));
        }
    }

    public static class Spi extends OperatorSpi {
        static {
            RgbProfiles.installMeris4RgbProfiles();
        }

        public Spi() {
            super(C2rccMeris4Operator.class);
        }
    }
}
