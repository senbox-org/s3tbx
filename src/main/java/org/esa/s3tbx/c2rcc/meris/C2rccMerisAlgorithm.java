package org.esa.s3tbx.c2rcc.meris;

import static java.lang.Math.PI;
import static java.lang.Math.abs;
import static java.lang.Math.acos;
import static java.lang.Math.cos;
import static java.lang.Math.exp;
import static java.lang.Math.log;
import static java.lang.Math.max;
import static java.lang.Math.pow;
import static java.lang.Math.sin;
import static java.lang.Math.toDegrees;
import static java.lang.Math.toRadians;
import static org.esa.s3tbx.ArrayMath.a_abs;
import static org.esa.s3tbx.ArrayMath.a_exp;
import static org.esa.s3tbx.ArrayMath.a_max;

import org.esa.snap.core.nn.NNffbpAlphaTabFast;
import org.esa.snap.core.util.BitSetter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * @author Roland Doerffer
 * @author Norman Fomferra
 */
public class C2rccMerisAlgorithm {

    public final int IDX_rtosa_aann = 0;
    public final int IDX_rtosa_rw = 1;
    public final int IDX_rw_iop = 2;
    public final int IDX_iop_rw = 3;
    public final int IDX_rw_kd = 4;
    public final int IDX_iop_unciop = 5;
    public final int IDX_iop_uncsumiop_unckd = 6;
    public final int IDX_rw_rwnorm = 7;
    public final int IDX_rtosa_trans = 8;
    public final int IDX_rtosa_rpath = 9;

    private final ArrayList<String> nnNames;


    // todo RD20151007 warnings durchsehen

    /**
     * Structure for returning the algorithm's result.
     */
    public static class Result {

        public final double[] r_toa;
        public final double[] r_tosa;
        public final double[] rtosa_aann;
        public final double rtosa_oos;
        public final double[] rpath_nn;
        public final double[] transd_nn;
        public final double[] transu_nn;
        public final double[] rwa;
        public final double rwa_oos;
        public final double[] rwn;
        public final double[] iops_nn;
        public final double kd489_nn;
        public final double kdmin_nn;
        public final double[] unc_iop_abs;
        public final double unc_abs_adg;
        public final double unc_abs_atot;
        public final double unc_abs_btot;
        public final double unc_abs_chl;
        public final double unc_abs_tsm;
        public final double unc_abs_kd489;
        public final double unc_abs_kdmin;
        public final int flags;

        public Result(double[] r_toa, double[] r_tosa, double[] rtosa_aann, double[] rpath_nn, double[] transd_nn, double[] transu_nn, double[] rwa,
                      double[] rwn, double rtosa_oos, double rwa_oos, double[] iops_nn,
                      double kd489_nn, double kdmin_nn, double[] unc_iop_abs,
                      double unc_abs_adg, double unc_abs_atot, double unc_abs_btot, double unc_abs_chl,
                      double unc_abs_tsm, double unc_abs_kd489, double unc_abs_kdmin, int flags) {

            this.r_toa = r_toa;
            this.r_tosa = r_tosa;
            this.rtosa_aann = rtosa_aann;
            this.rtosa_oos = rtosa_oos;
            this.rpath_nn = rpath_nn;
            this.transd_nn = transd_nn;
            this.transu_nn = transu_nn;
            this.rwa = rwa;
            this.rwa_oos = rwa_oos;
            this.rwn = rwn;
            this.iops_nn = iops_nn;
            this.kd489_nn = kd489_nn;
            this.kdmin_nn = kdmin_nn;
            this.unc_iop_abs = unc_iop_abs;
            this.unc_abs_adg = unc_abs_adg;
            this.unc_abs_atot = unc_abs_atot;
            this.unc_abs_btot = unc_abs_btot;
            this.unc_abs_chl = unc_abs_chl;
            this.unc_abs_tsm = unc_abs_tsm;
            this.unc_abs_kdmin = unc_abs_kdmin;
            this.unc_abs_kd489 = unc_abs_kd489;
            this.flags = flags;
        }
    }


