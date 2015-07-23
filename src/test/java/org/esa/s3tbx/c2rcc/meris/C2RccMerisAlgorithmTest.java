package org.esa.s3tbx.c2rcc.meris;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


public class C2RccMerisAlgorithmTest {
    @Test
    public void testIt() throws Exception {
        C2rccMerisAlgorithm algo = new C2rccMerisAlgorithm();


        C2rccMerisAlgorithm.Result result1 = algo.processPixel(
                250, 575, 7.9456024, 54.150196,
                new double[]{55.086716, 49.46522, 38.112446, 33.45525, 23.108776, 14.337405, 11.306171, 10.365329, 8.529731, 6.4291587, 2.262602, 5.485246, 3.394396, 3.1312065, 2.291696},
                64.19979, 158.32169, 24.818445, 102.8721, -34.242188, 1019.4312, 277.9019);

        assertNotNull(result1);
        assertEquals(12, result1.rw.length);
        assertEquals(5, result1.iops.length);


        // Line #1 in MER_RR__1PTACR20051013_outfile.txt
        double[] y1 = {5.415e+01, 7.946e+00, 6.420e+01, 2.482e+01, 5.545e+01 - 3.759e+00, 5.127e+00, 1.019e+03, 2.779e+02, 7.203e+01, 1.500e+01, 3.500e+01, 1.011e-02, 1.208e-02, 1.752e-02, 1.894e-02, 2.064e-02, 7.203e-03, 4.387e-03, 4.018e-03, 2.270e-03, 6.658e-04, 7.215e-04, 2.917e-04, 2.234e-01, 1.818e-01, 1.345e-01, 1.171e-01, 8.844e-02, 6.418e-02, 5.200e-02, 4.859e-02, 4.321e-02, 3.632e-02, 3.321e-02, 2.537e-02, 7.128e-01, 7.609e-01, 8.163e-01, 8.357e-01, 8.715e-01, 9.015e-01, 9.177e-01, 9.226e-01, 9.304e-01, 9.406e-01, 9.454e-01, 9.579e-01, 8.341e-01, 8.682e-01, 9.045e-01, 9.166e-01, 9.377e-01, 9.541e-01, 9.625e-01, 9.650e-01, 9.688e-01, 9.737e-01, 9.759e-01, 9.816e-01, 8.428e-02, 1.421e-01, 5.155e-02, 5.880e-01, 1.328e+00, 1.961e-02, 1.572e-02, 1.291e-02, 2.337e-01, 2.619e-01, 1.603e+00, 3.315e+00, 2.431e-01, 3.131e-01, 3.518e-01, 2.946e-02, 3.199e-02, 1.146e-01, 5.358e-02, 1.983e-01, 1.001e+00, 1.001e+00, 0000};

        double[] rwExpected1 = new double[result1.rw.length];
        System.arraycopy(y1, 11, rwExpected1, 0, rwExpected1.length);
        assertArrayEquals(rwExpected1, result1.rw, 1e-4);

        double[] iopsExpected1 = new double[result1.iops.length];
        System.arraycopy(y1, 59, iopsExpected1, 0, iopsExpected1.length);
        assertArrayEquals(iopsExpected1, result1.iops, 1e-2);

        assertEquals(0, result1.flags);

        C2rccMerisAlgorithm.Result result32 = algo.processPixel(278, 583, 8.346703, 54.009,
                                                 new double[]{53.599163, 48.36246, 38.01977, 34.09759, 25.049278, 15.639317, 12.230661, 11.190948, 9.002904, 6.3771706, 2.244856, 5.44892, 3.3233092, 3.0518582, 2.2971265},
                                                 63.981014, // sun_zenith
                                                 158.73405, //sun_azimuth
                                                 22.776539, // view_zenith
                                                 103.210495, // view_azimuth
                                                 -24.90625, // dem_alt
                                                 1019.6313, // atm_press
                                                 278.57166); // ozone

        // Line #32 in MER_RR__1PTACR20051013_outfile.txt
        double[] y32 = {5.401e+01, 8.347e+00, 6.398e+01, 2.278e+01, 5.552e+01 - 3.504e+00, 4.225e+00, 1.020e+03, 2.786e+02, 6.827e+01, 1.500e+01, 3.500e+01, 7.122e-03, 9.572e-03, 1.593e-02, 1.986e-02, 2.878e-02, 1.599e-02, 1.044e-02, 9.942e-03, 5.875e-03, 1.698e-03, 1.866e-03, 7.468e-04, 2.151e-01, 1.742e-01, 1.289e-01, 1.120e-01, 8.453e-02, 6.138e-02, 4.968e-02, 4.635e-02, 4.117e-02, 3.454e-02, 3.160e-02, 2.417e-02, 7.175e-01, 7.659e-01, 8.215e-01, 8.408e-01, 8.766e-01, 9.062e-01, 9.220e-01, 9.268e-01, 9.343e-01, 9.440e-01, 9.486e-01, 9.602e-01, 8.368e-01, 8.708e-01, 9.072e-01, 9.192e-01, 9.402e-01, 9.565e-01, 9.646e-01, 9.671e-01, 9.708e-01, 9.754e-01, 9.775e-01, 9.827e-01, 2.457e-01, 3.942e-01, 2.611e-01, 1.583e+00, 2.452e+00, 4.947e-02, 3.486e-02, 6.146e-02, 7.243e-01, 7.175e-01, 4.879e+00, 6.980e+00, 4.882e-01, 8.247e-01, 9.212e-01, 1.601e-01, 2.671e-01, 8.624e-01, 1.225e-01, 1.492e+00, 1.001e+00, 1.001e+00, 0010};

        double[] rwExpected32 = new double[result32.rw.length];
        System.arraycopy(y32, 11, rwExpected32, 0, rwExpected32.length);
        assertArrayEquals(rwExpected32, result32.rw, 1e-4);

        double[] iopsExpected32 = new double[result32.iops.length];
        System.arraycopy(y32, 59, iopsExpected32, 0, iopsExpected32.length);
        assertArrayEquals(iopsExpected32, result32.iops, 1e-1);

        assertEquals(0, result32.flags);

        System.out.println("iopsExpected32 = " + Arrays.toString(iopsExpected32));
        System.out.println("result32 = " + Arrays.toString(result32.iops));
        for (int i = 0; i < iopsExpected32.length; i++) {
            double expected = iopsExpected32[i];
            double actual = result32.iops[i];
            double error = (expected - actual) / expected;
            System.out.printf("iops["+i+"].error = %s%%%n", 100 * Math.round(1000 * error) / 1000.);
        }

        /*
        int irw = -1;
        int iiop = -1;
        double eps = 1e-4;
        for (int i = 0; i < y.length; i++) {
            double v = y[i];
            if (irw == -1 && Math.abs(v - result.rw[0]) < eps) {
                irw = i;
            }
            if (iiop == -1 && Math.abs(v - result.iops[0]) < eps) {
                iiop = i;
            }
        }
        System.out.println("irw  = " + irw);
        System.out.println("iiop = " + iiop);
        */

    }
}