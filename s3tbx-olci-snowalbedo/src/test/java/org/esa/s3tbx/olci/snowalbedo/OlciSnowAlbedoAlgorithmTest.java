package org.esa.s3tbx.olci.snowalbedo;

/**
 * todo: add comment
 * To change this template use File | Settings | File Templates.
 * Date: 29.05.2017
 * Time: 17:29
 *
 * @author olafd
 */

import org.junit.Test;


public class OlciSnowAlbedoAlgorithmTest {

    @Test
    public void testComputePlanarBroadbandAlbedo() throws Exception {
        // // TODO: 29.05.2017

        double[] spectralAlbedos = new double[] {
                0.998, 0.998, 0.998, 0.996, 0.993, 0.99, 0.984, 0.975, 0.97, 0.964,0.961,
                0.95, 0.92, 0.91
        };
        double r21 = 0;
        double sza = 30.0;
        final double broadbandSphericalAlbedo =
                OlciSnowAlbedoAlgorithm.computeSphericalBroadbandAlbedo(spectralAlbedos, r21);
        final double broadbandPlanarAlbedo =
                OlciSnowAlbedoAlgorithm.computePlanarBroadbandAlbedo(broadbandSphericalAlbedo, sza);
    }
}
