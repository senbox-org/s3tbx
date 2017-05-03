package org.esa.s3tbx.olci.mph_chl;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.util.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * todo: add comment
 * To change this template use File | Settings | File Templates.
 * Date: 03.05.2017
 * Time: 17:18
 *
 * @author olafd
 */
public class MphChlOpAcceptanceTest {
    private File testOutDirectory;

    @Before
    public void setUp() {
        testOutDirectory = new File("output");
        if (!testOutDirectory.mkdirs()) {
            fail("unable to create test directory: " + testOutDirectory);
        }

        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(new MphChlOp.Spi());
    }

    @After
    public void tearDown() {
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(new MphChlOp.Spi());

        if (testOutDirectory != null) {
            if (!FileUtils.deleteTree(testOutDirectory)) {
                fail("Unable to delete test directory: " + testOutDirectory);
            }
        }
    }

    @Test
    @Ignore
    public void testComputeMphChlProduct() throws IOException {
        final Product brrProduct = OlciBrrProduct.create();

        MphChlOp mphChlOp = new MphChlOp();
        mphChlOp.setSourceProduct(brrProduct);
        mphChlOp.setParameterDefaultValues();
        mphChlOp.setParameter("applyLowPassFilter", false);
        final Product mphChlProduct = mphChlOp.getTargetProduct();

        Product savedProduct = null;
        try {
            final String targetProductPath = testOutDirectory.getAbsolutePath() + File.separator + "OLCI_MPHCHL.dim";
            ProductIO.writeProduct(mphChlProduct, targetProductPath, "BEAM-DIMAP");

            savedProduct = ProductIO.readProduct(targetProductPath);
            assertNotNull(savedProduct);

            final Band chlBand = savedProduct.getBand("chl");
            assertNotNull(chlBand);
            assertEquals(1.5443997383117676f, chlBand.getSampleFloat(0, 0), 1e-8);
            assertEquals(0.6783487796783447f, chlBand.getSampleFloat(1, 0), 1e-8);
            assertEquals(25.435853958129883f, chlBand.getSampleFloat(0, 1), 1e-8);
            assertEquals(Double.NaN, chlBand.getSampleFloat(1, 1), 1e-8);

            final Band flagBand = savedProduct.getBand("mph_chl_flags");
            assertNotNull(flagBand);
            assertEquals(0, flagBand.getSampleInt(0, 0));
            assertEquals(0, flagBand.getSampleInt(1, 0));
            assertEquals(1, flagBand.getSampleInt(0, 1));
            assertEquals(0, flagBand.getSampleInt(1, 1));

            final Band immersed_cyanobacteria = savedProduct.getBand("immersed_cyanobacteria");
            assertNotNull(immersed_cyanobacteria);
            assertEquals(0, immersed_cyanobacteria.getSampleInt(0, 0));
            assertEquals(0, immersed_cyanobacteria.getSampleInt(1, 0));
            assertEquals(1, immersed_cyanobacteria.getSampleInt(0, 1));
            assertEquals(0, immersed_cyanobacteria.getSampleInt(1, 1));

            final Band floating_cyanobacteria = savedProduct.getBand("floating_cyanobacteria");
            assertNotNull(floating_cyanobacteria);
            assertEquals(0, floating_cyanobacteria.getSampleInt(0, 0));
            assertEquals(0, floating_cyanobacteria.getSampleInt(1, 0));
            assertEquals(0, floating_cyanobacteria.getSampleInt(0, 1));
            assertEquals(0, floating_cyanobacteria.getSampleInt(1, 1));

            final Band floating_vegetation = savedProduct.getBand("floating_vegetation");
            assertNotNull(floating_vegetation);
            assertEquals(0, floating_vegetation.getSampleInt(0, 0));
            assertEquals(0, floating_vegetation.getSampleInt(1, 0));
            assertEquals(0, floating_vegetation.getSampleInt(0, 1));
            assertEquals(0, floating_vegetation.getSampleInt(1, 1));

            final Band mphBand = savedProduct.getBand("mph");
            assertNull(mphBand);

            final Band l1_flagband = savedProduct.getBand("l1_flags");
            assertNotNull(l1_flagband);
            assertEquals(2, l1_flagband.getSampleInt(0, 0));
            assertEquals(0, l1_flagband.getSampleInt(1, 0));
            assertEquals(0, l1_flagband.getSampleInt(0, 1));
            assertEquals(16, l1_flagband.getSampleInt(1, 1));
        } finally {
            if (savedProduct != null) {
                savedProduct.dispose();
            }
        }
    }

    @Test
    @Ignore
    public void testComputeMphChlProduct_withMph() throws IOException {
        final Product brrProduct = OlciBrrProduct.create();

        MphChlOp mphChlOp = new MphChlOp();
        mphChlOp.setSourceProduct(brrProduct);
        mphChlOp.setParameterDefaultValues();
        mphChlOp.setParameter("exportMph", true);
        final Product mphChlProduct = mphChlOp.getTargetProduct();

                Product savedProduct = null;
        try {
            final String targetProductPath = testOutDirectory.getAbsolutePath() + File.separator + "OLCI_MPHCHL.dim";
            ProductIO.writeProduct(mphChlProduct, targetProductPath, "BEAM-DIMAP");

            savedProduct = ProductIO.readProduct(targetProductPath);
            assertNotNull(savedProduct);

            final Band mphBand = savedProduct.getBand("mph");
            assertNotNull(mphBand);
            assertEquals(-1.1474395432742313E-4f, mphBand.getSampleFloat(0, 0), 1e-8);
            assertEquals(-4.521883383858949E-4f, mphBand.getSampleFloat(1, 0), 1e-8);
            assertEquals(0.003501386847347021f, mphBand.getSampleFloat(0, 1), 1e-8);
            assertEquals(Double.NaN, mphBand.getSampleFloat(1, 1), 1e-8);
        } finally {
            if (savedProduct != null) {
                savedProduct.dispose();
            }
        }
    }

    private Map<String, Object> createParameter() {
        final HashMap<String, Object> parameterMap = new HashMap<>();
        parameterMap.put("exportMph", Boolean.TRUE);
        return parameterMap;
    }

    @Test
    public void testWithFaultyValidPixelExpression() {
        final Product brrProduct = OlciBrrProduct.create();

        HashMap<String, Object> params = new HashMap<>();
        params.put("validPixelExpression", "extremely INVALID");

        try {
            GPF.createProduct("OlciMphChl", params, brrProduct);
            fail("OperatorException expected");
        } catch (OperatorException expected) {
        }
    }

}
