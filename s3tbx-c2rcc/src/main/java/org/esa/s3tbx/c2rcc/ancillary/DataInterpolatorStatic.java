package org.esa.s3tbx.c2rcc.ancillary;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.Product;

import java.util.Arrays;

class DataInterpolatorStatic extends DataInterpolator {

    private final double startTimeMJD;
    private final double endTimeMJD;
    private Band startBand;
    private Band endBand;
    private final GeoCoding startGC;
    private final GeoCoding endGC;

    public DataInterpolatorStatic(double startTimeMJD, double endTimeMJD, Product startProduct, Product endProduct, final String bandName, double defaultValue) {
        this.startTimeMJD = startTimeMJD;
        this.endTimeMJD = endTimeMJD;
        final String bandNameStart;
        if (bandName.contains("gtco3") || bandName.contains("msl")) {
            String[] splitted_band_name = bandName.split("_");
            int positon1 = new Integer(splitted_band_name[1]) + 1;
            // bandNameStart = Arrays.stream(startProduct.getBandNames()).filter(name -> name.contains(bandName)).findFirst().map(Object::toString).orElse("");
            bandNameStart = splitted_band_name[0] + "_time" + positon1;
        } else {
            bandNameStart = bandName;
        }
        final String bandNameEnd;
        if (bandName.contains("gtco3") || bandName.contains("msl")) {
            String[] splitted_band_name = bandName.split("_");
            int positon2 = new Integer(splitted_band_name[2]) + 1;
            //bandNameEnd = Arrays.stream(startProduct.getBandNames()).filter(name -> name.contains(bandName)).reduce((first, second) -> second).orElse(null);
            bandNameEnd = splitted_band_name[0] + "_time" + positon2;
        } else {
            bandNameEnd = bandName;
        }
        this.startBand = ensureInterpolation("start", startProduct, bandNameStart, defaultValue);
        this.endBand = ensureInterpolation("end", endProduct, bandNameEnd, defaultValue);
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
        endBand = null;
        startBand = null;
    }

    protected double getStartValue(double latitude, double longitude) {
        double value = getValue(startBand, startGC, latitude, longitude);
        if (startBand.getName().contains("gtco3")) {
            value = value / 21.41 * 1e6; // convert from kg/m^2 to DU (https://de.wikipedia.org/wiki/Dobson-Einheit)
        }
        return value;
    }

    protected double getEndValue(double latitude, double longitude) {
        double value = getValue(endBand, endGC, latitude, longitude);
        if (endBand.getName().contains("gtco3")) {
            value = value / 21.41 * 1e6; // convert from kg/m^2 to DU
        }
        return value;
    }
}
