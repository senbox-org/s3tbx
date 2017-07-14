package org.esa.s3tbx.c2rcc.util;

import com.bc.ceres.core.Assert;

import java.util.stream.DoubleStream;

import static java.lang.Math.*;

public class ArrayMath {

    /**
     * Returns an array. Same size as input. Filled with all the input values raised with
     * Euler's number <i>e</i> to the power of x[n]
     * @see Math#exp(double)
     */
    public static double[] a_exp(double[] x) {
        return DoubleStream.of(x).map(Math::exp).toArray();
    }

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
     */
    public static double a_max(double[] x) {
        double max = Double.NEGATIVE_INFINITY;
        for (double v : x) {
            max = Math.max(max, v);
        }
        return max;
    }

    public static double a_sumx(double[] x,int anf, int ende) {
        double suma; //= new double; //Double.NEGATIVE_INFINITY;
        suma= 0.0;
        for (int i=anf; i < (ende+1); i++) {
            suma = suma+x[i];
        }
        return suma;
    }

    /**
     * Returns a {@code double[]} array with quotients x[n] divided by y[n].
     * It is mandatory that both input arrays must have the same size.
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

    public static double[] a_abs(double[] x, double[] y) {
        Assert.argument(x.length == y.length);
        double[] z = new double[x.length];
        for (int i = 0; i < x.length; i++) {
            z[i] = abs(x[i] - y[i]);
        }
        return z;
    }

}
