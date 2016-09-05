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

package org.esa.s3tbx.olci.radiometry.rayleighcorrection;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.esa.s3tbx.olci.radiometry.smilecorr.SmileUtils;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.dataop.dem.ElevationModel;
import org.esa.snap.core.dataop.dem.ElevationModelDescriptor;
import org.esa.snap.core.dataop.dem.ElevationModelRegistry;
import org.esa.snap.core.dataop.resamp.Resampling;
import org.esa.snap.core.gpf.Tile;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * @author muhammad.bc.
 */
public class RayleighAux {

    public static final String GETASSE_30 = "GETASSE30";
    public static final String COEFF_MATRIX_TXT = "coeffMatrix.txt";
    public static final String TAU_RAY = "tau_ray";
    public static final String THETA = "theta";
    public static final String RAY_COEFF_MATRIX = "ray_coeff_matrix";
    public static final String RAY_ALBEDO_LUT = "ray_albedo_lut";
    public static PolynomialSplineFunction linearInterpolate;
    public static double[] tau_ray;
    private static ElevationModel elevationModel;
    private static double[] thetas;
    private static double[][][] rayCooefMatrixA;
    private static double[][][] rayCooefMatrixB;
    private static double[][][] rayCooefMatrixC;
    private static double[][][] rayCooefMatrixD;
    private int sourceBandIndex;
    private float waveLength;
    private String sourceBandName;
    private double[] longitude;
    private double[] altitudes;
    private Map<Integer, double[]> fourierPoly;
    private Map<Integer, List<double[]>> interpolateMap;

    private Tile sunZenithAnglesTile;
    private Tile viewZenithAnglesTile;
    private Tile sunAzimuthAnglesTile;
    private Tile viewAzimuthAnglesTile;
    private Tile seaLevelsTile;
    private Tile totalOzonesTile;
    private Tile latitudesTile;
    private Tile solarFluxsTile;
    private Tile lambdaSourceTile;
    private Tile sourceSampleRadTile;
    private Tile aziDiffTile;
    private Tile cosSZARadsTile;
    private Tile sinOZARadsTile;
    private Tile sinSZARadsTile;
    private Tile cosOZARadsTile;
    private Tile airMassTile;
    private Tile altitudesTile;
    private Tile longitudeTile;


