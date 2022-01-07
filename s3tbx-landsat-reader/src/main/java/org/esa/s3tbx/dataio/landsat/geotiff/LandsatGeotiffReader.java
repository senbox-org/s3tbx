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

package org.esa.s3tbx.dataio.landsat.geotiff;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.VirtualDir;
import com.bc.ceres.glevel.MultiLevelImage;
import org.esa.snap.core.dataio.AbstractProductReader;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.image.ImageManager;
import org.esa.snap.core.image.SourceImageScaler;
import org.esa.snap.dataio.geotiff.GeoTiffProductReaderPlugIn;
import org.esa.snap.runtime.Config;

import javax.media.jai.ImageLayout;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import java.awt.Dimension;
import java.awt.RenderingHints;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This reader is capable of reading Landsat data products
 * where each bands is distributes as a single GeoTIFF image.
 */
public class LandsatGeotiffReader extends AbstractProductReader {

    public static final String SYSPROP_READ_AS = "s3tbx.landsat.readAs";

    enum Resolution {
        DEFAULT,
        L8_PANCHROMATIC,
        L8_REFLECTIVE,
    }

    private static final String READ_AS_REFLECTANCE = "reflectance";
    private static final Logger LOG = Logger.getLogger(LandsatGeotiffReader.class.getName());

    private static final String RADIANCE_UNITS = "W/(m^2*sr*µm)";
    private static final String REFLECTANCE_UNITS = "dl";
    private static final String DEGREE_UNITS = "deg";

    private final Resolution targetResolution;

    private LandsatMetadata landsatMetadata;
    private LandsatQA landsatQA;
    private List<Product> bandProducts;
    private VirtualDir virtualDir;
    private String basePath;

    public LandsatGeotiffReader(ProductReaderPlugIn readerPlugin) {
        this(readerPlugin, Resolution.DEFAULT);
    }

    public LandsatGeotiffReader(ProductReaderPlugIn readerPlugin, Resolution targetResolution) {
        super(readerPlugin);
        this.targetResolution = targetResolution;
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        virtualDir = LandsatGeotiffReaderPlugin.getInput(getInput());

        File mtlFile = getMtlFile();
        landsatMetadata = LandsatMetadataFactory.create(mtlFile);
        landsatQA = LandsatQAFactory.create(mtlFile);
        // todo - retrieving the product dimension needs a revision
        Dimension productDim;
        switch (targetResolution) {
            case L8_REFLECTIVE:
                productDim = landsatMetadata.getReflectanceDim();
                if (productDim == null) {
                    productDim = landsatMetadata.getThermalDim();
                }
                break;
            case L8_PANCHROMATIC:
                productDim = landsatMetadata.getPanchromaticDim();
                break;
            default:
                productDim = landsatMetadata.getPanchromaticDim();
                if (productDim == null) {
                    productDim = landsatMetadata.getReflectanceDim();
                }
                if (productDim == null) {
                    productDim = landsatMetadata.getThermalDim();
                }
        }

        MetadataElement metadataElement = landsatMetadata.getMetaDataElementRoot();
        Product product = new Product(getProductName(mtlFile), landsatMetadata.getProductType(), productDim.width, productDim.height);
        product.setFileLocation(mtlFile);
        product.getMetadataRoot().addElement(metadataElement);

        ProductData.UTC utcCenter = landsatMetadata.getCenterTime();
        product.setStartTime(utcCenter);
        product.setEndTime(utcCenter);

        addBands(product);

        return product;
    }

    protected File getMtlFile() throws IOException {
        File mtlFile = null;
        File fileInput = LandsatGeotiffReaderPlugin.getFileInput(getInput());
        if (fileInput != null && fileInput.exists() && LandsatGeotiffReaderPlugin.isMetadataFilename(fileInput.getName())) {
            basePath = "";
            mtlFile = fileInput;
        } else {
            String[] fileList = virtualDir.listAllFiles();
            for (String filePath : fileList) {
                if (LandsatGeotiffReaderPlugin.isMetadataFilename(filePath)) {
                    basePath = new File(filePath).getParent();
                    basePath = basePath == null ? "" : basePath + "/";

                    mtlFile = virtualDir.getFile(filePath);
                    break;
                }
            }
        }
        if (mtlFile == null) {
            throw new IOException("Can not find metadata file.");
        }
        if (!mtlFile.canRead()) {
            throw new IOException("Can not read metadata file: " + mtlFile.getAbsolutePath());
        }
        return mtlFile;
    }

    private static String getProductName(File mtlfile) {
        String filename = mtlfile.getName();
        int extensionIndex = filename.toLowerCase().indexOf("_mtl.txt");
        return filename.substring(0, extensionIndex);
    }

