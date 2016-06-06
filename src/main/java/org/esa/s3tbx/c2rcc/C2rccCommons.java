package org.esa.s3tbx.c2rcc;

import org.esa.snap.core.datamodel.ConstantTimeCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.RGBImageProfile;
import org.esa.snap.core.datamodel.RGBImageProfileManager;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.pointop.Sample;

public class C2rccCommons {

    private static final String[] RGB_PROFILE_NAMES = {
            "rhow", "rrs", "rhown", "rpath", "rtoa",
            "rtosa_gc", "rtosagc_aann", "tdown", "tup"
    };

    public static void ensureTimeCoding_Fallback(Product product) {
        if (product.getSceneTimeCoding() == null) {
            final ProductData.UTC startTime = product.getStartTime();
            final ProductData.UTC endTime = product.getEndTime();
            if (endTime == null || startTime == null) {
                throw new OperatorException("Could not retrieve time information from source product");
            }
            setTimeCoding(product, startTime, endTime);
        }
    }

    public static void setTimeCoding(Product product, ProductData.UTC startTime, ProductData.UTC endTime) {
        if (startTime != null && endTime != null) {
            product.setStartTime(startTime);
            product.setEndTime(endTime);
            double startTimeMJD = startTime.getMJD();
            double constantTime = (endTime.getMJD() - startTimeMJD) / 2.0 + startTimeMJD;
            product.setSceneTimeCoding(new ConstantTimeCoding(constantTime));
        }
    }

    public static boolean areSamplesValid(Sample[] sourceSamples, int x, int y) {
        boolean samplesValid = true;
        for (Sample sourceSample : sourceSamples) {
            // can be null because samples for ozone and atm_pressure might be missing
            RasterDataNode node = sourceSample.getNode();
            if (node != null) {
                if (!node.isPixelValid(x, y)) {
                    samplesValid = false;
                    break;
                }
            }
        }
        return samplesValid;
    }

    static void installRGBProfiles() {
        RGBImageProfileManager profileManager = RGBImageProfileManager.getInstance();
        for (String profileName : RGB_PROFILE_NAMES) {
            profileManager.addProfile(new RGBImageProfile(profileName, new String[]{
                    String.format("log(0.05 + 0.35 * %1$s_2 + 0.60 * %1$s_5 + %1$s_6 + 0.13 * %1$s_7)", profileName),
                    String.format("log(0.05 + 0.21 * %1$s_3 + 0.50 * %1$s_4 + %1$s_5 + 0.38 * %1$s_6)", profileName),
                    String.format("log(0.05 + 0.21 * %1$s_1 + 1.75 * %1$s_2 + 0.47 * %1$s_3 + 0.16 * %1$s_4)", profileName)}));
        }
    }
}
