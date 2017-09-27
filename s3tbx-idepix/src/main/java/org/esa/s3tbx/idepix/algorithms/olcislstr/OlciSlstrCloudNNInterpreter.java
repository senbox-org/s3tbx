package org.esa.s3tbx.idepix.algorithms.olcislstr;

import com.bc.ceres.binding.ValueRange;

/**
 * @author Marco Peters
 */
public class OlciSlstrCloudNNInterpreter {

//    Schiller:
//    target
//    1      class 6    clear snow/ice
//    2      class 1    opaque cloud
//    3      class 2    semi-transparent cloud
//    4      class 3    spatially mixed cloud
//    5      class 4    clear Land
//    6      class 5    clear water
//    und den cuts best
//    1.7    2.5    3.46    4.54    5.46

    private static final ValueRange CLEAR_SNOW_ICE_BOUNDS = new ValueRange(0.0, 1.7, true, false);
    private static final ValueRange OPAQUE_CLOUD_BOUNDS = new ValueRange(1.7, 2.5, true, false);
    private static final ValueRange SEMI_TRANS_CLOUD_BOUNDS = new ValueRange(2.5, 3.46, true, false);
    private static final ValueRange SPATIAL_MIXED_CLOUD_BOUNDS = new ValueRange(3.46, 4.54, true, false);
    private static final ValueRange CLEAR_LAND_BOUNDS = new ValueRange(4.54, 5.46, true, false);
    private static final ValueRange CLEAR_WATER_BOUNDS = new ValueRange(5.46, 6.00, true, false);

    private OlciSlstrCloudNNInterpreter() {
    }

    // Here we might add the nn as parameter to decide which valueRanges to load
    public static OlciSlstrCloudNNInterpreter create() {
        return new OlciSlstrCloudNNInterpreter();
    }

    boolean isCloudAmbiguous(double nnValue, boolean isLand, boolean considerGlint) {
        return SEMI_TRANS_CLOUD_BOUNDS.contains(nnValue) || SPATIAL_MIXED_CLOUD_BOUNDS.contains(nnValue);
    }

    boolean isCloudSure(double nnValue) {
        return OPAQUE_CLOUD_BOUNDS.contains(nnValue);
    }

    boolean isSnowIce(double nnValue) {
        return CLEAR_SNOW_ICE_BOUNDS.contains(nnValue);

    }

}
