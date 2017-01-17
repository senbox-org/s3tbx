package org.esa.s3tbx.dataio.s3;

import org.esa.snap.core.datamodel.RGBImageProfile;
import org.esa.snap.core.datamodel.RGBImageProfileManager;

/**
 * @author Marco Peters
 */
class Sentinel3RgbProfiles {

    static void registerRGBProfiles() {
        RGBImageProfileManager manager = RGBImageProfileManager.getInstance();
        manager.addProfile(new RGBImageProfile("OLCI L1 - Tristimulus",
                                               new String[]{
                                                       "log(1.0 + 0.01 * Oa01_radiance + 0.09 * Oa02_radiance + 0.35 * Oa03_radiance + 0.04 * Oa04_radiance + " +
                                                       "0.01 * Oa05_radiance + 0.59 * Oa06_radiance + 0.85 * Oa07_radiance + 0.12 * Oa08_radiance + " +
                                                       "0.07 * Oa09_radiance + 0.04 * Oa10_radiance)",
                                                       "log(1.0 + 0.26 * Oa03_radiance + 0.21 * Oa04_radiance + 0.50 * Oa05_radiance + Oa06_radiance + " +
                                                       "0.38 * Oa07_radiance + 0.04 * Oa08_radiance + 0.03 * Oa09_radiance + 0.02 * Oa10_radiance)",
                                                       "log(1.0 + 0.07 * Oa01_radiance + 0.28 * Oa02_radiance + 1.77 * Oa03_radiance + 0.47 * Oa04_radiance + " +
                                                       "0.16 * Oa05_radiance)"
                                               },
                                               new String[]{
                                                       "OL_1_*",
                                                       "S3*_OL_1*",
                                                       "",
                                               }
        ));
        manager.addProfile(new RGBImageProfile("OLCI L2 W - Tristimulus",
                                               new String[]{
                                                       "log(0.05 + 0.01 * Oa01_reflectance + 0.09 * Oa02_reflectance + 0.35 * Oa03_reflectance + " +
                                                       "0.04 * Oa04_reflectance + 0.01 * Oa05_reflectance + 0.59 * Oa06_reflectance + " +
                                                       "0.85 * Oa07_reflectance + 0.12 * Oa08_reflectance + 0.07 * Oa09_reflectance + " +
                                                       "0.04 * Oa10_reflectance)",
                                                       "log(0.05 + 0.26 * Oa03_reflectance + 0.21 * Oa04_reflectance + 0.50 * Oa05_reflectance + " +
                                                       "Oa06_reflectance + 0.38 * Oa07_reflectance + 0.04 * Oa08_reflectance + " +
                                                       "0.03 * Oa09_reflectance + 0.02 * Oa10_reflectance)",
                                                       "log(0.05 + 0.07 * Oa01_reflectance + 0.28 * Oa02_reflectance + 1.77 * Oa03_reflectance + " +
                                                       "0.47 * Oa04_reflectance + 0.16 * Oa05_reflectance)"
                                               },
                                               new String[]{
                                                       "OL_2_W*",
                                                       "S3*OL_2_W*",
                                                       "",
                                               }
        ));
        manager.addProfile(new RGBImageProfile("OLCI L1 - 17,6,3",
                                               new String[]{
                                                       "Oa17_radiance",
                                                       "Oa06_radiance",
                                                       "Oa03_radiance"
                                               }
        ));
        manager.addProfile(new RGBImageProfile("OLCI L1 - 17,5,2",
                                               new String[]{
                                                       "Oa17_radiance",
                                                       "Oa05_radiance",
                                                       "Oa02_radiance"
                                               }
        ));
        manager.addProfile(new RGBImageProfile("OLCI L2W - 17,6,3",
                                               new String[]{
                                                       "Oa17_reflectance",
                                                       "Oa06_reflectance",
                                                       "Oa03_reflectance"
                                               }
        ));
        manager.addProfile(new RGBImageProfile("OLCI L2W - 17,5,2",
                                               new String[]{
                                                       "Oa17_reflectance",
                                                       "Oa05_reflectance",
                                                       "Oa02_reflectance"
                                               }
        ));
        manager.addProfile(new RGBImageProfile("SLSTR L1 - Nadir",
                                               new String[]{
                                                       "S3_radiance_an",
                                                       "S2_radiance_an",
                                                       "S1_radiance_an",
                                               }
        ));
        manager.addProfile(new RGBImageProfile("SLSTR L1 - Nadir, False colour",
                                               new String[]{
                                                       "S5_radiance_an",
                                                       "S3_radiance_an",
                                                       "S2_radiance_an",
                                               }
        ));
        manager.addProfile(new RGBImageProfile("SLSTR L1 - Oblique",
                                               new String[]{
                                                       "S3_radiance_ao",
                                                       "S2_radiance_ao",
                                                       "S1_radiance_ao",
                                               }
        ));
        manager.addProfile(new RGBImageProfile("SLSTR L1 - Oblique, False colour",
                                               new String[]{
                                                       "S5_radiance_ao",
                                                       "S3_radiance_ao",
                                                       "S2_radiance_ao",
                                               }
        ));
        manager.addProfile(new RGBImageProfile("SYN L2 OLCI - Tristimulus",
                                               new String[]{
                                                       "log(0.05 + 0.01 * SDR_01 + 0.09 * SDR_02 + 0.35 * SDR_03 + " +
                                                       "0.04 * SDR_04 + 0.01 * SDR_05 + 0.59 * SDR_06 + " +
                                                       "0.85 * SDR_07 + 0.12 * SDR_08 + 0.07 * SDR_09 + " +
                                                       "0.04 * SDR_10)",
                                                       "log(0.05 + 0.26 * SDR_03 + 0.21 * SDR_03 + 0.50 * SDR_05 + " +
                                                       "SDR_06 + 0.38 * SDR_07 + 0.04 * SDR_08 + " +
                                                       "0.03 * SDR_09 + 0.02 * SDR_10)",
                                                       "log(0.05 + 0.07 * SDR_01 + 0.28 * SDR_02 + 1.77 * SDR_03 + " +
                                                       "0.47 * SDR_04 + 0.16 * SDR_05)"
                                               },
                                               new String[]{
                                                       "SY_2_SYN",
                                                       "S3*SY_2_SYN*",
                                                       "",
                                               }
        ));
        manager.addProfile(new RGBImageProfile("SYN L2 SLSTR - False colour",
                                               new String[]{
                                                       "SDR_23",
                                                       "SDR_21",
                                                       "SDR_20",
                                               },
                                               new String[]{
                                                       "SY_2_SYN",
                                                       "S3*SY_2_SYN*",
                                                       "",
                                               }
        ));
        manager.addProfile(new RGBImageProfile("SYN L2 SLSTR/OLCI- False colour",
                                               new String[]{
                                                       "SDR_23",
                                                       "SDR_15",
                                                       "SDR_08",
                                               },
                                               new String[]{
                                                       "SY_2_SYN",
                                                       "S3*SY_2_SYN*",
                                                       "",
                                               }
        ));
    }
}
