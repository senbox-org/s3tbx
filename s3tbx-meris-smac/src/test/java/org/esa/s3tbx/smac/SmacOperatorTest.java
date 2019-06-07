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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.GPF;
import org.junit.*;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Thomas Storm
 * @author Marco Peters
 * @author Sabine Embacher
 */
public class SmacOperatorTest {

    private static final URL PRODUCT_URL = SmacOperatorTest.class.getResource("MER_RR__1PQBCM20030407_100459_000007352015_00194_05759_0002_subset.nc");
    private static final String PRODUCT_PATH = PRODUCT_URL.getPath();

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
    public void testOperatorOnSampleProduct_useMerisADS_true() throws IOException {
        final boolean useMerisADS_ECMWF = true;

        HashMap<String, Object> params = new HashMap<>();
        params.put("tauAero550", "0.2");
        params.put("uH2o", "3.0");
        params.put("uO3", "0.15");
        params.put("surfPress", "1013.0");
        params.put("useMerisADS", String.valueOf(useMerisADS_ECMWF));    // if set to false it always works
        params.put("bandNames", new String[]{"radiance_5", "radiance_8", "radiance_9"});

        final Product product = ProductIO.readProduct(PRODUCT_PATH);

        Product run1 = GPF.createProduct("SmacOP", params, product);
        Product run2 = GPF.createProduct("SmacOP", params, product);

        run1.getBand("reflec_5").readRasterDataFully(ProgressMonitor.NULL);
        run1.getBand("reflec_8").readRasterDataFully(ProgressMonitor.NULL);
        run1.getBand("reflec_9").readRasterDataFully(ProgressMonitor.NULL);
        run2.getBand("reflec_5").readRasterDataFully(ProgressMonitor.NULL);
        run2.getBand("reflec_8").readRasterDataFully(ProgressMonitor.NULL);
        run2.getBand("reflec_9").readRasterDataFully(ProgressMonitor.NULL);

        final float[] run1Values = {
                run1.getBand("reflec_5").getPixelFloat(85, 116),
                run1.getBand("reflec_8").getPixelFloat(85, 116),
                run1.getBand("reflec_9").getPixelFloat(85, 116)
        };
        final float[] run2Values = {
                run2.getBand("reflec_5").getPixelFloat(85, 116),
                run2.getBand("reflec_8").getPixelFloat(85, 116),
                run2.getBand("reflec_9").getPixelFloat(85, 116)
        };

        final float[] expected = {0.08310253F, 0.08464402F, 0.12101826F};
        assertThat(run1Values, is(equalTo(expected)));
        assertThat(run2Values, is(equalTo(expected)));
    }

    @Test
    public void testOperatorOnSampleProduct_useMerisADS_false() throws IOException {
        final boolean useMerisADS_ECMWF = false;

        HashMap<String, Object> params = new HashMap<>();
        params.put("tauAero550", "0.2");
        params.put("uH2o", "3.0");
        params.put("uO3", "0.15");
        params.put("surfPress", "1013.0");
        params.put("useMerisADS", String.valueOf(useMerisADS_ECMWF));    // if set to false it always works
        params.put("bandNames", new String[]{"radiance_5", "radiance_8", "radiance_9"});

        final Product product = ProductIO.readProduct(PRODUCT_PATH);

        Product run1 = GPF.createProduct("SmacOP", params, product);
        Product run2 = GPF.createProduct("SmacOP", params, product);

        run1.getBand("reflec_5").readRasterDataFully(ProgressMonitor.NULL);
        run1.getBand("reflec_8").readRasterDataFully(ProgressMonitor.NULL);
        run1.getBand("reflec_9").readRasterDataFully(ProgressMonitor.NULL);
        run2.getBand("reflec_5").readRasterDataFully(ProgressMonitor.NULL);
        run2.getBand("reflec_8").readRasterDataFully(ProgressMonitor.NULL);
        run2.getBand("reflec_9").readRasterDataFully(ProgressMonitor.NULL);

        final float[] run1Values = {
                run1.getBand("reflec_5").getPixelFloat(85, 116),
                run1.getBand("reflec_8").getPixelFloat(85, 116),
                run1.getBand("reflec_9").getPixelFloat(85, 116)
        };
        final float[] run2Values = {
                run2.getBand("reflec_5").getPixelFloat(85, 116),
                run2.getBand("reflec_8").getPixelFloat(85, 116),
                run2.getBand("reflec_9").getPixelFloat(85, 116)
        };

        final float[] expected = {0.0747551F, 0.0824052F, 0.11957148F};
        assertThat(run1Values, is(equalTo(expected)));
        assertThat(run2Values, is(equalTo(expected)));
    }


