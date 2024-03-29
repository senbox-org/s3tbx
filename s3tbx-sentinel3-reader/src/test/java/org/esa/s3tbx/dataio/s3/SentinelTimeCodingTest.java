package org.esa.s3tbx.dataio.s3;

import org.esa.snap.core.datamodel.PixelPos;
import org.junit.Test;

import static org.junit.Assert.*;

public class
SentinelTimeCodingTest {

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

    @Test
    public void testGetMaxDelta_RR() {
        final long[] timeStamps = {573547550874421L,
                573547551050457L,
                573547551226456L,
                573547551402454L,
                573547551578452L,
                573547551754450L
        };

        final SentinelTimeCoding timeCoding = new SentinelTimeCoding(timeStamps);

        final int delta = timeCoding.getMaxDelta();
        assertEquals(176036, delta);
    }

    @Test
    public void testGetMaxDelta_FR() {
        final long[] timeStamps = {576377323831920L,
                576377323875919L,
                576377323919919L,
                576377323963918L,
                576377324007956L,
                576377324051955L
        };

        final SentinelTimeCoding timeCoding = new SentinelTimeCoding(timeStamps);

        final int delta = timeCoding.getMaxDelta();
        assertEquals(44038, delta);
    }
}
