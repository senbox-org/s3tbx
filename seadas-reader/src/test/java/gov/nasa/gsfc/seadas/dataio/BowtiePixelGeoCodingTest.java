/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package gov.nasa.gsfc.seadas.dataio;

import org.esa.snap.core.dataio.ProductFlipper;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.dataio.ProductSubsetBuilder;
import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.subset.PixelSubsetRegion;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.test.LongTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(LongTestRunner.class)
public class BowtiePixelGeoCodingTest {

    @Test
    public void testTransferGeoCoding() throws URISyntaxException, IOException {
        Product product = ProductIO.readProduct(new File(getClass().getResource("bowtiepixelgeocoding_test_product.L2_sub").toURI()));
        assertTrue(product.getSceneGeoCoding() instanceof BowtiePixelGeoCoding);

        Product targetProduct = new Product("name", "type", product.getSceneRasterWidth(), product.getSceneRasterHeight());

        assertNull(targetProduct.getSceneGeoCoding());
        ProductUtils.copyGeoCoding(product, targetProduct);

        assertNotNull(targetProduct.getSceneGeoCoding());
        assertTrue(targetProduct.getSceneGeoCoding() instanceof BowtiePixelGeoCoding);
    }


    @Test
    public void testLatAndLonAreCorrectlySubsetted() throws URISyntaxException, IOException {
        Product product = ProductIO.readProduct(new File(getClass().getResource("bowtiepixelgeocoding_test_product.L2_sub").toURI()));
        GeoCoding sourcceGeoCoding = product.getSceneGeoCoding();
        assertTrue(sourcceGeoCoding instanceof BowtiePixelGeoCoding);

        ProductSubsetDef subsetDef = new ProductSubsetDef();
        subsetDef.setSubsetRegion(new PixelSubsetRegion(50, 50, 10, 10, 0));
        subsetDef.addNodeName("chlor_a");
        Product targetProduct = product.createSubset(subsetDef, "subset", "");

        GeoCoding targetGeoCoding = targetProduct.getSceneGeoCoding();
        assertNotNull(targetGeoCoding);
        assertTrue(targetGeoCoding instanceof BowtiePixelGeoCoding);
        assertTrue(targetProduct.containsBand("latitude"));
        assertTrue(targetProduct.containsBand("longitude"));

        PixelPos sourcePixelPos = new PixelPos(50.5, 50.5);
        GeoPos expected = sourcceGeoCoding.getGeoPos(sourcePixelPos, new GeoPos());
        PixelPos targetPixelPos = new PixelPos(0.5, 0.5);
        GeoPos actual = targetGeoCoding.getGeoPos(targetPixelPos, new GeoPos());
        assertEquals(expected.getLat(), actual.getLat(), 1.0e-6);
        assertEquals(expected.getLon(), actual.getLon(), 1.0e-6);
    }

    @Test
    public void testScanLineOffset() throws URISyntaxException, IOException {
        Product product = ProductIO.readProduct(new File(getClass().getResource("bowtiepixelgeocoding_test_product.L2_sub").toURI()));

        // latitude values increasing
        BowtiePixelGeoCoding bowtiePixelGeoCoding = (BowtiePixelGeoCoding) product.getSceneGeoCoding();
        assertEquals(0, bowtiePixelGeoCoding.getScanlineOffset());

        // flipped product, latitude values decreasing
        Product flippedProduct = ProductFlipper.createFlippedProduct(product, ProductFlipper.FLIP_BOTH, "f", "f");
        bowtiePixelGeoCoding = (BowtiePixelGeoCoding) flippedProduct.getSceneGeoCoding();
        assertEquals(0, bowtiePixelGeoCoding.getScanlineOffset());

        // small product, just one scan (10 lines)
        testScanlineOffsetOnSubset(product, 0, 10, 0);
        // other small products, with different offsets
        testScanlineOffsetOnSubset(product, 0, 30, 0);
        testScanlineOffsetOnSubset(product, 1, 30, 9);
        testScanlineOffsetOnSubset(product, 2, 30, 8);
        testScanlineOffsetOnSubset(product, 3, 30, 7);
        testScanlineOffsetOnSubset(product, 4, 30, 6);
        testScanlineOffsetOnSubset(product, 5, 30, 5);
        testScanlineOffsetOnSubset(product, 6, 30, 4);
        testScanlineOffsetOnSubset(product, 7, 30, 3);
        testScanlineOffsetOnSubset(product, 8, 30, 2);
        testScanlineOffsetOnSubset(product, 9, 30, 1);
        testScanlineOffsetOnSubset(product, 10, 30, 0);
    }

    private static void testScanlineOffsetOnSubset(Product product, int yStart, int height, int scanlineOffset) throws IOException {
        ProductSubsetDef subsetDef = new ProductSubsetDef();
        subsetDef.setSubsetRegion(new PixelSubsetRegion(0, yStart, product.getSceneRasterWidth(), height, 0));
        Product subsetProduct = ProductSubsetBuilder.createProductSubset(product, subsetDef, "s", "s");
        BowtiePixelGeoCoding bowtiePixelGeoCoding = (BowtiePixelGeoCoding) subsetProduct.getSceneGeoCoding();
        assertEquals("for y=" + yStart + " scanlineOffset", scanlineOffset, bowtiePixelGeoCoding.getScanlineOffset());
    }
}
