package org.esa.s3tbx.slstr.pdu.stitching;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import org.esa.s3tbx.dataio.s3.util.ColorProvider;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProducts;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.io.WildcardMatcher;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Tonio Fincke
 */
@OperatorMetadata(alias = "PduStitching",
        category = "Optical",
        version = "1.0",
        authors = "Tonio Fincke",
        copyright = "Copyright (C) 2015 by Brockmann Consult (info@brockmann-consult.de)",
        description = "Stitches multiple SLSTR L1B product dissemination units (PDUs) of the same orbit to a single product.",
        autoWriteDisabled = true)
public class PDUStitchingOp extends Operator {

    public static final String SLSTR_L1B_NAME_PATTERN = "S3.?_SL_1_RBT_.*(.SEN3)?";
    public static final ImageSize NULL_IMAGE_SIZE = new ImageSize("null", 0, 0, 0, 0);

    @SourceProducts(description = "The product dissemination units to be stitched together. Must all be of type 'SLSTR L1B'.\n" +
            "If not given, the parameter 'sourceProductPaths' must be provided.")
    Product[] sourceProducts;

    @Parameter(description = "A comma-separated list of file paths specifying the product dissemination units.\n" +
            "Each path may contain the wildcards '**' (matches recursively any directory),\n" +
            "'*' (matches any character sequence in path names) and\n" +
            "'?' (matches any single character).\n" +
            "If not given, the parameter 'sourceProducts' must be provided.")
    private String[] sourceProductPaths;

    @Parameter(description = "The directory to which the stitched product shall be written.\n" +
            "Within this directory, a folder of the SLSTR L1B naming format will be created.\n" +
            "If no target directory is given, the product will be written to the user directory.")
    private File targetDir;

    private File[] files;

    @Override
    public void initialize() throws OperatorException {
        if ((sourceProducts == null || sourceProducts.length == 0) &&
                (sourceProductPaths == null || sourceProductPaths.length == 0)) {
            throw new OperatorException("Either 'sourceProducts' pr 'sourceProductPaths' must be set");
        }
        final Set<File> filesByProduct = getSourceProductsFileSet(sourceProducts);
        final Set<File> filesByPath = getSourceProductsPathFileSet(sourceProductPaths, getLogger());
        filesByPath.addAll(filesByProduct);
        files = filesByPath.toArray(new File[filesByPath.size()]);
        if (files.length == 0) {
            throw new OperatorException("No PDUs to be stitched could be found.");
        }
        if (targetDir == null || StringUtils.isNullOrEmpty(targetDir.getAbsolutePath())) {
            targetDir = new File(SystemUtils.getUserHomeDir().getPath());
        }
        try {
            setTargetProduct(createTargetProduct(files));
        } catch (PDUStitchingException | IOException e) {
            throw new OperatorException("Cannot create product: " + e.getMessage());
        }
    }

    private Product createTargetProduct(File[] slstrProductFiles) throws PDUStitchingException, IOException {
        Validator.validateSlstrProductFiles(slstrProductFiles);
        SlstrPduStitcher.SlstrNameDecomposition[] slstrNameDecompositions =
                new SlstrPduStitcher.SlstrNameDecomposition[slstrProductFiles.length];
        Document[] manifestDocuments = new Document[slstrProductFiles.length];
        final Date now = Calendar.getInstance().getTime();
        List<String> ncFileNames = new ArrayList<>();
        Map<String, ImageSize[]> idToImageSizes = new HashMap<>();
        List<ProductData.UTC> startTimes = new ArrayList<>();
        List<ProductData.UTC> stopTimes = new ArrayList<>();
        for (int i = 0; i < slstrProductFiles.length; i++) {
                slstrNameDecompositions[i] =
                        SlstrPduStitcher.decomposeSlstrName(slstrProductFiles[i].getParentFile().getName());
                manifestDocuments[i] = SlstrPduStitcher.createXmlDocument(new FileInputStream(slstrProductFiles[i]));
            final ImageSize[] imageSizes = ImageSizeHandler.extractImageSizes(manifestDocuments[i]);
            for (ImageSize imageSize : imageSizes) {
                if (idToImageSizes.containsKey(imageSize.getIdentifier())) {
                    idToImageSizes.get(imageSize.getIdentifier())[i] = imageSize;
                } else {
                    final ImageSize[] mapImageSizes = new ImageSize[slstrProductFiles.length];
                    mapImageSizes[i] = imageSize;
                    idToImageSizes.put(imageSize.getIdentifier(), mapImageSizes);
                }
            }
            startTimes.add(getStartTime(manifestDocuments[i]));
            stopTimes.add(getStopTime(manifestDocuments[i]));
            SlstrPduStitcher.collectFiles(ncFileNames, manifestDocuments[i]);
        }
        Validator.validateOrbitReference(manifestDocuments);
        Validator.validateAdjacency(manifestDocuments);
        final String stitchedProductFileName =
                SlstrPduStitcher.createParentDirectoryNameOfStitchedFile(slstrNameDecompositions, now);
        Map<String, ImageSize> idToTargetImageSize = new HashMap<>();
        for (String id : idToImageSizes.keySet()) {
            idToTargetImageSize.put(id, ImageSizeHandler.createTargetImageSize(idToImageSizes.get(id)));
        }
        Product targetProduct = new Product(stitchedProductFileName, "SL_1_RBT");
        targetProduct.setStartTime(getStartTime(startTimes));
        targetProduct.setEndTime(getStopTime(stopTimes));
        String referenceEnding = getReferenceEnding(idToTargetImageSize);
        ImageSize referenceImageSize = idToTargetImageSize.get(referenceEnding);
        ColorProvider colorProvider = new ColorProvider();
        for (final String ncFileName : ncFileNames) {
            String[] splitFileName = ncFileName.split("/");
            String id = ncFileName.substring(ncFileName.length() - 5, ncFileName.length() - 3);
            if (id.equals("tx")) {
                id = "tn";
            }
            ImageSize targetImageSize = idToTargetImageSize.get(id);
            if (targetImageSize == null) {
                targetImageSize = NULL_IMAGE_SIZE;
            }
            SlstrRasterDateNodeAdder.addRasterDataNodes(targetProduct,
                    splitFileName[splitFileName.length - 1], targetImageSize, colorProvider,
                    referenceImageSize, referenceEnding);
        }
        setGeoCoding(targetProduct);
        setAutoGrouping(targetProduct);
        return targetProduct;
    }

