package org.esa.s3tbx.c2rcc.ancillary;

/**
 * @author Marco Peters
 */
class ConstantAtmosphericAuxdata implements AtmosphericAuxdata {

    private final double ozone;
    private final double surfPressure;

    ConstantAtmosphericAuxdata(double ozone, double surfPressure) {
        this.ozone = ozone;
        this.surfPressure = surfPressure;
    }

    /**
     * Ignores all parameters and returns always the constant
     * ozone value given in the constructor of this class.
     *
     * @param mjd ignored
     * @param x   ignored
     * @param y   ignored
     * @param lat ignored
     * @param lon ignored
     * @return constant ozone value
     * @throws Exception never thrown
     */
    @Override
    public double getOzone(double mjd, int x, int y, double lat, double lon) throws Exception {
        return ozone;
    }

    /**
     * Ignores all parameters and returns always the constant
     * surface pressure value given in the constructor of this class.
     *
     * @param mjd ignored
     * @param x   ignored
     * @param y   ignored
     * @param lat ignored
     * @param lon ignored
     * @return constant surface pressure value
     * @throws Exception never thrown
     */
    @Override
    public double getSurfacePressure(double mjd, int x, int y, double lat, double lon) throws Exception {
        return surfPressure;
    }

    @Override
    public void dispose() {

    }
}
