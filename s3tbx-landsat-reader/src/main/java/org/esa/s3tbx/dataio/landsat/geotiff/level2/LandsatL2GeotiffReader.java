package org.esa.s3tbx.dataio.landsat.geotiff.level2;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.VirtualDir;
import com.bc.ceres.glevel.MultiLevelImage;
import org.esa.s3tbx.dataio.landsat.geotiff.LandsatTypeInfo;
import org.esa.s3tbx.dataio.landsat.metadata.XmlMetadataParser;
import org.esa.s3tbx.dataio.landsat.metadata.XmlMetadataParserFactory;
import org.esa.snap.core.dataio.AbstractProductReader;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.dataio.geotiff.GeoTiffProductReaderPlugIn;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;


/**
 * Created by obarrile on 07/02/2019.
 */
public class LandsatL2GeotiffReader extends AbstractProductReader {

    private VirtualDir virtualDir;
    protected final Logger logger;
    private String basePath;
    protected LandsatLevel2Metadata metadata = null;
    protected LandsatL2Saturation l2saturation = null;
    protected LandsatL2QA l2qa = null;
    protected LandsatL2Aerosol l2aerosol = null;
    protected LandsatL2Cloud l2cloud = null;

    private List<Product> bandProducts;

    public LandsatL2GeotiffReader(ProductReaderPlugIn readerPlugin) {
        super(readerPlugin);
        logger = SystemUtils.LOG;
        bandProducts = new ArrayList<>();
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {

        try {
            virtualDir = LandsatL2GeotiffReaderPlugin.getInput(getInput());
            if (virtualDir == null) {
                throw new FileNotFoundException(getInput().toString());
            }
        } catch (IOException e) {
            throw new FileNotFoundException(getInput().toString());
        }

        //initialize basepath
        initializeBasePath();

        //create metadata
        XmlMetadataParserFactory.registerParser(LandsatLevel2Metadata.class, new XmlMetadataParser<>(LandsatLevel2Metadata.class));


        InputStream metadataInputStream = getInputStreamXml();
        if (metadataInputStream == null) {
            throw new IOException(String.format("Unable to read metadata file from product: %s", getInput().toString()));
        }

        try {
            metadata = (LandsatLevel2Metadata) XmlMetadataParserFactory.getParser(LandsatLevel2Metadata.class).parse(metadataInputStream);
        } catch (Exception e) {
            throw new IOException(String.format("Unable to parse metadata file: %s", getInput().toString()));
        }

        //close stream
        if (metadataInputStream != null) try {
            metadataInputStream.close();
        } catch (IOException e) {
            // swallowed exception
        }

        l2saturation = LandsatL2SaturationFactory.create(metadata);
        l2qa = LandsatL2QAFactory.create(metadata);
        l2aerosol = LandsatL2AerosolFactory.create(metadata);
        l2cloud = LandsatL2CloudFactory.create(metadata);

        //create product
        Product product = new Product(metadata.getProductName(),
                                      "LANDSAT_LEVEL2",
                                      metadata.getRasterWidth(),
                                      metadata.getRasterHeight());
        product.setDescription(metadata.getProductDescription());

        product.getMetadataRoot().addElement(metadata.getRootElement());

        //set File Location
        File fileLocation = null;
        try {
            // in case of zip products, getTempDir returns the temporary location of the uncompressed product
            fileLocation = virtualDir.getTempDir();
        } catch (IOException e) {
            logger.warning(e.getMessage());
        }
        if (fileLocation == null) {
            fileLocation = new File(virtualDir.getBasePath());
        }
        product.setFileLocation(fileLocation);

        //time
        ProductData.UTC utcCenter = metadata.getCenterTime();
        product.setStartTime(utcCenter);
        product.setEndTime(utcCenter);

        //add bands
        for (String imageName : metadata.getRasterFileNames()) {
            if(metadata.isReflectanceBand(imageName)) {
                addReflectanceImage(product,imageName);
            } else if (metadata.isSaturationBand(imageName) && l2saturation != null) {
                addSaturationBand(product,imageName);
            } else if (metadata.isAerosolBand(imageName) && l2aerosol != null) {
                addAerosolBand(product,imageName);
            } else if (metadata.isQualityBand(imageName) && l2qa != null) {
                addQualityBand(product,imageName);
            } else if (metadata.isAtmosBand(imageName)) {
                addAtmosImage(product,imageName);
            } else if (metadata.isCloudBand(imageName) && l2cloud != null) {
                addCloudBand(product,imageName);
            } else {
                logger.warning(String.format("Unknown type of band: %s", imageName));
            }

        }
        return product;
    }



    @Override
    protected void readBandRasterDataImpl(int i, int i1, int i2, int i3, int i4, int i5, Band band, int i6, int i7, int i8, int i9, ProductData productData, ProgressMonitor progressMonitor) throws IOException {

    }
    private InputStream getInputStreamXml() {
        String xmlFile = "";
        try {
            String[] files = null;
            files = virtualDir.listAllFiles();
            for (String file : files) {
                if (file.endsWith(".xml") && LandsatTypeInfo.isLandsatL2(file)) { //add another check?
                    xmlFile = file;
                    return virtualDir.getInputStream(file);
                }
            }
        } catch (IOException e) {
            logger.warning(String.format("Unable to get input stream: %s", xmlFile));
        }
        return null;
    }

    private void addReflectanceImage (Product product, String fileName) throws IOException {
        final GeoTiffProductReaderPlugIn plugIn = new GeoTiffProductReaderPlugIn();
        File bandFile = virtualDir.getFile(basePath + fileName);
        ProductReader productReader = plugIn.createReaderInstance();
        Product bandProduct = productReader.readProductNodes(bandFile, null);
        if (bandProduct != null) {
            bandProducts.add(bandProduct);
            Band srcBand = bandProduct.getBandAt(0);
            String bandName = metadata.getBandName(fileName);
            Band band = addBandToProduct(bandName, srcBand, product);
            band.setScalingFactor(metadata.getScalingFactor(fileName));
            band.setScalingOffset(metadata.getScalingOffset(fileName));

            band.setNoDataValue(metadata.getFillValue(fileName));
            band.setNoDataValueUsed(true);

            band.setSpectralWavelength(metadata.getWavelength(fileName));
            band.setSpectralBandwidth(metadata.getBandwidth(fileName));

            band.setDescription(metadata.getBandDescription(fileName));
            band.setUnit(" ");

            final MultiLevelImage sourceImage = bandProduct.getBandAt(0).getSourceImage();
            band.setSourceImage(sourceImage);
            band.setGeoCoding(bandProduct.getSceneGeoCoding());
        }
    }

    private void addAtmosImage (Product product, String fileName) throws IOException {
        final GeoTiffProductReaderPlugIn plugIn = new GeoTiffProductReaderPlugIn();
        File bandFile = virtualDir.getFile(basePath + fileName);
        ProductReader productReader = plugIn.createReaderInstance();
        Product bandProduct = productReader.readProductNodes(bandFile, null);
        if (bandProduct != null) {
            bandProducts.add(bandProduct);
            Band srcBand = bandProduct.getBandAt(0);
            String bandName = metadata.getBandName(fileName);
            Band band = addBandToProduct(bandName, srcBand, product);
            band.setScalingFactor(metadata.getScalingFactor(fileName));
            band.setScalingOffset(metadata.getScalingOffset(fileName));

            band.setNoDataValue(metadata.getFillValue(fileName));
            band.setNoDataValueUsed(true);

            band.setDescription(metadata.getBandDescription(fileName));
            band.setUnit(" ");

            final MultiLevelImage sourceImage = bandProduct.getBandAt(0).getSourceImage();
            band.setSourceImage(sourceImage);
            band.setGeoCoding(bandProduct.getSceneGeoCoding());
        }
    }

    private void addSaturationBand (Product product, String fileName) throws IOException {
        final GeoTiffProductReaderPlugIn plugIn = new GeoTiffProductReaderPlugIn();
        File bandFile = virtualDir.getFile(basePath + fileName);
        ProductReader productReader = plugIn.createReaderInstance();
        Product bandProduct = productReader.readProductNodes(bandFile, null);
        if (bandProduct != null) {
            bandProducts.add(bandProduct);
            Band srcBand = bandProduct.getBandAt(0);
            String bandName = "saturation";

            Band band = addBandToProduct(bandName, srcBand, product);
            band.setDescription("Saturation Band");

            band.setNoDataValue(metadata.getFillValue(fileName));
            band.setNoDataValueUsed(true);

            FlagCoding flagCoding = l2saturation.createFlagCoding(bandName);
            band.setSampleCoding(flagCoding);
            product.getFlagCodingGroup().add(flagCoding);
            List<Mask> masks;
            masks = l2saturation.createMasks(band.getRasterWidth(), band.getRasterHeight());
            for (Mask mask : masks) {
                product.getMaskGroup().add(mask);
            }
            final MultiLevelImage sourceImage = bandProduct.getBandAt(0).getSourceImage();
            band.setSourceImage(sourceImage);
            band.setGeoCoding(bandProduct.getSceneGeoCoding());
        }
    }

    private void addQualityBand(Product product, String imageName) throws IOException {
        final GeoTiffProductReaderPlugIn plugIn = new GeoTiffProductReaderPlugIn();
        File bandFile = virtualDir.getFile(basePath + imageName);
        ProductReader productReader = plugIn.createReaderInstance();
        Product bandProduct = productReader.readProductNodes(bandFile, null);
        if (bandProduct != null) {
            bandProducts.add(bandProduct);
            Band srcBand = bandProduct.getBandAt(0);
            String bandName = "pixel_qa";

            Band band = addBandToProduct(bandName, srcBand, product);
            band.setDescription("Quality Band");

            band.setNoDataValue(metadata.getFillValue(imageName));
            band.setNoDataValueUsed(true);

            FlagCoding flagCoding = l2qa.createFlagCoding();
            band.setSampleCoding(flagCoding);
            product.getFlagCodingGroup().add(flagCoding);
            List<Mask> masks;
            masks = l2qa.createMasks(band.getRasterWidth(), band.getRasterHeight());
            for (Mask mask : masks) {
                product.getMaskGroup().add(mask);
            }
            final MultiLevelImage sourceImage = bandProduct.getBandAt(0).getSourceImage();
            band.setSourceImage(sourceImage);
            band.setGeoCoding(bandProduct.getSceneGeoCoding());
        }
    }

    private void addCloudBand(Product product, String imageName) throws IOException {
        final GeoTiffProductReaderPlugIn plugIn = new GeoTiffProductReaderPlugIn();
        File bandFile = virtualDir.getFile(basePath + imageName);
        ProductReader productReader = plugIn.createReaderInstance();
        Product bandProduct = productReader.readProductNodes(bandFile, null);
        if (bandProduct != null) {
            bandProducts.add(bandProduct);
            Band srcBand = bandProduct.getBandAt(0);
            String bandName = "cloud_qa";

            Band band = addBandToProduct(bandName, srcBand, product);
            band.setDescription("Cloud QA Band");

            band.setNoDataValue(metadata.getFillValue(imageName));
            band.setNoDataValueUsed(true);

            FlagCoding flagCoding = l2cloud.createFlagCoding();
            band.setSampleCoding(flagCoding);
            product.getFlagCodingGroup().add(flagCoding);
            List<Mask> masks;
            masks = l2cloud.createMasks(band.getRasterWidth(), band.getRasterHeight());
            for (Mask mask : masks) {
                product.getMaskGroup().add(mask);
            }
            final MultiLevelImage sourceImage = bandProduct.getBandAt(0).getSourceImage();
            band.setSourceImage(sourceImage);
            band.setGeoCoding(bandProduct.getSceneGeoCoding());
        }
    }

    private void addAerosolBand(Product product, String imageName) throws IOException {
        final GeoTiffProductReaderPlugIn plugIn = new GeoTiffProductReaderPlugIn();
        File bandFile = virtualDir.getFile(basePath + imageName);
        ProductReader productReader = plugIn.createReaderInstance();
        Product bandProduct = productReader.readProductNodes(bandFile, null);
        if (bandProduct != null) {
            bandProducts.add(bandProduct);
            Band srcBand = bandProduct.getBandAt(0);
            String bandName = "aerosol_flags";

            Band band = addBandToProduct(bandName, srcBand, product);
            band.setDescription("Aerosol Mask");

            band.setNoDataValue(metadata.getFillValue(imageName));
            band.setNoDataValueUsed(true);

            FlagCoding flagCoding = l2aerosol.createFlagCoding();
            band.setSampleCoding(flagCoding);
            product.getFlagCodingGroup().add(flagCoding);
            List<Mask> masks;
            masks = l2aerosol.createMasks(band.getRasterWidth(), band.getRasterHeight());
            for (Mask mask : masks) {
                product.getMaskGroup().add(mask);
            }
            final MultiLevelImage sourceImage = bandProduct.getBandAt(0).getSourceImage();
            band.setSourceImage(sourceImage);
            band.setGeoCoding(bandProduct.getSceneGeoCoding());
        }
    }

    protected void initializeBasePath() throws IOException {
        File fileInput = LandsatL2GeotiffReaderPlugin.getFileInput(getInput());
        if (fileInput != null && fileInput.exists() && LandsatL2GeotiffReaderPlugin.isMetadataFilename(fileInput.getName())) {
            basePath = "";
            return;
        } else {
            String[] fileList = virtualDir.listAllFiles();
            for (String filePath : fileList) {
                if (LandsatL2GeotiffReaderPlugin.isMetadataFilename(filePath)) {
                    basePath = new File(filePath).getParent();
                    basePath = basePath == null ? "" : basePath + "/";
                    return;
                }
            }
        }
    }

    private Band addBandToProduct(String bandName, Band srcBand, Product product) {
        Band band = new Band(bandName, srcBand.getDataType(), srcBand.getRasterWidth(), srcBand.getRasterHeight());
        product.addBand(band);
        return band;
    }
}
