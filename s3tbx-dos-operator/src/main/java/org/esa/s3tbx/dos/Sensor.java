package org.esa.s3tbx.dos;

import static org.esa.s3tbx.dos.SensorConstants.*;

/**
 * Enumeration for supported sensors for Dark Object Subtraction operator.
 *
 * @author olafd
 */
public enum Sensor {

    S2_MSI("S2_MSI", S2_MSI_SPECTRAL_BANDS),
    LANDSAT_TM("LANDSAT_TM", LANDSAT_TM_SPECTRAL_BANDS);

    private String name;
    private String[] spectralBands;

    Sensor(String name, String[] spectralBands) {
        this.name = name;
        this.spectralBands = spectralBands;
    }

    public String getName() {
        return name;
    }

    public String[] getSpectralBands() {
        return spectralBands;
    }
}
