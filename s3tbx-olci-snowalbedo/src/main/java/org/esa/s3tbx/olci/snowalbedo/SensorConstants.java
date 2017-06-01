package org.esa.s3tbx.olci.snowalbedo;

/**
 * Constants for supported sensors for snow albedo retrieval (just OLCI so far).
 *
 * @author olafd
 */
public class SensorConstants {

    public static final String OLCI_SZA_NAME = "SZA";
    public static final String OLCI_VZA_NAME = "OZA";
    public static final String OLCI_SAA_NAME = "SAA";
    public static final String OLCI_VAA_NAME = "OAA";
    public static final String OLCI_L1B_FLAGS_NAME = "quality_flags";
    public static final int OLCI_INVALID_BIT = 25;

    public final static String[] OLCI_REQUIRED_RADIANCE_BAND_NAMES = new String[]{
            "Oa01_radiance", "Oa02_radiance", "Oa03_radiance", "Oa04_radiance", "Oa05_radiance",
            "Oa06_radiance", "Oa07_radiance", "Oa08_radiance", "Oa10_radiance", "Oa11_radiance",
            "Oa16_radiance", "Oa17_radiance", "Oa18_radiance", "Oa21_radiance"
    };

    public final static String OLCI_BRR_BAND_PREFIX = "rBRR";
    public final static String[] OLCI_REQUIRED_BRR_BAND_NAMES =
            {"rBRR_01", "rBRR_02", "rBRR_03", "rBRR_04", "rBRR_05", "rBRR_06",  "rBRR_07",
                    "rBRR_08", "rBRR_10", "rBRR_11", "rBRR_16", "rBRR_17", "rBRR_18",  "rBRR_21"
            };
}
