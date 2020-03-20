/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.s3tbx.operator.cloud;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.jexp.ParseException;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.ResourceInstaller;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.dataio.envisat.EnvisatConstants;

import java.awt.Rectangle;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * The <code>CloudProcessor</code> implements all specific functionality to calculate a cloud probability.
 */
@SuppressWarnings({"UnusedDeclaration", "FieldCanBeLocal"})
@OperatorMetadata(alias = "CloudProb",
        category = "Optical/Preprocessing/Masking",
        version = "1.7",
        authors = "Rene Preusker (Algorithm), Tom Block (BEAM Implementation), Thomas Storm (GPF conversion)",
        copyright = "Copyright (C) 2004-2014 by ESA, FUB and Brockmann Consult",
        description = "Applies a clear sky conservative cloud detection algorithm.")
public class CloudOperator extends Operator {

    @SourceProduct(alias = "source", label = "Source product", description = "The MERIS Level 1b source product.")
    private Product l1bProduct;

    @TargetProduct(label = "Cloud product")
    private Product targetProduct;

    private CloudPN cloudNode;
    private Product tempCloudProduct;

    @Override
    public void initialize() {
        cloudNode = new CloudPN(getAuxdataInstallationPath().toString());
        try {
            initOutputProduct();
        } catch (IOException e) {
            throw new OperatorException("Unable to initialise output product.", e);
        }
    }

    @Override
    public void doExecute(ProgressMonitor pm) throws OperatorException {
        pm.beginTask("Preparing Computation", 3);
        try {
            pm.setSubTaskName("Installing auxiliary data");
            try {
                installAuxdata();
                pm.worked(1);
            } catch (IOException e) {
                throw new OperatorException("Failed to install auxiliary data: " + e.getMessage(), e);
            }
            pm.setSubTaskName("Set up cloud node");
            final Map<String, String> cloudConfig = new HashMap<>();
            cloudConfig.put(CloudPN.CONFIG_FILE_NAME, "cloud_config.txt");
            cloudConfig.put(CloudPN.INVALID_EXPRESSION, "l1_flags.INVALID");
            try {
                cloudNode.setUp(cloudConfig);
                pm.worked(1);
            } catch (IOException e) {
                throw new OperatorException("Initialise cloud source: " + e.getMessage(), e);
            }
            pm.setSubTaskName("Start processing cloud node");
            cloudNode.startProcessing();
            getLogger().info("Output product successfully initialised");
            pm.worked(1);
        } finally {
            pm.done();
        }
    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {
        for (Map.Entry<Band, Tile> entry : targetTiles.entrySet()) {
            checkForCancellation();
            Band targetBand = entry.getKey();
            Tile targetTile = entry.getValue();
            Band sourceBand = tempCloudProduct.getBand(targetBand.getName());
            Rectangle targetRect = targetTile.getRectangle();
            ProductData rawSamples = getSourceTile(sourceBand, targetRect).getRawSamples();
            targetTile.setRawSamples(rawSamples);
        }
    }

    private void initCloudNode() {
        cloudNode = new CloudPN(getAuxdataInstallationPath().toString());
    }

    // package local for testing purposes
    void installAuxdata() throws IOException {
        Path auxdataDirPath = getAuxdataInstallationPath();
        Path sourcePath = ResourceInstaller.findModuleCodeBasePath(getClass()).resolve("auxdata");
        new ResourceInstaller(sourcePath, auxdataDirPath).install(".*", ProgressMonitor.NULL);
    }

    // package local for testing purposes
    Path getAuxdataInstallationPath() {
        return SystemUtils.getAuxDataPath().resolve("meris/cloud-op").toAbsolutePath();
    }

    /**
     * Creates the output product skeleton.
     */
    private void initOutputProduct() throws IOException {
        if (!EnvisatConstants.MERIS_L1_TYPE_PATTERN.matcher(l1bProduct.getProductType()).matches()) {
            throw new OperatorException("Product type '" + l1bProduct.getProductType() + "' is not supported." +
                    "It must be a MERIS Level 1b product.");
        }
        tempCloudProduct = cloudNode.readProductNodes(l1bProduct, null);
        targetProduct = cloudNode.createTargetProductImpl();

        ProductUtils.copyFlagBands(l1bProduct, targetProduct, true);
        ProductUtils.copyTiePointGrids(l1bProduct, targetProduct);

        ProductUtils.copyGeoCoding(l1bProduct, targetProduct);
        ProductUtils.copyMetadata(l1bProduct, targetProduct);
        targetProduct.setStartTime(l1bProduct.getStartTime());
        targetProduct.setEndTime(l1bProduct.getEndTime());
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(CloudOperator.class);
        }

    }

}
