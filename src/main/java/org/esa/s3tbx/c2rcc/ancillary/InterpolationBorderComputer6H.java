package org.esa.s3tbx.c2rcc.ancillary;

import static org.esa.s3tbx.c2rcc.ancillary.AncillaryCommons.convertToFileNamePräfix;

public class InterpolationBorderComputer6H implements InterpolationBorderComputer {

    private double startFileTimeMJD;

    @Override
    public void setInterpolationTimeMJD(double timeMJD) {
        startFileTimeMJD = Math.floor((timeMJD - 0.125) * 4) * 0.25;
    }

    @Override
    public double getStartBorderTimeMDJ() {
        return startFileTimeMJD + 0.125;
    }

    @Override
    public double getEndBorderTimeMJD() {
        return getStartBorderTimeMDJ() + 0.25;
    }

    @Override
    public String getStartAncFilePrefix() {
        return convertToFileNamePräfix(startFileTimeMJD);
    }

    @Override
    public String getEndAncFilePrefix() {
        return convertToFileNamePräfix(startFileTimeMJD + 0.25);
    }

}
