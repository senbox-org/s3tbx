package org.esa.s3tbx.c2rcc.anc;

import org.esa.snap.framework.datamodel.Band;
import org.esa.snap.framework.datamodel.GeoCoding;
import org.esa.snap.framework.datamodel.Product;

public class DataInterpolatorStatic extends DataInterpolator {

    private final double startTimeMJD;
    private final double endTimeMJD;
    private final Band startBand;
    private final Band endBand;
    private final GeoCoding startGC;
    private final GeoCoding endGC;

    public DataInterpolatorStatic(double startTimeMJD, double endTimeMJD, Product startProduct, Product endProduct, final String bandName, double defaultValue) {
        this.startTimeMJD = startTimeMJD;
        this.endTimeMJD = endTimeMJD;
        this.startBand = ensureInterpolation("start", startProduct, bandName, defaultValue);
        this.endBand = ensureInterpolation("end", endProduct, bandName, defaultValue);
        this.startGC = ensureGeocoding(startProduct);
        this.endGC = ensureGeocoding(endProduct);
    }

    @Override
    public double getValue(double timeMJD, double lat, double lon) {
        final double startValue = getStartValue(lat, lon);
        final double endValue = getEndValue(lat, lon);
        return startValue + (timeMJD - startTimeMJD) / (endTimeMJD - startTimeMJD) * (endValue - startValue);
    }

    @Override
    public void dispose() {
        startGC.dispose();
        endGC.dispose();
    }

    protected double getStartValue(double latitude, double longitude) {
        return getValue(startBand, startGC, latitude, longitude);
    }

    protected double getEndValue(double latitude, double longitude) {
        return getValue(endBand, endGC, latitude, longitude);
    }
}
