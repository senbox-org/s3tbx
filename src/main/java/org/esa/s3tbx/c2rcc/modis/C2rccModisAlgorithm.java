package org.esa.s3tbx.c2rcc.modis;

import org.esa.snap.core.nn.NNffbpAlphaTabFast;
import org.esa.snap.core.util.BitSetter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static java.lang.Math.*;
import static org.esa.s3tbx.ArrayMath.*;

/**
 * @author Wolfgang Schoenfeld
 * @author Sabine Embacher
 */
public class C2rccModisAlgorithm {

    public final static double salinity_default = 35.0;
    public final static double temperature_default = 15.0;
    public final static double pressure_default = 1000.0;
    public final static double ozone_default = 330.0;

    // input for rtoa_rw_modis_nn3/33x73x53x33_508087.3.net
    // private static final int[] reflec_wavelengths = new int[]{412, 443, 489, 531, 551, 665, 678, 748, 869}
    // corrected ...
    // 489 replaced by 488
    // 551 replaced by 547
    // 665 replaced by 667
    public final static int[] reflec_wavelengths = new int[]{
                412,
                443,
                488,
                531,
                547,
                667,
                678,
                748,
                869
    };

    /**
     * Structure for returning the algorithm's result.
     */
    public static class Result {

        public final double[] rw;
        public final double[] iops;
        public final double[] rtosa_in;
        public final double[] rtosa_out;
        public final double rtosa_ratio_min;
        public final double rtosa_ratio_max;
        public final int flags;

        public Result(double[] rw, double[] iops, double[] rtosa_in, double[] rtosa_out, double rtosa_ratio_min, double rtosa_ratio_max, int flags) {
            this.rw = rw;
            this.iops = iops;
            this.rtosa_in = rtosa_in;
            this.rtosa_out = rtosa_out;
            this.rtosa_ratio_min = rtosa_ratio_min;
            this.rtosa_ratio_max = rtosa_ratio_max;
            this.flags = flags;
        }
    }

    // ozon absorption constants for MODIS channels
    public final static double[] k_oz_per_wl = new double[]{
                1.987E-03, // k_oz(1) =   Lambda(1) = 412
                3.189E-03, // k_oz(2) =   Lambda(2) = 443
                2.032E-02, // k_oz(3) =   Lambda(3) = 488
                6.838E-02, // k_oz(4) =   Lambda(4) = 531
                8.622E-02, // k_oz(5) =   Lambda(5) = 547
                //             #Lambda(5) = 551
                4.890E-02, // k_oz(6) =   Lambda(6) = 667
                3.787E-02, // k_oz(7) =   Lambda(7) = 678
                1.235E-02, // k_oz(8) =   Lambda(8) = 748
                1.936E-03  // k_oz(9) =   Lambda(9) = 869
    };

    private double salinity = salinity_default;
    private double temperature = temperature_default;

    // (5) thresholds for flags
    double[] thresh_rtosaaaNNrat = {0.95, 1.05};  // threshold for out of scope flag Rtosa has to be adjusted
    double[] thresh_rwslope = {0.95, 1.05};    // threshold for out of scope flag Rw has to be adjusted

    private ThreadLocal<NNffbpAlphaTabFast> rtoa_rw_nn3;
    private ThreadLocal<NNffbpAlphaTabFast> rw_IOP;
    private ThreadLocal<NNffbpAlphaTabFast> rtoa_aaNN7;

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public void setSalinity(double salinity) {
        this.salinity = salinity;
    }

    public C2rccModisAlgorithm() throws IOException {
        rtoa_rw_nn3 = nnhs("modis/rtoa_rw_modis_nn3/33x73x53x33_508087.3.net");
        rw_IOP = nnhs("modis/inv_modis_fl/97x77x37_13150.2.net");
        rtoa_aaNN7 = nnhs("modis/rtoa_modis_aaNN7/31x7x31_250.8.net");
    }

