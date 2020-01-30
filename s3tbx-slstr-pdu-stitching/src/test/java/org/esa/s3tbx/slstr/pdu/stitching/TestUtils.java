package org.esa.s3tbx.slstr.pdu.stitching;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * @author Tonio Fincke
 */
public class TestUtils {

    public static String FIRST_FILE_NAME =
            "S3A_SL_1_RBT____20130707T153252_20130707T153752_20150217T183530_0299_158_182______SVL_O_NR_001.SEN3";
    public static String SECOND_FILE_NAME =
            "S3A_SL_1_RBT____20130707T153752_20130707T154252_20150217T183530_0299_158_182______SVL_O_NR_001.SEN3";
    public static String THIRD_FILE_NAME =
            "S3A_SL_1_RBT____20130707T154252_20130707T154752_20150217T183537_0299_158_182______SVL_O_NR_001.SEN3";

    static File[] getSlstrFiles() throws URISyntaxException {
        return new File[]{getFirstSlstrFile(), getSecondSlstrFile(), getThirdSlstrFile()};
    }

    static File getFirstSlstrFile() throws URISyntaxException {
        return getResource(TestUtils.FIRST_FILE_NAME);
    }

    static File getSecondSlstrFile() throws URISyntaxException {
        return getResource(TestUtils.SECOND_FILE_NAME);
    }

    static File getThirdSlstrFile() throws URISyntaxException {
        return getResource(TestUtils.THIRD_FILE_NAME);
    }

    private static File getResource(String fileName) throws URISyntaxException {
        final String fullFileName = fileName + "/xfdumanifest.xml";
        final URL resource = TestUtils.class.getResource(fullFileName);
        URI uri = new URI(resource.toString());
        return new File(uri.getPath());
    }
}
