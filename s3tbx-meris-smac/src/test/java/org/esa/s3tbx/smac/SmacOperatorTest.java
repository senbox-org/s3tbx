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
import org.esa.snap.test.LongTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Thomas Storm
 * @author Marco Peters
 * @author Sabine Embacher
 */
@RunWith(LongTestRunner.class)
public class SmacOperatorTest {

    private static final URL PRODUCT_URL_MERIS = SmacOperatorTest.class.getResource("MER_RR__1PQBCM20030407_100459_000007352015_00194_05759_0002_subset.nc");
    private static final URL PRODUCT_URL_AATSR = SmacOperatorTest.class.getResource("ATS_TOA_1CNPDK20030504_111259_000000572016_00080_06146_0157.nc");

    private static String getProductPathMeris() throws URISyntaxException {
        return new URI(PRODUCT_URL_MERIS.toString()).getPath();
    }

    private static String getProductPathAatsr() throws URISyntaxException {
        return new URI(PRODUCT_URL_AATSR.toString()).getPath();
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
    public void testOutputSignature_MERIS() throws IOException, URISyntaxException {
        final Product product = ProductIO.readProduct(getProductPathMeris());

        try {
            HashMap<String, Object> params = new HashMap<>();
            params.put("bandNames", new String[]{"radiance_5", "radiance_8", "radiance_9"});
            final SmacOperator smacOperator = createOp(params, product);
            Product smacResult = smacOperator.getTargetProduct();

            // generated
            assertTrue(smacResult.containsBand("reflec_5"));
            assertTrue(smacResult.containsBand("reflec_8"));
            assertTrue(smacResult.containsBand("reflec_9"));
            assertTrue(smacResult.containsRasterDataNode("smac_mask"));
            // copied from source
            assertTrue(smacResult.containsBand("l1_flags"));
            assertTrue(smacResult.containsRasterDataNode("coastline"));
            assertTrue(smacResult.containsRasterDataNode("land"));
            assertTrue(smacResult.containsRasterDataNode("water"));
            assertTrue(smacResult.containsRasterDataNode("cosmetic"));
            assertTrue(smacResult.containsRasterDataNode("duplicated"));
            assertTrue(smacResult.containsRasterDataNode("glint_risk"));
            assertTrue(smacResult.containsRasterDataNode("suspect"));
            assertTrue(smacResult.containsRasterDataNode("bright"));
            assertTrue(smacResult.containsRasterDataNode("invalid"));
            // tie-points copied (not testing all)
            assertTrue(smacResult.containsRasterDataNode("latitude"));
            assertTrue(smacResult.containsRasterDataNode("dem_alt"));
            assertTrue(smacResult.containsRasterDataNode("view_zenith"));
            assertTrue(smacResult.containsRasterDataNode("ozone"));


            assertNotNull(smacResult.getSceneGeoCoding());
            assertNotNull(smacResult.getFlagCodingGroup().get("l1_flags"));
            assertNotNull(smacResult.getMetadataRoot().getElement("Processing_Graph"));
        } finally {
            product.dispose();
        }

    }
    @Test
    public void testOutputSignature_AATSR() throws IOException, URISyntaxException {
        final Product product = ProductIO.readProduct(getProductPathAatsr());

        try {
            HashMap<String, Object> params = new HashMap<>();
        params.put("bandNames", new String[]{"btemp_nadir_1200", "btemp_nadir_1100", "reflec_nadir_1600",
                "reflec_nadir_0870", "btemp_fward_0370", "reflec_nadir_0550"});
            final SmacOperator smacOperator = createOp(params, product);
            Product smacResult = smacOperator.getTargetProduct();

            // generated (testing just some)
            assertTrue(smacResult.containsBand("btemp_nadir_1200"));
            assertTrue(smacResult.containsBand("btemp_nadir_1100"));
            assertTrue(smacResult.containsBand("reflec_nadir_1600"));
            assertTrue(smacResult.containsBand("reflec_nadir_0870"));
            assertTrue(smacResult.containsBand("btemp_fward_0370"));
            assertTrue(smacResult.containsBand("reflec_nadir_0550"));
            assertTrue(smacResult.containsRasterDataNode("smac_mask"));
            assertTrue(smacResult.containsRasterDataNode("smac_mask_forward"));
            // copied from source (testing just some)
            assertTrue(smacResult.containsBand("confid_flags_nadir"));
            assertTrue(smacResult.containsBand("confid_flags_fward"));
            assertTrue(smacResult.containsBand("cloud_flags_nadir"));
            assertTrue(smacResult.containsBand("cloud_flags_fward"));
            assertTrue(smacResult.containsRasterDataNode("qln_blanking"));
            assertTrue(smacResult.containsRasterDataNode("qln_saturation"));
            assertTrue(smacResult.containsRasterDataNode("qlf_blanking"));
            assertTrue(smacResult.containsRasterDataNode("qlf_saturation"));
            assertTrue(smacResult.containsRasterDataNode("cln_cloudy"));
            assertTrue(smacResult.containsRasterDataNode("clf_sun_glint"));
            assertTrue(smacResult.containsRasterDataNode("clf_sun_glint"));
            // tie-points copied (not testing all)
            assertTrue(smacResult.containsRasterDataNode("latitude"));
            assertTrue(smacResult.containsRasterDataNode("altitude"));
            assertTrue(smacResult.containsRasterDataNode("sun_elev_nadir"));
            assertTrue(smacResult.containsRasterDataNode("view_azimuth_fward"));


            assertNotNull(smacResult.getSceneGeoCoding());
            assertNotNull(smacResult.getFlagCodingGroup().get("confid_flags_nadir"));
            assertNotNull(smacResult.getFlagCodingGroup().get("confid_flags_fward"));
            assertNotNull(smacResult.getFlagCodingGroup().get("cloud_flags_nadir"));
            assertNotNull(smacResult.getFlagCodingGroup().get("cloud_flags_fward"));
            assertNotNull(smacResult.getMetadataRoot().getElement("Processing_Graph"));
        } finally {
            product.dispose();
        }

    }

    @Test
    public void testOperatorOnSampleProduct_useMerisADS_true() throws IOException, URISyntaxException {
        final boolean useMerisADS_ECMWF = true;

        HashMap<String, Object> params = new HashMap<>();
        params.put("tauAero550", "0.2");
        params.put("uH2o", "3.0");
        params.put("uO3", "0.15");
        params.put("surfPress", "1013.0");
        params.put("useMerisADS", String.valueOf(useMerisADS_ECMWF));    // if set to false it always works
        params.put("bandNames", new String[]{"radiance_5", "radiance_8", "radiance_9"});

        final Product product = ProductIO.readProduct(getProductPathMeris());

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
    public void testOperatorOnSampleProduct_useMerisADS_false() throws IOException, URISyntaxException {
        final boolean useMerisADS_ECMWF = false;

        HashMap<String, Object> params = new HashMap<>();
        params.put("tauAero550", "0.2");
        params.put("uH2o", "3.0");
        params.put("uO3", "0.15");
        params.put("surfPress", "1013.0");
        params.put("useMerisADS", String.valueOf(useMerisADS_ECMWF));    // if set to false it always works
        params.put("bandNames", new String[]{"radiance_5", "radiance_8", "radiance_9"});

        final Product product = ProductIO.readProduct(getProductPathMeris());

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
    public void testOperatorOnSampleProduct_OperatorDirect_useMerisADS_true() throws IOException, URISyntaxException {
        final boolean useMerisADS_ECMWF = true;

        HashMap<String, Object> params = new HashMap<>();
        params.put("tauAero550", "0.2");
        params.put("uH2o", "3.0");
        params.put("uO3", "0.15");
        params.put("surfPress", "1013.0");
        params.put("useMerisADS", String.valueOf(useMerisADS_ECMWF));    // if set to false it always works
        params.put("bandNames", new String[]{"radiance_5", "radiance_8", "radiance_9"});

        final Product product = ProductIO.readProduct(getProductPathMeris());

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
    public void testOperatorOnSampleProduct_OperatorDirect_useMerisADS_false() throws IOException, URISyntaxException {
        final boolean useMerisADS_ECMWF = false;

        HashMap<String, Object> params = new HashMap<>();
        params.put("tauAero550", "0.2");
        params.put("uH2o", "3.0");
        params.put("uO3", "0.15");
        params.put("surfPress", "1013.0");
        params.put("useMerisADS", String.valueOf(useMerisADS_ECMWF));    // if set to false it always works
        params.put("bandNames", new String[]{"radiance_5", "radiance_8", "radiance_9"});

        final Product product = ProductIO.readProduct(getProductPathMeris());

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