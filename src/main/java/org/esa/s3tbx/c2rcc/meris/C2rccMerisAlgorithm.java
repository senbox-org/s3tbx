package org.esa.s3tbx.c2rcc.meris;

import org.esa.snap.nn.NNffbpAlphaTabFast;
import org.esa.snap.util.BitSetter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static java.lang.Math.PI;
import static java.lang.Math.acos;
import static java.lang.Math.cos;
import static java.lang.Math.log;
import static java.lang.Math.sin;
import static java.lang.Math.toDegrees;
import static java.lang.Math.toRadians;
import static org.esa.s3tbx.ArrayMath.*;

/**
 * @author Roland Doerffer
 * @author Norman Fomferra
 */
public class C2rccMerisAlgorithm {


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


    // gas absorption constants for 12 MERIS channels
    static final double[] absorb_ozon = {
            8.2e-04, 2.82e-03, 2.076e-02, 3.96e-02,
            1.022e-01, 1.059e-01, 5.313e-02, 3.552e-02,
            1.895e-02, 8.38e-03, 7.2e-04, 0.0};

    // polynom coefficients for band708 H2O correction
    static final double[] h2o_cor_poly = {0.3832989, 1.6527957, -1.5635101, 0.5311913};

    static final int[] merband12_ix = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 13};

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
    double[] solflux;

    // (5) thresholds for flags
    double[] thresh_rtosaaaNNrat = {0.95, 1.05};  // threshold for out of scope flag Rtosa has to be adjusted
    double[] thresh_rwslope = {0.95, 1.05};    // threshold for out of scope flag Rw has to be adjusted


    final ThreadLocal<NNffbpAlphaTabFast> inv_nn7;
    final ThreadLocal<NNffbpAlphaTabFast> inv_ac_nn9;
    final ThreadLocal<NNffbpAlphaTabFast> aa_rtosa_nn_bn7_9;

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public void setSalinity(double salinity) {
        this.salinity = salinity;
    }

    public void setSolflux(double[] solflux) {
        this.solflux = solflux;
    }

    public Result processPixel(int px, int py,
                               double lat, double lon,
                               double[] toa_rad,
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
            r_tosa_ur[i] = r_toa[merband12_ix[i] - 1];
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

            double trans_ozoned12 = Math.exp(-(absorb_ozon[i] * ozone / 1000.0 - model_ozone) / cos_sun);
            double trans_ozoneu12 = Math.exp(-(absorb_ozon[i] * ozone / 1000.0 - model_ozone) / cos_view);
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
            alti_press = atm_press * Math.exp(-dem_alt / 8000.0);
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

        double[] log_rw = inv_ac_nn9.get().calc(nn_in);
        double[] rw = a_exp(log_rw);

        // (9.5) test out of scope spectra with autoassociative neural network
        double[] log_rtosa_aann = aa_rtosa_nn_bn7_9.get().calc(nn_in);
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
        double[] mi = aa_rtosa_nn_bn7_9.get().getInmin();
        double[] ma = aa_rtosa_nn_bn7_9.get().getInmax();
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
        nn_in_inv[1] = view_zeni;
        nn_in_inv[2] = azi_diff_deg;
        nn_in_inv[3] = temperature;
        nn_in_inv[4] = salinity;
        System.arraycopy(log_rw, 0, nn_in_inv, 5, 10);
        double[] log_iops_nn1 = inv_nn7.get().calc(nn_in_inv);
        double[] iops_nn1 = a_exp(log_iops_nn1);

        // (9.10.2) test if input tosa spectrum is out of range
        //mima=inv_nn7(5); // minima and maxima of aaNN input
        mi = inv_nn7.get().getInmin();
        ma = inv_nn7.get().getInmax();
        boolean rw_oor_flag = false; // (ipix)
        //for iv=1:15,// variables
        for (int iv = 0; iv < nn_in_inv.length; iv++) {
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

    C2rccMerisAlgorithm() throws IOException {

        // rtosa auto NN
        aa_rtosa_nn_bn7_9 = nnhs("meris/richard_atmo_invers29_press_20150125/rtoa_aaNN7/31x7x31_555.6.net");

        // rtosa-rw NN
        inv_ac_nn9 = nnhs("meris/richard_atmo_invers29_press_20150125/rtoa_rw_nn3/33x73x53x33_470639.6.net");

        // rtosa - rpath NN
        //ThreadLocal<NNffbpAlphaTabFast> rpath_nn9 = nnhs("meris/richard_atmo_invers29_press_20150125/rtoa_rpath_nn2/31x77x57x37_2388.6.net");

        // rtosa - trans NN
        //ThreadLocal<NNffbpAlphaTabFast> inv_trans_nn = nnhs("meris/richard_atmo_invers29_press_20150125/rtoa_trans_nn2/31x77x57x37_37087.4.net");

        // rw-IOP inverse NN
        inv_nn7 = nnhs("meris/coastcolour_wat_20140318/inv_meris_logrw_logiop_20140318_noise_p5_fl/97x77x37_11671.0.net");

        // IOP-rw forward NN
        //ThreadLocal<NNffbpAlphaTabFast> for_nn9b = nnhs("meris/coastcolour_wat_20140318/for_meris_logrw_logiop_20140318_p5_fl/17x97x47_335.3.net"); //only 10 MERIS bands

        // rw-kd NN, output are kdmin and kd449
        //ThreadLocal<NNffbpAlphaTabFast> kd2_nn7 = nnhs("meris/coastcolour_wat_20140318/inv_meris_kd/97x77x7_232.4.net");

        // uncertainty NN for IOPs after bias corretion
        //ThreadLocal<NNffbpAlphaTabFast> unc_biasc_nn1 = nnhs("meris/coastcolour_wat_20140318/uncertain_log_abs_biasc_iop/17x77x37_11486.7.net");

        // uncertainty for atot, adg, btot and kd
        //ThreadLocal<NNffbpAlphaTabFast> unc_biasc_atotkd_nn = nnhs("meris/coastcolour_wat_20140318/uncertain_log_abs_tot_kd/17x77x37_9113.1.net");
    }

    private ThreadLocal<NNffbpAlphaTabFast> nnhs(String path) throws IOException {
        String name = "/auxdata/nets/" + path;
        InputStream stream = C2rccMerisAlgorithm.class.getResourceAsStream(name);
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
