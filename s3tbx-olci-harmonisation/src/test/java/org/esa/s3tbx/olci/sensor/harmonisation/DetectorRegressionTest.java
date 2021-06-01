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
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class DetectorRegressionTest {

    @Test
    public void testAtoB() {
        final DetectorRegression.AtoB atoB = new DetectorRegression.AtoB();

        assertEquals(0.973983, atoB.calculate(0), 1e-8);
        assertEquals(0.979218065738678, atoB.calculate(400.234f), 1e-8);
        assertEquals(0.9847420847680664, atoB.calculate(822.56f), 1e-8);
    }

    @Test
    public void testBtoA() {
        final DetectorRegression.BtoA btoA = new DetectorRegression.BtoA();

        assertEquals(1.0267119651985712, btoA.calculate(0), 1e-8);
        assertEquals(1.0212229941381172, btoA.calculate(400.234f), 1e-8);
        assertEquals(1.0154943263499572, btoA.calculate(822.56f), 1e-8);
    }

    @Test
    public void testGet() {
        DetectorRegression detectorRegression = DetectorRegression.get(0);
        assertTrue(detectorRegression instanceof DetectorRegression.AtoB);

        detectorRegression = DetectorRegression.get(1);
        assertTrue(detectorRegression instanceof DetectorRegression.BtoA);
    }

    @Test
    public void testGet_invalid() {
        try {
            DetectorRegression.get(-1);
            fail("OperatorException expected");
        } catch (OperatorException expected) {
        }

        try {
            DetectorRegression.get(2);
            fail("OperatorException expected");
        } catch (OperatorException expected) {
        }
    }
}
