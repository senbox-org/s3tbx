package org.esa.s3tbx.c2rcc.ancillary;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

class AtmosphericAuxdataStatic implements AtmosphericAuxdata {

    private final DataInterpolator ozoneInterpolator;
    private final DataInterpolator pressureInterpolator;

    AtmosphericAuxdataStatic(Product startOzone, Product endOzone,
                             String ozoneBandName, double ozoneDefault,
                             Product startPressure, Product endPressure,
                             String pressureBandName, double pressureDefault, ProductData.UTC sourceTime) throws IOException {
        this(getOzoneInterpolator(startOzone, endOzone, ozoneBandName, ozoneDefault, sourceTime),
                getPressureInterpolator(startPressure, endPressure, pressureBandName, pressureDefault, sourceTime));
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

    private static DataInterpolator getOzoneInterpolator(Product startOzone, Product endOzone, String ozoneBandName, double ozoneDefault, ProductData.UTC sourceTime) throws IOException {
        if (!isValidProduct(startOzone)) {
            throw new IOException("Ozone interpolation start product is invalid.");
        }
        if (!isValidProduct(endOzone)) {
            throw new IOException("Ozone interpolation end product is invalid.");
        }
        final double halfDayOffset = 0.5;
        final double ozoneTimeStart;
        final double ozoneTimeEnd;
        if (ozoneBandName.equals("gtco3")) {
            CAMSBandResult ozoneCAMSBandStart = getTimeCAMS(startOzone, true, sourceTime);
            CAMSBandResult ozoneCAMSBandEnd = getTimeCAMS(endOzone, false, sourceTime);
            ozoneTimeStart = ozoneCAMSBandStart.getTime();
            ozoneTimeEnd = ozoneCAMSBandEnd.getTime();
            ozoneBandName = ozoneBandName + "_" + ozoneCAMSBandStart.getPositon() + "_" + ozoneCAMSBandEnd.getPositon();
        } else {
            ozoneTimeStart = getTime(startOzone) + halfDayOffset;
            ozoneTimeEnd = getTime(endOzone) + halfDayOffset;
        }
        return new DataInterpolatorStatic(ozoneTimeStart, ozoneTimeEnd, startOzone, endOzone, ozoneBandName, ozoneDefault);
    }

    private static DataInterpolator getPressureInterpolator(Product startPressure, Product endPressure, String pressureBandName, double pressureDefault, ProductData.UTC sourceTime) throws IOException {
        if (!isValidProduct(startPressure)) {
            throw new IOException("Air pressure interpolation start product is invalid.");
        }
        if (!isValidProduct(endPressure)) {
            throw new IOException("Air pressure interpolation end product is invalid.");
        }
        final double threeHoursOffset = 0.125;
        final double pressureTimeStart;
        final double pressureTimeEnd;
        if (pressureBandName.equals("msl")) {
            CAMSBandResult pressureCAMSBandStart = getTimeCAMS(startPressure, true, sourceTime);
            CAMSBandResult pressureCAMSBandEnd = getTimeCAMS(endPressure, false, sourceTime);
            pressureTimeStart = pressureCAMSBandStart.getTime();
            pressureTimeEnd = pressureCAMSBandEnd.getTime();
            pressureBandName = pressureBandName + "_" + pressureCAMSBandStart.getPositon() + "_" + pressureCAMSBandStart.getPositon();
        } else {
            pressureTimeStart = getTime(startPressure) + threeHoursOffset;
            pressureTimeEnd = getTime(endPressure) + threeHoursOffset;
        }
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

    private static CAMSBandResult getTimeCAMS(Product product, Boolean isStartProduct, ProductData.UTC sourceTime) {
        double sourceTime_mjd = sourceTime.getMJD();
        ProductData.Int availableTimes = (ProductData.Int) product.getMetadataRoot().getElement("Variable_Attributes").getElement("time").getElement("Values").getAttribute("data").getData();

        ArrayList<Double> mjd_times = new ArrayList<>();
        Calendar calendar;
        for (int time : availableTimes.getArray()) {
            calendar = createCalendarByHoursSince1900(time);
            mjd_times.add(ProductData.UTC.create(calendar.getTime(), 0).getMJD());
        }
        CAMSBandResult bandResult;
        int position = Collections.binarySearch(mjd_times, sourceTime_mjd);
        if (isStartProduct) {
            if (position >= 0) {
                bandResult = new CAMSBandResult(position, mjd_times.get(position));
            } else {
                bandResult = new CAMSBandResult(-1 * position - 2, mjd_times.get(-1 * position - 2));
            }
        } else {
            if (position >= 0) {
                bandResult = new CAMSBandResult(position, mjd_times.get(position));
            } else {
                bandResult = new CAMSBandResult(-1 * position - 1, mjd_times.get(-1 * position - 1));
            }
        }
        return bandResult;
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

    static Calendar createCalendarByHoursSince1900(int hoursSince1900) {
        LocalDateTime t0 = LocalDateTime.parse("1900-01-01T00:00:00");
        LocalDateTime timeSince1900 = t0.plusHours(hoursSince1900);
        final Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.ENGLISH);
        calendar.clear();
        calendar.set(timeSince1900.getYear(), timeSince1900.getMonthValue() - 1, timeSince1900.getDayOfMonth(),
                timeSince1900.getHour(), timeSince1900.getMinute(), timeSince1900.getSecond());
        return calendar;
    }

    static final class CAMSBandResult {
        private final int position;
        private final double time;

        public CAMSBandResult(int position, double time) {
            this.position = position;
            this.time = time;
        }

        public int getPositon() {
            return position;
        }

        public double getTime() {
            return time;
        }
    }

}
