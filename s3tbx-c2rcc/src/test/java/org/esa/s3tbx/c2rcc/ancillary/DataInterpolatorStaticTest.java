package org.esa.s3tbx.c2rcc.ancillary;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.*;

/**
 * Created by Sabine on 03.09.2015.
 */
public class DataInterpolatorStaticTest {

    private final double endTimeMJD = 25.5;
    private DataInterpolator di;
    private Product p1;
    private Product p2;
    private double startTimeMJD;

    @Before
    public void setUp() throws Exception {
        p1 = new Product("p1", "t1", 2, 2);
        p1.addBand("ozone", ProductData.TYPE_FLOAT64).setDataElems(new double[]{3, 4, 5, 6});
        p2 = new Product("p2", "t1", 2, 2);
        p2.addBand("ozone", ProductData.TYPE_FLOAT64).setDataElems(new double[]{8, 11.2, 2.7, 12.25});
        startTimeMJD = 24.5;
        di = new DataInterpolatorStatic(startTimeMJD, endTimeMJD, p1, p2, "ozone", 123);
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testInitializing() {
        assertNull(p1.getSceneGeoCoding());
        assertNull(p2.getSceneGeoCoding());
    }

    @Test
    public void testGetStartTimeValue() throws Exception {
        assertEquals(3.0, di.getValue(startTimeMJD, 45, -90));
        assertEquals(4.0, di.getValue(startTimeMJD, 45, 90));
        assertEquals(5.0, di.getValue(startTimeMJD, -45, -90));
        assertEquals(6.0, di.getValue(startTimeMJD, -45, 90));
    }

    @Test
    public void testGetEndTimeValue() throws Exception {
        assertEquals(8.0, di.getValue(endTimeMJD, 45, -90));
        assertEquals(11.2, di.getValue(endTimeMJD, 45, 90));
        assertEquals(2.7, di.getValue(endTimeMJD, -45, -90));
        assertEquals(12.25, di.getValue(endTimeMJD, -45, 90));
    }

    @Test
    public void testGetCenterTimeValue() throws Exception {
        final double centerTimeMJD = (endTimeMJD - startTimeMJD) / 2 + startTimeMJD;
        assertEquals(5.5, di.getValue(centerTimeMJD, 45, -90));
        assertEquals(7.6, di.getValue(centerTimeMJD, 45, 90));
        assertEquals(3.85, di.getValue(centerTimeMJD, -45, -90));
        assertEquals(9.125, di.getValue(centerTimeMJD, -45, 90));
    }
}