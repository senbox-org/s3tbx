package org.esa.s3tbx.c2rcc.meris;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import static org.junit.Assert.assertTrue;


/**
 * @author Marco Peters
 */
public class MerisProductSignatureTest {
    private static final String[] EXPECTED_REFLEC_BANDS = {
            "rwa_" + 412, "rwa_" + 443, "rwa_" + 488, "rwa_" + 531, "rwa_" + 547,
            "rwa_" + 667, "rwa_" + 678, "rwa_" + 748, "rwa_" + 869};
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

        C2rccMerisOperator operator = createDefaultOperator();

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

    private C2rccMerisOperator createDefaultOperator() throws FactoryException, TransformException {
        C2rccMerisOperator operator = new C2rccMerisOperator();
        operator.setParameterDefaultValues();
        operator.setSourceProduct(createMerisTestProduct());
        return operator;
    }

    private Product createMerisTestProduct() throws FactoryException, TransformException {
        Product product = new Product("test-meris", "t", 1, 1);
        for (int i = 1; i <= C2rccMerisOperator.BAND_COUNT; i++) {
            String expression = String.valueOf(i);
            Band radiance = product.addBand(C2rccMerisOperator.SOURCE_RADIANCE_NAME_PREFIX + i, expression);
            radiance.setSolarFlux((float) C2rccMerisAlgorithm.DEFAULT_SOLAR_FLUX[i-1]);
        }

////        for (String angleName : C2rccModisOperator.GEOMETRY_ANGLE_NAMES) {
////            product.addBand(angleName, "42");
////        }
////
        product.addBand(C2rccMerisOperator.RASTER_NAME_DEM_ALT, ProductData.TYPE_INT8);
        Band flagBand = product.addBand(C2rccMerisOperator.RASTER_NAME_L1_FLAGS, ProductData.TYPE_INT8);
        FlagCoding l1FlagsCoding = new FlagCoding(C2rccMerisOperator.RASTER_NAME_L1_FLAGS);
//        l1FlagsCoding.addFlag("LAND", 0x01, "");
        product.getFlagCodingGroup().add(l1FlagsCoding);
        flagBand.setSampleCoding(l1FlagsCoding);

        product.setSceneGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84, 1, 1, 10, 50, 1, 1));

        return product;
    }
}