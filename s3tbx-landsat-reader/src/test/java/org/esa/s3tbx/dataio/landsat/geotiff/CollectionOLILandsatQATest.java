package org.esa.s3tbx.dataio.landsat.geotiff;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.junit.Assume;
import org.junit.Test;

import java.awt.Color;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Created by obarrile on 09/01/2019.
 */
public class CollectionOLILandsatQATest {
    @Test
    public void testCreateMasks() {
        CollectionOLILandsatQA landsatQA = new CollectionOLILandsatQA();
        java.util.List<Mask> masks = landsatQA.createMasks(new Dimension(1, 1));
        assertEquals(masks.size(),19);
    }

    @Test
    public void testConsistentMaskColors() throws IOException {
        File file1 = new File("Y:\\testdata\\sensors_platforms\\LANDSAT\\LANDSAT_8_OLI_TIRS\\geotiff\\LC81970222013122LGN01\\LC81970222013122LGN01_MTL.txt");
        File file2 = new File("Y:\\testdata\\sensors_platforms\\LANDSAT\\LANDSAT_8_OLI_TIRS\\geotiff\\LC81960222013195LGN00\\LC81960222013195LGN00_MTL.txt");

        Assume.assumeTrue(file1.exists());
        Assume.assumeTrue(file2.exists());

        Product product1 = ProductIO.readProduct(file1);
        assertEquals(Color.RED, product1.getMaskGroup().get("designated_fill").getImageColor());


        Product product2 = ProductIO.readProduct(file2);
        assertEquals(Color.RED, product2.getMaskGroup().get("designated_fill").getImageColor());

    }

}
