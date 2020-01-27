package org.esa.s3tbx.dataio.s3.util;

import org.esa.snap.core.datamodel.Product;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;

/**
 * @author Tonio Fincke
 */
public class S3NetcdfReaderTest {

    private S3NetcdfReader reader;

    @Before
    public void setUp() throws IOException {
        reader = new S3NetcdfReader();
    }

    @Test
    public void testReadProduct() throws Exception {
        final Product product = reader.readProductNodes(getTestFilePath("../../s3/FRP_in.nc"), null);
        assertNotNull(product);
        assertEquals("FRP_in", product.getName());
        assertEquals("NetCDF", product.getProductType());
        assertEquals(1568, product.getSceneRasterWidth());
        assertEquals(266, product.getSceneRasterHeight());
    }

    private String getTestFilePath(String name) throws URISyntaxException {
        URL url = getClass().getResource(name);
        URI uri = new URI(url.toString());
        return uri.getPath();
    }

}
