package org.esa.s3tbx.c2rcc.anc;

import static org.esa.s3tbx.c2rcc.anc.InterpolationBorderComputer.convertToFileNamePräfix;

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
