/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.s3tbx.fu;

import static org.esa.s3tbx.fu.Instrument.Constants.*;

enum Instrument {
    AUTO_DETECT(new double[]{}, new double[]{}, new double[]{}, new double[]{}, new double[]{}, new String[]{""}, false),
    MERIS(MERIS_WAVELENGTHS, MERIS_XFACTORS, MERIS_YFACTORS, MERIS_ZFACTORS, MERIS_POLYFACTORS, MERIS_VALID_EXPRESSIONS, true),
    OLCI(OLCI_WAVELENGTHS, OLCI_XFACTORS, OLCI_YFACTORS, OLCI_ZFACTORS, OLCI_POLYFACTORS, OLCI_VALID_EXPRESSIONS, false),
    MODIS(MODIS_WAVELENGTHS, MODIS_XFACTORS, MODIS_YFACTORS, MODIS_ZFACTORS, MODIS_POLYFACTORS, MODIS_VALID_EXPRESSIONS, false),
    SEAWIFS(SEAWIFS_WAVELENGTHS, SEAWIFS_XFACTORS, SEAWIFS_YFACTORS, SEAWIFS_ZFACTORS, SEAWIFS_POLYFACTORS, SEAWIFS_VALID_EXPRESSIONS, false);


    private final double[] wavelengths;
    private double[] xFactors;
    private double[] yFactors;
    private double[] zFactors;
    private double[] polynomCoefficients;
    private final String[] validExpressions;
    private final boolean isIrradiance;

    Instrument(double[] wavelengths, double[] xFactors, double[] yFactors, double[] zFactors, double[] polynomCoefficients, String[] validExpressions,
               boolean isIrradiance) {
        this.wavelengths = wavelengths;
        this.xFactors = xFactors;
        this.yFactors = yFactors;
        this.zFactors = zFactors;
        this.polynomCoefficients = polynomCoefficients;
        this.validExpressions = validExpressions;
        this.isIrradiance = isIrradiance;
    }

    public String[] getValidExpressions() {
        return validExpressions;
    }

    public double[] getWavelengths() {
        return wavelengths;
    }

    public boolean isIrradiance() {
        return isIrradiance;
    }

    public double[] getXFactors() {
        return xFactors;
    }

    public double[] getYFactors() {
        return yFactors;
    }

    public double[] getZFactors() {
        return zFactors;
    }

    public double[] getPolynomCoefficients() {
        return polynomCoefficients;
    }

    static class Constants {

        // MERIS Constants
        static final double[] MERIS_WAVELENGTHS = {412.691, 442.55902, 489.88202, 509.81903, 559.69403, 619.601, 664.57306, 680.82104, 708.32904};
        static final String[] MERIS_VALID_EXPRESSIONS = new String[]{
                "l2_flags.WATER",
                "not l1p_flags.CC_LAND and not l1p_flags.CC_CLOUD",
                "NOT l1_flags.LAND_OCEAN"
        };
        static final double[] MERIS_XFACTORS = new double[]{2.957, 10.861, 3.744, 3.750, 34.687, 41.853, 7.619, 0.844, 0.189};
        static final double[] MERIS_YFACTORS = new double[]{0.112, 1.711, 5.672, 23.263, 48.791, 23.949, 2.944, 0.307, 0.068};
        static final double[] MERIS_ZFACTORS = new double[]{14.354, 58.356, 28.227, 4.022, 0.618, 0.026, 0.000, 0.000, 0.000};
        static final double[] MERIS_POLYFACTORS = new double[]{-12.0506, 88.9325, -244.6960, 305.2361, -164.6960, 28.5255};
        // OLCI Constants
        static final double[] OLCI_WAVELENGTHS = {400.0, 412.5, 442.5, 490.0, 510.0, 560.0, 620.0, 665.0, 673.75, 681.25, 708.75};
        static final String[] OLCI_VALID_EXPRESSIONS = {"WQSF_lsb.WATER and not WQSF_lsb.CLOUD", "LQSF.WATER"};
        static final double[] OLCI_XFACTORS = new double[]{0.154, 2.957, 10.861, 3.744, 3.750, 34.687, 41.853, 7.323, 0.591, 0.549, 0.189};
        static final double[] OLCI_YFACTORS = new double[]{0.004, 0.112, 1.711, 5.672, 23.263, 48.791, 23.949, 2.836, 0.216, 0.199, 0.068};
        static final double[] OLCI_ZFACTORS = new double[]{0.731, 14.354, 58.356, 28.227, 4.022, 0.618, 0.026, 0.000, 0.000, 0.000, 0.000};
        static final double[] OLCI_POLYFACTORS = new double[]{-12.5076, 91.6345, -249.848, 308.6561, -165.4818, 28.5608};
        // MODIS Constants
        static final double[] MODIS_WAVELENGTHS = {412.0, 443.0, 488.0, 531.0, 555.0, 667.0, 678.0};
        static final String[] MODIS_VALID_EXPRESSIONS = {"not l2_flags.LAND and not l2_flags.CLDICE"};
        static final double[] MODIS_XFACTORS = new double[]{2.957, 10.861, 4.031, 3.989, 49.037, 34.586, 0.829};
        static final double[] MODIS_YFACTORS = new double[]{0.112, 1.711, 11.106, 22.579, 51.477, 19.452, 0.301};
        static final double[] MODIS_ZFACTORS = new double[]{14.354, 58.356, 29.993, 2.618, 0.262, 0.000, 0.000};
        static final double[] MODIS_POLYFACTORS = new double[]{-48.0880, 362.6179, -1011.7151, 1262.0348, -666.5981, 113.9215};
        // SEAWIFS Constants
        static final double[] SEAWIFS_WAVELENGTHS = {412.0, 443.0, 490.0, 510.0, 555.0, 670.0};
        static final String[] SEAWIFS_VALID_EXPRESSIONS = {"not l2_flags.LAND and not l2_flags.CLDICE"};
        static final double[] SEAWIFS_XFACTORS = new double[]{2.957, 10.861, 3.744, 3.455, 52.304, 32.825};
        static final double[] SEAWIFS_YFACTORS = new double[]{0.112, 1.711, 5.672, 21.929, 59.454, 17.810};
        static final double[] SEAWIFS_ZFACTORS = new double[]{14.354, 58.356, 28.227, 3.967, 0.682, 0.018};
        static final double[] SEAWIFS_POLYFACTORS = new double[]{-49.4377, 363.2770, -978.1648, 1154.6030, -552.2701, 78.2940};
    }
}
