package org.esa.s3tbx.c2rcc.modis;

import static java.lang.Math.cos;
import static java.lang.Math.exp;
import static java.lang.Math.log;
import static java.lang.Math.sin;
import static java.lang.Math.toDegrees;
import static java.lang.Math.toRadians;

import org.esa.snap.nn.NNffbpAlphaTabFast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

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

    /**
     * Structure for returning the algorithm's result.
     */
    public static class Result {

        public final double[] rw;
        public final double[] iops;

        public Result(double[] rw, double[] iops) {
            this.rw = rw;
            this.iops = iops;
        }
    }

//    static final int[] modband12_ix = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 13};

    private double salinity = salinity_default;
    private double temperature = temperature_default;

    private ThreadLocal<NNffbpAlphaTabFast> rtoa_rw_nn3;
    private ThreadLocal<NNffbpAlphaTabFast> rw_IOP;

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public void setSalinity(double salinity) {
        this.salinity = salinity;
    }

    public C2rccModisAlgorithm() throws IOException {
        rtoa_rw_nn3 = nnhs("modis/rtoa_rw_modis_nn3/33x73x53x33_508087.3.net");
        rw_IOP = nnhs("modis/inv_modis_logrw_logiop_20131210_noise_1/97x77x37_5374.6.net");
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

        double[] log_rtosa = new double[toa_ref.length];
        for (int i = 0; i < toa_ref.length; i++) {
            double trans_ozoned12 = exp(-(k_oz_per_wl[i] * ozone / 1000.0) / cos_sun_zen);
            double trans_ozoneu12 = exp(-(k_oz_per_wl[i] * ozone / 1000.0) / cos_sensor_zen);
            double trans_ozone12 = trans_ozoned12 * trans_ozoneu12;
            log_rtosa[i] = log(toa_ref[i] / trans_ozone12);
        }

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
        double[] rw = new double[log_rw.length];
        for (int i = 0; i < rw.length; i++) {
            rw[i] = exp(log_rw[i]);
        }

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
        double[] iops_nn1 = new double[5];
        for (int i = 0; i < iops_nn1.length; i++) {
            iops_nn1[i] = exp(log_iops_nn1[i]);
        }

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

        return new Result(rw, iops_nn1);
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
