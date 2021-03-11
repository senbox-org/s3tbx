package org.esa.s3tbx.dataio.s3.synergy;

import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.transform.MathTransform2D;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SynL1CSceneTransformGeoCodingTest {

    private GeoCoding wrappedCoding;
    private SynL1CSceneTransformGeoCoding coding;

    @Before
    public void setUp() {
        wrappedCoding = mock(GeoCoding.class);
        when(wrappedCoding.getMapCRS()).thenReturn(DefaultGeographicCRS.WGS84);
        when(wrappedCoding.getGeoCRS()).thenReturn(DefaultGeographicCRS.WGS84);

        final MathTransform2D transform = mock(MathTransform2D.class);
        coding = new SynL1CSceneTransformGeoCoding(wrappedCoding, transform, transform);
    }

    @Test
    public void testCanClone() {
        assertTrue(coding.canClone());
    }

    @Test
    public void testClone_wrappedCanClone() {
        final GeoCoding clonedGC = mock(GeoCoding.class);
        when(clonedGC.getMapCRS()).thenReturn(DefaultGeographicCRS.WGS84);
        when(clonedGC.getGeoCRS()).thenReturn(DefaultGeographicCRS.WGS84);

        when(wrappedCoding.canClone()).thenReturn(true);
        when(wrappedCoding.clone()).thenReturn(clonedGC);

        final GeoCoding clone = coding.clone();
        assertNotNull(clone);

        verify(wrappedCoding, times(1)).canClone();
        verify(wrappedCoding, times(1)).clone();
    }

    @Test
    public void testClone_wrappedCanNotClone() {
        final GeoCoding clonedGC = mock(GeoCoding.class);
        when(clonedGC.getMapCRS()).thenReturn(DefaultGeographicCRS.WGS84);
        when(clonedGC.getGeoCRS()).thenReturn(DefaultGeographicCRS.WGS84);

        when(wrappedCoding.canClone()).thenReturn(false);

        final GeoCoding clone = coding.clone();
        assertNotNull(clone);

        verify(wrappedCoding, times(1)).canClone();
        verify(wrappedCoding, times(0)).clone();
    }

    @Test
    public void testIsCrossingMeridianAt180() {
        when(wrappedCoding.isCrossingMeridianAt180()).thenReturn(true);
        assertTrue(coding.isCrossingMeridianAt180());

        when(wrappedCoding.isCrossingMeridianAt180()).thenReturn(false);
        assertFalse(coding.isCrossingMeridianAt180());
    }
}
