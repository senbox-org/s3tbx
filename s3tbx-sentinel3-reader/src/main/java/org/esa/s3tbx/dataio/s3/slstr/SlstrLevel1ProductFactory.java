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
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.BasicPixelGeoCoding;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoCodingFactory;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.image.ImageManager;
import org.esa.snap.core.image.ResolutionLevel;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.dataio.netcdf.metadata.profiles.cf.CfBandPart;
import org.esa.snap.dataio.netcdf.util.AbstractNetcdfMultiLevelImage;
import org.esa.snap.dataio.netcdf.util.DataTypeUtils;
import org.esa.snap.dataio.netcdf.util.NetcdfFileOpener;
import org.esa.snap.dataio.netcdf.util.NetcdfOpImage;
import org.esa.snap.runtime.Config;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import ucar.ma2.DataType;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.awt.Dimension;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SlstrLevel1ProductFactory extends SlstrProductFactory {

    public final static String SLSTR_L1B_USE_PIXELGEOCODINGS = "s3tbx.reader.slstrl1b.pixelGeoCodings";
    public final static String SLSTR_L1B_LOAD_ORPHAN_PIXELS = "s3tbx.reader.slstrl1b.loadOrphanPixels";
    public final static String SLSTR_L1B_CUSTOM_CALIBRATION = "s3tbx.reader.slstrl1b.applyCustomCalibration";
    public final static String SLSTR_L1B_S3MPC_CALIBRATION = "s3tbx.reader.slstrl1b.applyS3MPCCalibration";
    private final static String SLSTR_L1B_CALIBRATION_PATTERN = "s3tbx.reader.slstrl1b.ID.calibration.TYPE";
    private final static double SLSTR_L1B_S3MPC_S5_NADIR_ADJUSTMENT_FACTOR = 1.12;
    private final static double SLSTR_L1B_S3MPC_S6_NADIR_ADJUSTMENT_FACTOR = 1.13;
    private final static double SLSTR_L1B_S3MPC_S5_OBLIQUE_ADJUSTMENT_FACTOR = 1.15;
    private final static double SLSTR_L1B_S3MPC_S6_OBLIQUE_ADJUSTMENT_FACTOR = 1.14;

    //todo read all these as metadata - tf 20160401
    // --> included Sn_quality_*.nc products to access solar irradiances - od 20170630
    private final static String[] EXCLUDED_IDS = new String[]{
            "ADFData", "SLSTR_F1_QUALITY_IN_Data",
            "SLSTR_F1_QUALITY_IO_Data", "SLSTR_F2_QUALITY_IN_Data", "SLSTR_F2_QUALITY_IO_Data",
//                "SLSTR_S1_QUALITY_AN_Data", "SLSTR_S1_QUALITY_AO_Data", "SLSTR_S2_QUALITY_AN_Data",
//                "SLSTR_S2_QUALITY_AO_Data", "SLSTR_S3_QUALITY_AN_Data", "SLSTR_S3_QUALITY_AO_Data",
//                "SLSTR_S4_QUALITY_AN_Data", "SLSTR_S4_QUALITY_AO_Data", "SLSTR_S4_QUALITY_BN_Data",
//                "SLSTR_S4_QUALITY_BO_Data", "SLSTR_S4_QUALITY_CN_Data", "SLSTR_S4_QUALITY_CO_Data",
//                "SLSTR_S5_QUALITY_AN_Data", "SLSTR_S5_QUALITY_AO_Data", "SLSTR_S5_QUALITY_BN_Data",
//                "SLSTR_S5_QUALITY_BO_Data", "SLSTR_S5_QUALITY_CN_Data", "SLSTR_S5_QUALITY_CO_Data",
//                "SLSTR_S6_QUALITY_AN_Data", "SLSTR_S6_QUALITY_AO_Data", "SLSTR_S6_QUALITY_BN_Data",
//                "SLSTR_S6_QUALITY_BO_Data", "SLSTR_S6_QUALITY_CN_Data", "SLSTR_S6_QUALITY_CO_Data",
            "SLSTR_S7_QUALITY_IN_Data", "SLSTR_S7_QUALITY_IO_Data", "SLSTR_S8_QUALITY_IN_Data",
            "SLSTR_S8_QUALITY_IO_Data", "SLSTR_S9_QUALITY_IN_Data", "SLSTR_S9_QUALITY_IO_Data"
    };
    private final Map<String, String> gridTypeToGridIndex;
    private final Map<String, Double> gridIndexToTrackOffset;
    private final Map<String, Double> gridIndexToStartOffset;
    private Map<String, Float> nameToWavelengthMap;
    private Map<String, Float> nameToBandwidthMap;
    private Map<String, Integer> nameToIndexMap;
    private Map<String, GeoCoding> geoCodingMap;
    private List<NetcdfFile> netcdfFileList;


    public SlstrLevel1ProductFactory(Sentinel3ProductReader productReader) {
        super(productReader);
        gridTypeToGridIndex = new HashMap<>();
        //todo adapt this when grid is contained in metadata file
        gridTypeToGridIndex.put("Fire", "f");
        gridTypeToGridIndex.put("1 km", "i");
        gridTypeToGridIndex.put("0.5 km stripe A", "a");
        gridTypeToGridIndex.put("0.5 km stripe B", "b");
        gridTypeToGridIndex.put("0.5 km TDI", "c");
        gridTypeToGridIndex.put("Tie Points", "t");
        gridIndexToTrackOffset = new HashMap<>();
        gridIndexToStartOffset = new HashMap<>();
        nameToWavelengthMap = new HashMap<>();
        nameToBandwidthMap = new HashMap<>();
        nameToIndexMap = new HashMap<>();
        geoCodingMap = new HashMap<>();
        netcdfFileList = new ArrayList<>();
    }

    protected Double getStartOffset(String gridIndex) {
        return gridIndexToStartOffset.get(gridIndex);
    }

    protected Double getTrackOffset(String gridIndex) {
        return gridIndexToTrackOffset.get(gridIndex);
    }

    protected String getProductSpecificMetadataElementName() {
        return "slstrProductInformation";
    }

    @Override
    protected void processProductSpecificMetadata(MetadataElement metadataElement) {
        final MetadataElement productInformationElement = metadataElement.getElement(getProductSpecificMetadataElementName());
        final Product masterProduct = findMasterProduct();
        final int numberOfMasterColumns = masterProduct.getSceneRasterWidth();
        final int numberOfMasterRows = masterProduct.getSceneRasterHeight();
        for (MetadataElement infoElement : productInformationElement.getElements()) {
            final String infoElementName = infoElement.getName();
            if (infoElementName.endsWith("ImageSize")) {
                if (infoElement.containsAttribute("grid")) {
                    String firstLetter = gridTypeToGridIndex.get(infoElement.getAttribute("grid").getData().getElemString());
                    String index;
                    if (infoElementName.equals("nadirImageSize")) {
                        index = firstLetter + "n";
                    } else {
                        index = firstLetter + "o";
                    }
                    MetadataAttribute startOffsetAttribute = infoElement.getAttribute("startOffset");
                    MetadataAttribute trackOffsetAttribute = infoElement.getAttribute("trackOffset");
                    double startOffset = startOffsetAttribute != null ? Double.parseDouble(startOffsetAttribute.getData().getElemString()) : 0.0;
                    double trackOffset = trackOffsetAttribute != null ? Double.parseDouble(trackOffsetAttribute.getData().getElemString()) : 0.0;
                    gridIndexToStartOffset.put(index, startOffset);
                    gridIndexToTrackOffset.put(index, trackOffset);
                    if (firstLetter.equals("t")) {
                        gridIndexToStartOffset.put("tx", startOffset);
                        gridIndexToTrackOffset.put("tx", trackOffset);
                    }
                    int numberOfRows = Integer.parseInt(infoElement.getAttribute("rows").getData().getElemString());
                    int numberOfColumns = Integer.parseInt(infoElement.getAttribute("columns").getData().getElemString());
                    if (numberOfColumns == numberOfMasterColumns && numberOfRows == numberOfMasterRows) {
                        setReferenceStartOffset(startOffset);
                        setReferenceTrackOffset(trackOffset);
                        setReferenceResolutions(getResolutions(index));
                    }
                }
            }
            if (infoElementName.equals("bandDescriptions")) {
                for (int j = 0; j < infoElement.getNumElements(); j++) {
                    final MetadataElement bandElement = infoElement.getElementAt(j);
                    final String bandName = bandElement.getAttribute("name").getData().getElemString();
                    float wavelength = Float.parseFloat(bandElement.getAttribute("centralWavelength").getData().getElemString());
                    //consider case that wavelength is given in micro meters
                    if (wavelength < 100) {
                        wavelength *= 1000;
                    }
                    float bandWidth = Float.parseFloat(bandElement.getAttribute("bandWidth").getData().getElemString());
                    if (bandWidth <= 1.0) {
                        bandWidth *= 1000;
                    }
                    nameToWavelengthMap.put(bandName, wavelength);
                    nameToBandwidthMap.put(bandName, bandWidth);
                    nameToIndexMap.put(bandName, j);
                }
            }
        }
    }

    @Override
    protected void configureTargetNode(Band sourceBand, RasterDataNode targetNode) {
        final String sourceBandName = sourceBand.getName();
        final String sourceBandNameStart = sourceBandName.substring(0, 2);
        if (nameToWavelengthMap.containsKey(sourceBandNameStart)) {
            ((Band) targetNode).setSpectralWavelength(nameToWavelengthMap.get(sourceBandNameStart));
            ((Band) targetNode).setSpectralBandIndex(nameToIndexMap.get(sourceBandNameStart));
            ((Band) targetNode).setSpectralBandwidth(nameToBandwidthMap.get(sourceBandNameStart));
            applyFactorAndOffset(targetNode);
        }
        configureDescription(sourceBand, targetNode);
    }

    private void applyFactorAndOffset(RasterDataNode targetNode) {
        double scalingOffset = targetNode.getScalingOffset();
        double scalingFactor = targetNode.getScalingFactor();
        double adjustmentFactor = 1.0;
        if (applyCustomCalibration()) {
            final double calibrationOffset = getCalibrationOffset(targetNode.getName());
            if (!Double.isNaN(calibrationOffset)) {
                scalingOffset = calibrationOffset;
            }
            final double calibrationFactor = getCalibrationFactor(targetNode.getName());
            if (!Double.isNaN(calibrationFactor)) {
                scalingFactor = calibrationFactor;
            }
            final double calibrationAdjustmentFactor = getCalibrationAdjustmentFactor(targetNode.getName());
            if (!Double.isNaN(calibrationAdjustmentFactor)) {
                adjustmentFactor = calibrationAdjustmentFactor;
            }
        }
        if (applyS3MPCCalibration()) {
            double s3mpcAdjustmentFactor = getS3MPCAdjustmentFactor(targetNode.getName());
            if (!Double.isNaN(s3mpcAdjustmentFactor)) {
                adjustmentFactor = s3mpcAdjustmentFactor;
            }
        }
        targetNode.setScalingOffset(scalingOffset * adjustmentFactor);
        targetNode.setScalingFactor(scalingFactor * adjustmentFactor);
    }

    private double getCalibrationOffset(String sourceBandName) {
        String calibrationOffsetPropertyName =
                SLSTR_L1B_CALIBRATION_PATTERN.replace("ID", sourceBandName.toLowerCase()).replace("TYPE", "offset");
        return Config.instance("s3tbx").load().preferences().getDouble(calibrationOffsetPropertyName, Double.NaN);
    }

    private double getCalibrationFactor(String sourceBandName) {
        String calibrationFactorPropertyName =
                SLSTR_L1B_CALIBRATION_PATTERN.replace("ID", sourceBandName.toLowerCase()).replace("TYPE", "factor");
        return Config.instance("s3tbx").load().preferences().getDouble(calibrationFactorPropertyName, Double.NaN);
    }

    private double getCalibrationAdjustmentFactor(String sourceBandName) {
        String calibrationFactorPropertyName =
                SLSTR_L1B_CALIBRATION_PATTERN.replace("ID", sourceBandName.toLowerCase()).
                        replace("TYPE", "adjustment_factor");
        return Config.instance("s3tbx").load().preferences().getDouble(calibrationFactorPropertyName, Double.NaN);
    }

    private double getS3MPCAdjustmentFactor(String sourceBandName) {
        if (sourceBandName.toLowerCase().startsWith("s5_radiance")) {
            if (sourceBandName.toLowerCase().endsWith("n")) {
                return SLSTR_L1B_S3MPC_S5_NADIR_ADJUSTMENT_FACTOR;
            } else if (sourceBandName.toLowerCase().endsWith("o")) {
                return SLSTR_L1B_S3MPC_S5_OBLIQUE_ADJUSTMENT_FACTOR;
            }
        } else if (sourceBandName.toLowerCase().startsWith("s6_radiance")) {
            if (sourceBandName.toLowerCase().endsWith("n")) {
                return SLSTR_L1B_S3MPC_S6_NADIR_ADJUSTMENT_FACTOR;
            } else if (sourceBandName.toLowerCase().endsWith("o")) {
                return SLSTR_L1B_S3MPC_S6_OBLIQUE_ADJUSTMENT_FACTOR;
            }
        }
        return Double.NaN;
    }

    protected void configureDescription(Band sourceBand, RasterDataNode targetNode) {
        final String sourceBandName = sourceBand.getName();
        final String sourceBandNameEnd = sourceBandName.substring(sourceBandName.length() - 2);
        if (sourceBandNameEnd.startsWith("i") || sourceBandNameEnd.startsWith("f")) {
            String description = sourceBand.getDescription();
            if (description == null) {
                targetNode.setDescription("(1 km)");
            } else {
                targetNode.setDescription(description + " (1 km)");
            }
        } else if (sourceBandNameEnd.startsWith("a") || sourceBandName.startsWith("b") ||
                   sourceBandName.startsWith("c")) {
            String description = sourceBand.getDescription();
            if (description == null) {
                targetNode.setDescription("(500 m)");
            } else {
                targetNode.setDescription(description + " (500 m)");
            }
        }
    }

    @Override
    protected List<String> getFileNames(Manifest manifest) {
        return manifest.getFileNames(EXCLUDED_IDS);
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
    protected void addDataNodes(Product masterProduct, Product targetProduct) throws IOException {
        super.addDataNodes(masterProduct, targetProduct);

        if (isOrphanPixelsAllowed()) {
            for (final Product sourceProduct : getOpenProductList()) {
                loadOrphanPixelBands(targetProduct, sourceProduct);
            }
        }
    }

    @Override
    protected RasterDataNode addSpecialNode(Product masterProduct, Band sourceBand, Product targetProduct) {
        final String sourceBandName = sourceBand.getName();
        final int sourceBandNameLength = sourceBandName.length();
        String gridIndex = sourceBandName;
        if (sourceBandNameLength > 1) {
            gridIndex = sourceBandName.substring(sourceBandNameLength - 2);
        }
        final Double sourceStartOffset = getStartOffset(gridIndex);
        final Double sourceTrackOffset = getTrackOffset(gridIndex);
        if (sourceStartOffset != null && sourceTrackOffset != null) {
            final short[] sourceResolutions = getResolutions(gridIndex);
            if (gridIndex.startsWith("t")) {
                return copyTiePointGrid(sourceBand, targetProduct, sourceStartOffset, sourceTrackOffset, sourceResolutions);
            } else {
                final Band targetBand = new Band(sourceBandName, sourceBand.getDataType(),
                                                 sourceBand.getRasterWidth(), sourceBand.getRasterHeight());
                targetProduct.addBand(targetBand);
                ProductUtils.copyRasterDataNodeProperties(sourceBand, targetBand);
                final RenderedImage sourceRenderedImage = sourceBand.getSourceImage().getImage(0);
                //todo remove commented lines when resampling works with scenetransforms
                //if pixel band geo-codings are used, scenetransforms are set
//                if (Config.instance("s3tbx").load().preferences().getBoolean(SLSTR_L1B_USE_PIXELGEOCODINGS, false)) {
//                    targetBand.setSourceImage(sourceRenderedImage);
//                } else {
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
        }
        return sourceBand;
    }

    @Override
    protected String getAutoGroupingString(Product[] sourceProducts) {
        String autoGrouping = super.getAutoGroupingString(sourceProducts);
        String[] unwantedGroups = new String[]{
                "F1_BT", "F2_BT", "S1_radiance", "S2_radiance", "S3_radiance",
                "S4_radiance", "S5_radiance", "S6_radiance", "S7_BT", "S8_BT", "S9_BT", "met"
        };
        for (String unwantedGroup : unwantedGroups) {
            if (autoGrouping.startsWith(unwantedGroup)) {
                autoGrouping = autoGrouping.replace(unwantedGroup + ":", "");
            } else if (autoGrouping.contains(unwantedGroup)) {
                autoGrouping = autoGrouping.replace(":" + unwantedGroup, "");
            }
        }
        return autoGrouping;
    }

    @Override
    protected void setAutoGrouping(Product[] sourceProducts, Product targetProduct) {
        String bandGrouping = getAutoGroupingString(sourceProducts);
        targetProduct.setAutoGrouping(
                                      "F*BT_in:F*exception_in:" +
                                      "F*BT_io:F*exception_io:" +
                                      "S*BT_in:S*exception_in:" +
                                      "S*BT_io:s*exception_io:" +
                                      "radiance_an:S*exception_an:" +
                                      "radiance_ao:S*exception_ao:" +
                                      "radiance_bn:S*exception_bn:" +
                                      "radiance_bo:S*exception_bo:" +
                                      "radiance_cn:S*exception_cn:" +
                                      "radiance_co:S*exception_co:" +
                                      (isOrphanPixelsAllowed() ? "*orphan*:" : "") +
                                      "x_*:y_*:" +
                                      "elevation:latitude:longitude:" +
                                      "specific_humidity:temperature_profile:" +
                                      "bayes_an_:bayes_ao_:" +
                                      "bayes_bn_:bayes_bo_:" +
                                      "bayes_cn_:bayes_co_:" +
                                      "bayes_in_:bayes_io_:" +
                                      "cloud_an_:cloud_ao_:" +
                                      "cloud_bn_:cloud_bo_:" +
                                      "cloud_cn_:cloud_co_:" +
                                      "cloud_in_:cloud_io_:" +
                                      "confidence_an_:confidence_ao_:" +
                                      "confidence_bn_:confidence_bo_:" +
                                      "confidence_cn_:confidence_co_:" +
                                      "confidence_in_:confidence_io_:" +
                                      "pointing_an_:pointing_ao_:" +
                                      "pointing_bn_:pointing_bo_:" +
                                      "pointing_cn_:pointing_co_:" +
                                      "pointing_in_:pointing_io_:" +
                                      "S*_exception_an_*:S*_exception_ao_*:" +
                                      "S*_exception_bn_*:S*_exception_bo_*:" +
                                      "S*_exception_cn_*:S*_exception_co_*:" +
                                      "S*_exception_in_*:S*_exception_io_*:" +
                                      "F*_exception_in_*:F*_exception_io_*:" +
                                      bandGrouping);
    }

    @Override
    protected void setSceneTransforms(Product product) {
        //if tie point band geo-codings are used, imagetomodeltransforms are set
        //todo remove commented lines when resampling works with scenetransforms
//        if (Config.instance("s3tbx").load().preferences().getBoolean(SLSTR_L1B_USE_PIXELGEOCODINGS, false)) {
//            final Band[] bands = product.getBands();
//            for (Band band : bands) {
//                final GeoCoding bandGeoCoding = getBandGeoCoding(product, band.getName().substring(band.getName().length() - 2));
//                final SlstrGeoCodingSceneTransformProvider transformProvider =
//                        new SlstrGeoCodingSceneTransformProvider(product.getSceneGeoCoding(), bandGeoCoding);
//                band.setModelToSceneTransform(transformProvider.getModelToSceneTransform());
//                band.setSceneToModelTransform(transformProvider.getSceneToModelTransform());
//            }
//            final ProductNodeGroup<Mask> maskGroup = product.getMaskGroup();
//            for (int i = 0; i < maskGroup.getNodeCount(); i++) {
//                final Mask mask = maskGroup.get(i);
//                final GeoCoding bandGeoCoding = getBandGeoCoding(product, mask.getName().substring(mask.getName().length() - 2));
//                final SlstrGeoCodingSceneTransformProvider transformProvider =
//                        new SlstrGeoCodingSceneTransformProvider(product.getSceneGeoCoding(), bandGeoCoding);
//                mask.setModelToSceneTransform(transformProvider.getModelToSceneTransform());
//                mask.setSceneToModelTransform(transformProvider.getSceneToModelTransform());
//            }
//        }
    }

    @Override
    protected void setBandGeoCodings(Product product) {
        if (Config.instance("s3tbx").load().preferences().getBoolean(SLSTR_L1B_USE_PIXELGEOCODINGS, false)) {
            setPixelBandGeoCodings(product);
        } else {
            setTiePointBandGeoCodings(product);
        }
    }

    protected boolean isOrphanPixelsAllowed() {
        return Config.instance("s3tbx").load().preferences().getBoolean(SLSTR_L1B_LOAD_ORPHAN_PIXELS, false);
    }

    private boolean applyCustomCalibration() {
        return Config.instance("s3tbx").load().preferences().getBoolean(SLSTR_L1B_CUSTOM_CALIBRATION, false);
    }

    private boolean applyS3MPCCalibration() {
        return Config.instance("s3tbx").load().preferences().getBoolean(SLSTR_L1B_S3MPC_CALIBRATION, false);
    }

    protected void loadOrphanPixelBands(Product targetProduct, final Product sourceProduct) throws IOException {
        File file = sourceProduct.getFileLocation();
        NetcdfFile netcdfFile = NetcdfFileOpener.open(file.getAbsolutePath());
        boolean foundOrphan = false;
        if (netcdfFile != null) {
            List<Variable> variables = netcdfFile.getVariables();
            for (Variable variable : variables) {
                String shortName = variable.getShortName();
                if (shortName.contains("orphan") && !shortName.equals("orphan_pixels")) {
                    final int height = variable.getDimension(0).getLength();
                    final int width = variable.getDimension(1).getLength();
                    Band band = new Band(shortName, getDataType(variable), width, height);
                    targetProduct.addBand(band);
                    CfBandPart.readCfBandAttributes(variable, band);
                    band.setSourceImage(new AbstractNetcdfMultiLevelImage(band) {
                        @Override
                        protected RenderedImage createImage(int level) {
                            RasterDataNode rdn = getRasterDataNode();
                            ResolutionLevel resolutionLevel = ResolutionLevel.create(getModel(), level);
                            Dimension imageTileSize = new Dimension(getTileWidth(), getTileHeight());
                            return new SlstrOrphanOpImage(variable, netcdfFile, rdn, imageTileSize, resolutionLevel);
                        }
                    });
                    foundOrphan = true;
                }
            }
            // close later if we loaded an orphan variable, otherwise close immediately
            if(foundOrphan) {
                netcdfFileList.add(netcdfFile);
            } else {
                netcdfFile.close();
            }
        }
    }

    @Override
    public void dispose() throws IOException {
        super.dispose();
        for (NetcdfFile netcdfFile : netcdfFileList) {
            netcdfFile.close();
        }
        netcdfFileList.clear();
    }

    private void setTiePointBandGeoCodings(Product product) {
        final Band[] bands = product.getBands();
        for (Band band : bands) {
            setTiePointBandGeoCoding(product, band, band.getName().substring(band.getName().length() - 2));
        }
        final ProductNodeGroup<Mask> maskGroup = product.getMaskGroup();
        for (int i = 0; i < maskGroup.getNodeCount(); i++) {
            final Mask mask = maskGroup.get(i);
            setTiePointBandGeoCoding(product, mask, getGridIndexFromMask(mask));
        }
    }

    private void setTiePointBandGeoCoding(Product product, Band band, String gridIndex) {
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
                    //todo handle exception - tf 20160106
                }
            }
        }
    }

    private void setPixelBandGeoCodings(Product product) {
        final Band[] bands = product.getBands();
        for (Band band : bands) {
            final GeoCoding bandGeoCoding = getBandGeoCoding(product, band.getName().substring(band.getName().length() - 2));
            band.setGeoCoding(bandGeoCoding);
        }
        final ProductNodeGroup<Mask> maskGroup = product.getMaskGroup();
        for (int i = 0; i < maskGroup.getNodeCount(); i++) {
            final Mask mask = maskGroup.get(i);
            final GeoCoding bandGeoCoding = getBandGeoCoding(product, mask.getName().substring(mask.getName().length() - 2));
            mask.setGeoCoding(bandGeoCoding);
        }
    }

    private GeoCoding getBandGeoCoding(Product product, String end) {
        if (geoCodingMap.containsKey(end)) {
            return geoCodingMap.get(end);
        } else {
            Band latBand = null;
            Band lonBand = null;
            switch (end) {
                case "an":
                    latBand = product.getBand("latitude_an");
                    lonBand = product.getBand("longitude_an");
                    break;
                case "ao":
                    latBand = product.getBand("latitude_ao");
                    lonBand = product.getBand("longitude_ao");
                    break;
                case "bn":
                    latBand = product.getBand("latitude_bn");
                    lonBand = product.getBand("longitude_bn");
                    break;
                case "bo":
                    latBand = product.getBand("latitude_bo");
                    lonBand = product.getBand("longitude_bo");
                    break;
                case "cn":
                    latBand = product.getBand("latitude_cn");
                    lonBand = product.getBand("longitude_cn");
                    break;
                case "co":
                    latBand = product.getBand("latitude_co");
                    lonBand = product.getBand("longitude_co");
                    break;
                case "in":
                    latBand = product.getBand("latitude_in");
                    lonBand = product.getBand("longitude_in");
                    break;
                case "io":
                    latBand = product.getBand("latitude_io");
                    lonBand = product.getBand("longitude_io");
                    break;
                case "fn":
                    latBand = product.getBand("latitude_fn");
                    lonBand = product.getBand("longitude_fn");
                    break;
                case "fo":
                    latBand = product.getBand("latitude_fo");
                    lonBand = product.getBand("longitude_fo");
                    break;
            }
            if (latBand != null && lonBand != null) {
                final BasicPixelGeoCoding geoCoding = GeoCodingFactory.createPixelGeoCoding(latBand, lonBand, "", 5);
                geoCodingMap.put(end, geoCoding);
                return geoCoding;
            }
        }
        return null;
    }

    private String getGridIndexFromMask(Mask mask) {
        final String maskName = mask.getName();
        if (maskName.contains("_an_")) {
            return "an";
        } else if (maskName.contains("_ao_")) {
            return "ao";
        } else if (maskName.contains("_bn_")) {
            return "bn";
        } else if (maskName.contains("_bo_")) {
            return "bo";
        } else if (maskName.contains("_cn_")) {
            return "cn";
        } else if (maskName.contains("_co_")) {
            return "co";
        } else if (maskName.contains("_in_")) {
            return "in";
        } else if (maskName.contains("_io_")) {
            return "io";
        } else if (maskName.contains("_fn_")) {
            return "fn";
        } else if (maskName.contains("_fo_")) {
            return "fo";
        }
        return "";
    }


    private int getDataType(Variable variable) {
        int rasterDataType = DataTypeUtils.getRasterDataType(variable);
        if (variable.getDataType() == DataType.LONG) {
            rasterDataType = variable.isUnsigned() ? ProductData.TYPE_UINT32 : ProductData.TYPE_INT32;
        }
        return rasterDataType;
    }

    private static class SlstrOrphanOpImage extends NetcdfOpImage {

        public SlstrOrphanOpImage(Variable variable, NetcdfFile netcdf, RasterDataNode rdn, Dimension imageTileSize,
                                  ResolutionLevel resolutionLevel) {
            super(variable, new int[]{}, false, netcdf, ImageManager.getDataBufferType(rdn.getDataType()),
                  rdn.getRasterWidth(), rdn.getRasterHeight(), imageTileSize, resolutionLevel,
                  ArrayConverter.IDENTITY, new DimensionIndices(1, 0, 2));

        }
    }
}
