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
 */

package org.esa.s3tbx.olci.anomaly.flagging;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;

@OperatorMetadata(alias = "OlciAnomalyFlagging",
        version = "1.0",
        authors = "T. Block",
        category = "Optical/Preprocessing",
        copyright = "Copyright (C) 2021 by Brockmann Consult",
        description = "Adds a flagging band indicating saturated pixels and altitude data overflows")
public class OlciAnomalyFlaggingOp extends Operator {

    @SourceProduct(description = "OLCI L1b or fully compatible product.",
            label = "OLCI L1b product")
    private Product l1bProduct;

    @TargetProduct
    private Product targetProduct;

    @Override
    public void initialize() throws OperatorException {
        // validate input
        // - solar flux
        // - sza
        // - wavelength

        // create output product
        // - copy all inputs
        // - add anomaly-flags band


    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {

    }

    // package access for testing only tb 2021-04-08
    static double toReflectance(double radiance, double invSolarFlux, double invCosSZA) {
        return radiance * Math.PI * invSolarFlux * invCosSZA;
    }

    static void validateInputProduct(Product input) {
        checkRadianceBands(input, 2, 12);
        checkRadianceBands(input, 16, 18);
        checkRadianceBands(input, 21, 21);
    }

    private static void checkRadianceBands(Product input, int lower, int upper) {
        for (int i = lower; i <= upper; i++) {
            final String variableName = "Oa" + String.format("%02d", i) + "_radiance";
            if (!input.containsBand(variableName)) {
                throw new OperatorException("Input variable '" + variableName + "' missing.");
            }
        }
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(OlciAnomalyFlaggingOp.class);
        }
    }
}
