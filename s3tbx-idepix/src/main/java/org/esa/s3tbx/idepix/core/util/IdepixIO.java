package org.esa.s3tbx.idepix.core.util;


import org.esa.s3tbx.idepix.algorithms.viirs.ViirsConstants;
import org.esa.s3tbx.idepix.core.AlgorithmSelector;
import org.esa.s3tbx.idepix.core.IdepixConstants;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.dataio.envisat.EnvisatConstants;

/**
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
public class IdepixIO {

    private IdepixIO() {
    }

    /**
     * creates a new product with the same size
     **/
    public static Product createCompatibleTargetProduct(Product sourceProduct, String name, String type,
                                                        boolean copyAllTiePoints) {
        final int sceneWidth = sourceProduct.getSceneRasterWidth();
        final int sceneHeight = sourceProduct.getSceneRasterHeight();

        Product targetProduct = new Product(name, type, sceneWidth, sceneHeight);
        copyTiePoints(sourceProduct, targetProduct, copyAllTiePoints);
        ProductUtils.copyGeoCoding(sourceProduct, targetProduct);
        targetProduct.setStartTime(sourceProduct.getStartTime());
        targetProduct.setEndTime(sourceProduct.getEndTime());
        return targetProduct;
    }

    /**
     * Copies the tie point data.
     *
     */
    public static void copyTiePoints(Product sourceProduct,
                                     Product targetProduct, boolean copyAllTiePoints) {
        if (copyAllTiePoints) {
            // copy all tie point grids to output product
            ProductUtils.copyTiePointGrids(sourceProduct, targetProduct);
        } else {
            for (int i = 0; i < sourceProduct.getNumTiePointGrids(); i++) {
                TiePointGrid srcTPG = sourceProduct.getTiePointGridAt(i);
                if (srcTPG.getName().equals("latitude") || srcTPG.getName().equals("longitude")) {
                    targetProduct.addTiePointGrid(srcTPG.cloneTiePointGrid());
                }
            }
        }
    }

    /**
     * copies a geocoding from a given reference band as scene geocoding to a given product
     * todo: move to a more general place?!
     *
     * @param referenceBand - the reference band
     * @param product       - the product where the geocoding shall be copied
     */
    public static void copyGeocodingFromBandToProduct(Band referenceBand, Product product) {
        final Scene srcScene = SceneFactory.createScene(referenceBand);
        final Scene destScene = SceneFactory.createScene(product);
        if (srcScene != null && destScene != null) {
            srcScene.transferGeoCodingTo(destScene, null);
        }
    }

    public static Product cloneProduct(Product sourceProduct, boolean copySourceBands) {
        return cloneProduct(sourceProduct, sourceProduct.getSceneRasterWidth(), sourceProduct.getSceneRasterHeight(), copySourceBands);
    }

    public static Product cloneProduct(Product sourceProduct, int width, int height, boolean copySourceBands) {
        Product clonedProduct = new Product(sourceProduct.getName(),
                                            sourceProduct.getProductType(),
                                            width,
                                            height);

        ProductUtils.copyMetadata(sourceProduct, clonedProduct);
        ProductUtils.copyGeoCoding(sourceProduct, clonedProduct);
        ProductUtils.copyFlagCodings(sourceProduct, clonedProduct);
        ProductUtils.copyFlagBands(sourceProduct, clonedProduct, true);
        ProductUtils.copyMasks(sourceProduct, clonedProduct);
        clonedProduct.setStartTime(sourceProduct.getStartTime());
        clonedProduct.setEndTime(sourceProduct.getEndTime());

        if (copySourceBands) {
            // copy all bands from source product
            for (Band b : sourceProduct.getBands()) {
                if (!clonedProduct.containsBand(b.getName())) {
                    ProductUtils.copyBand(b.getName(), sourceProduct, clonedProduct, true);
                    if (isIdepixSpectralBand(b)) {
                        ProductUtils.copyRasterDataNodeProperties(b, clonedProduct.getBand(b.getName()));
                    }
                }
            }

            for (int i = 0; i < sourceProduct.getNumTiePointGrids(); i++) {
                TiePointGrid srcTPG = sourceProduct.getTiePointGridAt(i);
                if (!clonedProduct.containsTiePointGrid(srcTPG.getName())) {
                    clonedProduct.addTiePointGrid(srcTPG.cloneTiePointGrid());
                }
            }
        }

        return clonedProduct;
    }

    public static boolean isIdepixSpectralBand(Band b) {
        return b.getName().startsWith("radiance") || b.getName().startsWith("refl") ||
                b.getName().startsWith("brr") || b.getName().startsWith("rho_toa");
    }


    public static boolean validateInputProduct(Product inputProduct, AlgorithmSelector algorithm) {
        return isInputValid(inputProduct) && isInputConsistentWithAlgorithm(inputProduct, algorithm);
    }

    public static boolean isInputValid(Product inputProduct) {
        if (!isValidAvhrrProduct(inputProduct) &&
                !isValidLandsat8Product(inputProduct) &&
                !isValidProbavProduct(inputProduct) &&
                !isValidModisProduct(inputProduct) &&
                !isValidSeawifsProduct(inputProduct) &&
                !isValidViirsProduct(inputProduct, ViirsConstants.VIIRS_SPECTRAL_BAND_NAMES) &&
                !isValidMerisProduct(inputProduct) &&
                !isValidOlciProduct(inputProduct) &&
                !isValidOlciSlstrSynergyProduct(inputProduct) &&
                !isValidVgtProduct(inputProduct)) {
            IdepixUtils.logErrorMessage("Input sensor must be either Landsat-8, MERIS, AATSR, AVHRR, " +
                                                "OLCI, colocated OLCI/SLSTR, " +
                                                "MODIS/SeaWiFS, PROBA-V or VGT!");
        }
        return true;
    }

    public static boolean isValidMerisProduct(Product product) {
        final boolean merisL1TypePatternMatches = EnvisatConstants.MERIS_L1_TYPE_PATTERN.matcher(product.getProductType()).matches();
        // accept also ICOL L1N products...
        final boolean merisIcolTypePatternMatches = isValidMerisIcolL1NProduct(product);
        final boolean merisCCL1PTypePatternMatches = isValidMerisCCL1PProduct(product);
        return merisL1TypePatternMatches || merisIcolTypePatternMatches || merisCCL1PTypePatternMatches;
    }

    public static boolean isValidOlciProduct(Product product) {
//        return product.getProductType().startsWith("S3A_OL_");  // todo: clarify
        return product.getProductType().contains("OL_1");  // new products have product type 'OL_1_ERR'
    }

    public static boolean isValidOlciSlstrSynergyProduct(Product product) {
        return product.getName().contains("S3A_SY_1");  // todo: clarify
    }

    private static boolean isValidMerisIcolL1NProduct(Product product) {
        final String icolProductType = product.getProductType();
        if (icolProductType.endsWith("_1N")) {
            int index = icolProductType.indexOf("_1");
            final String merisProductType = icolProductType.substring(0, index) + "_1P";
            return (EnvisatConstants.MERIS_L1_TYPE_PATTERN.matcher(merisProductType).matches());
        } else {
            return false;
        }
    }

    private static boolean isValidMerisCCL1PProduct(Product product) {
        return IdepixConstants.MERIS_CCL1P_TYPE_PATTERN.matcher(product.getProductType()).matches();
    }

    public static boolean isValidAvhrrProduct(Product product) {
        return product.getProductType().equalsIgnoreCase(IdepixConstants.AVHRR_L1b_PRODUCT_TYPE) ||
                product.getProductType().equalsIgnoreCase(IdepixConstants.AVHRR_L1b_USGS_PRODUCT_TYPE);
    }

    public static boolean isValidLandsat8Product(Product product) {
        return product.containsBand("coastal_aerosol") &&
                product.containsBand("blue") &&
                product.containsBand("green") &&
                product.containsBand("red") &&
                product.containsBand("near_infrared") &&
                product.containsBand("swir_1") &&
                product.containsBand("swir_2") &&
                product.containsBand("panchromatic") &&
                product.containsBand("cirrus") &&
                product.containsBand("thermal_infrared_(tirs)_1") &&
                product.containsBand("thermal_infrared_(tirs)_2");

    }

    public static boolean isValidModisProduct(Product product) {
        //        return (product.getName().matches("MOD021KM.A[0-9]{7}.[0-9]{4}.[0-9]{3}.[0-9]{13}.(?i)(hdf)") ||
        //                product.getName().matches("MOD021KM.A[0-9]{7}.[0-9]{4}.[0-9]{3}.[0-9]{13}") ||
        //                product.getName().matches("A[0-9]{13}.(?i)(L1B_LAC)"));
        return (product.getName().contains("MOD021KM") || product.getName().contains("MYD021KM") ||
                //                product.getName().contains("L1B_LAC"));
                product.getName().contains("L1B_"));  // seems that we have various extensions :-(
    }

    public static boolean isValidSeawifsProduct(Product product) {
        return (product.getName().matches("S[0-9]{13}.(?i)(L1B_HRPT)") ||
                product.getName().matches("S[0-9]{13}.(?i)(L1B_GAC)") ||
                product.getName().matches("S[0-9]{13}.(?i)(L1C)"));
    }

    public static boolean isValidViirsProduct(Product product, String[] expectedBandNames) {
        // first check expected bands
        if (expectedBandNames != null) {
            for (String expectedBandName : expectedBandNames) {
                if (!product.containsBand(expectedBandName)) {
                    return false;
                }
            }
        }

        // e.g. V2012024230521.L1C
        // 20181005: PML requested L1C_SNPP.nc as valid extension
        return (product.getName().matches("V[0-9]{13}.(?i)(L1C)") ||
                product.getName().matches("V[0-9]{13}.(?i)(L1C.nc)") ||
                product.getName().matches("V[0-9]{13}.(?i)(L1C_SNPP.nc)") ||
                product.getName().matches("V[0-9]{13}.(?i)(L2)") ||
                product.getName().matches("V[0-9]{13}.(?i)(L2.nc)"));
    }


    public static boolean isValidProbavProduct(Product product) {
        return product.getProductType().startsWith(IdepixConstants.PROBAV_PRODUCT_TYPE_PREFIX);
    }

    public static boolean isValidVgtProduct(Product product) {
        return product.getProductType().startsWith(IdepixConstants.SPOT_VGT_PRODUCT_TYPE_PREFIX);
    }

    private static boolean isInputConsistentWithAlgorithm(Product sourceProduct, AlgorithmSelector algorithm) {
        switch (algorithm) {
            case AVHRR:
                return (isValidAvhrrProduct(sourceProduct));
            case LANDSAT8:
                return (isValidLandsat8Product(sourceProduct));
            case MODIS:
                return (isValidModisProduct(sourceProduct));
            case PROBAV:
                return (isValidProbavProduct(sourceProduct));
            case SEAWIFS:
                return (isValidSeawifsProduct(sourceProduct));
            case VIIRS:
                return (isValidViirsProduct(sourceProduct, ViirsConstants.VIIRS_SPECTRAL_BAND_NAMES));
            case MERIS:
                return (isValidMerisProduct(sourceProduct));
            case OLCI:
                return (isValidOlciProduct(sourceProduct));
            case OLCISLSTR:
                return (isValidOlciSlstrSynergyProduct(sourceProduct));
            case VGT:
                return (isValidVgtProduct(sourceProduct));
            default:
                throw new OperatorException("Algorithm " + algorithm.toString() + " not supported.");
        }
    }


    public static boolean areAllReflectancesValid(float[] reflectance) {
        for (float aReflectance : reflectance) {
            if (Float.isNaN(aReflectance) || aReflectance <= 0.0f) {
                return false;
            }
        }
        return true;
    }

    public static boolean areAllReflectancesValid(double[] reflectance) {
        for (double aReflectance : reflectance) {
            if (Double.isNaN(aReflectance) || aReflectance <= 0.0f) {
                return false;
            }
        }
        return true;
    }

    public static void setNewBandProperties(Band band, String description, String unit, double noDataValue,
                                            boolean useNoDataValue) {
        band.setDescription(description);
        band.setUnit(unit);
        band.setNoDataValue(noDataValue);
        band.setNoDataValueUsed(useNoDataValue);
    }

    public static void copySourceBands(Product sourceProduct, Product targetProduct, String bandNameSubstring) {
        for (String bandname : sourceProduct.getBandNames()) {
            if (bandname.contains(bandNameSubstring) && !targetProduct.containsBand(bandname)) {
                ProductUtils.copyBand(bandname, sourceProduct, targetProduct, true);
            }
        }
    }

    public static void addRadianceBands(Product l1bProduct, Product targetProduct, String[] bandsToCopy) {
        for (String bandname : bandsToCopy) {
            if (!targetProduct.containsBand(bandname) && bandname.contains("radiance")) {
                ProductUtils.copyBand(bandname, l1bProduct, targetProduct, true);
            }
        }
    }


}
