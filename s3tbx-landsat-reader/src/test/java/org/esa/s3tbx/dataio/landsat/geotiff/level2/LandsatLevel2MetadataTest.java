package org.esa.s3tbx.dataio.landsat.geotiff.level2;

import org.esa.s3tbx.dataio.landsat.metadata.XmlMetadataParser;
import org.esa.s3tbx.dataio.landsat.metadata.XmlMetadataParserFactory;
import org.esa.snap.core.datamodel.FlagCoding;
import org.junit.Before;
import org.junit.Test;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

import static org.junit.Assert.*;

/**
 * Created by obarrile on 07/02/2019.
 */
public class LandsatLevel2MetadataTest {

    private LandsatLevel2Metadata metadata;
    @Before
    public void setUp() throws Exception {
        URL url = getClass().getResource("LC08_L1TP_024036_20181011_20181011_01_RT.xml");
        URI uri = new URI(url.toString());
        String pathString = uri.getPath();
        //create metadata
        XmlMetadataParserFactory.registerParser(LandsatLevel2Metadata.class, new XmlMetadataParser<>(LandsatLevel2Metadata.class));


        InputStream metadataInputStream = new FileInputStream(pathString);
        if (metadataInputStream == null) {
            throw new IOException(String.format("Unable to read metadata file from product"));
        }

        try {
            metadata = (LandsatLevel2Metadata) XmlMetadataParserFactory.getParser(LandsatLevel2Metadata.class).parse(metadataInputStream);
        } catch (Exception e) {
            throw new IOException(String.format("Unable to parse metadata file"));
        }
    }

    @Test
    public void testProductName() throws Exception {
        String name = metadata.getProductName();
        assertEquals(name,"LC08_L1TP_024036_20181011_20181011_01_RT - Level 2");
    }


    @Test
    public void testRasterFileNames() throws Exception {
        String[] name = metadata.getRasterFileNames();
        assertEquals(name.length,10);
    }

    @Test
    public void testHeightAndWidth() throws Exception {
        int width = metadata.getRasterWidth();
        int height = metadata.getRasterHeight();
        assertEquals(height,7781);
        assertEquals(width,7641);
    }

    @Test
    public void testUpperLeft() throws Exception {
        Point2D.Double upperLeft = metadata.getUpperLeft();
        assertNotNull(upperLeft);
        assertEquals(upperLeft.x,473400,0.1);
        assertEquals(upperLeft.y,3946500,0.1);
    }

    @Test
    public void testSaturationFlags() throws Exception {
        FlagCoding flagCoding = metadata.createSaturationFlagCoding("LC08_L1TP_024036_20181011_20181011_01_RT_radsat_qa.tif");
        assertNotNull(flagCoding);
    }

    @Test
    public void testWavelength() throws Exception {
        Float wavelength = metadata.getWavelength("LC08_L1TP_024036_20181011_20181011_01_RT_sr_band3.tif");
        assertEquals(560.0,wavelength,1E-6);
    }

}
