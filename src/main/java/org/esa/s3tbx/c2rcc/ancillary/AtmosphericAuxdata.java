package org.esa.s3tbx.c2rcc.ancillary;

public interface AtmosphericAuxdata {

    /**
     * Gets the daily interpolated ozone content value at the specified geo-location in dobson units.
     *
     * @param timeMJD the date/time value as mjd to retrieve the data for
     * @param lat     the latitude value
     * @param lon     the longitude value
     *
     * @return the ozone value or NaN if value could not be retrieved
     *
     * @throws Exception in case of disk access failures
     */
    double getOzone(double timeMJD, double lat, double lon) throws Exception;

    /**
     * Gets the quarter daily interpolated surface pressure value at the specified geo-location in mBar.
     *
     * @param timeMJD the date/time value as mjd to retrieve the data for
     * @param lat     the latitude value
     * @param lon     the longitude value
     *
     * @return the pressure value or NaN if value could not be retrieved
     *
     * @throws Exception in case of disk access failures
     */
    double getSurfacePressure(double timeMJD, double lat, double lon) throws Exception;

    /**
     * Releases all resources.
     */
    void dispose();
}
