package org.esa.s3tbx.c2rcc.msi;

import org.esa.snap.core.nn.NNCalc;

interface NeuralNetwork {
    double[] getInmin();

    void setInmin(double[] inmin);

    double[] getInmax();

    void setInmax(double[] inmax);

    double[] getOutmin();

    double[] getOutmax();

    NNCalc calcJacobi(double[] nnInp);

    double[] calc(double[] nninp);

    double[] calc_vtl(double[] nninp, double[][] nninp_vtl);
}