    private String getReferenceEnding(Map<String, ImageSize> idToTargetImageSize) {
        String[] preferedReferences =
                new String[]{"an", "bn", "cn", "in", "fn", "ao", "bo", "co", "io", "fo", "tx", "tn"};
        for (String preferedReference : preferedReferences) {
            if (idToTargetImageSize.containsKey(preferedReference)) {
                return preferedReference;
            }
        }
        return null;
    }

    private ProductData.UTC getStartTime(List<ProductData.UTC> startTimes) {
        ProductData.UTC startTime = null;
        Calendar startCalendar = new GregorianCalendar(3000, 1, 1);
        for (ProductData.UTC time : startTimes) {
            if (time != null && time.getAsCalendar().before(startCalendar)) {
                startTime = time;
                startCalendar = time.getAsCalendar();
            }
        }
        return startTime;
    }

    private ProductData.UTC getStopTime(List<ProductData.UTC> stopTimes) {
        ProductData.UTC stopTime = null;
        Calendar stopCalendar = new GregorianCalendar(2000, 1, 1);
        for (ProductData.UTC time : stopTimes) {
            if (time != null && time.getAsCalendar().after(stopCalendar)) {
                stopTime = time;
                stopCalendar = time.getAsCalendar();
            }
        }
        return stopTime;
    }

    private ProductData.UTC getStartTime(Document manifest) {
        return getTime(manifest, "sentinel-safe:startTime");
    }

    private ProductData.UTC getStopTime(Document manifest) {
        return getTime(manifest, "sentinel-safe:stopTime");
    }

    private ProductData.UTC getTime(Document manifest, String timeId) {
        NodeList times = manifest.getElementsByTagName(timeId);
        if (times.getLength() == 0) {
            return null;
        }
        String time = times.item(0).getTextContent();
        try {
            if (!Character.isDigit(time.charAt(time.length() - 1))) {
                time = time.substring(0, time.length() - 1);
            }
            return ProductData.UTC.parse(time, "yyyy-MM-dd'T'HH:mm:ss");
        } catch (ParseException ignored) {
            return null;
        }
    }

    private void setGeoCoding(Product targetProduct) {
        TiePointGrid latGrid = null;
        TiePointGrid lonGrid = null;
        for (final TiePointGrid grid : targetProduct.getTiePointGrids()) {
            if (latGrid == null && grid.getName().endsWith("latitude_tx")) {
                latGrid = grid;
            }
            if (lonGrid == null && grid.getName().endsWith("longitude_tx")) {
                lonGrid = grid;
            }
        }
        if (latGrid != null && lonGrid != null) {
            targetProduct.setSceneGeoCoding(new TiePointGeoCoding(latGrid, lonGrid));
        }
    }

    private void setAutoGrouping(Product targetProduct) {
        targetProduct.setAutoGrouping(
                "F*BT_*n:F*exception_*n:" +
                        "F*BT_*o:F*exception_*o:" +
                        "S*BT_in:S*exception_in:" +
                        "S*BT_io:S*exception_io:" +
                        "radiance_an:S*exception_an:" +
                        "radiance_ao:S*exception_ao:" +
                        "radiance_bn:S*exception_bn:" +
                        "radiance_bo:S*exception_bo:" +
                        "radiance_cn:S*exception_cn:" +
                        "radiance_co:S*exception_co:" +
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
                        "F*_exception_*n_*:F*_exception_*o_*");
    }