    public static void initDefaultAuxiliary() {
        try {
            ElevationModelDescriptor getasse30 = ElevationModelRegistry.getInstance().getDescriptor(GETASSE_30);
            elevationModel = getasse30.createDem(Resampling.NEAREST_NEIGHBOUR);

            RayleighCorrectionAux rayleighCorrectionAux = new RayleighCorrectionAux();
            Path coeffMatrix = rayleighCorrectionAux.installAuxdata().resolve(COEFF_MATRIX_TXT);

            JSONParser jsonObject = new JSONParser();
            JSONObject parse = (JSONObject) jsonObject.parse(new FileReader(coeffMatrix.toString()));

            tau_ray = rayleighCorrectionAux.parseJSON1DimArray(parse, TAU_RAY);
            thetas = rayleighCorrectionAux.parseJSON1DimArray(parse, THETA);

            ArrayList<double[][][]> ray_coeff_matrix = rayleighCorrectionAux.parseJSON3DimArray(parse, RAY_COEFF_MATRIX);
            rayCooefMatrixA = ray_coeff_matrix.get(0);
            rayCooefMatrixB = ray_coeff_matrix.get(1);
            rayCooefMatrixC = ray_coeff_matrix.get(2);
            rayCooefMatrixD = ray_coeff_matrix.get(3);

            double[] lineSpace = getLineSpace(0, 1, 17);
            double[] rayAlbedoLuts = rayleighCorrectionAux.parseJSON1DimArray(parse, RAY_ALBEDO_LUT);
            linearInterpolate = new LinearInterpolator().interpolate(lineSpace, rayAlbedoLuts);

        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }

    public void setAltitudes(Tile altitude) {
        this.altitudesTile = altitude;
    }

    //for test only
    public void setInterpolation(HashMap<Integer, List<double[]>> integerHashMap) {
        this.interpolateMap = integerHashMap;
    }

    public double[] getTaur() {
        return tau_ray;
    }

    public double getAltitudes(int x, int y) {
        double longitude = getLongitude(x, y);
        double latitude = getLatitudes(x, y);

        if (Objects.nonNull(longitude) && Objects.nonNull(latitude)) {
            try {
                return elevationModel.getElevation(new GeoPos(latitude, longitude));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        throw new NullPointerException("");
    }


/*
    void setInterpolation() {
        BicubicSplineInterpolator gridInterpolator = new BicubicSplineInterpolator();
        Map<Integer, List<double[]>> interpolate = new HashMap<>();
        double[] sunZenithAngles = getSunZenithAngles();
        double[] viewZenithAngles = getViewZenithAngles();

        //todo mba ask Mp if to use this approach.
        assert sunZenithAngles != null;

        if (Objects.nonNull(sunZenithAngles) && Objects.nonNull(viewZenithAngles)) {
            for (int index = 0; index < sunZenithAngles.length; index++) {
                double yVal = viewZenithAngles[index];
                double xVal = sunZenithAngles[index];

                List<double[]> valueList = new ArrayList<>();
                for (int i = 0; i < rayCooefMatrixA.length; i++) {
                    double thetaMin = thetas[0];
                    double thetaMax = thetas[thetas.length - 1];

                    if (yVal > thetaMin && yVal < thetaMax) {
                        double[] values = new double[4];
                        values[0] = gridInterpolator.interpolate(thetas, thetas, rayCooefMatrixA[i]).value(xVal, yVal);
                        values[1] = gridInterpolator.interpolate(thetas, thetas, rayCooefMatrixB[i]).value(xVal, yVal);
                        values[2] = gridInterpolator.interpolate(thetas, thetas, rayCooefMatrixC[i]).value(xVal, yVal);
                        values[3] = gridInterpolator.interpolate(thetas, thetas, rayCooefMatrixD[i]).value(xVal, yVal);
                        valueList.add(values);
                    } else {
                        valueList.add(new double[]{0, 0, 0, 0});
                    }
                }
                interpolate.put(index, valueList);
            }
            interpolateMap = interpolate;
        }
    }
*/

    public List<double[]> getInterpolation(int x, int y) {
        return getSpikeInterpolation(x, y);
    }

    public List<double[]> getSpikeInterpolation(int x, int y) {
        double yVal = getViewZenithAngles(x, y);
        double xVal = getSunZenithAngles(x, y);
        List<double[]> valueList = new ArrayList<>();
        for (int i = 0; i < rayCooefMatrixA.length; i++) {
            double thetaMin = thetas[0];
            double thetaMax = thetas[thetas.length - 1];
            if (yVal > thetaMin && yVal < thetaMax) {
                double[] values = new double[4];
                values[0] = SpikeInterpolation.interpolate2D(rayCooefMatrixA[i], thetas, thetas, xVal, yVal);
                values[1] = SpikeInterpolation.interpolate2D(rayCooefMatrixB[i], thetas, thetas, xVal, yVal);
                values[2] = SpikeInterpolation.interpolate2D(rayCooefMatrixC[i], thetas, thetas, xVal, yVal);
                values[3] = SpikeInterpolation.interpolate2D(rayCooefMatrixD[i], thetas, thetas, xVal, yVal);
                valueList.add(values);
            } else {
                valueList.add(new double[]{0, 0, 0, 0});
            }
        }
        return valueList;
    }

    private List<double[]> getFourierMap(int x, int y) {
        // Fourier components of multiple scattering
        List<double[]> fourierPoly = new ArrayList<>();

        double cosSZARad = getCosSZARads(x, y);
        double cosOZARad = getCosOZARads(x, y);
        double sinSZARad = getSinSZARads(x, y);
        double sinOZARad = getSinOZARads(x, y);
        double sinOZA2 = Math.pow(sinOZARad, 2);
        double sinSZA2 = Math.pow(sinSZARad, 2);

        double[] fourierSeries = new double[3];
        //Rayleigh Phase function, 3 Fourier terms
        fourierSeries[0] = (3.0 * 0.9587256 / 4.0 * (1 + (cosSZARad * cosSZARad) * (cosOZARad * cosOZARad) + (sinSZA2 * sinOZA2) / 2.0) + (1.0 - 0.9587256));
        fourierSeries[1] = (-3.0 * 0.9587256 / 4.0 * cosSZARad * cosOZARad * sinSZARad * sinOZARad);
        fourierSeries[2] = (3.0 * 0.9587256 / 16.0 * sinSZA2 * sinOZA2);

        fourierPoly.add(fourierSeries);
        return fourierPoly;

    }

    public void setWavelength(float waveLenght) {
        this.waveLength = waveLenght;
    }

    public double getWaveLength() {
        return waveLength;
    }

    public void setSourceBandName(String targetBandName) {
        this.sourceBandName = targetBandName;
    }

    public String getSourceBandName() {
        return sourceBandName;
    }

    public double getSunAzimuthAnglesRad(int x, int y) {
        if (Objects.nonNull(sunAzimuthAnglesTile)) {
            return Math.toRadians(getSampleDouble(x, y, sunAzimuthAnglesTile));
        }
        throw new NullPointerException("The sun azimuth angles is null.");
    }

    public double getViewAzimuthAnglesRad(int x, int y) {
        if (Objects.nonNull(viewAzimuthAnglesTile)) {
            return Math.toRadians(getSampleDouble(x, y, viewZenithAnglesTile));
        }
        throw new NullPointerException("The view azimuth angles is null.");
    }

    public double getViewZenithAnglesRad(int x, int y) {
        if (Objects.nonNull(viewZenithAnglesTile)) {
            return getSampleDouble(x, y, viewZenithAnglesTile);
        }
        throw new NullPointerException("The view zenith angles is null.");
    }

    public double getSinOZARads(int x, int y) {
        return Math.sin(getViewZenithAnglesRad(x, y));
    }

    public double getAirMass(int x, int y) {
        return SmileUtils.getAirMass(this.getCosOZARads(x, y), this.getCosSZARads(x, y));
    }

    public double getAziDifferent(int x, int y) {
        return SmileUtils.getAziDiff(this.getSunAzimuthAnglesRad(x, y), this.getViewAzimuthAnglesRad(x, y));
    }

    public double getCosSZARads(int x, int y) {
        return Math.cos(getSunZenithAnglesRad(x, y));
    }


    public double getCosOZARads(int x, int y) {
        return Math.cos(getViewZenithAnglesRad(x, y));
    }


    public double getSinSZARads(int x, int y) {
        return Math.sin(getSunZenithAnglesRad(x, y));
    }


    public double getSunZenithAnglesRad(int x, int y) {
        if (Objects.nonNull(sunZenithAnglesTile)) {
            return Math.toRadians(getSampleDouble(x, y, sunAzimuthAnglesTile));
        }
        throw new NullPointerException("The value at x and does not exist");
    }

    public double getSunZenithAngles(int x, int y) {
        return getSampleDouble(x, y, sunZenithAnglesTile);
    }

    public void setSunZenithAngles(Tile sourceTile) {
        this.sunZenithAnglesTile = sourceTile;
    }

    public double getViewZenithAngles(int x, int y) {
        return getSampleDouble(x, y, viewZenithAnglesTile);
    }

    public void setViewZenithAngles(Tile sourceTile) {
        this.viewZenithAnglesTile = sourceTile;
    }

    public double getSunAzimuthAngles(int x, int y) {
        return getSampleDouble(x, y, sunAzimuthAnglesTile);
    }

    public void setSunAzimuthAngles(Tile sourceTile) {
        this.sunAzimuthAnglesTile = sourceTile;
    }

    public double getLatitudes(int x, int y) {
        return getSampleDouble(x, y, latitudesTile);
    }

    public void setLatitudes(Tile sourceTile) {
        this.latitudesTile = sourceTile;
    }

    public double getViewAzimuthAngles(int x, int y) {
        return getSampleDouble(x, y, viewAzimuthAnglesTile);
    }

    public void setViewAzimuthAngles(Tile sourceTile) {
        this.viewAzimuthAnglesTile = sourceTile;
    }

    public double getSeaLevels(int x, int y) {
        return getSampleDouble(x, y, seaLevelsTile);
    }

    public void setSeaLevels(Tile sourceTile) {
        this.seaLevelsTile = sourceTile;
    }


    public double getTotalOzones(int x, int y) {
        return getSampleDouble(x, y, totalOzonesTile);
    }

    public void setTotalOzones(Tile sourceTile) {
        this.totalOzonesTile = sourceTile;
    }

    public double getSolarFluxs(int x, int y) {
        return getSampleDouble(x, y, solarFluxsTile);
    }

    public void setSolarFluxs(Tile sourceTile) {
        solarFluxsTile = sourceTile;
    }

    public double getLambdaSource(int x, int y) {
        return getSampleDouble(x, y, lambdaSourceTile);
    }

    public void setLambdaSource(Tile sourceTile) {
        this.lambdaSourceTile = sourceTile;
    }

    public double getSourceSampleRad(int x, int y) {
        return getSampleDouble(x, y, sourceSampleRadTile);
    }


    public void setSourceSampleRad(Tile sourceTile) {
        this.sourceSampleRadTile = sourceTile;
    }

    public int getSourceBandIndex() {
        return sourceBandIndex;
    }

    public void setSourceBandIndex(int sourceBandIndex) {
        this.sourceBandIndex = sourceBandIndex;
    }

    public double getLongitude(int x, int y) {
        return getSampleDouble(x, y, longitudeTile);
    }

    public void setLongitude(Tile sourceTile) {
        this.longitudeTile = sourceTile;
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

    //todo mb/*** write a test
    private double[] getSquarePower(double[] sinOZARads) {
        if (Objects.nonNull(sinOZARads)) {
            return Arrays.stream(sinOZARads).map(p -> Math.pow(p, 2)).toArray();
        }
        throw new NullPointerException("The array is null.");
    }

    private double getSampleDouble(int x, int y, Tile sourceSampleRadTile) {
        return sourceSampleRadTile.getSampleDouble(x, y);
    }

    public static double[] getSampleDoubles(Tile sourceTile) {
        int maxX = sourceTile.getWidth();
        int maxY = sourceTile.getHeight();

        double[] val = new double[maxX * maxY];
        int index = 0;
        for (int y = sourceTile.getMinY(); y <= sourceTile.getMaxY(); y++) {
            for (int x = sourceTile.getMinX(); x <= sourceTile.getMaxX(); x++) {
                val[index++] = sourceTile.getSampleDouble(x, y);
            }
        }
        return val;
    }
}
