package org.esa.s3tbx;

import com.bc.ceres.core.Assert;

import java.util.stream.DoubleStream;

public class ArrayMath {

    /**
     * Returns an array. Same size as input. Filled with all the input values raised with
     * Euler's number <i>e</i> to the power of x[n]
     * @see Math#exp(double)
     * @param x
     * @return
     */
    public static double[] a_exp(double[] x) {
        return DoubleStream.of(x).map(Math::exp).toArray();
    }

    /**
     *
     * @param x
     * @param ind
     * @return
     */
    public static double[] a_ind(double[] x, int[] ind) {
        double[] y = new double[ind.length];
        for (int i = 0; i < ind.length; i++) {
            y[i] = x[ind[i]];
        }
        return y;
    }

    /**
     * Returns the smallest value of all the {@code double} values.
     * @see Math#min(double, double)
     * @param x
     * @return
     */
    public static double a_min(double[] x) {
        double min = Double.POSITIVE_INFINITY;
        for (double v : x) {
            min = Math.min(min, v);
        }
        return min;
    }

    /**
     * Returns the greatest value of all the {@code double} values.
     * @see Math#max(double, double)
     * @param x
     * @return
     */
    public static double a_max(double[] x) {
        double max = Double.NEGATIVE_INFINITY;
        for (double v : x) {
            max = Math.max(max, v);
        }
        return max;
    }

    /**
     * Returns a {@code double[]} array with quotients x[n] divided by y[n].
     * It is mandatory that both input arrays must have the same size.
     * @param x
     * @param y
     * @return
     */
    public static double[] a_div(double[] x, double[] y) {
        Assert.argument(x.length == y.length);
        double[] z = new double[x.length];
        for (int i = 0; i < x.length; i++) {
            z[i] = x[i] / y[i];
        }
        return z;
    }

    /**
     * Returns an array of log(x)
     * @see Math#log(double)
     * @param x
     * @return
     */
    public static double[] a_log(double[] x) {
        return DoubleStream.of(x).map(Math::log).toArray();
    }

    public static double[] a_mul(double[] v, double mul) {
        for (int i = 0; i < v.length; i++) {
            v[i] = v[i] * mul;
        }
        return v;
    }
}
