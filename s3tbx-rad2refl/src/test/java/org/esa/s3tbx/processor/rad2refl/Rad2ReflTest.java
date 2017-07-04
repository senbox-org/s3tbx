/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.s3tbx.processor.rad2refl;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class Rad2ReflTest {

    @Test
    public void testGetSolarFluxesMeris() {
        try {
            Rad2ReflAuxdata auxdataRR = Rad2ReflAuxdata.loadMERISAuxdata("MER_RR");
            assertNotNull(auxdataRR);
            final double[][] fluxesRR = auxdataRR.getDetectorSunSpectralFluxes();
            assertEquals(925, fluxesRR.length);
            assertEquals(15, fluxesRR[0].length);
            assertEquals(1935.689, fluxesRR[376][2], 1.E-3);
            assertEquals(1177.575, fluxesRR[525][11], 1.E-3);
            assertEquals(1471.933, fluxesRR[37][7], 1.E-3);

            Rad2ReflAuxdata auxdataFR = Rad2ReflAuxdata.loadMERISAuxdata("MER_FR");
            assertNotNull(auxdataFR);
            final double[][] fluxesFR = auxdataFR.getDetectorSunSpectralFluxes();
            assertEquals(3700, fluxesFR.length);
            assertEquals(15, fluxesFR[0].length);
            assertEquals(1807.942, fluxesFR[613][4], 1.E-3);
            assertEquals(958.49, fluxesFR[1374][12], 1.E-3);
            assertEquals(1877.713, fluxesFR[2085][1], 1.E-3);


        } catch (IOException e) {
            fail(e.getMessage());
        }

    }
}

