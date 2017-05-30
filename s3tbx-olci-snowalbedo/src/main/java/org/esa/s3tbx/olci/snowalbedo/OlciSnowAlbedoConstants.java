package org.esa.s3tbx.olci.snowalbedo;

/**
 * todo: add comment
 * To change this template use File | Settings | File Templates.
 * Date: 29.05.2017
 * Time: 18:11
 *
 * @author olafd
 */
public class OlciSnowAlbedoConstants {

    public static final double[] WAVELENGTH_GRID = {
            0.400,         // OA01
            0.4125,        // OA02
            0.4425,        // OA03
            0.490,         // OA04
            0.510,         // OA05
            0.560,         // OA06
            0.620,         // OA07
            0.665,         // OA08
            0.68125,       // OA10
            0.70875,       // OA11
            0.77875,       // OA16
            0.865,         // OA17
            0.885,         // OA18
            1.02           // OA21
    };

    public static final int[] WAVELENGTH_INDICES = {
            0, 1, 2, 3, 4, 5, 6, 7, 9, 10, 15, 16, 17, 20
    };

    public static final double[] F_LAMBDA = {
            // todo: these are the values 'closest' to wavelength grid. For better precision interpolate using table
            // A.2 from AK TN 'algorithm__BROADBAND_SPHERICAL_ALBEDO.docx' if necessary
            0.01035, 0.01172, 0.01445, 0.02076, 0.022, 0.02499, 0.02783,
            0.01858, 0.01898, 0.01936, 0.0207, 0.02134, 0.01408, 0.00884
    };
}
