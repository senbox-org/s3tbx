package org.esa.s3tbx.c2rcc.ancillary;

import static org.esa.s3tbx.c2rcc.ancillary.AncillaryCommons.convertToFileNamePräfix;

public class InterpolationBorderComputer24H implements InterpolationBorderComputer {

    private double startFileTimeMJD;

    @Override
    public void setInterpolationTimeMJD(double timeMJD) {
        startFileTimeMJD = Math.floor(timeMJD - 0.5);
    }

    @Override
    public double getStartBorderTimeMDJ() {
        return startFileTimeMJD + 0.5;
    }

    @Override
    public double getEndBorderTimeMJD() {
        return getStartBorderTimeMDJ() + 1;
    }

    @Override
    public String getStartAncFilePrefix() {
        return convertToFileNamePräfix(startFileTimeMJD);
    }

    @Override
    public String getEndAncFilePrefix() {
        return convertToFileNamePräfix(startFileTimeMJD + 1);
    }
}
