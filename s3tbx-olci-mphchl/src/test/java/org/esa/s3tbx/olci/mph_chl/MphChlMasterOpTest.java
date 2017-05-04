package org.esa.s3tbx.olci.mph_chl;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class MphChlMasterOpTest {
    @Before
    public void setUp() {
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(new MphChlMasterOp.Spi());
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(new MphChlOp.Spi());
    }

    @After
    public void tearDown() {
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(new MphChlMasterOp.Spi());
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(new MphChlOp.Spi());
    }

    @Test
    public void testOperatorMetadata() {
        final OperatorMetadata operatorMetadata = MphChlMasterOp.class.getAnnotation(OperatorMetadata.class);
        assertNotNull(operatorMetadata);
        assertEquals("OlciMphChl", operatorMetadata.alias());
        assertEquals("1.0", operatorMetadata.version());
        assertEquals("Mark William Matthews, Daniel Odermatt, Tom Block, Olaf Danne", operatorMetadata.authors());
        assertEquals("(c) 2013, 2014, 2017 by Brockmann Consult", operatorMetadata.copyright());
        assertEquals("This operator computes maximum peak height of chlorophyll (MPH/CHL) from OLCI.",
                     operatorMetadata.description());
    }

    @Test
    public void testSourceProductAnnotation() throws NoSuchFieldException {
        final Field productField = MphChlMasterOp.class.getDeclaredField("sourceProduct");
        assertNotNull(productField);

        final SourceProduct productFieldAnnotation = productField.getAnnotation(SourceProduct.class);
        assertNotNull(productFieldAnnotation);
    }

    @Test
    public void testInvalidPixelExpressionAnnotation() throws NoSuchFieldException {
        final Field validPixelField = MphChlMasterOp.class.getDeclaredField("validPixelExpression");

        final Parameter annotation = validPixelField.getAnnotation(Parameter.class);
        assertNotNull(annotation);
        assertEquals("not (quality_flags.land or quality_flags.invalid)", annotation.defaultValue());
        assertEquals("Expression defining pixels considered for processing.", annotation.description());
    }

    @Test
    public void testCyanoMaxValueAnnotation() throws NoSuchFieldException {
        final Field cyanoMaxValueField = MphChlMasterOp.class.getDeclaredField("cyanoMaxValue");

        final Parameter annotation = cyanoMaxValueField.getAnnotation(Parameter.class);
        assertNotNull(annotation);
        assertEquals("1000.0", annotation.defaultValue());
        assertEquals("Maximum chlorophyll, arithmetically higher values are capped.", annotation.description());
    }

    @Test
    public void testChlThreshForFloatFlagAnnotation() throws NoSuchFieldException {
        final Field chlThreshForFloatFlagField = MphChlMasterOp.class.getDeclaredField("chlThreshForFloatFlag");

        final Parameter annotation = chlThreshForFloatFlagField.getAnnotation(Parameter.class);
        assertNotNull(annotation);
        assertEquals("500.0", annotation.defaultValue());
        assertEquals("Chlorophyll threshold, above which all cyanobacteria dominated waters are 'float.", annotation.description());
    }

    @Test
    public void testExportMphAnnotation() throws NoSuchFieldException {
        final Field exportMphField = MphChlMasterOp.class.getDeclaredField("exportMph");

        final Parameter annotation = exportMphField.getAnnotation(Parameter.class);
        assertNotNull(annotation);
        assertEquals("false", annotation.defaultValue());
        assertEquals("Switch to true to write 'mph' band.", annotation.description());
    }

    @Test
    public void testApplyLowPassFilterAnnotation() throws NoSuchFieldException {
        final Field applyLowPassFilterField = MphChlMasterOp.class.getDeclaredField("applyLowPassFilter");

        final Parameter annotation = applyLowPassFilterField.getAnnotation(Parameter.class);
        assertNotNull(annotation);
        assertEquals("false", annotation.defaultValue());
        assertEquals("Switch to true to apply a 3x3 low-pass filter on the result.", annotation.description());
    }

    @Test
    public void testComputeMphChlProduct() throws IOException {
        final Product brrProduct = OlciBrrProduct.create();

        final Product mphChlPixelProduct = GPF.createProduct("MphChl", GPF.NO_PARAMS, brrProduct);
        assertNotNull(mphChlPixelProduct);

        HashMap<String, Object> mphChlParams = new HashMap<>();
        mphChlParams.put("applyLowPassFilter", false);
        final Product mphChlProduct = GPF.createProduct("MphChl", mphChlParams, brrProduct);
        assertNotNull(mphChlProduct);

        final Band chlBand = mphChlProduct.getBand("chl");
        assertNotNull(chlBand);
        final Band chlPixelBand = mphChlPixelProduct.getBand("chl");
        assertNotNull(chlPixelBand);

        assertEquals(chlBand.getSampleFloat(0, 0), chlPixelBand.getSampleFloat(0, 0), 1e-8);
        assertEquals(chlBand.getSampleFloat(0, 1), chlPixelBand.getSampleFloat(0, 1), 1e-8);
        assertEquals(chlBand.getSampleFloat(1, 0), chlPixelBand.getSampleFloat(1, 0), 1e-8);
        assertEquals(chlBand.getSampleFloat(1, 1), chlPixelBand.getSampleFloat(1, 1), 1e-8);

    }
}
