package org.esa.s3tbx.c2rcc.landsat;

import com.bc.ceres.core.Assert;
import org.esa.s3tbx.c2rcc.C2rccCommons;
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
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.TimeCoding;
import org.esa.snap.core.dataop.dem.ElevationModel;
import org.esa.snap.core.dataop.dem.ElevationModelDescriptor;
import org.esa.snap.core.dataop.dem.ElevationModelRegistry;
import org.esa.snap.core.dataop.resamp.Resampling;
import org.esa.snap.core.gpf.GPF;
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

import static org.esa.s3tbx.c2rcc.C2rccCommons.*;
import static org.esa.s3tbx.c2rcc.landsat.C2rccLandsat8Algorithm.*;

// todo (nf) - Add Thullier solar fluxes as default values to C2RCC operator (https://github.com/bcdev/s3tbx-c2rcc/issues/1)
// todo (nf) - Add flags band and check for OOR of inputs and outputs of the NNs (https://github.com/bcdev/s3tbx-c2rcc/issues/2)
// todo (nf) - Add min/max values of NN inputs and outputs to metadata (https://github.com/bcdev/s3tbx-c2rcc/issues/3)
// todo (RD) - salinity and temperature have to be passed to C2R ?
// todo (RD) - parameters, to control which variables to be processed, pass to C2R

/**
 * The Case 2 Regional / CoastColour Operator for MERIS.
 * <p/>
 * Computes AC-reflectances and IOPs from MERIS L1b data products using
 * an neural-network approach.
 */
@OperatorMetadata(alias = "c2rcc.landsat8", version = "1.1",
        authors = "Roland Doerffer, Marco Peters (Brockmann Consult)",
        category = "Optical/Thematic Water Processing",
        copyright = "Copyright (C) 2016 by Brockmann Consult",
        description = "Performs atmospheric correction and IOP retrieval with uncertainties on Landsat-8 L1 data products.")
public class C2rccLandsat8Operator extends PixelOperator implements C2rccConfigurable {
    /*
        c2rcc ops have been removed from Graph Builder. In the layer xml they are disabled
        see https://senbox.atlassian.net/browse/SNAP-395
    */

    // Landsat sources
    static final String[] EXPECTED_BANDNAMES = new String[]{"coastal_aerosol", "blue", "green", "red", "near_infrared"};
    static final int L8_BAND_COUNT = EXPECTED_BANDNAMES.length;
    private static final String[] ANGEL_BANDNAMES = new String[]{"view_azimuth", "view_zenith", "sun_azimuth", "sun_zenith"};


    private static final int VALID_PIXEL_IX = L8_BAND_COUNT + 1;

    private static final int RTOA_IX = 0;
    private static final int RTOSA_IX = L8_BAND_COUNT;
    private static final int RTOSA_AANN_IX = 2 * L8_BAND_COUNT;
    private static final int RPATH_IX = 3 * L8_BAND_COUNT;
    private static final int TDOWN_IX = 4 * L8_BAND_COUNT;
    private static final int TUP_IX = 5 * L8_BAND_COUNT;
    private static final int AC_REFLEC_IX = 6 * L8_BAND_COUNT;
    private static final int RHOWN_IX = 7 * L8_BAND_COUNT;

    private static final int OOS_RTOSA_IX = 8 * L8_BAND_COUNT;
    private static final int OOS_AC_REFLEC_IX = OOS_RTOSA_IX + 1;

    private static final int IOP_APIG_IX = OOS_RTOSA_IX + 2;
    private static final int IOP_ADET_IX = OOS_RTOSA_IX + 3;
    private static final int IOP_AGELB_IX = OOS_RTOSA_IX + 4;
    private static final int IOP_BPART_IX = OOS_RTOSA_IX + 5;
    private static final int IOP_BWIT_IX = OOS_RTOSA_IX + 6;

    private static final int KD489_IX = OOS_RTOSA_IX + 7;
    private static final int KDMIN_IX = OOS_RTOSA_IX + 8;

