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
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;

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

    private Product createTestProduct() {
        final Product product = new Product("test_me", "test_type", 3, 5);

        for (int i = 1; i < 22; i++) {
            final Band band = product.addBand("Oa" + String.format("%02d", i) + "_radiance", ProductData.TYPE_FLOAT32);
            band.setUnit("mW.m-2.sr-1.nm-1");
            band.setSpectralWavelength(12.5f * i);
            band.setSpectralBandwidth(0.8f * i);
            band.setDescription("whatever");
        }

        return product;
    }
}
