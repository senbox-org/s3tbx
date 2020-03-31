package org.esa.s3tbx.c2rcc.olci;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.test.LongTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * @author Marco Peters
 */
@RunWith(LongTestRunner.class)
public class OlciProductSignatureTest {
    private static final String[] EXPECTED_RHOW_BANDS = {
            "rhow_" + 1, "rhow_" + 2, "rhow_" + 3, "rhow_" + 4, "rhow_" + 5,
            "rhow_" + 6, "rhow_" + 7, "rhow_" + 8, "rhow_" + 9, "rhow_" + 10,
            "rhow_" + 11, "rhow_" + 12, "rhow_" + 16, "rhow_" + 17, "rhow_" + 18, "rhow_" + 21};
    private static final String[] EXPECTED_RRS_BANDS = {
            "rrs_" + 1, "rrs_" + 2, "rrs_" + 3, "rrs_" + 4, "rrs_" + 5,
            "rrs_" + 6, "rrs_" + 7, "rrs_" + 8, "rrs_" + 9, "rrs_" + 10,
            "rrs_" + 12, "rrs_" + 12, "rrs_" + 16, "rrs_" + 17, "rrs_" + 18, "rrs_" + 21};
    private static final String[] EXPECTED_NORM_REFLEC_BANDS = {
            "rhown_" + 1, "rhown_" + 2, "rhown_" + 3, "rhown_" + 4, "rhown_" + 5,
            "rhown_" + 6, "rhown_" + 7, "rhown_" + 8, "rhown_" + 9, "rhown_" + 10,
            "rhown_" + 11, "rhown_" + 12, "rhown_" + 16, "rhown_" + 17, "rhown_" + 18, "rhown_" + 21};
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
    private static final String EXPECTED_OOS_RTOSA = "oos_rtosa";
    private static final String EXPECTED_OOS_RHOW ="oos_rhow";
    private static final String EXPECTED_OOS_RRS = "oos_rrs";
    private static final String[] EXPECTED_IOP_UNC_BANDS = {
            "unc_apig", "unc_adet", "unc_agelb", "unc_bpart",
            "unc_bwit", "unc_adg", "unc_atot", "unc_btot"};
    private static final String[] EXPECTED_KD_UNC_BANDS = {"unc_kd489", "unc_kdmin"};
    private static final String[] EXPECTED_RTOSA_GC_BANDS = {
            "rtosa_gc_" + 1, "rtosa_gc_" + 2, "rtosa_gc_" + 3, "rtosa_gc_" + 4, "rtosa_gc_" + 5,
            "rtosa_gc_" + 6, "rtosa_gc_" + 7, "rtosa_gc_" + 8, "rtosa_gc_" + 9, "rtosa_gc_" + 10,
            "rtosa_gc_" + 11, "rtosa_gc_" + 12, "rtosa_gc_" + 16, "rtosa_gc_" + 17, "rtosa_gc_" + 18, "rtosa_gc_" + 21};
    private static final String[] EXPECTED_RTOSA_GCAANN_BANDS = {
            "rtosagc_aann_" + 1, "rtosagc_aann_" + 2, "rtosagc_aann_" + 3, "rtosagc_aann_" + 4, "rtosagc_aann_" + 5,
            "rtosagc_aann_" + 6, "rtosagc_aann_" + 7, "rtosagc_aann_" + 8, "rtosagc_aann_" + 9, "rtosagc_aann_" + 10,
            "rtosagc_aann_" + 11, "rtosagc_aann_" + 12, "rtosagc_aann_" + 16, "rtosagc_aann_" + 17, "rtosagc_aann_" + 18, "rtosagc_aann_" + 21};
    private static final String[] EXPECTED_RTOA_BANDS = {
            "rtoa_" + 1, "rtoa_" + 2, "rtoa_" + 3, "rtoa_" + 4, "rtoa_" + 5,
            "rtoa_" + 6, "rtoa_" + 7, "rtoa_" + 8, "rtoa_" + 9, "rtoa_" + 10,
            "rtoa_" + 11, "rtoa_" + 12, "rtoa_" + 13, "rtoa_" + 14, "rtoa_" + 15,
            "rtoa_" + 16, "rtoa_" + 17, "rtoa_" + 18, "rtoa_" + 19, "rtoa_" + 20, "rtoa_" + 21};
    private static final String[] EXPECTED_RPATH_BANDS = {
            "rpath_" + 1, "rpath_" + 2, "rpath_" + 3, "rpath_" + 4, "rpath_" + 5,
            "rpath_" + 6, "rpath_" + 7, "rpath_" + 8, "rpath_" + 9, "rpath_" + 10,
            "rpath_" + 11, "rpath_" + 12, "rpath_" + 16, "rpath_" + 17, "rpath_" + 18, "rpath_" + 21};
    private static final String[] EXPECTED_TDOWN_BANDS = {
            "tdown_" + 1, "tdown_" + 2, "tdown_" + 3, "tdown_" + 4, "tdown_" + 5,
            "tdown_" + 6, "tdown_" + 7, "tdown_" + 8, "tdown_" + 9, "tdown_" + 10,
            "tdown_" + 11, "tdown_" + 12, "tdown_" + 16, "tdown_" + 17, "tdown_" + 18, "tdown_" + 21};
    private static final String[] EXPECTED_TUP_BANDS = {
            "tup_" + 1, "tup_" + 2, "tup_" + 3, "tup_" + 4, "tup_" + 5,
            "tup_" + 6, "tup_" + 7, "tup_" + 8, "tup_" + 9, "tup_" + 10,
            "tup_" + 11, "tup_" + 12, "tup_" + 16, "tup_" + 17, "tup_" + 18, "tup_" + 21};

