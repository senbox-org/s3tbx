/*
 *
 * Copyright (c) 2022.  Brockmann Consult GmbH (info@brockmann-consult.de)
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
 *
 */

package org.esa.s3tbx.dataio.landsat.geotiff;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.VirtualDir;
import com.bc.ceres.glevel.MultiLevelImage;
import org.esa.snap.core.dataio.AbstractProductReader;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.image.ImageManager;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.dataio.geotiff.GeoTiffProductReaderPlugIn;

import javax.media.jai.ImageLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * This reader is capable of reading Landsat Collection 2 Level 2 data products
 * where each band is present as a single GeoTIFF image.
 *
 * @author Sabine Embacher
 */
public class LandsatGeotiffColl2L2Reader extends AbstractProductReader {

    private MetadataElement productMetadata;

    private static final Logger LOG = Logger.getLogger(LandsatGeotiffColl2L2Reader.class.getName());

    private static final Map<String, String> bandDescriptions = Collections.unmodifiableMap(new HashMap<String, String>() {{
        put("sr_b1", "Coastal Aerosol (Operational Land Imager (OLI))");
        put("sr_b2", "Blue (OLI)");
        put("sr_b3", "Green (OLI)");
        put("sr_b4", "Red (OLI)");
        put("sr_b5", "Near-Infrared (NIR) (OLI)");
        put("sr_b6", "Short Wavelength Infrared (SWIR) 1 (OLI)");
        put("sr_b7", "SWIR 2 (OLI)");
        put("st_b10", "Thermal Infrared Sensor (TIRS) 1");
        put("st_trad", "Thermal band converted to radiance");
        put("st_urad", "Upwelled Radiance");
        put("st_drad", "Downwelled Radiance");
        put("st_atran", "Atmospheric Transmittance");
        put("st_emis", "Emissivity of Band 10 estimated from ASTER GED");
        put("st_emsd", "Emissivity standard deviation");
        put("st_cdist", "Pixel distance to cloud");
        put("qa_pixel", "Level-1 QA Band. The output from the CFMask algorithm is used as an input for the Quality " +
                        "Assessment Application, which calculates values for all fields in the QA Band file. " +
                        "The QA Band file contains quality statistics gathered from the cloud mask and statistics " +
                        "information for the scene.");
        put("qa_radsat", "Level-1 Radiometric Saturation QA and Terrain Occlusion");
        put("sr_qa_aerosol", "The SR Aerosol QA file provides low-level details about factors that may have " +
                             "influenced the final product. The default value for bits 6 and 7, “Aerosol Level’, is " +
                             "Climatology (00). A value of Climatology means no aerosol correction was applied.");
        put("st_qa", "The ST QA file indicates uncertainty of the temperatures given in the ST band file");
    }});

    private static final Map<String, Double> scalingFactors = Collections.unmodifiableMap(new HashMap<String, Double>() {{
        put("sr_b1", 2.75e-5);
        put("sr_b2", 2.75e-5);
        put("sr_b3", 2.75e-5);
        put("sr_b4", 2.75e-5);
        put("sr_b5", 2.75e-5);
        put("sr_b6", 2.75e-5);
        put("sr_b7", 2.75e-5);
        put("st_b10", 0.00341802);
        put("st_trad", 0.001);
        put("st_urad", 0.001);
        put("st_drad", 0.001);
        put("st_atran", 0.0001);
        put("st_emis", 0.0001);
        put("st_emsd", 0.0001);
        put("st_cdist", 0.01);
    }});

    private static final Map<String, Double> addOffsets = Collections.unmodifiableMap(new HashMap<String, Double>() {{
        put("sr_b1", -0.2);
        put("sr_b2", -0.2);
        put("sr_b3", -0.2);
        put("sr_b4", -0.2);
        put("sr_b5", -0.2);
        put("sr_b6", -0.2);
        put("sr_b7", -0.2);
        put("st_b10", 149.0);
    }});

