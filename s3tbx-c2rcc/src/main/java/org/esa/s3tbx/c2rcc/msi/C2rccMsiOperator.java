package org.esa.s3tbx.c2rcc.msi;

import org.esa.s3tbx.c2rcc.C2rccConfigurable;
import org.esa.s3tbx.c2rcc.ancillary.AtmosphericAuxdata;
import org.esa.s3tbx.c2rcc.ancillary.AtmosphericAuxdataBuilder;
import org.esa.s3tbx.c2rcc.util.NNUtils;
import org.esa.s3tbx.c2rcc.util.RgbProfiles;
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
import org.esa.snap.core.datamodel.TimeCoding;
import org.esa.snap.core.dataop.dem.ElevationModel;
import org.esa.snap.core.dataop.dem.ElevationModelDescriptor;
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
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Level;

import static org.esa.s3tbx.c2rcc.C2rccCommons.*;
import static org.esa.s3tbx.c2rcc.msi.C2rccMsiAlgorithm.*;

// todo (nf) - Add min/max values of NN inputs and outputs to metadata (https://github.com/bcdev/s3tbx-c2rcc/issues/3)

/**
 * The Case 2 Regional / CoastColour Operator for OLCI.
 * <p/>
 * Computes AC-reflectances and IOPs from OLCI L1b data products using
 * an neural-network approach.
 *
 * @author Norman Fomferra
 */
@OperatorMetadata(alias = "c2rcc.msi", version = "1.0",
        authors = "Roland Doerffer, Marco Peters, Sabine Embacher (Brockmann Consult)",
        category = "Optical/Thematic Water Processing",
        copyright = "Copyright (C) 2016 by Brockmann Consult",
        description = "Performs atmospheric correction and IOP retrieval with uncertainties on Sentinel-2 MSI L1C data products.")
public class C2rccMsiOperator extends PixelOperator implements C2rccConfigurable {
    private static final int SUN_ZEN_IX = SOURCE_BAND_REFL_NAMES.length + 0;
    private static final int SUN_AZI_IX = SOURCE_BAND_REFL_NAMES.length + 1;
    private static final int VIEW_ZEN_IX = SOURCE_BAND_REFL_NAMES.length + 2;
    private static final int VIEW_AZI_IX = SOURCE_BAND_REFL_NAMES.length + 3;
    private static final int VALID_PIXEL_IX = SOURCE_BAND_REFL_NAMES.length + 4;
    /*
        c2rcc ops have been removed from Graph Builder. In the layer xml they are disabled
        see https://senbox.atlassian.net/browse/SNAP-395
    */

    // MSI targets
    private static final int NN_SPECTRUM_COUNT = NN_SOURCE_BAND_REFL_NAMES.length;
    private static final int NORM_NN_SPECTRUM_COUNT = NN_SPECTRUM_COUNT - 2;
    private static final int FULL_SPECTRUM_COUNT = SOURCE_BAND_REFL_NAMES.length;
    private static final int SINGLE_IX = FULL_SPECTRUM_COUNT + 7 * NN_SPECTRUM_COUNT;

    private static final int RTOA_IX = 0;
    private static final int RTOSA_IX = FULL_SPECTRUM_COUNT;
    private static final int RTOSA_AANN_IX = FULL_SPECTRUM_COUNT + NN_SPECTRUM_COUNT;
    private static final int RPATH_IX = FULL_SPECTRUM_COUNT + 2 * NN_SPECTRUM_COUNT;
    private static final int TDOWN_IX = FULL_SPECTRUM_COUNT + 3 * NN_SPECTRUM_COUNT;
    private static final int TUP_IX = FULL_SPECTRUM_COUNT + 4 * NN_SPECTRUM_COUNT;
    private static final int AC_REFLEC_IX = FULL_SPECTRUM_COUNT + 5 * NN_SPECTRUM_COUNT;
    private static final int RHOWN_IX = FULL_SPECTRUM_COUNT + 6 * NN_SPECTRUM_COUNT;

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

    private static final String PRODUCT_TYPE = "C2RCC_S2-MSI";

