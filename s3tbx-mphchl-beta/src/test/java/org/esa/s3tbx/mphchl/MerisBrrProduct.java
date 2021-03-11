package org.esa.s3tbx.mphchl;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;

/**
 * MERIS BRR test product for MPH retrieval
 *
 * @author olafd
 */
public class MerisBrrProduct {
    // creates an in memory product of Type MER_FRS_1P_BRR with four pixels
    // Product: L2_of_MER_FSG_1PNUPA20110605_160100_000003633103_00155_48445_6691.dim
    //
    // px       original [x,y]
    // [0]      [636,  507]
    // [1]      [980,  702]
    // [2]      [1117, 750]
    // [3]      [1045, 385]
    //
    static Product create() {
        final Product product = new Product("Meris L1B BRR", "MER_FRS_1P_BRR", 2, 2);

        addBrr_06(product);
        addBrr_07(product);
        addBrr_08(product);
        addBrr_09(product);
        addBrr_10(product);
        addBrr_14(product);
        addBrr_05(product);

        addl1_flags(product);
        addcloud_classif_flags(product);
        addgas_flags(product);
        addray_corr_flags(product);

        addFlagCodings(product);

        return product;
    }

    private static void addBrr_05(Product merisL1BProduct) {
        final Band band = merisL1BProduct.addBand("rBRR_05", ProductData.TYPE_FLOAT32);
        final ProductData rasterData = band.createCompatibleRasterData();
        rasterData.setElemFloatAt(0, 0.056901217f);
        rasterData.setElemFloatAt(1, 0.08701459f);
        rasterData.setElemFloatAt(2, 0.09533884f);
        rasterData.setElemFloatAt(3, 0.06557705f);
        band.setData(rasterData);
    }

    private static void addBrr_06(Product merisL1BProduct) {
        final Band band = merisL1BProduct.addBand("rBRR_06", ProductData.TYPE_FLOAT32);
        final ProductData rasterData = band.createCompatibleRasterData();
        rasterData.setElemFloatAt(0, 0.056901217f);
        rasterData.setElemFloatAt(1, 0.08701459f);
        rasterData.setElemFloatAt(2, 0.09533884f);
        rasterData.setElemFloatAt(3, 0.06557705f);
        band.setData(rasterData);
    }

    private static void addBrr_07(Product merisL1BProduct) {
        final Band band = merisL1BProduct.addBand("rBRR_07", ProductData.TYPE_FLOAT32);
        final ProductData rasterData = band.createCompatibleRasterData();
        rasterData.setElemFloatAt(0, 0.047871254f);
        rasterData.setElemFloatAt(1, 0.083326936f);
        rasterData.setElemFloatAt(2, 0.085396804f);
        rasterData.setElemFloatAt(3, 0.057835124f);
        band.setData(rasterData);
    }

    private static void addBrr_08(Product merisL1BProduct) {
        final Band band = merisL1BProduct.addBand("rBRR_08", ProductData.TYPE_FLOAT32);
        final ProductData rasterData = band.createCompatibleRasterData();
        rasterData.setElemFloatAt(0, 0.04565412f);
        rasterData.setElemFloatAt(1, 0.08187773f);
        rasterData.setElemFloatAt(2, 0.080895595f);
        rasterData.setElemFloatAt(3, 0.058175918f);
        band.setData(rasterData);
    }

    private static void addBrr_09(Product merisL1BProduct) {
        final Band band = merisL1BProduct.addBand("rBRR_09", ProductData.TYPE_FLOAT32);
        final ProductData rasterData = band.createCompatibleRasterData();
        rasterData.setElemFloatAt(0, 0.042183727f);
        rasterData.setElemFloatAt(1, 0.07923279f);
        rasterData.setElemFloatAt(2, 0.082916535f);
        rasterData.setElemFloatAt(3, 0.11774466f);
        band.setData(rasterData);
    }