    private static final int UNC_APIG_IX = OOS_RTOSA_IX + 9;
    private static final int UNC_ADET_IX = OOS_RTOSA_IX + 10;
    private static final int UNC_AGELB_IX = OOS_RTOSA_IX + 11;
    private static final int UNC_BPART_IX = OOS_RTOSA_IX + 12;
    private static final int UNC_BWIT_IX = OOS_RTOSA_IX + 13;
    private static final int UNC_ADG_IX = OOS_RTOSA_IX + 14;
    private static final int UNC_ATOT_IX = OOS_RTOSA_IX + 15;
    private static final int UNC_BTOT_IX = OOS_RTOSA_IX + 16;
    private static final int UNC_KD489_IX = OOS_RTOSA_IX + 17;
    private static final int UNC_KDMIN_IX = OOS_RTOSA_IX + 18;

    private static final int C2RCC_FLAGS_IX = OOS_RTOSA_IX + 19;

    private static final int DEBUG_VIEW_AZI_IX = OOS_RTOSA_IX + 20;
    private static final int DEBUG_VIEW_ZEN_IX = OOS_RTOSA_IX + 21;
    private static final int DEBUG_SUN_AZI_IX = OOS_RTOSA_IX + 22;
    private static final int DEBUG_SUN_ZEN_IX = OOS_RTOSA_IX + 23;

    private static final String PRODUCT_TYPE = "C2RCC_LANDSAT-8";

    private static final String STANDARD_NETS = "C2RCC-Nets";
    private static final String EXTREME_NETS = "C2X-Nets";
    private static final Map<String, String[]> c2rccNetSetMap = new HashMap<>();

    static {
        String[] standardNets = new String[10];
        standardNets[IDX_iop_rw] = "landsat/l8_nets_20160818/iop_rw/17x97x47_79.9.net";
        standardNets[IDX_iop_unciop] = "landsat/l8_nets_20160818/iop_unciop/17x77x37_11486.7.net";
        standardNets[IDX_iop_uncsumiop_unckd] = "landsat/l8_nets_20160818/iop_uncsumiop_unckd/17x77x37_9113.1.net";
        standardNets[IDX_rtosa_aann] = "landsat/l8_nets_20160818/rtosa_aann/29x7x29_56.3.net";
        standardNets[IDX_rtosa_rpath] = "landsat/l8_nets_20160818/rtosa_rpath/31x77x57x37_2336.9.net";
        standardNets[IDX_rtosa_rw] = "landsat/l8_nets_20160818/rtosa_rw/31x71x51x31_229436.1.net";
        standardNets[IDX_rtosa_trans] = "landsat/l8_nets_20160818/rtosa_trans/29x75x55x35_28119.6.net";
        standardNets[IDX_rw_iop] = "landsat/l8_nets_20160818/rw_iop/47x97x17_15723.1.net";
        standardNets[IDX_rw_kd] = "landsat/l8_nets_20160818/rw_kd/97x77x7_268.5.net";
        standardNets[IDX_rw_rwnorm] = "landsat/l8_nets_20160818/rw_rwnorm/27x7x27_9.7.net";
        c2rccNetSetMap.put(STANDARD_NETS, standardNets);
    }

    static {
        String[] extremeNets = new String[10];
        extremeNets[IDX_iop_rw] = "landsat/l8_hitsm_20161115/iop_rw/17x97x47_106.0.net";
        extremeNets[IDX_iop_unciop] = "landsat/l8_hitsm_20161115/iop_unciop/17x77x37_11486.7.net";
        extremeNets[IDX_iop_uncsumiop_unckd] = "landsat/l8_hitsm_20161115/iop_uncsumiop_unckd/17x77x37_9113.1.net";
        extremeNets[IDX_rtosa_aann] = "landsat/l8_hitsm_20161115/rtosa_aann/29x7x29_46.7.net";
        extremeNets[IDX_rtosa_rpath] = "landsat/l8_hitsm_20161115/rtosa_rpath/31x37_3296.0.net";
        extremeNets[IDX_rtosa_rw] = "landsat/l8_hitsm_20161115/rtosa_rw/31x77x57x37_160378.5.net";
        extremeNets[IDX_rtosa_trans] = "landsat/l8_hitsm_20161115/rtosa_trans/29x75x55x35_27514.5.net";
        extremeNets[IDX_rw_iop] = "landsat/l8_hitsm_20161115/rw_iop/47x97x17_10481.8.net";
        extremeNets[IDX_rw_kd] = "landsat/l8_hitsm_20161115/rw_kd/97x77x7_681.2.net";
        extremeNets[IDX_rw_rwnorm] = "landsat/l8_hitsm_20161115/rw_rwnorm/27x7x27_9.7.net";
        c2rccNetSetMap.put(EXTREME_NETS, extremeNets);
    }

