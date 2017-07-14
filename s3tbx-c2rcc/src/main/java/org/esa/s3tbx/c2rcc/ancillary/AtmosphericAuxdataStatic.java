package org.esa.s3tbx.c2rcc.ancillary;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

class AtmosphericAuxdataStatic implements AtmosphericAuxdata {

    private final DataInterpolator ozoneInterpolator;
    private final DataInterpolator pressureInterpolator;

    AtmosphericAuxdataStatic(Product startOzone, Product endOzone,
                             String ozoneBandName, double ozoneDefault,
                             Product startPressure, Product endPressure,
                             String pressureBandName, double pressureDefault) throws IOException {
        this(getOzoneInterpolator(startOzone, endOzone, ozoneBandName, ozoneDefault),
             getPressureInterpolator(startPressure, endPressure, pressureBandName, pressureDefault));
    }

    private AtmosphericAuxdataStatic(DataInterpolator ozoneInterpolator, DataInterpolator pressureInterpolator) {
        this.ozoneInterpolator = ozoneInterpolator;
        this.pressureInterpolator = pressureInterpolator;
    }

    @Override
    public double getOzone(double mjd, int x, int y, double lat, double lon) throws IOException {
        return ozoneInterpolator.getValue(mjd, lat, lon);
    }

    @Override
    public double getSurfacePressure(double mjd, int x, int y, double lat, double lon) throws IOException {
        return pressureInterpolator.getValue(mjd, lat, lon);
    }

    @Override
    public void dispose() {
        //todo not a good practice because the products should be set to null but should be disposed where they are initialized
        ozoneInterpolator.dispose();
        ozoneInterpolator.dispose();
        pressureInterpolator.dispose();
        pressureInterpolator.dispose();
    }

    private static DataInterpolator getOzoneInterpolator(Product startOzone, Product endOzone, String ozoneBandName, double ozoneDefault) throws IOException {
        if (!isValidProduct(startOzone)) {
            throw new IOException("Ozone interpolation start product is invalid.");
        }
        if (!isValidProduct(endOzone)) {
            throw new IOException("Ozone interpolation end product is invalid.");
        }
        final double halfDayOffset = 0.5;
        final double ozoneTimeStart = getTime(startOzone) + halfDayOffset;
        final double ozoneTimeEnd = getTime(endOzone) + halfDayOffset;
        return new DataInterpolatorStatic(ozoneTimeStart, ozoneTimeEnd, startOzone, endOzone, ozoneBandName, ozoneDefault);
    }

    private static DataInterpolator getPressureInterpolator(Product startPressure, Product endPressure, String pressureBandName, double pressureDefault) throws IOException {
        if (!isValidProduct(startPressure)) {
            throw new IOException("Air pressure interpolation start product is invalid.");
        }
        if (!isValidProduct(endPressure)) {
            throw new IOException("Air pressure interpolation end product is invalid.");
        }
        final double threeHoursOffset = 0.125;
        final double pressureTimeStart = getTime(startPressure) + threeHoursOffset;
        final double pressureTimeEnd = getTime(endPressure) + threeHoursOffset;
        return new DataInterpolatorStatic(pressureTimeStart, pressureTimeEnd, startPressure, endPressure, pressureBandName, pressureDefault);
    }

    private static boolean isValidProduct(Product product) {
        return product != null;
    }

    private static double getTime(Product product) {
        final ProductData.UTC startTime = product.getStartTime();
        if (startTime != null) {
            return startTime.getMJD();
        }
        final File fileLocation = product.getFileLocation();
        final String fileName = fileLocation.getName();
        final Calendar calendar = createCalendarByFilename(fileName);
        product.setStartTime(ProductData.UTC.create(calendar.getTime(), 0));
        return product.getStartTime().getMJD();
    }

    static Calendar createCalendarByFilename(String fileName) {
        int year = Integer.parseInt(fileName.substring(1, 5));
        int doy = Integer.parseInt(fileName.substring(5, 8));
        int hour = Integer.parseInt(fileName.substring(8, 10));
        final Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.ENGLISH);
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.DAY_OF_YEAR, doy);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

}
