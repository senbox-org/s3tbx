package org.esa.s3tbx.processor.flh_mci;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.OperatorException;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.*;

/**
 * @author Marco Peters
 */
public class FlhMciOpTest {

    @Test
    public void testMissingParameter_lowerBaselineBandName() throws Exception {
        Product sourceProduct = new Product("dummy", "D", 10, 10);
        HashMap<String, Object> parameters = new HashMap<>();

        String expectedMessage = "Parameter 'lowerBaselineBandName' not specified";
        runWithExpectedException(parameters, sourceProduct, expectedMessage);
    }

    @Test
    public void testMissingParameter_lowerBaselineBandName_notInProduct() throws Exception {
        Product sourceProduct = new Product("dummy", "D", 10, 10);
        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("lowerBaselineBandName", "lbase");

        String expectedMessage = "Value for 'Lower baseline band name' is invalid";
        runWithExpectedException(parameters, sourceProduct, expectedMessage);
    }

    @Test
    public void testMissingParameter_upperBaselineBandName() throws Exception {
        Product sourceProduct = new Product("dummy", "D", 10, 10);
        sourceProduct.addBand("lbase", ProductData.TYPE_INT8);
        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("lowerBaselineBandName", "lbase");

        String expectedMessage = "Parameter 'upperBaselineBandName' not specified";
        runWithExpectedException(parameters, sourceProduct, expectedMessage);
    }

    @Test
    public void testMissingParameter_upperBaselineBandName_notInProduct() throws Exception {
        Product sourceProduct = new Product("dummy", "D", 10, 10);
        sourceProduct.addBand("lbase", ProductData.TYPE_INT8);
        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("lowerBaselineBandName", "lbase");
        parameters.put("upperBaselineBandName", "ubase");

        String expectedMessage = "Value for 'Upper baseline band name' is invalid";
        runWithExpectedException(parameters, sourceProduct, expectedMessage);
    }

    @Test
    public void testMissingParameter_signalBandName() throws Exception {
        Product sourceProduct = new Product("dummy", "D", 10, 10);
        sourceProduct.addBand("lbase", ProductData.TYPE_INT8);
        sourceProduct.addBand("ubase", ProductData.TYPE_INT8);
        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("lowerBaselineBandName", "lbase");
        parameters.put("upperBaselineBandName", "ubase");

        String expectedMessage = "Parameter 'signalBandName' not specified";
        runWithExpectedException(parameters, sourceProduct, expectedMessage);
    }

    @Test
    public void testMissingParameter_signalBandName_notInProduct() throws Exception {
        Product sourceProduct = new Product("dummy", "D", 10, 10);
        sourceProduct.addBand("lbase", ProductData.TYPE_INT8);
        sourceProduct.addBand("ubase", ProductData.TYPE_INT8);
        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("lowerBaselineBandName", "lbase");
        parameters.put("upperBaselineBandName", "ubase");
        parameters.put("signalBandName", "sband");

        String expectedMessage = "Value for 'Signal band name' is invalid";
        runWithExpectedException(parameters, sourceProduct, expectedMessage);
    }

    @Test
    public void testMissingParameter_lineHeightBandName() throws Exception {
        Product sourceProduct = new Product("dummy", "D", 10, 10);
        sourceProduct.addBand("lbase", ProductData.TYPE_INT8);
        sourceProduct.addBand("ubase", ProductData.TYPE_INT8);
        sourceProduct.addBand("sband", ProductData.TYPE_INT8);
        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("lowerBaselineBandName", "lbase");
        parameters.put("upperBaselineBandName", "ubase");
        parameters.put("signalBandName", "sband");

        String expectedMessage = "Parameter 'lineHeightBandName' not specified";
        runWithExpectedException(parameters, sourceProduct, expectedMessage);
    }

    @Test
    public void testMissingParameter_slopeBandName() throws Exception {
        Product sourceProduct = new Product("dummy", "D", 10, 10);
        sourceProduct.addBand("lbase", ProductData.TYPE_INT8);
        sourceProduct.addBand("ubase", ProductData.TYPE_INT8);
        sourceProduct.addBand("sband", ProductData.TYPE_INT8);
        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("lowerBaselineBandName", "lbase");
        parameters.put("upperBaselineBandName", "ubase");
        parameters.put("signalBandName", "sband");
        parameters.put("lineHeightBandName", "lineHeight");

        String expectedMessage = "Parameter 'slopeBandName' not specified";
        runWithExpectedException(parameters, sourceProduct, expectedMessage);
    }

    private void runWithExpectedException(HashMap<String, Object> parameters, Product sourceProduct, String expectedExceptionMessage) {
        try {
            GPF.createProduct("FlhMci", parameters, sourceProduct);
            fail("Expected OperatorException: " + expectedExceptionMessage);
        } catch (OperatorException e) {
            assertThat(e.getMessage(), CoreMatchers.containsString(expectedExceptionMessage));
        }
    }
}