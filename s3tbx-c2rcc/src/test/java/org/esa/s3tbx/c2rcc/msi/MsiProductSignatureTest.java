package org.esa.s3tbx.c2rcc.msi;

import org.esa.snap.core.datamodel.Product;
import org.junit.Test;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * @author Marco Peters
 */
public class MsiProductSignatureTest {

    @Test
    public void testProductSignature_Default() throws FactoryException, TransformException {

        C2rccMsiOperator operator = createDefaultOperator();

        Product targetProduct = operator.getTargetProduct();

        assertDefaults(targetProduct, false);
    }

    @Test
    public void testProductSignature_Default_AsRrs() throws FactoryException, TransformException {

        C2rccMsiOperator operator = createDefaultOperator();
        operator.setOutputAsRrs(true);
        operator.setOutputOos(true);
        Product targetProduct = operator.getTargetProduct();

        assertDefaults(targetProduct, true);
        assertBands(targetProduct, ExpectedSignature.EXPECTED_OOS_RTOSA);
        assertBands(targetProduct, ExpectedSignature.EXPECTED_OOS_RRS);

    }

    @Test
    public void testProductSignature_OnlyMandatory() throws FactoryException, TransformException {

        C2rccMsiOperator operator = createDefaultOperator();
        operator.setOutputRtoa(false);
        operator.setOutputUncertainties(false);
        operator.setOutputAcReflectance(false);
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

        assertDefaults(targetProduct, false);
        assertBands(targetProduct, ExpectedSignature.EXPECTED_RTOSA_GC_BANDS);
        assertBands(targetProduct, ExpectedSignature.EXPECTED_RTOSA_GCAANN_BANDS);
    }

    @Test
    public void testProductSignature_DefaultWithRtoa() throws FactoryException, TransformException {

        C2rccMsiOperator operator = createDefaultOperator();
        operator.setOutputRtoa(true);
        Product targetProduct = operator.getTargetProduct();

        assertDefaults(targetProduct, false);
        assertBands(targetProduct, ExpectedSignature.EXPECTED_RTOA_BANDS);
    }

    @Test
    public void testProductSignature_DefaultWithOthers() throws FactoryException, TransformException {

        C2rccMsiOperator operator = createDefaultOperator();
        operator.setOutputRpath(true);
        operator.setOutputTdown(true);
        operator.setOutputTup(true);
        operator.setOutputOos(true);
        Product targetProduct = operator.getTargetProduct();

        assertDefaults(targetProduct, false);
        assertBands(targetProduct, ExpectedSignature.EXPECTED_RPATH_BANDS);
        assertBands(targetProduct, ExpectedSignature.EXPECTED_TDOWN_BANDS);
        assertBands(targetProduct, ExpectedSignature.EXPECTED_TUP_BANDS);
        assertBands(targetProduct, ExpectedSignature.EXPECTED_OOS_RTOSA);
        assertBands(targetProduct, ExpectedSignature.EXPECTED_OOS_RHOW);
    }

    @Test
    public void testProductSignature_DeriveRwAlternative() throws FactoryException, TransformException {

        C2rccMsiOperator operator = createDefaultOperator();
        operator.setDeriveRwFromPathAndTransmittance(true);

        Product targetProduct = operator.getTargetProduct();
        assertDefaults(targetProduct, false);
    }

    @Test
    public void testProductSignature_DefaultWithKd() throws FactoryException, TransformException {

        C2rccMsiOperator operator = createDefaultOperator();
        operator.setOutputKd(true);
        Product targetProduct = operator.getTargetProduct();

        assertDefaults(targetProduct, false);
        assertBands(targetProduct, ExpectedSignature.EXPECTED_KD_BANDS);
    }

    private void assertDefaults(Product targetProduct, boolean asRrs) {
        assertMandatoryBands(targetProduct);
        if (asRrs) {
            assertBands(targetProduct, ExpectedSignature.EXPECTED_RRS_BANDS);
        } else {
            assertBands(targetProduct, ExpectedSignature.EXPECTED_RHOW_BANDS);
        }
        assertBands(targetProduct, ExpectedSignature.EXPECTED_RTOA_BANDS);
        assertBands(targetProduct, ExpectedSignature.EXPECTED_NORM_REFLEC_BANDS);
        assertBands(targetProduct, ExpectedSignature.EXPECTED_KD_BANDS);
        assertBands(targetProduct, ExpectedSignature.EXPECTED_IOP_UNC_BANDS);
        assertBands(targetProduct, ExpectedSignature.EXPECTED_KD_UNC_BANDS);

        assertEquals(ExpectedSignature.EXPECTED_PRODUCT_TYPE, targetProduct.getProductType());
    }

    private void assertMandatoryBands(Product targetProduct) {
        assertBands(targetProduct, ExpectedSignature.EXPECTED_IOP_APIG);
        assertBands(targetProduct, ExpectedSignature.EXPECTED_IOP_ADET);
        assertBands(targetProduct, ExpectedSignature.EXPECTED_IOP_AGELB);
        assertBands(targetProduct, ExpectedSignature.EXPECTED_IOP_BPART);
        assertBands(targetProduct, ExpectedSignature.EXPECTED_IOP_BWIT);
        assertBands(targetProduct, ExpectedSignature.EXPECTED_IOP_ADG);
        assertBands(targetProduct, ExpectedSignature.EXPECTED_IOP_ATOT);
        assertBands(targetProduct, ExpectedSignature.EXPECTED_IOP_BTOT);
        assertBands(targetProduct, ExpectedSignature.EXPECTED_CONC_CHL);
        assertBands(targetProduct, ExpectedSignature.EXPECTED_CONC_TSM);
        assertBands(targetProduct, ExpectedSignature.EXPECTED_C2RCC_FLAGS);
    }

    private void assertBands(Product targetProduct, String... expectedBands) {
        for (String expectedBand : expectedBands) {
            assertTrue("Expected band " + expectedBand + " in product", targetProduct.containsBand(expectedBand));
        }
    }

    private static C2rccMsiOperator createDefaultOperator() throws FactoryException, TransformException {
        C2rccMsiOperator operator = new C2rccMsiOperator();
        operator.setParameterDefaultValues();
        operator.setSourceProduct(MsiTestProduct.createInput());
        return operator;
    }

}