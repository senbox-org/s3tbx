package org.esa.s3tbx.idepix.algorithms.olci;

import org.esa.s3tbx.idepix.core.IdepixFlagCoding;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.math.MathUtils;

/**
 * Utility class for Idepix OLCI
 *
 * @author olafd
 */

public class OlciUtils {

    /**
     * Provides OLCI pixel classification flag coding
     *
     * @param flagId - the flag ID
     *
     * @return - the flag coding
     */
    public static FlagCoding createOlciFlagCoding(String flagId) {
        return IdepixFlagCoding.createDefaultFlagCoding(flagId);
    }

    /**
     * Provides OLCI pixel classification flag bitmask
     *
     * @param classifProduct - the pixel classification product
     */
    public static void setupOlciClassifBitmask(Product classifProduct) {
        IdepixFlagCoding.setupDefaultClassifBitmask(classifProduct);
    }

    /**
     * Provides a special cloud flag using FUB O2 corrected band 13. Potentially useful for cloud/snow distinction.
     *
     *
     * @param cameraBounds
     * @param x - x pixel coordinate
     * @param trans13 - corrected transmission from O2 product
     * @param altitude - altitude
     * @param sza - SZA
     * @param oza - OZA
     * @param isBright - brightness flag from quality flags
     *
     * @return boolean
     */
    public static boolean isO2Cloud(int[] cameraBounds, int x, double trans13, double altitude, double sza, double oza, boolean isBright) {
        final double airmass =
                1./Math.cos(sza* MathUtils.DTOR) + 1./Math.cos(oza* MathUtils.DTOR);
        final double trans13Baseline =
                0.0000000053*altitude*altitude+0.000021002793*altitude+0.225247877059;
        double trans13BaselineAMFcorr13 = 0.0;
        if (x >= 0 && x <= cameraBounds[0]) {
            trans13BaselineAMFcorr13 = trans13Baseline/(0.3245 * airmass);
        } else if (x > cameraBounds[0] && x <= cameraBounds[1]) {
            trans13BaselineAMFcorr13 = trans13Baseline/(0.3319 * airmass);
        } else if (x > cameraBounds[1] && x <= cameraBounds[2]) {
            trans13BaselineAMFcorr13 = trans13Baseline/(0.3274 * airmass);
        } else if (x > cameraBounds[2] && x <= cameraBounds[3]) {
            trans13BaselineAMFcorr13 = trans13Baseline/(0.3345 * airmass);
        } else if (x > cameraBounds[3] && x < cameraBounds[4]) {
            trans13BaselineAMFcorr13 = trans13Baseline/(0.3503 * airmass);
        }
        final double trans13Excess = trans13 - trans13BaselineAMFcorr13;
        return isBright && trans13Excess > 0.023;
    }

}