    // gas absorption constants for 12 MERIS channels
    static final double[] absorb_ozon = {
                8.2e-04, 2.82e-03, 2.076e-02, 3.96e-02,
                1.022e-01, 1.059e-01, 5.313e-02, 3.552e-02,
                1.895e-02, 8.38e-03, 7.2e-04, 0.0
    };

    // polynom coefficients for band708 H2O correction
    static final double[] h2o_cor_poly = {0.3832989, 1.6527957, -1.5635101, 0.5311913};

    static final int[] merband12_ix = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 13};
    static final int[] merband15_ix = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};

    public static double[] DEFAULT_SOLAR_FLUX = new double[]{
            1724.724,
            1889.8026,
            1939.5339,
            1940.1365,
            1813.5457,
            1660.3589,
            1540.5198,
            1480.7161,
            1416.1177,
            1273.394,
            1261.8658,
            1184.0952,
            963.94995,
            935.23706,
            900.659,
    };

    double salinity = 35.0;
    double temperature = 15.0;

    private boolean outputRtoaGcAann;
    private boolean outputRpath;
    private boolean outputTdown;
    private boolean outputTup;
    private boolean outputRwa;
    private boolean outputRwn;
    private boolean outputOos;
    private boolean outputKd;
    private boolean outputUncertainties;

    // (5) thresholds for flags
    double[] thresh_rtosaaaNNrat = {0.98, 1.05};  // threshold for out of scope flag Rtosa has to be adjusted
    double[] thresh_rwslope = {0.95, 1.05};    // threshold for out of scope flag Rw has to be adjusted

    // (5) thresholds for flags
    double log_threshfak_oor = 0.02; // == ~1.02, for log variables
    double thresh_absd_log_rtosa; // threshold for rtosa_oos (max abs log difference)
    double thresh_rwlogslope;  // threshold for rwa_oos

    final ThreadLocal<NNffbpAlphaTabFast> rw_iop; // NN Rw -< IOPs input 10 bands, 5 IOPs
    final ThreadLocal<NNffbpAlphaTabFast> rtosa_rw; // NN Rtosa -> Rw 12 bands
    final ThreadLocal<NNffbpAlphaTabFast> rtosa_aann; // Rtosa -> Rtosa' 12 bands

    final ThreadLocal<NNffbpAlphaTabFast> rtosa_rpath; //Rtosa -> Rpath 12 bands
    final ThreadLocal<NNffbpAlphaTabFast> rtosa_trans; // Rtosa -> transd, transu 12 bands
    final ThreadLocal<NNffbpAlphaTabFast> iop_rw; // IOPs(5) -> Rw' (10 bands)
    final ThreadLocal<NNffbpAlphaTabFast> rw_kd; // Rw (10 bands) -> kd489, kdmin
    final ThreadLocal<NNffbpAlphaTabFast> iop_unciop; // IOPs (5) -> uncertainties of IOPs (5)
    final ThreadLocal<NNffbpAlphaTabFast> iop_uncsumiop_unckd; // IOPs (5) -> unc_adg, unc_atot, unc_btot, unc_kd489, unc_kdmin
    final ThreadLocal<NNffbpAlphaTabFast> rw_rwnorm; // Rw (10) -> Rwn (10)

    public void setThresh_absd_log_rtosa(double thresh_absd_log_rtosa) {
        this.thresh_absd_log_rtosa = thresh_absd_log_rtosa;
    }

    public void setThresh_rwlogslope(double thresh_rwlogslope) {
        this.thresh_rwlogslope = thresh_rwlogslope;
    }

    public void setOutputRtoaGcAann(boolean outputRtoaGcAann) {
        this.outputRtoaGcAann = outputRtoaGcAann;
    }

    public void setOutputRpath(boolean outputRpath) {
        this.outputRpath = outputRpath;
    }

    public void setOutputTdown(boolean outputTdown) {
        this.outputTdown = outputTdown;
    }

    public void setOutputTup(boolean outputTup) {
        this.outputTup = outputTup;
    }

    public void setOutputRwa(boolean outputRwa) {
        this.outputRwa = outputRwa;
    }

    public void setOutputRwn(boolean outputRwn) {
        this.outputRwn = outputRwn;
    }

    public void setOutputOos(boolean outputOos) {
        this.outputOos = outputOos;
    }

    public void setOutputKd(boolean outputKd) {
        this.outputKd = outputKd;
    }

    public void setOutputUncertainties(boolean outputUncertainties) {
        this.outputUncertainties = outputUncertainties;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public void setSalinity(double salinity) {
        this.salinity = salinity;
    }

    public Result processPixel(int px, int py,
                               double lat, double lon,
                               double[] toa_rad,
                               double[] solflux,
                               double sun_zeni,
                               double sun_azi,
                               double view_zeni,
                               double view_azi,
                               double dem_alt,
                               double atm_press,
                               double ozone) {

        //  (9.2) compute angles
        double cos_sun = cos(toRadians(sun_zeni));
        double cos_view = cos(toRadians(view_zeni));
        double sin_sun = sin(toRadians(sun_zeni));
        double sin_view = sin(toRadians(view_zeni));

        double cos_azi_diff = cos(toRadians(view_azi - sun_azi));
        double azi_diff_rad = acos(cos_azi_diff);
        double sin_azi_diff = sin(azi_diff_rad);
        double azi_diff_deg = toDegrees(azi_diff_rad);

        double x = sin_view * cos_azi_diff;
        double y = sin_view * sin_azi_diff;
        double z = cos_view;

        double[] r_toa = new double[toa_rad.length];
        for (int i = 0; i < toa_rad.length; i++) {
            r_toa[i] = PI * toa_rad[i] / solflux[i] / cos_sun;
        }

        double[] r_tosa_ur = new double[merband12_ix.length];
        for (int i = 0; i < merband12_ix.length; i++) {
            r_tosa_ur[i] = r_toa[merband12_ix[i] - 1]; // -1 because counts in Scilab start at 1 not 0
        }

        // (9.3.0) +++ water vapour correction for band 9 +++++ */
        //X2=rho_900/rho_885;
        double X2 = r_toa[14] / r_toa[13];
        double trans708 = h2o_cor_poly[0] + (h2o_cor_poly[1] + (h2o_cor_poly[2] + h2o_cor_poly[3] * X2) * X2) * X2;
        r_tosa_ur[8] /= trans708;

        //*** (9.3.1) ozone correction ***/
        double model_ozone = 0;

        double[] r_tosa = new double[r_tosa_ur.length];
        double[] log_rtosa = new double[r_tosa_ur.length];
        for (int i = 0; i < r_tosa_ur.length; i++) {

            double trans_ozoned12 = exp(-(absorb_ozon[i] * ozone / 1000.0 - model_ozone) / cos_sun);
            double trans_ozoneu12 = exp(-(absorb_ozon[i] * ozone / 1000.0 - model_ozone) / cos_view);
            double trans_ozone12 = trans_ozoned12 * trans_ozoneu12;

            double r_tosa_oz = r_tosa_ur[i] / trans_ozone12;

            r_tosa[i] = r_tosa_oz;
            log_rtosa[i] = log(r_tosa[i]);
        }

        // (9.3.2) altitude pressure correction
        // this is only a very simplified formula, later use more exact one
        // also for larger lakes the dem_alt presently provideds the altitude of the lake bottom
        // will be changed later to altitude of the lake surface
        double alti_press;
        if (dem_alt > 10.0) {
            alti_press = atm_press * exp(-dem_alt / 8000.0);
        } else {
            alti_press = atm_press;
        }

        // (9.4) )set input to all atmosphere NNs
        //nn_in=[sun_zeni,x,y,z,temperature, salinity, alti_press, log_rtosa];
        double[] nn_in = new double[7 + log_rtosa.length];
        nn_in[0] = sun_zeni;
        nn_in[1] = x;
        nn_in[2] = y;
        nn_in[3] = z;
        nn_in[4] = temperature;
        nn_in[5] = salinity;
        nn_in[6] = alti_press;
        System.arraycopy(log_rtosa, 0, nn_in, 7, log_rtosa.length);

        int flags = 0;

        // (9.4.1) test if input tosa spectrum is out of range
        // mima=aa_rtosa_nn_bn7_9(5); // minima and maxima of aaNN input
        double[] mi = rtosa_aann.get().getInmin();
        double[] ma = rtosa_aann.get().getInmax();
        boolean rtosa_oor_flag = false; // (ipix)
        // for iv=1:19,// variables
        for (int iv = 0; iv < nn_in.length; iv++) { // variables
            if (nn_in[iv] < mi[iv] || nn_in[iv] > ma[iv]) {
                rtosa_oor_flag = true; // (ipix)
            }
        }
        flags = BitSetter.setFlag(flags, 0, rtosa_oor_flag);


        // (9.4.2) test out of scope spectra with autoassociative neural network
        double[] rtosa_aann = new double[0];
        double[] log_rtosa_aann = new double[0];
        if (outputRtoaGcAann || outputOos) {
            log_rtosa_aann = this.rtosa_aann.get().calc(nn_in);
            rtosa_aann = a_exp(log_rtosa_aann);
        }
        //double[] rtosa_aaNNrat = adiv(rtosa_aann, r_tosa);
        //rtosa_aaNNrat_a(ipix,:)=rtosa_aaNNrat;
        double rtosa_oos = 0;
        if (outputOos) {
            double[] abs_diff_log_rtosa = a_abs(log_rtosa, log_rtosa_aann);
            rtosa_oos = a_max(abs_diff_log_rtosa);
        }


        // (9.6.1) set rho_toa out of scope flag
        // double rtosa_aaNNrat_min = amin(rtosa_aaNNrat);
        // double rtosa_aaNNrat_max = amax(rtosa_aaNNrat);
        //double rtosa_aaNNrat_minmax_a = Math.max(rtosa_aaNNrat_max, 1.0 / rtosa_aaNNrat_min); // (ipix)

        // (9.4.3) set rho_toa out of scope flag
        boolean rtosa_oos_flag = false;
        if (rtosa_oos > thresh_absd_log_rtosa) {
            rtosa_oos_flag = true; // set flag if ratios outside thresholds
        }
                /*
        boolean flag_rtosa = false; // (ipix)
        if (rtosa_aaNNrat_min < thresh_rtosaaaNNrat[0] || rtosa_aaNNrat_max > thresh_rtosaaaNNrat[1]) {
            flag_rtosa = true; // set flag if difference of band 5 > threshold // (ipix)
        }
        */
        flags = BitSetter.setFlag(flags, 1, rtosa_oos_flag);

        // (9.4.4) NN compute rpath from rtosa
        double[] rpath_nn = new double[0];
        if (outputRpath) {
            double[] log_rpath_nn = rtosa_rpath.get().calc(nn_in);
            rpath_nn = a_exp(log_rpath_nn);
        }

        // (9.4.5) NN compute transmittance from rtosa
        double[] transd_nn = new double[0];
        double[] transu_nn = new double[0];
        if (outputTdown || outputTup) {
            double[] trans_nn = rtosa_trans.get().calc(nn_in);
            if (outputTdown) {
                transd_nn = Arrays.copyOfRange(trans_nn, 0, 12);
            }
            if (outputTup) {
                transu_nn = Arrays.copyOfRange(trans_nn, 12, 24);
            }
        }

        // (9.4.6)
        double[] log_rw = rtosa_rw.get().calc(nn_in);
        double[] rwa = new double[0];
        if (outputRwa) {
            rwa = a_exp(log_rw);
        }

        // (9.5) water part

        // define input to water NNs
        //nn_in_inv=[sun_zeni view_zeni azi_diff_deg temperature salinity log_rw(1:10)];
        double[] nn_in_inv = new double[5 + 10];
        nn_in_inv[0] = sun_zeni;
        nn_in_inv[1] = view_zeni;
        nn_in_inv[2] = azi_diff_deg;
        nn_in_inv[3] = temperature;
        nn_in_inv[4] = salinity;
        System.arraycopy(log_rw, 0, nn_in_inv, 5, 10);

        // (9.5.1)check input to rw -> IOP NN out of range
        mi = rw_iop.get().getInmin();
        ma = rw_iop.get().getInmax();
        boolean rwa_oor_flag = false;
        for (int iv = 0; iv < nn_in_inv.length; iv++) {
            if (nn_in_inv[iv] < mi[iv] | nn_in_inv[iv] > ma[iv]) {
                rwa_oor_flag = true; // (ipix)
            }
        }
        flags = BitSetter.setFlag(flags, 2, rwa_oor_flag);

        // (9.x.x.) NN compute Rwn from Rw
        double[] rwn = new double[0];
        if (outputRwn) {
            double[] log_rwn = rw_rwnorm.get().calc(nn_in_inv);
            rwn = a_exp(log_rwn);
        }

        // (9.10.1) NN compute IOPs from rw
        double[] log_iops_nn1 = rw_iop.get().calc(nn_in_inv);
        double[] iops_nn = a_exp(log_iops_nn1);

        // (9.14) compute combined IOPs and concentrations
        // split IOPs
        double log_conc_ap_nn1 = log_iops_nn1[0];
        double log_conc_ad_nn1 = log_iops_nn1[1];
        double log_conc_ag_nn1 = log_iops_nn1[2];
        double log_conc_bp_nn1 = log_iops_nn1[3];
        double log_conc_bw_nn1 = log_iops_nn1[4];

        double ap_nn1 = exp(log_conc_ap_nn1);
        double ad_nn1 = exp(log_conc_ad_nn1);
        double ag_nn1 = exp(log_conc_ag_nn1);
        double bp_nn1 = exp(log_conc_bp_nn1);
        double bw_nn1 = exp(log_conc_bw_nn1);

        // combine IOPs
        double adg_nn1 = ad_nn1 + ag_nn1;
        double atot_nn1 = adg_nn1 + ap_nn1;
        double btot_nn1 = bp_nn1 + bw_nn1;

        // compute concentrations
        // todo Roland fragen ... CHLfaktor wirklich 21.o oder 20.0 ?
//        double chl_nn1 = 21.0 * pow(ap_nn1, 1.04);
//        double tsm_nn1 = btot_nn1 * 1.73;

        // (9.5.4) check if log_IOPs out of range
        mi = rw_iop.get().getOutmin();
        ma = rw_iop.get().getOutmax();
        boolean iop_oor_flag = false;
        for (int iv = 0; iv < log_iops_nn1.length; iv++) {
            if (log_iops_nn1[iv] < mi[iv] | log_iops_nn1[iv] > ma[iv]) {
                iop_oor_flag = true;
            }
        }
        flags = BitSetter.setFlag(flags, 3, iop_oor_flag);

        // (9.5.5)check if log_IOPs at limit
        boolean[] iop_at_max_flag = new boolean[log_iops_nn1.length];
        for (int iv = 0; iv < log_iops_nn1.length; iv++) {
            iop_at_max_flag[iv] = false;
            if (log_iops_nn1[iv] > (ma[iv] - log_threshfak_oor)) {
                iop_at_max_flag[iv] = true;
            }
            flags = BitSetter.setFlag(flags, iv + 4, iop_at_max_flag[iv]);
        }

        boolean[] iop_at_min_flag = new boolean[log_iops_nn1.length];
        for (int iv = 0; iv < log_iops_nn1.length; iv++) {
            iop_at_min_flag[iv] = false;
            if (log_iops_nn1[iv] < (mi[iv] + log_threshfak_oor)) {
                iop_at_min_flag[iv] = true;
            }
            flags = BitSetter.setFlag(flags, iv + 9, iop_at_min_flag[iv]);
        }

        // (9.5.6) compute Rw out of scope
        //nn_in_for=[sun_zeni view_zeni azi_diff_deg temperature salinity log_iops_nn1];// input to forward water NN

        double[] nn_in_for = new double[5 + 5];
        nn_in_for[0] = sun_zeni;
        nn_in_for[1] = view_zeni;
        nn_in_for[2] = azi_diff_deg;
        nn_in_for[3] = temperature;
        nn_in_for[4] = salinity;
        System.arraycopy(log_iops_nn1, 0, nn_in_for, 5, 5);

        //log_rw_nn2 = nnhs_ff(for_nn9b,nn_in_for); // compute rho_w from IOPs

        double rwa_oos = 0;
        if (outputOos) {
            double[] log_rw_nn2 = iop_rw.get().calc(nn_in_for);

            // (9.5.7) test out of scope of rho_w by combining inverse and forward NN
            //  compute the test and set rw is out of scope flag
            double s1_mess = abs(log_rw[4] - log_rw[1]); // s1_mess and s2_mess are the band ratios of Rw
            double s2_mess = abs(log_rw[5] - log_rw[4]);
            double s1_nn2 = abs(log_rw_nn2[4] - log_rw_nn2[1]);// s1_nn2 is the band ratios of Rw'
            double s2_nn2 = abs(log_rw_nn2[5] - log_rw_nn2[4]);
            double s1_test = abs(s1_nn2 - s1_mess); // relative deviation for band ratio 5/2 (diff on log)
            double s2_test = abs(s2_nn2 - s2_mess); // relative deviation for band ratio 6/5 (diff on log)
            rwa_oos = max(s1_test, s2_test);// maximum deviation output as quality indicator
            boolean rwa_oos_flag = false;
            if (rwa_oos > thresh_rwlogslope) {
                rwa_oos_flag = true;
            }
            flags = BitSetter.setFlag(flags, 14, rwa_oos_flag);
        }

        // (9.5.8) NN compute kd from rw
        double kdmin_nn = 0;
        double kd489_nn = 0;
        if (outputKd || outputUncertainties) {
            double[] log_kd2_nn = rw_kd.get().calc(nn_in_inv);
            kdmin_nn = exp(log_kd2_nn[0]);
            kd489_nn = exp(log_kd2_nn[1]);
//            double z90max = 1.0 / kdmin_nn;

            // (9.5.9) test if kd is at nn limits
            mi = rw_kd.get().getOutmin();
            ma = rw_kd.get().getOutmax();
            boolean kdmin_oor_flag = false;
            if (log_kd2_nn[0] < mi[0] | log_kd2_nn[0] > ma[0]) {
                kdmin_oor_flag = true;
            }
            flags = BitSetter.setFlag(flags, 16, kdmin_oor_flag);

            boolean kd489_oor_flag = false;
            if (log_kd2_nn[1] < mi[1] | log_kd2_nn[1] > ma[1]) {
                kd489_oor_flag = true;
            }
            flags = BitSetter.setFlag(flags, 15, kd489_oor_flag);

            boolean kdmin_at_max_flag = false;
            if (log_kd2_nn[1] > ma[1] - log_threshfak_oor) {
                kdmin_at_max_flag = true;
            }
            flags = BitSetter.setFlag(flags, 18, kdmin_at_max_flag);

            boolean kd489_at_max_flag = false;
            if (log_kd2_nn[1] > ma[1] - log_threshfak_oor) {
                kd489_at_max_flag = true;
            }
            flags = BitSetter.setFlag(flags, 17, kd489_at_max_flag);
        }

        // (9.6) )NN compute uncertainties
        double unc_iop_abs[] = new double[0];
        double unc_abs_chl = 0;
        double unc_abs_adg = 0;
        double unc_abs_atot = 0;
        double unc_abs_btot = 0;
        double unc_abs_kd489 = 0;
        double unc_abs_kdmin = 0;
        double unc_abs_tsm = 0;
        if (outputUncertainties) {
            double[] diff_log_abs_iop = iop_unciop.get().calc(log_iops_nn1);


            double unc_iop_rel[] = new double[diff_log_abs_iop.length];
            unc_iop_abs = new double[diff_log_abs_iop.length];
            for (int iv = 0; iv < diff_log_abs_iop.length; iv++) {
                unc_iop_rel[iv] = (exp(diff_log_abs_iop[iv]) - 1) * 100;
                unc_iop_abs[iv] = iops_nn[iv] * (1.0 - exp(-diff_log_abs_iop[iv]));
            }

            unc_abs_chl = 21.0 * pow(unc_iop_abs[1], 1.04);

            // (9.16) NN compute uncertainties for combined IOPs and kd
            double[] diff_log_abs_combi_kd = iop_uncsumiop_unckd.get().calc(log_iops_nn1);
            double diff_log_abs_adg = diff_log_abs_combi_kd[0];
            double diff_log_abs_atot = diff_log_abs_combi_kd[1];
            double diff_log_abs_btot = diff_log_abs_combi_kd[2];
            double diff_log_abs_kd489 = diff_log_abs_combi_kd[3];
            double diff_log_abs_kdmin = diff_log_abs_combi_kd[4];
            unc_abs_adg = (1.0 - exp(-diff_log_abs_adg)) * adg_nn1;
            unc_abs_atot = (1.0 - exp(-diff_log_abs_atot)) * atot_nn1;
            unc_abs_btot = (1.0 - exp(-diff_log_abs_btot)) * btot_nn1;
            unc_abs_kd489 = (1.0 - exp(-diff_log_abs_kd489)) * kd489_nn;
            unc_abs_kdmin = (1.0 - exp(-diff_log_abs_kd489)) * kdmin_nn;
//        double unc_z90max = abs(z90max - 1.0 / abs(kdmin_nn - unc_abs_kdmin));
            unc_abs_tsm = 1.73 * unc_abs_btot;
        }


        return new Result(r_toa, r_tosa, rtosa_aann, rpath_nn, transd_nn, transu_nn, rwa, rwn, rtosa_oos, rwa_oos,
                          iops_nn, kd489_nn, kdmin_nn, unc_iop_abs, unc_abs_adg, unc_abs_atot, unc_abs_btot,
                          unc_abs_chl, unc_abs_tsm, unc_abs_kd489, unc_abs_kdmin, flags);
    }

    C2rccMerisAlgorithm(final String[] nnFilePaths) throws IOException {
        nnNames = new ArrayList<>();

        final String[] paths;
        final boolean isDefault = nnFilePaths == null;
        if (isDefault) {
            paths = new String[10];
            paths[IDX_rtosa_aann] = "meris/richard_atmo_invers29_press_20150125/rtoa_aaNN7/31x7x31_555.6.net";
            paths[IDX_rtosa_rw] = "meris/richard_atmo_invers29_press_20150125/rtoa_rw_nn3/33x73x53x33_470639.6.net";
            paths[IDX_rw_iop] = "meris/coastcolour_wat_20140318/inv_meris_logrw_logiop_20140318_noise_p5_fl/97x77x37_11671.0.net";
            paths[IDX_iop_rw] = "meris/coastcolour_wat_20140318/for_meris_logrw_logiop_20140318_p5_fl/17x97x47_335.3.net";
            paths[IDX_rw_kd] = "meris/coastcolour_wat_20140318/inv_meris_kd/97x77x7_232.4.net";
            paths[IDX_iop_unciop] = "meris/coastcolour_wat_20140318/uncertain_log_abs_biasc_iop/17x77x37_11486.7.net";
            paths[IDX_iop_uncsumiop_unckd] = "meris/coastcolour_wat_20140318/uncertain_log_abs_tot_kd/17x77x37_9113.1.net";
            paths[IDX_rw_rwnorm] = "meris/coastcolour_wat_20140318/norma_net_20150307/37x57x17_76.8.net";
            paths[IDX_rtosa_trans] = "meris/richard_atmo_invers29_press_20150125/rtoa_trans_nn2/31x77x57x37_37087.4.net";
            paths[IDX_rtosa_rpath] = "meris/richard_atmo_invers29_press_20150125/rtoa_rpath_nn2/31x77x57x37_2388.6.net";
        } else {
            paths = nnFilePaths;
        }

        // rtosa auto NN
        rtosa_aann = nnhs(paths[IDX_rtosa_aann], isDefault);

        // rtosa-rw NN
        rtosa_rw = nnhs(paths[IDX_rtosa_rw], isDefault);

        // rtosa - rpath NN
        //ThreadLocal<NNffbpAlphaTabFast> rpath_nn9 = nnhs("meris/richard_atmo_invers29_press_20150125/rtoa_rpath_nn2/31x77x57x37_2388.6.net");

        // rtosa - trans NN
        //ThreadLocal<NNffbpAlphaTabFast> inv_trans_nn = nnhs("meris/richard_atmo_invers29_press_20150125/rtoa_trans_nn2/31x77x57x37_37087.4.net");

        // rw-IOP inverse NN
        rw_iop = nnhs(paths[IDX_rw_iop], isDefault);

        // IOP-rw forward NN
        //ThreadLocal<NNffbpAlphaTabFast> for_nn9b = nnhs("coastcolour_wat_20140318/for_meris_logrw_logiop_20140318_p5_fl/17x97x47_335.3.net"); //only 10 MERIS bands
        iop_rw = nnhs(paths[IDX_iop_rw], isDefault); //only 10 MERIS bands

        // rw-kd NN, output are kdmin and kd449
        //ThreadLocal<NNffbpAlphaTabFast> kd2_nn7 = nnhs("coastcolour_wat_20140318/inv_meris_kd/97x77x7_232.4.net");
        rw_kd = nnhs(paths[IDX_rw_kd], isDefault);

        // uncertainty NN for IOPs after bias corretion
        //ThreadLocal<NNffbpAlphaTabFast> unc_biasc_nn1 = nnhs("../nets/coastcolour_wat_20140318/uncertain_log_abs_biasc_iop/17x77x37_11486.7.net");
        iop_unciop = nnhs(paths[IDX_iop_unciop]   , isDefault);
        // uncertainty for atot, adg, btot and kd
        //ThreadLocal<NNffbpAlphaTabFast> unc_biasc_atotkd_nn = nnhs("../nets/coastcolour_wat_20140318/uncertain_log_abs_tot_kd/17x77x37_9113.1.net");
        iop_uncsumiop_unckd = nnhs(paths[IDX_iop_uncsumiop_unckd]  , isDefault);

        // todo RD20151007
        rw_rwnorm = nnhs(paths[IDX_rw_rwnorm] , isDefault);
        rtosa_trans = nnhs(paths[IDX_rtosa_trans], isDefault);
        rtosa_rpath = nnhs(paths[IDX_rtosa_rpath], isDefault);
    }

    public String[] getUsedNeuronalNetNames() {
        return nnNames.toArray(new String[nnNames.size()]);
    }

    private ThreadLocal<NNffbpAlphaTabFast> nnhs(String sourcePath, boolean resource) throws IOException {

//        Files.

        final InputStream stream;
        if (resource) {
            String name = "/auxdata/nets/" + sourcePath;
            stream = C2rccMerisAlgorithm.class.getResourceAsStream(name);
            if (stream == null) {
                throw new IllegalStateException("resource not found: " + name);
            }
            nnNames.add(name);
        } else {
            final Path path = Paths.get(sourcePath);
            stream = Files.newInputStream(path, StandardOpenOption.READ);
            if (stream == null) {
                throw new IllegalStateException("file not found: " + path.toString());
            }
            nnNames.add(path.toString());
        }
        final String nnCode = readFully(stream);
        return new ThreadLocal<NNffbpAlphaTabFast>() {
            @Override
            protected NNffbpAlphaTabFast initialValue() {
                try {
                    return new NNffbpAlphaTabFast(nnCode);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }
        };
    }

    private String readFully(InputStream stream) throws IOException {
        String text;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            text = "";
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                text += line + "\n";
            }
        }
        return text;
    }

}
