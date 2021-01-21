/*
 * Copyright (c) 2021.  Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.s3tbx.olci.sensor.harmonisation;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.OperatorException;
import org.junit.Test;

import static org.junit.Assert.fail;

public class OlciSensorHarmonisationOpTest {

    @Test
    public void testValidateInputProduct() {
        final Product product = createTestProduct();

        OlciSensorHarmonisationOp.validateInputProduct(product);
    }

    @Test
    public void testValidateInputProduct_missingDetectorIndex() {
        final Product product = createTestProduct();
        product.removeBand(product.getBand("detector_index"));

        try {
            OlciSensorHarmonisationOp.validateInputProduct(product);
            fail("OperatorException expected");
        } catch (OperatorException expected) {
        }
    }

    @Test
    public void testValidateInputProduct_missingRadiances() {
        final Product product = createTestProduct();
        product.removeBand(product.getBand("Oa07_radiance"));

        try {
            OlciSensorHarmonisationOp.validateInputProduct(product);
            fail("OperatorException expected");
        } catch (OperatorException expected) {
        }
    }

    private Product createTestProduct() {
        final Product product = new Product("test_me", "test_type", 3, 3);
        product.addBand("detector_index", ProductData.TYPE_INT16);
        for (int i = 1; i < 22; i++) {
            product.addBand("Oa" + String.format("%02d", i) + "_radiance", ProductData.TYPE_FLOAT32);
        }
        return product;
    }
}
