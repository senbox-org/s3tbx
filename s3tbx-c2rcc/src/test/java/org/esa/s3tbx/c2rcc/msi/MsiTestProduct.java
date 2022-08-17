/*
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
 *
 */

package org.esa.s3tbx.c2rcc.msi;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

/**
 * @author Marco Peters
 */
public final class MsiTestProduct {
    // Values taken from pin location S2Ref_Product.dim contained in auxdata/refProducts/S2Ref_Product.zip
    private final static float[] REFL_VALUES = new float[]{0.1251f, 0.0985f, 0.0827f, 0.0647f, 0.0705f, 0.0569f, 0.0593f, 0.0498f, 0.0547f, 0.0106f, 0.001f, 0.0452f, 0.0318f};
    private final static float SUN_AZI_VALUE = 131.473f;
    private final static float SUN_ZEN_VALUE = 30.195f;
    private final static float VIEW_AZI_VALUE = 86.0869f;
    private final static float VIEW_ZEN_VALUE = 5.01679f;

    // Hiding the constructor. Not needed.
    private MsiTestProduct() {
    }

    static Product createInput() {
        Product product = new Product("test-msi", "S2_MSI_Level-1C", 1, 1);
        String[] source_band_refl_names = C2rccMsiAlgorithm.SOURCE_BAND_REFL_NAMES;
        for (int i = 0; i < source_band_refl_names.length; i++) {
            product.addBand(source_band_refl_names[i], String.valueOf(REFL_VALUES[i]));
        }

        product.addBand(C2rccMsiOperator.RASTER_NAME_SUN_AZIMUTH, String.valueOf(SUN_AZI_VALUE));
        product.addBand(C2rccMsiOperator.RASTER_NAME_SUN_ZENITH, String.valueOf(SUN_ZEN_VALUE));
        product.addBand(C2rccMsiOperator.RASTER_NAME_VIEW_AZIMUTH, String.valueOf(VIEW_AZI_VALUE));
        product.addBand(C2rccMsiOperator.RASTER_NAME_VIEW_ZENITH, String.valueOf(VIEW_ZEN_VALUE));

        // it is easier to set up bands instead of tie-point grids
        final Band pressure = product.addBand(C2rccMsiOperator.RASTER_NAME_AIR_PRESSURE, String.valueOf(101140.055));
        pressure.setDescription("Mean sea level pressure at surface level provided by ECMWF");
        final Band ozone = product.addBand(C2rccMsiOperator.RASTER_NAME_OZONE, String.valueOf(0.0056297514));
        ozone.setDescription("Total column ozone at surface level provided by ECMWF");
        final Band wv = product.addBand(C2rccMsiOperator.RASTER_NAME_WATER_VAPOUR, String.valueOf(32.966743));
        wv.setDescription("Total column water vapour at surface level provided by ECMWF");


        try {
            product.setSceneGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84, 1, 1, 10, 50, 1, 1));
        } catch (FactoryException | TransformException e) {
            throw new RuntimeException(e);
        }

        MetadataElement l1cUserProduct = new MetadataElement("Level-1C_User_Product");
        MetadataElement generalInfo = new MetadataElement("General_Info");
        MetadataElement productInfo = new MetadataElement("Product_Info");
        productInfo.addAttribute(new MetadataAttribute("PRODUCT_START_TIME", ProductData.createInstance("2015-08-12T10:40:21.459Z"), true));
        productInfo.addAttribute(new MetadataAttribute("PRODUCT_STOP_TIME", ProductData.createInstance("2015-08-12T10:40:21.459Z"), true));
        MetadataElement imageCharacteristics = new MetadataElement("Product_Image_Characteristics");
        imageCharacteristics.addAttribute(new MetadataAttribute("QUANTIFICATION_VALUE", ProductData.createInstance("10000"), true));
        l1cUserProduct.addElement(generalInfo);
        generalInfo.addElement(productInfo);
        generalInfo.addElement(imageCharacteristics);
        product.getMetadataRoot().addElement(l1cUserProduct);
        return product;
    }
}
