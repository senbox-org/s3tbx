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

import org.esa.s3tbx.dataio.s3.AbstractProductFactory;
import org.esa.s3tbx.dataio.s3.Manifest;
import org.esa.s3tbx.dataio.s3.Sentinel3ProductReader;
import org.esa.s3tbx.dataio.s3.util.S3NetcdfReader;
import org.esa.snap.core.dataio.geocoding.*;
import org.esa.snap.core.dataio.geocoding.forward.PixelForward;
import org.esa.snap.core.dataio.geocoding.inverse.PixelQuadTreeInverse;
import org.esa.snap.core.dataio.geocoding.util.RasterUtils;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.runtime.Config;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.prefs.Preferences;

public class SynLevel2ProductFactory extends AbstractProductFactory {

    // TODO - time  data are provided on a different grid, so we currently don't use them
    private static final String[] excludedIDs = new String[]{"time_Data", "tiepoints_olci_Data",
            "tiepoints_slstr_n_Data", "tiepoints_slstr_o_Data", "tiepoints_meteo_Data"};

    private static final double RESOLUTION_IN_KM = 0.3;
    private final static String SYSPROP_SYN_L2_PIXEL_GEO_CODING_FORWARD = "s3tbx.reader.syn.l2.pixelGeoCoding.forward";
    private final static String SYSPROP_SYN_L2_PIXEL_GEO_CODING_INVERSE = "s3tbx.reader.syn.l2.pixelGeoCoding.inverse";

    public SynLevel2ProductFactory(Sentinel3ProductReader productReader) {
        super(productReader);
    }

    @Override
    protected List<String> getFileNames(Manifest manifest) {
        return manifest.getFileNames(excludedIDs);
    }

    @Override
    protected void addSpecialVariables(Product masterProduct, Product targetProduct) throws IOException {
        // disabled due to poor performance
//        final double[] olcTpLon;
//        final double[] olcTpLat;
//        final NcFile olcTiePoints = openNcFile("tiepoints_olci.nc");
//        try {
//            olcTpLon = olcTiePoints.read("OLC_TP_lon");
//            olcTpLat = olcTiePoints.read("OLC_TP_lat");
//        } finally {
//            olcTiePoints.close();
//        }
//        addVariables(targetProduct, olcTpLon, olcTpLat, "tiepoints_olci.nc");
//        addVariables(targetProduct, olcTpLon, olcTpLat, "tiepoints_meteo.nc");
//
//        final double[] slnTpLon;
//        final double[] slnTpLat;
//        final NcFile slnTiePoints = openNcFile("tiepoints_slstr_n.nc");
//        try {
//            slnTpLon = slnTiePoints.read("SLN_TP_lon");
//            slnTpLat = slnTiePoints.read("SLN_TP_lat");
//        } finally {
//            slnTiePoints.close();
//        }
//        addVariables(targetProduct, slnTpLon, slnTpLat, "tiepoints_slstr_n.nc");
//
//        final double[] sloTpLon;
//        final double[] sloTpLat;
//        final NcFile sloTiePoints = openNcFile("tiepoints_slstr_o.nc");
//        try {
//            sloTpLon = sloTiePoints.read("SLO_TP_lon");
//            sloTpLat = sloTiePoints.read("SLO_TP_lat");
//        } finally {
//            sloTiePoints.close();
//        }
//        addVariables(targetProduct, sloTpLon, sloTpLat, "tiepoints_slstr_o.nc");
    }

//    private void addVariables(Product targetProduct, double[] tpLon, double[] tpLat, String fileName) throws
//            IOException {
//        final String latBandName = "lat";
//        final String lonBandName = "lon";
//        final Band latBand = targetProduct.getBand(latBandName);
//        final Band lonBand = targetProduct.getBand(lonBandName);
//
//        final NcFile ncFile = openNcFile(fileName);
//        try {
//            final List<Variable> variables = ncFile.getVariables(".*");
//            for (final Variable variable : variables) {
//                final String targetBandName = variable.getFullName();
//                final Band targetBand = targetProduct.addBand(targetBandName, ProductData.TYPE_FLOAT32);
//                targetBand.setDescription(variable.getDescription());
//                targetBand.setUnit(variable.getUnitsString());
//                final double[] tpVar = ncFile.read(variable.getFullName());
//                final MultiLevelImage targetImage = createTiePointImage(lonBand.getGeophysicalImage(),
//                        latBand.getGeophysicalImage(),
//                        tpLon,
//                        tpLat, tpVar,
//                        400);
//
//                targetBand.setSourceImage(targetImage);
//            }
//        } finally {
//            ncFile.close();
//        }
//    }

//    private NcFile openNcFile(String fileName) throws IOException {
//        return NcFile.open(new File(getInputFileParentDirectory(), fileName));
//    }
//
//    private MultiLevelImage createTiePointImage(MultiLevelImage lonImage,
//                                                MultiLevelImage latImage,
//                                                double[] tpLonData,
//                                                double[] tpLatData,
//                                                double[] tpFunctionData, int colCount) {
//        final LonLatFunction function = new LonLatTiePointFunction(tpLonData,
//                tpLatData,
//                tpFunctionData, colCount);
//        return new DefaultMultiLevelImage(
//                LonLatMultiLevelSource.create(lonImage, latImage, function, DataBuffer.TYPE_FLOAT));
//    }

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
    protected void setGeoCoding(Product targetProduct) throws IOException {
        final String lonVarName = "lon";
        final String latVarName = "lat";
        final Band lonBand = targetProduct.getBand(lonVarName);
        final Band latBand = targetProduct.getBand(latVarName);
        if (lonBand == null || latBand == null) {
            return;
        }

        final double[] longitudes = RasterUtils.loadDataScaled(lonBand);
        lonBand.unloadRasterData();
        final double[] latitudes = RasterUtils.loadDataScaled(latBand);
        latBand.unloadRasterData();

        final int width = targetProduct.getSceneRasterWidth();
        final int height = targetProduct.getSceneRasterHeight();
        final GeoRaster geoRaster = new GeoRaster(longitudes, latitudes, lonVarName, latVarName,
                                                  width, height, RESOLUTION_IN_KM);

        final Preferences preferences = Config.instance("s3tbx").preferences();
        final String fwdKey = preferences.get(SYSPROP_SYN_L2_PIXEL_GEO_CODING_FORWARD, PixelForward.KEY);
        final String invKey = preferences.get(SYSPROP_SYN_L2_PIXEL_GEO_CODING_INVERSE, PixelQuadTreeInverse.KEY);
        final ForwardCoding forward = ComponentFactory.getForward(fwdKey);
        final InverseCoding inverse = ComponentFactory.getInverse(invKey);

        final ComponentGeoCoding geoCoding = new ComponentGeoCoding(geoRaster, forward, inverse, GeoChecks.ANTIMERIDIAN);
        geoCoding.initialize();

        targetProduct.setSceneGeoCoding(geoCoding);
    }

    @Override
    protected void setAutoGrouping(Product[] sourceProducts, Product targetProduct) {
        // todo set other autogrouping when tie points are read in again
        // targetProduct.setAutoGrouping("SDR:SDR*err:OLC:SLN:SLO");
        targetProduct.setAutoGrouping("SDR:SDR*err");
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
