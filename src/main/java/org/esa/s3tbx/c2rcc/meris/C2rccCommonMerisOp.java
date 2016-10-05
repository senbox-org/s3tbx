package org.esa.s3tbx.c2rcc.meris;

import org.esa.s3tbx.c2rcc.C2rccCommons;
import org.esa.s3tbx.c2rcc.C2rccConfigurable;
import org.esa.s3tbx.c2rcc.ancillary.AtmosphericAuxdata;
import org.esa.s3tbx.c2rcc.ancillary.AtmosphericAuxdataBuilder;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.datamodel.ProductNodeEvent;
import org.esa.snap.core.datamodel.ProductNodeListener;
import org.esa.snap.core.datamodel.ProductNodeListenerAdapter;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.TimeCoding;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.pointop.PixelOperator;
import org.esa.snap.core.gpf.pointop.ProductConfigurer;
import org.esa.snap.core.gpf.pointop.TargetSampleConfigurer;
import org.esa.snap.core.gpf.pointop.WritableSample;
import org.esa.snap.core.util.ProductUtils;

import java.awt.Color;

import static org.esa.s3tbx.c2rcc.meris.C2rccMerisAlgorithm.FLAG_INDEX_ADET_AT_MAX;
import static org.esa.s3tbx.c2rcc.meris.C2rccMerisAlgorithm.FLAG_INDEX_ADET_AT_MIN;
import static org.esa.s3tbx.c2rcc.meris.C2rccMerisAlgorithm.FLAG_INDEX_AGELB_AT_MAX;
import static org.esa.s3tbx.c2rcc.meris.C2rccMerisAlgorithm.FLAG_INDEX_AGELB_AT_MIN;
import static org.esa.s3tbx.c2rcc.meris.C2rccMerisAlgorithm.FLAG_INDEX_APIG_AT_MAX;
import static org.esa.s3tbx.c2rcc.meris.C2rccMerisAlgorithm.FLAG_INDEX_APIG_AT_MIN;
import static org.esa.s3tbx.c2rcc.meris.C2rccMerisAlgorithm.FLAG_INDEX_BPART_AT_MAX;
import static org.esa.s3tbx.c2rcc.meris.C2rccMerisAlgorithm.FLAG_INDEX_BPART_AT_MIN;
import static org.esa.s3tbx.c2rcc.meris.C2rccMerisAlgorithm.FLAG_INDEX_BWIT_AT_MAX;
import static org.esa.s3tbx.c2rcc.meris.C2rccMerisAlgorithm.FLAG_INDEX_BWIT_AT_MIN;
import static org.esa.s3tbx.c2rcc.meris.C2rccMerisAlgorithm.FLAG_INDEX_CLOUD;
import static org.esa.s3tbx.c2rcc.meris.C2rccMerisAlgorithm.FLAG_INDEX_IOP_OOR;
import static org.esa.s3tbx.c2rcc.meris.C2rccMerisAlgorithm.FLAG_INDEX_KD489_AT_MAX;
import static org.esa.s3tbx.c2rcc.meris.C2rccMerisAlgorithm.FLAG_INDEX_KD489_OOR;
import static org.esa.s3tbx.c2rcc.meris.C2rccMerisAlgorithm.FLAG_INDEX_KDMIN_AT_MAX;
import static org.esa.s3tbx.c2rcc.meris.C2rccMerisAlgorithm.FLAG_INDEX_KDMIN_OOR;
import static org.esa.s3tbx.c2rcc.meris.C2rccMerisAlgorithm.FLAG_INDEX_RHOW_OOR;
import static org.esa.s3tbx.c2rcc.meris.C2rccMerisAlgorithm.FLAG_INDEX_RHOW_OOS;
import static org.esa.s3tbx.c2rcc.meris.C2rccMerisAlgorithm.FLAG_INDEX_RTOSA_OOR;
import static org.esa.s3tbx.c2rcc.meris.C2rccMerisAlgorithm.FLAG_INDEX_RTOSA_OOS;
import static org.esa.s3tbx.c2rcc.meris.C2rccMerisAlgorithm.FLAG_INDEX_VALID_PE;
import static org.esa.s3tbx.c2rcc.meris.C2rccMerisAlgorithm.IDX_iop_rw;
import static org.esa.s3tbx.c2rcc.meris.C2rccMerisAlgorithm.IDX_iop_unciop;
import static org.esa.s3tbx.c2rcc.meris.C2rccMerisAlgorithm.IDX_iop_uncsumiop_unckd;
import static org.esa.s3tbx.c2rcc.meris.C2rccMerisAlgorithm.IDX_rtosa_aann;
import static org.esa.s3tbx.c2rcc.meris.C2rccMerisAlgorithm.IDX_rtosa_rpath;
import static org.esa.s3tbx.c2rcc.meris.C2rccMerisAlgorithm.IDX_rtosa_rw;
import static org.esa.s3tbx.c2rcc.meris.C2rccMerisAlgorithm.IDX_rtosa_trans;
import static org.esa.s3tbx.c2rcc.meris.C2rccMerisAlgorithm.IDX_rw_iop;
import static org.esa.s3tbx.c2rcc.meris.C2rccMerisAlgorithm.IDX_rw_kd;
import static org.esa.s3tbx.c2rcc.meris.C2rccMerisAlgorithm.IDX_rw_rwnorm;
import static org.esa.s3tbx.c2rcc.meris.C2rccMerisAlgorithm.merband12_ix;
import static org.esa.s3tbx.c2rcc.meris.C2rccMerisAlgorithm.merband15_ix;

