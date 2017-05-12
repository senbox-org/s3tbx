package org.esa.s3tbx.mphchl;

import org.esa.snap.dataio.envisat.EnvisatConstants;

/**
 * Constants for MPH CHL retrieval
 *
 * @author olafd
 */
public class MphChlConstants {

    public final static String RAD_UNIT = "mw.m-2.sr-1.nm-1";
    public final static String REFL_UNIT = "dl";


    /////// MERIS ////////

    public final static float[] MERIS_WAVELENGTHS = {
            0.f, 412.f, 442.f, 490.f, 510.f,
            560.f, 619.f, 664.f, 681.f, 709.f,
            753.f, 760.f, 779.f, 865.f, 885.f, 900.f
    };

    public final static int MERIS_NUM_SPECTRAL_BANDS = EnvisatConstants.MERIS_L1B_NUM_SPECTRAL_BANDS;

    public final static String[] MERIS_RAD_BAND_NAMES_3RD = EnvisatConstants.MERIS_L1B_SPECTRAL_BAND_NAMES;

    public final static String[] MERIS_REQUIRED_BRR_BAND_NAMES = new String[]{
            "brr_6", "brr_7", "brr_8", "brr_9", "brr_10", "brr_14"
    };

    public static final String MERIS_VALID_PIXEL_EXPR_3RD = "not (l1_flags.LAND_OCEAN or l1_flags.INVALID)";
    // todo: move to EnvisatConstants later?
    public final static String[] MERIS_RAD_BAND_NAMES_4TH = new String[]{
            "M01_radiance", "M02_radiance", "M03_radiance", "M04_radiance", "M05_radiance",
            "M06_radiance", "M07_radiance", "M08_radiance", "M09_radiance", "M10_radiance",
            "M11_radiance", "M12_radiance", "M13_radiance", "M14_radiance", "M15_radiance"
    };

    public static final String MERIS_VALID_PIXEL_EXPR_4TH = "not (quality_flags.land or quality_flags.invalid)";


    /////// OLCI ////////

    public final static float[] OLCI_WAVELENGHTS = {
            400.0f, 412.f, 442.f, 490.f, 510.0f,
            560.0f, 619.0f, 664.0f, 673.75f, 681.f,
            709.f, 753.f, 760.f, 764.375f, 767.5f,
            779.f, 865.0f, 885.0f, 900.0f, 940.0f, 1020.0f
    };

    public final static String[] OLCI_REQUIRED_RAD_BAND_NAMES = new String[]{
            "Oa07_radiance", "Oa08_radiance", "Oa10_radiance",
            "Oa11_radiance", "Oa12_radiance", "Oa18_radiance"
    };

    public final static String[] OLCI_REQUIRED_BRR_BAND_NAMES = new String[]{
            "rBRR_07", "rBRR_08", "rBRR_10",
            "rBRR_11", "rBRR_12", "rBRR_18"
    };

    public final static int OLCI_NUM_SPECTRAL_BANDS = OLCI_WAVELENGHTS.length;

    public final static String[] OLCI_RAD_BAND_NAMES = new String[]{
            "Oa01_radiance", "Oa02_radiance", "Oa03_radiance", "Oa04_radiance", "Oa05_radiance",
            "Oa06_radiance", "Oa07_radiance", "Oa08_radiance", "Oa09_radiance", "Oa10_radiance",
            "Oa11_radiance", "Oa12_radiance", "Oa13_radiance", "Oa14_radiance", "Oa15_radiance",
            "Oa16_radiance", "Oa17_radiance", "Oa18_radiance", "Oa19_radiance", "Oa20_radiance", "Oa21_radiance"
    };

    public static final String OLCI_VALID_PIXEL_EXPR = "not (quality_flags.land or quality_flags.invalid)";
}
