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
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;

import java.awt.*;
import java.util.Map;

@SuppressWarnings("ConstantConditions")
@OperatorMetadata(alias = "OlciAnomalyFlagging",
        version = "1.0",
        authors = "T. Block",
        category = "Optical/Preprocessing/Masking",
        copyright = "Copyright (C) 2021 by Brockmann Consult",
        description = "Adds a flagging band indicating saturated pixels and altitude data overflows")
public class OlciAnomalyFlaggingOp extends Operator {

    private static final String SUFFIX = "_ANOM";
    private static final float ALTITUDE_MAX = 8850.f;
    private static final float ALTITUDE_MIN = -11050.f;
    private static final int ANOM_SPECTRAL_MEASURE_VALUE = 1;
    private static final String ANOM_SPECTRAL_MEASURE_NAME = "ANOM_SPECTRAL_MEASURE";
    private static final String ANOM_SPECTRAL_MEASURE_DESCRIPTION = "Anomalous spectral sample due to saturation of one microband (partial saturation) or all microbands (saturated band)";
    private static final int PARTIALLY_SATURATED_VALUE = 2;
    private static final String PARTIALLY_SATURATED_NAME = "PARTIALLY_SATURATED";
    private static final String PARTIALLY_SATURATED_DESCIPTION = "Anomalous spectral sample and no saturation flag in spectral bands";
    private static final int ALT_OUT_OF_RANGE_VALUE = 4;
    private static final String ALT_OUT_OF_RANGE_NAME = "ALT_OUT_OF_RANGE";
    private static final String ALT_OUT_OF_RANGE_DESCRIPTION = "Altitude values are out of nominal data range";
    private static final int INPUT_DATA_INVALID_VALUE = 8;
    private static final String INPUT_DATA_INVALID_NAME = "INPUT_DATA_INVALID";
    private static final String INPUT_DATA_INVALID_DESCRIPTION = "Input data to detection algorithms is out of range/invalid";
    private static final int[] bandIndices = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 16, 17, 18, 21};
    private static final double SLOPE_THRESHOLD = 0.15;


    @SourceProduct(description = "OLCI L1b or fully compatible product.",
            label = "OLCI L1b product")
    private Product l1bProduct;

    @TargetProduct
    private Product targetProduct;

    @Parameter(defaultValue = "false",
            description = "If set to true, the operator adds two bands containing the maximal spectral slope and the band index where the peak is observed.",
            label = "Write spectral slope information")
    private boolean writeSlopeInformation;

    private int[] saturationFlags;

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
        final int width = input.getSceneRasterWidth();
        final int height = input.getSceneRasterHeight();

        final Product outputProduct = new Product(inputName + SUFFIX,
                inputProductType + SUFFIX,
                width,
                height);

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
        ProductUtils.copyMasks(input, outputProduct);

        Product.AutoGrouping autoGrouping = input.getAutoGrouping();
        outputProduct.setAutoGrouping(autoGrouping);

        final Band anomalyFlags = outputProduct.addBand("anomaly_flags", ProductData.TYPE_INT8);
        anomalyFlags.setDescription("Flags indicating OLCI data anomalies");

        final FlagCoding flagCoding = new FlagCoding("anomaly_flags");
        flagCoding.addFlag(ANOM_SPECTRAL_MEASURE_NAME, ANOM_SPECTRAL_MEASURE_VALUE, ANOM_SPECTRAL_MEASURE_DESCRIPTION);
        flagCoding.addFlag(PARTIALLY_SATURATED_NAME, PARTIALLY_SATURATED_VALUE, PARTIALLY_SATURATED_DESCIPTION);
        flagCoding.addFlag(ALT_OUT_OF_RANGE_NAME, ALT_OUT_OF_RANGE_VALUE, ALT_OUT_OF_RANGE_DESCRIPTION);
        flagCoding.addFlag(INPUT_DATA_INVALID_NAME, INPUT_DATA_INVALID_VALUE, INPUT_DATA_INVALID_DESCRIPTION);
        anomalyFlags.setSampleCoding(flagCoding);
        outputProduct.getFlagCodingGroup().add(flagCoding);

        outputProduct.addMask(Mask.BandMathsType.create("anomaly_flags_anom_spectral_measure", ANOM_SPECTRAL_MEASURE_DESCRIPTION,
                width, height, "anomaly_flags." + ANOM_SPECTRAL_MEASURE_NAME, Color.RED, 0.5));

        outputProduct.addMask(Mask.BandMathsType.create("anomaly_flags_partially_saturated", PARTIALLY_SATURATED_DESCIPTION,
                width, height, "anomaly_flags." + PARTIALLY_SATURATED_NAME, Color.ORANGE, 0.5));

        outputProduct.addMask(Mask.BandMathsType.create("anomaly_flags_altitude_out_of_range", ALT_OUT_OF_RANGE_DESCRIPTION,
                width, height, "anomaly_flags." + ALT_OUT_OF_RANGE_NAME, Color.MAGENTA, 0.5));

        outputProduct.addMask(Mask.BandMathsType.create("anomaly_flags_input_data_invalid", INPUT_DATA_INVALID_DESCRIPTION,
                width, height, "anomaly_flags." + INPUT_DATA_INVALID_NAME, Color.PINK, 0.5));

        if (writeSlopeInformation) {
            final Band maxSpectralSlope = outputProduct.addBand("max_spectral_slope", ProductData.TYPE_FLOAT32);
            maxSpectralSlope.setNoDataValue(Float.NaN);
            maxSpectralSlope.setNoDataValueUsed(true);
            maxSpectralSlope.setUnit("1/nm");
            maxSpectralSlope.setDescription("Value of maximal spectral slope for bands 1-12, 16-18, 21 in [reflectance/nm]");

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
            final String variableName = prefix + i;
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
        return flagValue | ALT_OUT_OF_RANGE_VALUE;
    }

    static int setAnomalMeasureFlag(int flagValue) {
        return flagValue | ANOM_SPECTRAL_MEASURE_VALUE;
    }

    static int setPartiallySaturatedFlag(int flagValue) {
        return flagValue | PARTIALLY_SATURATED_VALUE;
    }

    static int setInvalidInputFlag(int flagValue) {
        return flagValue | INPUT_DATA_INVALID_VALUE;
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
            final double reflectanceDelta = reflectances[i + 1] - reflectances[i];
            final double slope = reflectanceDelta / wlDelta;
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

    public static boolean isFillValue(double value, double fillValue) {
        if (Double.isNaN(fillValue)) {
            return Double.isNaN(value);
        } else {
            return Math.abs(value - fillValue) < 1e-8;
        }
    }

    public static boolean isFillValue(double[] values, double fillValue) {
        for (final double value : values) {
            if (isFillValue(value, fillValue)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void initialize() throws OperatorException {
        validateInputProduct(l1bProduct);

        targetProduct = createOutputProduct(l1bProduct, writeSlopeInformation);
        setTargetProduct(targetProduct);

        final ProductNodeGroup<FlagCoding> flagCodingGroup = l1bProduct.getFlagCodingGroup();
        final FlagCoding quality_flags = flagCodingGroup.get("quality_flags");
        saturationFlags = new int[21];
        for (int i = 1; i <= 21; i++) {
            MetadataAttribute flag = quality_flags.getFlag("saturated_Oa" + String.format("%02d", i));
            saturationFlags[i - 1] = flag.getData().getElemInt();
        }
    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {
        final Band anomalyFlags = targetProduct.getBand("anomaly_flags");
        final Tile anomalyFlagsTile = targetTiles.get(anomalyFlags);

        processSlopeDetection(targetTiles, targetRectangle, anomalyFlagsTile);
        processAltitudeOutliers(anomalyFlagsTile, targetRectangle);
        processPartiallySaturatedFlag(anomalyFlagsTile, targetRectangle);
    }

    private void processSlopeDetection(Map<Band, Tile> targetTiles, Rectangle targetRectangle, Tile anomalyFlagsTile) {
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

        // Load relevant data
        for (int i = 0; i < bandIndices.length; i++) {
            final Band radianceBand = l1bProduct.getBand(getRadianceBandName(bandIndices[i]));
            radianceTiles[i] = getSourceTile(radianceBand, targetRectangle);

            final int bandIndex = i + 1;
            final Band solarFluxBand = l1bProduct.getBand("solar_flux_band_" + bandIndex);
            solarFluxTiles[i] = getSourceTile(solarFluxBand, targetRectangle);

            final Band lambdaBand = l1bProduct.getBand("lambda0_band_" + bandIndex);
            lambdaTiles[i] = getSourceTile(lambdaBand, targetRectangle);
        }

        // load fill values
        final Band radianceBand = l1bProduct.getBand(getRadianceBandName(bandIndices[0]));
        final double radianceFillValue = radianceBand.getGeophysicalNoDataValue();

        final Band solarFluxBand = l1bProduct.getBand("solar_flux_band_1");
        final double solarFluxFillValue = solarFluxBand.getGeophysicalNoDataValue();

        final Band lambdaBand = l1bProduct.getBand("lambda0_band_1");
        final double wavelengthFillValue = lambdaBand.getGeophysicalNoDataValue();

        final TiePointGrid szaGrid = l1bProduct.getTiePointGrid("SZA");
        final double szaFillValue = szaGrid.getGeophysicalNoDataValue();
        final Tile szaTile = getSourceTile(szaGrid, targetRectangle);

        // allocate spectrum data vectors
        final double[] reflectances = new double[bandIndices.length];
        final double[] solarFluxes = new double[bandIndices.length];
        final double[] wavelengths = new double[bandIndices.length];

        for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
            checkForCancellation();

            for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {
                // read vector data for complete spectrum
                for (int i = 0; i < bandIndices.length; i++) {
                    reflectances[i] = radianceTiles[i].getSampleDouble(x, y);
                    solarFluxes[i] = solarFluxTiles[i].getSampleDouble(x, y);
                    wavelengths[i] = lambdaTiles[i].getSampleDouble(x, y);
                }
                final double sza = szaTile.getSampleDouble(x, y);

                boolean hasFillValue = checkFillValues(reflectances, radianceFillValue, solarFluxes, solarFluxFillValue,
                        wavelengths, wavelengthFillValue, szaFillValue, sza);

                if (hasFillValue) {
                    handleFillValue(anomalyFlagsTile, slopeTile, slopeIndexTile, y, x);
                    continue;
                }

                final double invCosSza = getInvCosSza(sza);

                toReflectance(reflectances, solarFluxes, invCosSza);
                // - calculate slope / processSlope of all Band-combinations
                final SlopeIndex slopeIndex = getMaxSlope(reflectances, wavelengths);

                // compare with threshold (and set flag)
                if (Math.abs(slopeIndex.slope) > SLOPE_THRESHOLD) {
                    final int flagValue = anomalyFlagsTile.getSampleInt(x, y);

                    final int flaggedValue = setAnomalMeasureFlag(flagValue);
                    anomalyFlagsTile.setSample(x, y, flaggedValue);
                }

                if (writeSlopeInformation) {
                    slopeTile.setSample(x, y, slopeIndex.slope);

                    final byte bandIndex = (byte) bandIndices[slopeIndex.slopeIndex];
                    slopeIndexTile.setSample(x, y, bandIndex);
                }
            }
        }
    }

    private void handleFillValue(Tile anomalyFlagsTile, Tile slopeTile, Tile slopeIndexTile, int y, int x) {
        final int flagValue = anomalyFlagsTile.getSampleInt(x, y);

        final int flaggedValue = setInvalidInputFlag(flagValue);
        anomalyFlagsTile.setSample(x, y, flaggedValue);
        if (writeSlopeInformation) {
            slopeTile.setSample(x, y, Float.NaN);
            slopeIndexTile.setSample(x, y, -1);
        }
    }

    // package access for testing only tb 2021-04-20
    static boolean checkFillValues(double[] radiances, double radianceFillValue, double[] solarFluxes, double solarFluxFillValue,
                                   double[] wavelengths, double wavelengthFillValue, double szaFillValue, double sza) {
        boolean hasFillValue = isFillValue(sza, szaFillValue);
        hasFillValue |= isFillValue(radiances, radianceFillValue);
        hasFillValue |= isFillValue(solarFluxes, solarFluxFillValue);
        hasFillValue |= isFillValue(wavelengths, wavelengthFillValue);
        return hasFillValue;
    }

    @Override
    public boolean canComputeTile() {
        return false;
    }

    @Override
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

    private void processPartiallySaturatedFlag(Tile anomalyFlagsTile, Rectangle targetRectangle) {
        final Band l1bFlagsBand = l1bProduct.getBand("quality_flags");
        final Tile l1bFlagsTile = getSourceTile(l1bFlagsBand, targetRectangle);
        for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
            checkForCancellation();
            for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {
                int anomalyFlag = anomalyFlagsTile.getSampleInt(x, y);
                if ((anomalyFlag & ANOM_SPECTRAL_MEASURE_VALUE) == ANOM_SPECTRAL_MEASURE_VALUE) {
                    final int l1bFlags = l1bFlagsTile.getSampleInt(x, y);
                    if (!hasSaturation(l1bFlags, saturationFlags)) {
                        anomalyFlag = setPartiallySaturatedFlag(anomalyFlag);
                    }
                }
                anomalyFlagsTile.setSample(x, y, anomalyFlag);
            }
        }
    }

    // package access for testing only tb 2021-07-12
    static boolean hasSaturation(int l1bFlags, int[] saturationFlagValues) {
        for (int saturationFlagValue : saturationFlagValues) {
            if ((l1bFlags & saturationFlagValue) == saturationFlagValue) {
                return true;
            }
        }
        return false;
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
