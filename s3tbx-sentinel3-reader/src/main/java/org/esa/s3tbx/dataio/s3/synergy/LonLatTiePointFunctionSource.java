package org.esa.s3tbx.dataio.s3.synergy;

import org.esa.snap.core.util.math.DistanceMeasure;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

class LonLatTiePointFunctionSource {

    private final static double SEARCH_DEGREE_RADIUS = 0.2;

    private final List<File> ncFiles;
    private double[][] data;
    private final int latIndex;
    private final int lonIndex;
    private boolean initialized;

    private final Comparator<double[]> latComparator = new Comparator<double[]>() {
        @Override
        public int compare(double[] o1, double[] o2) {
            return Double.compare(o1[latIndex], o2[latIndex]);
        }
    };

    LonLatTiePointFunctionSource(List<File> ncFiles, int latIndex, int lonIndex) {
        this.latIndex = latIndex;
        this.lonIndex = lonIndex;
        initialized = false;
        this.ncFiles = ncFiles;
    }

    private synchronized void initialize() {
        if (initialized) {
            return;
        }
        try {
            int dimension = -1;
            int numVariables = 0;
            List<NcFile> openedNcFiles = new ArrayList<>();
            for (File file : ncFiles) {
                NcFile ncFile = NcFile.open(file);
                openedNcFiles.add(ncFile);
                List<Dimension> dimensions = ncFile.getDimensions(".*_tp");
                dimension = dimensions.get(0).getLength();
                List<Variable> variables = ncFile.getVariables(".*");
                numVariables += variables.size();
            }
            data = new double[dimension][numVariables];
            int offset = 0;
            for (int i = 0; i < openedNcFiles.size(); i++) {
                List<Variable> variables = openedNcFiles.get(i).getVariables(".*");
                for (int j = 0; j < variables.size(); j++) {
                    double [] variableData = openedNcFiles.get(i).read(variables.get(j).getFullName());
                    for (int k = 0; k < dimension; k++) {
                        data[k][offset + j] = variableData[k];
                    }
                }
                offset += variables.size();
                openedNcFiles.get(i).close();
            }
            Arrays.sort(data, latComparator);
        } catch (IOException e) {
            data = new double[0][0];
        }
        initialized = true;
    }

