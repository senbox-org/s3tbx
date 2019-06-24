package org.esa.s3tbx.olci.harmonisation;

/**
 * Holder for contents of 'desmile' lookup table (e.g. O2_desmile_lut_13.json)
 *
 * @author olafd
 */
public class DesmileLut {
    private long L;
    private long M;
    private long N;

    private double[][] X;
    private double[][] Y;
    private double[][][] JACO;
    private double[] MEAN;
    private double[] VARI;
    private double cwvl;
    private double cbwd;
    private long leafsize;
    private String[] sequ;

    DesmileLut(long l, long m, long n,
               double[][] x, double[][] y,
               double[][][] JACO, double[] MEAN, double[] VARI,
               double cwvl, double cbwd,
               long leafsize,
               String[] sequ) {
        this.L = l;
        this.M = m;
        this.N = n;
        this.X = x;
        this.Y = y;
        this.JACO = JACO;
        this.MEAN = MEAN;
        this.VARI = VARI;
        this.cwvl = cwvl;
        this.cbwd = cbwd;
        this.leafsize = leafsize;
        this.sequ = sequ;
    }

    public long getL() {
        return L;
    }

    public long getM() {
        return M;
    }

    public long getN() {
        return N;
    }

    public double[][] getX() {
        return X;
    }

    public double[][] getY() {
        return Y;
    }

    public double[][][] getJACO() {
        return JACO;
    }

    public double[] getMEAN() {
        return MEAN;
    }

    public double[] getVARI() {
        return VARI;
    }

    public double getCwvl() {
        return cwvl;
    }

    public double getCbwd() {
        return cbwd;
    }

    public long getLeafsize() {
        return leafsize;
    }

    public String[] getSequ() {
        return sequ;
    }
}
