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

import com.google.common.primitives.Doubles;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * @author muhammad.bc.
 */
class SpikeInterpolation {
    static double interpolate2D(double[][] doubles2D, double[] xCoordinate, double[] yCoordinate,
                                double x, double y) {

//        https://en.wikipedia.org/wiki/Bilinear_interpolation
        double x1;
        double x2;
        double y1;
        double y2;
        if(x < xCoordinate[0]){
            x1 = xCoordinate[0];
            x2 = xCoordinate[1];
        }
        else{
            x1 = getLowerBound(xCoordinate, x);
            x2 = getUpperValue(xCoordinate, x);
        }
        if(y < yCoordinate[0]){
            y1 = yCoordinate[0];
            y2 = yCoordinate[1];
        }
        else{
            y1 = getLowerBound(yCoordinate, y);
            y2 = getUpperValue(yCoordinate, y);
        }

        int ix1 = arrayIndex(xCoordinate, x1);
        int ix2 = arrayIndex(xCoordinate, x2);

        int iy1 = arrayIndex(yCoordinate, y1);
        int iy2 = arrayIndex(yCoordinate, y2);

        double f11 = doubles2D[ix1][iy1];
        double f12 = doubles2D[ix1][iy2];
        double f21 = doubles2D[ix2][iy1];
        double f22 = doubles2D[ix2][iy2];

        double q11 = interBetween(f11, f21, x2, x1, x);
        double q12 = interBetween(f12, f22, x2, x1, x);

        return interBetween(q11, q12, y2, y1, y);
    }

    static double interBetween(double lowerY, double upperY, double upperX, double lowerX, double position) {
        if (upperX - lowerX == 0) {
            return lowerY;
        }
        return lowerY + ((upperY - lowerY) * (position - lowerX)) / (upperX - lowerX);
    }

    static int arrayIndex(double[] values, double val) {
        return Doubles.asList(values).indexOf(val);
    }

    static double getUpperValue(double[] doubles, double val) {
        final List<Double> xMin = new ArrayList<>();
        int length = doubles.length;
        IntStream.range(0, length).forEach(i -> {
            double v = doubles[i];
            if (v >= val) {
                xMin.add(v);
            }
        });
        double[] allMax = Doubles.toArray(xMin);
        if (allMax.length == 0) {
            throw new IllegalArgumentException("Can fine the closest max value of " + val);
        }
        return Doubles.min(allMax);
    }

    static double getLowerBound(double[] doubles, double val) {
        final double[] xMin = new double[1];
        int length = doubles.length;
        IntStream.range(0, length).forEach(i -> {
            double v = doubles[i];
            xMin[0] = v <= val ? v : xMin[0];
        });
        if (xMin[0] > val) {
            throw new IllegalArgumentException("Can find the closest min value of " + val);
        }
        return xMin[0];
    }
}
