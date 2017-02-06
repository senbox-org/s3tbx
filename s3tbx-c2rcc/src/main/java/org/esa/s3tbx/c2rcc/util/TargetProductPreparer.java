package org.esa.s3tbx.c2rcc.util;

import org.esa.s3tbx.c2rcc.seawifs.C2rccSeaWiFSAlgorithm;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.ProductUtils;

import java.awt.Color;

public class TargetProductPreparer {

    private static final String C2RCC_FLAGS_VALID_PE = "c2rcc_flags.Valid_PE";

    public static void prepareTargetProduct(Product targetProduct, Product sourceProduct,
                                            String prefixSourceBandName, int[] bandIndexOrWavelengths, boolean outputRtosa, boolean asRrs) {
        ProductUtils.copyFlagBands(sourceProduct, targetProduct, true);
        String acReflecPrefix;
        if (asRrs) {
            acReflecPrefix = "rrs";
        } else {
            acReflecPrefix = "rhow";
        }

        for (int idx_wl : bandIndexOrWavelengths) {
            Band reflecBand;
            if (asRrs) {
                reflecBand = addBand(targetProduct, acReflecPrefix + "_" + idx_wl, "sr^-1", "Atmospherically corrected Angular dependent remote sensing reflectances");
            } else {
                reflecBand = addBand(targetProduct, acReflecPrefix + "_" + idx_wl, "1", "Atmospherically corrected Angular dependent water leaving reflectances");
            }
            ProductUtils.copySpectralBandProperties(sourceProduct.getBand(prefixSourceBandName + idx_wl), reflecBand);
            reflecBand.setValidPixelExpression(C2RCC_FLAGS_VALID_PE);
        }

        addBand(targetProduct, "iop_apig", "m^-1", "Pigment absorption coefficient");
        addBand(targetProduct, "iop_adet", "m^-1", "Pigment absorption");
        addBand(targetProduct, "iop_agelb", "m^-1", "Yellow substance absorption coefficient");
        addBand(targetProduct, "iop_bpart", "m^-1", "");
        addBand(targetProduct, "iop_bwit", "m^-1", "Backscattering of suspended particulate matter");
        addVirtualBand(targetProduct, "iop_adg", "iop_adet + iop_agelb", "m^-1", "absorption of detritus and yellow substance");
        addVirtualBand(targetProduct, "iop_atot", "iop_apig + iop_adet + iop_agelb", "m^-1", "Total absorption coefficient of all water constituents");
        addVirtualBand(targetProduct, "iop_btot", "iop_bpart + iop_bwit", "m^-1", "Total particle scattering");

        addVirtualBand(targetProduct, "conc_tsm", "(iop_bpart + iop_bwit) * 1.7", "g m^-3", "Total suspended matter dry weight concentration");
        addVirtualBand(targetProduct, "conc_chl", "pow(iop_apig, 1.04) * 20.0", "mg/m^3", "Chlorophyll concentration");

        addBand(targetProduct, "rtosa_ratio_min", "1", "Minimum of rtosa_out:rtosa_in ratios");
        addBand(targetProduct, "rtosa_ratio_max", "1", "Maximum of rtosa_out:rtosa_in ratios");
        Band c2rcc_flags = targetProduct.addBand("c2rcc_flags", ProductData.TYPE_UINT32);
        c2rcc_flags.setDescription("C2RCC quality flags");

        FlagCoding flagCoding = new FlagCoding("c2rcc_flags");
        flagCoding.addFlag("Rtosa_OOS", 0x01 << C2rccSeaWiFSAlgorithm.FLAG_INDEX_RTOSA_OOS, "The input spectrum to the atmospheric correction neural net was out of the scope of the training range and the inversion is likely to be wrong");
        flagCoding.addFlag("Rtosa_OOR", 0x01 << C2rccSeaWiFSAlgorithm.FLAG_INDEX_RTOSA_OOR, "The input spectrum to atmospheric correction neural net out of training range");
        flagCoding.addFlag("Rhow_OOR", 0x01 << C2rccSeaWiFSAlgorithm.FLAG_INDEX_RHOW_OOR, "One of the inputs to the IOP retrieval neural net is out of training range");
        flagCoding.addFlag("Valid_PE", (int) (0x01L << C2rccSeaWiFSAlgorithm.FLAG_INDEX_VALID_PE), "The operators valid pixel expression has resolved to true");
        targetProduct.getFlagCodingGroup().add(flagCoding);
        c2rcc_flags.setSampleCoding(flagCoding);

        Color[] maskColors = {Color.RED, Color.ORANGE, Color.YELLOW, Color.BLUE, Color.GREEN, Color.PINK, Color.MAGENTA, Color.CYAN};
        String[] flagNames = flagCoding.getFlagNames();
        for (int i = 0; i < flagNames.length; i++) {
            String flagName = flagNames[i];
            MetadataAttribute flag = flagCoding.getFlag(flagName);
            targetProduct.addMask(flagName, "c2rcc_flags." + flagName, flag.getDescription(), maskColors[i % maskColors.length], 0.5);
        }

        if (outputRtosa) {
            for (int idx_wl : bandIndexOrWavelengths) {
                Band rtosaInBand = addBand(targetProduct, "rtosa_in_" + idx_wl, "1", "Top-of-standard-atmosphere reflectances, input to AC");
                ProductUtils.copySpectralBandProperties(sourceProduct.getBand(prefixSourceBandName + idx_wl), rtosaInBand);
                rtosaInBand.setValidPixelExpression(C2RCC_FLAGS_VALID_PE);
            }
            for (int idx_wl : bandIndexOrWavelengths) {
                Band rtosaOutBand = addBand(targetProduct, "rtosa_out_" + idx_wl, "1", "Top-of-standard-atmosphere reflectances, output from ANN");
                ProductUtils.copySpectralBandProperties(sourceProduct.getBand(prefixSourceBandName + idx_wl), rtosaOutBand);
                rtosaOutBand.setValidPixelExpression(C2RCC_FLAGS_VALID_PE);
            }
            targetProduct.setAutoGrouping(acReflecPrefix + ":iop:conc:rtosa_in:rtosa_out");
        } else {
            targetProduct.setAutoGrouping(acReflecPrefix + ":iop:conc");
        }
    }

    public static Band addBand(Product targetProduct, String name, String unit, String description) {
        Band targetBand = targetProduct.addBand(name, ProductData.TYPE_FLOAT32);
        targetBand.setUnit(unit);
        targetBand.setDescription(description);
        targetBand.setGeophysicalNoDataValue(Double.NaN);
        targetBand.setNoDataValueUsed(true);
        targetBand.setValidPixelExpression(C2RCC_FLAGS_VALID_PE);
        return targetBand;
    }

    private static void addVirtualBand(Product targetProduct, String name, String expression, String unit, String description) {
        Band band = targetProduct.addBand(name, expression);
        band.setUnit(unit);
        band.setDescription(description);
        band.getSourceImage(); // trigger source image creation
        band.setGeophysicalNoDataValue(Double.NaN);
        band.setNoDataValueUsed(true);
        band.setValidPixelExpression(C2RCC_FLAGS_VALID_PE);
    }
}
