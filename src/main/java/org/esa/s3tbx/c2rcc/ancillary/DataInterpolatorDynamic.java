package org.esa.s3tbx.c2rcc.ancillary;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DataInterpolatorDynamic extends DataInterpolator {

    private final Map<Double, DataInterpolatorStatic> interpolatorMap;
    private final AncRepository ancRepository;
    private final InterpolationBorderComputer ibc;
    private final AncDataFormat ancDataFormat;

    private double currentBorderTime = Double.NaN;
    private DataInterpolatorStatic currentDataInterpolator;

    public DataInterpolatorDynamic(AncDataFormat ancDataFormat, AncRepository ancRepository) {
        this.ancDataFormat = ancDataFormat;
        ibc = ancDataFormat.getInterpolationBorderComputer();
        this.ancRepository = ancRepository;

        interpolatorMap = new HashMap<>();
    }

    @Override
    synchronized double getValue(double timeMJD, double lat, double lon) throws IOException {
        ibc.setInterpolationTimeMJD(timeMJD);
        final double startBorderTimeMDJ = ibc.getStartBorderTimeMDJ();
        if (currentBorderTime == startBorderTimeMDJ) {
            return currentDataInterpolator.getValue(timeMJD, lat, lon);
        }
        if (interpolatorMap.containsKey(startBorderTimeMDJ)) {
            currentDataInterpolator = interpolatorMap.get(startBorderTimeMDJ);
            if (currentDataInterpolator == null) {
                interpolatorMap.remove(startBorderTimeMDJ);
            } else {
                currentBorderTime = startBorderTimeMDJ;
                return currentDataInterpolator.getValue(timeMJD, lat, lon);
            }
        }
        final String[] startFilenames = ancDataFormat.getFilenames(ibc.getStartAncFilePrefix());
        final String[] endFilenames = ancDataFormat.getFilenames(ibc.getEndAncFilePrefix());
        currentDataInterpolator = new DataInterpolatorStatic
                    (
                                startBorderTimeMDJ, ibc.getEndBorderTimeMJD(),
                                ancRepository.getProduct(startFilenames),
                                ancRepository.getProduct(endFilenames),
                                ancDataFormat.getBandname(),
                                ancDataFormat.getDefaultvalue()
                    );
        interpolatorMap.put(startBorderTimeMDJ, currentDataInterpolator);
        currentBorderTime = startBorderTimeMDJ;
        return currentDataInterpolator.getValue(timeMJD, lat, lon);
    }

    @Override
    void dispose() {
        for (DataInterpolatorStatic dataInterpolatorStatic : interpolatorMap.values()) {
            dataInterpolatorStatic.dispose();
        }
        ancRepository.dispose();
    }

}
