package org.esa.s3tbx.ppe;

import com.bc.ceres.glevel.MultiLevelImage;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.internal.TileImpl;
import org.junit.Test;

import java.awt.*;
import java.awt.image.Raster;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;



public class PpeOpTest {

    private static final float[] MERIS_WAVELENGTHS = new float[]{412, 442, 490, 510, 560, 620, 665, 681, 709, 754, 761, 779, 865, 885, 900};
    private String TESTFILENAME ="ppetest.dim";
    private String TESTFILENAME2 ="ppetest2.dim";

    @Test
    public void testPpeOp()  {
        Operator ppeOp = new PpeOp();
        ppeOp.setSourceProduct(createSourceProduct());
        Product targetProduct = ppeOp.getTargetProduct();

        assertEquals(MERIS_WAVELENGTHS.length*2+1,targetProduct.getNumBands());
        assertTrue(targetProduct.containsBand("radiance3_ppe_flag"));
        assertTrue(targetProduct.containsBand("radiance8_ppe_flag"));
        assertTrue(targetProduct.containsBand("radiance8"));
        assertTrue(targetProduct.containsBand("anotherBand"));
        assertTrue(!targetProduct.containsBand("anotherBand_ppe_flag"));
        assertTrue(targetProduct.getBand("radiance3_ppe_flag").isFlagBand());
        assertEquals("radiance1_ppe_flag.PPE applied",targetProduct.getAllFlagNames()[0]);
    }

    @Test
    public void testGetMedian() {
        double[] list = new double[] {1,2,1,1,1};
        assertEquals (1,PpeOp.getMedian(list),1e-10);
        double[] list2 = new double[] {0,0,1,4,2};
        assertEquals (2,PpeOp.getMedian(list2),1e-10);
        double[] list3 = new double[] {5,4,3,2,0};
        assertEquals (3.5,PpeOp.getMedian(list3),1e-10);
    }

    @Test
    public void testGetMAD(){
        double[] list = new double[] {1,6,3,7,11};
        assertEquals (3,PpeOp.getMAD(list),1e-10);
        double[] list2 = new double[] {0,0,3,1,11};
        assertEquals (2,PpeOp.getMAD(list2),1e-10);
        double[] list3 = new double[] {8,0,103,20,21};
        assertEquals (6.5,PpeOp.getMAD(list3),1e-10);

    }
    @Test
    public void getPixelValueTest() throws IOException{
        String testFilePath = PpeOpTest.class.getResource(TESTFILENAME).getFile();
        Product sourceProduct = ProductIO.readProduct(testFilePath);
        RasterDataNode rasterDataNode =sourceProduct.getRasterDataNode("Oa11_radiance");
        Rectangle expectedRect = new Rectangle(0, 0, 20, 20);
        MultiLevelImage image = rasterDataNode.getSourceImage();
        Raster raster = image.getData(expectedRect);
        TileImpl sourceTile = new TileImpl(rasterDataNode, raster);

        Double testValue = PpeOp.getPixelValue(sourceTile,0,21);
        assertEquals(0,testValue,1e-5);

        Double testValue2 = PpeOp.getPixelValue(sourceTile,3,image.getMaxY()+1);
        assertEquals(0,testValue2,1e-5);


        Double testValue3 = PpeOp.getPixelValue(sourceTile,8,-1);
        assertEquals(0,testValue3,1e-5);

        Double testValue4 = PpeOp.getPixelValue(sourceTile,1,1);
        assertEquals(10.36252,testValue4,1e-5);

        Double testValue5 = PpeOp.getPixelValue(sourceTile,10,10);
        assertEquals(10.47061,testValue5,1e-5);
    }

    @Test
    public void getPixelListTest() throws IOException {
        String testFilePath = PpeOpTest.class.getResource(TESTFILENAME).getFile();
        Product sourceProduct = ProductIO.readProduct(testFilePath);


        RasterDataNode rasterDataNode = sourceProduct.getRasterDataNode("Oa11_radiance");
        Rectangle expectedRect = new Rectangle(0, 0, 20, 20);
        MultiLevelImage image = rasterDataNode.getSourceImage();
        Raster raster = image.getData(expectedRect);
        TileImpl sourceTile = new TileImpl(rasterDataNode, raster);

        double[] pixelList1 = PpeOp.getPixelList(0,  0,  sourceTile);
        assertEquals(0,pixelList1[0],1e-5);
        assertEquals(10.39630,pixelList1[2],1e-5);
        assertEquals(10.58545,pixelList1[4],1e-5);

        double[] pixelList2 = PpeOp.getPixelList(0,  19,  sourceTile);
        assertEquals(0,pixelList2[0],1e-5);
        assertEquals(0,pixelList2[1],1e-5);
        assertEquals(10.22066,pixelList2[2],1e-5);
    }


