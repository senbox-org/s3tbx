package org.esa.s3tbx.c2rcc.msi;

import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * @author Marco Peters
 */
public class MsiProductSignatureTest {
    private static final String[] EXPECTED_REFLEC_BANDS = {
            "rhow_B" + 1, "rhow_B" + 2, "rhow_B" + 3, "rhow_B" + 4, "rhow_B" + 5,
            "rhow_B" + 6, "rhow_B" + 7, "rhow_B" + 8 + "A"};
    private static final String[] EXPECTED_NORM_REFLEC_BANDS = {
            "rhown_B" + 1, "rhown_B" + 2, "rhown_B" + 3, "rhown_B" + 4, "rhown_B" + 5,
            "rhown_B" + 6, "rhown_B" + 7, "rhown_B" + 8 + "A"};
    private static final String EXPECTED_IOP_APIG = "iop_apig";
    private static final String EXPECTED_IOP_ADET = "iop_adet";
    private static final String EXPECTED_IOP_AGELB = "iop_agelb";
    private static final String EXPECTED_IOP_BPART = "iop_bpart";
    private static final String EXPECTED_IOP_BWIT = "iop_bwit";
    private static final String EXPECTED_IOP_ADG = "iop_adg";
    private static final String EXPECTED_IOP_ATOT = "iop_atot";
    private static final String EXPECTED_IOP_BTOT = "iop_btot";
    private static final String EXPECTED_CONC_CHL = "conc_chl";
    private static final String EXPECTED_CONC_TSM = "conc_tsm";
    private static final String[] EXPECTED_KD_BANDS = {"kd489", "kdmin", "kd_z90max"};
    private static final String[] EXPECTED_OOS_BANDS = {"oos_rtosa", "oos_rhow"};
    private static final String[] EXPECTED_IOP_UNC_BANDS = {
            "unc_apig", "unc_adet", "unc_agelb", "unc_bpart",
            "unc_bwit", "unc_adg", "unc_atot", "unc_btot"};
    private static final String[] EXPECTED_KD_UNC_BANDS = {"unc_kd489", "unc_kdmin"};
    private static final String[] EXPECTED_RTOSA_GC_BANDS = {
            "rtosa_gc_B" + 1, "rtosa_gc_B" + 2, "rtosa_gc_B" + 3, "rtosa_gc_B" + 4, "rtosa_gc_B" + 5,
            "rtosa_gc_B" + 6, "rtosa_gc_B" + 7, "rtosa_gc_B" + 8 + "A"};
    private static final String[] EXPECTED_RTOSA_GCAANN_BANDS = {
            "rtosagc_aann_B" + 1, "rtosagc_aann_B" + 2, "rtosagc_aann_B" + 3, "rtosagc_aann_B" + 4, "rtosagc_aann_B" + 5,
            "rtosagc_aann_B" + 6, "rtosagc_aann_B" + 7, "rtosagc_aann_B" + 8 + "A"};
    private static final String[] EXPECTED_RTOA_BANDS = {
            "rtoa_B" + 1, "rtoa_B" + 2, "rtoa_B" + 3, "rtoa_B" + 4, "rtoa_B" + 5,
            "rtoa_B" + 6, "rtoa_B" + 7, "rtoa_B" + 8, "rtoa_B" + 8 + "A", "rtoa_B" + 9,
            "rtoa_B" + 10, "rtoa_B" + 11, "rtoa_B" + 12};
    private static final String[] EXPECTED_RPATH_BANDS = {
            "rpath_B" + 1, "rpath_B" + 2, "rpath_B" + 3, "rpath_B" + 4, "rpath_B" + 5,
            "rpath_B" + 6, "rpath_B" + 7, "rpath_B" + 8 + "A"};
    private static final String[] EXPECTED_TDOWN_BANDS = {
            "tdown_B" + 1, "tdown_B" + 2, "tdown_B" + 3, "tdown_B" + 4, "tdown_B" + 5,
            "tdown_B" + 6, "tdown_B" + 7, "tdown_B" + 8 + "A"};
    private static final String[] EXPECTED_TUP_BANDS = {
            "tup_B" + 1, "tup_B" + 2, "tup_B" + 3, "tup_B" + 4, "tup_B" + 5,
            "tup_B" + 6, "tup_B" + 7, "tup_B" + 8 + "A"};

    private static final String EXPECTED_L2_FLAGS = "l2_flags";

    @Test
    public void testProductSignature_Default() throws FactoryException, TransformException {

        C2rccMsiOperator operator = createDefaultOperator();

        Product targetProduct = operator.getTargetProduct();

        assertDefaultBands(targetProduct);
    }

    @Test
    public void testProductSignature_OnlyMandatory() throws FactoryException, TransformException {

        C2rccMsiOperator operator = createDefaultOperator();
        operator.setOutputRhow(false);
        operator.setOutputRhown(false);
        operator.setOutputKd(false);
        Product targetProduct = operator.getTargetProduct();

        assertMandatoryBands(targetProduct);
        assertEquals(11, targetProduct.getNumBands());
    }

    @Test
    public void testProductSignature_DefaultWithRtosa() throws FactoryException, TransformException {

        C2rccMsiOperator operator = createDefaultOperator();
        operator.setOutputRtosa(true);
        operator.setOutputRtosaGcAann(true);
        Product targetProduct = operator.getTargetProduct();

        assertDefaultBands(targetProduct);
        assertBands(targetProduct, EXPECTED_RTOSA_GC_BANDS);
        assertBands(targetProduct, EXPECTED_RTOSA_GCAANN_BANDS);
    }

