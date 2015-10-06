package org.esa.s3tbx.c2rcc.util;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.ProductUtils;

import java.awt.Color;

public class TargetProductPreparer {

    public static void prepareTargetProduct(Product targetProduct, Product sourceProduct, final String prefixSourceBandName, final int[] bandIndexOrWavelengths, boolean outputRtosa) {
        ProductUtils.copyFlagBands(sourceProduct, targetProduct, true);

        for (int idx_wl : bandIndexOrWavelengths) {
            Band reflecBand = targetProduct.addBand("reflec_" + idx_wl, ProductData.TYPE_FLOAT32);
            ProductUtils.copySpectralBandProperties(sourceProduct.getBand(prefixSourceBandName + idx_wl), reflecBand);
            reflecBand.setUnit("1");
        }

        addBand(targetProduct, "iop_apig", "m^-1", "Pigment absorption coefficient");
        addBand(targetProduct, "iop_adet", "m^-1", "Pigment absorption");
        addBand(targetProduct, "iop_agelb", "m^-1", "Yellow substance absorption coefficient");
        addBand(targetProduct, "iop_bpart", "m^-1", "");
        addBand(targetProduct, "iop_bwit", "m^-1", "Backscattering of suspended particulate matter");
        addVirtualBand(targetProduct, "iop_atot", "iop_apig + iop_adet + iop_agelb", "m^-1", "Total absorption coefficient of all water constituents");
        addVirtualBand(targetProduct, "iop_adg", "iop_adet + iop_agelb", "m^-1", "absorption of detritus and yellow substance");

        addVirtualBand(targetProduct, "conc_tsm", "(iop_bpart + iop_bwit) * 1.7", "g m^-3", "Total suspended matter dry weight concentration");
        addVirtualBand(targetProduct, "conc_chl", "pow(iop_apig, 1.04) * 20.0", "mg/m^3", "Chlorophyll concentration");

        addBand(targetProduct, "rtosa_ratio_min", "1", "Minimum of rtosa_out:rtosa_in ratios");
        addBand(targetProduct, "rtosa_ratio_max", "1", "Maximum of rtosa_out:rtosa_in ratios");
        Band l2_flags = targetProduct.addBand("l2_qflags", ProductData.TYPE_UINT32);
        l2_flags.setDescription("Quality flags");

        FlagCoding flagCoding = new FlagCoding("l2_qflags");
        flagCoding.addFlag("AC_NN_IN_ALIEN", 0x01, "The input spectrum to atmospheric correction neural net was unknown");
        flagCoding.addFlag("AC_NN_IN_OOR", 0x02, "One of the inputs to the atmospheric correction neural net was out of range");
        flagCoding.addFlag("IOP_NN_IN_OOR", 0x04, "One of the inputs to the IOP retrieval neural net was out of range");
        targetProduct.getFlagCodingGroup().add(flagCoding);
        l2_flags.setSampleCoding(flagCoding);

        Color[] maskColors = {Color.RED, Color.ORANGE, Color.YELLOW, Color.BLUE, Color.GREEN, Color.PINK, Color.MAGENTA, Color.CYAN};
        String[] flagNames = flagCoding.getFlagNames();
        for (int i = 0; i < flagNames.length; i++) {
            String flagName = flagNames[i];
            MetadataAttribute flag = flagCoding.getFlag(flagName);
            targetProduct.addMask(flagName, "l2_qflags." + flagName, flag.getDescription(), maskColors[i % maskColors.length], 0.5);
        }

        if (outputRtosa) {
            for (int idx_wl : bandIndexOrWavelengths) {
                Band rtosaInBand = addBand(targetProduct, "rtosa_in_" + idx_wl, "1", "Top-of-standard-atmosphere reflectances, input to AC");
                ProductUtils.copySpectralBandProperties(sourceProduct.getBand(prefixSourceBandName + idx_wl), rtosaInBand);
            }
            for (int idx_wl : bandIndexOrWavelengths) {
                Band rtosaOutBand = addBand(targetProduct, "rtosa_out_" + idx_wl, "1", "Top-of-standard-atmosphere reflectances, output from ANN");
                ProductUtils.copySpectralBandProperties(sourceProduct.getBand(prefixSourceBandName + idx_wl), rtosaOutBand);
            }
            targetProduct.setAutoGrouping("reflec:iop:conc:rtosa_in:rtosa_out");
        } else {
            targetProduct.setAutoGrouping("reflec:iop:conc");
        }
    }

    private static Band addBand(Product targetProduct, String name, String unit, String description) {
        Band targetBand = targetProduct.addBand(name, ProductData.TYPE_FLOAT32);
        targetBand.setUnit(unit);
        targetBand.setDescription(description);
        targetBand.setGeophysicalNoDataValue(Double.NaN);
        targetBand.setNoDataValueUsed(true);
        return targetBand;
    }

    private static void addVirtualBand(Product targetProduct, String name, String expression, String unit, String description) {
        Band band = targetProduct.addBand(name, expression);
        band.setUnit(unit);
        band.setDescription(description);
        band.getSourceImage(); // trigger source image creation
        band.setGeophysicalNoDataValue(Double.NaN);
        band.setNoDataValueUsed(true);
    }
}