    static final String RASTER_NAME_SUN_ZENITH = "sun_zenith";
    static final String RASTER_NAME_SUN_AZIMUTH = "sun_azimuth";
    static final String RASTER_NAME_VIEW_ZENITH = "view_zenith_mean";
    static final String RASTER_NAME_VIEW_AZIMUTH = "view_azimuth_mean";

    private static final String STANDARD_NETS = "C2RCC-Nets";
    private static final String EXTREME_NETS = "C2X-Nets";
    private static final Map<String, String[]> c2rccNetSetMap = new HashMap<>();
    static {
        String[] standardNets = new String[10];
        standardNets[IDX_iop_rw] = "msi/std_s2_20160502/iop_rw/17x97x47_125.5.net";
        standardNets[IDX_iop_unciop] = "msi/std_s2_20160502/iop_unciop/17x77x37_11486.7.net";
        standardNets[IDX_iop_uncsumiop_unckd] = "msi/std_s2_20160502/iop_uncsumiop_unckd/17x77x37_9113.1.net";
        standardNets[IDX_rtosa_aann] = "msi/std_s2_20160502/rtosa_aann/31x7x31_78.0.net";
        standardNets[IDX_rtosa_rpath] = "msi/std_s2_20160502/rtosa_rpath/31x77x57x37_1564.4.net";
        standardNets[IDX_rtosa_rw] = "msi/std_s2_20160502/rtosa_rw/33x73x53x33_291140.4.net";
        standardNets[IDX_rtosa_trans] = "msi/std_s2_20160502/rtosa_trans/31x77x57x37_37537.6.net";
        standardNets[IDX_rw_iop] = "msi/std_s2_20160502/rw_iop/97x77x37_17515.9.net";
        standardNets[IDX_rw_kd] = "msi/std_s2_20160502/rw_kd/97x77x7_306.8.net";
        standardNets[IDX_rw_rwnorm] = "msi/std_s2_20160502/rw_rwnorm/27x7x27_28.0.net";
        c2rccNetSetMap.put(STANDARD_NETS, standardNets);

    }

    static {
        String[] extremeNets = new String[10];
        extremeNets[IDX_iop_rw] = "msi/ext_s2_elbetsm_20170320/iop_rw/77x77x77_28.3.net";
        extremeNets[IDX_iop_unciop] = "msi/ext_s2_elbetsm_20170320/iop_unciop/17x77x37_11486.7.net";
        extremeNets[IDX_iop_uncsumiop_unckd] = "msi/ext_s2_elbetsm_20170320/iop_uncsumiop_unckd/17x77x37_9113.1.net";
        extremeNets[IDX_rtosa_aann] = "msi/ext_s2_elbetsm_20170320/rtosa_aann/31x7x31_7.2.net";
        extremeNets[IDX_rtosa_rpath] = "msi/ext_s2_elbetsm_20170320/rtosa_rpath/37x37x37_175.7.net";
        extremeNets[IDX_rtosa_rw] = "msi/ext_s2_elbetsm_20170320/rtosa_rw/77x77x77x77_10688.3.net";
        extremeNets[IDX_rtosa_trans] = "msi/ext_s2_elbetsm_20170320/rtosa_trans/77x77x77_7809.2.net";
        extremeNets[IDX_rw_iop] = "msi/ext_s2_elbetsm_20170320/rw_iop/77x77x77_785.6.net";
        extremeNets[IDX_rw_kd] = "msi/ext_s2_elbetsm_20170320/rw_kd/77x77x77_61.6.net";
        extremeNets[IDX_rw_rwnorm] = "msi/ext_s2_elbetsm_20170320/rw_rwnorm/27x7x27_28.0.net";
        c2rccNetSetMap.put(EXTREME_NETS, extremeNets);
    }


    private static final DateFormat PRODUCT_DATE_FORMAT = ProductData.UTC.createDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");


    @SourceProduct(label = "MSI L1C product", description = "MSI L1C source product.")
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
            defaultValue = "B8 > 0 && B8 < 0.1",
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

    @Parameter(defaultValue = "0", unit = "m", interval = "(0, 8500)", label = "Elevation",
            description = "Used as fallback if elevation could not be taken from GETASSE30 DEM")
    private double elevation;

