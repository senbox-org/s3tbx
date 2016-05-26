package org.esa.s3tbx.c2rcc.ancillary;

/**
 * @author Marco Peters
 */
public class ConstantAtmosphericAuxdata implements AtmosphericAuxdata {

    private final double ozone;
    private final double surfPressure;

    public ConstantAtmosphericAuxdata(double ozone, double surfPressure) {
        this.ozone = ozone;
        this.surfPressure = surfPressure;
    }

    /**
     * Ignores the {@code timeMJD}, {@code lat} and {@code lon} parameters and returns always the constant
     * ozone value given in the constructor of this class.
     *
     * @param timeMJD the date/time value as mjd to retrieve the data for (ignored)
     * @param lat     the latitude value (ignored)
     * @param lon     the longitude value (ignored)
     *
     * @return constant ozone value
     * @throws Exception never thrown
     */
    @Override
    public double getOzone(double timeMJD, double lat, double lon) throws Exception {
        return ozone;
    }

    /**
     * Ignores the {@code timeMJD}, {@code lat} and {@code lon} parameters and returns always the constant
     * surface pressure value given in the constructor of this class.
     *
     * @param timeMJD the date/time value as mjd to retrieve the data for (ignored)
     * @param lat     the latitude value (ignored)
     * @param lon     the longitude value (ignored)
     *
     * @return constant surface pressure value
     * @throws Exception never thrown
     */
    @Override
    public double getSurfacePressure(double timeMJD, double lat, double lon) throws Exception {
        return surfPressure;
    }

    @Override
    public void dispose() {

    }
}
