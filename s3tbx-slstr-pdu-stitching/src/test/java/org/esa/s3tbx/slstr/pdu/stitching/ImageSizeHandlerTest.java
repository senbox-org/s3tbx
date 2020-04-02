package org.esa.s3tbx.slstr.pdu.stitching;

import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static junit.framework.TestCase.assertEquals;

/**
 * @author Tonio Fincke
 */
public class ImageSizeHandlerTest {

    @Test
    public void testExtractImageSizes() throws IOException, URISyntaxException {
        final ImageSize[] imageSizes1 =
                ImageSizeHandler.extractImageSizes(createXmlDocument(new FileInputStream(getFirstSlstrFile())));
        assertEquals(2, imageSizes1.length);
        assertEquals(new ImageSize("tn", 21687, 64, 2000, 130), imageSizes1[0]);
        assertEquals(new ImageSize("to", 21687, 64, 2000, 130), imageSizes1[1]);

        final ImageSize[] imageSizes2 =
                ImageSizeHandler.extractImageSizes(createXmlDocument(new FileInputStream(getSecondSlstrFile())));
        assertEquals(3, imageSizes2.length);
        assertEquals(new ImageSize("tn", 23687, 64, 2000, 130), imageSizes2[0]);
        assertEquals(new ImageSize("io", 23687, 450, 2000, 900), imageSizes2[1]);
        assertEquals(new ImageSize("to", 23687, 64, 2000, 130), imageSizes2[2]);

        final ImageSize[] imageSizes3 =
                ImageSizeHandler.extractImageSizes(createXmlDocument(new FileInputStream(getThirdSlstrFile())));
        assertEquals(2, imageSizes3.length);
        assertEquals(new ImageSize("tn", 25687, 64, 2000, 130), imageSizes3[0]);
        assertEquals(new ImageSize("to", 25687, 64, 2000, 130), imageSizes3[1]);
    }

    @Test
    public void testCreateTargetImageSize() {
        ImageSize[] imageSizes = new ImageSize[]{
                new ImageSize("in", 21687, 998, 2000, 1500),
                new ImageSize("in", 23687, 445, 2000, 1500),
                new ImageSize("in", 25687, 1443, 2000, 1500)};

        final ImageSize targetImageSize = ImageSizeHandler.createTargetImageSize(imageSizes);

        Assert.assertNotNull(targetImageSize);
        assertEquals("in", targetImageSize.getIdentifier());
        assertEquals(21687, targetImageSize.getStartOffset());
        assertEquals(445, targetImageSize.getTrackOffset());
        assertEquals(6000, targetImageSize.getRows());
        assertEquals(2498, targetImageSize.getColumns());
    }

    private static Document createXmlDocument(InputStream inputStream) throws IOException {
        final String msg = "Cannot create document from manifest XML file.";
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream);
        } catch (SAXException | ParserConfigurationException e) {
            throw new IOException(msg, e);
        }
    }

    private static File getFirstSlstrFile() throws URISyntaxException {
        return getResource(TestUtils.FIRST_FILE_NAME);
    }

    private static File getSecondSlstrFile() throws URISyntaxException {
        return getResource(TestUtils.SECOND_FILE_NAME);
    }

    private static File getThirdSlstrFile() throws URISyntaxException {
        return getResource(TestUtils.THIRD_FILE_NAME);
    }

    private static File getResource(String fileName) throws URISyntaxException {
        final String fullFileName = fileName + "/xfdumanifest.xml";
        final URL resource = ImageSizeHandlerTest.class.getResource(fullFileName);
        URI uri = new URI(resource.toString());
        return new File(uri.getPath());
    }

}