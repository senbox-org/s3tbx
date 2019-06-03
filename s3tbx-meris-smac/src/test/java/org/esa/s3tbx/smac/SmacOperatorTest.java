/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.s3tbx.smac;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.GPF;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;

/**
 * @author Thomas Storm
 */
public class SmacOperatorTest {

    private static final URL PRODUCT_URL = SmacOperatorTest.class.getResource("MER_RR__1PQBCM20030407_100459_000007352015_00194_05759_0002_subset.nc");
    private static final String PRODUCT_PATH = PRODUCT_URL.getPath();

    private static Product product;
    private static Path smacTestOutput;

    @BeforeClass
    public static void setUp() throws Exception {
        smacTestOutput = Files.createTempDirectory("smac");
        product = ProductIO.readProduct(PRODUCT_PATH);
    }

    @After
    public void tearDown() throws Exception {
        Files.walk(smacTestOutput).forEach(path -> {
            try {
                path.toFile().deleteOnExit();
                Files.deleteIfExists(path);
            } catch (IOException e) {
            }
        });
    }

    @Test
    public void testConvertAndRevert() {
        HashMap<String, String> map = new HashMap<>();
        assertEquals("reflec_2", SmacOperator.convertMerisBandName("radiance_2", map));
        assertEquals("reflec_5", SmacOperator.convertMerisBandName("radiance_5", map));
        assertEquals("reflec", SmacOperator.convertMerisBandName("kaputtnick", map));
        assertEquals("radiance_2", map.get("reflec_2"));
        assertEquals("radiance_5", map.get("reflec_5"));
        assertEquals("kaputtnick", map.get("reflec"));


        map.clear();
        assertEquals("radiance_2", SmacOperator.revertMerisBandName(SmacOperator.convertMerisBandName("radiance_2", map), map));
        assertEquals("radiance_5", SmacOperator.revertMerisBandName(SmacOperator.convertMerisBandName("radiance_5", map), map));
        assertEquals("kaputtnick", SmacOperator.revertMerisBandName(SmacOperator.convertMerisBandName("kaputtnick", map), map));

        assertEquals("i dont exist", SmacOperator.revertMerisBandName("i dont exist", map));
    }


    @Test
    public void tesOperatorOnSampleProduct() throws IOException {
        HashMap<String, Object> params = new HashMap<>();
        params.put("tauAero550", "0.2");
        params.put("uH2o", "3.0");
        params.put("uO3", "0.15");
        params.put("surfPress", "1013.0");
        params.put("useMerisADS", "true");    // if set to false it always works
        params.put("bandNames", new String[]{"radiance_5", "radiance_8", "radiance_9"});

        Product run1 = GPF.createProduct("SmacOP", params, product);
        ProductIO.writeProduct(run1, smacTestOutput.resolve("smacRun1.dim").toString(), "BEAM-DIMAP");
        Product run2 = GPF.createProduct("SmacOP", params, product);
        ProductIO.writeProduct(run2, smacTestOutput.resolve("smacRun2.dim").toString(), "BEAM-DIMAP");

        // if written and read in again, the results are different. Does the DIMAP writer and reader change the data?
//        run1.dispose();
//        run2.dispose();
//        run1 = ProductIO.readProduct("G:\\EOData\\temp\\smac\\smacRun1.dim");
//        run2 = ProductIO.readProduct("G:\\EOData\\temp\\smac\\smacRun2.dim");

        run1.getBand("reflec_5").readRasterDataFully(ProgressMonitor.NULL);
        run1.getBand("reflec_8").readRasterDataFully(ProgressMonitor.NULL);
        run1.getBand("reflec_9").readRasterDataFully(ProgressMonitor.NULL);
        run2.getBand("reflec_5").readRasterDataFully(ProgressMonitor.NULL);
        run2.getBand("reflec_8").readRasterDataFully(ProgressMonitor.NULL);
        run2.getBand("reflec_9").readRasterDataFully(ProgressMonitor.NULL);
        assertEquals(0.0686927, run1.getBand("reflec_5").getPixelFloat(85, 116), 1.0e-6);
        assertEquals(0.0686927, run2.getBand("reflec_5").getPixelFloat(85, 116), 1.0e-6);
        assertEquals(0.0804815, run1.getBand("reflec_8").getPixelFloat(85, 116), 1.0e-6);
        assertEquals(0.0804815, run2.getBand("reflec_8").getPixelFloat(85, 116), 1.0e-6);
        assertEquals(0.1117397, run1.getBand("reflec_9").getPixelFloat(85, 116), 1.0e-6);
        assertEquals(0.1117397, run2.getBand("reflec_9").getPixelFloat(85, 116), 1.0e-6);
    }
}
