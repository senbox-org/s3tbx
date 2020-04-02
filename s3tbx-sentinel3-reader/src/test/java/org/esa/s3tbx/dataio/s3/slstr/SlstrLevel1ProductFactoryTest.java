package org.esa.s3tbx.dataio.s3.slstr;

import org.junit.Test;

import java.io.IOException;

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
}
