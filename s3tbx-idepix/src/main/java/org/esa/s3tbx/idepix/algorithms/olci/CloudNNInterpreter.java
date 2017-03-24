package org.esa.s3tbx.idepix.algorithms.olci;

import com.bc.ceres.binding.ValueRange;

/**
 * @author Marco Peters
 */
public class CloudNNInterpreter {

    private static final ValueRange CLEAR_SNOW_ICE_BOUNDS = new ValueRange(0.0, 1.66, true, false);
    private static final ValueRange OPAQUE_CLOUD_BOUNDS = new ValueRange(1.66, 2.46, true, false);
    private static final ValueRange SEMI_TRANS_CLOUD_BOUNDS = new ValueRange(2.46, 3.46, true, false);
    private static final ValueRange SPATIAL_MIXED_BOUNDS = new ValueRange(3.46, 4.52, true, false);
    private static final ValueRange CLEAR_LAND_BOUNDS = new ValueRange(4.52, 5.38, true, false);
    private static final ValueRange CLEAR_WATER_BOUNDS = new ValueRange(5.38, 6.00, true, true);


    private CloudNNInterpreter() {
    }

    // Here we might add the nn as parameter to decide which valueRanges to load
    public static CloudNNInterpreter create() {
        return new CloudNNInterpreter();
    }

    boolean isCloudAmbiguous(double nnValue) {
        return SEMI_TRANS_CLOUD_BOUNDS.contains(nnValue) || SPATIAL_MIXED_BOUNDS.contains(nnValue);
    }

    boolean isCloudSure(double nnValue) {
        return OPAQUE_CLOUD_BOUNDS.contains(nnValue);
    }
    boolean isSnowIce(double nnValue) {
        return CLEAR_SNOW_ICE_BOUNDS.contains(nnValue);

    }

}