    @Override
    public void doExecute(ProgressMonitor pm) throws OperatorException {
        try {
            Product targetProduct = getTargetProduct();
            int workload = targetProduct.getNumBands() + targetProduct.getNumTiePointGrids();
            pm.beginTask("Stitching SLSTR L1B Product Dissemination Units", 4 * workload);
            File stitchedSlstrL1BFile = SlstrPduStitcher.createStitchedSlstrL1BFile(targetDir, files,
                    targetProduct.getName(), new SubProgressMonitor(pm, 3 * workload));
            if (stitchedSlstrL1BFile == null) {
                throw new OperatorException("SLSTR L1B manifest file is missing");
            }
            pm.setSubTaskName("Adjusting Bands");
            Product stitchedProduct = ProductIO.readProduct(stitchedSlstrL1BFile);
            targetProduct.setFileLocation(stitchedProduct.getFileLocation());
            ProductUtils.copyMetadata(stitchedProduct, targetProduct);
            ProductUtils.copyVectorData(stitchedProduct, targetProduct);
            targetProduct.setSceneTimeCoding(stitchedProduct.getSceneTimeCoding());
            for (Band targetBand : targetProduct.getBands()) {
                Band bandFromStitched = stitchedProduct.getBand(targetBand.getName());
                if (bandFromStitched != null) {
                    ProductUtils.copyRasterDataNodeProperties(bandFromStitched, targetBand);
                    ProductUtils.copyImageGeometry(bandFromStitched, targetBand, true);
                    targetBand.setSourceImage(bandFromStitched.getSourceImage());
                } else {
                    targetProduct.removeBand(targetBand);
                }
                pm.worked(1);
            }
            pm.setSubTaskName("Adjusting Tie-Point Grids");
            for (TiePointGrid targetTPG : targetProduct.getTiePointGrids()) {
                TiePointGrid tpgFromStitched = stitchedProduct.getTiePointGrid(targetTPG.getName());
                if (tpgFromStitched != null) {
                    ProductUtils.copyRasterDataNodeProperties(tpgFromStitched, targetTPG);
                    ProductUtils.copyImageGeometry(tpgFromStitched, targetTPG, true);
                    targetTPG.setData(ProductData.createInstance(tpgFromStitched.getTiePoints()));
                } else {
                    targetProduct.removeTiePointGrid(targetTPG);
                }
                pm.worked(1);
            }
            for (String maskName : targetProduct.getMaskGroup().getNodeNames()) {
                if (!stitchedProduct.getMaskGroup().contains(maskName)) {
                    targetProduct.getMaskGroup().remove(targetProduct.getMaskGroup().get(maskName));
                }
            }
        } catch (Exception e) {
            throw new OperatorException(e.getMessage(), e);
        } finally {
            pm.done();
        }
    }

    private static Set<File> getSourceProductsFileSet(Product[] sourceProducts) {
        Set<File> sourceProductFileSet = new TreeSet<>();
        if (sourceProducts != null) {
            for (Product sourceProduct : sourceProducts) {
                sourceProductFileSet.add(sourceProduct.getFileLocation());
            }
        }
        return sourceProductFileSet;
    }

    //todo copied this from pixexop - move to utililty method? - tf 20151117
    public static Set<File> getSourceProductsPathFileSet(String[] sourceProductPaths, Logger logger) {
        Set<File> sourceProductFileSet = new TreeSet<>();
        String[] paths = trimSourceProductPaths(sourceProductPaths);
        if (paths != null && paths.length != 0) {
            for (String path : paths) {
                try {
                    WildcardMatcher.glob(path, sourceProductFileSet);
                } catch (IOException e) {
                    logger.severe("I/O problem occurred while scanning source product files: " + e.getMessage());
                }
            }
            if (sourceProductFileSet.isEmpty()) {
                logger.log(Level.WARNING, "No valid source product path found.");
            }
        }
        return sourceProductFileSet;
    }

    //todo copied this from pixexop - move to utililty method? - tf 20151117
    private static String[] trimSourceProductPaths(String[] sourceProductPaths) {
        final String[] paths;
        if (sourceProductPaths != null) {
            paths = sourceProductPaths.clone();
        } else {
            paths = null;
        }
        if (paths != null) {
            for (int i = 0; i < paths.length; i++) {
                paths[i] = paths[i].trim();
            }
        }
        return paths;
    }

    public static class Spi extends OperatorSpi {
        public Spi() {
            super(PDUStitchingOp.class);
        }
    }

}
