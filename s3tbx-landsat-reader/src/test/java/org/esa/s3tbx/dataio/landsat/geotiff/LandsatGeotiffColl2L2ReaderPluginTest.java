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

package org.esa.s3tbx.dataio.landsat.geotiff;

import org.junit.Test;

import static org.esa.s3tbx.dataio.landsat.geotiff.LandsatGeotiffColl2L2ReaderPlugin.endsWithDefaultExtension;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Marco Peters
 */
public class LandsatGeotiffColl2L2ReaderPluginTest {

    @Test
    public void testEndsWithDefaultExtension() {
        assertTrue(endsWithDefaultExtension("LC08_L2SP_140041_20130503_20200912_02_T1_MTL.txt"));
        assertTrue(endsWithDefaultExtension("LT05_L2SP_230056_20100501_20200824_02_T2.tar"));
        assertTrue(endsWithDefaultExtension("LE07_L2SP_231055_20100329_20200911_02_T1.tar"));
        assertFalse(endsWithDefaultExtension("LC08_L2SP_140041_20130503_20200912_02_T1_SR_B5.TIF"));
        assertFalse(endsWithDefaultExtension("LE07_L2SP_231055_20100329_20200911_02_T1_thumb_large.jpeg"));
    }
}