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
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;

@OperatorMetadata(alias = "OlciAnomalyFlagging",
        version = "1.0",
        authors = "T. Block",
        category = "Optical/Preprocessing",
        copyright = "Copyright (C) 2021 by Brockmann Consult",
        description = "Adds a flagging band indicating saturated pixels and altitude data overflows")
public class OlciAnomalyFlaggingOp extends Operator {

    private static final String SUFFIX = "_ANOM_FLAG";

    @SourceProduct(description = "OLCI L1b or fully compatible product.",
            label = "OLCI L1b product")
    private Product l1bProduct;

    @TargetProduct
    private Product targetProduct;

    @Parameter(defaultValue = "false",
            description = "If set to true, the operator adds two bands containing the maximal spectral slope and the band index where the peak is observed.",
            label = "Write spectral slope information")
    private boolean writeSlopeInformation;

    // package access for testing only tb 2021-04-08
    static double toReflectance(double radiance, double invSolarFlux, double invCosSZA) {
        return radiance * Math.PI * invSolarFlux * invCosSZA;
    }

    static void validateInputProduct(Product input) {
        checkRadianceBands(input, 2, 12);
        checkRadianceBands(input, 16, 18);
        checkRadianceBands(input, 21, 21);

        checkSimpleIndexedBands(input, "solar_flux_band_", 2, 12);
        checkSimpleIndexedBands(input, "solar_flux_band_", 16, 18);
        checkSimpleIndexedBands(input, "solar_flux_band_", 21, 21);

        checkSimpleIndexedBands(input, "lambda0_band_", 2, 12);
        checkSimpleIndexedBands(input, "lambda0_band_", 16, 18);
        checkSimpleIndexedBands(input, "lambda0_band_", 21, 21);

        final Band altitude = input.getBand("altitude");
        if (altitude == null) {
            throw new OperatorException("Band 'altitude' missing.");
        }

        final TiePointGrid sza = input.getTiePointGrid("SZA");
        if (sza == null) {
            throw new OperatorException("Tie point grid 'SZA' missing.");
        }
    }

    static Product createOutputProduct(Product input, boolean writeSlopeInformation) {
        final String inputProductType = input.getProductType();
        final String inputName = input.getName();
        final Product outputProduct = new Product(inputName + SUFFIX,
                                                  inputProductType + SUFFIX,
                                                  input.getSceneRasterWidth(),
                                                  input.getSceneRasterHeight());

        outputProduct.setDescription("OLCI anomaly flagged L1b");
        outputProduct.setStartTime(input.getStartTime());
        outputProduct.setEndTime(input.getEndTime());

        final Band[] inputBands = input.getBands();
        for (final Band inputBand : inputBands) {
            ProductUtils.copyBand(inputBand.getName(), input, outputProduct, true);
        }

        ProductUtils.copyFlagCodings(input, outputProduct);
        ProductUtils.copyTiePointGrids(input, outputProduct);
        ProductUtils.copyGeoCoding(input, outputProduct);
        ProductUtils.copyMetadata(input, outputProduct);

        final Band anomalyFlags = outputProduct.addBand("anomaly_flags", ProductData.TYPE_INT8);
        anomalyFlags.setDescription("Flags indicating OLCI data anomalies");

        final FlagCoding flagCoding = new FlagCoding("anomaly_flags");
        flagCoding.addFlag("ANOM_SPECTRAL_MEASURE", 1, "Anomalous spectral sample due to saturation of single microbands");
        flagCoding.addFlag("ALT_OUT_OF_RANGE", 2, "Altitude values are out of nominal data range");
        anomalyFlags.setSampleCoding(flagCoding);

        if (writeSlopeInformation) {
            final Band maxSpectralSlope = outputProduct.addBand("max_spectral_slope", ProductData.TYPE_FLOAT32);
            maxSpectralSlope.setNoDataValue(Float.NaN);
            maxSpectralSlope.setNoDataValueUsed(true);
            maxSpectralSlope.setUnit("1/nm");
            maxSpectralSlope.setDescription("Absolute value of maximal spectral slope for bands 1-12, 16-18, 21");

            final Band maxSlopeBandIndex = outputProduct.addBand("max_slope_band_index", ProductData.TYPE_INT8);
            maxSlopeBandIndex.setNoDataValue(-1);
            maxSlopeBandIndex.setNoDataValueUsed(true);
            maxSlopeBandIndex.setDescription("Band index where the maximal slope is detected");
        }

        return outputProduct;
    }

    private static void checkRadianceBands(Product input, int lower, int upper) {
        for (int i = lower; i <= upper; i++) {
            final String variableName = "Oa" + String.format("%02d", i) + "_radiance";
            if (!input.containsBand(variableName)) {
                throw new OperatorException("Input variable '" + variableName + "' missing.");
            }
        }
    }

    private static void checkSimpleIndexedBands(Product input, String prefix, int lower, int upper) {
        for (int i = lower; i <= upper; i++) {
            final String variableName = prefix + Integer.toString(i);
            if (!input.containsBand(variableName)) {
                throw new OperatorException("Input variable '" + variableName + "' missing.");
            }
        }
    }

    @Override
    public void initialize() throws OperatorException {
        validateInputProduct(l1bProduct);

        targetProduct = createOutputProduct(l1bProduct, writeSlopeInformation);
        setTargetProduct(targetProduct);
    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {

    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(OlciAnomalyFlaggingOp.class);
        }
    }
}
