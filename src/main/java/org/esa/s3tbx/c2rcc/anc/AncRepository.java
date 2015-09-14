package org.esa.s3tbx.c2rcc.anc;

import org.esa.snap.framework.dataio.ProductIO;
import org.esa.snap.framework.datamodel.Product;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AncRepository {

    private final File repsitoryRoot;
    private final AncDownloader ancDownloader;
    private final Map<String, Product> productMap;

    public AncRepository(File repsitoryRoot) {
        this(repsitoryRoot, null);
    }

    public AncRepository(File repsitoryRoot, AncDownloader ancDownloader) {
        this.repsitoryRoot = repsitoryRoot;
        this.ancDownloader = ancDownloader;
        productMap = new HashMap<>();
    }

    public Product getProduct(String[] filenames) throws IOException {
        Product product;
        product = findProductInMap(filenames);
        if (product != null) {
            return product;
        }
        final File[] productFiles = createProductFiles(filenames);
        product = findProductInArchive(productFiles);
        if (product != null) {
            return product;
        }
        if (ancDownloader == null) {
            return null;
        }
        File productFile = ancDownloader.download(productFiles);
        if (productFile == null) {
            return null;
        }
        return loadProduct(productFile);
    }

    private Product findProductInArchive(File[] productFiles) throws IOException {
        for (File productFile : productFiles) {
            if (productFile.exists()) {
                return loadProduct(productFile);
            }
        }
        return null;
    }

    private Product loadProduct(File productFile) throws IOException {
        final String filename = productFile.getName();
        final Product product = ProductIO.readProduct(productFile);
        if (product != null) {
            productMap.put(filename, product);
        }
        return product;
    }

    private File[] createProductFiles(String[] filenames) {
        final File[] productFiles = new File[filenames.length];
        for (int i = 0; i < filenames.length; i++) {
            String filename = filenames[i];
            if (!filename.matches("[N][12][09]\\d{2}[0-3]\\d{2}[0-2]\\d_.*")) {
                throw new IllegalArgumentException(filename + " is not a valid ancillary filename.");
            }
            final String year = filename.substring(1, 5);
            final String doy = filename.substring(5, 8);
            final File doyPath = new File(new File(repsitoryRoot, year), doy);
            final File productFile = new File(doyPath, filename);
            productFiles[i] = productFile;
        }
        return productFiles;
    }

    private Product findProductInMap(String[] filenames) {
        for (String filename : filenames) {
            if (productMap.containsKey(filename)) {
                return productMap.get(filename);
            }
        }
        return null;
    }

    public void dispose() {
        for (Product product : productMap.values()) {
            product.dispose();
        }
    }
}