    private static final Map<String, Double> fillValues = Collections.unmodifiableMap(new HashMap<String, Double>() {{
        put("sr_b1", 0.0);
        put("sr_b2", 0.0);
        put("sr_b3", 0.0);
        put("sr_b4", 0.0);
        put("sr_b5", 0.0);
        put("sr_b6", 0.0);
        put("sr_b7", 0.0);
        put("st_b10", 0.0);
        put("st_trad", -9999.0);
        put("st_urad", -9999.0);
        put("st_drad", -9999.0);
        put("st_atran", -9999.0);
        put("st_emis", -9999.0);
        put("st_emsd", -9999.0);
        put("st_cdist", -9999.0);
        put("qa_pixel", 1.0);
        put("sr_qa_aerosol", 1.0);
        put("st_qa", -9999.0);
    }});

    private static final Map<String, String> units = Collections.unmodifiableMap(new HashMap<String, String>() {{
        put("st_b10", "Kelvin");
        put("st_trad", "W/m^2 sr µm)");
        put("st_urad", "W/m^2 sr µm)");
        put("st_drad", "W/m^2 sr µm)");
        put("st_cdist", "km");
        put("st_qa", "Kelvin");
    }});

    private static final Map<String, Float> wavelengths = Collections.unmodifiableMap(new HashMap<String, Float>() {{
        // see doc: https://d9-wret.s3.us-west-2.amazonaws.com/assets/palladium/production/s3fs-public/atoms/files/LSDS-1328_Landsat8-9-OLI-TIRS-C2-L2-DFCB-v6.pdf
        put("sr_b1", 443.0f);     // from doc: 435.0 - 451.0 nm  =  wl: 443.0  bw: 16.0
        put("sr_b2", 482.0f);     // from doc: 452.0 - 512.0 nm  =  wl: 482.0  bw: 60.0
        put("sr_b3", 561.5f);     // from doc: 533.0 - 590.0 nm  =  wl: 561.5  bw: 57.0
        put("sr_b4", 654.5f);     // from doc: 636.0 - 673.0 nm  =  wl: 654.5  bw: 37.0
        put("sr_b5", 865.0f);     // from doc: 851.0 - 879.0 nm  =  wl: 865.0  bw: 28.0
        put("sr_b6", 1608.5f);    // from doc: 1566.0 - 1651.0 nm  =  wl: 1608.5  bw: 85.0
        put("sr_b7", 2200.5f);    // from doc: 2107.0 - 2294.0 nm  =  wl: 2200.5  bw: 187.0
        put("st_b10", 10895.0f);  // from doc: 10600.0 - 11190.0 nm  =  wl: 10895.0  bw: 590.0
    }});

    private static final Map<String, Float> bandwidths = Collections.unmodifiableMap(new HashMap<String, Float>() {{
        // see doc: https://d9-wret.s3.us-west-2.amazonaws.com/assets/palladium/production/s3fs-public/atoms/files/LSDS-1328_Landsat8-9-OLI-TIRS-C2-L2-DFCB-v6.pdf
        put("sr_b1", 16f);      // from doc: 435.0 - 451.0 nm  =  wl: 443.0  bw: 16.0
        put("sr_b2", 60f);      // from doc: 452.0 - 512.0 nm  =  wl: 482.0  bw: 60.0
        put("sr_b3", 57f);      // from doc: 533.0 - 590.0 nm  =  wl: 561.5  bw: 57.0
        put("sr_b4", 37f);      // from doc: 636.0 - 673.0 nm  =  wl: 654.5  bw: 37.0
        put("sr_b5", 28f);      // from doc: 851.0 - 879.0 nm  =  wl: 865.0  bw: 28.0
        put("sr_b6", 85f);      // from doc: 1566.0 - 1651.0 nm  =  wl: 1608.5  bw: 85.0
        put("sr_b7", 187f);     // from doc: 2107.0 - 2294.0 nm  =  wl: 2200.5  bw: 187.0
        put("st_b10", 590f);    // from doc: 10600.0 - 11190.0 nm  =  wl: 10895.0  bw: 590.0
    }});