    @Test
    public void testOperatorOnSampleProduct_OperatorDirect_useMerisADS_true() throws IOException {
        final boolean useMerisADS_ECMWF = true;

        HashMap<String, Object> params = new HashMap<>();
        params.put("tauAero550", "0.2");
        params.put("uH2o", "3.0");
        params.put("uO3", "0.15");
        params.put("surfPress", "1013.0");
        params.put("useMerisADS", String.valueOf(useMerisADS_ECMWF));    // if set to false it always works
        params.put("bandNames", new String[]{"radiance_5", "radiance_8", "radiance_9"});

        final Product product = ProductIO.readProduct(PRODUCT_PATH);

        final SmacOperator smacOperator = createOp(params, product);
        Product run1 = smacOperator.getTargetProduct();

        final SmacOperator smacOperator2 = createOp(params, product);
        Product run2 = smacOperator2.getTargetProduct();

        run1.getBand("reflec_5").readRasterDataFully(ProgressMonitor.NULL);
        run1.getBand("reflec_8").readRasterDataFully(ProgressMonitor.NULL);
        run1.getBand("reflec_9").readRasterDataFully(ProgressMonitor.NULL);
        run2.getBand("reflec_5").readRasterDataFully(ProgressMonitor.NULL);
        run2.getBand("reflec_8").readRasterDataFully(ProgressMonitor.NULL);
        run2.getBand("reflec_9").readRasterDataFully(ProgressMonitor.NULL);

        final float[] run1Values = {
                run1.getBand("reflec_5").getPixelFloat(85, 116),
                run1.getBand("reflec_8").getPixelFloat(85, 116),
                run1.getBand("reflec_9").getPixelFloat(85, 116)
        };
        final float[] run2Values = {
                run2.getBand("reflec_5").getPixelFloat(85, 116),
                run2.getBand("reflec_8").getPixelFloat(85, 116),
                run2.getBand("reflec_9").getPixelFloat(85, 116)
        };

        final float[] expected = {0.08310253F, 0.08464402F, 0.12101826F};
        assertThat(run1Values, is(equalTo(expected)));
        assertThat(run2Values, is(equalTo(expected)));
    }

    @Test
    public void testOperatorOnSampleProduct_OperatorDirect_useMerisADS_false() throws IOException {
        final boolean useMerisADS_ECMWF = false;

        HashMap<String, Object> params = new HashMap<>();
        params.put("tauAero550", "0.2");
        params.put("uH2o", "3.0");
        params.put("uO3", "0.15");
        params.put("surfPress", "1013.0");
        params.put("useMerisADS", String.valueOf(useMerisADS_ECMWF));    // if set to false it always works
        params.put("bandNames", new String[]{"radiance_5", "radiance_8", "radiance_9"});

        final Product product = ProductIO.readProduct(PRODUCT_PATH);

        final SmacOperator smacOperator = createOp(params, product);
        Product run1 = smacOperator.getTargetProduct();

        final SmacOperator smacOperator2 = createOp(params, product);
        Product run2 = smacOperator2.getTargetProduct();

        run1.getBand("reflec_5").readRasterDataFully(ProgressMonitor.NULL);
        run1.getBand("reflec_8").readRasterDataFully(ProgressMonitor.NULL);
        run1.getBand("reflec_9").readRasterDataFully(ProgressMonitor.NULL);
        run2.getBand("reflec_5").readRasterDataFully(ProgressMonitor.NULL);
        run2.getBand("reflec_8").readRasterDataFully(ProgressMonitor.NULL);
        run2.getBand("reflec_9").readRasterDataFully(ProgressMonitor.NULL);

        final float[] run1Values = {
                run1.getBand("reflec_5").getPixelFloat(85, 116),
                run1.getBand("reflec_8").getPixelFloat(85, 116),
                run1.getBand("reflec_9").getPixelFloat(85, 116)
        };
        final float[] run2Values = {
                run2.getBand("reflec_5").getPixelFloat(85, 116),
                run2.getBand("reflec_8").getPixelFloat(85, 116),
                run2.getBand("reflec_9").getPixelFloat(85, 116)
        };

        final float[] expected = {0.0747551F, 0.0824052F, 0.11957148F};
        assertThat(run1Values, is(equalTo(expected)));
        assertThat(run2Values, is(equalTo(expected)));
    }

    private SmacOperator createOp(HashMap<String, Object> params, Product product) {
        final SmacOperator smacOperator = new SmacOperator();
        smacOperator.setParameterDefaultValues();
        smacOperator.setSourceProduct(product);
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            smacOperator.setParameter(entry.getKey(), entry.getValue());
        }
        return smacOperator;
    }
}