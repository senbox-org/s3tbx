package org.esa.s3tbx.c2rcc;

import org.esa.snap.core.datamodel.RGBImageProfile;
import org.esa.snap.core.datamodel.RGBImageProfileManager;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Marco Peters
 */
public class C2rccCommonsTest {
    private static RGBImageProfileManager profileManager = RGBImageProfileManager.getInstance();
    private static RGBImageProfile[] storedProfiles;

    @BeforeClass
    public static void setUp() throws Exception {
        storedProfiles = profileManager.getAllProfiles();
        removeProfiles(storedProfiles);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        RGBImageProfile[] allProfiles = profileManager.getAllProfiles();
        removeProfiles(allProfiles);
        for (RGBImageProfile profile : storedProfiles) {
            profileManager.addProfile(profile);
        }
    }

    @Test
    public void installRGBProfiles() throws Exception {
        assertEquals(0, profileManager.getProfileCount());
        C2rccCommons.installRGBProfiles();
        assertEquals(9, profileManager.getProfileCount());
    }

    private static void removeProfiles(RGBImageProfile[] storedProfiles) {
        for (RGBImageProfile profile : storedProfiles) {
            profileManager.removeProfile(profile);
        }
    }

}