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

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class S2ResponseFunctionTest {

    private S2ResponseFunctions s2RfsA;
    private S2ResponseFunctions s2RfsA_v30;
    private S2ResponseFunctions s2RfsB;
    private S2ResponseFunctions s2RfsB_v30;

    @Before
    public void setUp() {
        s2RfsA = new S2ResponseFunctions("s2a_msi_spectral_responses.csv");
        s2RfsA_v30 = new S2ResponseFunctions();
        s2RfsB = new S2ResponseFunctions("s2b_msi_spectral_responses.csv");
        s2RfsB_v30 = new S2ResponseFunctions("s2b_msi_spectral_responses_v30.csv");
    }

    @Test
    public void testInstance() {
        assertNotNull(s2RfsA);
        assertNotNull(s2RfsA_v30);
        assertNotNull(s2RfsB);
        assertNotNull(s2RfsB_v30);
    }

    @Test
    public void testRecordsS2A() {
        int numRecords = s2RfsA.getSpectralResponseFunctionRecords();
        assertEquals(2301, numRecords);

        List<S2ResponseFunctions.ResponseFunction> objectUnderTest = s2RfsA.getS2ResponseFunctions();
        assertNotNull(objectUnderTest);

        final S2ResponseFunctions.ResponseFunction rf0 = objectUnderTest.get(0);
        assertNotNull(rf0);
        assertEquals(300.0, rf0.getWvl(), 0.0);
        assertNotNull(rf0.getRfs());
        assertEquals(13, rf0.getRfs().length);
        assertEquals(0.0, rf0.getRfs()[0], 0.0);
        assertEquals(0.0, rf0.getRfs()[6], 0.0);
        assertEquals(0.0, rf0.getRfs()[12], 0.0);

        final S2ResponseFunctions.ResponseFunction rf1 = objectUnderTest.get(133);
        assertNotNull(rf1);
        assertEquals(433.0, rf1.getWvl(), 0.0);
        assertNotNull(rf1.getRfs());
        assertEquals(13, rf1.getRfs().length);
        assertEquals(0.0, rf1.getRfs()[1], 0.0);
        assertEquals(0.0, rf1.getRfs()[4], 0.0);
        assertEquals(0.357242315, rf1.getRfs()[0], 0.0);
        assertEquals(0.0, rf1.getRfs()[6], 0.0);
        assertEquals(0.0, rf1.getRfs()[12], 0.0);

        final S2ResponseFunctions.ResponseFunction rf2 = objectUnderTest.get(433);
        assertNotNull(rf2);
        assertEquals(733.0, rf2.getWvl(), 0.0);
        assertNotNull(rf2.getRfs());
        assertEquals(13, rf2.getRfs().length);
        assertEquals(0.0, rf2.getRfs()[0], 0.0);
        assertEquals(0.0, rf2.getRfs()[4], 0.0);
        assertEquals(0.345713766, rf2.getRfs()[5], 0.0);
        assertEquals(0.0, rf2.getRfs()[6], 0.0);
        assertEquals(0.0, rf2.getRfs()[12], 0.0);

        final S2ResponseFunctions.ResponseFunction rf3 = objectUnderTest.get(1990);
        assertNotNull(rf3);
        assertEquals(2290.0, rf3.getWvl(), 0.0);
        assertNotNull(rf3.getRfs());
        assertEquals(13, rf3.getRfs().length);
        assertEquals(0.0, rf3.getRfs()[0], 0.0);
        assertEquals(0.0, rf3.getRfs()[4], 0.0);
        assertEquals(0.0, rf3.getRfs()[6], 0.0);
        assertEquals(0.342091763, rf3.getRfs()[12], 0.0);
    }

    @Test
    public void testRecordsS2A_v30() {
        int numRecords = s2RfsA_v30.getSpectralResponseFunctionRecords();
        assertEquals(2301, numRecords);

        List<S2ResponseFunctions.ResponseFunction> objectUnderTest = s2RfsA_v30.getS2ResponseFunctions();
        assertNotNull(objectUnderTest);

        final S2ResponseFunctions.ResponseFunction rf0 = objectUnderTest.get(0);
        assertNotNull(rf0);
        assertEquals(300.0, rf0.getWvl(), 0.0);
        assertNotNull(rf0.getRfs());
        assertEquals(13, rf0.getRfs().length);
        assertEquals(0.0, rf0.getRfs()[0], 0.0);
        assertEquals(0.0, rf0.getRfs()[6], 0.0);
        assertEquals(0.0, rf0.getRfs()[12], 0.0);

        final S2ResponseFunctions.ResponseFunction rf1 = objectUnderTest.get(133);
        assertNotNull(rf1);
        assertEquals(433.0, rf1.getWvl(), 0.0);
        assertNotNull(rf1.getRfs());
        assertEquals(13, rf1.getRfs().length);
        assertEquals(0.0, rf1.getRfs()[1], 0.0);
        assertEquals(0.0, rf1.getRfs()[4], 0.0);
        assertEquals(0.572819207, rf1.getRfs()[0], 0.0);
        assertEquals(0.0, rf1.getRfs()[6], 0.0);
        assertEquals(0.0, rf1.getRfs()[12], 0.0);

        final S2ResponseFunctions.ResponseFunction rf2 = objectUnderTest.get(433);
        assertNotNull(rf2);
        assertEquals(733.0, rf2.getWvl(), 0.0);
        assertNotNull(rf2.getRfs());
        assertEquals(13, rf2.getRfs().length);
        assertEquals(0.0, rf2.getRfs()[0], 0.0);
        assertEquals(0.0, rf2.getRfs()[4], 0.0);
        assertEquals(0.258066763, rf2.getRfs()[5], 0.0);
        assertEquals(0.0, rf2.getRfs()[6], 0.0);
        assertEquals(0.0, rf2.getRfs()[12], 0.0);

        final S2ResponseFunctions.ResponseFunction rf3 = objectUnderTest.get(1990);
        assertNotNull(rf3);
        assertEquals(2290.0, rf3.getWvl(), 0.0);
        assertNotNull(rf3.getRfs());
        assertEquals(13, rf3.getRfs().length);
        assertEquals(0.0, rf3.getRfs()[0], 0.0);
        assertEquals(0.0, rf3.getRfs()[4], 0.0);
        assertEquals(0.0, rf3.getRfs()[6], 0.0);
        assertEquals(0.342091763, rf3.getRfs()[12], 0.0);
    }


    @Test
    public void testRecordsS2B() {
        int numRecords = s2RfsB.getSpectralResponseFunctionRecords();
        assertEquals(2301, numRecords);

        List<S2ResponseFunctions.ResponseFunction> objectUnderTest = s2RfsB.getS2ResponseFunctions();
        assertNotNull(objectUnderTest);

        final S2ResponseFunctions.ResponseFunction rf1 = objectUnderTest.get(0);
        assertNotNull(rf1);
        assertEquals(300.0, rf1.getWvl(), 0.0);
        assertNotNull(rf1.getRfs());
        assertEquals(13, rf1.getRfs().length);
        assertEquals(0.0, rf1.getRfs()[0], 0.0);
        assertEquals(0.0, rf1.getRfs()[6], 0.0);
        assertEquals(0.0, rf1.getRfs()[12], 0.0);

        final S2ResponseFunctions.ResponseFunction rf2 = objectUnderTest.get(433);
        assertNotNull(rf2);
        assertEquals(733.0, rf2.getWvl(), 0.0);
        assertNotNull(rf2.getRfs());
        assertEquals(13, rf2.getRfs().length);
        assertEquals(0.0, rf2.getRfs()[0], 0.0);
        assertEquals(0.0, rf2.getRfs()[4], 0.0);
        assertEquals(0.782313159, rf2.getRfs()[5], 0.0);
        assertEquals(0.0, rf2.getRfs()[6], 0.0);
        assertEquals(0.0, rf2.getRfs()[12], 0.0);

        final S2ResponseFunctions.ResponseFunction rf3 = objectUnderTest.get(1990);
        assertNotNull(rf3);
        assertEquals(2290.0, rf3.getWvl(), 0.0);
        assertNotNull(rf3.getRfs());
        assertEquals(13, rf3.getRfs().length);
        assertEquals(0.0, rf3.getRfs()[0], 0.0);
        assertEquals(0.0, rf3.getRfs()[4], 0.0);
        assertEquals(0.0, rf3.getRfs()[6], 0.0);
        assertEquals(0.074063524, rf3.getRfs()[12], 0.0);
    }

    @Test
    public void testRecordsS2B_v30() {
        int numRecords = s2RfsB_v30.getSpectralResponseFunctionRecords();
        assertEquals(2301, numRecords);

        List<S2ResponseFunctions.ResponseFunction> objectUnderTest = s2RfsB_v30.getS2ResponseFunctions();
        assertNotNull(objectUnderTest);

        final S2ResponseFunctions.ResponseFunction rf1 = objectUnderTest.get(0);
        assertNotNull(rf1);
        assertEquals(300.0, rf1.getWvl(), 0.0);
        assertNotNull(rf1.getRfs());
        assertEquals(13, rf1.getRfs().length);
        assertEquals(0.0, rf1.getRfs()[0], 0.0);
        assertEquals(0.0, rf1.getRfs()[6], 0.0);
        assertEquals(0.0, rf1.getRfs()[12], 0.0);

        final S2ResponseFunctions.ResponseFunction rf2 = objectUnderTest.get(433);
        assertNotNull(rf2);
        assertEquals(733.0, rf2.getWvl(), 0.0);
        assertNotNull(rf2.getRfs());
        assertEquals(13, rf2.getRfs().length);
        assertEquals(0.0, rf2.getRfs()[0], 0.0);
        assertEquals(0.0, rf2.getRfs()[4], 0.0);
        assertEquals(0.258066763, rf2.getRfs()[5], 0.0);
        assertEquals(0.0, rf2.getRfs()[6], 0.0);
        assertEquals(0.0, rf2.getRfs()[12], 0.0);

        final S2ResponseFunctions.ResponseFunction rf3 = objectUnderTest.get(1990);
        assertNotNull(rf3);
        assertEquals(2290.0, rf3.getWvl(), 0.0);
        assertNotNull(rf3.getRfs());
        assertEquals(13, rf3.getRfs().length);
        assertEquals(0.0, rf3.getRfs()[0], 0.0);
        assertEquals(0.0, rf3.getRfs()[4], 0.0);
        assertEquals(0.0, rf3.getRfs()[6], 0.0);
        assertEquals(0.342091763, rf3.getRfs()[12], 0.0);
    }

    @Test
    public void testGetS2TrueWavelengths() {
        final double[] s2TrueWavelengths = S2Utils.getS2TrueWavelengths();
        assertNotNull(s2TrueWavelengths);
        assertEquals(13, s2TrueWavelengths.length);
    }
}