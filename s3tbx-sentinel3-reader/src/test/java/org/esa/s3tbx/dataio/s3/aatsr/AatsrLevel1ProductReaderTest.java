package org.esa.s3tbx.dataio.s3.aatsr;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.TiePointGeoCoding;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AatsrLevel1ProductReaderTest {

    private AatsrLevel1ProductReader reader;

    private static String TEST_FILENAME_ENV = "ENV_AT_1_RBT____20110107T111532_20110107T130045_20180928T095029_6312_098_166______TPZ_R_NT_004.SEN3";
    private static String TEST_FILENAME_ER1 = "ER1_AT_1_RBT____19910901T061936_19910901T080223_20180928T124040_6167_014_005______TPZ_R_NT_004.SEN3";
    private static String TEST_FILENAME_ER2 = "ER2_AT_1_RBT____19960702T203517_19960702T221812_20180928T150751_6175_012_414______TPZ_R_NT_004.SEN3";

    private boolean skipTests = false;

    private String testdataPath;

    @Before
    public void setUp() {
        reader = new AatsrLevel1ProductReader(new AatsrLevel1ProductReaderPlugIn());

        URL resource = getClass().getResource("testdata.properties");
        if (resource == null) {
            System.out.println("Warning: test data for test '" + getClass().getName() + "' not available. Configure in " + getClass().getPackage() + "testdata.properties.");
            skipTests = true;
            return;
        }
        Properties testdataProperties = new Properties();
        try (InputStream testdataResource = resource.openStream()) {
            testdataProperties.load(testdataResource);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        testdataPath = testdataProperties.getProperty("testdataPath");


    }

    @Test
    public void testReadProductNodesImpl_Env() throws IOException, ParseException {
        Path testFile = getTestFile(testdataPath, TEST_FILENAME_ENV);
        if (skipTests) {
            return;
        }
        Product product = reader.readProductNodes(testFile.toFile(), null);
        testForBandsPresence(product);

        double[] pixels = new double[512];
        Band band = product.getBand("S1_radiance_in");
        band.readPixels(0, 0, 512, 1, pixels);
        assertEquals(band.getGeophysicalNoDataValue(), pixels[0], 1E-5);
        assertEquals(band.getGeophysicalNoDataValue(), pixels[218], 1E-5);
        assertEquals(0, pixels[219], 1E-5);
        assertEquals(band.getGeophysicalNoDataValue(), pixels[220], 1E-5);
        assertEquals(band.scale(1), pixels[224], 1E-5);

        assertEquals(ProductData.UTC.parse("2011-01-07T11:15:32Z", "yyyy-MM-dd'T'HH:mm:ss'Z'").getMJD(), product.getStartTime().getMJD(), 1E-5);
        assertEquals(ProductData.UTC.parse("2011-01-07T13:00:45Z", "yyyy-MM-dd'T'HH:mm:ss'Z'").getMJD(), product.getEndTime().getMJD(), 1E-5);

        assertNotNull(product.getSceneGeoCoding());
        assertTrue(product.getSceneGeoCoding() instanceof TiePointGeoCoding);

        MetadataElement generalProductInformation = product.getMetadataRoot().getElement("Manifest").getElement("metadataSection").getElement("generalProductInformation");
        MetadataAttribute productSize = generalProductInformation.getAttribute("productSize");

        assertEquals("ENV_AT_1_RBT", product.getProductType());

        assertNotNull(generalProductInformation);
        assertNotNull(productSize);
        assertEquals("1134201405", productSize.getData().getElemString());

    }

    @Test
    public void testReadProductNodesImpl_Er1() throws IOException, ParseException {
        Path testFile = getTestFile(testdataPath, TEST_FILENAME_ER1);
        if (skipTests) {
            return;
        }
        Product product = reader.readProductNodes(testFile.toFile(), null);
        testForBandsPresence(product);

        double[] pixels = new double[512];
        Band band = product.getBand("S7_BT_in");
        band.readPixels(0, 11397, 512, 1, pixels);

        for (int x = 0; x < 512; x++) {
            if (x != 452) {
                assertEquals(band.getGeophysicalNoDataValue(), pixels[x], 1E-5);
            } else {
                assertEquals(band.scale(31325), pixels[x], 1E-5);
            }
        }

        assertEquals(ProductData.UTC.parse("1991-09-01T06:19:36Z", "yyyy-MM-dd'T'HH:mm:ss'Z'").getMJD(), product.getStartTime().getMJD(), 1E-5);
        assertEquals(ProductData.UTC.parse("1991-09-01T08:02:23Z", "yyyy-MM-dd'T'HH:mm:ss'Z'").getMJD(), product.getEndTime().getMJD(), 1E-5);

        assertNotNull(product.getSceneGeoCoding());
        assertTrue(product.getSceneGeoCoding() instanceof TiePointGeoCoding);

        MetadataElement qualityInformation = product.getMetadataRoot().getElement("Manifest").getElement("metadataSection").getElement("qualityInformation");
        MetadataAttribute degradationFlags = qualityInformation.getElement("extension").getElement("productQuality").getAttribute("degradationFlags");

        assertEquals("ER1_AT_1_RBT", product.getProductType());
        assertNotNull(qualityInformation);
        assertNotNull(degradationFlags);
        assertEquals("NON_NOMINAL_INPUT", degradationFlags.getData().getElemString());
    }

    @Test
    public void testReadProductNodesImpl_Er2() throws IOException, ParseException {
        Path testFile = getTestFile(testdataPath, TEST_FILENAME_ER2);
        if (skipTests) {
            return;
        }
        Product product = reader.readProductNodes(testFile.toFile(), null);
        testForBandsPresence(product);

        double[] pixels = new double[512];
        Band band = product.getBand("S2_radiance_io");
        band.readPixels(0, 6425, 512, 1, pixels);

        for (int x = 0; x < 512; x++) {
            if (x != 501) {
                assertTrue(band.getGeophysicalNoDataValue() == pixels[x] || 0 == pixels[x]);
            } else {
                assertEquals(band.scale(4), pixels[x], 1E-5);
            }
        }

        assertEquals(ProductData.UTC.parse("1996-07-02T20:35:17Z", "yyyy-MM-dd'T'HH:mm:ss'Z'").getMJD(), product.getStartTime().getMJD(), 1E-5);
        assertEquals(ProductData.UTC.parse("1996-07-02T22:18:12Z", "yyyy-MM-dd'T'HH:mm:ss'Z'").getMJD(), product.getEndTime().getMJD(), 1E-5);

        assertNotNull(product.getSceneGeoCoding());
        assertTrue(product.getSceneGeoCoding() instanceof TiePointGeoCoding);

        MetadataElement qualityInformation = product.getMetadataRoot().getElement("Manifest").getElement("metadataSection").getElement("qualityInformation");
        MetadataAttribute onlineQualityCheck = qualityInformation.getElement("extension").getElement("productQuality").getAttribute("onlineQualityCheck");

        assertEquals("ER2_AT_1_RBT", product.getProductType());
        assertNotNull(qualityInformation);
        assertNotNull(onlineQualityCheck);
        assertEquals("PASSED", onlineQualityCheck.getData().getElemString());
    }

    private static void testForBandsPresence(Product product) {
        assertTrue(product.getBandGroup().contains("S1_radiance_in"));
        assertTrue(product.getBandGroup().contains("S1_radiance_uncert_in"));
        assertTrue(product.getBandGroup().contains("S1_exception_in"));
        assertTrue(product.getBandGroup().contains("S1_radiance_io"));
        assertTrue(product.getBandGroup().contains("S1_radiance_uncert_io"));
        assertTrue(product.getBandGroup().contains("S1_exception_io"));
    }

    private Path getTestFile(String testdataPath, String testFilename) {
        Path testFile = Paths.get(testdataPath).resolve(testFilename);
        if (Files.exists(testFile)) {
            return testFile;
        } else {
            System.out.println("Warning: test file '" + testdataPath + "" + "' for test '" + getClass().getName() + "' not available.");
            skipTests = true;
        }
        return null;
    }
}