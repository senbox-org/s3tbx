package org.esa.s3tbx.mphchl;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.awt.Color;
import java.lang.reflect.Field;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class MphChlBasisBetaOpTest {
    private MphChlBasisBetaOp mphChlOp;

    @Before
    public void setUp() {
        mphChlOp = new MphChlBasisBetaOp();
    }

    @Test
    public void testOperatorMetadata() {
        final OperatorMetadata operatorMetadata = MphChlBasisBetaOp.class.getAnnotation(OperatorMetadata.class);
        assertNotNull(operatorMetadata);
        assertEquals("MphChlBasis-beta", operatorMetadata.alias());
        assertEquals("1.0", operatorMetadata.version());
        assertEquals("Mark William Matthews, Daniel Odermatt, Tom Block, Olaf Danne", operatorMetadata.authors());
        assertEquals("(c) 2013, 2014, 2017 by Brockmann Consult", operatorMetadata.copyright());
        assertEquals("Computes maximum peak height of chlorophyll. Basis class, contains sensor-independent parts.",
                     operatorMetadata.description());
    }

    @Test
    public void testSourceProductAnnotation() throws NoSuchFieldException {
        final Field productField = MphChlBasisBetaOp.class.getDeclaredField("sourceProduct");
        assertNotNull(productField);

        final SourceProduct productFieldAnnotation = productField.getAnnotation(SourceProduct.class);
        assertNotNull(productFieldAnnotation);
        assertEquals("Name", productFieldAnnotation.alias());
    }

    @Test
    public void testInvalidPixelExpressionAnnotation() throws NoSuchFieldException {
        final Field validPixelField = MphChlBasisBetaOp.class.getDeclaredField("validPixelExpression");

        final Parameter annotation = validPixelField.getAnnotation(Parameter.class);
        assertNotNull(annotation);
        assertEquals("", annotation.defaultValue());
        assertEquals("Expression defining pixels considered for processing.", annotation.description());
    }


    @Test
    public void testCyanoMaxValueAnnotation() throws NoSuchFieldException {
        final Field cyanoMaxValueField = MphChlBasisBetaOp.class.getDeclaredField("cyanoMaxValue");

        final Parameter annotation = cyanoMaxValueField.getAnnotation(Parameter.class);
        assertNotNull(annotation);
        assertEquals("1000.0", annotation.defaultValue());
        assertEquals("Maximum chlorophyll, arithmetically higher values are capped.", annotation.description());
    }

    @Test
    public void testExportAddBandsAnnotation() throws NoSuchFieldException {
        final Field exportAddBandsField = MphChlBasisBetaOp.class.getDeclaredField("exportAddBands");

        final Parameter annotation = exportAddBandsField.getAnnotation(Parameter.class);
        assertNotNull(annotation);
        assertEquals("false", annotation.defaultValue());
        assertEquals("Add additional chl bands.", annotation.description());
    }

    @Test
    public void testChlThreshForFloatFlagAnnotation() throws NoSuchFieldException {
        final Field chlThreshForFloatFlagField = MphChlBasisBetaOp.class.getDeclaredField("chlThreshForFloatFlag");

        final Parameter annotation = chlThreshForFloatFlagField.getAnnotation(Parameter.class);
        assertNotNull(annotation);
        assertEquals("350.0", annotation.defaultValue());
        assertEquals("Chlorophyll threshold, above which all cyanobacteria dominated waters are 'float.", annotation.description());
    }

    @Test
    public void testExportMphAnnotation() throws NoSuchFieldException {
        final Field exportMphField = MphChlBasisBetaOp.class.getDeclaredField("exportMph");

        final Parameter annotation = exportMphField.getAnnotation(Parameter.class);
        assertNotNull(annotation);
        assertEquals("false", annotation.defaultValue());
        assertEquals("Switch to true to write 'mph' band.", annotation.description());
    }

    @Test
    public void testConfigureTargetProduct() {
        final TestProductConfigurer productConfigurer = new TestProductConfigurer();

        mphChlOp.configureTargetProduct(productConfigurer);

        final Product targetProduct = productConfigurer.getTargetProduct();
        assertNotNull(targetProduct);

        final Band chlBand = targetProduct.getBand("chl");
        assertNotNull(chlBand);
        assertEquals(ProductData.TYPE_FLOAT32, chlBand.getDataType());
        assertEquals("mg/m^3", chlBand.getUnit());
        assertEquals(Double.NaN, chlBand.getGeophysicalNoDataValue(), 1e-8);

        final Band immersed_cyanobacteriaBand = targetProduct.getBand("immersed_cyanobacteria");
        assertNotNull(immersed_cyanobacteriaBand);
        assertEquals(ProductData.TYPE_INT8, immersed_cyanobacteriaBand.getDataType());

        final Band floating_cyanobacteriaBand = targetProduct.getBand("floating_cyanobacteria");
        assertNotNull(floating_cyanobacteriaBand);
        assertEquals(ProductData.TYPE_INT8, floating_cyanobacteriaBand.getDataType());

        final Band floating_vegetationBand = targetProduct.getBand("floating_vegetation");
        assertNotNull(floating_vegetationBand);
        assertEquals(ProductData.TYPE_INT8, floating_vegetationBand.getDataType());

        final Band flagBand = targetProduct.getBand("mph_chl_flags");
        Assert.assertNotNull(flagBand);
        assertEquals(ProductData.TYPE_INT8, flagBand.getDataType());

        assertTrue(productConfigurer.isCopyGeoCodingCalled());

        final FlagCoding flagCoding = targetProduct.getFlagCodingGroup().get("mph_chl_flags");
        assertNotNull(flagCoding);
        FlagCoding bandFlagcoding = flagBand.getFlagCoding();
        assertSame(flagCoding, bandFlagcoding);

        final MetadataAttribute cyanoFlag = flagCoding.getFlag("mph_cyano");
        assertEquals("mph_cyano", cyanoFlag.getName());
        assertEquals("Cyanobacteria dominated waters", cyanoFlag.getDescription());
        assertEquals(1, cyanoFlag.getData().getElemInt());

        final MetadataAttribute floatingFlag = flagCoding.getFlag("mph_floating");
        assertNotNull(floatingFlag);
        assertEquals("mph_floating", floatingFlag.getName());
        assertEquals("Floating vegetation or cyanobacteria on water surface", floatingFlag.getDescription());
        assertEquals(2, floatingFlag.getData().getElemInt());

        final MetadataAttribute adjacencyFlag = flagCoding.getFlag("mph_adjacency");
        assertNotNull(adjacencyFlag);
        assertEquals("mph_adjacency", adjacencyFlag.getName());
        assertEquals("Pixel suspect of adjacency effects", adjacencyFlag.getDescription());
        assertEquals(4, adjacencyFlag.getData().getElemInt());

        final ProductNodeGroup<Mask> maskGroup = targetProduct.getMaskGroup();
        assertNotNull(maskGroup);
        final Mask cyanoMask = maskGroup.get("mph_cyano");
        assertNotNull(cyanoMask);
        assertEquals("Cyanobacteria dominated waters", cyanoMask.getDescription());
        assertEquals(Color.cyan, cyanoMask.getImageColor());
        assertEquals(0.5f, cyanoMask.getImageTransparency(), 1e-8);

        final Mask floatingMask = maskGroup.get("mph_floating");
        assertNotNull(floatingMask);
        assertEquals("Floating vegetation or cyanobacteria on water surface", floatingMask.getDescription());
        assertEquals(Color.green, floatingMask.getImageColor());
        assertEquals(0.5f, floatingMask.getImageTransparency(), 1e-8);

        final Mask adjacencyMask = maskGroup.get("mph_adjacency");
        assertNotNull(adjacencyMask);
        assertEquals("Pixel suspect of adjacency effects", adjacencyMask.getDescription());
        assertEquals(Color.red, adjacencyMask.getImageColor());
        assertEquals(0.5f, adjacencyMask.getImageTransparency(), 1e-8);
    }

    @Test
    public void testConfigureTargetProduct_withMphBand() {
        final TestProductConfigurer productConfigurer = new TestProductConfigurer();

        mphChlOp.exportMph = true;
        mphChlOp.configureTargetProduct(productConfigurer);

        final Product targetProduct = productConfigurer.getTargetProduct();
        assertNotNull(targetProduct);

        final Band mphBand = targetProduct.getBand("mph");
        assertNotNull(mphBand);
        assertEquals(ProductData.TYPE_FLOAT32, mphBand.getDataType());
        assertEquals("dl", mphBand.getUnit());
        assertEquals(Double.NaN, mphBand.getGeophysicalNoDataValue(), 1e-8);
    }

    @Test
    public void testConfigureTargetSample() {
        final TestTargetSampleConfigurer sampleConfigurer = new TestTargetSampleConfigurer();

        mphChlOp.configureTargetSamples(sampleConfigurer);

        final HashMap<Integer, String> sampleMap = sampleConfigurer.getSampleMap();
        assertEquals(5, sampleMap.size());
        assertEquals("chl", sampleMap.get(0));
        assertEquals("mph_chl_flags", sampleMap.get(1));
        assertEquals("immersed_cyanobacteria", sampleMap.get(2));
        assertEquals("floating_cyanobacteria", sampleMap.get(3));
        assertEquals("floating_vegetation", sampleMap.get(4));
    }

    @Test
    public void testConfigureTargetSample_withMph() {
        final TestTargetSampleConfigurer sampleConfigurer = new TestTargetSampleConfigurer();

        mphChlOp.exportMph = true;
        mphChlOp.configureTargetSamples(sampleConfigurer);

        final HashMap<Integer, String> sampleMap = sampleConfigurer.getSampleMap();
        assertEquals(6, sampleMap.size());
        assertEquals("chl", sampleMap.get(0));
        assertEquals("mph_chl_flags", sampleMap.get(1));
        assertEquals("immersed_cyanobacteria", sampleMap.get(2));
        assertEquals("floating_cyanobacteria", sampleMap.get(3));
        assertEquals("floating_vegetation", sampleMap.get(4));
        assertEquals("mph", sampleMap.get(5));
    }

}