    @Test
    public void testWithProduct() throws IOException {
        Operator ppeOp = new PpeOp();
        String testFilePath = PpeOpTest.class.getResource(TESTFILENAME).getFile();
        Product product = ProductIO.readProduct(testFilePath);
        ppeOp.setSourceProduct(product);
        ppeOp.setParameterDefaultValues();
        Product result = ppeOp.getTargetProduct();
        result.getBand("Oa10_radiance").readRasterDataFully();
        result.getBand("Oa10_radiance_ppe_flag").readRasterDataFully();

        assertEquals(113, result.getNumBands());
        assertEquals(20, result.getSceneRasterHeight());
        assertEquals(20, result.getSceneRasterWidth());
        assertEquals(13.20930, result.getBand("Oa10_radiance").getPixelDouble(0, 3), 1e-5);
        assertEquals(13.20930, result.getBand("Oa10_radiance").getPixelDouble(0, 4), 1e-5);
        assertEquals(13.10102, result.getBand("Oa10_radiance").getPixelDouble(7, 9), 1e-5);
        assertEquals(1, result.getBand("Oa10_radiance_ppe_flag").getPixelDouble(0, 3), 1e-5);
        assertEquals(0, result.getBand("Oa10_radiance_ppe_flag").getPixelDouble(0, 4), 1e-5);
        assertEquals(1, result.getBand("Oa10_radiance_ppe_flag").getPixelDouble(6, 9), 1e-5);
        assertEquals(1, result.getBand("Oa10_radiance_ppe_flag").getPixelDouble(18, 6), 1e-5);
        assertEquals(0, result.getBand("Oa10_radiance_ppe_flag").getPixelDouble(19, 19), 1e-5);

    }

    @Test
    public void testLandTransform() throws IOException {
        Operator ppeOp = new PpeOp();
        String testFilePath = PpeOpTest.class.getResource("ppetest2.dim").getFile();
        Product product = ProductIO.readProduct(testFilePath);
        ppeOp.setSourceProduct(product);
        ppeOp.setParameterDefaultValues();
        Product result = ppeOp.getTargetProduct();
        result.getBand("Oa10_radiance").readRasterDataFully();
        result.getBand("Oa01_radiance").readRasterDataFully();
        result.getBand("Oa01_radiance_ppe_flag").readRasterDataFully();

        assertEquals(40, result.getSceneRasterHeight());
        assertEquals(40, result.getSceneRasterWidth());

        assertEquals(53.10787, result.getBand("Oa10_radiance").getPixelDouble(5, 8), 1e-5);
        assertEquals(53.10787, result.getBand("Oa10_radiance").getPixelDouble(6, 8), 1e-5);
        assertEquals(52.96093, result.getBand("Oa10_radiance").getPixelDouble(5, 7), 1e-5);
        assertEquals(76.95656, result.getBand("Oa01_radiance").getPixelDouble(26, 0), 1e-5);
        assertEquals(1, result.getBand("Oa01_radiance_ppe_flag").getPixelDouble(26, 0), 1e-5);
        assertEquals(1, result.getBand("Oa01_radiance_ppe_flag").getPixelDouble(27, 0), 1e-5);
        assertEquals(0, result.getBand("Oa01_radiance_ppe_flag").getPixelDouble(25, 0), 1e-5);

    }

    private Product createSourceProduct() {
        Product product = new Product("OWT_Input", "REFLEC", 10, 10);
        for (int i = 0; i < MERIS_WAVELENGTHS.length; i++) {
            Band reflecBand = product.addBand("radiance" + (i + 1), ProductData.TYPE_FLOAT32);
            reflecBand.setSpectralWavelength(MERIS_WAVELENGTHS[i]);
            reflecBand.setSpectralBandwidth(10);
            reflecBand.setUnit("mW.m-2.sr-1.nm-1");
        }
        Band anotherBand = product.addBand("anotherBand", ProductData.TYPE_FLOAT32);
        anotherBand.setSpectralWavelength(10000);
        anotherBand.setSpectralBandwidth(10);

        return product;
    }
}
