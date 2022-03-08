/*
 * Copyright (c) 2022.  Brockmann Consult GmbH (info@brockmann-consult.de)
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
 *
 *
 */

package org.esa.s3tbx.olci.radiometry.rayleigh;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Marco Peters
 */
public class L8UtilsTest {

    @Test
    public void getSpectralBandIndex() {
        assertEquals(0, L8Utils.getSpectralBandIndex("coastal_aerosol"));
        assertEquals(1, L8Utils.getSpectralBandIndex("blue"));
        assertEquals(2, L8Utils.getSpectralBandIndex("green"));
        assertEquals(3, L8Utils.getSpectralBandIndex("red"));
        assertEquals(4, L8Utils.getSpectralBandIndex("near_infrared"));
        assertEquals(5, L8Utils.getSpectralBandIndex("cirrus"));
        assertEquals(6, L8Utils.getSpectralBandIndex("swir_1"));
        assertEquals(7, L8Utils.getSpectralBandIndex("swir_2"));
    }

    @Test
    public void toReflectances() {
        double sunAngleCorrectionFactor = 0.638393;
        assertEquals(0.12399, L8Utils.toReflectances(40.89599,
                                                     -51.65807, 0.010332,
                                                     -0.1 / sunAngleCorrectionFactor,
                                                     2.0e-5 / sunAngleCorrectionFactor), 1.0e-4);
    }
}