    private final static Map<String, Map<String, FlagCodingArgs>> flagCodings = Collections.unmodifiableMap(new HashMap<String, Map<String, FlagCodingArgs>>() {{
        put("qa_pixel", // QA Pixel
            Collections.unmodifiableMap(new LinkedHashMap<String, FlagCodingArgs>() {{
                ArrayList<FlagCodingArgs> args = new ArrayList<FlagCodingArgs>() {{
                    add(new FlagCodingArgs(0b01 << 0, "designated_fill", "Designated Fill", new Color(255, 0, 0), 0.0f));
                    add(new FlagCodingArgs(0b01 << 1, "dillated_cloud", "Dillated Cloud", new Color(196, 174, 78), 0.0f));
                    add(new FlagCodingArgs(0b01 << 2, "cirrus", "Cirrus", new Color(220, 204, 178), 0.0f));
                    add(new FlagCodingArgs(0b01 << 3, "cloud", "Cloud", new Color(175, 159, 122), 0.0f));
                    add(new FlagCodingArgs(0b01 << 4, "cloud_shadow", "Cloud Shadow", new Color(91, 34, 143), 0.0f));
                    add(new FlagCodingArgs(0b01 << 5, "snow", "Snow/Ice Cover", new Color(255, 255, 153), 0.0f));
                    add(new FlagCodingArgs(0b01 << 6, "clear", "Cloud and Dilated Cloud bits are not set", new Color(192, 192, 192), 0.0f));
                    add(new FlagCodingArgs(0b01 << 7, "water", "WATER (false = land or cloud)", new Color(0, 0, 0), 0.0f));

                    Color cloudColor = new Color(255, 255, 255);
                    add(new FlagCodingArgs(0b11 << 8, 0b00 << 8, "cloud_confidence_not_set", "Cloud confidence level is not set", darker(3, cloudColor), 0.5f));
                    add(new FlagCodingArgs(0b11 << 8, 0b01 << 8, "cloud_confidence_low", "Cloud confidence level is low", darker(2, cloudColor), 0.5f));
                    add(new FlagCodingArgs(0b11 << 8, 0b10 << 8, "cloud_confidence_medium", "Cloud confidence level is medium", darker(1, cloudColor), 0.5f));
                    add(new FlagCodingArgs(0b11 << 8, 0b11 << 8, "cloud_confidence_high", "Cloud confidence level is high", cloudColor, 0.5f));

                    Color cloudShadowColor = new Color(52, 18, 18);
                    add(new FlagCodingArgs(0b11 << 10, 0b00 << 10, "cloud_shadow_confidence_not_set", "Cloud shadow confidence is not set", brighter(3, cloudShadowColor), 0.5f));
                    add(new FlagCodingArgs(0b11 << 10, 0b01 << 10, "cloud_shadow_confidence_low", "Cloud shadow confidence level is low", brighter(2, cloudShadowColor), 0.5f));
                    add(new FlagCodingArgs(0b11 << 10, 0b10 << 10, "cloud_shadow_confidence_medium", "Cloud shadow confidence level is medium", brighter(1, cloudShadowColor), 0.5f));
                    add(new FlagCodingArgs(0b11 << 10, 0b11 << 10, "cloud_shadow_confidence_high", "Cloud shadow confidence level is high", cloudShadowColor, 0.5f));

                    Color snowIceColor = new Color(255, 255, 153);
                    add(new FlagCodingArgs(0b11 << 12, 0b00 << 12, "snow_ice_confidence_not_set", "Snow/ice confidence is not set", darker(3, snowIceColor), 0.5f));
                    add(new FlagCodingArgs(0b11 << 12, 0b01 << 12, "snow_ice_confidence_low", "Snow/ice confidence level is low", darker(2, snowIceColor), 0.5f));
                    add(new FlagCodingArgs(0b11 << 12, 0b10 << 12, "snow_ice_confidence_medium", "Snow/ice confidence level is medium", darker(1, snowIceColor), 0.5f));
                    add(new FlagCodingArgs(0b11 << 12, 0b11 << 12, "snow_ice_confidence_high", "Snow/ice confidence level is high", snowIceColor, 0.5f));

                    Color cirrusColor = new Color(128, 128, 128);
                    add(new FlagCodingArgs(0b11 << 14, 0b00 << 14, "cirrus_confidence_not_set", "Cirrus confidence is not set", brighter(3, cirrusColor), 0.5f));
                    add(new FlagCodingArgs(0b11 << 14, 0b01 << 14, "cirrus_confidence_low", "Cirrus confidence level is low", brighter(2, cirrusColor), 0.5f));
                    add(new FlagCodingArgs(0b11 << 14, 0b10 << 14, "cirrus_confidence_medium", "Cirrus confidence level is medium", brighter(1, cirrusColor), 0.5f));
                    add(new FlagCodingArgs(0b11 << 14, 0b11 << 14, "cirrus_confidence_high", "Cirrus confidence level is high", cirrusColor, 0.5f));
                }};
                for (FlagCodingArgs arg : args) {
                    put(arg.getName(), arg);
                }
            }})
        );
        put("qa_radsat",  // Radiometric Saturation and Terrain Occlusion QA Band
            Collections.unmodifiableMap(new LinkedHashMap<String, FlagCodingArgs>() {{
                ArrayList<FlagCodingArgs> args = new ArrayList<FlagCodingArgs>() {{
                    add(new FlagCodingArgs(0b01 << 0, "radiometric_saturation_b1", "Band 1 data saturation", new Color(255, 0, 0), 0.0f));
                    add(new FlagCodingArgs(0b01 << 1, "radiometric_saturation_b2", "Band 2 data saturation", new Color(255, 0, 0), 0.0f));
                    add(new FlagCodingArgs(0b01 << 2, "radiometric_saturation_b3", "Band 3 data saturation", new Color(255, 0, 0), 0.0f));
                    add(new FlagCodingArgs(0b01 << 3, "radiometric_saturation_b4", "Band 4 data saturation", new Color(255, 0, 0), 0.0f));
                    add(new FlagCodingArgs(0b01 << 4, "radiometric_saturation_b5", "Band 5 data saturation", new Color(255, 0, 0), 0.0f));
                    add(new FlagCodingArgs(0b01 << 5, "radiometric_saturation_b6", "Band 6 data saturation", new Color(255, 0, 0), 0.0f));
                    add(new FlagCodingArgs(0b01 << 6, "radiometric_saturation_b7", "Band 7 data saturation", new Color(255, 0, 0), 0.0f));
                    // position 7 not used
                    add(new FlagCodingArgs(0b01 << 8, "radiometric_saturation_b9", "Band 9 data saturation", new Color(255, 0, 0), 0.0f));
                    // positions 9 and 10 not used
                    add(new FlagCodingArgs(0b01 << 11, "terrain_occlusion", "Terrain occlusion", new Color(255, 0, 0), 0.0f));
                }};
                for (FlagCodingArgs arg : args) {
                    put(arg.getName(), arg);
                }
            }})
        );
        put("sr_qa_aerosol", // SR Aerosol QA File
            Collections.unmodifiableMap(new LinkedHashMap<String, FlagCodingArgs>() {{
                ArrayList<FlagCodingArgs> args = new ArrayList<FlagCodingArgs>() {{
                    add(new FlagCodingArgs(0b01 << 0, "aerosol_fill", "Pixel is fill", new Color(255, 0, 0), 0.0f));
                    add(new FlagCodingArgs(0b01 << 1, "aerosol_retrieval_valid", "Pixel aerosol retrieval is valid", new Color(0, 180, 0), 0.0f));
                    add(new FlagCodingArgs(0b01 << 2, "aerosol_water", "Pixel is water", new Color(46, 77, 145), 0.0f));
                    // positions 3 and 4 not used
                    add(new FlagCodingArgs(0b01 << 5, "aerosol_interpolated", " Pixel is aerosol interpolated", new Color(255, 0, 255), 0.0f));

                    final Color aerosolLevelColor = new Color(89, 60, 5);
                    add(new FlagCodingArgs(0b11 << 6, 0b00 << 6, "aerosol_level_climatology", "Aerosol level climatology", brighter(3, aerosolLevelColor), 0.0f));
                    add(new FlagCodingArgs(0b11 << 6, 0b01 << 6, "aerosol_level_low", "Aerosol level low", brighter(2, aerosolLevelColor), 0.0f));
                    add(new FlagCodingArgs(0b11 << 6, 0b10 << 6, "aerosol_level_medium", "Aerosol level medium", brighter(1, aerosolLevelColor), 0.0f));
                    add(new FlagCodingArgs(0b11 << 6, 0b11 << 6, "aerosol_level_high", "Aerosol level high", aerosolLevelColor, 0.0f));
                }};
                for (FlagCodingArgs arg : args) {
                    put(arg.getName(), arg);
                }
            }})
        );
    }});

