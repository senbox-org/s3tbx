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

package org.esa.s3tbx.olci.anomaly.flagging;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.gpf.OperatorException;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class OlciAnomalyFlaggingOpTest {

    @Test
    public void testToReflectance() {
        double invSolarFlux = 1.0 / 1284.2445;
        double invCosSZA = 1.0 / Math.cos(Math.toRadians(37.220443));

        double reflectance = OlciAnomalyFlaggingOp.toReflectance(46.044, invSolarFlux, invCosSZA);
        assertEquals(0.14144603301041955, reflectance, 1e-8);

        invSolarFlux = 1.0 / 1518.9329;
        invCosSZA = 1.0 / Math.cos(Math.toRadians(30.307082));
        reflectance = OlciAnomalyFlaggingOp.toReflectance(46.044, invSolarFlux, invCosSZA);
        assertEquals(0.1103077168290055, reflectance, 1e-8);
    }

    @Test
    public void testValidateInputProduct() {
        final Product testProduct = createTestProduct();

        // expect no exception here tb 2021-04-08
        OlciAnomalyFlaggingOp.validateInputProduct(testProduct);
    }

    @Test
    public void testValidateInputProduct_missingRadiance() {
        Product testProduct = createTestProduct();
        testProduct.removeBand(testProduct.getBand("Oa03_radiance"));

        try {
            OlciAnomalyFlaggingOp.validateInputProduct(testProduct);
            fail("OperatorException expected");
        } catch (OperatorException expected) {
        }

        testProduct = createTestProduct();
        testProduct.removeBand(testProduct.getBand("Oa17_radiance"));

        try {
            OlciAnomalyFlaggingOp.validateInputProduct(testProduct);
            fail("OperatorException expected");
        } catch (OperatorException expected) {
        }
    }

    @Test
    public void testValidateInputProduct_missingSolarFlux() {
        Product testProduct = createTestProduct();
        testProduct.removeBand(testProduct.getBand("solar_flux_band_4"));

        try {
            OlciAnomalyFlaggingOp.validateInputProduct(testProduct);
            fail("OperatorException expected");
        } catch (OperatorException expected) {
        }

        testProduct = createTestProduct();
        testProduct.removeBand(testProduct.getBand("solar_flux_band_18"));

        try {
            OlciAnomalyFlaggingOp.validateInputProduct(testProduct);
            fail("OperatorException expected");
        } catch (OperatorException expected) {
        }
    }

    @Test
    public void testValidateInputProduct_missingSZA() {
        Product testProduct = createTestProduct();
        testProduct.removeTiePointGrid(testProduct.getTiePointGrid("SZA"));

        try {
            OlciAnomalyFlaggingOp.validateInputProduct(testProduct);
            fail("OperatorException expected");
        } catch (OperatorException expected) {
        }
    }

    @Test
    public void testValidateInputProduct_missingWavelength() {
        Product testProduct = createTestProduct();
        testProduct.removeBand(testProduct.getBand("lambda0_band_5"));

        try {
            OlciAnomalyFlaggingOp.validateInputProduct(testProduct);
            fail("OperatorException expected");
        } catch (OperatorException expected) {
        }

        testProduct = createTestProduct();
        testProduct.removeBand(testProduct.getBand("lambda0_band_21"));

        try {
            OlciAnomalyFlaggingOp.validateInputProduct(testProduct);
            fail("OperatorException expected");
        } catch (OperatorException expected) {
        }
    }

    @Test
    public void testValidateInputProduct_missingAltitude() {
        Product testProduct = createTestProduct();
        testProduct.removeBand(testProduct.getBand("altitude"));

        try {
            OlciAnomalyFlaggingOp.validateInputProduct(testProduct);
            fail("OperatorException expected");
        } catch (OperatorException expected) {
        }
    }

    @Test
    public void testCreateOutputProduct() {
        final Product testProduct = createTestProduct();

        final Product outputProduct = OlciAnomalyFlaggingOp.createOutputProduct(testProduct, false);
        assertEquals("test_type_ANOM_FLAG", outputProduct.getProductType());
        assertEquals("test_me_ANOM_FLAG", outputProduct.getName());
        assertEquals(3, outputProduct.getSceneRasterWidth());
        assertEquals(5, outputProduct.getSceneRasterHeight());

        assertEquals("OLCI anomaly flagged L1b", outputProduct.getDescription());
        assertEquals("24-JAN-2021 18:50:51.000000", outputProduct.getStartTime().format());
        assertEquals("25-JAN-2021 22:40:31.000000", outputProduct.getEndTime().format());

        for (int i = 1; i < 22; i++) {
            assertNotNull(outputProduct.getBand("Oa" + String.format("%02d", i) + "_radiance"));
            assertNotNull(outputProduct.getBand("solar_flux_band_" + Integer.toString(i)));
            assertNotNull(outputProduct.getBand("lambda0_band_" + Integer.toString(i)));
        }

        assertNotNull(outputProduct.getTiePointGrid("SZA"));

        final Band anomalyFlags = outputProduct.getBand("anomaly_flags");
        assertEquals(ProductData.TYPE_INT8, anomalyFlags.getDataType());
        assertFalse(anomalyFlags.isNoDataValueUsed());
        assertNull(anomalyFlags.getUnit());
        assertEquals(0, anomalyFlags.getSpectralWavelength(), 1e-8);
        assertEquals(0, anomalyFlags.getSpectralBandwidth(), 1e-8);
        assertEquals("Flags indicating OLCI data anomalies", anomalyFlags.getDescription());

        final FlagCoding flagCoding = anomalyFlags.getFlagCoding();
        assertEquals(2, flagCoding.getFlagNames().length);
        assertEquals(1, flagCoding.getFlagMask("ANOM_SPECTRAL_MEASURE"));
        assertEquals(2, flagCoding.getFlagMask("ALT_OUT_OF_RANGE"));
    }

    @Test
    public void testCreateOutputProduct_withSlopeBands() {
        final Product testProduct = createTestProduct();

        final Product outputProduct = OlciAnomalyFlaggingOp.createOutputProduct(testProduct, true);

        final Band maxSpectralSlope = outputProduct.getBand("max_spectral_slope");
        assertEquals(ProductData.TYPE_FLOAT32, maxSpectralSlope.getDataType());
        assertEquals(Float.NaN, maxSpectralSlope.getNoDataValue(), 1e-8);
        assertTrue(maxSpectralSlope.isNoDataValueUsed());
        assertEquals("1/nm", maxSpectralSlope.getUnit());
        assertEquals(0, maxSpectralSlope.getSpectralWavelength(), 1e-8);
        assertEquals(0, maxSpectralSlope.getSpectralBandwidth(), 1e-8);
        assertEquals("Absolute value of maximal spectral slope for bands 1-12, 16-18, 21", maxSpectralSlope.getDescription());

        final Band bandIndex = outputProduct.getBand("max_slope_band_index");
        assertEquals(ProductData.TYPE_INT8, bandIndex.getDataType());
        assertEquals(-1, bandIndex.getNoDataValue(), 1e-8);
        assertTrue(bandIndex.isNoDataValueUsed());
        assertNull(bandIndex.getUnit());
        assertEquals(0, bandIndex.getSpectralWavelength(), 1e-8);
        assertEquals(0, bandIndex.getSpectralBandwidth(), 1e-8);
        assertEquals("Band index where the maximal slope is detected", bandIndex.getDescription());
    }

    private Product createTestProduct() {
        final Product product = new Product("test_me", "test_type", 3, 5);
        product.setStartTime(ProductData.UTC.create(new Date(1611514251000L), 0));
        product.setEndTime(ProductData.UTC.create(new Date(1611614431000L), 0));

        for (int i = 1; i < 22; i++) {
            final Band band = product.addBand("Oa" + String.format("%02d", i) + "_radiance", ProductData.TYPE_FLOAT32);
            band.setUnit("mW.m-2.sr-1.nm-1");
            band.setSpectralWavelength(12.5f * i);
            band.setSpectralBandwidth(0.8f * i);
            band.setDescription("whatever");

            product.addBand("solar_flux_band_" + Integer.toString(i), ProductData.TYPE_FLOAT32);
            product.addBand("lambda0_band_" + Integer.toString(i), ProductData.TYPE_FLOAT32);
        }

        product.addBand("altitude", ProductData.TYPE_FLOAT32);

        final TiePointGrid sza = new TiePointGrid("SZA", 2, 2, 0, 0, 5, 5, new float[]{1, 2, 3, 4});
        product.addTiePointGrid(sza);

        return product;
    }
}
