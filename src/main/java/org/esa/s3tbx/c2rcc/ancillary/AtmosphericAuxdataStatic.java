package org.esa.s3tbx.c2rcc.ancillary;

import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.datamodel.ProductData;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public class AtmosphericAuxdataStatic implements AtmosphericAuxdata {

    private static final int[] months = new int[]{
                Calendar.JANUARY, Calendar.FEBRUARY, Calendar.MARCH,
                Calendar.APRIL, Calendar.MAY, Calendar.JUNE,
                Calendar.JULY, Calendar.AUGUST, Calendar.SEPTEMBER,
                Calendar.OCTOBER, Calendar.NOVEMBER, Calendar.DECEMBER
    };

    private final DataInterpolator ozoneInterpolator;
    private final DataInterpolator pressureInterpolator;

    public AtmosphericAuxdataStatic(Product startOzone, Product endOzone,
                                    String ozoneBandName, double ozoneDefault,
                                    Product startPressure, Product endPressure,
                                    String pressureBandName, double pressureDefault) throws IOException {
        this(getOzoneInterpolator(startOzone, endOzone, ozoneBandName, ozoneDefault),
             getPressureInterpolator(startPressure, endPressure, pressureBandName, pressureDefault));
    }

    public AtmosphericAuxdataStatic(DataInterpolator ozoneInterpolator, DataInterpolator pressureInterpolator) {
        this.ozoneInterpolator = ozoneInterpolator;
        this.pressureInterpolator = pressureInterpolator;
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

    @Override
    public double getOzone(double timeMJD, double lat, double lon) throws IOException {
        return ozoneInterpolator.getValue(timeMJD, lat, lon);
    }

    @Override
    public double getSurfacePressure(double timeMJD, double lat, double lon) throws Exception {
        return pressureInterpolator.getValue(timeMJD, lat, lon);
    }

    @Override
    public void dispose() {
        //todo not a good practice because the products should be set to null but should be disposed where they are initialized
        ozoneInterpolator.dispose();
        ozoneInterpolator.dispose();
        pressureInterpolator.dispose();
        pressureInterpolator.dispose();
    }

    private static boolean isValidProduct(Product product) {
        return product != null;
    }

    static double getTime(Product product) {
        final ProductData.UTC startTime = product.getStartTime();
        if (startTime != null) {
            return startTime.getMJD();
        }
        final File fileLocation = product.getFileLocation();
        final String fileName = fileLocation.getName();
        int year = Integer.parseInt(fileName.substring(1, 5));
        int dayInYear = Integer.parseInt(fileName.substring(5, 8));
        int[] firstDaysOfMonths = {1, 32, 60, 91, 121, 152, 182, 213, 244, 274, 305, 335};
        if (isLeapYear(year)) {
            firstDaysOfMonths = new int[]{1, 32, 61, 92, 122, 153, 183, 214, 245, 275, 306, 336};
        }
        int month = months[11];
        int dayInMonth = dayInYear - firstDaysOfMonths[11];
        for (int i = 1; i < firstDaysOfMonths.length; i++) {
            int firstDayOfMonth = firstDaysOfMonths[i];
            if (dayInYear <= firstDayOfMonth) {
                month = months[i - 1];
                dayInMonth = dayInYear - firstDaysOfMonths[i - 1] + 1;
                break;
            }
        }
        int hour = Integer.parseInt(fileName.substring(8, 10));
        final Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.ENGLISH);
        calendar.set(year, month, dayInMonth, hour, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        product.setStartTime(ProductData.UTC.create(calendar.getTime(), 0));
        return product.getStartTime().getMJD();
    }

    static boolean isLeapYear(int year) {
        return !(year % 4 != 0 || year % 400 == 0);
    }
}
