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
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
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

    @SourceProduct(description = "OLCI L1b or fully compatible product.",
            label = "OLCI L1b product")
    private Product l1bProduct;

    @TargetProduct
    private Product targetProduct;

    private float[][] cameraGains;

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
        if (!metadataRoot.containsElement("lambda0")) {
            throw new OperatorException("Metadata element 'lambda0' missing.");
        }
    }

    static Product createOutputProduct(Product input) {
        final String inputProductType = input.getProductType();
        final String inputName = input.getName();
        final Product outputProduct = new Product(inputName + SUFFIX,
                                                  inputProductType + SUFFIX,
                                                  input.getSceneRasterWidth(),
                                                  input.getSceneRasterHeight());

        outputProduct.setDescription("OLCI sensor harmonized L1b");
        outputProduct.setStartTime(input.getStartTime());
        outputProduct.setEndTime(input.getEndTime());

        // @todo 2 tb/tb which metadata to copy over? 2021-01-21

        for (int i = 1; i < 22; i++) {
            final String bandName = "Oa" + String.format("%02d", i) + "_radiance";
            final Band sourceBand = input.getBand(bandName);

            final Band band = outputProduct.addBand(bandName + SUFFIX, ProductData.TYPE_FLOAT32);
            band.setNoDataValue(Float.NaN);
            band.setNoDataValueUsed(true);

            band.setUnit(sourceBand.getUnit());
            band.setSpectralWavelength(sourceBand.getSpectralWavelength());
            band.setSpectralBandwidth(sourceBand.getSpectralBandwidth());
        }

        ProductUtils.copyTiePointGrids(input, outputProduct);
        ProductUtils.copyGeoCoding(input, outputProduct);

        return outputProduct;
    }

    static float[][] parseCameraGains(BufferedReader reader) throws IOException {
        final ArrayList<float[]> arrayList = new ArrayList<>();
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
            final float[] gains = new float[numTokens];
            for (int i = 0; i < numTokens; i++) {
                final String gainString = tokenizer.nextToken();
                gains[i] = Float.parseFloat(gainString.trim());
            }

            arrayList.add(gains);
        }

        final int numGainVectors = arrayList.size();
        final float[][] result = new float[numGainVectors][];
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

    @Override
    public void initialize() throws OperatorException {
        validateInputProduct(l1bProduct);

        targetProduct = createOutputProduct(l1bProduct);
        setTargetProduct(targetProduct);

        loadCameraGains();
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
        // get source tile detector index

        for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
            checkForCancellation();

            for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {

                // detect camera
                // load factor
            }
        }
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(OlciSensorHarmonisationOp.class);
        }
    }
}
