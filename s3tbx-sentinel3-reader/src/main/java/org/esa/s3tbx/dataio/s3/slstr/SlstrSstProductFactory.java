package org.esa.s3tbx.dataio.s3.slstr;/*
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

import com.bc.ceres.glevel.support.DefaultMultiLevelImage;
import com.bc.ceres.glevel.support.DefaultMultiLevelModel;
import com.bc.ceres.glevel.support.DefaultMultiLevelSource;
import org.esa.s3tbx.dataio.s3.Manifest;
import org.esa.s3tbx.dataio.s3.Sentinel3ProductReader;
import org.esa.snap.core.dataio.geocoding.ComponentFactory;
import org.esa.snap.core.dataio.geocoding.ComponentGeoCoding;
import org.esa.snap.core.dataio.geocoding.ForwardCoding;
import org.esa.snap.core.dataio.geocoding.GeoChecks;
import org.esa.snap.core.dataio.geocoding.GeoRaster;
import org.esa.snap.core.dataio.geocoding.InverseCoding;
import org.esa.snap.core.dataio.geocoding.forward.PixelForward;
import org.esa.snap.core.dataio.geocoding.inverse.PixelQuadTreeInverse;
import org.esa.snap.core.dataio.geocoding.util.RasterUtils;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.runtime.Config;
import org.geotools.referencing.operation.transform.AffineTransform2D;

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

public class SlstrSstProductFactory extends SlstrProductFactory {

    public final static String SLSTR_L2_SST_USE_PIXELGEOCODINGS = "s3tbx.reader.slstrl2sst.pixelGeoCodings";
    private final static String SLSTR_L2_SST_PIXEL_CODING_FORWARD = "s3tbx.reader.slstrl2sst.pixelGeoCodings.forward";
    private final static String SLSTR_L2_SST_PIXEL_CODING_INVERSE = "s3tbx.reader.slstrl2sst.pixelGeoCodings.inverse";

    private Map<String, GeoCoding> geoCodingMap;
    private Double nadirStartOffset;
    private Double nadirTrackOffset;
    private Double obliqueStartOffset;
    private Double obliqueTrackOffset;

    public SlstrSstProductFactory(Sentinel3ProductReader productReader) {
        super(productReader);
        geoCodingMap = new HashMap<>();
    }

    @Override
    protected List<String> getFileNames(Manifest manifest) {
        return manifest.getFileNames(new String[0]);
    }

    @Override
    protected void configureTargetNode(Band sourceBand, RasterDataNode targetNode) {
        final String sourceBandName = sourceBand.getName();
        if (sourceBand.getProduct().getName().contains("SST")) {
            targetNode.setName(sourceBand.getProduct().getName() + "_" + sourceBandName);
        }
    }

    @Override
    protected Product findMasterProduct() {
        final List<Product> productList = getOpenProductList();
        Product masterProduct = productList.get(0);
        for (int i = 1; i < productList.size(); i++) {
            Product product = productList.get(i);
            if (product.getSceneRasterWidth() > masterProduct.getSceneRasterWidth() &&
                    product.getSceneRasterHeight() > masterProduct.getSceneRasterHeight() &&
                    !product.getName().contains("flags")) {
                masterProduct = product;
            }
        }
        return masterProduct;
    }

    @Override
    protected void processProductSpecificMetadata(MetadataElement metadataElement) {
        MetadataElement slstrInformationElement = metadataElement.getElement("slstrProductInformation");
        for (int i = 0; i < slstrInformationElement.getNumElements(); i++) {
            MetadataElement slstrElement = slstrInformationElement.getElementAt(i);
            String slstrElementName = slstrElement.getName();
            if (slstrElementName.endsWith("ImageSize")) {
                MetadataAttribute startOffsetElem = slstrElement.getAttribute("startOffset");
                MetadataAttribute trackOffsetElem = slstrElement.getAttribute("trackOffset");
                Double startOffset = startOffsetElem != null ? Double.parseDouble(startOffsetElem.getData().getElemString()) : 0.0;
                Double trackOffset = trackOffsetElem != null ? Double.parseDouble(trackOffsetElem.getData().getElemString()) : 0.0;
                if (slstrElementName.equals("nadirImageSize")) {
                    nadirStartOffset = startOffset;
                    nadirTrackOffset = trackOffset;
                    setReferenceStartOffset(startOffset);
                    setReferenceTrackOffset(trackOffset);
                    setReferenceResolutions(getResolutions("in"));
                } else {
                    obliqueStartOffset = startOffset;
                    obliqueTrackOffset = trackOffset;
                }
            }
        }
    }

    protected Double getStartOffset(String gridIndex) {
        if (gridIndex.endsWith("o")) {
            return obliqueStartOffset;
        }
        return nadirStartOffset;
    }

    protected Double getTrackOffset(String gridIndex) {
        if (gridIndex.endsWith("o")) {
            return obliqueTrackOffset;
        }
        return nadirTrackOffset;
    }

    @Override
    protected RasterDataNode addSpecialNode(Product masterProduct, Band sourceBand, Product targetProduct) {
        final String sourceBandName = sourceBand.getName();
        final String sourceProductName = sourceBand.getProduct().getName();
        final String gridIndex = getGridIndex(sourceProductName);
        Double sourceStartOffset = getStartOffset(gridIndex);
        Double sourceTrackOffset = getTrackOffset(gridIndex);
        final short[] sourceResolutions = getResolutions(gridIndex);
        if (gridIndex.startsWith("t")) {
            final MetadataElement attributesElement =
                    sourceBand.getProduct().getMetadataRoot().getElement("Global_Attributes");
            //as these are not included in the manifest for tie point grids, we have to extract them from the product
            if (attributesElement != null) {
                final MetadataAttribute startOffsetAttribute = attributesElement.getAttribute("start_offset");
                if (startOffsetAttribute != null) {
                    sourceStartOffset = startOffsetAttribute.getData().getElemDouble();
                }
                final MetadataAttribute trackOffsetAttribute = attributesElement.getAttribute("track_offset");
                if (trackOffsetAttribute != null) {
                    sourceTrackOffset = trackOffsetAttribute.getData().getElemDouble();
                }
            }
        }
        if (sourceStartOffset == null || sourceTrackOffset == null) {
            return sourceBand;
        }
        if (gridIndex.startsWith("t")) {
            return copyTiePointGrid(sourceBand, targetProduct, sourceStartOffset, sourceTrackOffset, sourceResolutions);
        }
        final Band targetBand = new Band(sourceBandName, sourceBand.getDataType(),
                                         sourceBand.getRasterWidth(), sourceBand.getRasterHeight());
        targetProduct.addBand(targetBand);
        ProductUtils.copyRasterDataNodeProperties(sourceBand, targetBand);
        final RenderedImage sourceRenderedImage = sourceBand.getSourceImage().getImage(0);
        //todo remove commented lines when resampling works with scenetransforms
        //if pixel band geo-codings are used, scenetransforms are set
//                if (Config.instance("s3tbx").load().preferences().getBoolean(SLSTR_L2_SST_USE_PIXELGEOCODINGS, false)) {
//                    targetBand.setSourceImage(sourceRenderedImage);
        final AffineTransform imageToModelTransform = new AffineTransform();
        final float[] offsets = getOffsets(sourceStartOffset, sourceTrackOffset, sourceResolutions);
        imageToModelTransform.translate(offsets[0], offsets[1]);
        final short[] referenceResolutions = getReferenceResolutions();
        final int subSamplingX = sourceResolutions[0] / referenceResolutions[0];
        final int subSamplingY = sourceResolutions[1] / referenceResolutions[1];
        imageToModelTransform.scale(subSamplingX, subSamplingY);
        final DefaultMultiLevelModel targetModel =
                new DefaultMultiLevelModel(imageToModelTransform,
                                           sourceRenderedImage.getWidth(), sourceRenderedImage.getHeight());
        final DefaultMultiLevelSource targetMultiLevelSource =
                new DefaultMultiLevelSource(sourceRenderedImage, targetModel);
        targetBand.setSourceImage(new DefaultMultiLevelImage(targetMultiLevelSource));
//                }
        return targetBand;
    }

    @Override
    protected void setBandGeoCodings(Product product) throws IOException {
        if (Config.instance("s3tbx").load().preferences().getBoolean(SLSTR_L2_SST_USE_PIXELGEOCODINGS, false)) {
            setPixelBandGeoCodings(product);
        } else {
            setTiePointBandGeoCodings(product);
        }
    }

    @Override
    protected void setTimeCoding(Product targetProduct) throws IOException {
        setTimeCoding(targetProduct, "time_in.nc", "time_stamp_i");
    }

    private void setTiePointBandGeoCodings(Product product) throws IOException {
        final Band[] bands = product.getBands();
        for (Band band : bands) {
            setTiePointBandGeoCoding(product, band, getIdentifier(band.getName()));
        }
        final ProductNodeGroup<Mask> maskGroup = product.getMaskGroup();
        for (int i = 0; i < maskGroup.getNodeCount(); i++) {
            final Mask mask = maskGroup.get(i);
            setTiePointBandGeoCoding(product, mask, getGridIndexFromMask(mask));
        }
    }

    private void setTiePointBandGeoCoding(Product product, Band band, String gridIndex) throws IOException {
        if (geoCodingMap.containsKey(gridIndex)) {
            band.setGeoCoding(geoCodingMap.get(gridIndex));
        } else {
            final TiePointGrid origLatGrid = product.getTiePointGrid("latitude_tx");
            final TiePointGrid origLonGrid = product.getTiePointGrid("longitude_tx");
            if (origLatGrid == null || origLonGrid == null) {
                return;
            }
            final short[] referenceResolutions = getReferenceResolutions();
            final short[] sourceResolutions = getResolutions(gridIndex);
            final Double sourceStartOffset = getStartOffset(gridIndex);
            final Double sourceTrackOffset = getTrackOffset(gridIndex);
            if (sourceStartOffset != null && sourceTrackOffset != null) {
                final float[] offsets = getOffsets(sourceStartOffset, sourceTrackOffset, sourceResolutions);
                final float[] scalings = new float[]{
                        ((float) sourceResolutions[0]) / referenceResolutions[0],
                        ((float) sourceResolutions[1]) / referenceResolutions[1]
                };
                final AffineTransform transform = new AffineTransform();
                transform.translate(offsets[0], offsets[1]);
                transform.scale(scalings[0], scalings[1]);
                try {
                    final SlstrTiePointGeoCoding geoCoding =
                            new SlstrTiePointGeoCoding(origLatGrid, origLonGrid, new AffineTransform2D(transform));
                    band.setGeoCoding(geoCoding);
                    geoCodingMap.put(gridIndex, geoCoding);
                } catch (NoninvertibleTransformException e) {
                    throw new IOException("Could not create band-specific tie-point geo-coding. Try per-pixel geo-codings.");
                }
            }
        }
    }

    private void setPixelBandGeoCodings(Product product) throws IOException {
        final Band[] bands = product.getBands();
        for (Band band : bands) {
            final GeoCoding bandGeoCoding = getBandGeoCoding(product, getIdentifier(band.getName()));
            band.setGeoCoding(bandGeoCoding);
        }
        final ProductNodeGroup<Mask> maskGroup = product.getMaskGroup();
        for (int i = 0; i < maskGroup.getNodeCount(); i++) {
            final Mask mask = maskGroup.get(i);
            final GeoCoding bandGeoCoding = getBandGeoCoding(product, getIdentifier(mask.getName()));
            mask.setGeoCoding(bandGeoCoding);
        }
    }

    private String getIdentifier(String bandName) {
        final String[] identifierCandidates = bandName.split("_");
        for (int i = identifierCandidates.length - 1; i >= 0; i--) {
            if (identifierCandidates[i].equals("io") || identifierCandidates[i].equals("in")) {
                return identifierCandidates[i];
            }
        }
        return "";
    }

    private GeoCoding getBandGeoCoding(Product product, String end) throws IOException {
        if (geoCodingMap.containsKey(end)) {
            return geoCodingMap.get(end);
        } else {
            // @todo 1 tb/tb add test for the switch 2020-02-14
            String lonVariableName = null;
            String latVariableName = null;
            switch (end) {
                case "in":
                    lonVariableName = "longitude_in";
                    latVariableName = "latitude_in";
                    break;
                case "io":
                    lonVariableName = "longitude_io";
                    latVariableName = "latitude_io";
                    break;
            }

            final Band lonBand = product.getBand(lonVariableName);
            final Band latBand = product.getBand(latVariableName);
            if (latBand == null || lonBand == null) {
                return null;
            }

            final double[] longitudes = RasterUtils.loadDataScaled(lonBand);
            lonBand.unloadRasterData();
            final double[] latitudes = RasterUtils.loadDataScaled(latBand);
            latBand.unloadRasterData();

            final int width = product.getSceneRasterWidth();
            final int height = product.getSceneRasterHeight();
            final GeoRaster geoRaster = new GeoRaster(longitudes, latitudes, lonVariableName, latVariableName,
                                                      width, height, 1.0);

            final Preferences preferences = Config.instance("s3tbx").preferences();
            final String fwdKey = preferences.get(SLSTR_L2_SST_PIXEL_CODING_FORWARD, PixelForward.KEY);
            final String inverseKey = preferences.get(SLSTR_L2_SST_PIXEL_CODING_INVERSE, PixelQuadTreeInverse.KEY);

            final ForwardCoding forward = ComponentFactory.getForward(fwdKey);
            final InverseCoding inverse = ComponentFactory.getInverse(inverseKey);

            final ComponentGeoCoding geoCoding = new ComponentGeoCoding(geoRaster, forward, inverse, GeoChecks.ANTIMERIDIAN);
            geoCoding.initialize();

            geoCodingMap.put(end, geoCoding);
            return geoCoding;
        }
    }

    private String getGridIndexFromMask(Mask mask) {
        final String maskName = mask.getName();
        if (maskName.contains("_in_")) {
            return "in";
        } else if (maskName.contains("_io_")) {
            return "io";
        }
        return "";
    }

}
