package org.esa.s3tbx.mphchl;

import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.pointop.WritableSample;
import org.esa.snap.core.util.BitSetter;

import java.awt.Color;

/**
 * Utility class for MPH CHL retrieval
 *
 * @author olafd
 */
public class MphChlUtils {

    private static final String CYANO_FLAG_NAME = "mph_cyano";
    private static final String CYANO_FLAG_DESCRIPTION = "Cyanobacteria dominated waters";
    private static final String FLOATING_FLAG_NAME = "mph_floating";
    private static final String FLOATING_FLAG_DESCRIPTION = "Floating vegetation or cyanobacteria on water surface";
    private static final String ADJACENCY_FLAG_NAME = "mph_adjacency";
    private static final String ADJACENCY_FLAG_DESCRIPTION = "Pixel suspect of adjacency effects";

    /**
     * Provides MPH CHL flag coding
     *
     * @param flagId - the flag ID
     * @return - the flag coding
     */
    public static FlagCoding createMphChlFlagCoding(String flagId) {
        FlagCoding flagCoding = new FlagCoding(flagId);

        flagCoding.addFlag(CYANO_FLAG_NAME, BitSetter.setFlag(0, 0), CYANO_FLAG_DESCRIPTION);
        flagCoding.addFlag(FLOATING_FLAG_NAME, BitSetter.setFlag(0, 1), FLOATING_FLAG_DESCRIPTION);
        flagCoding.addFlag(ADJACENCY_FLAG_NAME, BitSetter.setFlag(0, 2), ADJACENCY_FLAG_DESCRIPTION);

        return flagCoding;
    }

    /**
     * Provides MPH CHL flag bitmask
     *
     * @param mphChlProduct - the MPH CHL product
     */
    public static void setupMphChlBitmask(Product mphChlProduct) {
        int index = 0;

        int w = mphChlProduct.getSceneRasterWidth();
        int h = mphChlProduct.getSceneRasterHeight();
        Mask mask;

        mask = Mask.BandMathsType.create(CYANO_FLAG_NAME, CYANO_FLAG_DESCRIPTION, w, h,
                "mph_chl_flags." + CYANO_FLAG_NAME,
                Color.cyan, 0.5f);
        mphChlProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create(FLOATING_FLAG_NAME, FLOATING_FLAG_DESCRIPTION, w, h,
                "mph_chl_flags." + FLOATING_FLAG_NAME,
                Color.green, 0.5f);
        mphChlProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create(ADJACENCY_FLAG_NAME, ADJACENCY_FLAG_DESCRIPTION, w, h,
                "mph_chl_flags." + ADJACENCY_FLAG_NAME,
                Color.red, 0.5f);
        mphChlProduct.getMaskGroup().add(index, mask);
    }

    public static int encodeFlags(boolean cyano_flag, boolean floating_flag, boolean adj_flag) {
        int flag = 0;
        if (cyano_flag) {
            flag = flag | 0x1;
        }
        if (floating_flag) {
            flag = flag | 0x2;
        }
        if (adj_flag) {
            flag = flag | 0x4;
        }
        return flag;
    }

    public static double computeChlExponential(double mph) {
        final double exponent = 35.79 * mph;

        return 22.44 * Math.exp(exponent);
    }

    public static double computeChlPolynomial(double mph) {
        final double mph_sq = mph * mph;
        final double mph_p3 = mph_sq * mph;
        final double mph_p4 = mph_sq * mph_sq;
        final double mph_p5 = mph_p4 * mph;
        final double mph_p6 = mph_p4 * mph_sq;

        // return 5.2392E9 * mph_p4 - 1.9524E8 * mph_p3 + 2.4649E6 * mph_sq + 4.0172E3 * mph + 1.9726;
        // updated algorithm:
        return 8.272E12 * mph_p6 - 5.812E11 * mph_p5 + 1.572E10 * mph_p4 - 2.132E8 * mph_p3 + 1.496E6 * mph_sq + 1268 * mph + 5.189;
    }

    public static double computeChlMatthewsPolynomial(double mph) {
        final double mph_sq = mph * mph;
        final double mph_p3 = mph_sq * mph;
        final double mph_p4 = mph_sq * mph_sq;

        return 5.2392E9 * mph_p4 - 1.9524E8 * mph_p3 + 2.4649E6 * mph_sq + 4.0172E3 * mph + 1.9726;
    }

    public static boolean isCyano(double SICF_peak, double SIPF_peak, double BAIR_peak) {
        return SICF_peak < 0.0 && SIPF_peak > 0.0 && BAIR_peak > 0.002;
    }

    public static boolean isCyano(double SICF_peak, double SIPF_peak) {
        return SICF_peak < 0.0 && SIPF_peak > 0.0;
    }

    public static void setToInvalid(WritableSample[] targetSamples, boolean exportMph, boolean exportAddBands) {
        targetSamples[0].set(Double.NaN);
        targetSamples[1].set(0.0);  // mph_chl_flag
        targetSamples[2].set(0.0);  // immersed cyanobacteria
        targetSamples[3].set(0.0);  // floating cyanobacteria
        targetSamples[4].set(0.0);  // floating vegetation
        if (exportMph) {
            targetSamples[5].set(Double.NaN);
        }
        if (exportAddBands) {
            targetSamples[5].set(Double.NaN);
            targetSamples[7].set(Double.NaN);
            targetSamples[8].set(Double.NaN);
        }
    }

    public static double computeMph(double rBr_Max, double r_7, double r_14, double wl_max, double wl_7, double wl_14) {
        return rBr_Max - r_7 - ((r_14 - r_7) * (wl_max - wl_7) / (wl_14 - wl_7));
    }

    public static double computePci(double r_5, double r_6, double r_7, double wl_5, double wl_6, double wl_7) {
        return -(r_6 - r_5 - (wl_6 - wl_5) / (wl_7 - wl_5) * (r_7 - r_5));
    }

    public static double computeChlPitarch(double mph) {
        final double mph_sq = mph * mph;
        final double mph_p3 = mph_sq * mph;

        return 848468 * mph_p3 - 72058 * mph_sq + 5515.7 * mph;
    }

    public static double computeChlPciPitarch(double mph, double pci) {
        final double mph_sq = mph * mph;
        final double mph_p3 = mph_sq * mph;
        final double pci_sq = pci * Math.abs(pci);

        return 490947 * mph_p3 - 611074 * pci_sq + 3872.9 * mph;
    }
}
