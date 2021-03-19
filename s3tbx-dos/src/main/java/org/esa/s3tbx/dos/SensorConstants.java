package org.esa.s3tbx.dos;

/**
 * Constants for supported sensors for snow albedo retrieval (just OLCI so far).
 *
 * @author olafd
 */
public class SensorConstants {

    public final static String[] S2_MSI_SPECTRAL_BANDS = {
            "B1", "B2", "B3", "B4", "B5", "B6", "B7", "B8", "B8A", "B9", "B10", "B11", "B12"
    };

    public final static String[] LANDSAT_TM_SPECTRAL_BANDS = {
            "Coastal aerosol", "Blue", "Green", "Red", "Near Infrared", "SWIR 1", "SWIR 2"
    };

}