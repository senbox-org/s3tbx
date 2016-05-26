package org.esa.s3tbx.c2rcc.meris;

import org.esa.s3tbx.c2rcc.modis.C2rccModisAlgorithm;
import org.esa.s3tbx.c2rcc.modis.C2rccModisOperator;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Ignore;
import org.junit.Test;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import static org.junit.Assert.assertTrue;


/**
 * @author Marco Peters
 */
@Ignore("not yet ready")
public class MerisProductSignatureTest {
    private static final String[] EXPECTED_REFLEC_BANDS = {
            "reflec_" + 412, "reflec_" + 443, "reflec_" + 488, "reflec_" + 531, "reflec_" + 547,
            "reflec_" + 667, "reflec_" + 678, "reflec_" + 748, "reflec_" + 869};
    private static final String EXPECTED_RTOSA_RATION_MIN = "rtosa_ratio_min";
    private static final String EXPECTED_RTOSA_RATION_MAX = "rtosa_ratio_max";
    private static final String EXPECTED_IOP_APIG = "iop_apig";
    private static final String EXPECTED_IOP_ADET = "iop_adet";
    private static final String EXPECTED_IOP_AGELB = "iop_agelb";
    private static final String EXPECTED_IOP_BPART = "iop_bpart";
    private static final String EXPECTED_IOP_BWIT = "iop_bwit";
    private static final String EXPECTED_IOP_ADG = "iop_adg";
    private static final String EXPECTED_IOP_ATOT = "iop_atot";
    private static final String EXPECTED_CONC_CHL = "conc_chl";
    private static final String EXPECTED_CONC_TSM = "conc_tsm";
    private static final String[] EXPECTED_RTOSA_IN_BANDS = {
            "rtosa_in_" + 412, "rtosa_in_" + 443, "rtosa_in_" + 488, "rtosa_in_" + 531, "rtosa_in_" + 547,
            "rtosa_in_" + 667, "rtosa_in_" + 678, "rtosa_in_" + 748, "rtosa_in_" + 869};
    private static final String[] EXPECTED_RTOSA_OUT_BANDS = {
            "rtosa_out_" + 412, "rtosa_out_" + 443, "rtosa_out_" + 488, "rtosa_out_" + 531, "rtosa_out_" + 547,
            "rtosa_out_" + 667, "rtosa_out_" + 678, "rtosa_out_" + 748, "rtosa_out_" + 869};

    private static final String EXPECTED_L2_QFLAGS = "l2_qflags";
    private static final String[] EXPECTED_GEOMETRY_ANGLES = new String[]{"solz", "sola", "senz", "sena"};

    @Test
    public void testProductSignature_Default() throws FactoryException, TransformException {

        C2rccModisOperator operator = createDefaultOperator();

        Product targetProduct = operator.getTargetProduct();

        assertDefaultBands(targetProduct);
    }


    private void assertDefaultBands(Product targetProduct) {
        assertBands(targetProduct, EXPECTED_REFLEC_BANDS);
        assertBands(targetProduct, EXPECTED_RTOSA_RATION_MIN);
        assertBands(targetProduct, EXPECTED_RTOSA_RATION_MAX);
        assertBands(targetProduct, EXPECTED_IOP_APIG);
        assertBands(targetProduct, EXPECTED_IOP_ADET);
        assertBands(targetProduct, EXPECTED_IOP_AGELB);
        assertBands(targetProduct, EXPECTED_IOP_BPART);
        assertBands(targetProduct, EXPECTED_IOP_BWIT);
        assertBands(targetProduct, EXPECTED_IOP_ADG);
        assertBands(targetProduct, EXPECTED_IOP_ATOT);
        assertBands(targetProduct, EXPECTED_CONC_CHL);
        assertBands(targetProduct, EXPECTED_CONC_TSM);
        assertBands(targetProduct, EXPECTED_L2_QFLAGS);
    }

    private void assertBands(Product targetProduct, String... expectedBands) {
        for (String expectedBand : expectedBands) {
            assertTrue("Expected band " + expectedBand + " in product", targetProduct.containsBand(expectedBand));
        }
    }

    private C2rccModisOperator createDefaultOperator() throws FactoryException, TransformException {
        C2rccModisOperator operator = new C2rccModisOperator();
        operator.setParameterDefaultValues();
        operator.setSourceProduct(createModisTestProduct());
        return operator;
    }

    private Product createModisTestProduct() throws FactoryException, TransformException {
        Product product = new Product("test-meris", "t", 1, 1);
        for (int i = 1; i <= C2rccMerisOperator.BAND_COUNT; i++) {
            String expression = String.valueOf(i);
            product.addBand(C2rccMerisOperator.SOURCE_RADIANCE_NAME_PREFIX + i, expression);
        }

////        for (String angleName : C2rccModisOperator.GEOMETRY_ANGLE_NAMES) {
////            product.addBand(angleName, "42");
////        }
////
////        Band flagBand = product.addBand(C2rccModisOperator.FLAG_BAND_NAME, ProductData.TYPE_INT8);
////        FlagCoding l2FlagsCoding = new FlagCoding(C2rccModisOperator.FLAG_BAND_NAME);
//        l2FlagsCoding.addFlag("LAND", 0x01, "");
//        product.getFlagCodingGroup().add(l2FlagsCoding);
//        flagBand.setSampleCoding(l2FlagsCoding);

        product.setSceneGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84, 1, 1, 10, 50, 1, 1));

        return product;
    }
}