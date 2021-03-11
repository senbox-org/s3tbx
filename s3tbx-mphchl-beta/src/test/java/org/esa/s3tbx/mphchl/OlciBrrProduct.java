package org.esa.s3tbx.mphchl;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;

/**
 * OLCI BRR test product for MPH retrieval
 *
 * @author olafd
 */
public class OlciBrrProduct {
    // creates an in memory product of Type S3A_OL_1_EFR__... with four pixels
    // Product: L2_of_MER_FSG_1PNUPA20110605_160100_000003633103_00155_48445_6691.dim
    //
    // px       original [x,y]
    // [0]      [636,  507]
    // [1]      [980,  702]
    // [2]      [1117, 750]
    // [3]      [1045, 385]
    //
    static Product create() {
        final Product product = new Product("OLCI L1B BRR", "S3A_OL_1_EFR__", 2, 2);

        addBrr_07(product);
        addBrr_08(product);
        addBrr_10(product);
        addBrr_11(product);
        addBrr_12(product);
        addBrr_18(product);
        addBrr_06(product);

        addQualityFlags(product);
        addFlagCodings(product);

        return product;
    }

    private static void addBrr_06(Product olciL1BProduct) {
        final Band band = olciL1BProduct.addBand("rBRR_06", ProductData.TYPE_FLOAT32);
        final ProductData rasterData = band.createCompatibleRasterData();
        rasterData.setElemFloatAt(0, 0.056901217f);
        rasterData.setElemFloatAt(1, 0.08701459f);
        rasterData.setElemFloatAt(2, 0.09533884f);
        rasterData.setElemFloatAt(3, 0.06557705f);
        band.setData(rasterData);
    }

    private static void addBrr_07(Product olciL1BProduct) {
        final Band band = olciL1BProduct.addBand("rBRR_07", ProductData.TYPE_FLOAT32);
        final ProductData rasterData = band.createCompatibleRasterData();
        rasterData.setElemFloatAt(0, 0.056901217f);
        rasterData.setElemFloatAt(1, 0.08701459f);
        rasterData.setElemFloatAt(2, 0.09533884f);
        rasterData.setElemFloatAt(3, 0.06557705f);
        band.setData(rasterData);
    }

    private static void addBrr_08(Product olciL1BProduct) {
        final Band band = olciL1BProduct.addBand("rBRR_08", ProductData.TYPE_FLOAT32);
        final ProductData rasterData = band.createCompatibleRasterData();
        rasterData.setElemFloatAt(0, 0.047871254f);
        rasterData.setElemFloatAt(1, 0.083326936f);
        rasterData.setElemFloatAt(2, 0.085396804f);
        rasterData.setElemFloatAt(3, 0.057835124f);
        band.setData(rasterData);
    }

    private static void addBrr_10(Product olciL1BProduct) {
        final Band band = olciL1BProduct.addBand("rBRR_10", ProductData.TYPE_FLOAT32);
        final ProductData rasterData = band.createCompatibleRasterData();
        rasterData.setElemFloatAt(0, 0.04565412f);
        rasterData.setElemFloatAt(1, 0.08187773f);
        rasterData.setElemFloatAt(2, 0.080895595f);
        rasterData.setElemFloatAt(3, 0.058175918f);
        band.setData(rasterData);
    }

    private static void addBrr_11(Product olciL1BProduct) {
        final Band band = olciL1BProduct.addBand("rBRR_11", ProductData.TYPE_FLOAT32);
        final ProductData rasterData = band.createCompatibleRasterData();
        rasterData.setElemFloatAt(0, 0.042183727f);
        rasterData.setElemFloatAt(1, 0.07923279f);
        rasterData.setElemFloatAt(2, 0.082916535f);
        rasterData.setElemFloatAt(3, 0.11774466f);
        band.setData(rasterData);
    }

    private static void addBrr_12(Product olciL1BProduct) {
        final Band band = olciL1BProduct.addBand("rBRR_12", ProductData.TYPE_FLOAT32);
        final ProductData rasterData = band.createCompatibleRasterData();
        rasterData.setElemFloatAt(0, 0.02940591f);
        rasterData.setElemFloatAt(1, 0.072338045f);
        rasterData.setElemFloatAt(2, 0.06302089f);
        rasterData.setElemFloatAt(3, 0.25775266f);
        band.setData(rasterData);
    }

    private static void addBrr_18(Product olciL1BProduct) {
        final Band band = olciL1BProduct.addBand("rBRR_18", ProductData.TYPE_FLOAT32);
        final ProductData rasterData = band.createCompatibleRasterData();
        rasterData.setElemFloatAt(0, 0.020540202f);
        rasterData.setElemFloatAt(1, 0.07036572f);
        rasterData.setElemFloatAt(2, 0.05602023f);
        rasterData.setElemFloatAt(3, 0.30576614f);
        band.setData(rasterData);
    }

    private static void addQualityFlags(Product olciL1BProduct) {
        final Band band = olciL1BProduct.addBand("quality_flags", ProductData.TYPE_INT32);
        final ProductData rasterData = band.createCompatibleRasterData();
        rasterData.setElemIntAt(0, 2);
        rasterData.setElemIntAt(1, 0);
        rasterData.setElemIntAt(2, 0);
        rasterData.setElemIntAt(3, 16);
        band.setData(rasterData);
    }


    private static void addFlagCodings(Product olciL1BProduct) {
        final FlagCoding quality_flags = new FlagCoding("quality_flags");
        quality_flags.addFlag("land", 0x10, "Pixel is over land, not ocean.");
        quality_flags.addFlag("invalid", 0x80, "Pixel is invalid.");
        olciL1BProduct.getBand("quality_flags").setSampleCoding(quality_flags);
        olciL1BProduct.getFlagCodingGroup().add(quality_flags);
    }


}
