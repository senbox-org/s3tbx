package org.esa.s3tbx.idepix.algorithms.olci;

import org.esa.snap.core.util.math.MathUtils;

/**
 * Holder for O2 correction terms
 *
 * @author olafd
 */
public class O2Correction {
    private double trans13Baseline;
    private double trans13BaselineAmfCorr;
    private double trans13Excess;
    private boolean isO2Cloud;

    public O2Correction(double trans13Baseline, double trans13BaselineAmfCorr, double trans13Excess, boolean isO2Cloud) {
        this.trans13Baseline = trans13Baseline;
        this.trans13BaselineAmfCorr = trans13BaselineAmfCorr;
        this.trans13Excess = trans13Excess;
        this.isO2Cloud = isO2Cloud;
    }

    /**
     * Provides O2 correction terms for given input. Potentially useful for cloud/snow distinction.
     *
     * @param cameraBounds - x coordinates of camera bounds (i.e. different for OLCI FR/RR)
     * @param x
     * @param trans13 - band 13 corrected transmission (from FUB algorithm)
     * @param altitude - altitude from OLCI L1b
     * @param sza - SZA from OLCI L1b
     * @param oza - OZA from OLCI L1b
     * @param isBright - brightness flag from OLCI L1b
     *
     * @return O2Correction terms
     */
    public static O2Correction computeO2CorrectionTerms(int[] cameraBounds, int x, double trans13, double altitude,
                                                        double sza, double oza, boolean isBright) {
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
        final boolean isO2Cloud = isBright && trans13Excess > 0.023;

        return new O2Correction(trans13Baseline, trans13BaselineAMFcorr13, trans13Excess, isO2Cloud);
    }

    public double getTrans13Baseline() {
        return trans13Baseline;
    }

    public void setTrans13Baseline(double trans13Baseline) {
        this.trans13Baseline = trans13Baseline;
    }

    public double getTrans13BaselineAmfCorr() {
        return trans13BaselineAmfCorr;
    }

    public void setTrans13BaselineAmfCorr(double trans13BaselineAmfCorr) {
        this.trans13BaselineAmfCorr = trans13BaselineAmfCorr;
    }

    public double getTrans13Excess() {
        return trans13Excess;
    }

    public void setTrans13Excess(double trans13Excess) {
        this.trans13Excess = trans13Excess;
    }

    public boolean isO2Cloud() {
        return isO2Cloud;
    }

    public void setO2Cloud(boolean o2Cloud) {
        isO2Cloud = o2Cloud;
    }
}