    @Parameter(defaultValue = "1.72", description = "Conversion factor bpart. (TSM = bpart * TSMfakBpart + bwit * TSMfakBwit)", label = "TSM factor bpart")
    private double TSMfakBpart;

    @Parameter(defaultValue = "3.1", description = "Conversion factor bwit. (TSM = bpart * TSMfakBpart + bwit * TSMfakBwit)", label = "TSM factor bwit")
    private double TSMfakBwit;

    @Parameter(defaultValue = "1.04", description = "Chlorophyll exponent ( CHL = iop-apig^CHLexp * CHLfak ) ", label = "CHL exponent")
    private double CHLexp;

    @Parameter(defaultValue = "21.0", description = "Chlorophyll factor ( CHL = iop-apig^CHLexp * CHLfak ) ", label = "CHL factor")
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

    @Parameter(valueSet = {STANDARD_NETS, EXTREME_NETS},
            description = "Set of neuronal nets for algorithm.",
            defaultValue = STANDARD_NETS,
            label = "Set of neuronal nets")
    private String netSet = STANDARD_NETS;

    @Parameter(defaultValue = "false", description =
            "Reflectance values in the target product shall be either written as remote sensing or water leaving reflectances",
            label = "Output AC reflectances as rrs instead of rhow")
    private boolean outputAsRrs;

    @Parameter(defaultValue = "false", description = "Alternative way of calculating water reflectance. Still experimental.",
            label = "Derive water reflectance from path radiance and transmittance")
    private boolean deriveRwFromPathAndTransmittance;

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

    private C2rccMsiAlgorithm algorithm;
    private AtmosphericAuxdata atmosphericAuxdata;
    private ElevationModel elevationModel;
    private double[] solflux;
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


    @Override
    public void setValidPixelExpression(String validPixelExpression) {
        this.validPixelExpression = validPixelExpression;
    }

    @Override
    public void setOutputRtosa(boolean outputRtosa) {
        this.outputRtosaGc = outputRtosa;
    }

    @Override
    public void setOutputAsRrs(boolean asRadianceRefl) {
        outputAsRrs = asRadianceRefl;
    }

    public void setDeriveRwFromPathAndTransmittance(boolean deriveRwFromPathAndTransmittance) {
        this.deriveRwFromPathAndTransmittance = deriveRwFromPathAndTransmittance;
    }

    void setOutputKd(boolean outputKd) {
        this.outputKd = outputKd;
    }

    void setOutputOos(boolean outputOos) {
        this.outputOos = outputOos;
    }

    void setOutputRpath(boolean outputRpath) {
        this.outputRpath = outputRpath;
    }

    void setOutputRtoa(boolean outputRtoa) {
        this.outputRtoa = outputRtoa;
    }

    void setOutputRtosaGcAann(boolean outputRtosaGcAann) {
        this.outputRtosaGcAann = outputRtosaGcAann;
    }

    void setOutputAcReflectance(boolean outputAcReflectance) {
        this.outputAcReflectance = outputAcReflectance;
    }

    void setOutputRhown(boolean outputRhown) {
        this.outputRhown = outputRhown;
    }

    void setOutputTdown(boolean outputTdown) {
        this.outputTdown = outputTdown;
    }

    void setOutputTup(boolean outputTup) {
        this.outputTup = outputTup;
    }

