package org.esa.s3tbx.dataio.s3;

import org.esa.snap.core.datamodel.PixelPos;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

public class SentinelTimeCodingTest {

    @Test
    public void testCanGetPixelPos() {
        final SentinelTimeCoding timeCoding = new SentinelTimeCoding(null);

        assertFalse(timeCoding.canGetPixelPos());
    }

    @Test
    public void testGetPixelPos() {
        final SentinelTimeCoding timeCoding = new SentinelTimeCoding(null);

        try {
            timeCoding.getPixelPos(2345687.887, null);
            fail("IllegalStateException expected");
        } catch (IllegalStateException expected) {
        }
    }

    @Test
    public void testGetMjd() {
        final long[] timeStamps = {661942736889806L, 661942737065843L, 661942737241844L, 661942737417845L, 661942737593847L};

        final SentinelTimeCoding timeCoding = new SentinelTimeCoding(timeStamps);

        double mjd = timeCoding.getMJD(new PixelPos(128, 0));
        assertEquals(7661.374269548804, mjd, 1e-8);

        mjd = timeCoding.getMJD(new PixelPos(128, 2));
        assertEquals(7661.374273622874, mjd, 1e-8);

        mjd = timeCoding.getMJD(new PixelPos(128, 4));
        assertEquals(7661.374277696945, mjd, 1e-8);
    }

    @Test
    public void testGetMjd_zero() {
        final long[] timeStamps = {0L};

        final SentinelTimeCoding timeCoding = new SentinelTimeCoding(timeStamps);

        double mjd = timeCoding.getMJD(new PixelPos(128, 0));
        assertEquals(0.0, mjd, 1e-8);
    }

    @Test
    public void testGetMjd_outOfRange() {
        final long[] timeStamps = {1, 2, 3};

        final SentinelTimeCoding timeCoding = new SentinelTimeCoding(timeStamps);

        try {
            timeCoding.getMJD(new PixelPos(128, -1));
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }

        try {
            timeCoding.getMJD(new PixelPos(128, 3));
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }
    }
}
