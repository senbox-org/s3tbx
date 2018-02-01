package org.esa.s3tbx.c2rcc.util;

import org.esa.snap.core.datamodel.RGBImageProfile;
import org.esa.snap.core.datamodel.RGBImageProfileManager;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * @author Marco Peters
 */
public class RgbProfiles {
    private static final String[] RGB_BAND_NAMES = {
            "rhow", "rrs", "rhown", "rpath", "rtoa",
            "rtosa_gc", "rtosagc_aann", "tdown", "tup"
    };

    public static void installRgbProfiles(String productType, String redExpr, String greenExpr, String blueExpr) {
        RGBImageProfileManager profileManager = RGBImageProfileManager.getInstance();
        RGBImageProfile[] allProfiles = profileManager.getAllProfiles();
        for (String bandName : RGB_BAND_NAMES) {
            String profileName = productType + " " + bandName;
            Stream<RGBImageProfile> profileStream = Arrays.stream(allProfiles);
            boolean profileExists = profileStream.anyMatch(rgbImageProfile -> rgbImageProfile.getName().equals(profileName));
            if (!profileExists) {
                String[] rgbaExpressions = {
                        String.format(redExpr, bandName),
                        String.format(greenExpr, bandName),
                        String.format(blueExpr, bandName)};
                String[] pattern = {productType, "", ""};
                profileManager.addProfile(new RGBImageProfile(profileName, rgbaExpressions, pattern));
            }
        }
    }

    public static void installMerisRgbProfiles() {
        installRgbProfiles("C2RCC_MERIS",
                           "log(0.05 + 0.35 * %1$s_2 + 0.60 * %1$s_5 + %1$s_6 + 0.13 * %1$s_7)",
                           "log(0.05 + 0.21 * %1$s_3 + 0.50 * %1$s_4 + %1$s_5 + 0.38 * %1$s_6)",
                           "log(0.05 + 0.21 * %1$s_1 + 1.75 * %1$s_2 + 0.47 * %1$s_3 + 0.16 * %1$s_4)");
    }

    public static void installMeris4RgbProfiles() {
        installRgbProfiles("C2RCC_MERIS4",
                           "log(0.05 + 0.35 * %1$s_2 + 0.60 * %1$s_5 + %1$s_6 + 0.13 * %1$s_7)",
                           "log(0.05 + 0.21 * %1$s_3 + 0.50 * %1$s_4 + %1$s_5 + 0.38 * %1$s_6)",
                           "log(0.05 + 0.21 * %1$s_1 + 1.75 * %1$s_2 + 0.47 * %1$s_3 + 0.16 * %1$s_4)");
    }

    public static void installS2MsiRgbProfiles() {
        installRgbProfiles("C2RCC_S2-MSI",
                           "log(0.05 + 0.35 * %1$s_B2 + 0.60 * %1$s_B5 + %1$s_6 + 0.13 * %1$s_B7)",
                           "log(0.05 + 0.21 * %1$s_B3 + 0.50 * %1$s_B4 + %1$s_B5 + 0.38 * %1$s_B6)",
                           "log(0.05 + 0.21 * %1$s_B1 + 1.75 * %1$s_B2 + 0.47 * %1$s_B3 + 0.16 * %1$s_B4)");
    }

    public static void installLandsat7RgbProfiles() {
        installRgbProfiles("C2RCC_LANDSAT-7",
                           "%1$s_3",
                           "%1$s_2",
                           "%1$s_1"
        );
    }

    public static void installLandsat8RgbProfiles() {
        installRgbProfiles("C2RCC_LANDSAT-8",
                           "%1$s_4",
                           "%1$s_3",
                           "%1$s_2"
        );
    }

    public static void installModisRgbProfiles() {
        installRgbProfiles("C2RCC_MODIS",
                           "%1$s_667)",
                           "%1$s_547)",
                           "%1$s_443)");
    }

    public static void installSeaWifsRgbProfiles() {
        installRgbProfiles("C2RCC_SEAWIFS",
                           "%1$s_670",
                           "%1$s_555",
                           "%1$s_443");
    }

    public static void installViirsRgbProfiles() {
        installRgbProfiles("C2RCC_VIIRS",
                           "%1$s_671",
                           "%1$s_551",
                           "%1$s_443");
    }

}