    static Color brighter(int times, Color color) {
        for (int i = 0; i < times; i++) {
            color = color.brighter();
        }
        return color;
    }

    static Color darker(int times, Color color) {
        for (int i = 0; i < times; i++) {
            color = color.darker();
        }
        return color;
    }

    private List<Product> bandProducts;
    private VirtualDir virtualDir;
    private String basePath;

    public LandsatGeotiffColl2L2Reader(ProductReaderPlugIn readerPlugin) {
        super(readerPlugin);
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        virtualDir = LandsatGeotiffReaderPlugin.getInput(getInput());

        File mtlFile = getMtlFile();
        productMetadata = OdlParser.parse(mtlFile).getElementAt(0);
        // todo - retrieving the product dimension needs a revision
        Dimension productDim = getReflectanceDim();
        if (productDim == null) {
            productDim = getThermalDim();
        }

        Product product = new Product(getProductName(mtlFile), getProductType(), productDim.width, productDim.height);
        product.setFileLocation(mtlFile);
        product.setAutoGrouping("sr:st:qa");
        final MetadataElement metadataRoot = product.getMetadataRoot();
        metadataRoot.addElement(productMetadata);
        final MetadataElement angleMetadata = getAngleData();
        if (angleMetadata != null) {
            metadataRoot.addElement(angleMetadata);
        }

        ProductData.UTC utcCenter = getCenterTime();
        product.setStartTime(utcCenter);
        product.setEndTime(utcCenter);

        addBands(product);
        setAncillaryVariables(product);
        return product;
    }

