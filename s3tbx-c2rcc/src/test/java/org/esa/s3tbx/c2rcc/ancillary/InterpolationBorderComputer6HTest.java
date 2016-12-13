package org.esa.s3tbx.c2rcc.ancillary;

import org.esa.snap.core.datamodel.ProductData;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.*;

/**
 * Created by Sabine on 07.09.2015.
 */
public class InterpolationBorderComputer6HTest {

    private InterpolationBorderComputer6H computer;
    private ProductData.UTC utc;

    @Before
    public void setUp() throws Exception {
        computer = new InterpolationBorderComputer6H();
    }


    @Test
    public void testFirstJanuar2000() throws Exception {
        computer.setInterpolationTimeMJD(ProductData.UTC.parse("01-JAN-2000 12:00:00").getMJD());

        assertEquals(0.375, computer.getStartBorderTimeMDJ());
        assertEquals(0.625, computer.getEndBorderTimeMJD());
        assertEquals("N200000106", computer.getStartAncFilePrefix());
        assertEquals("N200000112", computer.getEndAncFilePrefix());
    }

    @Test
    public void testTimeNearUpperBorder() throws Exception {
        computer.setInterpolationTimeMJD(ProductData.UTC.parse("01-JAN-2000 14:59:00").getMJD());

        assertEquals(0.375, computer.getStartBorderTimeMDJ());
        assertEquals(0.625, computer.getEndBorderTimeMJD());
        assertEquals("N200000106", computer.getStartAncFilePrefix());
        assertEquals("N200000112", computer.getEndAncFilePrefix());
    }

    @Test
    public void testTimeNearLowerBorder() throws Exception {
        computer.setInterpolationTimeMJD(ProductData.UTC.parse("01-JAN-2000 15:00:01").getMJD());

        assertEquals(0.625, computer.getStartBorderTimeMDJ());
        assertEquals(0.875, computer.getEndBorderTimeMJD());
        assertEquals("N200000112", computer.getStartAncFilePrefix());
        assertEquals("N200000118", computer.getEndAncFilePrefix());
    }
}