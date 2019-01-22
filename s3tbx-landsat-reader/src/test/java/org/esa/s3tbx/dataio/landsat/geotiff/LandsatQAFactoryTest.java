package org.esa.s3tbx.dataio.landsat.geotiff;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by obarrile on 02/01/2019.
 */
public class LandsatQAFactoryTest {
    @Test
    public void testPreCollectionQA() throws Exception {
        File testFile = new File(getClass().getResource("test_L8_MTL.txt").getFile());
        LandsatQA landsatQA = LandsatQAFactory.create(testFile);
        assertTrue(landsatQA instanceof PreCollectionLandsatQA);
    }
    @Test
    public void testCollectionOLIQA() throws Exception {
        File testFile = new File(getClass().getResource("test_CollectionOLI.txt").getFile());
        LandsatQA landsatQA = LandsatQAFactory.create(testFile);
        assertTrue(landsatQA instanceof CollectionOLILandsatQA);
    }
    @Test
    public void testNoQA() throws Exception {
        File testFile = new File(getClass().getResource("test_7_reproc_MTL.txt").getFile());
        LandsatQA landsatQA = LandsatQAFactory.create(testFile);
        assertNull(landsatQA);
    }
}