    public Result processPixel(double[] toa_ref,
                               double sun_zeni,
                               double sun_azi,
                               double sensor_zeni,
                               double view_azi,
                               double atm_press,
                               double ozone) {

        //  (9.2) compute angles
        final double cos_sun_zen = cos(toRadians(sun_zeni));
        final double cos_sensor_zen = cos(toRadians(sensor_zeni));
        final double sin_sun = sin(toRadians(sun_zeni));
        final double sin_sensor_zen = sin(toRadians(sensor_zeni));

        final double azi_diff_rad = toRadians(view_azi - sun_azi);
        final double cos_azi_diff = cos(azi_diff_rad);
        final double sin_azi_diff = sin(azi_diff_rad);
        final double azi_diff_deg = toDegrees(azi_diff_rad);

        double x = sin_sensor_zen * cos_azi_diff;
        double y = sin_sensor_zen * sin_azi_diff;
        double z = cos_sensor_zen;

        double[] r_tosa = new double[toa_ref.length];
        for (int i = 0; i < toa_ref.length; i++) {
            double trans_ozoned12 = exp(-(k_oz_per_wl[i] * ozone / 1000.0) / cos_sun_zen);
            double trans_ozoneu12 = exp(-(k_oz_per_wl[i] * ozone / 1000.0) / cos_sensor_zen);
            double trans_ozone12 = trans_ozoned12 * trans_ozoneu12;
            r_tosa[i] = toa_ref[i] / trans_ozone12;
        }
        double[] log_rtosa = a_log(r_tosa);

        // set NN input
        double[] nn_in = new double[7 + log_rtosa.length];
        nn_in[0] = sun_zeni;
        nn_in[1] = x;
        nn_in[2] = y;
        nn_in[3] = z;
        nn_in[4] = temperature;
        nn_in[5] = salinity;
        nn_in[6] = atm_press;
        System.arraycopy(log_rtosa, 0, nn_in, 7, log_rtosa.length);

        double[] log_rw = rtoa_rw_nn3.get().calc(nn_in);
        double[] rw = a_exp(log_rw);

         // (9.5) test out of scope spectra with autoassociative neural network
        double[] log_rtosa_aann = rtoa_aaNN7.get().calc(nn_in);
        double[] rtosa_aann = a_exp(log_rtosa_aann);
        double[] rtosa_aaNNrat = a_div(rtosa_aann, r_tosa);
        //rtosa_aaNNrat_a(ipix,:)=rtosa_aaNNrat;

        int flags = 0;

        // (9.6.1) set rho_toa out of scope flag
        double rtosa_aaNNrat_min = a_min(rtosa_aaNNrat);
        double rtosa_aaNNrat_max = a_max(rtosa_aaNNrat);
        //double rtosa_aaNNrat_minmax_a = Math.max(rtosa_aaNNrat_max, 1.0 / rtosa_aaNNrat_min); // (ipix)

        boolean flag_rtosa = false; // (ipix)
        if (rtosa_aaNNrat_min < thresh_rtosaaaNNrat[0] || rtosa_aaNNrat_max > thresh_rtosaaaNNrat[1]) {
            flag_rtosa = true; // set flag if difference of band 5 > threshold // (ipix)
        }
        flags = BitSetter.setFlag(flags, 0, flag_rtosa);

        // (9.6.2) test if input tosa spectrum is out of range
        // mima=aa_rtosa_nn_bn7_9(5); // minima and maxima of aaNN input
        double[] mi = rtoa_aaNN7.get().getInmin();
        double[] ma = rtoa_aaNN7.get().getInmax();
        boolean tosa_oor_flag = false; // (ipix)
        // for iv=1:19,// variables
        for (int iv = 0; iv < nn_in.length; iv++) { // variables
            if (nn_in[iv] < mi[iv] || nn_in[iv] > ma[iv]) {
                tosa_oor_flag = true; // (ipix)
            }
        }
        flags = BitSetter.setFlag(flags, 1, tosa_oor_flag);

        // (9.10.1) NN compute IOPs from rw

        // define input to water NNs
        //nn_in_inv=[sun_zeni view_zeni azi_diff_deg temperature salinity log_rw(1:10)];
        double[] nn_in_inv = new double[5 + 10];
        nn_in_inv[0] = sun_zeni;
        nn_in_inv[1] = sensor_zeni;
        nn_in_inv[2] = azi_diff_deg;
        nn_in_inv[3] = temperature;
        nn_in_inv[4] = salinity;
        System.arraycopy(log_rw, 0, nn_in_inv, 5, log_rw.length - 1);
        double[] log_iops_nn1 = rw_IOP.get().calc(nn_in_inv);
        double[] iops_nn1 = a_exp(log_iops_nn1);

        // (9.10.2) test if input tosa spectrum is out of range
        //mima=inv_nn7(5); // minima and maxima of aaNN input
        mi = rw_IOP.get().getInmin();
        ma = rw_IOP.get().getInmax();
        boolean rw_oor_flag = false; // (ipix)
        //for iv=1:15,// variables
        for (int iv = 0; iv < mi.length; iv++) {
            if (nn_in_inv[iv] < mi[iv] | nn_in_inv[iv] > ma[iv]) {
                rw_oor_flag = true; // (ipix)
            }
        }
        flags = BitSetter.setFlag(flags, 2, rw_oor_flag);

// todo (nf): migrate following code to Java
/*
        // (9.14) compute combined IOPs and concentrations
        // split IOPs
        double log_conc_ap_nn1=log_iops_nn1[0];
        double log_conc_ad_nn1=log_iops_nn1[1];
        double log_conc_ag_nn1=log_iops_nn1[2];
        double log_conc_bp_nn1=log_iops_nn1[3];
        double log_conc_bw_nn1=log_iops_nn1[4];

        double ap_a_nn1=exp(log_conc_ap_nn1);
        double ad_a_nn1=exp(log_conc_ad_nn1);
        double ag_a_nn1=exp(log_conc_ag_nn1);
        double bp_a_nn1=exp(log_conc_bp_nn1);
        double bw_a_nn1=exp(log_conc_bw_nn1);

        // combine IOPs
        double adg_a_nn1  = ad_a_nn1+ag_a_nn1;
        double atot_a_nn1 = adg_a_nn1+ap_a_nn1;
        double btot_a_nn1 = bp_a_nn1+bw_a_nn1;

        // compute concentrations
        double chl_a_nn1 = 21.0.*(ap_a_nn1)^(1.04);
        double tsm_a_nn1 = btot_a_nn1*1.73;

        // (9.15) )NN compute uncertainties
        double diff_log_abs_iop=nnhs_ff(unc_biasc_nn1,log_iops_nn1);
        double diff_log_abs_iop_a(ipix,:)=diff_log_abs_iop;
        double unc_iop_rel(ipix,:)=(exp(diff_log_abs_iop)-1).*100;
        double unc_iop_abs(ipix,:)=iop_nn1(ipix,:).*(1.0-exp(-diff_log_abs_iop));

        double unc_abs_chl = 21.0.*unc_iop_abs(ipix,1).^(1.04);

        // (9.16) NN compute uncertainties for combined IOPs and kd
        double[] diff_log_abs_combi_kd=nnhs_ff(unc_biasc_atotkd_nn,log_iops_nn1);
        double diff_log_abs_adg  = diff_log_abs_combi_kd[0];
        double diff_log_abs_atot = diff_log_abs_combi_kd[1];
        double diff_log_abs_btot = diff_log_abs_combi_kd[2];

        double unc_abs_adg   = (1.0-exp(-diff_log_abs_adg))* adg_a_nn1;
        double unc_abs_atot  = (1.0-exp(-diff_log_abs_atot))* atot_a_nn1;
        double unc_abs_btot  = (1.0-exp(-diff_log_abs_btot))* btot_a_nn1;
        double unc_abs_tsm = 1.73.*unc_abs_btot;
*/

        return new Result(rw, iops_nn1, r_tosa, rtosa_aann, rtosa_aaNNrat_min, rtosa_aaNNrat_max, flags);
    }

    private ThreadLocal<NNffbpAlphaTabFast> nnhs(String path) throws IOException {
        String name = "/auxdata/nets/" + path;
        InputStream stream = C2rccModisAlgorithm.class.getResourceAsStream(name);
        if (stream == null) {
            throw new IllegalStateException("resource not found: " + name);
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
