package org.esa.s3tbx.dataio;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.runtime.Config;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

import static org.esa.snap.core.datamodel.GeoCodingFactory.USE_ALTERNATE_PIXEL_GEO_CODING_PROPERTY;
import static org.junit.Assert.*;

public class TestGeocoding {

    // Raster size	4865 x 4091
    @Test
    @Ignore
    public void testForwardInverse() throws IOException {
        final Product product = ProductIO.readProduct("/data/EOdata/SNAP_GEOCODING/S3A_OL_1_EFR____20180527T193613_20180527T193913_20180604T114027_0179_031_327_3420_LN1_O_NT_002.SEN3/xfdumanifest.xml");
        assertNotNull(product);

        try {
            // TiePointGeoCoding
            // -----------------
            final GeoCoding geoCoding = product.getSceneGeoCoding();
            assertTrue(geoCoding instanceof TiePointGeoCoding);

            GeoPos geoPos = geoCoding.getGeoPos(new PixelPos(100.5, 100.5), null);
            assertEquals(-156.62610629449705, geoPos.lon, 1e-8);
            assertEquals(-20.911497387568154, geoPos.lat, 1e-8);

            PixelPos pixelPos = geoCoding.getPixelPos(new GeoPos(-20.909884104645705, -156.62610629449705), null);
            assertEquals(100.34747412917713, pixelPos.getX(), 1e-8);
            assertEquals(99.96540832571223, pixelPos.getY(), 1e-8);

            geoPos = geoCoding.getGeoPos(new PixelPos(1000.5, 1000.5), null);
            assertEquals(-155.07309155651424, geoPos.lon, 1e-8);
            assertEquals(-23.814745574030454, geoPos.lat, 1e-8);

            pixelPos = geoCoding.getPixelPos(new GeoPos(-23.814745574030454, -155.07309155651424), null);
            assertEquals(1000.2381897034645, pixelPos.getX(), 1e-8);
            assertEquals(1000.4490167488432, pixelPos.getY(), 1e-8);

            geoPos = geoCoding.getGeoPos(new PixelPos(4000.5, 4000.5), null);
            assertEquals(-149.33252087402803, geoPos.lon, 1e-8);
            assertEquals(-33.370952189436586, geoPos.lat, 1e-8);

            pixelPos = geoCoding.getPixelPos(new GeoPos(-33.370952189436586, -149.33252087402803), null);
            assertEquals(4000.5222656045316, pixelPos.getX(), 1e-8);
            assertEquals(4000.579633251839, pixelPos.getY(), 1e-8);

            // ---------------------------------------
            // check border pixels - TiePointGeoCoding
            // ---------------------------------------
            geoPos = geoCoding.getGeoPos(new PixelPos(4864.5, 2000.5), null);
            assertEquals(-145.6376257633207, geoPos.lon, 1e-8);
            assertEquals(-28.652977378727655, geoPos.lat, 1e-8);

            pixelPos = geoCoding.getPixelPos(new GeoPos(-28.652977378727655, -145.6376257633207), null);
            assertEquals(4864.8394797580495, pixelPos.getX(), 1e-8);
            assertEquals(2000.2886280975745, pixelPos.getY(), 1e-8);

            geoPos = geoCoding.getGeoPos(new PixelPos(0.5, 2100.5), null);
            assertEquals(-158.55297493709136, geoPos.lon, 1e-8);
            assertEquals(-25.908400953935274, geoPos.lat, 1e-8);

            pixelPos = geoCoding.getPixelPos(new GeoPos(-25.908400953935274, -158.55297493709136), null);
            assertEquals(0.7087610434670637, pixelPos.getX(), 1e-8);
            assertEquals(2100.456800795308, pixelPos.getY(), 1e-8);

            geoPos = geoCoding.getGeoPos(new PixelPos(2200.5, 0.5), null);
            assertEquals(-151.2752191102679, geoPos.lon, 1e-8);
            assertEquals(-22.01070630523416, geoPos.lat, 1e-8);

            pixelPos = geoCoding.getPixelPos(new GeoPos(-22.01070630523416, -151.2752191102679), null);
            assertEquals(2200.306015087945, pixelPos.getX(), 1e-8);
            assertEquals(0.5635152529131133, pixelPos.getY(), 1e-8);

            geoPos = geoCoding.getGeoPos(new PixelPos(2300.5, 4090.5), null);
            assertEquals(-154.16094218111306, geoPos.lon, 1e-8);
            assertEquals(-32.57973295284413, geoPos.lat, 1e-8);

            pixelPos = geoCoding.getPixelPos(new GeoPos(-32.57973295284413, -154.16094218111306), null);
            assertEquals(2300.4915504336673, pixelPos.getX(), 1e-8);
            assertEquals(4090.5376611716874, pixelPos.getY(), 1e-8);

            // PixelGeoCoding2
            // ---------------
            final BasicPixelGeoCoding pixelGeoCoding2 = GeoCodingFactory.createPixelGeoCoding(product.getBand("latitude"),
                    product.getBand("longitude"),
                    "",
                    5);

            geoPos = pixelGeoCoding2.getGeoPos(new PixelPos(100.5, 100.5), null);
            assertEquals(-156.626957, geoPos.lon, 1e-8);
            assertEquals(-20.909883, geoPos.lat, 1e-8);

            pixelPos = pixelGeoCoding2.getPixelPos(new GeoPos(-20.909883, -156.626957), null);
            assertEquals(100.5, pixelPos.getX(), 1e-8);
            assertEquals(100.5, pixelPos.getY(), 1e-8);

            geoPos = pixelGeoCoding2.getGeoPos(new PixelPos(1000.5, 1000.5), null);
            assertEquals(-155.07592499999998, geoPos.lon, 1e-8);
            assertEquals(-23.814293, geoPos.lat, 1e-8);

            pixelPos = pixelGeoCoding2.getPixelPos(new GeoPos(-23.814293, -155.07592499999998), null);
            assertEquals(999.5, pixelPos.getX(), 1e-8);
            assertEquals(1000.5, pixelPos.getY(), 1e-8);

            geoPos = pixelGeoCoding2.getGeoPos(new PixelPos(4000.5, 4000.5), null);
            assertEquals(-149.334552, geoPos.lon, 1e-8);
            assertEquals(-33.370613, geoPos.lat, 1e-8);

            pixelPos = pixelGeoCoding2.getPixelPos(new GeoPos(-33.370613, -149.334552), null);
            assertEquals(4000.5, pixelPos.getX(), 1e-8);
            assertEquals(4000.5, pixelPos.getY(), 1e-8);

            // -------------------------------------
            // check border pixels - PixelGeoCoding2
            // -------------------------------------
            geoPos = pixelGeoCoding2.getGeoPos(new PixelPos(4864.5, 2000.5), null);
            assertEquals(-145.638679, geoPos.lon, 1e-8);
            assertEquals(-28.651436999999998, geoPos.lat, 1e-8);

            pixelPos = pixelGeoCoding2.getPixelPos(new GeoPos(-28.651436999999998, -145.638679), null);
            assertEquals(4864.5, pixelPos.getX(), 1e-8);
            assertEquals(2000.5, pixelPos.getY(), 1e-8);

            geoPos = pixelGeoCoding2.getGeoPos(new PixelPos(0.5, 2100.5), null);
            assertEquals(-158.553825, geoPos.lon, 1e-8);
            assertEquals(-25.906769999999998, geoPos.lat, 1e-8);

            pixelPos = pixelGeoCoding2.getPixelPos(new GeoPos(-25.906769999999998, -158.553825), null);
            assertEquals(0.5, pixelPos.getX(), 1e-8);
            assertEquals(2100.5, pixelPos.getY(), 1e-8);

            geoPos = pixelGeoCoding2.getGeoPos(new PixelPos(2200.5, 0.5), null);
            assertEquals(-151.277994, geoPos.lon, 1e-8);
            assertEquals(-22.010883, geoPos.lat, 1e-8);

            pixelPos = pixelGeoCoding2.getPixelPos(new GeoPos(-22.010883, -151.277994), null);
            assertEquals(2200.5, pixelPos.getX(), 1e-8);
            assertEquals(0.5, pixelPos.getY(), 1e-8);

            geoPos = pixelGeoCoding2.getGeoPos(new PixelPos(2300.5, 4090.5), null);
            assertEquals(-154.163175, geoPos.lon, 1e-8);
            assertEquals(-32.580104, geoPos.lat, 1e-8);

            pixelPos = pixelGeoCoding2.getPixelPos(new GeoPos(-32.580104, -154.163175), null);
            assertEquals(2300.5, pixelPos.getX(), 1e-8);
            assertEquals(4090.5, pixelPos.getY(), 1e-8);

            Config.instance().preferences().put(USE_ALTERNATE_PIXEL_GEO_CODING_PROPERTY, "true");
            final BasicPixelGeoCoding oldPixelGeoCoding = GeoCodingFactory.createPixelGeoCoding(product.getBand("latitude"),
                    product.getBand("longitude"),
                    "",
                    5);
            assertTrue(oldPixelGeoCoding instanceof PixelGeoCoding);

            geoPos = oldPixelGeoCoding.getGeoPos(new PixelPos(100.5, 100.5), null);
            assertEquals(-156.626957, geoPos.lon, 1e-8);
            assertEquals(-20.909883, geoPos.lat, 1e-8);

            pixelPos = oldPixelGeoCoding.getPixelPos(new GeoPos(-20.909883, -156.626957), null);
            assertEquals(100.5, pixelPos.getX(), 1e-8);
            assertEquals(100.5, pixelPos.getY(), 1e-8);

            geoPos = oldPixelGeoCoding.getGeoPos(new PixelPos(4000.5, 4000.5), null);
            assertEquals(-149.334552, geoPos.lon, 1e-8);
            assertEquals(-33.370613, geoPos.lat, 1e-8);

            pixelPos = oldPixelGeoCoding.getPixelPos(new GeoPos(-33.370613, -149.334552), null);
            assertEquals(4000.5, pixelPos.getX(), 1e-8);
            assertEquals(4000.5, pixelPos.getY(), 1e-8);

        } finally {
            product.dispose();
        }
    }
}