/**
 * @author Marco Peters
 */
abstract class C2rccCommonMerisOp extends PixelOperator implements C2rccConfigurable {
    // MERIS4 sources
    static final int BAND_COUNT = 15;
    static final String RASTER_NAME_TOTAL_OZONE = "total_ozone";
    static final String RASTER_NAME_SEA_LEVEL_PRESSURE = "sea_level_pressure";
    // MERIS4 targets
    private static final int BC_12 = merband12_ix.length; // Band count 12
    private static final int BC_15 = merband15_ix.length; // Band count 15
    private static final int SINGLE_IX = BC_15 + 7 * BC_12;

    static final int RTOA_IX = 0;
    static final int RTOSA_IX = BC_15;
    static final int RTOSA_AANN_IX = BC_15 + BC_12;
    static final int RPATH_IX = BC_15 + 2 * BC_12;
    static final int TDOWN_IX = BC_15 + 3 * BC_12;
    static final int TUP_IX = BC_15 + 4 * BC_12;
    static final int AC_REFLEC_IX = BC_15 + 5 * BC_12;
    static final int RHOWN_IX = BC_15 + 6 * BC_12;

    static final int OOS_RTOSA_IX = SINGLE_IX;
    static final int OOS_AC_REFLEC_IX = SINGLE_IX + 1;

    static final int IOP_APIG_IX = SINGLE_IX + 2;
    static final int IOP_ADET_IX = SINGLE_IX + 3;
    static final int IOP_AGELB_IX = SINGLE_IX + 4;
    static final int IOP_BPART_IX = SINGLE_IX + 5;
    static final int IOP_BWIT_IX = SINGLE_IX + 6;

    static final int KD489_IX = SINGLE_IX + 7;
    static final int KDMIN_IX = SINGLE_IX + 8;

    static final int UNC_APIG_IX = SINGLE_IX + 9;
    static final int UNC_ADET_IX = SINGLE_IX + 10;
    static final int UNC_AGELB_IX = SINGLE_IX + 11;
    static final int UNC_BPART_IX = SINGLE_IX + 12;
    static final int UNC_BWIT_IX = SINGLE_IX + 13;
    static final int UNC_ADG_IX = SINGLE_IX + 14;
    static final int UNC_ATOT_IX = SINGLE_IX + 15;
    static final int UNC_BTOT_IX = SINGLE_IX + 16;
    static final int UNC_KD489_IX = SINGLE_IX + 17;
    static final int UNC_KDMIN_IX = SINGLE_IX + 18;

