package org.esa.s3tbx.c2rcc.viirs;

import org.esa.snap.core.nn.NNffbpAlphaTabFast;
import org.esa.snap.core.util.BitSetter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;

import static java.lang.Math.*;
import static org.esa.s3tbx.c2rcc.util.ArrayMath.*;

/**
 * @author Roland Doerffer
 * @author Marco Peters
 */
public class C2rccViirsAlgorithm {
    public static final int FLAG_INDEX_RTOSA_OOS = 0;
    public static final int FLAG_INDEX_RTOSA_OOR = 1;
    public static final int FLAG_INDEX_RHOW_OOR = 2;
//    static final int FLAG_INDEX_CLOUD = 3;
//    static final int FLAG_INDEX_IOP_OOR = 4;
//    static final int FLAG_INDEX_APIG_AT_MAX = 5;
//    static final int FLAG_INDEX_ADET_AT_MAX = 6;
//    static final int FLAG_INDEX_AGELB_AT_MAX = 7;
//    static final int FLAG_INDEX_BPART_AT_MAX = 8;
//    static final int FLAG_INDEX_BWIT_AT_MAX = 9;
//    static final int FLAG_INDEX_APIG_AT_MIN = 10;
//    static final int FLAG_INDEX_ADET_AT_MIN = 11;
//    static final int FLAG_INDEX_AGELB_AT_MIN = 12;
//    static final int FLAG_INDEX_BPART_AT_MIN = 13;
//    static final int FLAG_INDEX_BWIT_AT_MIN = 14;
//    static final int FLAG_INDEX_RHOW_OOS = 15;
//    static final int FLAG_INDEX_KD489_OOR = 16;
//    static final int FLAG_INDEX_KDMIN_OOR = 17;
//    static final int FLAG_INDEX_KD489_AT_MAX = 18;
//    static final int FLAG_INDEX_KDMIN_AT_MAX = 19;
    public static final int FLAG_INDEX_VALID_PE = 31;


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

    // gas absorption constants for viirs channels
    // http://oceancolor.gsfc.nasa.gov/SeaWiFS/TECH_REPORTS/PreLPDF/PreLVol9.pdf
    // adapted for 745nm (mp - 2016/12/06)
    private static final double[] absorb_ozon = {
                0.0,      // 410nm
                0.0027,   // 443nm
                0.0205,   // 486nm
                0.0898,   // 551nm
                0.0463,   // 671nm
                0.0095,   // 745nm
                0.0       // 862nm
    };

    static final int[] viirsWavelengths = {410, 443, 486, 551, 671, 745, 862};

    private double salinity = 35.0;
    private double temperature = 15.0;

    // (5) thresholds for flags
    double[] thresh_rtosaaaNNrat = {0.95, 1.05};  // threshold for out of scope flag Rtosa has to be adjusted
    double[] thresh_rwslope = {0.95, 1.05};    // threshold for out of scope flag Rw has to be adjusted


    final ThreadLocal<NNffbpAlphaTabFast> logrw_iop_NN;
    final ThreadLocal<NNffbpAlphaTabFast> rtosa_rw_nn;
    final ThreadLocal<NNffbpAlphaTabFast> aaNN_test_oos_rtosa;

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public void setSalinity(double salinity) {
        this.salinity = salinity;
    }

