/*
 *
 *  * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
 *  *
 *  * This program is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU General Public License as published by the Free
 *  * Software Foundation; either version 3 of the License, or (at your option)
 *  * any later version.
 *  * This program is distributed in the hope that it will be useful, but WITHOUT
 *  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 *  * more details.
 *  *
 *  * You should have received a copy of the GNU General Public License along
 *  * with this program; if not, see http://www.gnu.org/licenses/
 *
 */

package org.esa.s3tbx.olci.radiometry.rayleigh;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class S2UtilsTest {

    @Before
    public void setUp() {
    }

    @Test
    public void testGetS2SpectralBandIndex() {
        int s2SpectralBandIndex = S2Utils.getS2SpectralBandIndex("B1");
        assertEquals(0, s2SpectralBandIndex);

        s2SpectralBandIndex = S2Utils.getS2SpectralBandIndex("B9");
        assertEquals(9, s2SpectralBandIndex);

        s2SpectralBandIndex = S2Utils.getS2SpectralBandIndex("B8A");
        assertEquals(8, s2SpectralBandIndex);

        s2SpectralBandIndex = S2Utils.getS2SpectralBandIndex("B12");
        assertEquals(12, s2SpectralBandIndex);
    }

    @Test
    public void testGetS2TargetBandName() {
        String s2TargetBandName = S2Utils.getS2TargetBandName(RayleighCorrectionOp.BAND_CATEGORIES[0], "B1");
        assertEquals("taur_B1", s2TargetBandName);

        s2TargetBandName = S2Utils.getS2TargetBandName(RayleighCorrectionOp.BAND_CATEGORIES[1], "B9");
        assertEquals("rBRR_B9", s2TargetBandName);

        s2TargetBandName = S2Utils.getS2TargetBandName(RayleighCorrectionOp.BAND_CATEGORIES[2], "B8A");
        assertEquals("rtoa_ng_B8A", s2TargetBandName);

        s2TargetBandName = S2Utils.getS2TargetBandName(RayleighCorrectionOp.BAND_CATEGORIES[3], "B12");
        assertEquals("rtoa_B12", s2TargetBandName);
    }

    @Test
    public void testTargetS2BandNameMatches() {
        String targetBandName = "taur_01";
        assertFalse(S2Utils.targetS2BandNameMatches(targetBandName, RayleighCorrectionOp.TAUR_PATTERN));
        targetBandName = "taur_B1";
        assertTrue(S2Utils.targetS2BandNameMatches(targetBandName, RayleighCorrectionOp.TAUR_PATTERN));

        targetBandName = "rBRR_09";
        assertFalse(S2Utils.targetS2BandNameMatches(targetBandName, RayleighCorrectionOp.R_BRR_PATTERN));
        targetBandName = "rBRR_B9";
        assertTrue(S2Utils.targetS2BandNameMatches(targetBandName, RayleighCorrectionOp.R_BRR_PATTERN));

        targetBandName = "rtoa_ng_8A";
        assertFalse(S2Utils.targetS2BandNameMatches(targetBandName, RayleighCorrectionOp.RTOA_NG_PATTERN));
        targetBandName = "rtoa_ng_B8A";
        assertTrue(S2Utils.targetS2BandNameMatches(targetBandName, RayleighCorrectionOp.RTOA_NG_PATTERN));

        targetBandName = "rtoa_12";
        assertFalse(S2Utils.targetS2BandNameMatches(targetBandName, RayleighCorrectionOp.RTOA_PATTERN));
        targetBandName = "rtoa_B12";
        assertTrue(S2Utils.targetS2BandNameMatches(targetBandName, RayleighCorrectionOp.RTOA_PATTERN));
    }

    @Test
    public void testGetNumBandsToRcCorrect() {
        String[] inputBands = new String[]{"B1", "B4", "B7", "B8A"};
        assertEquals(4, S2Utils.getNumBandsToRcCorrect(inputBands));

        inputBands = new String[]{"B7", "B8A", "B10", "B11"};
        assertEquals(4, S2Utils.getNumBandsToRcCorrect(inputBands));

        inputBands = new String[]{"B12", "B10", "B11"};
        assertEquals(3, S2Utils.getNumBandsToRcCorrect(inputBands));
    }
}