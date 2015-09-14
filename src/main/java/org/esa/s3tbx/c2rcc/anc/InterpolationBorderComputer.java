package org.esa.s3tbx.c2rcc.anc;

import org.esa.snap.framework.datamodel.ProductData;

import java.util.Calendar;

public interface InterpolationBorderComputer {

    void setInterpolationTimeMJD(double timeMJD);

    double getStartBorderTimeMDJ();

    double getEndBorderTimeMJD();

    String getStartAncFilePrefix();

    String getEndAncFilePrefix();

    static String convertToFileNamePräfix(double borderFileTimeMJD) {
        final ProductData.UTC utc = new ProductData.UTC(borderFileTimeMJD);
        final Calendar calendar = utc.getAsCalendar();
        final int year = calendar.get(Calendar.YEAR);
        final int doy = calendar.get(Calendar.DAY_OF_YEAR);
        final int h = calendar.get(Calendar.HOUR_OF_DAY);
        return String.format("N%4d%03d%02d", year, doy, h);
    }
}
