package org.esa.s3tbx.olci.mph_chl;

/**
 * Constants for MPH CHL retrieval
 *
 * @author olafd
 */
public class MphChlConstants {

    public final static float[] OLCI_WAVELENGHTS = {
            400.0f, 412.5f, 442.5f, 490.0f, 510.0f,
            560.0f, 620.0f, 665.0f, 673.75f, 681.25f,
            708.75f, 753.75f, 761.25f, 764.375f, 767.5f,
            778.75f, 865.0f, 885.0f, 900.0f, 940.0f, 1020.0f
    };

    public final static String[] OLCI_REQUIRED_RAD_BAND_NAMES = new String[]{
            "Oa07_radiance", "Oa08_radiance", "Oa10_radiance",
            "Oa11_radiance", "Oa12_radiance", "Oa18_radiance"
    };

    public final static String[] OLCI_REQUIRED_BRR_BAND_NAMES = new String[]{
            "rBRR_07", "rBRR_08", "rBRR_10",
            "rBRR_11", "rBRR_12", "rBRR_18"
    };

}
