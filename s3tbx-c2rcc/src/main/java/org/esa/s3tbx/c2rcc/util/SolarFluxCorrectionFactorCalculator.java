package org.esa.s3tbx.c2rcc.util;

import org.esa.snap.core.datamodel.ProductData;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import static java.lang.Math.*;

public class SolarFluxCorrectionFactorCalculator {

    private final static Calendar cal = GregorianCalendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.ENGLISH);

    static {
        cal.set(Calendar.MONTH, Calendar.DECEMBER);
        cal.set(Calendar.DAY_OF_MONTH, 31);
        cal.set(Calendar.HOUR_OF_DAY, 12);
    }

    public static double computeFactorFor(ProductData.UTC startTime, ProductData.UTC endTime) {
        final ProductData.UTC centerUTC = getCenterUTC(startTime, endTime);
        return computeFactorFor(centerUTC);
    }

    public static double computeFactorFor(ProductData.UTC time) {
        final Calendar calendar = time.getAsCalendar();
        final int doy = calendar.get(Calendar.DAY_OF_YEAR);
        final int year = calendar.get(Calendar.YEAR);
        return getDayCorrectionFactorFor(doy, year);
    }

    public static ProductData.UTC getCenterUTC(ProductData.UTC start, ProductData.UTC end) {
        final long startMillis = start.getAsDate().getTime();
        final long endMillis = end.getAsDate().getTime();
        final long diff = endMillis - startMillis;
        final Date centerDate = new Date(startMillis + diff / 2);
        return ProductData.UTC.create(centerDate, 0);
    }

    static double getDayCorrectionFactorFor(int day, int year) {
        // see """"""""""""""""""""""""""""""""""""""""
        //     "  An introduction to solar radiation  "
        //     "            Muhammad Iqbal            "
        //     "                1993                  "
        //     " Chapter 1.2 ... Sun-Earth Distance r "
        //     """"""""""""""""""""""""""""""""""""""""
        final double gamma = PI * 2 * (day - 1) / getNumDaysInTheYear(year);
        return 1.000110
               + 0.034221 * cos(gamma)
               + 0.001280 * sin(gamma)
               + 0.000719 * cos(2 * gamma)
               + 0.000077 * sin(2 * gamma);
    }

    public static synchronized int getNumDaysInTheYear(int year) {
        cal.set(GregorianCalendar.YEAR, year);
        return cal.get(Calendar.DAY_OF_YEAR);
    }
}
