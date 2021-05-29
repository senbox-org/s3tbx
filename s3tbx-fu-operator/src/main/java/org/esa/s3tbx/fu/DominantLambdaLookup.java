/*
 * Copyright (c) 2021.  Brockmann Consult GmbH (info@brockmann-consult.de)
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
 *
 *
 */

package org.esa.s3tbx.fu;

import org.esa.snap.core.util.io.CsvReader;
import org.esa.snap.core.util.math.LookupTable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author Marco Peters
 */
public class DominantLambdaLookup {

    private final static String DEFAULT_LUT_RESOURCE = "hueAngel_domLambda.csv";
    private static final String LUT_NOT_READABLE_ERRMSG = "Could not read LUT for converting hue angle to dominant wavelength.";
    private LookupTable lookupTable;

    /**
     * Creates the default instance using the look-up-table provided in the resources at 'hueAngel_domLambda.csv'.
     */
    public DominantLambdaLookup() {
        this(readLutValues());
    }

    private DominantLambdaLookup(double[][] lutValues) {
        this(lutValues[0], lutValues[1]);
    }

    public DominantLambdaLookup(double[] hueAngles, double[] domLambda) {
        if (hueAngles.length != domLambda.length) {
            throw new IllegalStateException("Number of values for hue angles and dominant Wavelength must be equal");
        }
        lookupTable = new LookupTable(domLambda, hueAngles);
    }

    public double getDominantLambda(double hueAngle) {
        if (hueAngle < 0) {
            return Double.NaN;
        }
        return lookupTable.getValue(hueAngle);
    }

    private static double[][] readLutValues() {
        final InputStream stream = DominantLambdaLookup.class.getResourceAsStream(DEFAULT_LUT_RESOURCE);
        if (stream == null) {
            throw new IllegalStateException(LUT_NOT_READABLE_ERRMSG);
        }
        return createLutValues(stream);
    }

    private static double[][] createLutValues(InputStream stream) {
        final List<double[]> lutValues;
        try (CsvReader csvReader = new CsvReader(new InputStreamReader(stream, StandardCharsets.UTF_8),
                                                 new char[]{';'}, true, "#")) {
            lutValues = csvReader.readDoubleRecords();
        } catch (IOException e) {
            throw new IllegalStateException(LUT_NOT_READABLE_ERRMSG, e);
        }

        final double[] hueAngle = new double[lutValues.size()];
        final double[] domLambda = new double[lutValues.size()];
        for (int i = 0; i < lutValues.size(); i++) {
            double[] doubles = lutValues.get(i);
            hueAngle[i] = doubles[0];
            domLambda[i] = doubles[1];
        }

        return new double[][]{hueAngle, domLambda};
    }
}
