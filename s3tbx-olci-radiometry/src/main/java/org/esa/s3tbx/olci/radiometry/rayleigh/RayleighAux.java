/*
 *
 *  * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
 *  *
 *  * This program is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU General Public License as published by the Free
 *  * Software Foundation; either version 3 of the License, or (at your option)
 *  * any later version.
 *  * This program is distributed in the hope that it will be useful, but WITHOUT
 *  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 *  * more details.
 *  *
 *  * You should have received a copy of the GNU General Public License along
 *  * with this program; if not, see http://www.gnu.org/licenses/
 *
 */

package org.esa.s3tbx.olci.radiometry.rayleigh;

import com.bc.ceres.core.ProgressMonitor;
import com.google.common.primitives.Doubles;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.esa.s3tbx.olci.radiometry.smilecorr.SmileCorrectionUtils;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.dataop.dem.ElevationModel;
import org.esa.snap.core.dataop.dem.ElevationModelDescriptor;
import org.esa.snap.core.dataop.dem.ElevationModelRegistry;
import org.esa.snap.core.dataop.resamp.Resampling;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.OperatorSpiRegistry;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.util.ResourceInstaller;
import org.esa.snap.core.util.SystemUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.awt.Rectangle;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author muhammad.bc.
 */
public class RayleighAux {

    private static final String GETASSE_30 = "GETASSE30";
    private static final String COEFF_MATRIX_TXT = "coeffMatrix.txt";
    private static final String TAU_RAY = "tau_ray";
    private static final String THETA = "theta";
    private static final String RAY_COEFF_MATRIX = "ray_coeff_matrix";
    private static final String RAY_ALBEDO_LUT = "ray_albedo_lut";
    private static ElevationModel elevationModel;
    private static double[] thetas;
    private static double[][][] rayCoeffMatrixA;
    private static double[][][] rayCoeffMatrixB;
    private static double[][][] rayCoeffMatrixC;
    private static double[][][] rayCoeffMatrixD;
    private double[] sunZenithAngles;
    private double[] viewZenithAngles;
    private double[] sunAzimuthAngles;
    private double[] viewAzimuthAngles;
    private double[] seaLevels;
    private double[] totalOzones;
    private double[] latitudes;
    private double[] solarFluxs;
    private double[] sourceSampleRad;
    private float waveLength;
    private double[] longitudes;
    private double[] altitudes;
    private Map<Integer, double[]> fourierPoly;
    private Map<Integer, List<double[]>> interpolateMap;
    private double[] viewAzimuthAnglesRad;
    private double[] sunZenithAnglesRad;
    private double[] sunAzimuthAnglesRad;
    private double[] viewZenithAnglesRad;
    private double[] aziDiff;
    private double[] cosSZARads;
    private double[] sinOZARads;
    private double[] sinSZARads;
    private double[] cosOZARads;
    private double[] airMass;

    static PolynomialSplineFunction linearInterpolate;
    static double[] tau_ray;

    public static double[] parseJSON1DimArray(JSONObject parse, String ray_coeff_matrix) {
        JSONArray theta = (JSONArray) parse.get(ray_coeff_matrix);
        List<Double> collect = (List<Double>) theta.stream().collect(Collectors.toList());
        return Doubles.toArray(collect);
    }

    public static void initDefaultAuxiliary() throws IOException, ParseException {
        ElevationModelDescriptor getasse30 = ElevationModelRegistry.getInstance().getDescriptor(GETASSE_30);
        elevationModel = getasse30.createDem(Resampling.NEAREST_NEIGHBOUR);
        Path coeffMatrix = installAuxdata().resolve(COEFF_MATRIX_TXT);
        JSONParser jsonObject = new JSONParser();
        JSONObject parse = (JSONObject) jsonObject.parse(new FileReader(coeffMatrix.toString()));

        tau_ray = parseJSON1DimArray(parse, TAU_RAY);
        thetas = parseJSON1DimArray(parse, THETA);

        ArrayList<double[][][]> ray_coeff_matrix = parseJSON3DimArray(parse, RAY_COEFF_MATRIX);
        rayCoeffMatrixA = ray_coeff_matrix.get(0);
        rayCoeffMatrixB = ray_coeff_matrix.get(1);
        rayCoeffMatrixC = ray_coeff_matrix.get(2);
        rayCoeffMatrixD = ray_coeff_matrix.get(3);

        double[] lineSpace = getLineSpace(0, 1, 17);
        double[] rayAlbedoLuts = parseJSON1DimArray(parse, RAY_ALBEDO_LUT);
        linearInterpolate = new LinearInterpolator().interpolate(lineSpace, rayAlbedoLuts);
    }

