package org.esa.s3tbx.dataio.landsat.geotiff;

import org.esa.snap.core.datamodel.Mask;
import org.junit.Test;

import java.awt.*;
import java.io.File;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by obarrile on 09/01/2019.
 */
public class CollectionOLILandsatQATest {
    @Test
    public void testCreateMasks() throws Exception {
        CollectionOLILandsatQA landsatQA = new CollectionOLILandsatQA();
        java.util.List<Mask> masks = landsatQA.createMasks(new Dimension(1, 1));
        assertEquals(masks.size(),19);
    }
}