    @Test
    public void testProductSignature_DefaultWithRtoa() throws FactoryException, TransformException {

        C2rccMsiOperator operator = createDefaultOperator();
        operator.setOutputRtoa(true);
        Product targetProduct = operator.getTargetProduct();

        assertDefaultBands(targetProduct);
        assertBands(targetProduct, EXPECTED_RTOA_BANDS);
    }

    @Test
    public void testProductSignature_DefaultWithOthers() throws FactoryException, TransformException {

        C2rccMsiOperator operator = createDefaultOperator();
        operator.setOutputRpath(true);
        operator.setOutputTdown(true);
        operator.setOutputTup(true);
        operator.setOutputOos(true);
        Product targetProduct = operator.getTargetProduct();

        assertDefaultBands(targetProduct);
        assertBands(targetProduct, EXPECTED_RPATH_BANDS);
        assertBands(targetProduct, EXPECTED_TDOWN_BANDS);
        assertBands(targetProduct, EXPECTED_TUP_BANDS);
        assertBands(targetProduct, EXPECTED_OOS_BANDS);
    }

    @Test
    public void testProductSignature_DefaultWithUncertainties() throws FactoryException, TransformException {

        C2rccMsiOperator operator = createDefaultOperator();
        operator.setOutputUncertainties(true);
        Product targetProduct = operator.getTargetProduct();

        assertDefaultBands(targetProduct);
        assertBands(targetProduct, EXPECTED_IOP_UNC_BANDS);
    }

    @Test
    public void testProductSignature_DefaultWithUncertaintiesAndKd() throws FactoryException, TransformException {

        C2rccMsiOperator operator = createDefaultOperator();
        operator.setOutputUncertainties(true);
        operator.setOutputKd(true);
        Product targetProduct = operator.getTargetProduct();

        assertDefaultBands(targetProduct);
        assertBands(targetProduct, EXPECTED_IOP_UNC_BANDS);
        assertBands(targetProduct, EXPECTED_KD_UNC_BANDS);
        assertBands(targetProduct, EXPECTED_KD_BANDS);
    }

    private void assertDefaultBands(Product targetProduct) {
        assertMandatoryBands(targetProduct);
        assertBands(targetProduct, EXPECTED_REFLEC_BANDS);
        assertBands(targetProduct, EXPECTED_NORM_REFLEC_BANDS);
        assertBands(targetProduct, EXPECTED_KD_BANDS);
    }

    private void assertMandatoryBands(Product targetProduct) {
        assertBands(targetProduct, EXPECTED_IOP_APIG);
        assertBands(targetProduct, EXPECTED_IOP_ADET);
        assertBands(targetProduct, EXPECTED_IOP_AGELB);
        assertBands(targetProduct, EXPECTED_IOP_BPART);
        assertBands(targetProduct, EXPECTED_IOP_BWIT);
        assertBands(targetProduct, EXPECTED_IOP_ADG);
        assertBands(targetProduct, EXPECTED_IOP_ATOT);
        assertBands(targetProduct, EXPECTED_IOP_BTOT);
        assertBands(targetProduct, EXPECTED_CONC_CHL);
        assertBands(targetProduct, EXPECTED_CONC_TSM);
        assertBands(targetProduct, EXPECTED_L2_FLAGS);
    }

    private void assertBands(Product targetProduct, String... expectedBands) {
        for (String expectedBand : expectedBands) {
            assertTrue("Expected band " + expectedBand + " in product", targetProduct.containsBand(expectedBand));
        }
    }

    private C2rccMsiOperator createDefaultOperator() throws FactoryException, TransformException {
        C2rccMsiOperator operator = new C2rccMsiOperator();
        operator.setParameterDefaultValues();
        operator.setSourceProduct(createMsiTestProduct());
        return operator;
    }

    private Product createMsiTestProduct() throws FactoryException, TransformException {
        Product product = new Product("test-msi", "t", 1, 1);
        for (String reflBandName : C2rccMsiAlgorithm.SOURCE_BAND_REFL_NAMES) {
            product.addBand(reflBandName, "3863");
        }

        product.addBand(C2rccMsiOperator.RASTER_NAME_SUN_AZIMUTH, "42");
        product.addBand(C2rccMsiOperator.RASTER_NAME_SUN_ZENITH, "42");
        product.addBand(C2rccMsiOperator.RASTER_NAME_VIEWING_AZIMUTH, "42");
        product.addBand(C2rccMsiOperator.RASTER_NAME_VIEWING_ZENITH, "42");

        product.setSceneGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84, 1, 1, 10, 50, 1, 1));

        MetadataElement l1cUserProduct = new MetadataElement("Level-1C_User_Product");
        MetadataElement generalInfo = new MetadataElement("General_Info");
        MetadataElement productInfo = new MetadataElement("Product_Info");
        productInfo.addAttribute(new MetadataAttribute("PRODUCT_START_TIME", ProductData.createInstance("2015-08-12T10:40:21.459Z"), true));
        productInfo.addAttribute(new MetadataAttribute("PRODUCT_STOP_TIME", ProductData.createInstance("2015-08-12T10:40:21.459Z"), true));
        MetadataElement imageCharacteristics = new MetadataElement("Product_Image_Characteristics");
        imageCharacteristics.addAttribute(new MetadataAttribute("QUANTIFICATION_VALUE", ProductData.createInstance("1000"), true));
        l1cUserProduct.addElement(generalInfo);
        generalInfo.addElement(productInfo);
        generalInfo.addElement(imageCharacteristics);
        product.getMetadataRoot().addElement(l1cUserProduct);
        return product;
    }
}