package org.esa.s3tbx.slstr.pdu.stitching;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProducts;
import org.esa.snap.core.jexp.Term;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.io.WildcardMatcher;
import org.w3c.dom.Document;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

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
        setDummyTargetProduct();
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
    }

    private void createTargetProduct(File[] slstrProductFiles) {
        if (slstrProductFiles.length == 0) {
            throw new IllegalArgumentException("No product files provided");
        }
        final Pattern slstrNamePattern = Pattern.compile(SLSTR_L1B_NAME_PATTERN);
        for (int i = 0; i < slstrProductFiles.length; i++) {
            if (slstrProductFiles[i] == null) {
                throw new OperatorException("File must not be null");
            }
            if (!slstrProductFiles[i].getName().equals("xfdumanifest.xml")) {
                slstrProductFiles[i] = new File(slstrProductFiles[i], "xfdumanifest.xml");
            }
            if (!slstrProductFiles[i].getName().equals("xfdumanifest.xml") ||
                    slstrProductFiles[i].getParentFile() == null ||
                    !slstrNamePattern.matcher(slstrProductFiles[i].getParentFile().getName()).matches()) {
                throw new IllegalArgumentException("The PDU Stitcher only supports SLSTR L1B products");
            }
        }
        SlstrPduStitcher.SlstrNameDecomposition[] slstrNameDecompositions = new SlstrPduStitcher.SlstrNameDecomposition[slstrProductFiles.length];
        Document[] manifestDocuments = new Document[slstrProductFiles.length];
        final Date now = Calendar.getInstance().getTime();
        List<String> ncFileNames = new ArrayList<>();
        Map<String, ImageSize[]> idToImageSizes = new HashMap<>();
        for (int i = 0; i < slstrProductFiles.length; i++) {
            try {
                slstrNameDecompositions[i] = SlstrPduStitcher.decomposeSlstrName(slstrProductFiles[i].getParentFile().getName());
                manifestDocuments[i] = SlstrPduStitcher.createXmlDocument(new FileInputStream(slstrProductFiles[i]));
            } catch (PDUStitchingException | IOException e) {
                throw new OperatorException(e.getMessage());
            }
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
            SlstrPduStitcher.collectFiles(ncFileNames, manifestDocuments[i]);
        }
        final String stitchedProductFileName =
                SlstrPduStitcher.createParentDirectoryNameOfStitchedFile(slstrNameDecompositions, now);
        Map<String, ImageSize> idToTargetImageSize = new HashMap<>();
        for (String id : idToImageSizes.keySet()) {
            idToTargetImageSize.put(id, ImageSizeHandler.createTargetImageSize(idToImageSizes.get(id)));
        }
        Product targetProduct = new Product(stitchedProductFileName, "SL_1_RBT___");
        for (int i = 0; i < ncFileNames.size(); i++) {
            final String ncFileName = ncFileNames.get(i);
            String[] splitFileName = ncFileName.split("/");
            final String displayFileName = splitFileName[splitFileName.length - 1];
            String id = ncFileName.substring(ncFileName.length() - 5, ncFileName.length() - 3);
            if (id.equals("tx")) {
                id = "tn";
            }
            ImageSize targetImageSize = idToTargetImageSize.get(id);
            if (targetImageSize == null) {
                targetImageSize = NULL_IMAGE_SIZE;
            }
            Band targetBand = new Band(displayFileName, , targetImageSize.getRows(), targetImageSize.getColumns());
            targetProduct.addBand(targetBand);
        }
    }

    @Override
    public void doExecute(ProgressMonitor pm) throws OperatorException {
        try {
            SlstrPduStitcher.createStitchedSlstrL1BFile(targetDir, files, pm);
        } catch (Exception e) {
            throw new OperatorException(e.getMessage(), e);
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

    private void setDummyTargetProduct() {
        final Product product = new Product("dummy", "dummy", 2, 2);
        product.addBand("dummy", ProductData.TYPE_INT8);
        setTargetProduct(product);
    }

    public static class Spi extends OperatorSpi {
        public Spi() {
            super(PDUStitchingOp.class);
        }
    }

}
