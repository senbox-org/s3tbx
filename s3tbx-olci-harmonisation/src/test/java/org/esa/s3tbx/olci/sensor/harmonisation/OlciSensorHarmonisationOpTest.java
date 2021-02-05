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
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.gpf.OperatorException;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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
    public void testCreateOutputProduct_dontCopyInputs() {
        final Product testProduct = createTestProduct();

        final Product outputProduct = OlciSensorHarmonisationOp.createOutputProduct(testProduct, false);

        assertEquals("test_type_HARM", outputProduct.getProductType());
        assertEquals("test_me_HARM", outputProduct.getName());
        assertEquals(3, outputProduct.getSceneRasterWidth());
        assertEquals(5, outputProduct.getSceneRasterHeight());

        assertEquals("OLCI sensor harmonised L1b", outputProduct.getDescription());
        assertEquals("22-JAN-2021 11:17:31.000000", outputProduct.getStartTime().format());
        assertEquals("22-JAN-2021 11:20:31.000000", outputProduct.getEndTime().format());

        for (int i = 1; i < 22; i++) {
            final Band band = outputProduct.getBand("Oa" + String.format("%02d", i) + "_radiance_harm");
            assertNotNull(band);
            assertEquals(ProductData.TYPE_FLOAT32, band.getDataType());
            assertEquals(Float.NaN, band.getNoDataValue(), 1e-8);
            assertTrue(band.isNoDataValueUsed());

            assertEquals("mW.m-2.sr-1.nm-1", band.getUnit());
            assertEquals(12.5f * i, band.getSpectralWavelength(), 1e-8);
            assertEquals(0.8f * i, band.getSpectralBandwidth(), 1e-8);
            assertEquals("whatever harmonised", band.getDescription());
        }

        final Band detectorIndex = outputProduct.getBand("detector_index");
        assertNull(detectorIndex);

        final TiePointGrid heffalump = outputProduct.getTiePointGrid("heffalump");
        assertNotNull(heffalump);

        final MetadataElement metadataRoot = outputProduct.getMetadataRoot();
        final MetadataElement lambda0 = metadataRoot.getElement("lambda0");
        assertNotNull(lambda0);
    }

    @Test
    public void testCreateOutputProduct_copyInputs() {
        final Product testProduct = createTestProduct();

        final Product outputProduct = OlciSensorHarmonisationOp.createOutputProduct(testProduct, true);
        final Band detectorIndex = outputProduct.getBand("detector_index");
        assertNotNull(detectorIndex);
    }

    @Test
    public void testParseCameraGains() throws IOException {
        final String resource = "# S3A\n" +
                "0.992, 0.997, 1.000, 0.998, 0.988\n" +
                "# S3B\n" +
                "0.991, 0.997, 1.000, 0.996, 0.983";
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(resource.getBytes());
        final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        final double[][] gains = OlciSensorHarmonisationOp.parseCameraGains(reader);
        assertEquals(2, gains.length);

        final double[] s3aGain = gains[0];
        assertEquals(0.992, s3aGain[0], 1e-8);
        assertEquals(0.997, s3aGain[1], 1e-8);

        final double[] s3bGain = gains[1];
        assertEquals(1.0, s3bGain[2], 1e-8);
        assertEquals(0.996, s3bGain[3], 1e-8);
    }

    @Test
    public void testGetCameraIndex() {
        assertEquals(0, OlciSensorHarmonisationOp.getCameraIndex(0));
        assertEquals(0, OlciSensorHarmonisationOp.getCameraIndex(2));
        assertEquals(0, OlciSensorHarmonisationOp.getCameraIndex(739));

        assertEquals(1, OlciSensorHarmonisationOp.getCameraIndex(740));
        assertEquals(1, OlciSensorHarmonisationOp.getCameraIndex(1479));

        assertEquals(2, OlciSensorHarmonisationOp.getCameraIndex(1480));
        assertEquals(2, OlciSensorHarmonisationOp.getCameraIndex(2219));

        assertEquals(3, OlciSensorHarmonisationOp.getCameraIndex(2220));
        assertEquals(3, OlciSensorHarmonisationOp.getCameraIndex(2959));

        assertEquals(4, OlciSensorHarmonisationOp.getCameraIndex(2960));
        assertEquals(4, OlciSensorHarmonisationOp.getCameraIndex(3699));
    }

    @Test
    public void testGetCameraIndex_invalidDetectors() {
        assertEquals(-1, OlciSensorHarmonisationOp.getCameraIndex(-1));
        assertEquals(-1, OlciSensorHarmonisationOp.getCameraIndex(3700));
    }

    @Test
    public void testGetSensorIndex() {
        assertEquals(0, OlciSensorHarmonisationOp.getSensorIndex("S3A_OL_1_EFR____20130621T100921_20130621T101417_20140613T170503_0295_001_002______LN2_D_NR____.SEN3"));
        assertEquals(1, OlciSensorHarmonisationOp.getSensorIndex("S3B_OL_1_EFR____20190420T120914_20190420T121214_20190421T151110_0179_024_237_3420_LN1_O_NT_002.SEN3"));
        assertEquals(2, OlciSensorHarmonisationOp.getSensorIndex("S3C_OL_1_EFR____20190420T120914_20190420T121214_20190421T151110_0179_024_237_3420_LN1_O_NT_002.SEN3"));
        assertEquals(3, OlciSensorHarmonisationOp.getSensorIndex("S3D_OL_1_EFR____20190420T120914_20190420T121214_20190421T151110_0179_024_237_3420_LN1_O_NT_002.SEN3"));
    }

    @Test
    public void testGetSensorIndex_invalidType() {
        try {
            OlciSensorHarmonisationOp.getSensorIndex("S3A_SL_1_RBT____20180809T035343_20180809T035643_20180810T124116_0179_034_218_2520_MAR_O_NT_002.SEN3");
            fail("OperatorException expected");
        } catch (OperatorException expected) {
        }
    }

    @Test
    public void testGetSourceBandName() {
        assertEquals("Oa07_radiance", OlciSensorHarmonisationOp.getSourceBandName("Oa07_radiance_HARM"));
        assertEquals("Oa18_radiance", OlciSensorHarmonisationOp.getSourceBandName("Oa18_radiance_HARM"));
    }

    @Test
    public void testGetSourceBandName_invalidName() {
        try {
            OlciSensorHarmonisationOp.getSourceBandName("Heffalump");
            fail("OperatorException expected");
        } catch (OperatorException expected) {
        }
    }

    @Test
    public void testLoadDetectorWavelengths() {
        final Product testProduct = createTestProduct();

        // testdata contains only data for 3 wavelengths instead of 3700 tb 2021-01-29
        final float[][] wavelengths = OlciSensorHarmonisationOp.loadDetectorWavelengths(testProduct);
        assertEquals(21, wavelengths.length);

        float[] wavelength_vector = wavelengths[3];
        assertEquals(3, wavelength_vector.length);
        assertEquals(5.f, wavelength_vector[0], 1e-8);

        wavelength_vector = wavelengths[7];
        assertEquals(3, wavelength_vector.length);
        assertEquals(10.f, wavelength_vector[1], 1e-8);

        wavelength_vector = wavelengths[19];
        assertEquals(3, wavelength_vector.length);
        assertEquals(23.f, wavelength_vector[2], 1e-8);
    }

    @Test
    public void testGetBandIndex() {
        assertEquals(0, OlciSensorHarmonisationOp.getBandIndex("Oa01_radiance"));
        assertEquals(6, OlciSensorHarmonisationOp.getBandIndex("Oa07_radiance"));
        assertEquals(20, OlciSensorHarmonisationOp.getBandIndex("Oa21_radiance"));
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
            band.setDescription("whatever");
        }

        product.addTiePointGrid(new TiePointGrid("heffalump", 2, 3, 0.5, 0.5, 2, 2, new float[6]));

        final MetadataElement lambda0 = new MetadataElement("lambda0");
        for (int i = 1; i < 22; i++) {
            final MetadataElement waveLengthElement = new MetadataElement("Central wavelengths for band " + i);
            final float[] values = {1.f + i, 2.f + i, 3.f + i};
            final MetadataAttribute centralWavelength = new MetadataAttribute("Central wavelength", ProductData.createInstance(values), true);
            waveLengthElement.addAttribute(centralWavelength);
            lambda0.addElement(waveLengthElement);
        }
        product.getMetadataRoot().addElement(lambda0);

        return product;
    }
}
