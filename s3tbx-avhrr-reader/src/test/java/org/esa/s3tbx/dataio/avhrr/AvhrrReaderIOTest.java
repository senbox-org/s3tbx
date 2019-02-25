package org.esa.s3tbx.dataio.avhrr;

import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.junit.Test;

import java.io.IOException;

public class AvhrrReaderIOTest {

    @Test
    public void testRead() throws IOException {
        final AvhrrReaderPlugIn plugIn = new AvhrrReaderPlugIn();
        final ProductReader reader = plugIn.createReaderInstance();

        try {
            final Product product = reader.readProductNodes("/usr/local/data/Fiduceo_TestData/avhrr-frac-ma/v1/2018/05/11/NSS.FRAC.M2.D18131.S1404.E1544.B5998081.SV", null);

            final GeoCoding geoCoding = product.getSceneGeoCoding();

            final GeoPos geoPos = geoCoding.getGeoPos(new PixelPos(0, 0), null);
            final PixelPos pixelPos = geoCoding.getPixelPos(geoPos, null);
            System.out.println("pixelPos = " + pixelPos);

        } finally {
            reader.close();
        }


    }
}