    private MetadataElement getAngleData() throws IOException {
        final String[] allFiles = virtualDir.listAllFiles();
        for (String file : allFiles) {
            if (file != null && file.trim().toLowerCase().endsWith("_ang.txt")) {
                final MetadataElement parsed = OdlParser.parse(virtualDir.getFile(basePath + file));
                if (parsed != null && parsed.getNumElements() > 0) {
                    final MetadataElement angleElement = new MetadataElement("Angle Coefficient File");
                    Arrays.stream(parsed.getElements()).forEach(angleElement::addElement);
                    return angleElement;
                }
            }
        }
        return null;
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
        final GeoTiffProductReaderPlugIn geoTiffPlugIn = new GeoTiffProductReaderPlugIn();
        bandProducts = new ArrayList<>();

        for (String tiffFile : getTiffFiles()) {
            final String filenameWithoutExtension = FileUtils.getFilenameWithoutExtension(tiffFile);
            final String key = CollectionTools.getPatternSubtractedFilename(filenameWithoutExtension);
            String bandname = key.substring(1).toLowerCase();
//            if (bandnames.containsKey(key)) {
//                bandname = bandnames.get(key);
//            }
            final File geoTiffFile = virtualDir.getFile(basePath + tiffFile);
            if (!geoTiffFile.exists()) {
                LOG.warning("The expected geotiff file " + geoTiffFile.getAbsolutePath() + " does not exist.");
                continue;
            }
            final ProductReader geoTiffReader = geoTiffPlugIn.createReaderInstance();
            final Product bandProduct = geoTiffReader.readProductNodes(geoTiffFile, null);
            bandProducts.add(bandProduct);
            final Band band = addBandToProduct(bandname, bandProduct.getBandAt(0), product);
            if (bandDescriptions.containsKey(bandname)) {
                band.setDescription(bandDescriptions.get(bandname));
            }
            if (scalingFactors.containsKey(bandname)) {
                band.setScalingFactor(scalingFactors.get(bandname));
            }
            if (addOffsets.containsKey(bandname)) {
                band.setScalingOffset(addOffsets.get(bandname));
            }
            if (wavelengths.containsKey(bandname)) {
                band.setSpectralWavelength(wavelengths.get(bandname));
            }
            if (bandwidths.containsKey(bandname)) {
                band.setSpectralBandwidth(bandwidths.get(bandname));
            }
            if (fillValues.containsKey(bandname)) {
                band.setNoDataValue(fillValues.get(bandname));
                band.setNoDataValueUsed(true);
            }
            if (units.containsKey(bandname)) {
                band.setUnit(units.get(bandname));
            }
            if (flagCodings.containsKey(bandname)) {
                final FlagCoding flagCoding = createFlagCoding(bandname, flagCodings.get(bandname).values());
                band.setSampleCoding(flagCoding);
                product.getFlagCodingGroup().add(flagCoding);
                addMasks(product, band);
            }
        }

        ImageLayout imageLayout = new ImageLayout();
        for (Product bandProduct : bandProducts) {
            if (
                    product.getSceneGeoCoding() == null
                    && product.getSceneRasterWidth() == bandProduct.getSceneRasterWidth()
                    && product.getSceneRasterHeight() == bandProduct.getSceneRasterHeight()
            ) {
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

        for (int i = 0; i < bandProducts.size(); i++) {
            Product bandProduct = bandProducts.get(i);
            Band band = product.getBandAt(i);
            final MultiLevelImage sourceImage = bandProduct.getBandAt(0).getSourceImage();
            band.setSourceImage(sourceImage);
            band.setGeoCoding(bandProduct.getSceneGeoCoding());
        }
    }

    private void setAncillaryVariables(Product product) {
        final Band st_qa = product.getBand("st_qa");
        final Band st_b10 = product.getBand("st_b10");
        if (st_qa == null || st_b10 == null) {
            return;
        }
        st_b10.addAncillaryVariable(st_qa, "uncertainty");
        st_b10.setAncillaryRelations("uncertainty");
    }

    private ArrayList<String> getTiffFiles() throws IOException {
        final ArrayList<String> tiffFiles = new ArrayList<>();
        final MetadataAttribute[] attributes = getProductMetadata().getAttributes();
        for (MetadataAttribute attribute : attributes) {
            final String name = attribute.getName();
            if (name.startsWith("FILE_NAME_")) {
                String filename = attribute.getData().getElemString();
                if (filename.toLowerCase().endsWith(".tif")) {
                    tiffFiles.add(filename);
                }
            }
        }
        return tiffFiles;
    }

    private Band addBandToProduct(String bandName, Band srcBand, Product product) {
        Dimension bandDimension = srcBand.getRasterSize();
        Band band = new Band(bandName, srcBand.getDataType(), bandDimension.width, bandDimension.height);
        product.addBand(band);
        return band;
    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth,
                                          int sourceHeight, int sourceStepX, int sourceStepY,
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

    private Dimension getReflectanceDim() {
        return getDimension("REFLECTIVE_SAMPLES", "REFLECTIVE_LINES");
    }

    private Dimension getThermalDim() {
        return getDimension("THERMAL_SAMPLES", "THERMAL_LINES");
    }

    private Dimension getDimension(String widthAttributeName, String heightAttributeName) {
        MetadataElement metadata = getProjectionAttributes();
        MetadataAttribute widthAttribute = metadata.getAttribute(widthAttributeName);
        MetadataAttribute heightAttribute = metadata.getAttribute(heightAttributeName);
        if (widthAttribute != null && heightAttribute != null) {
            int width = widthAttribute.getData().getElemInt();
            int height = heightAttribute.getData().getElemInt();
            return new Dimension(width, height);
        } else {
            return null;
        }
    }

    private ProductData.UTC getCenterTime() {
        MetadataElement metadata = getImageAttributes();
        String dateString = metadata.getAttributeString("DATE_ACQUIRED");
        String timeString = metadata.getAttributeString("SCENE_CENTER_TIME");

        try {
            if (dateString != null && timeString != null) {
                timeString = timeString.substring(0, 12);
                final DateFormat dateFormat = ProductData.UTC.createDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                final Date date = dateFormat.parse(dateString + " " + timeString);
                String milliSeconds = timeString.substring(timeString.length() - 3);
                return ProductData.UTC.create(date, Long.parseLong(milliSeconds) * 1000);
            }
        } catch (ParseException ignored) {
            // ignore
        }
        return null;
    }

    private String getProductType() {
        final MetadataAttribute spacecraft_id = getImageAttributes().getAttribute("SPACECRAFT_ID");
        final MetadataAttribute sensor_id = getImageAttributes().getAttribute("SENSOR_ID");
        final MetadataAttribute product_type = getProductMetadata().getAttribute("PROCESSING_LEVEL");

        final StringBuilder result = new StringBuilder();
        result.append(spacecraft_id.getData().getElemString());
        result.append("_");
        result.append(sensor_id.getData().getElemString());
        result.append("_");
        result.append(product_type.getData().getElemString());

        return result.toString();
    }

    private MetadataElement getProductMetadata() {
        return productMetadata.getElement("PRODUCT_CONTENTS");
    }

    private MetadataElement getProjectionAttributes() {
        return productMetadata.getElement("PROJECTION_ATTRIBUTES");
    }

    private MetadataElement getImageAttributes() {
        return productMetadata.getElement("IMAGE_ATTRIBUTES");
    }

    private FlagCoding createFlagCoding(String bandName, Collection<FlagCodingArgs> flagCodingArgs) {
        final FlagCoding flagCoding = new FlagCoding(bandName);
        for (FlagCodingArgs a : flagCodingArgs) {
            if (a.hasFlagValue()) {
                flagCoding.addFlag(a.getName(), a.getFlagMask(), a.getFlagValue(), a.getDescription());
            } else {
                flagCoding.addFlag(a.getName(), a.getFlagMask(), a.getDescription());
            }
        }
        return flagCoding;
    }

    private void addMasks(Product product, Band flagBand) {
        final String bandName = flagBand.getName();
        final FlagCoding flagCoding = flagBand.getFlagCoding();
        final MetadataAttribute[] flagAttributes = flagCoding.getAttributes();
        for (MetadataAttribute flagAttribute : flagAttributes) {
            final String flagName = flagAttribute.getName();
            final String expression = bandName + "." + flagName;
            final String description = flagAttribute.getDescription();
            final Color color = getColorForMask(bandName, flagName);
            final float transparency = getTransparencyForMask(bandName, flagName);
            product.addMask(flagName, expression, description, color, transparency);
        }
    }

    private Color getColorForMask(String bandName, String flagName) {
        return flagCodings.get(bandName).get(flagName).getColor();
    }

    private float getTransparencyForMask(String bandName, String flagName) {
        return flagCodings.get(bandName).get(flagName).getTransparency();
    }

    private static class FlagCodingArgs {
        final int flagMask;
        final Integer flagValue;
        final String name;
        final String description;
        private Color color;
        private float transparency;

        FlagCodingArgs(int flagMask, String name, String description, Color color, float transparency) {
            this(flagMask, null, name, description, color, transparency);
        }

        FlagCodingArgs(int flagMask, Integer flagValue, String name, String description, Color color, float transparency) {
            this.flagMask = flagMask;
            this.flagValue = flagValue;
            this.name = name;
            this.description = description;
            this.color = color;
            this.transparency = transparency;
        }

        public boolean hasFlagValue() {
            return flagValue != null;
        }

        public int getFlagMask() {
            return flagMask;
        }

        public int getFlagValue() {
            return flagValue;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public Color getColor() {
            return color;
        }

        public float getTransparency() {
            return transparency;
        }
    }
}