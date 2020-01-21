package org.esa.s3tbx.dataio.s3.olci;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class OlciProductFactoryTest {

    @Test
    public void testUnitExtrationFromLogScaledUnit() {
        String logUnit = "lg(re g.m-3)";
        Pattern pattern = Pattern.compile("lg\\s*\\(\\s*re:?\\s*(.*)\\)");
        final Matcher m = pattern.matcher(logUnit);
        assertTrue(m.matches());

        assertEquals(logUnit, m.group(0));
        assertEquals("g.m-3", m.group(1));
    }

    @Test
    public void testGetResolutionInKm() {
        assertEquals(0.3, OlciProductFactory.getResolutionInKm("OL_1_EFR"), 1e-8);
        assertEquals(1.2, OlciProductFactory.getResolutionInKm("OL_1_ERR"), 1e-8);
    }

}
