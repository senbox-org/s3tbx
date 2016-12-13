package org.esa.s3tbx.c2rcc.ancillary;

public interface AtmosphericAuxdata {

    /**
     * Gets the daily interpolated ozone content value at the specified geo-location in Dobson units.
     *
     * @param mjd the date/time value as mjd to retrieve the data for
     * @param x   the pixel x-index, preferred over lat if applicable
     * @param y   the pixel x-index, preferred over lon if applicable
     * @param lat the latitude value
     * @param lon the longitude value   @return the ozone value or NaN if value could not be retrieved
     * @throws Exception in case of disk access failures
     */
    double getOzone(double mjd, int x, int y, double lat, double lon) throws Exception;

    /**
     * Gets the quarter daily interpolated surface pressure value at the specified geo-location in mBar.
     *
     * @param mjd the date/time value as mjd to retrieve the data for
     * @param x   the pixel x-index, preferred over lat if applicable
     * @param y   the pixel y-index, preferred over lon if applicable
     * @param lat the latitude value
     * @param lon the longitude value
     * @return the pressure value or NaN if value could not be retrieved
     * @throws Exception in case of disk access failures
     */
    double getSurfacePressure(double mjd, int x, int y, double lat, double lon) throws Exception;

    /**
     * Releases all resources.
     */
    void dispose();
}
