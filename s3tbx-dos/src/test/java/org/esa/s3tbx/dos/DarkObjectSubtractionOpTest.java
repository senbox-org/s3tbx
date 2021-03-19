package org.esa.s3tbx.dos;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.Stx;
import org.esa.snap.core.datamodel.StxFactory;
import org.esa.snap.core.datamodel.VirtualBand;
import org.junit.Before;
import org.junit.Test;

import javax.media.jai.RenderedOp;
import java.awt.image.DataBuffer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DarkObjectSubtractionOpTest {

    private Band targetBand1;

    private int width;
    private int height;
    private Product product;

    @Before
    public void setUp() {
        width = 4;
        height = 3;
        product = new Product("p1", "t", width, height);
        targetBand1 = product.addBand("b1", ProductData.TYPE_FLOAT32);

        targetBand1.setDataElems(new float[]{
                2, 3, Float.NaN, 5,
                6, Float.NaN, 8, 9,
                10, 11, 12, 13
        });
    }

    @Test
    public void testSubtractConstantFromImage() {
        Band testBand = new Band("test", targetBand1.getDataType(), width, height);
        final float[] dataElems = new float[]{
                0.02f, 0.03f, Float.NaN, 0.05f,
                0.06f, Float.NaN, 0.08f, 0.09f,
                0.1f, 0.11f, 0.12f, 0.13f
        };
        testBand.setDataElems(dataElems);

        final double constValue = 0.1;
        final RenderedOp subtractedImage = DarkObjectSubtractionOp.subtractConstantFromImage(testBand, constValue);

        assertNotNull(dataElems);
        final DataBuffer subtractedDataBuffer = subtractedImage.getData().getDataBuffer();
        assertNotNull(subtractedDataBuffer);
        for (int i = 0; i < dataElems.length; i++) {
            if (!Float.isNaN((dataElems[i]))) {
                final float subtractedDataElem = subtractedDataBuffer.getElemFloat(i);
                assertEquals(dataElems[i] - constValue, subtractedDataElem, 1.E-6);
            }
        }
    }

    @Test
    public void testSubtractConstantFromImage_withScalingFactor() {
        Band testBand = new Band("test", targetBand1.getDataType(), width, height);
        final double scalingFactor = 1.E-4;
        testBand.setScalingFactor(scalingFactor);
        final float[] dataElems = new float[]{
                200, 300, 400, 500,
                600, 700, 800, 900,
                1000, 1100, 1200, 1300
        };
        testBand.setDataElems(dataElems);

        final double constValue = 0.1;
        final RenderedOp subtractedImage = DarkObjectSubtractionOp.subtractConstantFromImage(testBand, constValue);

        assertNotNull(dataElems);
        final DataBuffer subtractedDataBuffer = subtractedImage.getData().getDataBuffer();
        assertNotNull(subtractedDataBuffer);
        for (int i = 0; i < dataElems.length; i++) {
            if (!Float.isNaN((dataElems[i]))) {
                final float subtractedDataElem = subtractedDataBuffer.getElemFloat(i);
                assertEquals(scalingFactor * dataElems[i] - constValue, subtractedDataElem, 1.E-6);
            }
        }
    }

    @Test
    public void testSubtractConstantFromImage_withScalingFactorAndOffset() {
        Band testBand = new Band("test", targetBand1.getDataType(), width, height);
        final double scalingFactor = 1.E-4;
        final double scalingOffset = 0.01;
        testBand.setScalingFactor(scalingFactor);
        testBand.setScalingOffset(scalingOffset);
        final float[] dataElems = new float[]{
                300, 400, 500, 600,
                700, 800, 900, 1000,
                1100, 1200, 1300, 1400
        };
        testBand.setDataElems(dataElems);

        final double constValue = 0.1;
        final RenderedOp subtractedImage = DarkObjectSubtractionOp.subtractConstantFromImage(testBand, constValue);

        // add subtracted elements to metadata:
        final MetadataElement metadataRoot = product.getMetadataRoot();
        assertNotNull(metadataRoot);
        final MetadataElement darkObjectSpectralValueMetadataElement = new MetadataElement("Dark Object Spectral Value");
        metadataRoot.addElement(darkObjectSpectralValueMetadataElement);

        assertNotNull(dataElems);
        final DataBuffer subtractedDataBuffer = subtractedImage.getData().getDataBuffer();
        assertNotNull(subtractedDataBuffer);
        for (int i = 0; i < dataElems.length; i++) {
            if (!Float.isNaN((dataElems[i]))) {
                final float subtractedDataElem = subtractedDataBuffer.getElemFloat(i);
                assertEquals((scalingFactor * dataElems[i] + scalingOffset) - constValue, subtractedDataElem, 1.E-6);

                final MetadataAttribute dosAttr =
                        new MetadataAttribute("B_" + i,
                                              ProductData.createInstance(new float[]{subtractedDataElem}), true);
                metadataRoot.getElement("Dark Object Spectral Value").addAttribute(dosAttr);
            }
        }

        final MetadataAttribute[] darkObjectSpectralValueAttributes =
                product.getMetadataRoot().getElement("Dark Object Spectral Value").getAttributes();
        assertNotNull(darkObjectSpectralValueAttributes);
        
    }


    @Test
    public void testGetHistogramMinumum() {
        final double offset = 1.3;
        final Product product = new Product("F", "F", 100, 100);
        final Band band = new VirtualBand("V", ProductData.TYPE_FLOAT32, 100, 100, "(X-0.5) + (Y-0.5) + " + offset);
        product.addBand(band);
        final Stx stx = new StxFactory().create(band, ProgressMonitor.NULL);
        final double histoMin = DarkObjectSubtractionOp.getHistogramMinimum(stx);
        System.out.println("histoMin = " + histoMin);
        assertEquals(1.3, histoMin, 1.E-6);
    }

    @Test
    public void testGetHistogramMinumumWithRoiMask() {
        final double offset = 1.3;
        final Product product = new Product("F", "F", 100, 100);
        final Band band = new VirtualBand("V", ProductData.TYPE_FLOAT32, 100, 100, "(X-0.5) + (Y-0.5) + " + offset);
        product.addBand(band);

        Mask mask = new Mask("m", 100, 100, Mask.BandMathsType.INSTANCE);
        Mask.BandMathsType.setExpression(mask, "X >= 10 && Y >= 10");
        product.getMaskGroup().add(mask);

        final Stx stx = new StxFactory().withRoiMask(mask).create(band, ProgressMonitor.NULL);
        final double histoMin = DarkObjectSubtractionOp.getHistogramMinimum(stx);
        System.out.println("histoMinWithRoiMask = " + histoMin);
        assertEquals(21.3, histoMin, 1.E-6);
    }

    @Test
    public void testGetHistogramMinumumAtPercentile() {
        final double offset = 1.3;
        final Product product = new Product("F", "F", 100, 100);
        final Band band = new VirtualBand("V", ProductData.TYPE_FLOAT32, 100, 100, "(X-0.5) + (Y-0.5) + " + offset);
        product.addBand(band);
        final Stx stx = new StxFactory().create(band, ProgressMonitor.NULL);

        double histoMin = DarkObjectSubtractionOp.getHistogramMinAtPercentile(stx, 0);
        System.out.println("histoMin percentile = 0 : " + histoMin);
        assertEquals(1.3, histoMin, 1.E-6);

        histoMin = DarkObjectSubtractionOp.getHistogramMinAtPercentile(stx, 1);
        System.out.println("histoMin percentile = 1 : " + histoMin);
//        assertEquals(1.3, histoMin, 1.E-6);

        histoMin = DarkObjectSubtractionOp.getHistogramMinAtPercentile(stx, 5);
        System.out.println("histoMin percentile = 5 : " + histoMin);
//        assertEquals(1.3, histoMin, 1.E-6);

        histoMin = DarkObjectSubtractionOp.getHistogramMinAtPercentile(stx, 100);
        System.out.println("histoMin percentile = 100 : " + histoMin);
        assertEquals(DarkObjectSubtractionOp.getHistogramMaximum(stx), histoMin, 1.E-6);
    }

}
