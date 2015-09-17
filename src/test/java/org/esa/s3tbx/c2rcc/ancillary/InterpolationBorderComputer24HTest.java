package org.esa.s3tbx.c2rcc.ancillary;

import static junit.framework.Assert.*;

import org.esa.snap.framework.datamodel.ProductData;
import org.junit.*;

/**
 * Created by Sabine on 07.09.2015.
 */
public class InterpolationBorderComputer24HTest {

    private InterpolationBorderComputer24H computer;
    private ProductData.UTC utc;

    @Before
    public void setUp() throws Exception {
        computer = new InterpolationBorderComputer24H();
        utc = ProductData.UTC.parse("03-AUG-2008 13:30:00");
    }

    @Test
    public void testUTCmjd() throws Exception {
        assertEquals(3137, 5625, utc.getMJD());
    }

    @Test
    public void testFirstJanuar2000_before_twelve() throws Exception {
        computer.setInterpolationTimeMJD(ProductData.UTC.parse("01-JAN-2000 11:59:59").getMJD());

        assertEquals(-0.5, computer.getStartBorderTimeMDJ());
        assertEquals(0.5, computer.getEndBorderTimeMJD());
        assertEquals("N199936500", computer.getStartAncFilePrefix());
        assertEquals("N200000100", computer.getEndAncFilePrefix());
    }

    @Test
    public void testFirstJanuar2000_twelve() throws Exception {
        computer.setInterpolationTimeMJD(ProductData.UTC.parse("01-JAN-2000 12:00:00").getMJD());

        assertEquals(0.5, computer.getStartBorderTimeMDJ());
        assertEquals(1.5, computer.getEndBorderTimeMJD());
        assertEquals("N200000100", computer.getStartAncFilePrefix());
        assertEquals("N200000200", computer.getEndAncFilePrefix());
    }

    @Test
    public void testFirstJanuar2000_after_twelve() throws Exception {
        computer.setInterpolationTimeMJD(ProductData.UTC.parse("01-JAN-2000 12:00:01").getMJD());

        assertEquals(0.5, computer.getStartBorderTimeMDJ());
        assertEquals(1.5, computer.getEndBorderTimeMJD());
        assertEquals("N200000100", computer.getStartAncFilePrefix());
        assertEquals("N200000200", computer.getEndAncFilePrefix());
    }

    @Test
    public void testBorderStartAndEndTime() throws Exception {
        computer.setInterpolationTimeMJD(utc.getMJD());

        assertEquals("03-AUG-2008 13:30:00.000000", utc.format());
        assertEquals(3137.5, computer.getStartBorderTimeMDJ());
        assertEquals(3138.5, computer.getEndBorderTimeMJD());
    }

    @Test
    public void testStartAndEndFilePräfix() throws Exception {
        computer.setInterpolationTimeMJD(utc.getMJD());

        assertEquals("03-AUG-2008 13:30:00.000000", utc.format());
        assertEquals("N200821600", computer.getStartAncFilePrefix());
        assertEquals("N200821700", computer.getEndAncFilePrefix());
    }
}