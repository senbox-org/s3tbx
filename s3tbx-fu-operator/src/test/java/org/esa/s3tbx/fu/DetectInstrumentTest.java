package org.esa.s3tbx.fu;

import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author muhammad.bc .
 */
public class DetectInstrumentTest {

    @Test
    public void testMERIS() {
        Product product = new Product("dummy", "dummy-mer_r", 2, 2);
        product.setProductType("MER_RR__2P");
        assertEquals(Instrument.MERIS, DetectInstrument.getInstrument(product));

        product.setProductType("MER_FRS_2P");
        assertEquals(Instrument.MERIS, DetectInstrument.getInstrument(product));

        product.setProductType("MER_RR__2P");
        assertEquals(Instrument.MERIS, DetectInstrument.getInstrument(product));

        product.setProductType("MER_FR__2P");
        assertEquals(Instrument.MERIS, DetectInstrument.getInstrument(product));
    }


    @Test
    public void testOLCI() {
        Product product = new Product("dummy", "dummy", 2, 2);
        product.addBand("Oa01_reflectance", ProductData.TYPE_INT8);
        product.addBand("Oa02_reflectance", ProductData.TYPE_INT8);
        product.addBand("Oa03_reflectance", ProductData.TYPE_INT8);
        product.addBand("Oa04_reflectance", ProductData.TYPE_INT8);
        product.addBand("Oa05_reflectance", ProductData.TYPE_INT8);
        product.addBand("Oa06_reflectance", ProductData.TYPE_INT8);
        product.addBand("Oa07_reflectance", ProductData.TYPE_INT8);
        assertEquals(Instrument.OLCI, DetectInstrument.getInstrument(product));
    }

    @Test
    public void testS2_A_MSI() {
        Product product = new Product("dummy", "S2_MSI_Level-1C", 2, 2);
        addS2SpacecraftMetadta(product, "Sentinel-2A");
        assertEquals(Instrument.S2A_MSI, DetectInstrument.getInstrument(product));
    }

    @Test
    public void testS2_B_MSI() {
        Product product = new Product("dummy", "S2_MSI_Level-1C", 2, 2);
        addS2SpacecraftMetadta(product, "Sentinel-2B");
        assertEquals(Instrument.S2B_MSI, DetectInstrument.getInstrument(product));
    }

    private void addS2SpacecraftMetadta(Product product, String spacecraftName) {
        final MetadataElement userProduct = new MetadataElement("Level-1C_User_Product");
        final MetadataElement generalInfo = new MetadataElement("General_Info");
        final MetadataElement productInfo = new MetadataElement("Product_Info");
        final MetadataElement datatake = new MetadataElement("Datatake");
        datatake.addAttribute(new MetadataAttribute("SPACECRAFT_NAME", ProductData.createInstance(spacecraftName), true));

        productInfo.addElement(datatake);
        generalInfo.addElement(productInfo);
        userProduct.addElement(generalInfo);
        product.getMetadataRoot().addElement(userProduct);
    }

    // TODO - DISABLED SENSOR
//    @Test
//    public void testLandsat8() {
//        Product l8Product = new Product("dummy", "LANDSAT_8_OLI_TIRS_L1T", 2, 2);
//        assertEquals(Instrument.LANDSAT8, DetectInstrument.getInstrument(l8Product));
//
//        Product l8Class1Product = new Product("dummy", "LANDSAT_8_OLI_TIRS_L1TP", 2, 2);
//        assertEquals(Instrument.LANDSAT8, DetectInstrument.getInstrument(l8Class1Product));
//
//        // TODO - What about L8 containing only OLI?
//        Product l8OliOnlyProduct = new Product("dummy", "LANDSAT_8_OLI_L1T", 2, 2);
//        assertEquals(Instrument.LANDSAT8, DetectInstrument.getInstrument(l8Class1Product));
//    }

    @Test
    public void testMODIS1KM() {
        Product product = new Product("dummy", "dummy-modis1km", 2, 2);
        MetadataElement root = product.getMetadataRoot();
        MetadataElement globalAttributes = new MetadataElement("Global_Attributes");
        globalAttributes.setAttributeString("title", "HMODISA Level-2 Data");
        globalAttributes.setAttributeString("spatialResolution", "1 km");
        root.addElement(globalAttributes);

        assertEquals(Instrument.MODIS, DetectInstrument.getInstrument(product));
    }

    @Test
    public void testMODIS500() {
        Product product = new Product("dummy", "dummy-modis500", 2, 2);
        MetadataElement root = product.getMetadataRoot();
        MetadataElement mph = new MetadataElement("MPH");
        mph.setAttributeString("identifier_product_doi", "10.5067/MODIS/MOD09A1.006");
        root.addElement(mph);

        assertEquals(Instrument.MODIS500, DetectInstrument.getInstrument(product));
    }

    @Test
    public void testSEAWIFS() {
        Product product = new Product("dummy", "dummy-seawifs", 2, 2);
        MetadataElement root = product.getMetadataRoot();
        MetadataElement dsd23 = new MetadataElement("Global_Attributes");
        dsd23.setAttributeString("Title", "SeaWiFS Level-2 Data");
        root.addElement(dsd23);

        assertEquals(Instrument.SEAWIFS, DetectInstrument.getInstrument(product));
    }

    @Test
    public void testCZCS() {
        Product product = new Product("dummy", "dummy-czcs", 2, 2);
        MetadataElement root = product.getMetadataRoot();
        MetadataElement dsd23 = new MetadataElement("Global_Attributes");
        dsd23.setAttributeString("Title", "CZCS Level-2 Data");
        root.addElement(dsd23);

        assertEquals(Instrument.CZCS, DetectInstrument.getInstrument(product));
    }
}