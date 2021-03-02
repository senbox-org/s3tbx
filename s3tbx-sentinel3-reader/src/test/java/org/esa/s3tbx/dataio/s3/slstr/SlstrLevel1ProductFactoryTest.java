package org.esa.s3tbx.dataio.s3.slstr;

import org.esa.snap.core.dataio.geocoding.forward.PixelForward;
import org.esa.snap.core.dataio.geocoding.forward.PixelInterpolatingForward;
import org.esa.snap.core.dataio.geocoding.inverse.PixelGeoIndexInverse;
import org.esa.snap.core.dataio.geocoding.inverse.PixelQuadTreeInverse;
import org.junit.Test;

import java.io.IOException;

import static org.esa.s3tbx.dataio.s3.slstr.SlstrLevel1ProductFactory.SLSTR_L1B_PIXEL_GEOCODING_INVERSE;
import static org.esa.snap.core.dataio.geocoding.ComponentGeoCoding.SYSPROP_SNAP_PIXEL_CODING_FRACTION_ACCURACY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class SlstrLevel1ProductFactoryTest {

    @Test
    public void testGetResolutionInKm() {
        assertEquals(0.5, SlstrLevel1ProductFactory.getResolutionInKm("an"), 1e-8);
        assertEquals(0.5, SlstrLevel1ProductFactory.getResolutionInKm("ao"), 1e-8);
        assertEquals(0.5, SlstrLevel1ProductFactory.getResolutionInKm("bn"), 1e-8);
        assertEquals(0.5, SlstrLevel1ProductFactory.getResolutionInKm("bo"), 1e-8);
        assertEquals(0.5, SlstrLevel1ProductFactory.getResolutionInKm("cn"), 1e-8);
        assertEquals(0.5, SlstrLevel1ProductFactory.getResolutionInKm("co"), 1e-8);

        assertEquals(1.0, SlstrLevel1ProductFactory.getResolutionInKm("fn"), 1e-8);
        assertEquals(1.0, SlstrLevel1ProductFactory.getResolutionInKm("fo"), 1e-8);
        assertEquals(1.0, SlstrLevel1ProductFactory.getResolutionInKm("in"), 1e-8);
        assertEquals(1.0, SlstrLevel1ProductFactory.getResolutionInKm("io"), 1e-8);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    public void testGetResolutionInKm_invalidNameEnding() {
        try {
            SlstrLevel1ProductFactory.getResolutionInKm("heffalump");
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testGetGeolocationVariableNames() throws IOException {
        String[] variableNames = SlstrLevel1ProductFactory.getGeolocationVariableNames("an");
        assertEquals("longitude_an", variableNames[0]);

        variableNames = SlstrLevel1ProductFactory.getGeolocationVariableNames("ao");
        assertEquals("latitude_ao", variableNames[1]);

        variableNames = SlstrLevel1ProductFactory.getGeolocationVariableNames("bn");
        assertEquals("longitude_bn", variableNames[0]);

        variableNames = SlstrLevel1ProductFactory.getGeolocationVariableNames("bo");
        assertEquals("latitude_bo", variableNames[1]);

        variableNames = SlstrLevel1ProductFactory.getGeolocationVariableNames("cn");
        assertEquals("longitude_cn", variableNames[0]);

        variableNames = SlstrLevel1ProductFactory.getGeolocationVariableNames("co");
        assertEquals("latitude_co", variableNames[1]);

        variableNames = SlstrLevel1ProductFactory.getGeolocationVariableNames("in");
        assertEquals("longitude_in", variableNames[0]);

        variableNames = SlstrLevel1ProductFactory.getGeolocationVariableNames("io");
        assertEquals("latitude_io", variableNames[1]);

        variableNames = SlstrLevel1ProductFactory.getGeolocationVariableNames("fn");
        assertEquals("longitude_fn", variableNames[0]);

        variableNames = SlstrLevel1ProductFactory.getGeolocationVariableNames("fo");
        assertEquals("latitude_fo", variableNames[1]);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    public void testGetGeolocationVariableNames_invalidExtension() {
        try {
            SlstrLevel1ProductFactory.getGeolocationVariableNames("quatsch");
            fail("IOException expected");
        } catch (IOException expected) {
        }
    }

    @Test
    public void testGetForwardAndInverseKeys_default() {
        final String inverseKey = System.getProperty(SLSTR_L1B_PIXEL_GEOCODING_INVERSE);
        final String fractionalAccuracy = System.getProperty(SYSPROP_SNAP_PIXEL_CODING_FRACTION_ACCURACY);

        try {
            System.clearProperty(SLSTR_L1B_PIXEL_GEOCODING_INVERSE);
            System.clearProperty(SYSPROP_SNAP_PIXEL_CODING_FRACTION_ACCURACY);

            final String[] keys = SlstrLevel1ProductFactory.getForwardAndInverseKeys();
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

            final String[] keys = SlstrLevel1ProductFactory.getForwardAndInverseKeys();
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

            final String[] keys = SlstrLevel1ProductFactory.getForwardAndInverseKeys();
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
