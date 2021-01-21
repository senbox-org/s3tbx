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

package org.esa.s3tbx.olci.sensor.harmonisation;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;

/**
 * Performs OLCI sensor harmonisation on OLCI L1b products.
 * Implements algorithm described in "OLCI A/B Tandem Phase Analysis, Nicolas Lamquin, Sébastien Clerc,
 * Ludovic Bourg and Craig Donlon"
 * <p/>
 *
 * @author Tom Block
 */

@OperatorMetadata(alias = "OlciSensorHarmonisation",
        version = "1.0",
        authors = "T. Block",
        category = "Optical/Preprocessing",
        copyright = "Copyright (C) 2021 by Brockmann Consult",
        description = "Performs sensor harmonisation on OLCI L1b product. Implements algorithm described in 'OLCI A/B Tandem Phase Analysis'")
public class OlciSensorHarmonisationOp extends Operator {

    @SourceProduct(description = "OLCI L1b or fully compatible product.",
            label = "OLCI L1b product")
    private Product l1bProduct;

    @TargetProduct
    private Product targetProduct;

    static void validateInputProduct(Product input) {
        if (!input.containsBand("detector_index")) {
            throw new OperatorException("Input variable 'detector_index' missing.");
        }
        for (int i = 1; i <= 21; i++) {
            final String variableName = "Oa" + String.format("%02d", i) + "_radiance";
            if (!input.containsBand(variableName)) {
                throw new OperatorException("Input variable '" + variableName + "' missing.");
            }
        }

        // @todo 1 tb/tb check for central wavelength metadata 2021-01-21
    }

    @Override
    public void initialize() throws OperatorException {
        validateInputProduct(l1bProduct);
        // create output
        // load auxdata
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(OlciSensorHarmonisationOp.class);
        }
    }
}