    public void setSolarFluxs(double[] solarFluxs) {
        this.solarFluxs = solarFluxs;
    }

    public void setAltitudes(Tile altitude) {
        this.altitudes = altitude.getSamplesDouble();
    }

    public double[] getTaur() {
        return tau_ray;
    }

    public double[] getAltitudes() {
        if (Objects.isNull(altitudes)) {
            double[] longitudes = getLongitudes();
            double[] latitudes = getLatitudes();

            if (Objects.nonNull(longitudes) && Objects.nonNull(latitudes)) {
                double[] elevation = new double[latitudes.length];
                for (int i = 0; i < longitudes.length; i++) {
                    try {
                        elevation[i] = elevationModel.getElevation(new GeoPos(latitudes[i], longitudes[i]));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                altitudes = elevation;
            }
        }
        return altitudes;
    }

    public void setAltitudes(double... alt) {
        this.altitudes = alt;
    }

    public Map<Integer, List<double[]>> getInterpolation() {
        if (Objects.isNull(interpolateMap)) {
            interpolateMap = getSpikeInterpolation();
        }
        return interpolateMap;
    }

    //for test only
    void setInterpolation(HashMap<Integer, List<double[]>> integerHashMap) {
        this.interpolateMap = integerHashMap;
    }

    private Map<Integer, List<double[]>> getSpikeInterpolation() {
        double[] sunZenithAngles = getSunZenithAngles();
        double[] viewZenithAngles = getViewZenithAngles();
        Map<Integer, List<double[]>> interpolate = new HashMap<>();

        if (Objects.nonNull(sunZenithAngles) && Objects.nonNull(viewZenithAngles)) {
            for (int index = 0; index < sunZenithAngles.length; index++) {
                double vzaVal = viewZenithAngles[index];
                double szaVal = sunZenithAngles[index];
                if (Double.isNaN(szaVal) || Double.isNaN(vzaVal)) {
                    continue;
                }
                List<double[]> valueList = new ArrayList<>();
                for (int i = 0; i < rayCoeffMatrixA.length; i++) {
                    double[] values = new double[4];
                    values[0] = SpikeInterpolation.interpolate2D(rayCoeffMatrixA[i], thetas, thetas, szaVal, vzaVal);
                    values[1] = SpikeInterpolation.interpolate2D(rayCoeffMatrixB[i], thetas, thetas, szaVal, vzaVal);
                    values[2] = SpikeInterpolation.interpolate2D(rayCoeffMatrixC[i], thetas, thetas, szaVal, vzaVal);
                    values[3] = SpikeInterpolation.interpolate2D(rayCoeffMatrixD[i], thetas, thetas, szaVal, vzaVal);
                    valueList.add(values);
                }
                interpolate.put(index, valueList);
            }
        }
        return interpolate;
    }

    Map<Integer, double[]> getFourier() {
        if (Objects.isNull(fourierPoly)) {
            return fourierPoly = getFourierMap();
        }
        return fourierPoly;
    }

    public void setWavelength(float waveLength) {
        this.waveLength = waveLength;
    }

    public double getWaveLength() {
        return waveLength;
    }

    public double[] getSunAzimuthAnglesRad() {
        if (Objects.nonNull(sunAzimuthAnglesRad)) {
            return sunAzimuthAnglesRad;
        }
        throw new NullPointerException("The sun azimuth angles is null.");
    }

    public void setSunAzimuthAnglesRad(double[] sunAzimuthAngles) {
        if (Objects.nonNull(sunAzimuthAngles)) {
            sunAzimuthAnglesRad = SmileCorrectionUtils.convertDegreesToRadians(sunAzimuthAngles);
        }
    }

    public double[] getViewAzimuthAnglesRad() {
        if (Objects.nonNull(viewAzimuthAnglesRad)) {
            return viewAzimuthAnglesRad;
        }
        throw new NullPointerException("The view azimuth angles is null.");
    }

    public void setViewAzimuthAnglesRad(double[] viewAzimuthAngles) {
        if (Objects.nonNull(viewAzimuthAngles)) {
            viewAzimuthAnglesRad = SmileCorrectionUtils.convertDegreesToRadians(viewAzimuthAngles);
        }
    }

    public double[] getSunZenithAnglesRad() {
        if (Objects.nonNull(sunZenithAnglesRad)) {
            return sunZenithAnglesRad;
        }
        throw new NullPointerException("The sun zenith angles is null.");
    }

    public void setSunZenithAnglesRad(double[] sunZenithAngles) {
        if (Objects.nonNull(sunZenithAngles)) {
            sunZenithAnglesRad = SmileCorrectionUtils.convertDegreesToRadians(sunZenithAngles);
        }
        setCosSZARads(sunZenithAnglesRad);
        setSinSZARads(sunZenithAnglesRad);
    }

    public double[] getViewZenithAnglesRad() {
        if (Objects.nonNull(viewZenithAnglesRad)) {
            return viewZenithAnglesRad;
        }
        throw new NullPointerException("The view zenith angles is null.");
    }

    public void setViewZenithAnglesRad(double[] viewZenithAngles) {
        if (Objects.nonNull(viewZenithAngles)) {
            viewZenithAnglesRad = SmileCorrectionUtils.convertDegreesToRadians(viewZenithAngles);
        }
        setCosOZARads(viewZenithAnglesRad);
        setSinOZARads(viewZenithAnglesRad);
    }

    public double[] getAirMass() {
        if (Objects.isNull(airMass)) {
            airMass = SmileCorrectionUtils.getAirMass(this.getCosOZARads(), this.getCosSZARads());
        }
        return airMass;
    }

    public double[] getAziDifferent() {
        if (Objects.isNull(aziDiff)) {
            aziDiff = SmileCorrectionUtils.getAziDiff(this.getSunAzimuthAnglesRad(), this.getViewAzimuthAnglesRad());
        }
        return aziDiff;
    }

    public double[] getCosSZARads() {
        if (Objects.nonNull(cosSZARads)) {
            return cosSZARads;
        }
        throw new NullPointerException("The sun zenith angles is null.");
    }

    public void setCosSZARads(double[] sunZenithAnglesRad) {
        if (Objects.nonNull(sunZenithAnglesRad)) {
            cosSZARads = Arrays.stream(sunZenithAnglesRad).map(Math::cos).toArray();
        }
    }

    public double[] getCosOZARads() {
        if (Objects.nonNull(cosOZARads)) {
            return cosOZARads;
        }
        throw new NullPointerException("The view zenith angles is null.");
    }

    public void setCosOZARads(double[] zenithAnglesRad) {
        if (Objects.nonNull(zenithAnglesRad)) {
            cosOZARads = Arrays.stream(zenithAnglesRad).map(Math::cos).toArray();
        }
    }

    public double[] getSinSZARads() {
        if (Objects.nonNull(sinSZARads)) {
            return sinSZARads;
        }
        throw new NullPointerException("The sun zenith angles is null.");
    }

    public void setSinSZARads(double[] sunZenithAnglesRad) {
        if (Objects.nonNull(sunZenithAnglesRad)) {
            sinSZARads = Arrays.stream(sunZenithAnglesRad).map(Math::sin).toArray();
        }
    }

    public double[] getSinOZARads() {
        if (Objects.nonNull(sinOZARads)) {
            return sinOZARads;
        }
        throw new NullPointerException("The view zenith angles is null.");
    }

    public void setSinOZARads(double[] zenithAnglesRad) {
        if (Objects.nonNull(zenithAnglesRad)) {
            sinOZARads = Arrays.stream(zenithAnglesRad).map(Math::sin).toArray();
        }
    }

    public double[] getSunZenithAngles() {
        return sunZenithAngles;
    }

    public void setSunZenithAngles(Tile sourceTile) {
        this.sunZenithAngles = sourceTile.getSamplesDouble();
        setSunZenithAnglesRad(sunZenithAngles);
    }

    public void setSunZenithAngles(double... sunZenithAngles) {
        this.sunZenithAngles = sunZenithAngles;
        setSunZenithAnglesRad(sunZenithAngles);
    }

    public double[] getViewZenithAngles() {
        return viewZenithAngles;
    }

    public void setViewZenithAngles(double... viewZenithAngles) {
        this.viewZenithAngles = viewZenithAngles;
        setViewZenithAnglesRad(viewZenithAngles);
    }

    public void setViewZenithAngles(Tile sourceTile) {
        this.viewZenithAngles = sourceTile.getSamplesDouble();
        setViewZenithAnglesRad(viewZenithAngles);
    }

    public void setSunAzimuthAngles(double... sunAzimuthAngles) {
        this.sunAzimuthAngles = sunAzimuthAngles;
        setSunAzimuthAnglesRad(sunAzimuthAngles);
    }

    public void setSunAzimuthAngles(Tile sourceTile) {
        this.sunAzimuthAngles = sourceTile.getSamplesDouble();
        setSunAzimuthAnglesRad(sunAzimuthAngles);
    }

    public double[] getLatitudes() {
        return latitudes;
    }

    public void setLatitudes(double... lat) {
        this.latitudes = lat;
    }

    public void setLatitudes(Tile sourceTile) {
        this.latitudes = sourceTile.getSamplesDouble();
    }

    public void setViewAzimuthAngles(double... viewAzimuthAngles) {
        this.viewAzimuthAngles = viewAzimuthAngles;
        setViewAzimuthAnglesRad(viewAzimuthAngles);
    }

    public void setViewAzimuthAngles(Tile sourceTile) {
        this.viewAzimuthAngles = sourceTile.getSamplesDouble();
        setViewAzimuthAnglesRad(viewAzimuthAngles);
    }

    public double[] getSeaLevels() {
        return seaLevels;
    }

    public void setSeaLevels(double... seaLevels) {
        this.seaLevels = seaLevels;
    }

    public void setS2MsiSeaLevelsPressures(double seaLevel, Rectangle rectangle) {
        this.seaLevels = new double[rectangle.width * rectangle.height];
        Arrays.fill(this.seaLevels, seaLevel);
    }

    public void setS2MsiTotalOzones(double ozone, Rectangle rectangle) {
        this.totalOzones = new double[rectangle.width * rectangle.height];
        Arrays.fill(this.totalOzones, ozone);
    }

    public void setS2MsiAngles(GeoCoding geoCoding, Tile szaTile, Tile vzaTile, Tile saaTile, Tile vaaTile, Rectangle rectangle) {
        // in general we have no lat/lon raster in S2,
        // so we use a center values for given tile, should be ok for our purpose
        this.latitudes = new double[rectangle.width * rectangle.height];
        this.longitudes = new double[rectangle.width * rectangle.height];

        final PixelPos pixelPos = new PixelPos(rectangle.x + rectangle.width/2, rectangle.y + rectangle.height/2);

        final GeoPos geoPos = geoCoding.getGeoPos(pixelPos, null);
        Arrays.fill(this.latitudes, geoPos.getLat());
        Arrays.fill(this.longitudes, geoPos.getLon());
    }

    public void setSeaLevels(Tile sourceTile) {
        this.seaLevels = sourceTile.getSamplesDouble();
    }

    public double[] getTotalOzones() {
        return totalOzones;
    }

    public void setTotalOzones(double... totalO) {
        this.totalOzones = totalO;
    }

    public void setTotalOzones(Tile sourceTile) {
        this.totalOzones = sourceTile.getSamplesDouble();
    }

    public void setOlciTotalOzones(Tile sourceTile) {
        this.totalOzones = new double[sourceTile.getRectangle().width * sourceTile.getRectangle().height];
        final double[] samplesOzone = sourceTile.getSamplesDouble();
        for (int i = 0; i < samplesOzone.length; i++) {
            this.totalOzones[i] = samplesOzone[i] / 2.144e-5;   // convert from kg/m2 to DU
        }
    }

    public double[] getSolarFluxs() {
        return solarFluxs;
    }

    public void setSolarFluxs(Tile sourceTile) {
        this.solarFluxs = sourceTile.getSamplesDouble();
    }

    public double[] getSourceSampleRad() {
        return sourceSampleRad;
    }

    public void setSourceSampleRad(Tile sourceTile) {
        this.sourceSampleRad = sourceTile.getSamplesDouble();
    }

    public double[] getLongitudes() {
        return longitudes;
    }

    public void setLongitudes(double... longitudes) {
        this.longitudes = longitudes;
    }

    public void setLongitude(Tile sourceTile) {
        this.longitudes = sourceTile.getSamplesDouble();
    }

    public double[] getInterpolateRayleighThickness(double... taur) {
        if (Objects.nonNull(taur)) {
            double[] val = new double[taur.length];
            for (int i = 0; i < taur.length; i++) {
                val[i] = linearInterpolate.value(taur[i]);
            }
            return val;
        }
        throw new NullPointerException("The linearInterpolate Rayleigh thickness is empty.");
    }

    //todo mb/*** write a test
    public double[] getSquarePower(double[] sinOZARads) {
        if (Objects.nonNull(sinOZARads)) {
            return Arrays.stream(sinOZARads).map(p -> Math.pow(p, 2)).toArray();
        }
        throw new NullPointerException("The array is null.");
    }

    private double[] getGridValueAt(int x, int y) {
        double[] values = new double[4];
        values[0] = rayCoeffMatrixA[x][y][0];
        values[1] = rayCoeffMatrixB[x][y][0];
        values[2] = rayCoeffMatrixC[x][y][0];
        values[3] = rayCoeffMatrixD[x][y][0];
        return values;
    }

    private Map<Integer, double[]> getFourierMap() {
        // Fourier components of multiple scattering
        Map<Integer, double[]> fourierPoly = new HashMap<>();
        double[] sunZenithAnglesRad = getSunZenithAnglesRad();
        double[] viewZenithAnglesRad = getViewZenithAnglesRad();

        double[] cosSZARads = getCosSZARads();
        double[] cosOZARads = getCosOZARads();

        double[] sinSZARads = getSinSZARads();
        double[] sinOZARads = getSinOZARads();

        double[] sinOZA2s = getSquarePower(sinOZARads);
        double[] sinSZA2s = getSquarePower(sinSZARads);

        if (Objects.nonNull(sunZenithAnglesRad) && Objects.nonNull(viewZenithAnglesRad)) {
            for (int index = 0; index < sunZenithAnglesRad.length; index++) {
                double cosSZARad = cosSZARads[index];
                double cosOZARad = cosOZARads[index];

                double sinSZARad = sinSZARads[index];
                double sinOZARad = sinOZARads[index];

                double sinSZA2 = sinSZA2s[index];
                double sinOZA2 = sinOZA2s[index];

                double[] fourierSeries = new double[3];
                //Rayleigh Phase function, 3 Fourier terms
                fourierSeries[0] = (3.0 * 0.9587256 / 4.0 * (1 + Math.pow(cosSZARad, 2) * Math.pow(cosOZARad, 2) + (sinSZA2 * sinOZA2) / 2.0) + (1.0 - 0.9587256));
                fourierSeries[1] = (-3.0 * 0.9587256 / 4.0 * cosSZARad * cosOZARad * sinSZARad * sinOZARad);
                fourierSeries[2] = (3.0 * 0.9587256 / 16.0 * sinSZA2 * sinOZA2);

                fourierPoly.put(index, fourierSeries);
            }
            return fourierPoly;

        }
        throw new NullPointerException("The Fourier polynomial is empty.");
    }

    static Path installAuxdata() throws IOException {
        OperatorSpiRegistry operatorSpiRegistry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        OperatorSpi spi = operatorSpiRegistry.getOperatorSpi("RayleighCorrection");
        String version = "v" + spi.getOperatorDescriptor().getVersion();
        Path auxdataDirectory = SystemUtils.getAuxDataPath().resolve("olci/rayleigh/" + version);
        final Path sourceDirPath = ResourceInstaller.findModuleCodeBasePath(RayleighAux.class).resolve("auxdata/rayleigh");
        final ResourceInstaller resourceInstaller = new ResourceInstaller(sourceDirPath, auxdataDirectory);
        resourceInstaller.install(".*", ProgressMonitor.NULL);
        return auxdataDirectory;
    }

    static ArrayList<double[][][]> parseJSON3DimArray(JSONObject parse, String ray_coeff_matrix) {
        JSONArray theta = (JSONArray) parse.get(ray_coeff_matrix);
        Iterator<JSONArray> iterator1 = theta.iterator();

        double[][][] rayCooffA = new double[3][12][12];
        double[][][] rayCooffB = new double[3][12][12];
        double[][][] rayCooffC = new double[3][12][12];
        double[][][] rayCooffD = new double[3][12][12];

        int k = 0;
        while (iterator1.hasNext()) { //3
            JSONArray next = iterator1.next();
            Iterator<JSONArray> iterator2 = next.iterator();
            int i1 = 0;
            while (iterator2.hasNext()) {//12
                JSONArray iterator3 = iterator2.next();
                Iterator<JSONArray> iterator4 = iterator3.iterator();
                for (int j = 0; j < 12; j++) {//12
                    JSONArray mainValue = iterator4.next();
                    List<Double> collectedValues = (List<Double>) mainValue.stream().collect(Collectors.toList());
                    rayCooffA[k][i1][j] = collectedValues.get(0);
                    rayCooffB[k][i1][j] = collectedValues.get(1);
                    rayCooffC[k][i1][j] = collectedValues.get(2);
                    rayCooffD[k][i1][j] = collectedValues.get(3);
                }
                i1++;
            }
            k++;
        }
        ArrayList<double[][][]> rayCoefficient = new ArrayList<>();
        rayCoefficient.add(rayCooffA);
        rayCoefficient.add(rayCooffB);
        rayCoefficient.add(rayCooffC);
        rayCoefficient.add(rayCooffD);
        return rayCoefficient;

    }

    static double[] getLineSpace(double start, double end, int interval) {
        if (interval < 0) {
            throw new NegativeArraySizeException("Array must not have negative index");
        }
        double[] temp = new double[interval];
        double steps = (end - start) / (interval - 1);
        for (int i = 0; i < temp.length; i++) {
            temp[i] = steps * i;
        }
        return temp;
    }

}

