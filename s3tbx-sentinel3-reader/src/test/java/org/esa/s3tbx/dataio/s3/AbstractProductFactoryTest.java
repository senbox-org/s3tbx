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

package org.esa.s3tbx.dataio.s3;

import org.esa.s3tbx.dataio.s3.slstr.SlstrLevel1ProductFactory;
import org.esa.snap.core.dataio.geocoding.forward.PixelForward;
import org.esa.snap.core.dataio.geocoding.forward.PixelInterpolatingForward;
import org.esa.snap.core.dataio.geocoding.inverse.PixelGeoIndexInverse;
import org.esa.snap.core.dataio.geocoding.inverse.PixelQuadTreeInverse;
import org.junit.Test;

import static org.esa.s3tbx.dataio.s3.slstr.SlstrLevel1ProductFactory.SLSTR_L1B_PIXEL_GEOCODING_INVERSE;
import static org.esa.snap.core.dataio.geocoding.ComponentGeoCoding.SYSPROP_SNAP_PIXEL_CODING_FRACTION_ACCURACY;
import static org.junit.Assert.assertEquals;

public class AbstractProductFactoryTest {

    @Test
    public void testGetForwardAndInverseKeys_default() {
        final String inverseKey = System.getProperty(SLSTR_L1B_PIXEL_GEOCODING_INVERSE);
        final String fractionalAccuracy = System.getProperty(SYSPROP_SNAP_PIXEL_CODING_FRACTION_ACCURACY);

        try {
            System.clearProperty(SLSTR_L1B_PIXEL_GEOCODING_INVERSE);
            System.clearProperty(SYSPROP_SNAP_PIXEL_CODING_FRACTION_ACCURACY);

            final String[] keys = AbstractProductFactory.getForwardAndInverseKeys_pixelCoding(SLSTR_L1B_PIXEL_GEOCODING_INVERSE);
            assertEquals(PixelForward.KEY, keys[0]);
            assertEquals(PixelQuadTreeInverse.KEY, keys[1]);
        } finally {
            if (inverseKey != null) {
                System.setProperty(SLSTR_L1B_PIXEL_GEOCODING_INVERSE, inverseKey);
            }
            if (fractionalAccuracy != null) {
                System.setProperty(SYSPROP_SNAP_PIXEL_CODING_FRACTION_ACCURACY, fractionalAccuracy);
            }
        }
    }

    @Test
    public void testGetForwardAndInverseKeys_interpolating() {
        final String inverseKey = System.getProperty(SLSTR_L1B_PIXEL_GEOCODING_INVERSE);
        final String fractionalAccuracy = System.getProperty(SYSPROP_SNAP_PIXEL_CODING_FRACTION_ACCURACY);

        try {
            System.setProperty(SYSPROP_SNAP_PIXEL_CODING_FRACTION_ACCURACY, "true");

            final String[] keys = SlstrLevel1ProductFactory.getForwardAndInverseKeys_pixelCoding(SLSTR_L1B_PIXEL_GEOCODING_INVERSE);
            assertEquals(PixelInterpolatingForward.KEY, keys[0]);
            assertEquals(PixelQuadTreeInverse.KEY_INTERPOLATING, keys[1]);
        } finally {
            if (inverseKey != null) {
                System.setProperty(SLSTR_L1B_PIXEL_GEOCODING_INVERSE, inverseKey);
            }
            if (fractionalAccuracy != null) {
                System.setProperty(SYSPROP_SNAP_PIXEL_CODING_FRACTION_ACCURACY, fractionalAccuracy);
            }
        }
    }

    @Test
    public void testGetForwardAndInverseKeys_inverse() {
        final String inverseKey = System.getProperty(SLSTR_L1B_PIXEL_GEOCODING_INVERSE);
        final String fractionalAccuracy = System.getProperty(SYSPROP_SNAP_PIXEL_CODING_FRACTION_ACCURACY);

        try {
            System.clearProperty(SYSPROP_SNAP_PIXEL_CODING_FRACTION_ACCURACY);
            System.setProperty(SLSTR_L1B_PIXEL_GEOCODING_INVERSE, PixelGeoIndexInverse.KEY);

            final String[] keys = SlstrLevel1ProductFactory.getForwardAndInverseKeys_pixelCoding(SLSTR_L1B_PIXEL_GEOCODING_INVERSE);
            assertEquals(PixelForward.KEY, keys[0]);
            assertEquals(PixelGeoIndexInverse.KEY, keys[1]);
        } finally {
            if (inverseKey != null) {
                System.setProperty(SLSTR_L1B_PIXEL_GEOCODING_INVERSE, inverseKey);
            }
            if (fractionalAccuracy != null) {
                System.setProperty(SYSPROP_SNAP_PIXEL_CODING_FRACTION_ACCURACY, fractionalAccuracy);
            }
        }
    }
}
