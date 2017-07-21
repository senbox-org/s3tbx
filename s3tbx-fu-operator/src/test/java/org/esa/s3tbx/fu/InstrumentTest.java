package org.esa.s3tbx.fu;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.Test;

import static org.junit.Assert.*;

public class InstrumentTest {

    @Test
    public void testDefaultReflectancesSelector_MERIS() throws Exception {
        Product product = new Product("dummy", "dummy", 10, 10);
        addBand(product, "a", 412);
        addBand(product, "a1", 422);
        addBand(product, "a2", 430);
        addBand(product, "b", 443);
        addBand(product, "c", 490);
        addBand(product, "d", 510);
        addBand(product, "e", 560);
        addBand(product, "e1", 570);
        addBand(product, "f", 620);
        addBand(product, "g", 665);
        addBand(product, "h", 680);
        addBand(product, "i", 708);
        addBand(product, "j0", 799);

        Instrument.DefaultReflectancesSelector selector = new Instrument.DefaultReflectancesSelector(
                new double[]{412.691, 442.55902, 489.88202, 509.81903, 559.69403, 619.601, 664.57306, 680.82104, 708.32904}, new String[0]);
        String[] reflecBands = selector.select(product, null);
        final String[] expecteds = {"a", "b", "c", "d", "e", "f", "g", "h", "i"};
        assertArrayEquals(expecteds, reflecBands);

    }

    private static void addBand(Product product, String bandName, float wavelength) {
        Band band = new Band(bandName, ProductData.TYPE_FLOAT64, 10, 10);
        band.setSpectralWavelength(wavelength);
        product.addBand(band);
    }


}