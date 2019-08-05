package org.esa.s3tbx.mphchl;

/**
 * Constants for MPH CHL retrieval
 *
 * @author olafd
 */
public class MphChlConstants {

    /////// MERIS ////////

    public final static float[] MERIS_WAVELENGTHS = {
            0.f, 412.f, 442.f, 490.f, 510.f,
            560.f, 619.f, 664.f, 681.f, 709.f,
            753.f, 760.f, 779.f, 865.f, 885.f, 900.f
    };

    public final static String[] MERIS_REQUIRED_RADIANCE_BAND_NAMES =
            {"radiance_6", "radiance_7", "radiance_8", "radiance_9", "radiance_10", "radiance_14", "radiance_5"};

    public final static String[] MERIS_REQUIRED_BRR_BAND_NAMES =
            {"rBRR_06", "rBRR_07", "rBRR_08", "rBRR_09", "rBRR_10", "rBRR_14", "rBRR_05"};

    public static final String MERIS_VALID_PIXEL_EXPR_3RD = "not (l1_flags.LAND_OCEAN or l1_flags.INVALID)";

    public static final String MERIS_VALID_PIXEL_EXPR_4TH = "not (quality_flags.land or quality_flags.invalid)";

    public final static String[] MERIS_REQUIRED_RADIANCE_BAND_NAMES_4TH =
            {"M06_radiance", "M07_radiance", "M08_radiance", "M09_radiance", "M10_radiance", "M14_radiance", "M05_radiance"};


    /////// OLCI ////////

    public final static float[] OLCI_WAVELENGHTS = {
            400.0f, 412.f, 442.f, 490.f, 510.0f,
            560.0f, 619.0f, 664.0f, 673.75f, 681.f,
            709.f, 753.f, 760.f, 764.375f, 767.5f,
            779.f, 865.0f, 885.0f, 900.0f, 940.0f, 1020.0f
    };

    public final static String[] OLCI_REQUIRED_RADIANCE_BAND_NAMES = new String[]{
            "Oa07_radiance", "Oa08_radiance", "Oa10_radiance",
            "Oa11_radiance", "Oa12_radiance", "Oa18_radiance", "Oa06_radiance"
    };

    public final static String[] OLCI_REQUIRED_BRR_BAND_NAMES =
            {"rBRR_07", "rBRR_08", "rBRR_10", "rBRR_11", "rBRR_12", "rBRR_18", "rBRR_06"};

    public static final String OLCI_VALID_PIXEL_EXPR = "not (quality_flags.land or quality_flags.invalid)";
}
