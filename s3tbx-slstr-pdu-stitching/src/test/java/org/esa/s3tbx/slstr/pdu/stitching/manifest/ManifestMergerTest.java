package org.esa.s3tbx.slstr.pdu.stitching.manifest;


import org.esa.s3tbx.slstr.pdu.stitching.PDUStitchingException;
import org.esa.s3tbx.slstr.pdu.stitching.TestUtils;
import org.esa.snap.core.util.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Tonio Fincke
 */
public class ManifestMergerTest {

    private File targetDirectory;
    private Document manifest;
    private ManifestMerger manifestMerger;

    @Before
    public void setUp() {
        targetDirectory = new File("test_out");
        if (targetDirectory.exists()) {
            // Delete leftovers
            FileUtils.deleteTree(targetDirectory);
        }
        if (!targetDirectory.mkdirs()) {
            fail("Unable to create test target directory");
        }
        manifestMerger = new ManifestMerger();
        try {
            manifest = ManifestTestUtils.createDocument();
        } catch (ParserConfigurationException e) {
            fail(e.getMessage());
        }
    }

    @After
    public void tearDown() {
        if (targetDirectory.isDirectory()) {
            if (!FileUtils.deleteTree(targetDirectory)) {
                fail("Unable to delete test directory");
            }
        }
    }

    @Test
    public void testMergeManifests_OneFile() throws IOException, ParserConfigurationException, TransformerException, PDUStitchingException {
        final File inputManifest = getManifestFile(TestUtils.FIRST_FILE_NAME);
        final Date now = Calendar.getInstance().getTime();
        final File productDir = new File(ManifestMergerTest.class.getResource("").getFile());
        final File manifestFile = manifestMerger.createMergedManifest(new File[]{inputManifest}, now, productDir, 5000);
        assertTrue(manifestFile.exists());
    }

    @Test
    public void testMergeManifests_MultipleFiles() throws IOException, ParserConfigurationException, TransformerException, PDUStitchingException {
        final Date now = Calendar.getInstance().getTime();
        final File productDir = new File(ManifestMergerTest.class.getResource("").getFile());
        final File manifestFile = manifestMerger.createMergedManifest(getManifestFiles(), now, productDir, 5000);
        assertTrue(manifestFile.exists());
    }

    private static File[] getManifestFiles() {
        return new File[]{getManifestFile(TestUtils.FIRST_FILE_NAME),
                getManifestFile(TestUtils.SECOND_FILE_NAME),
                getManifestFile(TestUtils.THIRD_FILE_NAME)
        };
    }

    private static File getManifestFile(String fileName) {
        final String fullFileName = fileName + "/xfdumanifest.xml";
        final URL resource = ManifestMergerTest.class.getResource(fullFileName);
        return new File(resource.getFile());
    }


}
