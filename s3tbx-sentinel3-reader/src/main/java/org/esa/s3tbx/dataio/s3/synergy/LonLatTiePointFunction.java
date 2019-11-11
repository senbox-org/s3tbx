package org.esa.s3tbx.dataio.s3.synergy;

import org.esa.s3tbx.dataio.s3.LonLatFunction;

public class LonLatTiePointFunction implements LonLatFunction {

    private LonLatTiePointFunctionSource functionSource;
    private int index;

    LonLatTiePointFunction(LonLatTiePointFunctionSource source, int index) {
        functionSource = source;
        this.index = index;
    }

    @Override
    public double getValue(double lon, double lat) {
        return functionSource.getValue(lon, lat, index);
    }

}
