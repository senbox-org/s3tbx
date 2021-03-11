package org.esa.s3tbx.dataio.landsat.geotiff;

import org.junit.Test;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by obarrile on 02/01/2019.
 */
public class LandsatQAFactoryTest {
    @Test
    public void testPreCollectionQA() throws Exception {
        File testFile = getTestFile("test_L8_MTL.txt");
        LandsatQA landsatQA = LandsatQAFactory.create(testFile);
        assertTrue(landsatQA instanceof PreCollectionLandsatQA);
    }
    @Test
    public void testCollectionOLIQA() throws Exception {
        File testFile = getTestFile("test_CollectionOLI.txt");
        LandsatQA landsatQA = LandsatQAFactory.create(testFile);
        assertTrue(landsatQA instanceof CollectionOLILandsatQA);
    }
    @Test
    public void testNoQA() throws Exception {
        File testFile = getTestFile("test_7_reproc_MTL.txt");
        LandsatQA landsatQA = LandsatQAFactory.create(testFile);
        assertNull(landsatQA);
    }

    private File getTestFile(String name) throws URISyntaxException {
        URL url = getClass().getResource(name);
        URI uri = new URI(url.toString());
        return new File(uri.getPath());
    }
}
