package org.esa.s3tbx.dataio.s3.aatsr;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.regex.Pattern;

import static org.esa.s3tbx.dataio.s3.aatsr.AatsrLevel1ProductReaderPlugIn.DIRECTORY_NAME_PATTERN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AatsrLevel1ProductReaderPlugInTest {

    private AatsrLevel1ProductReaderPlugIn plugIn;

    @Before
    public void setUp() {
        plugIn = new AatsrLevel1ProductReaderPlugIn();
    }

    @Test
    public void testGetFormatNames() {
        assertEquals(1, plugIn.getFormatNames().length);
        assertEquals("ATS_L1_S3", plugIn.getFormatNames()[0]);
    }

    @Test
    public void testGetInputTypes() {
        assertEquals(2, plugIn.getInputTypes().length);
        assertEquals(String.class, plugIn.getInputTypes()[0]);
        assertEquals(File.class, plugIn.getInputTypes()[1]);
    }

    @Test
    public void testCreateReaderInstance() {
        assertEquals(AatsrLevel1ProductReader.class, plugIn.createReaderInstance().getClass());
    }

    @Test
    public void testDirectoryNamePattern() {
        assertTrue(Pattern.matches(DIRECTORY_NAME_PATTERN,"ENV_AT_1_RBT____20110107T111532_20110107T130045_20180928T095029_6312_098_166______TPZ_R_NT_004.SEN3"));
        assertTrue(Pattern.matches(DIRECTORY_NAME_PATTERN, "ER1_AT_1_RBT____19910901T061936_19910901T080223_20180928T124040_6167_014_005______TPZ_R_NT_004.SEN3"));
        assertTrue(Pattern.matches(DIRECTORY_NAME_PATTERN, "ER2_AT_1_RBT____19960702T203517_19960702T221812_20180928T150751_6175_012_414______TPZ_R_NT_004.SEN3"));
    }
}