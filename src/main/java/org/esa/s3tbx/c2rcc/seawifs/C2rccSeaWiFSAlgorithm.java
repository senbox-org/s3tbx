package org.esa.s3tbx.c2rcc.seawifs;

import static java.lang.Math.PI;
import static java.lang.Math.acos;
import static java.lang.Math.cos;
import static java.lang.Math.log;
import static java.lang.Math.sin;
import static java.lang.Math.toDegrees;
import static java.lang.Math.toRadians;
import static org.esa.s3tbx.ArrayMath.a_div;
import static org.esa.s3tbx.ArrayMath.a_exp;
import static org.esa.s3tbx.ArrayMath.a_max;
import static org.esa.s3tbx.ArrayMath.a_min;
import static org.esa.s3tbx.ArrayMath.a_mul;

import org.esa.snap.framework.gpf.OperatorException;
import org.esa.snap.nn.NNffbpAlphaTabFast;
import org.esa.snap.util.BitSetter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;

/**
 * @author Roland Doerffer
 * @author Norman Fomferra
 * @author Sabine Embacher
 */
public class C2rccSeaWiFSAlgorithm {

    public final static double salinity_default = 35.0;
    public final static double temperature_default = 15.0;
    public final static double pressure_default = 1000.0;
    public final static double ozone_default = 330.0;
    private Thread thread;

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

    // gas absorption constants for Seawifs channels
    // http://oceancolor.gsfc.nasa.gov/SeaWiFS/TECH_REPORTS/PreLPDF/PreLVol9.pdf
    static final double[] absorb_ozon = {
                0.0,      // 412nm
                0.0027,   // 443nm
                0.0205,   // 490nm
                0.0382,   // 510nm
                0.0898,   // 555nm
                0.0463,   // 670nm
                0.0083,   // 765nm
                0.0       // 865nm
    };

    static final int[] seawifsWavelengths = {412, 443, 490, 510, 555, 670, 765, 865};

    // default nasa solar flux from waterradiance project
    // derived from cahalan table from Kerstin tb 2013-11-22
    private final static double[] DEFAULT_SOLAR_FLUX = new double[]{
                1735.518167,   // 412nm
                1858.404314,   // 443nm
                1981.076667,   // 490nm
                1881.566829,   // 510nm
                1874.005,      // 555nm
                1537.254783,   // 670nm
                1230.04,       // 765nm
                957.6122143    // 865nm
    };

    public static double[] getDefaultSolarFlux() {
        return Arrays.copyOf(DEFAULT_SOLAR_FLUX, DEFAULT_SOLAR_FLUX.length);
    }

    double salinity = salinity_default;
    double temperature = temperature_default;
    double[] correctedSolarFlux;

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

    public void setCorrectedSolarFlux(double[] correctedSolarFlux) {
        this.correctedSolarFlux = correctedSolarFlux;
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

        final Thread thread = Thread.currentThread();
        if (this.thread == null) {
            this.thread = thread;
        }
        if (this.thread != thread) {
            throw new OperatorException("Kotz!!!!");
        }

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

        toa_rad = a_mul(toa_rad, 10.0);
        double[] ref_toa = new double[toa_rad.length];
        for (int i = 0; i < toa_rad.length; i++) {
            ref_toa[i] = PI * toa_rad[i] / correctedSolarFlux[i] / cos_sun;
        }

        //*** (9.3.1) ozone correction ***/
        double model_ozone = 0;

        double[] r_tosa = new double[ref_toa.length];
        double[] log_rtosa = new double[ref_toa.length];
        for (int i = 0; i < ref_toa.length; i++) {

            double trans_ozoned12 = Math.exp(-(absorb_ozon[i] * ozone / 1000.0 - model_ozone) / cos_sun);
            double trans_ozoneu12 = Math.exp(-(absorb_ozon[i] * ozone / 1000.0 - model_ozone) / cos_view);
            double trans_ozone12 = trans_ozoned12 * trans_ozoneu12;

            double r_tosa_oz = ref_toa[i] / trans_ozone12;

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
        flags = BitSetter.setFlag(flags, 0, flag_rtosa);

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
        flags = BitSetter.setFlag(flags, 1, tosa_oor_flag);

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
        flags = BitSetter.setFlag(flags, 2, rw_oor_flag);

        return new Result(rw, iops_nn1, r_tosa, rtosa_aann, rtosa_aaNNrat_min, rtosa_aaNNrat_max, flags);
    }

    C2rccSeaWiFSAlgorithm() throws IOException {
        aaNN_test_oos_rtosa = nnhs("seawifs/coastcolour_atmo_press_20150221/rtoa_seaw_aaNN7/31x7x31_215.9.net");
        rtosa_rw_nn = nnhs("seawifs/coastcolour_atmo_press_20150221/rtoa_rw_seaw_nn3/33x73x53x33_515179.0.net");
        logrw_iop_NN = nnhs("seawifs/coastcolour_wat_20140318/inv_seawifs_logrw_logiop_20140318_noise_p5/87x77x37_14386.6.net");
    }

    private ThreadLocal<NNffbpAlphaTabFast> nnhs(String path) throws IOException {
        String name = "/auxdata/nets/" + path;
        InputStream stream = C2rccSeaWiFSAlgorithm.class.getResourceAsStream(name);
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
