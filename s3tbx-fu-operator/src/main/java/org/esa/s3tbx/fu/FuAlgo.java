/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.s3tbx.fu;

import com.bc.ceres.core.Assert;

class FuAlgo {

    public static final byte MAX_FU_VALUE = 21;

    private static final double CONST_WHITE_POINT = 0.333333;
    private static final double[] ANGLE_OF_TRANSITIONS = new double[]{
            232.0, 227.168, 220.977, 209.994, 190.779, 163.084, 132.999,
            109.054, 94.037, 83.346, 74.572, 67.957, 62.186, 56.435,
            50.665, 45.129, 39.769, 34.906, 30.439, 26.337, 22.741, 19.0, 19.0
    };
    private DominantLambdaLookup lambdaLookup;

    private double[] x3Factors;
    private double[] y3Factors;
    private double[] z3Factors;
    private double[] polyCoeffs;

    FuAlgo(Instrument instrument, boolean includeDominantLambda) {
        x3Factors = instrument.getXFactors();
        y3Factors = instrument.getYFactors();
        z3Factors = instrument.getZFactors();
        polyCoeffs = instrument.getPolynomCoefficients();
        if (includeDominantLambda) {
            lambdaLookup = new DominantLambdaLookup();
        }
    }

    // just for testing
    FuAlgo() {
    }


    void setPolyCoeffs(double[] polyCoeffs) {
        this.polyCoeffs = polyCoeffs;
    }

    void setZ3Factors(double[] z3Factors) {
        this.z3Factors = z3Factors;
    }

    void setY3Factors(double[] y3Factors) {
        this.y3Factors = y3Factors;
    }

    void setX3Factors(double[] x3Factors) {
        this.x3Factors = x3Factors;
    }

    public FuResult compute(double[] spectrum) {
        final double x3 = getTristimulusValue(spectrum, x3Factors);
        final double y3 = getTristimulusValue(spectrum, y3Factors);
        final double z3 = getTristimulusValue(spectrum, z3Factors);

        final double denominator = x3 + y3 + z3;

        final double chrX = x3 / denominator;
        final double chrY = y3 / denominator;

        final double hue = getHue(chrX, chrY);
        final double hue100 = (hue / 100);
        double polyCorr = getPolyCorr(hue100, polyCoeffs);

        FuResultImpl result = new FuResultImpl();
        result.x3 = x3;
        result.y3 = y3;
        result.z3 = z3;
        result.chrX = chrX;
        result.chrY = chrY;
        result.hue = hue;
        result.polyCorr = polyCorr;
        result.hueAngle = hue + polyCorr;
        result.fuValue = getFuValue(result.hueAngle);
        if (lambdaLookup != null) {
            result.domLambda = lambdaLookup.getDominantLambda(result.hueAngle);
        }
        return result;
    }

    static byte getFuValue(final double hueAngle) {
        for (byte i = 0; i < ANGLE_OF_TRANSITIONS.length; i++) {
            if (hueAngle > ANGLE_OF_TRANSITIONS[i]) {
                return i;
            }
        }
        return MAX_FU_VALUE;
    }

    double getTristimulusValue(double[] spectrum, double[] factors) {
        if (spectrum.length != factors.length) {
            throw new IllegalArgumentException("The spectrum must have equal length as factors.");
        }
        double summation = 0;
        for (int i = 0; i < spectrum.length; i++) {
            summation = (spectrum[i] * factors[i]) + summation;
        }
        return summation;
    }

    double getHue(double chrX, double chrY) {
        double atan2 = Math.atan2((chrY - CONST_WHITE_POINT), (chrX - CONST_WHITE_POINT));
        final double hue = (180 * atan2) / Math.PI;
        return hue < 0 ? hue + 360 : hue;
    }

    double getPolyCorr(double hue100, double[] constPolyHue) {
        Assert.argument(constPolyHue.length == 6, "constPolyHue.length == 6");
        double value = 0.0;
        for (int i = 0; i < constPolyHue.length; i++) {
            if ((i + 1) == constPolyHue.length) {
                value += constPolyHue[i];
                break;
            }
            value += constPolyHue[i] * Math.pow(hue100, 5 - i);
        }
        return value;
    }
}
