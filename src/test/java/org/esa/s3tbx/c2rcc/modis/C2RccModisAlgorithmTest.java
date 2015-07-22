package org.esa.s3tbx.c2rcc.modis;

import static org.junit.Assert.*;

import org.junit.*;

import java.util.Arrays;


public class C2RccModisAlgorithmTest {
    @Test
    public void testIt() throws Exception {
        C2rccModisAlgorithm algo = new C2rccModisAlgorithm();

        C2rccModisAlgorithm.Result result1 = algo.processPixel(
                    new double[]{
                                0.17401233, 0.14464998, 0.109189205,
                                0.07873539, 0.07067424, 0.041668475,
                                0.040452786, 0.032773286, 0.024377074
                    },
                    64.19979, 158.32169,
                    24.818445, 102.8721,
                    1019.4312, 277.9019);

        assertNotNull(result1);
        assertEquals(9, result1.rw.length);
        assertEquals(5, result1.iops.length);


//        // Line #1 in MER_RR__1PTACR20051013_outfile.txt
//        double[] y1 = {5.415e+01, 7.946e+00, 6.420e+01, 2.482e+01, 5.545e+01 - 3.759e+00,
//                       5.127e+00, 1.019e+03, 2.779e+02, 7.203e+01, 1.500e+01,
//                       3.500e+01, 1.011e-02, 1.208e-02, 1.752e-02, 1.894e-02,
//                       2.064e-02, 7.203e-03, 4.387e-03, 4.018e-03, 2.270e-03,
//                       6.658e-04, 7.215e-04, 2.917e-04, 2.234e-01, 1.818e-01,
//                       1.345e-01, 1.171e-01, 8.844e-02, 6.418e-02, 5.200e-02,
//                       4.859e-02, 4.321e-02, 3.632e-02, 3.321e-02, 2.537e-02,
//                       7.128e-01, 7.609e-01, 8.163e-01, 8.357e-01, 8.715e-01,
//                       9.015e-01, 9.177e-01, 9.226e-01, 9.304e-01, 9.406e-01,
//                       9.454e-01, 9.579e-01, 8.341e-01, 8.682e-01, 9.045e-01,
//                       9.166e-01, 9.377e-01, 9.541e-01, 9.625e-01, 9.650e-01,
//                       9.688e-01, 9.737e-01, 9.759e-01, 9.816e-01, 8.428e-02,
//                       1.421e-01, 5.155e-02, 5.880e-01, 1.328e+00, 1.961e-02,
//                       1.572e-02, 1.291e-02, 2.337e-01, 2.619e-01, 1.603e+00,
//                       3.315e+00, 2.431e-01, 3.131e-01, 3.518e-01, 2.946e-02,
//                       3.199e-02, 1.146e-01, 5.358e-02, 1.983e-01, 1.001e+00,
//                       1.001e+00, 0000};
//
//        double[] rwExpected1 = new double[result1.rw.length];
//        System.arraycopy(y1, 11, rwExpected1, 0, rwExpected1.length);
//        assertArrayEquals(rwExpected1, result1.rw, 1e-4);
//
//        double[] iopsExpected1 = new double[result1.iops.length];
//        System.arraycopy(y1, 59, iopsExpected1, 0, iopsExpected1.length);
//        assertArrayEquals(iopsExpected1, result1.iops, 1e-2);
    }
}