    @SourceProduct(label = "Landsat 8 product",
            description = "Landsat 8 source product.")
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
            defaultValue = "water_confidence_mid || water_confidence_high",
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

    private C2rccLandsat8Algorithm algorithm;
    private AtmosphericAuxdata atmosphericAuxdata;
    private TimeCoding timeCoding;
    private double[] reflectance_offset;
    private double[] reflectance_scale;
    private double sunAzimuth;
    private double sunZenith;
    private GeometryAnglesBuilder geometryAnglesBuilder;
    private ElevationModel elevationModel;
    private Product resampledProduct;

    private boolean debug_outputAngles = false;
    private boolean useAngleBands = false;


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

    public void setOutputRtosaGcAann(boolean outputRtoaGcAann) {
        this.outputRtosaGcAann = outputRtoaGcAann;
    }

    public void setOutputAcReflectance(boolean outputAcReflectance) {
        this.outputAcReflectance = outputAcReflectance;
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
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {
        double[] reflectances = new double[L8_BAND_COUNT];
        for (int i = 0; i < L8_BAND_COUNT; i++) {
            Sample sample = sourceSamples[i];
            double sourceValue = sample.getDouble();
            RasterDataNode rasterNode = sample.getNode();
            if (rasterNode.getDescription().contains("TOA Reflectance")) {
                reflectances[i] = sourceValue;
            } else {
                double radianceOffset = rasterNode.getScalingOffset();
                double radianceScaling = rasterNode.getScalingFactor();
                reflectances[i] = toReflectances(sourceValue, radianceOffset, radianceScaling,
                                                 reflectance_offset[i], reflectance_scale[i]);
            }
        }

        final PixelPos pixelPos = new PixelPos(x + 0.5f, y + 0.5f);
        final double mjd = timeCoding.getMJD(pixelPos);

        GeoPos geoPos = sourceProduct.getSceneGeoCoding().getGeoPos(pixelPos, null);
        double lat = geoPos.getLat();
        double lon = geoPos.getLon();
        double atmPress = fetchSurfacePressure(atmosphericAuxdata, mjd, x, y, lat, lon);
        double ozone = fetchOzone(atmosphericAuxdata, mjd, x, y, lat, lon);
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

        double viewAzimuth;
        double viewZenith;
        if (useAngleBands) {
            viewAzimuth = sourceSamples[VALID_PIXEL_IX + 1].getDouble();
            viewZenith = sourceSamples[VALID_PIXEL_IX + 2].getDouble();
            sunAzimuth = sourceSamples[VALID_PIXEL_IX + 3].getDouble();
            sunZenith = sourceSamples[VALID_PIXEL_IX + 4].getDouble();
        } else {
            GeometryAngles geometryAngles = geometryAnglesBuilder.getGeometryAngles(x, lat);
            viewAzimuth = geometryAngles.view_azimuth;
            viewZenith = geometryAngles.view_zenith;
        }

        Result result = algorithm.processPixel(x, y, lat, lon,
                                               reflectances,
                                               new double[0],
                                               sunZenith,
                                               sunAzimuth,
                                               viewZenith,
                                               viewAzimuth,
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

        if (debug_outputAngles) {
            targetSamples[DEBUG_VIEW_AZI_IX].set(viewAzimuth);
            targetSamples[DEBUG_VIEW_ZEN_IX].set(viewZenith);
            targetSamples[DEBUG_SUN_AZI_IX].set(sunAzimuth);
            targetSamples[DEBUG_SUN_ZEN_IX].set(sunZenith);
        }
    }

    @Override
    protected void configureSourceSamples(SourceSampleConfigurer sc) throws OperatorException {
        for (int i = 0; i < EXPECTED_BANDNAMES.length; i++) {
            sc.defineSample(i, EXPECTED_BANDNAMES[i], resampledProduct);
        }
        if (StringUtils.isNotNullAndNotEmpty(validPixelExpression)) {
            sc.defineComputedSample(VALID_PIXEL_IX, ProductData.TYPE_UINT8, validPixelExpression, resampledProduct);
        } else {
            sc.defineComputedSample(VALID_PIXEL_IX, ProductData.TYPE_UINT8, "true", resampledProduct);
        }

        if (useAngleBands) {
            for (int i = 0; i < ANGEL_BANDNAMES.length; i++) {
                sc.defineSample((VALID_PIXEL_IX + 1) + i, ANGEL_BANDNAMES[i], resampledProduct);
            }
        }

    }

    @Override
    protected Product createTargetProduct() throws OperatorException {
        Assert.state(resampledProduct != null, "source product not set");
        return new Product(getId(),
                           getClass().getName(),
                           resampledProduct.getSceneRasterWidth(),
                           resampledProduct.getSceneRasterHeight());
    }

    @Override
    protected void configureTargetProduct(ProductConfigurer productConfigurer) {
        final Product targetProduct = productConfigurer.getTargetProduct();
        targetProduct.setProductType(PRODUCT_TYPE);
        targetProduct.setStartTime(getSourceProduct().getStartTime());
        targetProduct.setEndTime(getSourceProduct().getEndTime());
        ProductUtils.copyTiePointGrids(resampledProduct, targetProduct);
        ProductUtils.copyGeoCoding(resampledProduct, targetProduct);
        ProductUtils.copyMetadata(resampledProduct, targetProduct);

        C2rccCommons.ensureTimeInformation(targetProduct, resampledProduct.getStartTime(), resampledProduct.getEndTime(), timeCoding);
        ProductUtils.copyFlagBands(resampledProduct, targetProduct, true);

        final StringBuilder autoGrouping = new StringBuilder("iop");
        autoGrouping.append(":conc");

        if (outputRtoa) {
            for (int i = 0; i < L8_BAND_COUNT; i++) {
                final Band band = addBand(targetProduct, "rtoa_" + (i + 1), "1", "Top-of-atmosphere reflectance");
                ensureSpectralProperties(band, i);
            }
            autoGrouping.append(":rtoa");
        }
        final String validPixelExpression = "c2rcc_flags.Valid_PE";
        if (outputRtosaGc) {
            for (int i = 0; i < L8_BAND_COUNT; i++) {
                Band band = addBand(targetProduct, "rtosa_gc_" + (i + 1), "1", "Gas corrected top-of-atmosphere reflectance, input to AC");
                ensureSpectralProperties(band, i);
                band.setValidPixelExpression(validPixelExpression);
            }
            autoGrouping.append(":rtosa_gc");
        }
        if (outputRtosaGcAann) {
            for (int i = 0; i < L8_BAND_COUNT; i++) {
                Band band = addBand(targetProduct, "rtosagc_aann_" + (i + 1), "1", "Gas corrected top-of-atmosphere reflectance, output from AANN");
                ensureSpectralProperties(band, i);
                band.setValidPixelExpression(validPixelExpression);
            }
            autoGrouping.append(":rtosagc_aann");
        }

        if (outputRpath) {
            for (int i = 0; i < L8_BAND_COUNT; i++) {
                Band band = addBand(targetProduct, "rpath_" + (i + 1), "1", "Path-radiance reflectances");
                ensureSpectralProperties(band, i);
                band.setValidPixelExpression(validPixelExpression);
            }
            autoGrouping.append(":rpath");
        }

        if (outputTdown) {
            for (int i = 0; i < L8_BAND_COUNT; i++) {
                Band band = addBand(targetProduct, "tdown_" + (i + 1), "1", "Transmittance of downweling irradiance");
                ensureSpectralProperties(band, i);
                band.setValidPixelExpression(validPixelExpression);
            }
            autoGrouping.append(":tdown");
        }

        if (outputTup) {
            for (int i = 0; i < L8_BAND_COUNT; i++) {
                Band band = addBand(targetProduct, "tup_" + (i + 1), "1", "Transmittance of upweling irradiance");
                ensureSpectralProperties(band, i);
                band.setValidPixelExpression(validPixelExpression);
            }
            autoGrouping.append(":tup");
        }

        if (outputAcReflectance) {
            for (int i = 0; i < L8_BAND_COUNT; i++) {
                final Band band;
                if (outputAsRrs) {
                    band = addBand(targetProduct, "rrs_" + (i + 1), "sr^-1",
                                   "Atmospherically corrected Angular dependent remote sensing reflectances");
                } else {
                    band = addBand(targetProduct, "rhow_" + (i + 1), "1", "Atmospherically corrected Angular dependent water leaving reflectances");
                }
                ensureSpectralProperties(band, i);
                band.setValidPixelExpression(validPixelExpression);
            }
            if (outputAsRrs) {
                autoGrouping.append(":rrs");
            } else {
                autoGrouping.append(":rhow");
            }
        }

        if (outputRhown) {
            for (int i = 0; i < L8_BAND_COUNT; i++) {
                final Band band = addBand(targetProduct, "rhown_" + (i + 1), "1", "Normalized water leaving reflectances");
                ensureSpectralProperties(band, i);
                band.setValidPixelExpression(validPixelExpression);
            }
            autoGrouping.append(":rhown");
        }

        if (outputOos) {
            final Band oos_rtosa = addBand(targetProduct, "oos_rtosa", "1",
                                           "Gas corrected top-of-atmosphere reflectances are out of scope of nn training dataset");
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
        Band iop_atot = addVirtualBand(targetProduct, "iop_atot", "iop_apig + iop_adet + iop_agelb", "m^-1",
                                       "phytoplankton + detritus + gelbstoff absorption at 443 nm");
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
            kd_z90max = addVirtualBand(targetProduct, "kd_z90max", "1 / kdmin", "m",
                                       "Depth of the water column from which 90% of the water leaving irradiance comes from");

            kd489.setValidPixelExpression(validPixelExpression);
            kdmin.setValidPixelExpression(validPixelExpression);
            kd_z90max.setValidPixelExpression(validPixelExpression);

            autoGrouping.append(":kd");
        }

        Band conc_tsm = addVirtualBand(targetProduct, "conc_tsm", "iop_bpart * " + TSMfakBpart + " + iop_bwit * " + TSMfakBwit, "g m^-3",
                                       "Total suspended matter dry weight concentration");
        Band conc_chl = addVirtualBand(targetProduct, "conc_chl", "pow(iop_apig, " + CHLexp + ") * " + CHLfak, "mg m^-3",
                                       "Chlorophylll concentration");

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

            iop_apig.setValidPixelExpression(validPixelExpression);
            iop_adet.setValidPixelExpression(validPixelExpression);
            iop_agelb.setValidPixelExpression(validPixelExpression);
            iop_bpart.setValidPixelExpression(validPixelExpression);
            iop_bwit.setValidPixelExpression(validPixelExpression);
            iop_adg.setValidPixelExpression(validPixelExpression);
            iop_atot.setValidPixelExpression(validPixelExpression);
            iop_btot.setValidPixelExpression(validPixelExpression);

            Band unc_tsm = addVirtualBand(targetProduct, "unc_tsm", "unc_btot * " + TSMfakBpart, "g m^-3",
                                          "uncertainty of total suspended matter (TSM) dry weight concentration");
            Band unc_chl = addVirtualBand(targetProduct, "unc_chl", "pow(unc_apig, " + CHLexp + ") * " + CHLfak, "mg m^-3",
                                          "uncertainty of chlorophylll concentration");

            conc_tsm.addAncillaryVariable(unc_tsm, "uncertainty");
            conc_chl.addAncillaryVariable(unc_chl, "uncertainty");

            conc_tsm.setValidPixelExpression(validPixelExpression);
            conc_chl.setValidPixelExpression(validPixelExpression);

            if (outputKd) {
                Band unc_kd489 = addBand(targetProduct, "unc_kd489", "m^-1", "uncertainty of irradiance attenuation coefficient");
                Band unc_kdmin = addBand(targetProduct, "unc_kdmin", "m^-1", "uncertainty of mean irradiance attenuation coefficient");
                Band unc_kd_z90max = addVirtualBand(targetProduct, "unc_kd_z90max", "abs(kd_z90max - 1.0 / abs(kdmin - unc_kdmin))", "m",
                                                    "uncertainty of depth of the water column from which 90% of the water leaving irradiance comes from");

                kd489.addAncillaryVariable(unc_kd489, "uncertainty");
                kdmin.addAncillaryVariable(unc_kdmin, "uncertainty");
                kd_z90max.addAncillaryVariable(unc_kd_z90max, "uncertainty");

                kd489.setValidPixelExpression(validPixelExpression);
                kdmin.setValidPixelExpression(validPixelExpression);
                kd_z90max.setValidPixelExpression(validPixelExpression);
            }

            autoGrouping.append(":unc");
        }

        Band c2rcc_flags = targetProduct.addBand("c2rcc_flags", ProductData.TYPE_UINT32);
        c2rcc_flags.setDescription("C2RCC quality flags");

        FlagCoding flagCoding = new FlagCoding("c2rcc_flags");
        //0
        flagCoding.addFlag("Rtosa_OOS", 0x01 << FLAG_INDEX_RTOSA_OOS,
                           "The input spectrum to the atmospheric correction neural net was out of the scope of the training range and the inversion is likely to be wrong");
        flagCoding.addFlag("Rtosa_OOR", 0x01 << FLAG_INDEX_RTOSA_OOR,
                           "The input spectrum to the atmospheric correction neural net out of training range");
        flagCoding.addFlag("Rhow_OOR", 0x01 << FLAG_INDEX_RHOW_OOR, "One of the inputs to the IOP retrieval neural net is out of training range");
        flagCoding.addFlag("Cloud_risk", 0x01 << FLAG_INDEX_CLOUD, "High downwelling transmission is indicating cloudy conditions");
        flagCoding.addFlag("Iop_OOR", 0x01 << FLAG_INDEX_IOP_OOR, "One of the IOPs is out of range");
        flagCoding.addFlag("Apig_at_max", 0x01 << FLAG_INDEX_APIG_AT_MAX,
                           "Apig output of the IOP retrieval neural net is at its maximum. This means that the true value is this value or higher.");
        //5
        flagCoding.addFlag("Adet_at_max", 0x01 << FLAG_INDEX_ADET_AT_MAX,
                           "Adet output of the IOP retrieval neural net is at its maximum. This means that the true value is this value or higher.");
        flagCoding.addFlag("Agelb_at_max", 0x01 << FLAG_INDEX_AGELB_AT_MAX,
                           "Agelb output of the IOP retrieval neural net is at its maximum. This means that the true value is this value or higher.");
        flagCoding.addFlag("Bpart_at_max", 0x01 << FLAG_INDEX_BPART_AT_MAX,
                           "Bpart output of the IOP retrieval neural net is at its maximum. This means that the true value is this value or higher.");
        flagCoding.addFlag("Bwit_at_max", 0x01 << FLAG_INDEX_BWIT_AT_MAX,
                           "Bwit output of the IOP retrieval neural net is at its maximum. This means that the true value is this value or higher.");
        flagCoding.addFlag("Apig_at_min", 0x01 << FLAG_INDEX_APIG_AT_MIN,
                           "Apig output of the IOP retrieval neural net is at its minimum. This means that the true value is this value or lower.");
        //10
        flagCoding.addFlag("Adet_at_min", 0x01 << FLAG_INDEX_ADET_AT_MIN,
                           "Adet output of the IOP retrieval neural net is at its minimum. This means that the true value is this value or lower.");
        flagCoding.addFlag("Agelb_at_min", 0x01 << FLAG_INDEX_AGELB_AT_MIN,
                           "Agelb output of the IOP retrieval neural net is at its minimum. This means that the true value is this value or lower.");
        flagCoding.addFlag("Bpart_at_min", 0x01 << FLAG_INDEX_BPART_AT_MIN,
                           "Bpart output of the IOP retrieval neural net is at its minimum. This means that the true value is this value or lower.");
        flagCoding.addFlag("Bwit_at_min", 0x01 << FLAG_INDEX_BWIT_AT_MIN,
                           "Bwit output of the IOP retrieval neural net is at its minimum. This means that the true value is this value or lower.");
        flagCoding.addFlag("Rhow_OOS", 0x01 << FLAG_INDEX_RHOW_OOS,
                           "The Rhow input spectrum to IOP neural net is probably not within the training range of the neural net, and the inversion is likely to be wrong.");
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

        if (debug_outputAngles) {
            targetProduct.addBand("debug_view_azi", ProductData.TYPE_FLOAT32);
            targetProduct.addBand("debug_view_zen", ProductData.TYPE_FLOAT32);
            targetProduct.addBand("debug_sun_azi", ProductData.TYPE_FLOAT32);
            targetProduct.addBand("debug_sun_zen", ProductData.TYPE_FLOAT32);
        }

        targetProduct.addProductNodeListener(getNnNamesMetadataAppender());
    }

    @Override
    protected void configureTargetSamples(TargetSampleConfigurer tsc) throws OperatorException {
        if (outputRtoa) {
            for (int i = 0; i < L8_BAND_COUNT; i++) {
                tsc.defineSample(RTOA_IX + i, "rtoa_" + (i + 1));
            }
        }

        if (outputRtosaGc) {
            for (int i = 0; i < L8_BAND_COUNT; i++) {
                tsc.defineSample(RTOSA_IX + i, "rtosa_gc_" + (i + 1));
            }
        }

        if (outputRtosaGcAann) {
            for (int i = 0; i < L8_BAND_COUNT; i++) {
                tsc.defineSample(RTOSA_AANN_IX + i, "rtosagc_aann_" + (i + 1));
            }
        }

        if (outputRpath) {
            for (int i = 0; i < L8_BAND_COUNT; i++) {
                tsc.defineSample(RPATH_IX + i, "rpath_" + (i + 1));
            }
        }

        if (outputTdown) {
            for (int i = 0; i < L8_BAND_COUNT; i++) {
                tsc.defineSample(TDOWN_IX + i, "tdown_" + (i + 1));
            }
        }

        if (outputTup) {
            for (int i = 0; i < L8_BAND_COUNT; i++) {
                tsc.defineSample(TUP_IX + i, "tup_" + (i + 1));
            }
        }

        if (outputAcReflectance) {
            for (int i = 0; i < L8_BAND_COUNT; i++) {
                if (outputAsRrs) {
                    tsc.defineSample(AC_REFLEC_IX + i, "rrs_" + (i + 1));
                } else {
                    tsc.defineSample(AC_REFLEC_IX + i, "rhow_" + (i + 1));
                }
            }
        }

        if (outputRhown) {
            for (int i = 0; i < L8_BAND_COUNT; i++) {
                tsc.defineSample(RHOWN_IX + i, "rhown_" + (i + 1));
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

        if (debug_outputAngles) {
            tsc.defineSample(DEBUG_VIEW_AZI_IX, "debug_view_azi");
            tsc.defineSample(DEBUG_VIEW_ZEN_IX, "debug_view_zen");
            tsc.defineSample(DEBUG_SUN_AZI_IX, "debug_sun_azi");
            tsc.defineSample(DEBUG_SUN_ZEN_IX, "debug_sun_zen");
        }

    }

    private void ensureSpectralProperties(Band band, int i) {
        ProductUtils.copySpectralBandProperties(resampledProduct.getBand(EXPECTED_BANDNAMES[i]), band);
        if (band.getSpectralWavelength() == 0) {
            band.setSpectralWavelength(DEFAULT_WAVELENGTH[i]);
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

    @Override
    public void dispose() {
        super.dispose();
        if (atmosphericAuxdata != null) {
            atmosphericAuxdata.dispose();
            atmosphericAuxdata = null;
        }
    }

    @Override
    protected void prepareInputs() throws OperatorException {
        for (String EXPECTED_BANDNAME : EXPECTED_BANDNAMES) {
            assertSourceBand(EXPECTED_BANDNAME);
        }
        if (useAngleBands) {
            for (String bandname : ANGEL_BANDNAMES) {
                assertSourceBand(bandname);
            }
        }

        MetadataElement metadataRoot = sourceProduct.getMetadataRoot();
        MetadataElement l1MetadataFile = metadataRoot.getElement("L1_METADATA_FILE");
        MetadataElement imageAttributes = l1MetadataFile.getElement("IMAGE_ATTRIBUTES");
        sunAzimuth = imageAttributes.getAttribute("SUN_AZIMUTH").getData().getElemDouble();
        double sunElevation = imageAttributes.getAttribute("SUN_ELEVATION").getData().getElemDouble();
        sunZenith = 90 - sunElevation;

        SubsetInfo subsetInfo = getSubsetInfo(metadataRoot);
        geometryAnglesBuilder = new GeometryAnglesBuilder(subsetInfo.subsampling_x, subsetInfo.offset_x, subsetInfo.center_x,
                                                          sunAzimuth, sunZenith);
        double sunAngleCorrectionFactor = Math.sin(Math.toRadians(sunElevation));

        MetadataElement radiometricRescaling = l1MetadataFile.getElement("RADIOMETRIC_RESCALING");
        reflectance_offset = new double[L8_BAND_COUNT];
        reflectance_scale = new double[L8_BAND_COUNT];
        for (int i = 0; i < L8_BAND_COUNT; i++) {
            // this follows:
            // http://landsat.usgs.gov/Landsat8_Using_Product.php, section 'Conversion to TOA Reflectance'
            // also see org.esa.s3tbx.dataio.landsat.geotiff.Landsat8Metadata#getSunAngleCorrectionFactor
            double scalingOffset = radiometricRescaling.getAttributeDouble(String.format("REFLECTANCE_ADD_BAND_%d", i + 1));
            reflectance_offset[i] = scalingOffset / sunAngleCorrectionFactor;
            double scalingFactor = radiometricRescaling.getAttributeDouble(String.format("REFLECTANCE_MULT_BAND_%d", i + 1));
            reflectance_scale[i] = scalingFactor / sunAngleCorrectionFactor;
        }

        if (sourceProduct.getSceneGeoCoding() == null) {
            throw new OperatorException("The source product must be geo-coded.");
        }

        ElevationModelDescriptor getasse30 = ElevationModelRegistry.getInstance().getDescriptor("GETASSE30");
        if (getasse30 != null) {
            // if elevation model cannot be initialised the fallback height will be used
            elevationModel = getasse30.createDem(Resampling.BILINEAR_INTERPOLATION);
        }

        try {
            if (StringUtils.isNotNullAndNotEmpty(alternativeNNPath)) {
                String[] nnFilePaths = NNUtils.getNNFilePaths(Paths.get(alternativeNNPath), NNUtils.ALTERNATIVE_NET_DIR_NAMES);
                algorithm = new C2rccLandsat8Algorithm(nnFilePaths, false);
            } else {
                String[] nnFilePaths = c2rccNetSetMap.get(netSet);
                if (nnFilePaths == null) {
                    throw new OperatorException(String.format("Unknown set '%s' of neural nets specified.", netSet));
                }
                algorithm = new C2rccLandsat8Algorithm(nnFilePaths, true);
            }
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
        algorithm.setDeriveRwFromPathAndTransmittance(deriveRwFromPathAndTransmittance);

        timeCoding = C2rccCommons.getTimeCoding(sourceProduct);
        initAtmosphericAuxdata();

        if (sourceProduct.isMultiSize()) {
            HashMap<String, Object> parameters = new HashMap<>();
            parameters.put("referenceBand", EXPECTED_BANDNAMES[0]);
            resampledProduct = GPF.createProduct("Resample", parameters, sourceProduct);
        } else {
            resampledProduct = sourceProduct;
        }
    }

    private SubsetInfo getSubsetInfo(MetadataElement metadataRoot) {
        SubsetInfo subsetInfo = new SubsetInfo(metadataRoot.getProduct().getSceneRasterWidth() / 2);

        MetadataElement history = metadataRoot.getElement("history");
        if (history != null) {
            MetadataElement subsetElement = history.getElement("SubsetInfo");
            if (subsetElement != null) {
                subsetInfo.subsampling_x = subsetElement.getAttributeInt("SubSampling.x", 1);
                // subsetInfo.subsampling_y = subsetElement.getAttributeInt("SubSampling.y", 1);
                subsetInfo.offset_x = subsetElement.getAttributeInt("SubRegion.x", 0);
                // y_off = subsetElement.getAttributeInt("SubRegion.y", 0);
            }
        }
        return subsetInfo;
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

    private void assertSourceBand(String name) {
        if (sourceProduct.getBand(name) == null) {
            throw new OperatorException("Invalid source product, band '" + name + "' required");
        }
    }

    private static double toReflectances(double source, double radiance_offset, double radiance_scale,
                                         double reflectance_offset, double reflectance_scale) {
        double count = (source - radiance_offset) / radiance_scale;
        return count * reflectance_scale + reflectance_offset;
    }

    public static class Spi extends OperatorSpi {

        static {
            RgbProfiles.installLandsat8RgbProfiles();
        }


        public Spi() {
            super(C2rccLandsat8Operator.class);
        }
    }
}
