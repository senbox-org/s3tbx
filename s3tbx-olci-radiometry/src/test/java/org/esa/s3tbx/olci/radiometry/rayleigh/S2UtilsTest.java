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

import org.esa.s3tbx.olci.radiometry.Sensor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class S2UtilsTest {

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void testGetS2SpectralBandIndex() throws Exception {
        Band band = new Band("B1", ProductData.TYPE_FLOAT32, 1, 1);
        int s2SpectralBandIndex = S2Utils.getS2SpectralBandIndex(band);
        assertEquals(0, s2SpectralBandIndex);

        band = new Band("B9", ProductData.TYPE_FLOAT32, 1, 1);
        s2SpectralBandIndex = S2Utils.getS2SpectralBandIndex(band);
        assertEquals(9, s2SpectralBandIndex);

        band = new Band("B8A", ProductData.TYPE_FLOAT32, 1, 1);
        s2SpectralBandIndex = S2Utils.getS2SpectralBandIndex(band);
        assertEquals(8, s2SpectralBandIndex);

        band = new Band("B12", ProductData.TYPE_FLOAT32, 1, 1);
        s2SpectralBandIndex = S2Utils.getS2SpectralBandIndex(band);
        assertEquals(12, s2SpectralBandIndex);
    }

    @Test
    public void testGetS2TargetBandName() throws Exception {
        Band band = new Band("B1", ProductData.TYPE_FLOAT32, 1, 1);
        String s2TargetBandName = S2Utils.getS2TargetBandName(RayleighCorrectionOp.BAND_CATEGORIES[0], band);
        assertEquals("taur_B1", s2TargetBandName);

        band = new Band("B9", ProductData.TYPE_FLOAT32, 1, 1);
        s2TargetBandName = S2Utils.getS2TargetBandName(RayleighCorrectionOp.BAND_CATEGORIES[1], band);
        assertEquals("rBRR_B9", s2TargetBandName);

        band = new Band("B8A", ProductData.TYPE_FLOAT32, 1, 1);
        s2TargetBandName = S2Utils.getS2TargetBandName(RayleighCorrectionOp.BAND_CATEGORIES[2], band);
        assertEquals("rtoa_ng_B8A", s2TargetBandName);

        band = new Band("B12", ProductData.TYPE_FLOAT32, 1, 1);
        s2TargetBandName = S2Utils.getS2TargetBandName(RayleighCorrectionOp.BAND_CATEGORIES[3], band);
        assertEquals("rtoa_B12", s2TargetBandName);
    }

    @Test
    public void testTargetS2BandNameMatches() throws Exception {
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
    public void testGetS2SourceBandIndex() throws Exception {
        assertEquals(1, S2Utils.getS2SourceBandIndex(1, "rBRR_B1"));
        assertEquals(3, S2Utils.getS2SourceBandIndex(3, "rBRR_B3"));
        assertEquals(7, S2Utils.getS2SourceBandIndex(7, "rBRR_B7"));
        assertEquals(8, S2Utils.getS2SourceBandIndex(8, "rBRR_B8"));
        assertEquals(9, S2Utils.getS2SourceBandIndex(8, "rBRR_B8A"));
        assertEquals(10, S2Utils.getS2SourceBandIndex(9, "rBRR_B9"));
        assertEquals(13, S2Utils.getS2SourceBandIndex(12, "rBRR_B12"));
    }
}