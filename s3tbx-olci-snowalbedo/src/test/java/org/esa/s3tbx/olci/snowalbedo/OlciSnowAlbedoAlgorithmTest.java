package org.esa.s3tbx.olci.snowalbedo;

import org.junit.Test;

import static junit.framework.TestCase.assertEquals;


public class OlciSnowAlbedoAlgorithmTest {

    @Test
    public void testComputeBroadbandAlbedos() throws Exception {
        double[] spectralAlbedos = new double[] {
                0.998, 0.998, 0.996, 0.993, 0.99, 0.984, 0.975, 0.964, 0.961,
                0.95, 0.92, 0.89, 0.86, 0.712
        };
        double r21 = 0.71233;     // theoretical brr_21 for snow of 200mu diameter
        double sza = 50.0;
        final OlciSnowAlbedoAlgorithm.SphericalBroadbandAlbedo sbbaTerms =
                OlciSnowAlbedoAlgorithm.computeSphericalBroadbandAlbedoTerms(spectralAlbedos, r21);
        final double sphericalBroadbandAlbedo = sbbaTerms.getR_b1() + sbbaTerms.getR_b2();
        assertEquals(0.8385, sphericalBroadbandAlbedo, 1.E-2);
        final double broadbandPlanarAlbedo =
                OlciSnowAlbedoAlgorithm.computePlanarBroadbandAlbedo(sphericalBroadbandAlbedo, sza);
        assertEquals(0.8416, broadbandPlanarAlbedo, 1.E-2);
    }

    @Test
    public void testComputeGrainDiameter() throws Exception {
        final double grainDiameter = OlciSnowAlbedoAlgorithm.computeGrainDiameter(0.71233);
        assertEquals(316.765, grainDiameter, 1.E-2);
    }

    @Test
    public void testComputeSpectralAlbedo() throws Exception {
        double brr = 0.71233;
        double sza = 55.5;
        double vza = 31.55;
        double saa = 154.28;
        double vaa = 103.75;
        final double spectralAlbedo = OlciSnowAlbedoAlgorithm.computeSpectralAlbedo(brr, sza, vza, saa, vaa);
        assertEquals(0.7244, spectralAlbedo, 1.E-2);
    }

    @Test
    public void testIntegrateR_b1() throws Exception {
        double[] spectralAlbedos = new double[] {
                0.998, 0.998, 0.996, 0.993, 0.99, 0.984, 0.975, 0.964, 0.961,
                0.95, 0.92, 0.89, 0.86, 0.712
        };
        double r_b1 = OlciSnowAlbedoAlgorithm.integrateR_b1(spectralAlbedos);
        assertEquals(0.7552, r_b1, 1.E-2);
    }

    @Test
    public void testIntegrateR_b2() throws Exception {

        double r_b2 = OlciSnowAlbedoAlgorithm.integrateR_b2(200.0);
        assertEquals(0.0947, r_b2, 1.E-2);

        r_b2 = OlciSnowAlbedoAlgorithm.integrateR_b2(50.0);
        assertEquals(0.1289, r_b2, 1.E-2);

        r_b2 = OlciSnowAlbedoAlgorithm.integrateR_b2(100.0);
        assertEquals(0.1118, r_b2, 1.E-2);

        r_b2 = OlciSnowAlbedoAlgorithm.integrateR_b2(400.0);
        assertEquals(0.077, r_b2, 1.E-2);

        r_b2 = OlciSnowAlbedoAlgorithm.integrateR_b2(800.0);
        assertEquals(0.0604, r_b2, 1.E-2);
    }
}
