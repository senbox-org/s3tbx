package org.esa.s3tbx.slstr.pdu.stitching;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.util.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * @author Tonio Fincke
 */
public class SlstrPduStitcherTest {

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
    public void testStitchPDUs_NotEmpty() {
        try {
            SlstrPduStitcher.createStitchedSlstrL1BFile(targetDirectory, new File[0], null, ProgressMonitor.NULL);
            fail("Exception expected");
        } catch (Exception e) {
            assertEquals("No product files provided", e.getMessage());
        }
    }

    @Test
    public void testStitchPDUs_OnlyOneSlstrL1BProductFile() throws Exception {
        final File firstSlstrFile = TestUtils.getFirstSlstrFile();

        final File stitchedProductFile = SlstrPduStitcher.createStitchedSlstrL1BFile(targetDirectory,
                new File[]{firstSlstrFile}, null, ProgressMonitor.NULL);

        final File slstrFileParentDirectory = firstSlstrFile.getParentFile();
        assertNotNull(stitchedProductFile);
        final File stitchedProductFileParentDirectory = stitchedProductFile.getParentFile();
        assertEquals(slstrFileParentDirectory.getName(), stitchedProductFileParentDirectory.getName());
        assertEquals(targetDirectory, stitchedProductFileParentDirectory.getParentFile());
        final File[] files = slstrFileParentDirectory.listFiles();
        assertNotNull(files);
        for (File slstrFile : files) {
            assert (new File(stitchedProductFileParentDirectory, slstrFile.getName()).exists());
        }
    }

    @Test
    public void testDecomposeSlstrName() throws URISyntaxException, PDUStitchingException {
        final SlstrPduStitcher.SlstrNameDecomposition firstSlstrNameDecomposition =
                SlstrPduStitcher.decomposeSlstrName(TestUtils.getFirstSlstrFile().getParentFile().getName());

        Date startTime = new GregorianCalendar(2013, Calendar.JULY, 7, 15, 32, 52).getTime();
        Date stopTime = new GregorianCalendar(2013, Calendar.JULY, 7, 15, 37, 52).getTime();
        assertEquals(startTime, firstSlstrNameDecomposition.startTime);
        assertEquals(stopTime, firstSlstrNameDecomposition.stopTime);
        assertEquals("0299", firstSlstrNameDecomposition.duration);
        assertEquals("158", firstSlstrNameDecomposition.cycleNumber);
        assertEquals("182", firstSlstrNameDecomposition.relativeOrbitNumber);
        assertEquals("____", firstSlstrNameDecomposition.frameAlongTrackCoordinate);
        assertEquals("SVL", firstSlstrNameDecomposition.fileGeneratingCentre);
        assertEquals("O", firstSlstrNameDecomposition.platform);
        assertEquals("NR", firstSlstrNameDecomposition.timelinessOfProcessingWorkflow);
        assertEquals("001", firstSlstrNameDecomposition.baselineCollectionOrDataUsage);
    }

    @Test
    public void testCreateParentDirectoryNameOfStitchedFile() throws URISyntaxException, PDUStitchingException {
        SlstrPduStitcher.SlstrNameDecomposition[] decompositions = new SlstrPduStitcher.SlstrNameDecomposition[3];
        decompositions[0] = SlstrPduStitcher.decomposeSlstrName(TestUtils.getFirstSlstrFile().getParentFile().getName());
        decompositions[1] = SlstrPduStitcher.decomposeSlstrName(TestUtils.getSecondSlstrFile().getParentFile().getName());
        decompositions[2] = SlstrPduStitcher.decomposeSlstrName(TestUtils.getThirdSlstrFile().getParentFile().getName());

        Date time = Calendar.getInstance().getTime();
        final String parentDirectoryNameOfStitchedFile =
                SlstrPduStitcher.createParentDirectoryNameOfStitchedFile(decompositions, time);

        final String now = new SimpleDateFormat("yyyyMMdd'T'HHmmss").format(time);
        assertEquals("S3A_SL_1_RBT____20130707T153252_20130707T154752_" + now + "_0299_158_182______SVL_O_NR_001.SEN3",
                              parentDirectoryNameOfStitchedFile);
    }

    @Test
    public void testCollectFiles() throws IOException, URISyntaxException {
        List<String> ncFiles = new ArrayList<>();
        final File[] slstrFiles = TestUtils.getSlstrFiles();
        for (File slstrFile : slstrFiles) {
            SlstrPduStitcher.collectFiles(ncFiles, createXmlDocument(new FileInputStream(slstrFile)));
        }

        assertEquals(3, ncFiles.size());
        assertEquals("met_tx.nc", ncFiles.get(0));
        assertEquals("viscal.nc", ncFiles.get(1));
        assertEquals("F1_BT_io.nc", ncFiles.get(2));
    }

    private static Document createXmlDocument(InputStream inputStream) throws IOException {
        final String msg = "Cannot create document from manifest XML file.";
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream);
        } catch (SAXException | ParserConfigurationException e) {
            throw new IOException(msg, e);
        }
    }

}