    public Result processPixel(double[] toa_ref,
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

        //*** (9.3.1) ozone correction ***/
        double model_ozone = 0;

        double[] r_tosa = new double[toa_ref.length];
        double[] log_rtosa = new double[toa_ref.length];
        for (int i = 0; i < toa_ref.length; i++) {

            double trans_ozoned12 = Math.exp(-(absorb_ozon[i] * ozone / 1000.0 - model_ozone) / cos_sun);
            double trans_ozoneu12 = Math.exp(-(absorb_ozon[i] * ozone / 1000.0 - model_ozone) / cos_view);
            double trans_ozone12 = trans_ozoned12 * trans_ozoneu12;

            double r_tosa_oz = toa_ref[i] / trans_ozone12;

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

        double[] log_rw = rtosa_rw_nn.get().calc(nn_in);
        double[] rw = a_exp(log_rw);

        // (9.5) test out of scope spectra with autoassociative neural network
        double[] log_rtosa_aann = aaNN_test_oos_rtosa.get().calc(nn_in);
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
        flags = BitSetter.setFlag(flags, FLAG_INDEX_RTOSA_OOS, flag_rtosa);

        // (9.6.2) test if input tosa spectrum is out of range
        // mima=aa_rtosa_nn_bn7_9(5); // minima and maxima of aaNN input
        double[] mi = aaNN_test_oos_rtosa.get().getInmin();
        double[] ma = aaNN_test_oos_rtosa.get().getInmax();
        boolean tosa_oor_flag = false; // (ipix)
        // for iv=1:19,// variables
        for (int iv = 0; iv < nn_in.length; iv++) { // variables
            if (nn_in[iv] < mi[iv] || nn_in[iv] > ma[iv]) {
                tosa_oor_flag = true; // (ipix)
            }
        }
        flags = BitSetter.setFlag(flags, FLAG_INDEX_RTOSA_OOR, tosa_oor_flag);

        // (9.10.1) NN compute IOPs from rw

        // define input to water NNs
        //nn_in_inv=[sun_zeni view_zeni azi_diff_deg temperature salinity log_rw(412 - 765)];
        final double[] log_rw_412to765 = Arrays.copyOf(log_rw, log_rw.length - 1);
        double[] nn_in_inv = new double[5 + log_rw_412to765.length];
        nn_in_inv[0] = sun_zeni;
        nn_in_inv[1] = view_zeni;
        nn_in_inv[2] = azi_diff_deg;
        nn_in_inv[3] = temperature;
        nn_in_inv[4] = salinity;
        System.arraycopy(log_rw, 0, nn_in_inv, 5, log_rw_412to765.length);
        double[] log_iops_nn1 = logrw_iop_NN.get().calc(nn_in_inv);
        double[] iops_nn1 = a_exp(log_iops_nn1);

        // (9.10.2) test if input tosa spectrum is out of range
        //mima=inv_nn7(5); // minima and maxima of aaNN input
        mi = logrw_iop_NN.get().getInmin();
        ma = logrw_iop_NN.get().getInmax();
        boolean rw_oor_flag = false; // (ipix)
        //for iv=1:15,// variables
        for (int iv = 0; iv < nn_in_inv.length; iv++) {
            if (nn_in_inv[iv] < mi[iv] | nn_in_inv[iv] > ma[iv]) {
                rw_oor_flag = true; // (ipix)
            }
        }
        flags = BitSetter.setFlag(flags, FLAG_INDEX_RHOW_OOR, rw_oor_flag);

        return new Result(rw, iops_nn1, r_tosa, rtosa_aann, rtosa_aaNNrat_min, rtosa_aaNNrat_max, flags);
    }

    C2rccViirsAlgorithm() throws IOException {
        aaNN_test_oos_rtosa = nnhs("viirs/coastcolour_atmo_press_20150221/rtoa_viirs_aaNN7/31x7x31_228.7.net");
        rtosa_rw_nn = nnhs("viirs/coastcolour_atmo_press_20150221/rtoa_rw_viirs_nn3/33x73x53x33_420666.6.net");
        logrw_iop_NN = nnhs("viirs/coastcolour_wat_20140318/inv_viirs_logrw_logiop_20140318_noise_p5/87x77x37_15389.9.net");
    }

    private ThreadLocal<NNffbpAlphaTabFast> nnhs(String path) throws IOException {
        String name = "/auxdata/nets/" + path;
        InputStream stream = C2rccViirsAlgorithm.class.getResourceAsStream(name);
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