    static final int C2RCC_FLAGS_IX = SINGLE_IX + 19;

    static final String[] alternativeNetDirNames = new String[]{
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

    static final String[] c2rccNNResourcePaths = new String[10];

    static {
        c2rccNNResourcePaths[IDX_rtosa_aann] = "meris/richard_atmo_invers29_press_20150125/rtoa_aaNN7/31x7x31_555.6.net";
        c2rccNNResourcePaths[IDX_rtosa_rw] = "meris/richard_atmo_invers29_press_20150125/rtoa_rw_nn3/33x73x53x33_470639.6.net";
        c2rccNNResourcePaths[IDX_rw_iop] = "meris/coastcolour_wat_20140318/inv_meris_logrw_logiop_20140318_noise_p5_fl/97x77x37_11671.0.net";
        c2rccNNResourcePaths[IDX_iop_rw] = "meris/coastcolour_wat_20140318/for_meris_logrw_logiop_20140318_p5_fl/17x97x47_335.3.net";
        c2rccNNResourcePaths[IDX_rw_kd] = "meris/coastcolour_wat_20140318/inv_meris_kd/97x77x7_232.4.net";
        c2rccNNResourcePaths[IDX_iop_unciop] = "meris/coastcolour_wat_20140318/uncertain_log_abs_biasc_iop/17x77x37_11486.7.net";
        c2rccNNResourcePaths[IDX_iop_uncsumiop_unckd] = "meris/coastcolour_wat_20140318/uncertain_log_abs_tot_kd/17x77x37_9113.1.net";
        c2rccNNResourcePaths[IDX_rw_rwnorm] = "meris/coastcolour_wat_20140318/norma_net_20150307/37x57x17_76.8.net";
        c2rccNNResourcePaths[IDX_rtosa_trans] = "meris/richard_atmo_invers29_press_20150125/rtoa_trans_nn2/31x77x57x37_37087.4.net";
        c2rccNNResourcePaths[IDX_rtosa_rpath] = "meris/richard_atmo_invers29_press_20150125/rtoa_rpath_nn2/31x77x57x37_2388.6.net";
    }

    static final String[] c2xNNResourcePaths = new String[10];
    static {
        c2xNNResourcePaths[IDX_rtosa_aann] = "meris/c2x/nn4snap_meris_hitsm_20151128/rtosa_aann/31x7x31_1244.3.net";
        c2xNNResourcePaths[IDX_rtosa_rw] = "meris/c2x/nn4snap_meris_hitsm_20151128/rtosa_rw/17x27x27x17_677356.6.net";
        c2xNNResourcePaths[IDX_rw_iop] = "meris/c2x/nn4snap_meris_hitsm_20151128/rw_iop/27x97x77x37_14746.2.net";
        c2xNNResourcePaths[IDX_iop_rw] = "meris/c2x/nn4snap_meris_hitsm_20151128/iop_rw/17x37x97x47_500.0.net";
        c2xNNResourcePaths[IDX_rw_kd] = "meris/c2x/nn4snap_meris_hitsm_20151128/rw_kd/97x77x7_232.4.net";
        c2xNNResourcePaths[IDX_iop_unciop] = "meris/c2x/nn4snap_meris_hitsm_20151128/iop_unciop/17x77x37_11486.7.net";
        c2xNNResourcePaths[IDX_iop_uncsumiop_unckd] = "meris/c2x/nn4snap_meris_hitsm_20151128/iop_uncsumiop_unckd/17x77x37_9113.1.net";
        c2xNNResourcePaths[IDX_rw_rwnorm] = "meris/c2x/nn4snap_meris_hitsm_20151128/rw_rwnorm/37x57x17_76.8.net";
        c2xNNResourcePaths[IDX_rtosa_trans] = "meris/c2x/nn4snap_meris_hitsm_20151128/rtosa_trans/31x77x57x37_45461.2.net";
        c2xNNResourcePaths[IDX_rtosa_rpath] = "meris/c2x/nn4snap_meris_hitsm_20151128/rtosa_rpath/31x77x57x37_4701.4.net";
    }

    protected C2rccMerisAlgorithm algorithm;
    AtmosphericAuxdata atmosphericAuxdata;
    TimeCoding timeCoding;

    protected abstract void setUseEcmwfAuxData(boolean useEcmwfAuxData);

    protected abstract boolean getUseEcmwfAuxData();

    protected abstract String getRadianceBandName(int index);

    protected abstract void setOutputKd(boolean outputKd);

    protected abstract boolean getOutputKd();

    protected abstract void setOutputOos(boolean outputOos);

    protected abstract boolean getOutputOos();

    protected abstract void setOutputRpath(boolean outputRpath);

    protected abstract boolean getOutputRpath();

    protected abstract void setOutputRtoa(boolean outputRtoa);

    protected abstract boolean getOutputRtoa();

    protected abstract boolean getOutputRtosa();

    protected abstract void setOutputRtosaGcAann(boolean outputRtosaGcAann);

    protected abstract boolean getOutputRtosaGcAann();

    protected abstract void setOutputAcReflec(boolean outputAcReflec);

    protected abstract boolean getOutputAcReflec();

    protected abstract void setOutputRhown(boolean outputRhown);

    protected abstract boolean getOutputRhown();

    protected abstract void setOutputTdown(boolean outputTdown);

    protected abstract boolean getOutputTdown();

    protected abstract void setOutputTup(boolean outputTup);

    protected abstract boolean getOutputTup();

    protected abstract void setOutputUncertainties(boolean outputUncertainties);

    protected abstract boolean getOutputUncertainties();

    protected abstract boolean getOutputAsRrs();


    protected void fillTargetSamples(WritableSample[] targetSamples, C2rccMerisAlgorithm.Result result) {
        if (getOutputRtoa()) {
            for (int i = 0; i < result.r_toa.length; i++) {
                targetSamples[RTOA_IX + i].set(result.r_toa[i]);
            }
        }

        if (getOutputRtosa()) {
            for (int i = 0; i < result.r_tosa.length; i++) {
                targetSamples[RTOSA_IX + i].set(result.r_tosa[i]);
            }
        }

        if (getOutputRtosaGcAann()) {
            for (int i = 0; i < result.rtosa_aann.length; i++) {
                targetSamples[RTOSA_AANN_IX + i].set(result.rtosa_aann[i]);
            }
        }

        if (getOutputRpath()) {
            for (int i = 0; i < result.rpath_nn.length; i++) {
                targetSamples[RPATH_IX + i].set(result.rpath_nn[i]);
            }
        }

        if (getOutputTdown()) {
            for (int i = 0; i < result.transd_nn.length; i++) {
                targetSamples[TDOWN_IX + i].set(result.transd_nn[i]);
            }
        }

        if (getOutputTup()) {
            for (int i = 0; i < result.transu_nn.length; i++) {
                targetSamples[TUP_IX + i].set(result.transu_nn[i]);
            }
        }

        if (getOutputAcReflec()) {
            for (int i = 0; i < result.rwa.length; i++) {
                targetSamples[AC_REFLEC_IX + i].set(getOutputAsRrs() ? result.rwa[i] / Math.PI : result.rwa[i]);
            }
        }

        if (getOutputRhown()) {
            for (int i = 0; i < result.rwn.length; i++) {
                targetSamples[RHOWN_IX + i].set(result.rwn[i]);
            }
        }

        if (getOutputOos()) {
            targetSamples[OOS_RTOSA_IX].set(result.rtosa_oos);
            targetSamples[OOS_AC_REFLEC_IX].set(result.rwa_oos);
        }

        for (int i = 0; i < result.iops_nn.length; i++) {
            targetSamples[IOP_APIG_IX + i].set(result.iops_nn[i]);
        }

        if (getOutputKd()) {
            targetSamples[KD489_IX].set(result.kd489_nn);
            targetSamples[KDMIN_IX].set(result.kdmin_nn);
        }

        if (getOutputUncertainties()) {
            for (int i = 0; i < result.unc_iop_abs.length; i++) {
                targetSamples[UNC_APIG_IX + i].set(result.unc_iop_abs[i]);
            }
            targetSamples[UNC_ADG_IX].set(result.unc_abs_adg);
            targetSamples[UNC_ATOT_IX].set(result.unc_abs_atot);
            targetSamples[UNC_BTOT_IX].set(result.unc_abs_btot);
            if (getOutputKd()) {
                targetSamples[UNC_KD489_IX].set(result.unc_abs_kd489);
                targetSamples[UNC_KDMIN_IX].set(result.unc_abs_kdmin);
            }
        }

        targetSamples[C2RCC_FLAGS_IX].set(result.flags);
    }

    @Override
    protected void configureTargetSamples(TargetSampleConfigurer tsc) throws OperatorException {

        if (getOutputRtoa()) {
            for (int i = 0; i < merband15_ix.length; i++) {
                tsc.defineSample(RTOA_IX + i, "rtoa_" + merband15_ix[i]);
            }
        }

        if (getOutputRtosa()) {
            for (int i = 0; i < merband12_ix.length; i++) {
                tsc.defineSample(RTOSA_IX + i, "rtosa_gc_" + merband12_ix[i]);
            }
        }

        if (getOutputRtosaGcAann()) {
            for (int i = 0; i < merband12_ix.length; i++) {
                tsc.defineSample(RTOSA_AANN_IX + i, "rtosagc_aann_" + merband12_ix[i]);
            }
        }

        if (getOutputRpath()) {
            for (int i = 0; i < merband12_ix.length; i++) {
                tsc.defineSample(RPATH_IX + i, "rpath_" + merband12_ix[i]);
            }
        }

        if (getOutputTdown()) {
            for (int i = 0; i < merband12_ix.length; i++) {
                tsc.defineSample(TDOWN_IX + i, "tdown_" + merband12_ix[i]);
            }
        }

        if (getOutputTup()) {
            for (int i = 0; i < merband12_ix.length; i++) {
                tsc.defineSample(TUP_IX + i, "tup_" + merband12_ix[i]);
            }
        }

        if (getOutputAcReflec()) {
            for (int i = 0; i < merband12_ix.length; i++) {
                if (getOutputAsRrs()) {
                    tsc.defineSample(AC_REFLEC_IX + i, "rrs_" + merband12_ix[i]);
                } else {
                    tsc.defineSample(AC_REFLEC_IX + i, "rhow_" + merband12_ix[i]);
                }
            }
        }

        if (getOutputRhown()) {
            for (int i = 0; i < merband12_ix.length; i++) {
                tsc.defineSample(RHOWN_IX + i, "rhown_" + merband12_ix[i]);
            }
        }

        if (getOutputOos()) {
            tsc.defineSample(OOS_RTOSA_IX, "oos_rtosa");
            if (getOutputAsRrs()) {
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

        if (getOutputKd()) {
            tsc.defineSample(KD489_IX, "kd489");
            tsc.defineSample(KDMIN_IX, "kdmin");
        }

        if (getOutputUncertainties()) {
            tsc.defineSample(UNC_APIG_IX, "unc_apig");
            tsc.defineSample(UNC_ADET_IX, "unc_adet");
            tsc.defineSample(UNC_AGELB_IX, "unc_agelb");
            tsc.defineSample(UNC_BPART_IX, "unc_bpart");
            tsc.defineSample(UNC_BWIT_IX, "unc_bwit");

            tsc.defineSample(UNC_ADG_IX, "unc_adg");
            tsc.defineSample(UNC_ATOT_IX, "unc_atot");
            tsc.defineSample(UNC_BTOT_IX, "unc_btot");
            if (getOutputKd()) {
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
        C2rccCommons.ensureTimeInformation(targetProduct, getSourceProduct().getStartTime(), getSourceProduct().getEndTime(), timeCoding);
        ProductUtils.copyFlagBands(getSourceProduct(), targetProduct, true);

        final StringBuilder autoGrouping = new StringBuilder("iop");
        autoGrouping.append(":conc");

        if (getOutputRtoa()) {
            for (int i : merband15_ix) {
                final Band band = C2rccCommons.addBand(targetProduct, "rtoa_" + i, "1", "Top-of-atmosphere reflectance");
                ensureSpectralProperties(band, i);
            }
            autoGrouping.append(":rtoa");
        }
        final String validPixelExpression = "c2rcc_flags.Valid_PE";
        if (getOutputRtosa()) {
            for (int bi : merband12_ix) {
                Band band = C2rccCommons.addBand(targetProduct, "rtosa_gc_" + bi, "1", "Gas corrected top-of-atmosphere reflectance, input to AC");
                ensureSpectralProperties(band, bi);
                band.setValidPixelExpression(validPixelExpression);
            }
            autoGrouping.append(":rtosa_gc");
        }
        if (getOutputRtosaGcAann()) {
            for (int bi : merband12_ix) {
                Band band = C2rccCommons.addBand(targetProduct, "rtosagc_aann_" + bi, "1", "Gas corrected top-of-atmosphere reflectance, output from AANN");
                ensureSpectralProperties(band, bi);
                band.setValidPixelExpression(validPixelExpression);
            }
            autoGrouping.append(":rtosagc_aann");
        }

        if (getOutputRpath()) {
            for (int bi : merband12_ix) {
                Band band = C2rccCommons.addBand(targetProduct, "rpath_" + bi, "1", "Path-radiance reflectances");
                ensureSpectralProperties(band, bi);
                band.setValidPixelExpression(validPixelExpression);
            }
            autoGrouping.append(":rpath");
        }

        if (getOutputTdown()) {
            for (int bi : merband12_ix) {
                Band band = C2rccCommons.addBand(targetProduct, "tdown_" + bi, "1", "Transmittance of downweling irradiance");
                ensureSpectralProperties(band, bi);
                band.setValidPixelExpression(validPixelExpression);
            }
            autoGrouping.append(":tdown");
        }

        if (getOutputTup()) {
            for (int bi : merband12_ix) {
                Band band = C2rccCommons.addBand(targetProduct, "tup_" + bi, "1", "Transmittance of upweling irradiance");
                ensureSpectralProperties(band, bi);
                band.setValidPixelExpression(validPixelExpression);
            }
            autoGrouping.append(":tup");
        }

        if (getOutputAcReflec()) {
            for (int index : merband12_ix) {
                final Band band;
                if (getOutputAsRrs()) {
                    band = C2rccCommons.addBand(targetProduct, "rrs_" + index, "sr^-1", "Atmospherically corrected angular dependent remote sensing reflectances");
                } else {
                    band = C2rccCommons.addBand(targetProduct, "rhow_" + index, "1", "Atmospherically corrected angular dependent water leaving reflectances");
                }
                ensureSpectralProperties(band, index);
                band.setValidPixelExpression(validPixelExpression);
            }
            if (getOutputAsRrs()) {
                autoGrouping.append(":rrs");
            } else {
                autoGrouping.append(":rhow");
            }
        }

        if (getOutputRhown()) {
            for (int index : merband12_ix) {
                final Band band = C2rccCommons.addBand(targetProduct, "rhown_" + index, "1", "Normalized water leaving reflectances");
                ensureSpectralProperties(band, index);
                band.setValidPixelExpression(validPixelExpression);
            }
            autoGrouping.append(":rhown");
        }

        if (getOutputOos()) {
            final Band oos_rtosa = C2rccCommons.addBand(targetProduct, "oos_rtosa", "1", "Gas corrected top-of-atmosphere reflectances are out of scope of nn training dataset");
            oos_rtosa.setValidPixelExpression(validPixelExpression);
            if (getOutputAsRrs()) {
                final Band oos_rrs = C2rccCommons.addBand(targetProduct, "oos_rrs", "1", "Remote sensing reflectance are out of scope of nn training dataset");
                oos_rrs.setValidPixelExpression(validPixelExpression);
            } else {
                final Band oos_rhow = C2rccCommons.addBand(targetProduct, "oos_rhow", "1", "Water leaving reflectances are out of scope of nn training dataset");
                oos_rhow.setValidPixelExpression(validPixelExpression);
            }

            autoGrouping.append(":oos");
        }

        Band iop_apig = C2rccCommons.addBand(targetProduct, "iop_apig", "m^-1", "Absorption coefficient of phytoplankton pigments at 443 nm");
        Band iop_adet = C2rccCommons.addBand(targetProduct, "iop_adet", "m^-1", "Absorption coefficient of detritus at 443 nm");
        Band iop_agelb = C2rccCommons.addBand(targetProduct, "iop_agelb", "m^-1", "Absorption coefficient of gelbstoff at 443 nm");
        Band iop_bpart = C2rccCommons.addBand(targetProduct, "iop_bpart", "m^-1", "Scattering coefficient of marine paticles at 443 nm");
        Band iop_bwit = C2rccCommons.addBand(targetProduct, "iop_bwit", "m^-1", "Scattering coefficient of white particles at 443 nm");
        Band iop_adg = C2rccCommons.addVirtualBand(targetProduct, "iop_adg", "iop_adet + iop_agelb", "m^-1", "Detritus + gelbstoff absorption at 443 nm");
        Band iop_atot = C2rccCommons.addVirtualBand(targetProduct, "iop_atot", "iop_apig + iop_adet + iop_agelb", "m^-1", "phytoplankton + detritus + gelbstoff absorption at 443 nm");
        Band iop_btot = C2rccCommons.addVirtualBand(targetProduct, "iop_btot", "iop_bpart + iop_bwit", "m^-1", "total particle scattering at 443 nm");

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
        if (getOutputKd()) {
            kd489 = C2rccCommons.addBand(targetProduct, "kd489", "m^-1", "Irradiance attenuation coefficient at 489 nm");
            kdmin = C2rccCommons.addBand(targetProduct, "kdmin", "m^-1", "Mean irradiance attenuation coefficient at the three bands with minimum kd");
            kd_z90max = C2rccCommons.addVirtualBand(targetProduct, "kd_z90max", "1 / kdmin", "m", "Depth of the water column from which 90% of the water leaving irradiance comes from");

            kd489.setValidPixelExpression(validPixelExpression);
            kdmin.setValidPixelExpression(validPixelExpression);
            kd_z90max.setValidPixelExpression(validPixelExpression);

            autoGrouping.append(":kd");
        }

        Band conc_tsm = C2rccCommons.addVirtualBand(targetProduct, "conc_tsm", "iop_bpart * " + getTSMfakBpart() + " + iop_bwit * " + getTSMfakBwit(), "g m^-3", "Total suspended matter dry weight concentration");
        Band conc_chl = C2rccCommons.addVirtualBand(targetProduct, "conc_chl", "pow(iop_apig, " + getCHLexp() + ") * " + getCHLfak(), "mg m^-3", "Chlorophyll concentration");

        conc_tsm.setValidPixelExpression(validPixelExpression);
        conc_chl.setValidPixelExpression(validPixelExpression);

        if (getOutputUncertainties()) {
            Band unc_apig = C2rccCommons.addBand(targetProduct, "unc_apig", "m^-1", "uncertainty of pigment absorption coefficient");
            Band unc_adet = C2rccCommons.addBand(targetProduct, "unc_adet", "m^-1", "uncertainty of detritus absorption coefficient");
            Band unc_agelb = C2rccCommons.addBand(targetProduct, "unc_agelb", "m^-1", "uncertainty of dissolved gelbstoff absorption coefficient");
            Band unc_bpart = C2rccCommons.addBand(targetProduct, "unc_bpart", "m^-1", "uncertainty of particle scattering coefficient");
            Band unc_bwit = C2rccCommons.addBand(targetProduct, "unc_bwit", "m^-1", "uncertainty of white particle scattering coefficient");
            Band unc_adg = C2rccCommons.addBand(targetProduct, "unc_adg", "m^-1", "uncertainty of total gelbstoff absorption coefficient");
            Band unc_atot = C2rccCommons.addBand(targetProduct, "unc_atot", "m^-1", "uncertainty of total water constituent absorption coefficient");
            Band unc_btot = C2rccCommons.addBand(targetProduct, "unc_btot", "m^-1", "uncertainty of total water constituent scattering coefficient");

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

            Band unc_tsm = C2rccCommons.addVirtualBand(targetProduct, "unc_tsm", "unc_btot * " + getTSMfakBpart(), "g m^-3", "uncertainty of total suspended matter (TSM) dry weight concentration");
            Band unc_chl = C2rccCommons.addVirtualBand(targetProduct, "unc_chl", "pow(unc_apig, " + getCHLexp() + ") * " + getCHLfak(), "mg m^-3", "uncertainty of chlorophyll concentration");

            conc_tsm.addAncillaryVariable(unc_tsm, "uncertainty");
            conc_chl.addAncillaryVariable(unc_chl, "uncertainty");

            unc_tsm.setValidPixelExpression(validPixelExpression);
            unc_chl.setValidPixelExpression(validPixelExpression);

            if (getOutputKd()) {
                Band unc_kd489 = C2rccCommons.addBand(targetProduct, "unc_kd489", "m^-1", "uncertainty of irradiance attenuation coefficient");
                Band unc_kdmin = C2rccCommons.addBand(targetProduct, "unc_kdmin", "m^-1", "uncertainty of mean irradiance attenuation coefficient");
                Band unc_kd_z90max = C2rccCommons.addVirtualBand(targetProduct, "unc_kd_z90max", "abs(kd_z90max - 1.0 / abs(kdmin - unc_kdmin))", "m", "uncertainty of depth of the water column from which 90% of the water leaving irradiance comes from");

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
        flagCoding.addFlag("Valid_PE", 0x01 << FLAG_INDEX_VALID_PE, "The operators valid pixel expression has resolved to true");

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

    private void ensureSpectralProperties(Band band, int i) {
        ProductUtils.copySpectralBandProperties(getSourceProduct().getBand(getRadianceBandName(i)), band);
        if (band.getSpectralWavelength() == 0) {
            band.setSpectralWavelength(C2rccMerisAlgorithm.DEFAULT_MERIS_WAVELENGTH[i - 1]);
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

    public abstract String getAtmosphericAuxDataPath();

    public abstract Product getTomsomiStartProduct();

    public abstract Product getTomsomiEndProduct();

    public abstract Product getNcepStartProduct();

    public abstract Product getNcepEndProduct();

    public abstract double getOzone();

    public abstract double getPress();

    public abstract double getTSMfakBpart();

    public abstract double getTSMfakBwit();

    public abstract double getCHLexp();

    public abstract double getCHLfak();

    protected abstract RasterDataNode getPressureRaster();

    protected abstract RasterDataNode getOzoneRaster();

    void initAtmosphericAuxdata() {
        AtmosphericAuxdataBuilder auxdataBuilder = new AtmosphericAuxdataBuilder();
        auxdataBuilder.setOzone(getOzone());
        auxdataBuilder.setSurfacePressure(getPress());
        auxdataBuilder.useAtmosphericAuxDataPath(getAtmosphericAuxDataPath());
        auxdataBuilder.useTomsomiProducts(getTomsomiStartProduct(), getTomsomiEndProduct());
        auxdataBuilder.useNcepProducts(getNcepStartProduct(), getNcepEndProduct());
        if (getUseEcmwfAuxData()) {
            auxdataBuilder.useAtmosphericRaster(getOzoneRaster(), getPressureRaster());
        }
        try {
            atmosphericAuxdata = auxdataBuilder.create();
        } catch (Exception e) {
            throw new OperatorException("Could not create provider for atmospheric auxdata", e);
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        if (atmosphericAuxdata != null) {
            atmosphericAuxdata.dispose();
            atmosphericAuxdata = null;
        }
    }

}
