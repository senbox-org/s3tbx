/*
 * Copyright (c) 2021.  Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.s3tbx.olci.sensor.harmonisation;

import org.esa.snap.core.gpf.OperatorException;

abstract class DetectorRegression {

    private static double G0 = 2.60170;
    private static double G1 = 0.001308;

    abstract double calculate(float waveLength);

    static DetectorRegression get(int sensorIndex) {
        if (sensorIndex == 0) {
            return new AtoB();
        } else if (sensorIndex == 1) {
            return new BtoA();
        }

        throw new OperatorException("unsupported sensor for cross-harmonisation");
    }

    static class AtoB extends DetectorRegression {
        @Override
        double calculate(float waveLength) {
            return 1.0 + (G1 * waveLength - G0) * 0.01;
        }
    }

    static class BtoA extends DetectorRegression {
        @Override
        double calculate(float waveLength) {
            return 1.0 / (1.0 + (G1 * waveLength - G0) * 0.01);
        }
    }

}
