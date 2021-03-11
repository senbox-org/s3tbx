package org.esa.s3tbx.mphchl;

import org.esa.s3tbx.olci.radiometry.SensorConstants;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.junit.Test;

import java.lang.reflect.Field;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class MphChlOpTest {

    @Test
    public void testOperatorMetadata() {
        final OperatorMetadata operatorMetadata = MphChlOp.class.getAnnotation(OperatorMetadata.class);
        assertNotNull(operatorMetadata);
        assertEquals("MphChl-beta", operatorMetadata.alias());
        assertEquals("1.0", operatorMetadata.version());
        assertEquals("Mark William Matthews, Daniel Odermatt, Tom Block, Olaf Danne", operatorMetadata.authors());
        assertEquals("(c) 2013, 2014, 2017 by Brockmann Consult", operatorMetadata.copyright());
        assertEquals("This operator computes maximum peak height of chlorophyll (MPH/CHL).",
                     operatorMetadata.description());
    }

    @Test
    public void testSourceProductAnnotation() throws NoSuchFieldException {
        final Field productField = MphChlOp.class.getDeclaredField("sourceProduct");
        assertNotNull(productField);

        final SourceProduct productFieldAnnotation = productField.getAnnotation(SourceProduct.class);
        assertNotNull(productFieldAnnotation);
    }

    @Test
    public void testCyanoMaxValueAnnotation() throws NoSuchFieldException {
        final Field cyanoMaxValueField = MphChlOp.class.getDeclaredField("cyanoMaxValue");

        final Parameter annotation = cyanoMaxValueField.getAnnotation(Parameter.class);
        assertNotNull(annotation);
        assertEquals("1000.0", annotation.defaultValue());
        assertEquals("Maximum chlorophyll, arithmetically higher values are capped.", annotation.description());
    }

    @Test
    public void testChlThreshForFloatFlagAnnotation() throws NoSuchFieldException {
        final Field chlThreshForFloatFlagField = MphChlOp.class.getDeclaredField("chlThreshForFloatFlag");

        final Parameter annotation = chlThreshForFloatFlagField.getAnnotation(Parameter.class);
        assertNotNull(annotation);
        assertEquals("500.0", annotation.defaultValue());
        assertEquals("Chlorophyll threshold, above which all cyanobacteria dominated waters are 'float'.", annotation.description());
    }

    @Test
    public void testExportMphAnnotation() throws NoSuchFieldException {
        final Field exportMphField = MphChlOp.class.getDeclaredField("exportMph");

        final Parameter annotation = exportMphField.getAnnotation(Parameter.class);
        assertNotNull(annotation);
        assertEquals("false", annotation.defaultValue());
        assertEquals("Switch to true to write 'mph' band.", annotation.description());
    }

    @Test
    public void testApplyLowPassFilterAnnotation() throws NoSuchFieldException {
        final Field applyLowPassFilterField = MphChlOp.class.getDeclaredField("applyLowPassFilter");

        final Parameter annotation = applyLowPassFilterField.getAnnotation(Parameter.class);
        assertNotNull(annotation);
        assertEquals("false", annotation.defaultValue());
        assertEquals("Switch to true to apply a 3x3 low-pass filter on the result.", annotation.description());
    }

    @Test
    public void testGetSensorType() {

        Product sourceProduct = new Product("test", "test", 1, 1);
        for (String bandName : MphChlConstants.MERIS_REQUIRED_RADIANCE_BAND_NAMES) {
            sourceProduct.addBand(bandName, ProductData.TYPE_FLOAT32);
        }
        Sensor sensor = MphChlOp.getSensorType(sourceProduct);
        assertEquals(sensor, Sensor.MERIS_3RD);

        sourceProduct = new Product("test2", "test2", 1, 1);
        for (String bandName : MphChlConstants.MERIS_REQUIRED_RADIANCE_BAND_NAMES_4TH) {
            sourceProduct.addBand(bandName, ProductData.TYPE_FLOAT32);
        }
        sensor = MphChlOp.getSensorType(sourceProduct);
        assertEquals(sensor, Sensor.MERIS_4TH);

        sourceProduct = new Product("test3", "test3", 1, 1);
        for (String bandName : MphChlConstants.OLCI_REQUIRED_RADIANCE_BAND_NAMES) {
            sourceProduct.addBand(bandName, ProductData.TYPE_FLOAT32);
        }
        sensor = MphChlOp.getSensorType(sourceProduct);
        assertEquals(sensor, Sensor.OLCI);

        sourceProduct = new Product("test4", "test4", 1, 1);
        for (String bandName : MphChlConstants.MERIS_REQUIRED_BRR_BAND_NAMES) {
            sourceProduct.addBand(bandName, ProductData.TYPE_FLOAT32);
        }
        sourceProduct.addBand(SensorConstants.MERIS_L1B_FLAGS_NAME, ProductData.TYPE_UINT8);
        sensor = MphChlOp.getSensorType(sourceProduct);
        assertEquals(sensor, Sensor.MERIS_3RD);

        sourceProduct = new Product("test5", "test5", 1, 1);
        for (String bandName : MphChlConstants.MERIS_REQUIRED_BRR_BAND_NAMES) {
            sourceProduct.addBand(bandName, ProductData.TYPE_FLOAT32);
        }
        sourceProduct.addBand(SensorConstants.MERIS_4TH_L1B_FLAGS_NAME, ProductData.TYPE_INT16);
        sensor = MphChlOp.getSensorType(sourceProduct);
        assertEquals(sensor, Sensor.MERIS_4TH);

        sourceProduct = new Product("test6", "test6", 1, 1);
        for (String bandName : MphChlConstants.OLCI_REQUIRED_BRR_BAND_NAMES) {
            sourceProduct.addBand(bandName, ProductData.TYPE_FLOAT32);
        }
        sensor = MphChlOp.getSensorType(sourceProduct);
        assertEquals(sensor, Sensor.OLCI);
    }

    @Test
    public void testIsValidL1bSourceProduct() {
        Product sourceProduct = new Product("test", "test", 1, 1);
        assertFalse(MphChlOp.isValidL1bSourceProduct(sourceProduct, Sensor.MERIS_3RD));
        for (String bandName : MphChlConstants.MERIS_REQUIRED_RADIANCE_BAND_NAMES) {
            sourceProduct.addBand(bandName, ProductData.TYPE_FLOAT32);
        }
        assertTrue(MphChlOp.isValidL1bSourceProduct(sourceProduct, Sensor.MERIS_3RD));
        assertFalse(MphChlOp.isValidL1bSourceProduct(sourceProduct, Sensor.OLCI));
        assertFalse(MphChlOp.isValidL1bSourceProduct(sourceProduct, Sensor.MERIS_4TH));

        for (String bandName : MphChlConstants.MERIS_REQUIRED_RADIANCE_BAND_NAMES_4TH) {
            sourceProduct.addBand(bandName, ProductData.TYPE_FLOAT32);
        }
        assertTrue(MphChlOp.isValidL1bSourceProduct(sourceProduct, Sensor.MERIS_4TH));

        for (String bandName : MphChlConstants.OLCI_REQUIRED_RADIANCE_BAND_NAMES) {
            sourceProduct.addBand(bandName, ProductData.TYPE_FLOAT32);
        }
        assertTrue(MphChlOp.isValidL1bSourceProduct(sourceProduct, Sensor.OLCI));
    }

    @Test
    public void testIsValidBrrSourceProduct() {
        Product sourceProduct = new Product("test", "test", 1, 1);
        assertFalse(MphChlOp.isValidBrrSourceProduct(sourceProduct, Sensor.MERIS_3RD));
        for (String bandName : MphChlConstants.MERIS_REQUIRED_BRR_BAND_NAMES) {
            sourceProduct.addBand(bandName, ProductData.TYPE_FLOAT32);
        }
        assertTrue(MphChlOp.isValidBrrSourceProduct(sourceProduct, Sensor.MERIS_3RD));
        assertTrue(MphChlOp.isValidBrrSourceProduct(sourceProduct, Sensor.MERIS_4TH));
        assertFalse(MphChlOp.isValidBrrSourceProduct(sourceProduct, Sensor.OLCI));

        for (String bandName : MphChlConstants.OLCI_REQUIRED_BRR_BAND_NAMES) {
            if (!sourceProduct.containsBand(bandName)) {
                sourceProduct.addBand(bandName, ProductData.TYPE_FLOAT32);
            }
        }
        assertTrue(MphChlOp.isValidBrrSourceProduct(sourceProduct, Sensor.OLCI));
    }

    @Test
    public void testGetSensorFromBrrSourceProduct() {
        String[] sourceBands = MphChlConstants.MERIS_REQUIRED_BRR_BAND_NAMES;
        Sensor sensor = MphChlOp.getSensorFromBrrSourceProduct(sourceBands);
        assertNotNull(sensor);
        assertEquals(Sensor.MERIS_3RD, sensor);

        sourceBands = MphChlConstants.OLCI_REQUIRED_BRR_BAND_NAMES;
        sensor = MphChlOp.getSensorFromBrrSourceProduct(sourceBands);
        assertNotNull(sensor);
        assertEquals(Sensor.OLCI, sensor);

        sourceBands = new String[]{"bla","blubb"};
        sensor = MphChlOp.getSensorFromBrrSourceProduct(sourceBands);
        assertNull(sensor);
    }
}
