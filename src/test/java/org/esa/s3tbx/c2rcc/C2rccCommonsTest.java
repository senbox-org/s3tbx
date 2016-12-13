package org.esa.s3tbx.c2rcc;

import org.esa.snap.core.datamodel.RGBImageProfileManager;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Marco Peters
 */
public class C2rccCommonsTest {
    @Test
    public void installRGBProfiles() throws Exception {
        RGBImageProfileManager profileManager = RGBImageProfileManager.getInstance();
        assertEquals(0, profileManager.getProfileCount());
        C2rccCommons.installRGBProfiles();
        assertEquals(9, profileManager.getProfileCount());
    }

}