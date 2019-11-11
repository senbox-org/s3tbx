package org.esa.s3tbx.dataio.s3.synergy;/*
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

import com.bc.ceres.glevel.MultiLevelImage;
import com.bc.ceres.glevel.support.DefaultMultiLevelImage;
import org.esa.s3tbx.dataio.s3.AbstractProductFactory;
import org.esa.s3tbx.dataio.s3.LonLatMultiLevelSource;
import org.esa.s3tbx.dataio.s3.Manifest;
import org.esa.s3tbx.dataio.s3.Sentinel3ProductReader;
import org.esa.s3tbx.dataio.s3.util.S3NetcdfReader;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoCodingFactory;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.RasterDataNode;
import ucar.nc2.Variable;

import java.awt.image.DataBuffer;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SynLevel2ProductFactory extends AbstractProductFactory {

    // TODO - time  data are provided on a different grid, so we currently don't use them
    private static final String[] excludedIDs = new String[]{"time_Data", "tiepoints_olci_Data",
            "tiepoints_slstr_n_Data", "tiepoints_slstr_o_Data", "tiepoints_meteo_Data"};

    public SynLevel2ProductFactory(Sentinel3ProductReader productReader) {
        super(productReader);
    }

    @Override
    protected List<String> getFileNames(Manifest manifest) {
        return manifest.getFileNames(excludedIDs);
    }

    @Override
    protected void addSpecialVariables(Product masterProduct, Product targetProduct) throws IOException {
        addTiepointVariables(targetProduct, new String[]{"tiepoints_olci.nc", "tiepoints_meteo.nc"});
        addTiepointVariables(targetProduct, new String[]{"tiepoints_slstr_n.nc"});
        addTiepointVariables(targetProduct, new String[]{"tiepoints_slstr_o.nc"});
    }

    private void addTiepointVariables(Product targetProduct, String[] fileNames) throws IOException {
        final String latBandName = "lat";
        final String lonBandName = "lon";
        final Band latBand = targetProduct.getBand(latBandName);
        final Band lonBand = targetProduct.getBand(lonBandName);
        int latIndex = -1;
        int lonIndex = -1;
        ArrayList<File> ncFileList = new ArrayList<>();
        List<Band> targetBands = new ArrayList<>();
        int offset = 0;
        for (String fileName : fileNames) {
            ncFileList.add(new File(getInputFileParentDirectory(), fileName));
            final NcFile ncFile = openNcFile(fileName);
            try {
                final List<Variable> variables = ncFile.getVariables(".*");
                for (int j = 0; j < variables.size(); j++) {
                    Variable variable = variables.get(j);
                    ncFileList.add(new File(getInputFileParentDirectory(), fileName));
                    if (variable.getFullName().contains("lat")) {
                        latIndex = offset + j;
                    } else if (variable.getFullName().contains("lon")) {
                        lonIndex = offset + j;
                    }
                    final String targetBandName = variable.getFullName();
                    final Band targetBand = targetProduct.addBand(targetBandName, ProductData.TYPE_FLOAT32);
                    targetBand.setDescription(variable.getDescription());
                    targetBand.setUnit(variable.getUnitsString());
                    targetBand.setNoDataValueUsed(true);
                    targetBand.setNoDataValue(Double.NaN);
                    targetBands.add(targetBand);
                }
                offset += variables.size();
            } finally {
                ncFile.close();
            }
        }
        LonLatTiePointFunctionSource source = new LonLatTiePointFunctionSource(ncFileList, latIndex, lonIndex);
        for (int i = 0; i < targetBands.size(); i++) {
            LonLatTiePointFunction function = new LonLatTiePointFunction(source, i);
            MultiLevelImage targetImage =
                    createTiePointImage(lonBand.getGeophysicalImage(), latBand.getGeophysicalImage(), function);
            targetBands.get(i).setSourceImage(targetImage);
        }
    }

    private NcFile openNcFile(String fileName) throws IOException {
        return NcFile.open(new File(getInputFileParentDirectory(), fileName));
    }

    private MultiLevelImage createTiePointImage(MultiLevelImage lonImage,
                                                MultiLevelImage latImage,
                                                LonLatTiePointFunction function
    ) {
        return new DefaultMultiLevelImage(
                LonLatMultiLevelSource.create(lonImage, latImage, function, DataBuffer.TYPE_FLOAT));
    }

    @Override
    protected void configureTargetNode(Band sourceBand, RasterDataNode targetNode) {
        //todo read spectral band information from metadata
        if (targetNode instanceof Band) {
            final MetadataElement variableAttributes = sourceBand.getProduct().getMetadataRoot().getElement(
                    "Variable_Attributes");
            if (variableAttributes != null) {
                final MetadataElement element = variableAttributes.getElement(sourceBand.getName());
                if (element != null) {
                    final MetadataAttribute wavelengthAttribute = element.getAttribute("wavelength");
                    final MetadataAttribute bandwidthAttribute = element.getAttribute("bandwidth");
                    final Band targetBand = (Band) targetNode;
                    if (wavelengthAttribute != null) {
                        targetBand.setSpectralWavelength(wavelengthAttribute.getData().getElemFloat());
                    }
                    if (bandwidthAttribute != null) {
                        targetBand.setSpectralBandwidth(bandwidthAttribute.getData().getElemFloat());
                    }
                }
            }
        }
    }

    @Override
    protected void setGeoCoding(Product targetProduct) {
        final String latBandName = "lat";
        final String lonBandName = "lon";
        final Band latBand = targetProduct.getBand(latBandName);
        final Band lonBand = targetProduct.getBand(lonBandName);
        targetProduct.setSceneGeoCoding(GeoCodingFactory.createPixelGeoCoding(latBand, lonBand, latBand.getValidMaskExpression(), 5));
    }

    @Override
    protected void setAutoGrouping(Product[] sourceProducts, Product targetProduct) {
        targetProduct.setAutoGrouping("SDR:SDR*err:OLC:SLN:SLO");
    }

    @Override
    protected Product readProduct(String fileName, Manifest manifest) throws IOException {
        final File file = new File(getInputFileParentDirectory(), fileName);
        if (!file.exists()) {
            return null;
        }
        final S3NetcdfReader reader = new S3NetcdfReader();
        return reader.readProductNodes(file, null);
    }

}
