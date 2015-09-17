package org.esa.s3tbx.c2rcc.ancillary;

import org.esa.snap.framework.datamodel.ProductData;

import java.util.Calendar;

public interface InterpolationBorderComputer {

    void setInterpolationTimeMJD(double timeMJD);

    double getStartBorderTimeMDJ();

    double getEndBorderTimeMJD();

    String getStartAncFilePrefix();

    String getEndAncFilePrefix();

}
