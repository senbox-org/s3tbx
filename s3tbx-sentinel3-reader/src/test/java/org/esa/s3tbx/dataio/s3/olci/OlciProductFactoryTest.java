package org.esa.s3tbx.dataio.s3.olci;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.esa.s3tbx.dataio.s3.olci.OlciProductFactory.SYSPROP_OLCI_TIE_POINT_CODING_FORWARD;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
        assertEquals(0.3, OlciProductFactory.getResolutionInKm("OL_2_LFR"), 1e-8);
        assertEquals(0.3, OlciProductFactory.getResolutionInKm("OL_2_WFR"), 1e-8);
        assertEquals(1.2, OlciProductFactory.getResolutionInKm("OL_1_ERR"), 1e-8);
        assertEquals(1.2, OlciProductFactory.getResolutionInKm("OL_2_LRR"), 1e-8);
        assertEquals(1.2, OlciProductFactory.getResolutionInKm("OL_2_WRR"), 1e-8);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    public void testGetResolutionInKm_invalid() {
        try {
            OlciProductFactory.getResolutionInKm("heffalump");
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testGetForwardAndInverseKeys_tiePointCoding_forwardKey() {
        final String forwardKey = System.getProperty(SYSPROP_OLCI_TIE_POINT_CODING_FORWARD);
        try {
            System.setProperty(SYSPROP_OLCI_TIE_POINT_CODING_FORWARD, "YEAH!");

            final String[] codingKeys = OlciProductFactory.getForwardAndInverseKeys_tiePointCoding();
            assertEquals("YEAH!", codingKeys[0]);
            assertEquals("INV_TIE_POINT", codingKeys[1]);

        } finally {
            if (forwardKey != null) {
                System.setProperty(SYSPROP_OLCI_TIE_POINT_CODING_FORWARD, forwardKey);
            } else {
                System.clearProperty(SYSPROP_OLCI_TIE_POINT_CODING_FORWARD);
            }
        }
    }

    @Test
    public void testGetForwardAndInverseKeys_tiePointCoding_default() {
        final String forwardKey = System.getProperty(SYSPROP_OLCI_TIE_POINT_CODING_FORWARD);
        try {
            System.clearProperty(SYSPROP_OLCI_TIE_POINT_CODING_FORWARD);

            final String[] codingKeys = OlciProductFactory.getForwardAndInverseKeys_tiePointCoding();
            assertEquals("FWD_TIE_POINT_BILINEAR", codingKeys[0]);
            assertEquals("INV_TIE_POINT", codingKeys[1]);

        } finally {
            if (forwardKey != null) {
                System.setProperty(SYSPROP_OLCI_TIE_POINT_CODING_FORWARD, forwardKey);
            }
        }
    }
}
