package org.esa.s3tbx.dataio.s3.slstr;

import org.junit.Test;

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
}