    void setOutputUncertainties(boolean outputUncertainties) {
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

    public static boolean isValidInput(Product product) {
        for (String SOURCE_BAND_REFL_NAME : SOURCE_BAND_REFL_NAMES) {
            if (!product.containsBand(SOURCE_BAND_REFL_NAME)) {
                return false;
            }
        }

        return product.containsRasterDataNode(RASTER_NAME_SUN_ZENITH)
                && product.containsRasterDataNode(RASTER_NAME_SUN_AZIMUTH)
                && product.containsRasterDataNode(RASTER_NAME_VIEW_ZENITH)
                && product.containsRasterDataNode(RASTER_NAME_VIEW_AZIMUTH);
    }

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {
        final double[] reflectances = new double[C2rccMsiAlgorithm.SOURCE_BAND_REFL_NAMES.length];
        for (int i = 0; i < reflectances.length; i++) {
            reflectances[i] = sourceSamples[i].getDouble();
        }

        final PixelPos pixelPos = new PixelPos(x + 0.5f, y + 0.5f);

        GeoPos geoPos = sourceProduct.getSceneGeoCoding().getGeoPos(pixelPos, null);
        double lat = geoPos.getLat();
        double lon = geoPos.getLon();

        final double mjd = timeCoding.getMJD(pixelPos);
        double ozone = fetchOzone(atmosphericAuxdata, mjd, x, y, lat, lon);
        double atmPress = fetchSurfacePressure(atmosphericAuxdata, mjd, x, y, lat, lon);

        final double altitude;
        if (elevationModel != null) {
            try {
                altitude = elevationModel.getElevation(geoPos);
            } catch (Exception e) {
                throw new OperatorException("Unable to compute altitude.", e);
            }
        } else {
            // in case elevationModel could not be initialised
            altitude = elevation;
        }

        Result result = algorithm.processPixel(x, y, lat, lon,
                                               reflectances,
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
        for (int i = 0; i < C2rccMsiAlgorithm.SOURCE_BAND_REFL_NAMES.length; i++) {
            sc.defineSample(i, C2rccMsiAlgorithm.SOURCE_BAND_REFL_NAMES[i]);
        }
        sc.defineSample(SUN_ZEN_IX, RASTER_NAME_SUN_ZENITH);
        sc.defineSample(SUN_AZI_IX, RASTER_NAME_SUN_AZIMUTH);
        sc.defineSample(VIEW_ZEN_IX, RASTER_NAME_VIEW_ZENITH);
        sc.defineSample(VIEW_AZI_IX, RASTER_NAME_VIEW_AZIMUTH);
        if (StringUtils.isNotNullAndNotEmpty(validPixelExpression)) {
            sc.defineComputedSample(VALID_PIXEL_IX, ProductData.TYPE_UINT8, validPixelExpression);
        } else {
            sc.defineComputedSample(VALID_PIXEL_IX, ProductData.TYPE_UINT8, "true");
        }

    }

    @Override
    protected void configureTargetSamples(TargetSampleConfigurer tsc) throws OperatorException {

        if (outputRtoa) {
            for (int i = 0; i < FULL_SPECTRUM_COUNT; i++) {
                tsc.defineSample(RTOA_IX + i, "rtoa_" + C2rccMsiAlgorithm.SOURCE_BAND_REFL_NAMES[i]);
            }
        }

        if (outputRtosaGc) {
            for (int i = 0; i < NN_SPECTRUM_COUNT; i++) {
                tsc.defineSample(RTOSA_IX + i, "rtosa_gc_" + NN_SOURCE_BAND_REFL_NAMES[i]);
            }
        }

        if (outputRtosaGcAann) {
            for (int i = 0; i < NN_SPECTRUM_COUNT; i++) {
                tsc.defineSample(RTOSA_AANN_IX + i, "rtosagc_aann_" + NN_SOURCE_BAND_REFL_NAMES[i]);
            }
        }

        if (outputRpath) {
            for (int i = 0; i < NN_SPECTRUM_COUNT; i++) {
                tsc.defineSample(RPATH_IX + i, "rpath_" + NN_SOURCE_BAND_REFL_NAMES[i]);
            }
        }

        if (outputTdown) {
            for (int i = 0; i < NN_SPECTRUM_COUNT; i++) {
                tsc.defineSample(TDOWN_IX + i, "tdown_" + NN_SOURCE_BAND_REFL_NAMES[i]);
            }
        }

        if (outputTup) {
            for (int i = 0; i < NN_SPECTRUM_COUNT; i++) {
                tsc.defineSample(TUP_IX + i, "tup_" + NN_SOURCE_BAND_REFL_NAMES[i]);
            }
        }

        if (outputAcReflectance) {
            for (int i = 0; i < NN_SPECTRUM_COUNT; i++) {
                if (outputAsRrs) {
                    tsc.defineSample(AC_REFLEC_IX + i, "rrs_" + NN_SOURCE_BAND_REFL_NAMES[i]);
                } else {
                    tsc.defineSample(AC_REFLEC_IX + i, "rhow_" + NN_SOURCE_BAND_REFL_NAMES[i]);
                }
            }
        }

        if (outputRhown) {
            for (int i = 0; i < NORM_NN_SPECTRUM_COUNT; i++) {
                tsc.defineSample(RHOWN_IX + i, "rhown_" + NN_SOURCE_BAND_REFL_NAMES[i]);
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
        ensureTimeInformation(targetProduct, getStartTime(), getEndTime(), timeCoding);

        targetProduct.setPreferredTileSize(128, 128);
        ProductUtils.copyFlagBands(sourceProduct, targetProduct, true);

        final StringBuilder autoGrouping = new StringBuilder("iop");
        autoGrouping.append(":conc");

        if (outputRtoa) {
            for (int i = 0; i < FULL_SPECTRUM_COUNT; i++) {
                String sourceBandName = C2rccMsiAlgorithm.SOURCE_BAND_REFL_NAMES[i];
                final Band band = addBand(targetProduct, "rtoa_" + sourceBandName, "1", "Top-of-atmosphere reflectance");
                ensureSpectralProperties(band, sourceBandName);
            }
            autoGrouping.append(":rtoa");
        }
        final String validPixelExpression = "c2rcc_flags.Valid_PE";
        if (outputRtosaGc) {
            for (int i = 0; i < NN_SPECTRUM_COUNT; i++) {
                String sourceBandName = NN_SOURCE_BAND_REFL_NAMES[i];
                Band band = addBand(targetProduct, "rtosa_gc_" + sourceBandName, "1", "Gas corrected top-of-atmosphere reflectance, input to AC");
                ensureSpectralProperties(band, sourceBandName);
                band.setValidPixelExpression(validPixelExpression);
            }
            autoGrouping.append(":rtosa_gc");
        }
        if (outputRtosaGcAann) {
            for (int i = 0; i < NN_SPECTRUM_COUNT; i++) {
                String sourceBandName = NN_SOURCE_BAND_REFL_NAMES[i];
                Band band = addBand(targetProduct, "rtosagc_aann_" + sourceBandName, "1", "Gas corrected top-of-atmosphere reflectance, output from AANN");
                ensureSpectralProperties(band, sourceBandName);
                band.setValidPixelExpression(validPixelExpression);
            }
            autoGrouping.append(":rtosagc_aann");
        }

        if (outputRpath) {
            for (int i = 0; i < NN_SPECTRUM_COUNT; i++) {
                String sourceBandName = NN_SOURCE_BAND_REFL_NAMES[i];
                Band band = addBand(targetProduct, "rpath_" + sourceBandName, "1", "Path-radiance reflectances");
                ensureSpectralProperties(band, sourceBandName);
                band.setValidPixelExpression(validPixelExpression);
            }
            autoGrouping.append(":rpath");
        }

        if (outputTdown) {
            for (int i = 0; i < NN_SPECTRUM_COUNT; i++) {
                String sourceBandName = NN_SOURCE_BAND_REFL_NAMES[i];
                Band band = addBand(targetProduct, "tdown_" + sourceBandName, "1", "Transmittance of downweling irradiance");
                ensureSpectralProperties(band, sourceBandName);
                band.setValidPixelExpression(validPixelExpression);
            }
            autoGrouping.append(":tdown");
        }

        if (outputTup) {
            for (int i = 0; i < NN_SPECTRUM_COUNT; i++) {
                String sourceBandName = NN_SOURCE_BAND_REFL_NAMES[i];
                Band band = addBand(targetProduct, "tup_" + sourceBandName, "1", "Transmittance of upweling irradiance");
                ensureSpectralProperties(band, sourceBandName);
                band.setValidPixelExpression(validPixelExpression);
            }
            autoGrouping.append(":tup");
        }

        if (outputAcReflectance) {
            for (int i = 0; i < NN_SPECTRUM_COUNT; i++) {
                String sourceBandName = NN_SOURCE_BAND_REFL_NAMES[i];
                final Band band;
                if (outputAsRrs) {
                    band = addBand(targetProduct, "rrs_" + sourceBandName, "sr^-1", "Atmospherically corrected Angular dependent remote sensing reflectances");
                } else {
                    band = addBand(targetProduct, "rhow_" + sourceBandName, "1", "Atmospherically corrected Angular dependent water leaving reflectances");
                }
                ensureSpectralProperties(band, sourceBandName);
                band.setValidPixelExpression(validPixelExpression);
            }
            if (outputAsRrs) {
                autoGrouping.append(":rrs");
            }else {
                autoGrouping.append(":rhow");
            }
        }

        if (outputRhown) {
            for (int i = 0; i < NORM_NN_SPECTRUM_COUNT; i++) {
                String sourceBandName = NN_SOURCE_BAND_REFL_NAMES[i];
                final Band band = addBand(targetProduct, "rhown_" + sourceBandName, "1", "Normalized water leaving reflectances");
                ensureSpectralProperties(band, sourceBandName);
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
        Band conc_chl = addVirtualBand(targetProduct, "conc_chl", "pow(iop_apig, " + CHLexp + ") * " + CHLfak, "mg m^-3", "Chlorophylll concentration");

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
            Band unc_chl = addVirtualBand(targetProduct, "unc_chl", "pow(unc_apig, " + CHLexp + ") * " + CHLfak, "mg m^-3", "uncertainty of chlorophylll concentration");

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

        targetProduct.addProductNodeListener(getNnNamesMetadataAppender());
    }

    @Override
    protected void prepareInputs() throws OperatorException {
        try {
            for (String sourceBandName : C2rccMsiAlgorithm.SOURCE_BAND_REFL_NAMES) {
                assertSourceBand(sourceBandName);
            }

            String msgFormat = "Invalid source product, raster '%s' required";
            assertSourceRaster(RASTER_NAME_SUN_ZENITH, msgFormat);
            assertSourceRaster(RASTER_NAME_SUN_AZIMUTH, msgFormat);
            assertSourceRaster(RASTER_NAME_VIEW_ZENITH, msgFormat);
            assertSourceRaster(RASTER_NAME_VIEW_AZIMUTH, msgFormat);

            if (sourceProduct.getSceneGeoCoding() == null) {
                throw new OperatorException("The source product must be geo-coded.");
            }

            ensureSingleRasterSize(sourceProduct);
        } catch (OperatorException e) {
            throw new OperatorException("Source must be a resampled S2 MSI L1C product", e);
        }


        ElevationModelDescriptor getasse30 = ElevationModelRegistry.getInstance().getDescriptor("GETASSE30");
        if (getasse30 != null) {
            // if elevation model cannot be initialised the fallback height will be used
            elevationModel = getasse30.createDem(Resampling.BILINEAR_INTERPOLATION);
        }
        // (mp/20160504) - SolarFlux is not used so we set it to 0
        solflux = new double[SOURCE_BAND_REFL_NAMES.length]; //getSolarFluxValues();



        try {
            if (StringUtils.isNotNullAndNotEmpty(alternativeNNPath)) {
                String[] nnFilePaths = NNUtils.getNNFilePaths(Paths.get(alternativeNNPath), NNUtils.ALTERNATIVE_NET_DIR_NAMES);
                algorithm = new C2rccMsiAlgorithm(nnFilePaths, false);
            } else {
                String[] nnFilePaths = c2rccNetSetMap.get(netSet);
                if(nnFilePaths == null) {
                    throw new OperatorException(String.format("Unknown set '%s' of neural nets specified.", netSet));
                }
                algorithm = new C2rccMsiAlgorithm(nnFilePaths, true);
            }
        } catch (IOException e) {
            throw new OperatorException(e);
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

        timeCoding = sourceProduct.getSceneTimeCoding();
        if (timeCoding == null) {
            // if not time coding is set, create one
            if (sourceProduct.getStartTime() == null || sourceProduct.getEndTime() == null) {
                // if no start/end time is set, read it from the metadata
                // (should not happen anymore from SNAP 4.0.2 on)
                timeCoding = getTimeCoding(getStartTime(), getEndTime());
            } else {
                timeCoding = getTimeCoding(sourceProduct);
            }

        }
        initAtmosphericAuxdata();
    }

    private ProductData.UTC getStartTime() {
        ProductData.UTC startTime = sourceProduct.getStartTime();
        if (startTime == null) {
            MetadataElement gi = getGeneralInfo();
            MetadataElement productInfo = getSubElementSafe(gi, "Product_Info");
            startTime = getTime(productInfo, PRODUCT_DATE_FORMAT, "PRODUCT_START_TIME");
        }
        return startTime;
    }

    private ProductData.UTC getEndTime() {
        ProductData.UTC endTime = sourceProduct.getEndTime();
        if (endTime == null) {
            MetadataElement gi = getGeneralInfo();
            MetadataElement productInfo = getSubElementSafe(gi, "Product_Info");
            endTime = getTime(productInfo, PRODUCT_DATE_FORMAT, "PRODUCT_STOP_TIME");
        }
        return endTime;
    }

    private ProductData.UTC getTime(MetadataElement productInfo, DateFormat dateFormat, String timeAttrName) {
        try {
            Date date = dateFormat.parse(getAttributeStringSafe(productInfo, timeAttrName));
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.ENGLISH);
            calendar.setTime(date);
            int millis = calendar.get(Calendar.MILLISECOND);
            calendar.set(Calendar.MILLISECOND, 0);
            return ProductData.UTC.create(calendar.getTime(), millis * 1000);
        } catch (ParseException e) {
            getLogger().log(Level.WARNING, "Could not retrieve " + timeAttrName + " from metadata");
            return null;
        }
    }

    // (mp/20160504) - SolarFlux is not used so we set it to 0
    private double[] getSolarFluxValues() {
        MetadataElement pic = getProductImageCharacteristics();
        MetadataElement reflCon = getSubElementSafe(pic, "Reflectance_Conversion");
        MetadataElement solIrrList = getSubElementSafe(reflCon, "Solar_Irradiance_List");
        MetadataAttribute[] attributes = solIrrList.getAttributes();
        final double[] solflux = new double[attributes.length];
        for (int i = 0; i < attributes.length; i++) {
            MetadataAttribute attribute = attributes[i];
            solflux[i] = Float.parseFloat(attribute.getData().getElemString());
        }
        return solflux;
    }

    private MetadataElement getProductImageCharacteristics() {
        return getSubElementSafe(getGeneralInfo(), "Product_Image_Characteristics");
    }

    private MetadataElement getSubElementSafe(MetadataElement element, String subElementName) {
        if (element.containsElement(subElementName)) {
            return element.getElement(subElementName);
        } else {
            String formatStr = "Metadata not found: The element '%s' does not contain a sub-element with the name '%s'";
            String msg = String.format(formatStr, element.getName(), subElementName);
            throw new IllegalStateException(msg);
        }
    }

    private String getAttributeStringSafe(MetadataElement element, String attrName) {
        if (element.containsAttribute(attrName)) {
            return element.getAttributeString(attrName);
        } else {
            String elementName = element.getName();
            String formatStr = "Metadata not found: The element '%s' does not contain an attribute with the name '%s'";
            String msg = String.format(formatStr, elementName, attrName);
            throw new IllegalStateException(msg);
        }
    }

    private MetadataElement getGeneralInfo() {
        MetadataElement l1cUserProduct = getSubElementSafe(sourceProduct.getMetadataRoot(), "Level-1C_User_Product");
        return getSubElementSafe(l1cUserProduct, "General_Info");
    }

    private void ensureSpectralProperties(Band band, String sourceBandName) {
        Band sourceBand = sourceProduct.getBand(sourceBandName);
        ProductUtils.copySpectralBandProperties(sourceBand, band);
        if (band.getSpectralWavelength() == 0) {
            band.setSpectralWavelength(sourceBand.getSpectralWavelength());
            band.setSpectralBandIndex(sourceBand.getSpectralBandIndex());
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

    private void assertSourceBand(String name) {
        if (!sourceProduct.containsBand(name)) {
            throw new OperatorException("Invalid source product, band '" + name + "' required");
        }
    }

    public static class Spi extends OperatorSpi {
        static {
            RgbProfiles.installS2MsiRgbProfiles();
        }

        public Spi() {
            super(C2rccMsiOperator.class);
        }
    }
}
