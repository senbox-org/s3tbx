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

package org.esa.s3tbx.dataio.s3.meris;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Marco Peters
 */
public class MerisLevel1ProductPlugInTest {

    @Test
    public void dirNamePatternMatching() {
        final MerisLevel1ProductPlugIn plugIn = new MerisLevel1ProductPlugIn();
        assertTrue(plugIn.isValidMerisDirectoryName("ENV_ME_1_FRG____20091208T101751_20091208T102057_________________0186_085_008______DSI_R_NT____.SEN3"));
        assertTrue(plugIn.isValidMerisDirectoryName("ENV_ME_1_RRG____20111230T102706_20111230T111042_________________2616_110_123______DSI_R_NT____.SEN3"));
        assertTrue(plugIn.isValidMerisDirectoryName("EN1_MDSI_MER_RR__1P_20040103T012931_20040103T021310_009633_0060_20180610T222321_0100"));
        assertTrue(plugIn.isValidMerisDirectoryName("EN1_MDSI_MER_FRS_1P_20040103T034519_20040103T034859_009634_0061_20180413T122431_0100"));
    }

}