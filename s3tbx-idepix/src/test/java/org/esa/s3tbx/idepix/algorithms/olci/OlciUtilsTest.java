/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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
 */

package org.esa.s3tbx.idepix.algorithms.olci;

import org.esa.s3tbx.processor.rad2refl.Rad2ReflConstants;
import org.esa.snap.core.datamodel.*;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OlciUtilsTest {

    @BeforeClass
    public static void beforeClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testAreAllRequiredL1bBandsAvailable() {
        final Product testProduct = createTestProduct(2, 2);
        for (String bandname : Rad2ReflConstants.OLCI_RAD_BAND_NAMES) {
            testProduct.addBand(bandname, ProductData.TYPE_FLOAT32);
        }
        assertFalse(OlciUtils.areAllRequiredL1bBandsAvailable(testProduct));
        for (String bandname : Rad2ReflConstants.OLCI_SOLAR_FLUX_BAND_NAMES) {
            testProduct.addBand(bandname, ProductData.TYPE_FLOAT32);
        }
        assertFalse(OlciUtils.areAllRequiredL1bBandsAvailable(testProduct));
        testProduct.addTiePointGrid(new TiePointGrid("SZA", 2, 2, 0, 0, 1, 1));
        assertFalse(OlciUtils.areAllRequiredL1bBandsAvailable(testProduct));
        testProduct.addTiePointGrid(new TiePointGrid("OZA", 2, 2, 0, 0, 1, 1));
        assertFalse(OlciUtils.areAllRequiredL1bBandsAvailable(testProduct));
        testProduct.addBand("quality_flags", ProductData.TYPE_INT32);
        assertTrue(OlciUtils.areAllRequiredL1bBandsAvailable(testProduct));
    }

//    private Band createTestBand(int type, int w, int h) {
//        final double mean = (w * h - 1.0) / 2.0;
//        return createTestBand(type, w, h, mean);
//    }
//
//    private Band createTestBand(int type, int w, int h, double offset) {
//        final Product product = createTestProduct(w, h);
//        final Band band = new VirtualBand("V", type, w, h, "(Y-0.5) * " + w + " + (X-0.5) - " + offset);
//        product.addBand(band);
//        return band;
//    }

    private Product createTestProduct(int w, int h) {
        return new Product("F", "F", w, h);
    }

}
