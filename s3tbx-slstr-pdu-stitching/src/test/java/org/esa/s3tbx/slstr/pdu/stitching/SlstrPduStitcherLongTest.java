package org.esa.s3tbx.slstr.pdu.stitching;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.test.LongTestRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * @author Tonio Fincke
 */
@RunWith(LongTestRunner.class)
public class SlstrPduStitcherLongTest {

    private File targetDirectory;

    @Before
    public void setUp() {
        targetDirectory = new File("test_out");
        if (!targetDirectory.mkdirs()) {
            fail("Unable to create test target directory");
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
    public void testStitchPDUs_AllSlstrL1BProductFiles() throws IOException, PDUStitchingException,
            TransformerException, ParserConfigurationException, URISyntaxException {
        final File[] slstrFiles = TestUtils.getSlstrFiles();
        final File stitchedProductFile = SlstrPduStitcher.createStitchedSlstrL1BFile(targetDirectory, slstrFiles,
                null, ProgressMonitor.NULL);
        assertNotNull(stitchedProductFile);
        final File stitchedProductFileParentDirectory = stitchedProductFile.getParentFile();
        assert(new File(stitchedProductFileParentDirectory, "xfdumanifest.xml").exists());
        assert(new File(stitchedProductFileParentDirectory, "F1_BT_io.nc").exists());
        assert(new File(stitchedProductFileParentDirectory, "met_tx.nc").exists());
        assert(new File(stitchedProductFileParentDirectory, "viscal.nc").exists());
        assertEquals(targetDirectory, stitchedProductFileParentDirectory.getParentFile());
    }

}