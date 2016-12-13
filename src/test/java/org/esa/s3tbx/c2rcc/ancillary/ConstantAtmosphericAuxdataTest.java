package org.esa.s3tbx.c2rcc.ancillary;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Marco Peters
 */
public class ConstantAtmosphericAuxdataTest {
    @Test
    public void getValues() throws Exception {
        ConstantAtmosphericAuxdata auxdata = new ConstantAtmosphericAuxdata(337, 980.7);

        assertEquals(337.0, auxdata.getOzone(23434.0, -1, -1, 43, 12.67), 1.0e-8);
        assertEquals(337.0, auxdata.getOzone(234.0, -1, -1, 43, 12.67), 1.0e-8);
        assertEquals(337.0, auxdata.getOzone(234.0, -1, -1, 143, 12.67), 1.0e-8);
        assertEquals(337.0, auxdata.getOzone(234.0, -1, -1, 143, -48), 1.0e-8);

        assertEquals(980.7, auxdata.getSurfacePressure(23434.0, -1, -1, 43, 12.67), 1.0e-8);
        assertEquals(980.7, auxdata.getSurfacePressure(234.0, -1, -1, 43, 12.67), 1.0e-8);
        assertEquals(980.7, auxdata.getSurfacePressure(234.0, -1, -1, 143, 12.67), 1.0e-8);
        assertEquals(980.7, auxdata.getSurfacePressure(234.0, -1, -1, 143, -48), 1.0e-8);

    }

}