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

import java.awt.Rectangle;
import java.util.Map;

@OperatorMetadata(alias = "OlciAnomalyFlagging",
        version = "1.0",
        authors = "T. Block",
        category = "Optical/Masking",
        copyright = "Copyright (C) 2021 by Brockmann Consult",
        description = "Adds a flagging band indicating saturated pixels and altitude data overflows")
public class OlciAnomalyFlaggingOp extends Operator {

    private static final String SUFFIX = "_ANOM_FLAG";
    private static final float ALTITUDE_MAX = 8850.f;
    private static final float ALTITUDE_MIN = -11050.f;
    private static final int ALT_OUT_OF_RANGE = 2;
    public static final int ANOM_SPECTRAL_MEASURE = 1;
    private static int[] bandIndices = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 16, 17, 18, 21};

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

    /**
     * converts radiances to reflectances, in-place. The incoming array is overwritten.
     *
     * @param radiances   vector of radiances to convert
     * @param solarFluxes solar fluxes for each radiance channel
     * @param invCosSZA   inverted cos solar zenith angle
     * @return reflectance vector
     */
    static double[] toReflectance(double[] radiances, double[] solarFluxes, double invCosSZA) {
        for (int i = 0; i < radiances.length; i++) {
            final double invSolarFlux = 1.0 / solarFluxes[i];

            radiances[i] = radiances[i] * Math.PI * invSolarFlux * invCosSZA;

        }
        return radiances;
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
        flagCoding.addFlag("ANOM_SPECTRAL_MEASURE", ANOM_SPECTRAL_MEASURE, "Anomalous spectral sample due to saturation of single microbands");
        flagCoding.addFlag("ALT_OUT_OF_RANGE", ALT_OUT_OF_RANGE, "Altitude values are out of nominal data range");
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
            final String variableName = getRadianceBandName(i);
            if (!input.containsBand(variableName)) {
                throw new OperatorException("Input variable '" + variableName + "' missing.");
            }
        }
    }

    // package access for testing only tb 2021-04-13
    static String getRadianceBandName(int i) {
        return "Oa" + String.format("%02d", i) + "_radiance";
    }

    private static void checkSimpleIndexedBands(Product input, String prefix, int lower, int upper) {
        for (int i = lower; i <= upper; i++) {
            final String variableName = prefix + Integer.toString(i);
            if (!input.containsBand(variableName)) {
                throw new OperatorException("Input variable '" + variableName + "' missing.");
            }
        }
    }

    static void processAltitudeOutlierPixel(Tile targetTile, Tile altitudeTile, int y, int x) {
        final float altitude = altitudeTile.getSampleFloat(x, y);
        if (altitude >= ALTITUDE_MAX || altitude <= ALTITUDE_MIN) {
            final int flagValue = targetTile.getSampleInt(x, y);

            final int flaggedValue = setOutOfRangeFlag(flagValue);
            targetTile.setSample(x, y, flaggedValue);
        }
    }

    static int setOutOfRangeFlag(int flagValue) {
        return flagValue | ALT_OUT_OF_RANGE;
    }

    static int setAnomalMeasureFlag(int flagValue) {
        return flagValue | ANOM_SPECTRAL_MEASURE;
    }

    // package access for testing only tb 20201-04-14
    static double getInvCosSza(double sza) {
        final double szaRad = Math.toRadians(sza);
        final double cosSza = Math.cos(szaRad);
        return 1.0 / cosSza;
    }

    // package access for testing only tb 2021-04-14
    static SlopeIndex getMaxSlope(double[] reflectances, double[] wavelengths) {
        final int numSlopes = reflectances.length - 1;
        double maxSlope = 0.0;
        byte index = -1;
        for (int i = 0; i < numSlopes; i++) {
            final double wlDelta = wavelengths[i + 1] - wavelengths[i];
            final double invWlDelta = 1.0 / wlDelta;
            final double reflectanceDelta = reflectances[i + 1] - reflectances[i];
            final double slope = reflectanceDelta * invWlDelta;
            if (Math.abs(slope) > Math.abs(maxSlope)) {
                maxSlope = slope;
                index = (byte) i;
            }
        }

        final SlopeIndex slopeIndex = new SlopeIndex();
        slopeIndex.slope = maxSlope;
        slopeIndex.slopeIndex = index;
        return slopeIndex;
    }

    @Override
    public void initialize() throws OperatorException {
        validateInputProduct(l1bProduct);

        targetProduct = createOutputProduct(l1bProduct, writeSlopeInformation);
        setTargetProduct(targetProduct);
    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {
        final Tile[] radianceTiles = new Tile[bandIndices.length];
        final Tile[] solarFluxTiles = new Tile[bandIndices.length];
        final Tile[] lambdaTiles = new Tile[bandIndices.length];

        Tile slopeTile = null;
        Tile slopeIndexTile = null;
        if (writeSlopeInformation) {
            Product targetProduct = getTargetProduct();
            final Band slopeBand = targetProduct.getBand("max_spectral_slope");
            slopeTile = targetTiles.get(slopeBand);

            final Band slopeIndexBand = targetProduct.getBand("max_slope_band_index");
            slopeIndexTile = targetTiles.get(slopeIndexBand);
        }

        // processRadiometricSaturation
        // - load relevant data
        for (int i = 0; i < bandIndices.length; i++) {
            final Band radianceBand = l1bProduct.getBand(getRadianceBandName(bandIndices[i]));
            radianceTiles[i] = getSourceTile(radianceBand, targetRectangle);

            final int bandIndex = i + 1;
            final Band solarFluxBand = l1bProduct.getBand("solar_flux_band_" + Integer.toString(bandIndex));
            solarFluxTiles[i] = getSourceTile(solarFluxBand, targetRectangle);

            final Band lambdaBand = l1bProduct.getBand("lambda0_band_" + Integer.toString(bandIndex));
            lambdaTiles[i] = getSourceTile(lambdaBand, targetRectangle);
        }

        final TiePointGrid szaGrid = l1bProduct.getTiePointGrid("SZA");
        final Tile szaTile = getSourceTile(szaGrid, targetRectangle);

        final double[] reflectances = new double[bandIndices.length];
        final double[] solarFluxes = new double[bandIndices.length];
        final double[] wavelengths = new double[bandIndices.length];

        final Band anomalyFlags = targetProduct.getBand("anomaly_flags");
        final Tile anomalyFlagsTile = targetTiles.get(anomalyFlags);

        for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
            checkForCancellation();

            for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {
                // read vector data for complete spectrum
                for (int i = 0; i < bandIndices.length; i++) {
                    reflectances[i] = radianceTiles[i].getSampleDouble(x, y);
                    solarFluxes[i] = solarFluxTiles[i].getSampleDouble(x, y);
                    wavelengths[i] = lambdaTiles[i].getSampleDouble(x, y);
                }

                final double invCosSza = getInvCosSza(szaTile.getSampleDouble(x, y));

                toReflectance(reflectances, solarFluxes, invCosSza);
                // - calculate slope / processSlope of all Band-combinations
                final SlopeIndex slopeIndex = getMaxSlope(reflectances, wavelengths);

                //
                // - compare with threshold (and set flag)
                if (slopeIndex.slope > 0.8) {

                }
                if (writeSlopeInformation) {
                    slopeTile.setSample(x, y, slopeIndex.slope);
                    // @todo 1 tb check for valid range 2021-04-14
                    final byte bandIndex = (byte) bandIndices[slopeIndex.slopeIndex];
                    slopeIndexTile.setSample(x, y, bandIndex);
                }
            }
        }

        processAltitudeOutliers(anomalyFlagsTile, targetRectangle);
    }

    @Override
    // @todo 1 tb/tb add test 2021-04-19
    public boolean canComputeTile() {
        return false;
    }

    @Override
    // @todo 1 tb/tb add test 2021-04-19
    public boolean canComputeTileStack() {
        return true;
    }

    private void processAltitudeOutliers(Tile targetTile, Rectangle targetRectangle) {
        final Band altitudeBand = l1bProduct.getBand("altitude");
        final Tile altitudeTile = getSourceTile(altitudeBand, targetRectangle);
        for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
            checkForCancellation();

            for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {
                processAltitudeOutlierPixel(targetTile, altitudeTile, y, x);
            }
        }
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(OlciAnomalyFlaggingOp.class);
        }
    }

    static class SlopeIndex {
        double slope;
        byte slopeIndex;
    }
}