    private static final String EXPECTED_QUALITY_FLAGS = "quality_flags";
    private static final String EXPECTED_C2RCC_FLAGS = "c2rcc_flags";

    private static final String EXPECTED_PRODUCT_TYPE = "C2RCC_OLCI";

    @Test
    public void testProductSignature_Default() throws FactoryException, TransformException {

        C2rccOlciOperator operator = createDefaultOperator();

        Product targetProduct = operator.getTargetProduct();

        assertDefaults(targetProduct, false);
    }

    @Test
    public void testProductSignature_Default_AsRrs() throws FactoryException, TransformException {

        C2rccOlciOperator operator = createDefaultOperator();
        operator.setOutputAsRrs(true);
        operator.setOutputOos(true);
        Product targetProduct = operator.getTargetProduct();

        assertDefaults(targetProduct, true);
        assertBands(targetProduct, EXPECTED_OOS_RTOSA);
        assertBands(targetProduct, EXPECTED_OOS_RRS);

    }

    @Test
    public void testProductSignature_OnlyMandatory() throws FactoryException, TransformException {

        C2rccOlciOperator operator = createDefaultOperator();
        operator.setOutputRtoa(false);
        operator.setOutputUncertainties(false);
        operator.setOutputAcReflec(false);
        operator.setOutputRhown(false);
        operator.setOutputKd(false);
        Product targetProduct = operator.getTargetProduct();

        assertMandatoryBands(targetProduct);
        assertEquals(12, targetProduct.getNumBands());
    }

    @Test
    public void testProductSignature_DefaultWithRtosa() throws FactoryException, TransformException {

        C2rccOlciOperator operator = createDefaultOperator();
        operator.setOutputRtosa(true);
        operator.setOutputRtosaGcAann(true);
        Product targetProduct = operator.getTargetProduct();

        assertDefaults(targetProduct, false);
        assertBands(targetProduct, EXPECTED_RTOSA_GC_BANDS);
        assertBands(targetProduct, EXPECTED_RTOSA_GCAANN_BANDS);
    }

    @Test
    public void testProductSignature_DefaultWithRtoa() throws FactoryException, TransformException {

        C2rccOlciOperator operator = createDefaultOperator();
        operator.setOutputRtoa(true);
        Product targetProduct = operator.getTargetProduct();

        assertDefaults(targetProduct, false);
        assertBands(targetProduct, EXPECTED_RTOA_BANDS);
    }

    @Test
    public void testProductSignature_DefaultWithOthers() throws FactoryException, TransformException {

        C2rccOlciOperator operator = createDefaultOperator();
        operator.setOutputRpath(true);
        operator.setOutputTdown(true);
        operator.setOutputTup(true);
        operator.setOutputOos(true);
        Product targetProduct = operator.getTargetProduct();

        assertDefaults(targetProduct, false);
        assertBands(targetProduct, EXPECTED_RPATH_BANDS);
        assertBands(targetProduct, EXPECTED_TDOWN_BANDS);
        assertBands(targetProduct, EXPECTED_TUP_BANDS);
        assertBands(targetProduct, EXPECTED_OOS_RTOSA);
        assertBands(targetProduct, EXPECTED_OOS_RHOW);
    }

    @Test
    public void testProductSignature_DefaultWithUncertainties() throws FactoryException, TransformException {

        C2rccOlciOperator operator = createDefaultOperator();
        operator.setOutputUncertainties(true);
        Product targetProduct = operator.getTargetProduct();

        assertDefaults(targetProduct, false);
        assertBands(targetProduct, EXPECTED_IOP_UNC_BANDS);
    }

    @Test
    public void testProductSignature_DefaultWithUncertaintiesAndKd() throws FactoryException, TransformException {

        C2rccOlciOperator operator = createDefaultOperator();
        operator.setOutputUncertainties(true);
        operator.setOutputKd(true);
        Product targetProduct = operator.getTargetProduct();

        assertDefaults(targetProduct, false);
        assertBands(targetProduct, EXPECTED_IOP_UNC_BANDS);
        assertBands(targetProduct, EXPECTED_KD_UNC_BANDS);
        assertBands(targetProduct, EXPECTED_KD_BANDS);
    }

    private void assertDefaults(Product targetProduct, boolean asRrs) {
        assertMandatoryBands(targetProduct);
        if(asRrs) {
            assertBands(targetProduct, EXPECTED_RRS_BANDS);
        }else {
            assertBands(targetProduct, EXPECTED_RHOW_BANDS);
        }
        assertBands(targetProduct, EXPECTED_RTOA_BANDS);
        assertBands(targetProduct, EXPECTED_NORM_REFLEC_BANDS);
        assertBands(targetProduct, EXPECTED_KD_BANDS);
        assertBands(targetProduct, EXPECTED_IOP_UNC_BANDS);
        assertBands(targetProduct, EXPECTED_KD_UNC_BANDS);

        assertEquals(EXPECTED_PRODUCT_TYPE, targetProduct.getProductType());
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
        assertBands(targetProduct, EXPECTED_QUALITY_FLAGS);
        assertBands(targetProduct, EXPECTED_C2RCC_FLAGS);
    }

    private void assertBands(Product targetProduct, String... expectedBands) {
        for (String expectedBand : expectedBands) {
            assertTrue("Expected band " + expectedBand + " in product", targetProduct.containsBand(expectedBand));
        }
    }

    private C2rccOlciOperator createDefaultOperator() throws FactoryException, TransformException {
        C2rccOlciOperator operator = new C2rccOlciOperator();
        operator.setParameterDefaultValues();
        operator.setValidPixelExpression(String.format("%s == 500", C2rccOlciOperator.RASTER_NAME_ALTITUDE));
        operator.setSourceProduct(OlciTestProduct.create());
        return operator;
    }

}