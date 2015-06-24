package org.esa.s3tbx.c2rcc;

/**
 * @author Norman Fomferra
 */
public class C2RCCResult {
    double[] rw;
    double[] iops;

    public C2RCCResult(double[] rw, double[] iops) {
        this.rw = rw;
        this.iops = iops;
    }
}
