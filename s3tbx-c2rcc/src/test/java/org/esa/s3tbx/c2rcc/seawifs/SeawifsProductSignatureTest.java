package org.esa.s3tbx.c2rcc.seawifs;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.test.LongTestRunner;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


/**
 * @author Marco Peters
 */
@RunWith(LongTestRunner.class)
public class SeawifsProductSignatureTest {
    private static final String[] EXPECTED_RHOW_BANDS = {
            "rhow_" + 412, "rhow_" + 443, "rhow_" + 490, "rhow_" + 510, "rhow_" + 555,
            "rhow_" + 670, "rhow_" + 765, "rhow_" + 865};
    private static final String[] EXPECTED_RRS_BANDS = {
            "rrs_" + 412, "rrs_" + 443, "rrs_" + 490, "rrs_" + 510, "rrs_" + 555,
            "rrs_" + 670, "rrs_" + 765, "rrs_" + 865};
    private static final String EXPECTED_RTOSA_RATION_MIN = "rtosa_ratio_min";
    private static final String EXPECTED_RTOSA_RATION_MAX = "rtosa_ratio_max";
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
    private static final String[] EXPECTED_RTOSA_IN_BANDS = {
            "rtosa_in_" + 412, "rtosa_in_" + 443, "rtosa_in_" + 490, "rtosa_in_" + 510, "rtosa_in_" + 555,
            "rtosa_in_" + 670, "rtosa_in_" + 765, "rtosa_in_" + 865};
    private static final String[] EXPECTED_RTOSA_OUT_BANDS = {
            "rtosa_out_" + 412, "rtosa_out_" + 443, "rtosa_out_" + 490, "rtosa_out_" + 510, "rtosa_out_" + 555,
            "rtosa_out_" + 670, "rtosa_out_" + 765, "rtosa_out_" + 865};

    private static final String EXPECTED_C2RCC_FLAGS = "c2rcc_flags";
    private static final String EXPECTED_VALID_PE_FLAG = "Valid_PE";
    private static final int EXPECTED_VPE_MASK = 0x80000000;
    private static final String EXPECTED_L2_FLAGS = "l2_flags";

    private static final String EXPECTED_PRODUCT_TYPE = "C2RCC_SEAWIFS";


    @Test
    public void testProductSignature_Default() throws FactoryException, TransformException {

        C2rccSeaWiFSOperator operator = createDefaultOperator();

        Product targetProduct = operator.getTargetProduct();

        assertMandatoryElements(targetProduct, false);
    }

    @Test
    public void testProductSignature_Default_AsRrs() throws FactoryException, TransformException {

        C2rccSeaWiFSOperator operator = createDefaultOperator();
        operator.setOutputAsRrs(true);
        Product targetProduct = operator.getTargetProduct();

        assertMandatoryElements(targetProduct, true);
    }

    @Test
    public void testProductSignature_WithRtosa() throws FactoryException, TransformException {
        C2rccSeaWiFSOperator operator = createDefaultOperator();
        operator.setOutputRtosa(true);

        Product targetProduct = operator.getTargetProduct();

        assertMandatoryElements(targetProduct, false);
        assertBands(targetProduct, EXPECTED_RTOSA_IN_BANDS);
        assertBands(targetProduct, EXPECTED_RTOSA_OUT_BANDS);
    }

    private void assertMandatoryElements(Product targetProduct, boolean asRrs) {
        if (asRrs) {
            assertBands(targetProduct, EXPECTED_RRS_BANDS);
            assertEquals("c2rcc_flags.Valid_PE", targetProduct.getBand(EXPECTED_RRS_BANDS[3]).getValidPixelExpression());
        } else {
            assertBands(targetProduct, EXPECTED_RHOW_BANDS);
            assertEquals("c2rcc_flags.Valid_PE", targetProduct.getBand(EXPECTED_RHOW_BANDS[3]).getValidPixelExpression());
        }
        assertBands(targetProduct, EXPECTED_RTOSA_RATION_MIN);
        assertBands(targetProduct, EXPECTED_RTOSA_RATION_MAX);
        assertBands(targetProduct, EXPECTED_IOP_APIG);
        assertEquals("c2rcc_flags.Valid_PE", targetProduct.getBand(EXPECTED_IOP_APIG).getValidPixelExpression());
        assertBands(targetProduct, EXPECTED_IOP_ADET);
        assertBands(targetProduct, EXPECTED_IOP_AGELB);
        assertBands(targetProduct, EXPECTED_IOP_BPART);
        assertBands(targetProduct, EXPECTED_IOP_BWIT);
        assertBands(targetProduct, EXPECTED_IOP_ADG);
        assertBands(targetProduct, EXPECTED_IOP_ATOT);
        assertBands(targetProduct, EXPECTED_IOP_BTOT);
        assertEquals("c2rcc_flags.Valid_PE", targetProduct.getBand(EXPECTED_IOP_BTOT).getValidPixelExpression());
        assertBands(targetProduct, EXPECTED_CONC_CHL);
        assertBands(targetProduct, EXPECTED_CONC_TSM);
        assertBands(targetProduct, EXPECTED_C2RCC_FLAGS);
        assertBands(targetProduct, EXPECTED_L2_FLAGS);
        FlagCoding flagCoding = targetProduct.getFlagCodingGroup().get(EXPECTED_C2RCC_FLAGS);
        assertNotNull(flagCoding.getFlag(EXPECTED_VALID_PE_FLAG));
        assertEquals(EXPECTED_VPE_MASK, flagCoding.getFlagMask(EXPECTED_VALID_PE_FLAG));

        assertEquals(EXPECTED_PRODUCT_TYPE, targetProduct.getProductType());
    }

    private void assertBands(Product targetProduct, String... expectedBands) {
        for (String expectedBand : expectedBands) {
            assertTrue("Expected band " + expectedBand + " in product", targetProduct.containsBand(expectedBand));
        }
    }

    private C2rccSeaWiFSOperator createDefaultOperator() throws FactoryException, TransformException {
        C2rccSeaWiFSOperator operator = new C2rccSeaWiFSOperator();
        operator.setParameterDefaultValues();
        operator.setSourceProduct(createSeawifsTestProduct());
        return operator;
    }

    private Product createSeawifsTestProduct() throws FactoryException, TransformException {
        Product product = new Product("test-seawifs", "t", 1, 1);
        int[] reflecWavelengths = C2rccSeaWiFSAlgorithm.seawifsWavelengths;
        for (int reflec_wavelength : reflecWavelengths) {
            String expression = String.valueOf(reflec_wavelength);
            product.addBand(C2rccSeaWiFSOperator.SOURCE_RADIANCE_NAME_PREFIX + reflec_wavelength, expression);
        }

        Date time = new Date();
        product.setStartTime(ProductData.UTC.create(time, 0));
        product.setEndTime(ProductData.UTC.create(time, 500));

        for (String angleName : C2rccSeaWiFSOperator.GEOMETRY_ANGLE_NAMES) {
            product.addBand(angleName, "42");
        }

        Band flagBand = product.addBand(C2rccSeaWiFSOperator.RASTER_NAME_L2_FLAGS, ProductData.TYPE_INT8);
        FlagCoding l2FlagsCoding = new FlagCoding(C2rccSeaWiFSOperator.RASTER_NAME_L2_FLAGS);
        l2FlagsCoding.addFlag("LAND", 0x01, "");
        product.getFlagCodingGroup().add(l2FlagsCoding);
        flagBand.setSampleCoding(l2FlagsCoding);

        product.setSceneGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84, 1, 1, 10, 50, 1, 1));

        return product;
    }
}