package org.esa.s3tbx.c2rcc.msi;

interface NeuralNetwork {

    double[] getInmin();

    double[] getInmax();

    double[] getOutmin();

    double[] getOutmax();

    double[] calc(double[] x);

    double[] calc_tl(double[] x, double[] x_tl);
}
