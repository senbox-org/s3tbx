package org.esa.s3tbx.mphchl;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.util.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class MphChlOlciBetaOpAcceptanceTest {
    private File testOutDirectory;

    @Before
    public void setUp() {
        testOutDirectory = new File("output");
        if (!testOutDirectory.mkdirs()) {
            fail("unable to create test directory: " + testOutDirectory);
        }
    }

    @After
    public void tearDown() {
        if (testOutDirectory != null) {
            if (!FileUtils.deleteTree(testOutDirectory)) {
                fail("Unable to delete test directory: " + testOutDirectory);
            }
        }
    }

    @Test
    public void testComputeMphChlProduct() throws IOException {
        final Product brrProduct = OlciBrrProduct.create();

        MphChlOlciBetaOp mphChlOp = new MphChlOlciBetaOp();
        mphChlOp.setSourceProduct(brrProduct);
        mphChlOp.setParameterDefaultValues();
        mphChlOp.setParameter("applyLowPassFilter", false);
        mphChlOp.setParameter("validPixelExpression", Sensor.OLCI.getValidPixelExpression());
        final Product mphChlProduct = mphChlOp.getTargetProduct();

        Product savedProduct = null;
        try {
            final String targetProductPath = testOutDirectory.getAbsolutePath() + File.separator + "OLCI_MPHCHL.dim";
            ProductIO.writeProduct(mphChlProduct, targetProductPath, "BEAM-DIMAP");

            savedProduct = ProductIO.readProduct(targetProductPath);
            assertNotNull(savedProduct);

            final Band chlBand = savedProduct.getBand("chl");
            assertNotNull(chlBand);
            assertEquals(5.063526153564453f, chlBand.getSampleFloat(0, 0), 1e-8);
            assertEquals(4.94189977645874f, chlBand.getSampleFloat(1, 0), 1e-8);
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

            final Band l1_flagband = savedProduct.getBand("quality_flags");
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
    public void testComputeMphChlProduct_withAddBands() throws IOException {
        final Product brrProduct = OlciBrrProduct.create();

        MphChlOlciBetaOp mphChlOp = new MphChlOlciBetaOp();
        mphChlOp.setSourceProduct(brrProduct);
        mphChlOp.setParameterDefaultValues();
        mphChlOp.setParameter("applyLowPassFilter", false);
        mphChlOp.setParameter("exportAddBands", true);
        mphChlOp.setParameter("exportMph", true);
        mphChlOp.setParameter("validPixelExpression", Sensor.OLCI.getValidPixelExpression());
        final Product mphChlProduct = mphChlOp.getTargetProduct();

        Product savedProduct = null;
        try {
            final String targetProductPath = testOutDirectory.getAbsolutePath() + File.separator + "OLCI_MPHCHL.dim";
            ProductIO.writeProduct(mphChlProduct, targetProductPath, "BEAM-DIMAP");

            savedProduct = ProductIO.readProduct(targetProductPath);
            assertNotNull(savedProduct);

            final Band chlBand = savedProduct.getBand("chl");
            assertNotNull(chlBand);
            assertEquals(5.063526153564453f, chlBand.getSampleFloat(0, 0), 1e-8);
            assertEquals(4.94189977645874f, chlBand.getSampleFloat(1, 0), 1e-8);
            assertEquals(25.435853958129883f, chlBand.getSampleFloat(0, 1), 1e-8);
            assertEquals(Double.NaN, chlBand.getSampleFloat(1, 1), 1e-8);

            final Band chlMatthewsBand = savedProduct.getBand("chl_matthews");
            assertNotNull(chlMatthewsBand);
            // old MERIS:
            assertEquals(1.5443997383117676f, chlMatthewsBand.getSampleFloat(0, 0), 1e-8);
            assertEquals(0.6783487796783447f, chlMatthewsBand.getSampleFloat(1, 0), 1e-8);
            assertEquals(25.435853958129883f, chlMatthewsBand.getSampleFloat(0, 1), 1e-8);

            final Band chlPitarchBand = savedProduct.getBand("chl_pitarch");
            assertNotNull(chlPitarchBand);
            // old MERIS:
            assertEquals(-0.6338432431221008f, chlPitarchBand.getSampleFloat(0, 0), 1e-8);
            assertEquals(-2.5089476108551025f, chlPitarchBand.getSampleFloat(1, 0), 1e-8);
            assertEquals(18.46561050415039f, chlPitarchBand.getSampleFloat(0, 1), 1e-8);

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
            assertNotNull(mphBand);

            final Band l1_flagband = savedProduct.getBand("quality_flags");
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
    public void testComputeMphChlProduct_withMph() throws IOException {
        final Product brrProduct = OlciBrrProduct.create();

        MphChlOlciBetaOp mphChlOp = new MphChlOlciBetaOp();
        mphChlOp.setSourceProduct(brrProduct);
        mphChlOp.setParameterDefaultValues();
        mphChlOp.setParameter("exportMph", true);
        mphChlOp.setParameter("validPixelExpression", Sensor.OLCI.getValidPixelExpression());
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
        } finally {
            if (savedProduct != null) {
                savedProduct.dispose();
            }
        }
    }

    @Test
    public void testWithFaultyValidPixelExpression() {
        final Product brrProduct = OlciBrrProduct.create();

        HashMap<String, Object> params = new HashMap<>();
        params.put("validPixelExpression", "extremely INVALID");

        try {
            GPF.createProduct("MphChlOlci-beta", params, brrProduct);
            fail("OperatorException expected");
        } catch (OperatorException ignored) {
        }
    }

}
