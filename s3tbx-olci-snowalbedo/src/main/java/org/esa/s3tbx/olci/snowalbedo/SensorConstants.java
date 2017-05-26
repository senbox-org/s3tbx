package org.esa.s3tbx.olci.snowalbedo;

/**
 * Constants for supported sensors
 *
 * @author olafd
 */
public class SensorConstants {

    public static final int OLCI_NUM_BANDS = 21;
    public static final String OLCI_SZA_NAME = "SZA";
    public static final String OLCI_VZA_NAME = "OZA";
    public static final String OLCI_SAA_NAME = "SAA";
    public static final String OLCI_VAA_NAME = "OAA";
    public static final String OLCI_OZONE_NAME = "total_ozone";
    public static final String OLCI_LAT_NAME = "latitude";
    public static final String OLCI_LON_NAME = "longitude";
    public static final String OLCI_ALT_NAME = "altitude";
    public static final String OLCI_SLP_NAME = "sea_level_pressure";
    public static final String OLCI_L1B_FLAGS_NAME = "quality_flags";
    public static final int OLCI_INVALID_BIT = 25;
    public static final int[] OLCI_BOUNDS = {17, 18};
    public static final String OLCI_NAME_FORMAT = "Oa%02d_";
    public static final String OLCI_NAME_PATTERN = "Oa\\d+_";
    public static final String OLCI_BAND_INFO_FILE_NAME = "band_info_olci.txt";

}
