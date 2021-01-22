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

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.gpf.OperatorException;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
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
        Product product = createTestProduct();
        product.removeBand(product.getBand("Oa07_radiance"));

        try {
            OlciSensorHarmonisationOp.validateInputProduct(product);
            fail("OperatorException expected");
        } catch (OperatorException expected) {
        }

        product = createTestProduct();
        product.removeBand(product.getBand("Oa14_radiance"));

        try {
            OlciSensorHarmonisationOp.validateInputProduct(product);
            fail("OperatorException expected");
        } catch (OperatorException expected) {
        }
    }

    @Test
    public void testValidateInputProduct_missingWavelengths() {
        final Product product = createTestProduct();
        final MetadataElement metadataRoot = product.getMetadataRoot();
        final MetadataElement toRemove = metadataRoot.getElement("lambda0");
        metadataRoot.removeElement(toRemove);

        try {
            OlciSensorHarmonisationOp.validateInputProduct(product);
            fail("OperatorException expected");
        } catch (OperatorException expected) {
        }
    }

    @Test
    public void testCreateOutputProduct() {
        final Product testProduct = createTestProduct();

        final Product outputProduct = OlciSensorHarmonisationOp.createOutputProduct(testProduct);

        assertEquals("test_type_HARM", outputProduct.getProductType());
        assertEquals("test_me_HARM", outputProduct.getName());
        assertEquals(3, outputProduct.getSceneRasterWidth());
        assertEquals(5, outputProduct.getSceneRasterHeight());

        assertEquals("OLCI sensor harmonized L1b", outputProduct.getDescription());
        assertEquals("22-JAN-2021 11:17:31.000000", outputProduct.getStartTime().format());
        assertEquals("22-JAN-2021 11:20:31.000000", outputProduct.getEndTime().format());

        for (int i = 1; i < 22; i++) {
            final Band band = outputProduct.getBand("Oa" + String.format("%02d", i) + "_radiance_harm");
            assertNotNull(band);
            assertEquals(ProductData.TYPE_FLOAT32, band.getDataType());
            assertEquals(Float.NaN, band.getNoDataValue(), 1e-8);
            assertTrue(band.isNoDataValueUsed());

            assertEquals("mW.m-2.sr-1.nm-1",band.getUnit());
            assertEquals(12.5f * i, band.getSpectralWavelength(), 1e-8);
            assertEquals(0.8f * i, band.getSpectralBandwidth(), 1e-8);
        }

        final TiePointGrid heffalump = outputProduct.getTiePointGrid("heffalump");
        assertNotNull(heffalump);
    }

    private Product createTestProduct() {
        final Product product = new Product("test_me", "test_type", 3, 5);
        product.setStartTime(ProductData.UTC.create(new Date(1611314251000L), 0));
        product.setEndTime(ProductData.UTC.create(new Date(1611314431000L), 0));

        product.addBand("detector_index", ProductData.TYPE_INT16);
        for (int i = 1; i < 22; i++) {
            final Band band = product.addBand("Oa" + String.format("%02d", i) + "_radiance", ProductData.TYPE_FLOAT32);
            band.setUnit("mW.m-2.sr-1.nm-1");
            band.setSpectralWavelength(12.5f * i);
            band.setSpectralBandwidth(0.8f * i);
        }

        product.addTiePointGrid(new TiePointGrid("heffalump", 2, 3, 0.5, 0.5, 2, 2, new float[6]));

        product.getMetadataRoot().addElement(new MetadataElement("lambda0"));

        return product;
    }
}
