package org.esa.s3tbx.c2rcc.util;

import static org.junit.Assert.*;

import org.junit.*;

/**
 * Created by Sabine on 24.08.2015.
 */
public class SolarFluxLazyLookupTest {

    private SolarFluxLazyLookup lookup;

    @Before
    public void setUp() throws Exception {
        lookup = new SolarFluxLazyLookup(new double[]{1, 2, 3, 4, 5, 6});
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testCreationWithDefaultFlux() {
        final SolarFluxLazyLookup lookup = new SolarFluxLazyLookup(new double[]{1, 2, 3, 4, 5, 6});

        assertArrayEquals(new double[]{1, 2, 3, 4, 5, 6}, lookup.solFlux, 1e-12);
    }

    @Test
    public void testRightValuesForNoneLeapYear() {
        assertArrayEquals(computeCorrectedFlux(0, 2005), lookup.getCorrectedFluxFor(0, 2005), 1e-12);
        assertArrayEquals(computeCorrectedFlux(23, 2005), lookup.getCorrectedFluxFor(23, 2005), 1e-12);
        assertArrayEquals(computeCorrectedFlux(50, 2005), lookup.getCorrectedFluxFor(50, 2005), 1e-12);
        assertArrayEquals(computeCorrectedFlux(77, 2005), lookup.getCorrectedFluxFor(77, 2005), 1e-12);
        assertArrayEquals(computeCorrectedFlux(100, 2005), lookup.getCorrectedFluxFor(100, 2005), 1e-12);
        assertArrayEquals(computeCorrectedFlux(119, 2005), lookup.getCorrectedFluxFor(119, 2005), 1e-12);
        assertArrayEquals(computeCorrectedFlux(150, 2005), lookup.getCorrectedFluxFor(150, 2005), 1e-12);
        assertArrayEquals(computeCorrectedFlux(176, 2005), lookup.getCorrectedFluxFor(176, 2005), 1e-12);
        assertArrayEquals(computeCorrectedFlux(200, 2005), lookup.getCorrectedFluxFor(200, 2005), 1e-12);
        assertArrayEquals(computeCorrectedFlux(250, 2005), lookup.getCorrectedFluxFor(250, 2005), 1e-12);
        assertArrayEquals(computeCorrectedFlux(300, 2005), lookup.getCorrectedFluxFor(300, 2005), 1e-12);
        assertArrayEquals(computeCorrectedFlux(325, 2005), lookup.getCorrectedFluxFor(325, 2005), 1e-12);
        assertArrayEquals(computeCorrectedFlux(365, 2005), lookup.getCorrectedFluxFor(365, 2005), 1e-12);
    }

    @Test
    public void testEqualValuesForDifferentNoneLeapYears() {
        assertArrayEquals(lookup.getCorrectedFluxFor(0, 2007), lookup.getCorrectedFluxFor(0, 2005), 1e-12);
        assertArrayEquals(lookup.getCorrectedFluxFor(23, 2007), lookup.getCorrectedFluxFor(23, 2005), 1e-12);
        assertArrayEquals(lookup.getCorrectedFluxFor(50, 2007), lookup.getCorrectedFluxFor(50, 2005), 1e-12);
        assertArrayEquals(lookup.getCorrectedFluxFor(77, 2007), lookup.getCorrectedFluxFor(77, 2005), 1e-12);
        assertArrayEquals(lookup.getCorrectedFluxFor(100, 2006), lookup.getCorrectedFluxFor(100, 2005), 1e-12);
        assertArrayEquals(lookup.getCorrectedFluxFor(119, 2006), lookup.getCorrectedFluxFor(119, 2005), 1e-12);
        assertArrayEquals(lookup.getCorrectedFluxFor(150, 2006), lookup.getCorrectedFluxFor(150, 2005), 1e-12);
        assertArrayEquals(lookup.getCorrectedFluxFor(176, 2006), lookup.getCorrectedFluxFor(176, 2005), 1e-12);
        assertArrayEquals(lookup.getCorrectedFluxFor(200, 2006), lookup.getCorrectedFluxFor(200, 2005), 1e-12);
        assertArrayEquals(lookup.getCorrectedFluxFor(250, 2006), lookup.getCorrectedFluxFor(250, 2005), 1e-12);
        assertArrayEquals(lookup.getCorrectedFluxFor(300, 2006), lookup.getCorrectedFluxFor(300, 2005), 1e-12);
        assertArrayEquals(lookup.getCorrectedFluxFor(325, 2006), lookup.getCorrectedFluxFor(325, 2005), 1e-12);
        assertArrayEquals(lookup.getCorrectedFluxFor(365, 2006), lookup.getCorrectedFluxFor(365, 2005), 1e-12);
    }

    @Test
    public void testSameArrayInstanceForDifferentNoneLeapYears() {
        assertSame(lookup.getCorrectedFluxFor(0, 2010), lookup.getCorrectedFluxFor(0, 2009));
        assertSame(lookup.getCorrectedFluxFor(23, 2010), lookup.getCorrectedFluxFor(23, 2009));
        assertSame(lookup.getCorrectedFluxFor(50, 2010), lookup.getCorrectedFluxFor(50, 2009));
        assertSame(lookup.getCorrectedFluxFor(77, 2010), lookup.getCorrectedFluxFor(77, 2009));
        assertSame(lookup.getCorrectedFluxFor(100, 2001), lookup.getCorrectedFluxFor(100, 2009));
        assertSame(lookup.getCorrectedFluxFor(119, 2001), lookup.getCorrectedFluxFor(119, 2009));
        assertSame(lookup.getCorrectedFluxFor(150, 2001), lookup.getCorrectedFluxFor(150, 2009));
        assertSame(lookup.getCorrectedFluxFor(176, 2001), lookup.getCorrectedFluxFor(176, 2009));
        assertSame(lookup.getCorrectedFluxFor(200, 2001), lookup.getCorrectedFluxFor(200, 2009));
        assertSame(lookup.getCorrectedFluxFor(250, 2001), lookup.getCorrectedFluxFor(250, 2009));
        assertSame(lookup.getCorrectedFluxFor(300, 2001), lookup.getCorrectedFluxFor(300, 2009));
        assertSame(lookup.getCorrectedFluxFor(325, 2001), lookup.getCorrectedFluxFor(325, 2009));
        assertSame(lookup.getCorrectedFluxFor(365, 2001), lookup.getCorrectedFluxFor(365, 2009));
    }

    @Test
    public void testRightValuesForLeapYear() {
        assertArrayEquals(computeCorrectedFlux(0, 2004), lookup.getCorrectedFluxFor(0, 2004), 1e-12);
        assertArrayEquals(computeCorrectedFlux(23, 2004), lookup.getCorrectedFluxFor(23, 2004), 1e-12);
        assertArrayEquals(computeCorrectedFlux(50, 2004), lookup.getCorrectedFluxFor(50, 2004), 1e-12);
        assertArrayEquals(computeCorrectedFlux(77, 2004), lookup.getCorrectedFluxFor(77, 2004), 1e-12);
        assertArrayEquals(computeCorrectedFlux(100, 2004), lookup.getCorrectedFluxFor(100, 2004), 1e-12);
        assertArrayEquals(computeCorrectedFlux(119, 2004), lookup.getCorrectedFluxFor(119, 2004), 1e-12);
        assertArrayEquals(computeCorrectedFlux(150, 2004), lookup.getCorrectedFluxFor(150, 2004), 1e-12);
        assertArrayEquals(computeCorrectedFlux(176, 2004), lookup.getCorrectedFluxFor(176, 2004), 1e-12);
        assertArrayEquals(computeCorrectedFlux(200, 2004), lookup.getCorrectedFluxFor(200, 2004), 1e-12);
        assertArrayEquals(computeCorrectedFlux(250, 2004), lookup.getCorrectedFluxFor(250, 2004), 1e-12);
        assertArrayEquals(computeCorrectedFlux(300, 2004), lookup.getCorrectedFluxFor(300, 2004), 1e-12);
        assertArrayEquals(computeCorrectedFlux(325, 2004), lookup.getCorrectedFluxFor(325, 2004), 1e-12);
        assertArrayEquals(computeCorrectedFlux(365, 2004), lookup.getCorrectedFluxFor(365, 2004), 1e-12);
    }

    @Test
    public void testEqualValuesForDifferentLeapYears() {
        assertArrayEquals(lookup.getCorrectedFluxFor(0, 2008), lookup.getCorrectedFluxFor(0, 2004), 1e-12);
        assertArrayEquals(lookup.getCorrectedFluxFor(23, 2008), lookup.getCorrectedFluxFor(23, 2004), 1e-12);
        assertArrayEquals(lookup.getCorrectedFluxFor(50, 2008), lookup.getCorrectedFluxFor(50, 2004), 1e-12);
        assertArrayEquals(lookup.getCorrectedFluxFor(77, 2008), lookup.getCorrectedFluxFor(77, 2004), 1e-12);
        assertArrayEquals(lookup.getCorrectedFluxFor(100, 2012), lookup.getCorrectedFluxFor(100, 2004), 1e-12);
        assertArrayEquals(lookup.getCorrectedFluxFor(119, 2012), lookup.getCorrectedFluxFor(119, 2004), 1e-12);
        assertArrayEquals(lookup.getCorrectedFluxFor(150, 2012), lookup.getCorrectedFluxFor(150, 2004), 1e-12);
        assertArrayEquals(lookup.getCorrectedFluxFor(176, 2012), lookup.getCorrectedFluxFor(176, 2004), 1e-12);
        assertArrayEquals(lookup.getCorrectedFluxFor(200, 2012), lookup.getCorrectedFluxFor(200, 2004), 1e-12);
        assertArrayEquals(lookup.getCorrectedFluxFor(250, 2012), lookup.getCorrectedFluxFor(250, 2004), 1e-12);
        assertArrayEquals(lookup.getCorrectedFluxFor(300, 2012), lookup.getCorrectedFluxFor(300, 2004), 1e-12);
        assertArrayEquals(lookup.getCorrectedFluxFor(325, 2012), lookup.getCorrectedFluxFor(325, 2004), 1e-12);
        assertArrayEquals(lookup.getCorrectedFluxFor(365, 2012), lookup.getCorrectedFluxFor(365, 2004), 1e-12);
    }

    @Test
    public void testSameArrayInstanceForDifferentLeapYears() {
        assertSame(lookup.getCorrectedFluxFor(0, 2008), lookup.getCorrectedFluxFor(0, 2004));
        assertSame(lookup.getCorrectedFluxFor(23, 2008), lookup.getCorrectedFluxFor(23, 2004));
        assertSame(lookup.getCorrectedFluxFor(50, 2008), lookup.getCorrectedFluxFor(50, 2004));
        assertSame(lookup.getCorrectedFluxFor(77, 2008), lookup.getCorrectedFluxFor(77, 2004));
        assertSame(lookup.getCorrectedFluxFor(100, 2012), lookup.getCorrectedFluxFor(100, 2004));
        assertSame(lookup.getCorrectedFluxFor(119, 2012), lookup.getCorrectedFluxFor(119, 2004));
        assertSame(lookup.getCorrectedFluxFor(150, 2012), lookup.getCorrectedFluxFor(150, 2004));
        assertSame(lookup.getCorrectedFluxFor(176, 2012), lookup.getCorrectedFluxFor(176, 2004));
        assertSame(lookup.getCorrectedFluxFor(200, 2012), lookup.getCorrectedFluxFor(200, 2004));
        assertSame(lookup.getCorrectedFluxFor(250, 2012), lookup.getCorrectedFluxFor(250, 2004));
        assertSame(lookup.getCorrectedFluxFor(300, 2012), lookup.getCorrectedFluxFor(300, 2004));
        assertSame(lookup.getCorrectedFluxFor(325, 2012), lookup.getCorrectedFluxFor(325, 2004));
        assertSame(lookup.getCorrectedFluxFor(365, 2012), lookup.getCorrectedFluxFor(365, 2004));
    }

    private double[] computeCorrectedFlux(int doy, int year) {
        final double correctionFactor = SolarFluxCorrectionFactorCalculator.getDayCorrectionFactorFor(doy, year);
        final double[] solFlux = lookup.solFlux;
        final double[] corrected = new double[solFlux.length];
        for (int i = 0; i < solFlux.length; i++) {
            double v = solFlux[i];
            corrected[i] = v*correctionFactor;
        }
        return corrected;
    }
}