package org.esa.s3tbx.dataio.s3;

import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.TimeCoding;
import org.esa.snap.core.util.DateTimeUtils;

import java.util.Calendar;

class SentinelTimeCoding implements TimeCoding {

    private final long[] timeStamps;

    SentinelTimeCoding(long[] timeStamps) {
        this.timeStamps = timeStamps;
    }

    @Override
    public double getMJD(PixelPos pixelPos) {
        final int y = (int) pixelPos.y;
        if (y < 0 || y >= timeStamps.length) {
            throw new IllegalArgumentException("requested coordinate out of range: " + y);
        }

        final Calendar cal2000 = ProductData.UTC.createCalendar();
        final long millisSince2k = timeStamps[y] / 1000L;
        final int secsSince2k = (int) (millisSince2k / 1000L);
        cal2000.add(Calendar.SECOND, secsSince2k);

        final int millis = (int) (millisSince2k - secsSince2k * 1000);
        cal2000.add(Calendar.MILLISECOND, millis);

        final double jd = DateTimeUtils.utcToJD(cal2000.getTime());
        return DateTimeUtils.jdToMJD(jd);
    }

    @Override
    public boolean canGetPixelPos() {
        return false;
    }

    @Override
    public PixelPos getPixelPos(double mjd, PixelPos pixelPos) {
        throw new IllegalStateException("not supported");
    }
}