    private void addBands(Product product) throws IOException {
        final GeoTiffProductReaderPlugIn plugIn = new GeoTiffProductReaderPlugIn();
        final MetadataAttribute[] productAttributes = landsatMetadata.getProductMetadata().getAttributes();
        final Pattern pattern = landsatMetadata.getOpticalBandFileNamePattern();
        product.setAutoGrouping("sun:view");
        bandProducts = new ArrayList<>();
        for (MetadataAttribute metadataAttribute : productAttributes) {
            String attributeName = metadataAttribute.getName();
            Matcher matcher = pattern.matcher(attributeName);
            final String qualityBandNameKey = landsatMetadata.getQualityBandNameKey();
            if (matcher.matches()) {
                String bandNumber = matcher.group(1);
                String fileName = metadataAttribute.getData().getElemString();

                File bandFile = virtualDir.getFile(basePath + fileName);
                ProductReader productReader = plugIn.createReaderInstance();
                Product bandProduct = productReader.readProductNodes(bandFile, null);
                if (bandProduct != null) {
                    bandProducts.add(bandProduct);
                    Band srcBand = bandProduct.getBandAt(0);
                    String bandName = landsatMetadata.getBandNamePrefix(bandNumber);
                    Band band = addBandToProduct(bandName, srcBand, product);
                    band.setScalingFactor(landsatMetadata.getScalingFactor(bandNumber));
                    band.setScalingOffset(landsatMetadata.getScalingOffset(bandNumber));

                    band.setNoDataValue(0.0);
                    band.setNoDataValueUsed(true);

                    band.setSpectralWavelength(landsatMetadata.getWavelength(bandNumber));
                    band.setSpectralBandwidth(landsatMetadata.getBandwidth(bandNumber));

                    band.setDescription(landsatMetadata.getBandDescription(bandNumber));
                    band.setUnit(RADIANCE_UNITS);
                    final Preferences preferences = Config.instance("s3tbx").load().preferences();
                    final String readAs = preferences.get(LandsatGeotiffReader.SYSPROP_READ_AS, null);
                    if (readAs != null) {
                        if (READ_AS_REFLECTANCE.equalsIgnoreCase(readAs)) {
                            band.setDescription(landsatMetadata.getBandDescription(bandNumber) + " , as TOA Reflectance");
                            band.setUnit(REFLECTANCE_UNITS);
                        } else {
                            LOG.warning(String.format("Property '%s' has unsupported value '%s'",
                                                      LandsatGeotiffReader.SYSPROP_READ_AS, readAs));
                        }
                    }
                }
            } else if (qualityBandNameKey != null && landsatQA != null && attributeName.startsWith(qualityBandNameKey) ) {
                String fileName = metadataAttribute.getData().getElemString();
                File bandFile = virtualDir.getFile(basePath + fileName);
                ProductReader productReader = plugIn.createReaderInstance();
                Product bandProduct = productReader.readProductNodes(bandFile, null);
                if (bandProduct != null) {
                    bandProducts.add(bandProduct);
                    Band srcBand = bandProduct.getBandAt(0);
                    String bandName = attributeName.endsWith("SATURATION") ? "satflags" : "flags";

                    Band band = addBandToProduct(bandName, srcBand, product);
                    band.setNoDataValue(0.0);
                    band.setNoDataValueUsed(true);
                    band.setDescription(attributeName.endsWith("SATURATION") ? "Saturation Band" : "Quality Band");

                    FlagCoding flagCoding = landsatQA.createFlagCoding(bandName);
                    band.setSampleCoding(flagCoding);
                    product.getFlagCodingGroup().add(flagCoding);
                }
            }
        }
        String angleBand;
        if ((angleBand = landsatMetadata.getAngleSensorAzimuthBandName()) != null) {
            addAngleBand(angleBand, "view_azimuth", product);
        }
        if ((angleBand = landsatMetadata.getAngleSensorZenithBandName()) != null) {
            addAngleBand(angleBand, "view_zenith", product);
        }
        if ((angleBand = landsatMetadata.getAngleSolarAzimuthBandName()) != null) {
            addAngleBand(angleBand, "sun_azimuth", product);
        }
        if ((angleBand = landsatMetadata.getAngleSolarZenithBandName()) != null) {
            addAngleBand(angleBand, "sun_zenith", product);
        }
        if (landsatQA != null) {
            List<Mask> masks;
            if (Resolution.DEFAULT.equals(targetResolution)) {
                Dimension dimension = landsatMetadata.getReflectanceDim();
                if (dimension == null) {
                    dimension = landsatMetadata.getThermalDim();
                }
                masks = landsatQA.createMasks(dimension != null ? dimension : product.getSceneRasterSize());
            } else {
                masks = landsatQA.createMasks(product.getSceneRasterSize());
            }
            for (Mask mask : masks) {
                product.getMaskGroup().add(mask);
            }
        }
        ImageLayout imageLayout = new ImageLayout();
        for (Product bandProduct : bandProducts) {
            if (product.getSceneGeoCoding() == null &&
                    product.getSceneRasterWidth() == bandProduct.getSceneRasterWidth() &&
                    product.getSceneRasterHeight() == bandProduct.getSceneRasterHeight()) {
                product.setSceneGeoCoding(bandProduct.getSceneGeoCoding());
                Dimension tileSize = bandProduct.getPreferredTileSize();
                if (tileSize == null) {
                    tileSize = ImageManager.getPreferredTileSize(bandProduct);
                }
                product.setPreferredTileSize(tileSize);
                imageLayout.setTileWidth(tileSize.width);
                imageLayout.setTileHeight(tileSize.height);
                break;
            }
        }

        if (Resolution.DEFAULT.equals(targetResolution)) {
            for (int i = 0; i < bandProducts.size(); i++) {
                Product bandProduct = bandProducts.get(i);
                Band band = product.getBandAt(i);
                final MultiLevelImage sourceImage = bandProduct.getBandAt(0).getSourceImage();
                band.setSourceImage(sourceImage);
                band.setGeoCoding(bandProduct.getSceneGeoCoding());
            }
        } else {
            MultiLevelImage targetImage = null;
            for (Product bandProduct : bandProducts) {
                if (product.getSceneRasterWidth() == bandProduct.getSceneRasterWidth() &&
                        product.getSceneRasterHeight() == bandProduct.getSceneRasterHeight()) {
                    targetImage = bandProduct.getBandAt(0).getSourceImage();
                    break;
                }
            }
            if (targetImage == null) {
                throw new IllegalStateException("Could not determine target image");
            }
            for (int i = 0; i < bandProducts.size(); i++) {
                Product bandProduct = bandProducts.get(i);
                final MultiLevelImage sourceImage = bandProduct.getBandAt(0).getSourceImage();
                final RenderingHints renderingHints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, imageLayout);
                float[] scalings = new float[2];
                scalings[0] = product.getSceneRasterWidth() / (float) bandProduct.getSceneRasterWidth();
                scalings[1] = product.getSceneRasterHeight() / (float) bandProduct.getSceneRasterHeight();

                Band band = product.getBandAt(i);
                PlanarImage image = SourceImageScaler.scaleMultiLevelImage(targetImage, sourceImage, scalings, null, renderingHints,
                        band.getNoDataValue(),
                        Interpolation.getInstance(Interpolation.INTERP_NEAREST));
                band.setSourceImage(image);
            }
        }
    }

    private void addAngleBand(String bandFileName, String bandName, Product product) throws IOException {
        File bandFile = virtualDir.getFile(basePath + bandFileName);
        final Product bandProduct = ProductIO.readProduct(bandFile);
        if (bandProduct != null) {
            bandProducts.add(bandProduct);
            Band srcBand = bandProduct.getBandAt(0);
            Band band = addBandToProduct(bandName, srcBand, product);
            band.setNoDataValue(0.0);
            band.setNoDataValueUsed(true);
            band.setDescription(bandName);
            band.setScalingFactor(.01);
            band.setUnit(DEGREE_UNITS);
        }
    }

    private Band addBandToProduct(String bandName, Band srcBand, Product product) {
        Dimension bandDimension = getBandDimension(srcBand, targetResolution);
        Band band = new Band(bandName, srcBand.getDataType(), bandDimension.width, bandDimension.height);
        product.addBand(band);
        return band;
    }

    private Dimension getBandDimension(Band srcBand, Resolution targetResolution) {
        switch (targetResolution) {
            case L8_REFLECTIVE:
                return landsatMetadata.getReflectanceDim();
            case L8_PANCHROMATIC:
                return landsatMetadata.getPanchromaticDim();
            default:
                return srcBand.getRasterSize();
        }
    }


    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight, int sourceStepX, int sourceStepY,
                                          Band destBand, int destOffsetX, int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {
        // all bands use source images as source for its data
        throw new IllegalStateException();
    }

    @Override
    public void close() throws IOException {
        for (Product bandProduct : bandProducts) {
            bandProduct.closeIO();
        }
        bandProducts.clear();
        virtualDir.close();
        virtualDir = null;
        super.close();
    }

}