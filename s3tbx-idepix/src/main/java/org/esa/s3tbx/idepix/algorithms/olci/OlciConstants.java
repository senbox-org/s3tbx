package org.esa.s3tbx.idepix.algorithms.olci;

/**
 * Constants for Idepix OLCI algorithm
 *
 * @author olafd
 */
public class OlciConstants {

    /* Level 1 Quality Flags Positions */
    public static final int L1_F_LAND = 31;
    public static final int L1_F_COASTLINE = 30;
//    public static final int L1_F_FRESH_INLAND_WATER = 29;
    public static final int L1_F_BRIGHT = 27;
    public static final int L1_F_INVALID = 25;
    public static final int L1_F_GLINT = 22;


    public static final String OLCI_QUALITY_FLAGS_BAND_NAME = "quality_flags";
}
