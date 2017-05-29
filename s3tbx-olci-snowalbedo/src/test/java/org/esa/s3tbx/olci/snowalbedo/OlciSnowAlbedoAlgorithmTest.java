package org.esa.s3tbx.olci.snowalbedo;

/**
 * todo: add comment
 * To change this template use File | Settings | File Templates.
 * Date: 29.05.2017
 * Time: 17:29
 *
 * @author olafd
 */

import org.esa.s3tbx.processor.rad2refl.Rad2ReflConstants;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;


public class OlciSnowAlbedoAlgorithmTest {

    @Test
    public void testComputePlanarBroadbandAlbedo() throws Exception {
        // // TODO: 29.05.2017

        // 0.998        400     OLCI
//        0.998         412
//        0.998         442
//        0.996         490
//        0.993         510
//        0.990          560
//        0.984          619
//        0.975          664
        // 0.97         673     OLCI
//        0.964          681
//        0.961          709
//        0.950          753
//        0.920          760
        // 0.91         764     OLCI
        // 0.9          767     OLCI
//        0.890          779
//        0.860          865
//        0.712          885
        // 0.7          900
        // 0.65         940
        // 0.6          1020

//        public final static float[] MERIS_WAVELENGTHS = {
//                0.f, 412.f, 442.f, 490.f, 510.f,
//                560.f, 619.f, 664.f, 681.f, 709.f,
//                753.f, 760.f, 779.f, 865.f, 885.f, 900.f
//        };


//        public final static float[] OLCI_WAVELENGHTS = {
//                400.0f, 412.5f, 442.5f, 490.0f, 510.0f,
//                560.0f, 620.0f, 665.0f, 673.75f, 681.25f,
//                708.75f, 753.75f, 761.25f, 764.375f, 767.5f,
//                778.75f, 865.0f, 885.0f, 900.0f, 940.0f, 1020.0f
//        };
//
        double[] spectralAlbedos = new double[] {
                0.998, 0.998, 0.998, 0.996, 0.993, 0.99, 0.984, 0.975, 0.97, 0.964,0.961,
                0.95, 0.92, 0.91, 0.9, 0.89, 0.86, 0.712, 0.7, 0.65, 0.6
        };
        float[] solarFluxes = Rad2ReflConstants.OLCI_SOLAR_FLUXES_DEFAULT;
        double r21 = 0;
        double sza = 0;
        final double albedo = OlciSnowAlbedoAlgorithm.computePlanarBroadbandAlbedo(spectralAlbedos, solarFluxes, r21, sza);
    }
}
