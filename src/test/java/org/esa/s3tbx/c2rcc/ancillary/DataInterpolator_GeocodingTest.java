package org.esa.s3tbx.c2rcc.ancillary;

import static junit.framework.Assert.*;

import org.esa.snap.framework.datamodel.GeoPos;
import org.esa.snap.framework.datamodel.PixelPos;
import org.junit.*;

/**
 * Created by Sabine on 03.09.2015.
 */
public class DataInterpolator_GeocodingTest {

    private GeoPos gPos;
    private PixelPos pPos;

    @Before
    public void setUp() {
        gPos = new GeoPos();
        pPos = new PixelPos();
    }

    @After
    public void tearDown() {
        gPos.setInvalid();
        pPos.setInvalid();
    }

    @Test
    public void testGetPixelPos_aroundTheCenter_140x70_Product_0_0() {
        final DataInterpolator.GlobalGeoCoding gc = new DataInterpolator.GlobalGeoCoding(140, 70);

        gPos.setLocation(0.0001,-0.0001);
        assertSame(pPos, gc.getPixelPos(gPos, pPos));
        assertEquals(69.5, pPos.getX());
        assertEquals(34.5, pPos.getY());

        gPos.setLocation(0.0001,0.0001);
        assertSame(pPos, gc.getPixelPos(gPos, pPos));
        assertEquals(70.5, pPos.getX());
        assertEquals(34.5, pPos.getY());

        gPos.setLocation(-0.0001,0.0001);
        assertSame(pPos, gc.getPixelPos(gPos, pPos));
        assertEquals(70.5, pPos.getX());
        assertEquals(35.5, pPos.getY());

        gPos.setLocation(-0.0001,-0.0001);
        assertSame(pPos, gc.getPixelPos(gPos, pPos));
        assertEquals(69.5, pPos.getX());
        assertEquals(35.5, pPos.getY());
    }

    @Test
    public void testGetPixelPos_alongTheBorder_140x70_Product_0_0() {
        final DataInterpolator.GlobalGeoCoding gc = new DataInterpolator.GlobalGeoCoding(140, 70);

        gPos.setLocation(90,-180);
        assertSame(pPos, gc.getPixelPos(gPos, pPos));
        assertEquals(0.5, pPos.getX());
        assertEquals(0.5, pPos.getY());

        gPos.setLocation(90,180);
        assertSame(pPos, gc.getPixelPos(gPos, pPos));
        assertEquals(139.5, pPos.getX());
        assertEquals(0.5, pPos.getY());

        gPos.setLocation(-90,180);
        assertSame(pPos, gc.getPixelPos(gPos, pPos));
        assertEquals(139.5, pPos.getX());
        assertEquals(69.5, pPos.getY());

        gPos.setLocation(-90,-180);
        assertSame(pPos, gc.getPixelPos(gPos, pPos));
        assertEquals(0.5, pPos.getX());
        assertEquals(69.5, pPos.getY());
    }

    @Test
    public void testGetPixelPos_aroundTheCenter_144x73_Product_0_0() {
        final DataInterpolator.GlobalGeoCoding gc = new DataInterpolator.GlobalGeoCoding(144, 73);

        gPos.setLocation(0.0001,-0.0001);
        assertSame(pPos, gc.getPixelPos(gPos, pPos));
        assertEquals(71.5, pPos.getX());
        assertEquals(36.5, pPos.getY());   // still the same because odd height

        gPos.setLocation(0.0001,0.0001);
        assertSame(pPos, gc.getPixelPos(gPos, pPos));
        assertEquals(72.5, pPos.getX());
        assertEquals(36.5, pPos.getY());   // still the same because odd height

        gPos.setLocation(-0.0001,0.0001);
        assertSame(pPos, gc.getPixelPos(gPos, pPos));
        assertEquals(72.5, pPos.getX());
        assertEquals(36.5, pPos.getY());   // still the same because odd height

        gPos.setLocation(-0.0001,-0.0001);
        assertSame(pPos, gc.getPixelPos(gPos, pPos));
        assertEquals(71.5, pPos.getX());
        assertEquals(36.5, pPos.getY());  // still the same because odd height
    }

    @Test
    public void testGetPixelPos_alongTheBorder_144x73_Product_0_0() {
        final DataInterpolator.GlobalGeoCoding gc = new DataInterpolator.GlobalGeoCoding(144, 73);

        gPos.setLocation(90,-180);
        assertSame(pPos, gc.getPixelPos(gPos, pPos));
        assertEquals(0.5, pPos.getX());
        assertEquals(0.5, pPos.getY());

        gPos.setLocation(90,180);
        assertSame(pPos, gc.getPixelPos(gPos, pPos));
        assertEquals(143.5, pPos.getX());
        assertEquals(0.5, pPos.getY());

        gPos.setLocation(-90,180);
        assertSame(pPos, gc.getPixelPos(gPos, pPos));
        assertEquals(143.5, pPos.getX());
        assertEquals(72.5, pPos.getY());

        gPos.setLocation(-90,-180);
        assertSame(pPos, gc.getPixelPos(gPos, pPos));
        assertEquals(0.5, pPos.getX());
        assertEquals(72.5, pPos.getY());
    }

    @Test
    public void testGetPixelPos_aroundTheCenter_360x180_Product_0_0() {
        final DataInterpolator.GlobalGeoCoding gc = new DataInterpolator.GlobalGeoCoding(360, 180);

        gPos.setLocation(0.0001,-0.0001);
        assertSame(pPos, gc.getPixelPos(gPos, pPos));
        assertEquals(179.5, pPos.getX());
        assertEquals(89.5, pPos.getY());

        gPos.setLocation(0.0001,0.0001);
        assertSame(pPos, gc.getPixelPos(gPos, pPos));
        assertEquals(180.5, pPos.getX());
        assertEquals(89.5, pPos.getY());

        gPos.setLocation(-0.0001,0.0001);
        assertSame(pPos, gc.getPixelPos(gPos, pPos));
        assertEquals(180.5, pPos.getX());
        assertEquals(90.5, pPos.getY());

        gPos.setLocation(-0.0001,-0.0001);
        assertSame(pPos, gc.getPixelPos(gPos, pPos));
        assertEquals(179.5, pPos.getX());
        assertEquals(90.5, pPos.getY());
    }

    @Test
    public void testGetPixelPos_alongTheBorder_360x181_Product_0_0() {
        final DataInterpolator.GlobalGeoCoding gc = new DataInterpolator.GlobalGeoCoding(360, 181);

        gPos.setLocation(90,-180);
        assertSame(pPos, gc.getPixelPos(gPos, pPos));
        assertEquals(0.5, pPos.getX());
        assertEquals(0.5, pPos.getY());

        gPos.setLocation(90,180);
        assertSame(pPos, gc.getPixelPos(gPos, pPos));
        assertEquals(359.5, pPos.getX());
        assertEquals(0.5, pPos.getY());

        gPos.setLocation(-90,180);
        assertSame(pPos, gc.getPixelPos(gPos, pPos));
        assertEquals(359.5, pPos.getX());
        assertEquals(180.5, pPos.getY());

        gPos.setLocation(-90,-180);
        assertSame(pPos, gc.getPixelPos(gPos, pPos));
        assertEquals(0.5, pPos.getX());
        assertEquals(180.5, pPos.getY());
    }
}