    public double getValue(double lon, double lat, int index) {
        if (!initialized) {
            initialize();
        }
        final DistanceMeasure distanceCalculator = new LonLatTiePointFunctionSource.DC(lon, lat);
        double[] search = new double[data[0].length];
        Entry upperLeftEntry = new Entry(Double.MAX_VALUE, Double.MIN_VALUE);
        Entry upperRightEntry = new Entry(Double.MAX_VALUE, Double.MIN_VALUE);
        Entry lowerRightEntry = new Entry(Double.MIN_VALUE, Double.MAX_VALUE);
        Entry lowerLeftEntry = new Entry(Double.MIN_VALUE, Double.MAX_VALUE);
        search[latIndex] = lat - SEARCH_DEGREE_RADIUS;
        int lowerLatIndex = Math.abs(Arrays.binarySearch(data, search, latComparator) + 1);
        search[latIndex] = lat + SEARCH_DEGREE_RADIUS;
        int upperLatIndex = Math.abs(Math.max(-data.length, Arrays.binarySearch(data, search, latComparator)));
        for (int i = lowerLatIndex; i < upperLatIndex; i++) {
            if (data[i][lonIndex] == lon && data[i][latIndex] == lat) {
                return data[i][index];
            }
            if (Math.abs(data[i][lonIndex] - lon) < SEARCH_DEGREE_RADIUS) {
                if (data[i][lonIndex] < lon) {
                    if (data[i][latIndex] > lat) {
                        if (data[i][lonIndex] > upperLeftEntry.lon || data[i][latIndex] < upperLeftEntry.lat) {
                            maybeUpdateEntry(upperLeftEntry, distanceCalculator, data[i], index);
                        }
                    } else {
                        if (data[i][lonIndex] > lowerLeftEntry.lon || data[i][latIndex] > lowerLeftEntry.lat) {
                            maybeUpdateEntry(lowerLeftEntry, distanceCalculator, data[i], index);
                        }
                    }
                } else {
                    if (data[i][latIndex] > lat) {
                        if (data[i][lonIndex] < upperRightEntry.lon || data[i][latIndex] < upperRightEntry.lat) {
                            maybeUpdateEntry(upperRightEntry, distanceCalculator, data[i], index);
                        }
                    } else {
                        if (data[i][lonIndex] < lowerRightEntry.lon || data[i][latIndex] > lowerRightEntry.lat) {
                            maybeUpdateEntry(lowerRightEntry, distanceCalculator, data[i], index);
                        }
                    }
                }
            }
        }
        if (upperRightEntry.weight < 0 && lowerRightEntry.weight < 0 &&
                upperLeftEntry.weight < 0 && lowerLeftEntry.weight < 0) {
            return Double.NaN;
        }
        double upperDist = lonDist(upperLeftEntry.lon, upperRightEntry.lon);
        double rightDist = latDist(upperRightEntry.lat, lowerRightEntry.lat);
        double lowerDist = lonDist(lowerLeftEntry.lon, lowerRightEntry.lon);
        double leftDist = latDist(upperLeftEntry.lat, lowerLeftEntry.lat);
        double upperLeftLonDist = lonDist(upperLeftEntry.lon, lon);
        double upperLeftLatDist = latDist(upperLeftEntry.lat, lat);
        double upperRightLonDist = lonDist(upperRightEntry.lon, lon);
        double upperRightLatDist = latDist(upperRightEntry.lat, lat);
        double lowerLeftLonDist = lonDist(lowerLeftEntry.lon, lon);
        double lowerLeftLatDist = latDist(lowerLeftEntry.lat, lat);
        double lowerRightLonDist = lonDist(lowerRightEntry.lon, lon);
        double lowerRightLatDist = latDist(lowerRightEntry.lat, lat);
        double upperLeftEntryFactor = (upperRightLonDist / upperDist) * (lowerLeftLatDist / leftDist);
        double upperRightEntryFactor = (upperLeftLonDist / upperDist) * (lowerRightLatDist / rightDist);
        double lowerRightEntryFactor = (lowerLeftLonDist / lowerDist) * (upperRightLatDist / rightDist);
        double lowerLeftEntryFactor = (lowerRightLonDist / lowerDist) * (upperLeftLatDist / leftDist);
        // use correction factor to get to have all the factors sum up to 1
        double allFactors = 0.0;
        if (Double.isFinite(upperLeftEntryFactor)) {
            allFactors += upperLeftEntryFactor;
        }
        if (Double.isFinite(upperRightEntryFactor)) {
            allFactors += upperRightEntryFactor;
        }
        if (Double.isFinite(lowerRightEntryFactor)) {
            allFactors += lowerRightEntryFactor;
        }
        if (Double.isFinite(lowerLeftEntryFactor)) {
            allFactors += lowerLeftEntryFactor;
        }
        double correction = 1.0 / allFactors;
        double value = 0.0;
        if (Double.isFinite(upperLeftEntryFactor)) {
            value += upperLeftEntry.value * upperLeftEntryFactor * correction;
        }
        if (Double.isFinite(upperRightEntryFactor)) {
            value += upperRightEntry.value * upperRightEntryFactor * correction;
        }
        if (Double.isFinite(lowerRightEntryFactor)) {
            value += lowerRightEntry.value * lowerRightEntryFactor * correction;
        }
        if (Double.isFinite(lowerLeftEntryFactor)) {
            value += lowerLeftEntry.value * lowerLeftEntryFactor * correction;
        }
        return value;
    }

    private void maybeUpdateEntry(Entry entry, DistanceMeasure distanceCalculator, double[] data, int index) {
        final double distance = distanceCalculator.distance(data[lonIndex], data[latIndex]);
        if (distance > entry.weight) {
            entry.weight = distance;
            entry.value = data[index];
            entry.lat = data[latIndex];
            entry.lon = data[lonIndex];
        }
    }

    private static class Entry {

        private double lat;
        private double lon;
        double weight;
        double value;

        Entry(double lat, double lon) {
            this.weight = -1.0;
            this.value = 1.0;
            this.lat = lat;
            this.lon = lon;
        }

    }

    private static double lonDist(double lon1, double lon2) {
        return Math.abs(lon1 - lon2);
    }

    private static double latDist(double lat1, double lat2) {
        return Math.abs(lat1 - lat2);
    }

    private static final class DC implements DistanceMeasure {

        private final double lon;
        private final double si;
        private final double co;

        private DC(double lon, double lat) {
            this.lon = lon;
            this.si = Math.sin(Math.toRadians(lat));
            this.co = Math.cos(Math.toRadians(lat));
        }

        @Override
        public double distance(double lon, double lat) {
            final double phi = Math.toRadians(lat);
            return si * Math.sin(phi) + co * Math.cos(phi) * Math.cos(Math.toRadians(lon - this.lon));
        }
    }

}