    private static void addBrr_10(Product merisL1BProduct) {
        final Band band = merisL1BProduct.addBand("rBRR_10", ProductData.TYPE_FLOAT32);
        final ProductData rasterData = band.createCompatibleRasterData();
        rasterData.setElemFloatAt(0, 0.02940591f);
        rasterData.setElemFloatAt(1, 0.072338045f);
        rasterData.setElemFloatAt(2, 0.06302089f);
        rasterData.setElemFloatAt(3, 0.25775266f);
        band.setData(rasterData);
    }

    private static void addBrr_14(Product merisL1BProduct) {
        final Band band = merisL1BProduct.addBand("rBRR_14", ProductData.TYPE_FLOAT32);
        final ProductData rasterData = band.createCompatibleRasterData();
        rasterData.setElemFloatAt(0, 0.020540202f);
        rasterData.setElemFloatAt(1, 0.07036572f);
        rasterData.setElemFloatAt(2, 0.05602023f);
        rasterData.setElemFloatAt(3, 0.30576614f);
        band.setData(rasterData);
    }

    private static void addl1_flags(Product merisL1BProduct) {
        final Band band = merisL1BProduct.addBand("l1_flags", ProductData.TYPE_INT32);
        final ProductData rasterData = band.createCompatibleRasterData();
        rasterData.setElemIntAt(0, 2);
        rasterData.setElemIntAt(1, 0);
        rasterData.setElemIntAt(2, 0);
        rasterData.setElemIntAt(3, 16);
        band.setData(rasterData);
    }

    private static void addcloud_classif_flags(Product merisL1BProduct) {
        final Band band = merisL1BProduct.addBand("cloud_classif_flags", ProductData.TYPE_INT32);
        final ProductData rasterData = band.createCompatibleRasterData();
        rasterData.setElemIntAt(0, 1312);
        rasterData.setElemIntAt(1, 1376);
        rasterData.setElemIntAt(2, 1312);
        rasterData.setElemIntAt(3, 8192);
        band.setData(rasterData);
    }

    private static void addgas_flags(Product merisL1BProduct) {
        final Band band = merisL1BProduct.addBand("gas_flags", ProductData.TYPE_INT32);
        final ProductData rasterData = band.createCompatibleRasterData();
        rasterData.setElemIntAt(0, 1);
        rasterData.setElemIntAt(1, 1);
        rasterData.setElemIntAt(2, 1);
        rasterData.setElemIntAt(3, 1);
        band.setData(rasterData);
    }

    private static void addray_corr_flags(Product merisL1BProduct) {
        final Band band = merisL1BProduct.addBand("ray_corr_flags", ProductData.TYPE_INT32);
        final ProductData rasterData = band.createCompatibleRasterData();
        rasterData.setElemIntAt(0, 0);
        rasterData.setElemIntAt(1, 0);
        rasterData.setElemIntAt(2, 0);
        rasterData.setElemIntAt(3, 0);
        band.setData(rasterData);
    }

    private static void addFlagCodings(Product merisL1BProduct) {
        final FlagCoding l1_flags = new FlagCoding("l1_flags");
        l1_flags.addFlag("LAND_OCEAN", 0x10, "Pixel is over land, not ocean.");
        l1_flags.addFlag("INVALID", 0x80, "Pixel is invalid.");
        merisL1BProduct.getBand("l1_flags").setSampleCoding(l1_flags);
        merisL1BProduct.getFlagCodingGroup().add(l1_flags);

        final FlagCoding cloud_classif_flags = new FlagCoding("cloud_classif_flags");
        cloud_classif_flags.addFlag("F_CLOUD", 0x1, "none");
        cloud_classif_flags.addFlag("F_CLOUD_BUFFER", 0x800, "none");
        cloud_classif_flags.addFlag("F_CLOUD_SHADOW", 0x1000, "none");
        cloud_classif_flags.addFlag("F_LAND", 0x2000, "none");
        cloud_classif_flags.addFlag("F_MIXED_PIXEL", 0x8000, "none");
        merisL1BProduct.getBand("cloud_classif_flags").setSampleCoding(cloud_classif_flags);
        merisL1BProduct.getFlagCodingGroup().add(cloud_classif_flags);
    }

}
