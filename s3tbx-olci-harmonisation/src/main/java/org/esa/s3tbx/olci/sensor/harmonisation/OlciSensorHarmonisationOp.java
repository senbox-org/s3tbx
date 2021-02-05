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

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.StringTokenizer;

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

    private static final String SUFFIX = "_HARM";
    private static final int DETECTORS_PER_CAMERA = 740;
    private static final int NUM_DETECTORS = 3700;
    private static final int NUM_BANDS = 21;
    private static final String LAMBDA_0 = "lambda0";

    @SourceProduct(description = "OLCI L1b or fully compatible product.",
            label = "OLCI L1b product")
    private Product l1bProduct;

    @TargetProduct
    private Product targetProduct;

    @Parameter(defaultValue = "false",
            description = "If set to true, in addition to the camera homogenisation, sensor cross-calibration (i.e. S3A->S3B or S3B->S3A) is performed using linear regression",
            label = "Perform sensor cross-calibration")
    private boolean performSensorCrossCalibration;

    @Parameter(defaultValue = "false",
            description = "If set to true, all bands of the input product (except for the radiances) are copied to the target product. " +
                    "If set to false, only the tie-point rasters are copied",
            label = "Copy input bands")
    private boolean copyInputBands;

    private double[][] cameraGains;
    private int sensorIndex;    // 0 -> S3A, 1 -> S3B
    private float[][] detectorWavelengths;
    private DetectorRegression regression;

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

        final MetadataElement metadataRoot = input.getMetadataRoot();
        if (!metadataRoot.containsElement(LAMBDA_0)) {
            throw new OperatorException("Metadata element '" + LAMBDA_0 + "' missing.");
        }
    }

    static Product createOutputProduct(Product input, boolean copyInputBands) {
        final String inputProductType = input.getProductType();
        final String inputName = input.getName();
        final Product outputProduct = new Product(inputName + SUFFIX,
                                                  inputProductType + SUFFIX,
                                                  input.getSceneRasterWidth(),
                                                  input.getSceneRasterHeight());

        outputProduct.setDescription("OLCI sensor harmonised L1b");
        outputProduct.setStartTime(input.getStartTime());
        outputProduct.setEndTime(input.getEndTime());

        for (int i = 0; i < NUM_BANDS; i++) {
            final String bandName = "Oa" + String.format("%02d", i + 1) + "_radiance";
            final Band sourceBand = input.getBand(bandName);

            final Band band = outputProduct.addBand(bandName + SUFFIX, ProductData.TYPE_FLOAT32);
            band.setNoDataValue(Float.NaN);
            band.setNoDataValueUsed(true);

            band.setUnit(sourceBand.getUnit());
            band.setSpectralWavelength(sourceBand.getSpectralWavelength());
            band.setSpectralBandwidth(sourceBand.getSpectralBandwidth());
            band.setDescription(sourceBand.getDescription() + " harmonised");
        }

        if (copyInputBands) {
            final Band[] inputBands = input.getBands();
            for (final Band inputBand : inputBands) {
                final String inputBandName = inputBand.getName();
                if (inputBandName.contains("_radiance") ||
                        inputBandName.contains("lambda") ||
                        inputBandName.contains("FWHM") ||
                        inputBandName.contains("solar") ||
                        inputBandName.contains("frame")) {
                    // skip original radiances and metadata exploded to full raster size tb 2021-02-03
                    continue;
                }

                ProductUtils.copyBand(inputBandName, input, outputProduct, true);
            }

            ProductUtils.copyFlagCodings(input, outputProduct);
        }

        ProductUtils.copyTiePointGrids(input, outputProduct);
        ProductUtils.copyGeoCoding(input, outputProduct);
        ProductUtils.copyMetadata(input, outputProduct);

        return outputProduct;
    }

    static double[][] parseCameraGains(BufferedReader reader) throws IOException {
        final ArrayList<double[]> arrayList = new ArrayList<>();
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            line = line.trim();
            if (line.startsWith("#")) {
                continue;
            }

            final StringTokenizer tokenizer = new StringTokenizer(line, ",", false);
            final int numTokens = tokenizer.countTokens();
            final double[] gains = new double[numTokens];
            for (int i = 0; i < numTokens; i++) {
                final String gainString = tokenizer.nextToken();
                gains[i] = Double.parseDouble(gainString.trim());
            }

            arrayList.add(gains);
        }

        final int numGainVectors = arrayList.size();
        final double[][] result = new double[numGainVectors][];
        for (int i = 0; i < numGainVectors; i++) {
            result[i] = arrayList.get(i);
        }
        return result;
    }

    static int getCameraIndex(int detector) {
        if (detector < 0 || detector >= NUM_DETECTORS) {
            return -1;
        }
        return detector / DETECTORS_PER_CAMERA;
    }

    static int getSensorIndex(String productName) {
        final String upperCaseName = productName.toUpperCase();
        if (upperCaseName.contains("S3A_OL")) {
            return 0;
        } else if (upperCaseName.contains("S3B_OL")) {
            return 1;
        } else if (upperCaseName.contains("S3C_OL")) {
            return 2;
        } else if (upperCaseName.contains("S3D_OL")) {
            return 3;
        }

        throw new OperatorException("Invalid input product type: " + productName);
    }

    static String getSourceBandName(String targetBandName) {
        final int lastIndex = targetBandName.indexOf(SUFFIX);
        if (lastIndex < 0) {
            throw new OperatorException("Invalid band name: " + targetBandName);
        }
        return targetBandName.substring(0, lastIndex);
    }

    static float[][] loadDetectorWavelengths(Product l1bProduct) {
        final MetadataElement metadataRoot = l1bProduct.getMetadataRoot();
        final MetadataElement lambdaElement = metadataRoot.getElement(LAMBDA_0);

        final float[][] waveLengths = new float[NUM_BANDS][];

        for (int i = 0; i < NUM_BANDS; i++) {
            final MetadataElement waveLengthElement = lambdaElement.getElement("Central wavelengths for band " + (i + 1));
            final MetadataAttribute centralWavelengthAttribute = waveLengthElement.getAttribute("Central wavelength");
            final ProductData wavelengthData = centralWavelengthAttribute.getData();
            final int numElems = wavelengthData.getNumElems();

            waveLengths[i] = new float[numElems];
            for (int k = 0; k < numElems; k++) {
                waveLengths[i][k] = wavelengthData.getElemFloatAt(k);
            }
        }

        return waveLengths;
    }

    static int getBandIndex(String bandName) {
        final String numberString = bandName.substring(2, 4);
        return Integer.parseInt(numberString) - 1;
    }

    @Override
    public void initialize() throws OperatorException {
        validateInputProduct(l1bProduct);

        sensorIndex = getSensorIndex(l1bProduct.getName());
        if (performSensorCrossCalibration) {
            regression = DetectorRegression.get(sensorIndex);
        }

        targetProduct = createOutputProduct(l1bProduct, copyInputBands);
        setTargetProduct(targetProduct);

        loadCameraGains();
        detectorWavelengths = loadDetectorWavelengths(l1bProduct);
    }

    private void loadCameraGains() throws OperatorException {
        final InputStream inputStream = OlciSensorHarmonisationOp.class.getResourceAsStream("camera_gains.csv");
        if (inputStream == null) {
            throw new IllegalArgumentException("resource I/O error: resource not found: camera_gains.csv");
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            cameraGains = parseCameraGains(reader);

        } catch (IOException e) {
            throw new OperatorException(e.getMessage());
        }
    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        final Rectangle targetRectangle = targetTile.getRectangle();
        final String targetBandName = targetBand.getName();

        // get source tile for radiance
        final String sourceBandName = getSourceBandName(targetBandName);
        final Band radianceBand = l1bProduct.getBand(sourceBandName);
        final Tile radianceSourceTile = getSourceTile(radianceBand, targetRectangle);

        final Band detectorIndexBand = l1bProduct.getBand("detector_index");
        final Tile detectorIndexTile = getSourceTile(detectorIndexBand, targetRectangle);

        if (performSensorCrossCalibration) {
            processCrossHarmonisation(targetTile, targetRectangle, radianceSourceTile, detectorIndexTile);
        } else {
            processCameraFlattening(targetTile, targetRectangle, radianceSourceTile, detectorIndexTile);
        }
    }

    private void processCameraFlattening(Tile targetTile, Rectangle targetRectangle, Tile radianceSourceTile, Tile detectorIndexTile) {
        final double[] sensorCameraGains = cameraGains[sensorIndex];
        for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
            checkForCancellation();

            for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {
                // detect camera
                final int detectorIndex = detectorIndexTile.getSampleInt(x, y);
                final int cameraIndex = getCameraIndex(detectorIndex);
                if (cameraIndex == -1) {
                    targetTile.setSample(x, y, Float.NaN);
                    continue;
                }

                final double camGain = sensorCameraGains[cameraIndex];
                final double sourceRadiance = radianceSourceTile.getSampleDouble(x, y);

                final float targetRadiance = (float) (sourceRadiance * camGain);
                targetTile.setSample(x, y, targetRadiance);
            }
        }
    }

    private void processCrossHarmonisation(Tile targetTile, Rectangle targetRectangle, Tile radianceSourceTile, Tile detectorIndexTile) {
        final double[] sensorCameraGains = cameraGains[sensorIndex];

        final int bandIndex = getBandIndex(targetTile.getRasterDataNode().getName());
        final float[] bandWavelengths = detectorWavelengths[bandIndex];

        for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
            checkForCancellation();

            for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {
                // detect camera
                final int detectorIndex = detectorIndexTile.getSampleInt(x, y);
                final int cameraIndex = getCameraIndex(detectorIndex);
                if (cameraIndex == -1) {
                    targetTile.setSample(x, y, Float.NaN);
                    continue;
                }

                final float detectorWavelength = bandWavelengths[detectorIndex];
                final double regFactor = regression.calculate(detectorWavelength);

                final double camGain = sensorCameraGains[cameraIndex];
                final double sourceRadiance = radianceSourceTile.getSampleDouble(x, y);

                final float targetRadiance = (float) (sourceRadiance * camGain * regFactor);

                targetTile.setSample(x, y, targetRadiance);
            }
        }
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(OlciSensorHarmonisationOp.class);
        }
    }
}
