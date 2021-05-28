/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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
 */

package org.esa.s3tbx.fu;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class DetectInstrument {

    private static final String GLOBAL_ATTRIBUTES = "Global_Attributes";
    private static final String SEAWIFS_TITLE = "SeaWiFS Level-2 Data";
    private static final String CZCS_TITLE = "CZCS Level-2 Data";
    private static final String MODIS_TITLE_VALUE = "HMODISA Level-2 Data";
    private static final String MODIS_1KM_RESOLUTION_VALUE = "1 km";
    private static final String OLCI_PRODUCT_TYPE_START = "OL_2";
    private static final String OLCI_L2_BANDNAME_PATTERN = "Oa\\d+_reflectance";
    private static final String MERIS_L2_TYPE_PATTER = "MER_..._2P";
    //TODO - DISABLED SENSOR
//    private static final String L8_TYPE_PREFIX = "LANDSAT_8_OLI_TIRS_L1T";

    static Instrument getInstrument(Product product) {
        if (meris(product)) {
            return Instrument.MERIS;
        } else if (modis1km(product)) {
            return Instrument.MODIS;
        } else if (modis500m(product)) {
            return Instrument.MODIS500;
        } else if (s2amsi(product)) {
            return Instrument.S2A_MSI;
        } else if (s2bmsi(product)) {
            return Instrument.S2B_MSI;
        } else if (olci(product)) {
            return Instrument.OLCI;
            //TODO - DISABLED SENSOR
//        } else if (landsat8(product)) {
//            return Instrument.LANDSAT8;
        } else if (seawifs(product)) {
            return Instrument.SEAWIFS;
        } else if (czcs(product)) {
            return Instrument.CZCS;
        }
        return null;
    }

    //TODO - DISABLED SENSOR
//    private static boolean landsat8(Product product) {
//        String productType = product.getProductType();
//        return productType.startsWith(L8_TYPE_PREFIX);
//    }

    private static boolean meris(Product product) {
        Pattern compile = Pattern.compile(MERIS_L2_TYPE_PATTER);
        return compile.matcher(product.getProductType()).matches();

    }

    private static boolean modis1km(Product product) {
        MetadataElement globalAttributes = product.getMetadataRoot().getElement(GLOBAL_ATTRIBUTES);

        return isModis(globalAttributes) && hasModisResultion(globalAttributes, MODIS_1KM_RESOLUTION_VALUE);
    }

    private static boolean modis500m(Product product) {
        MetadataElement mph = product.getMetadataRoot().getElement("MPH");
        if(mph != null && mph.containsAttribute("identifier_product_doi")) {
            String identDoi = mph.getAttributeString("identifier_product_doi");
            return Pattern.matches(".*/MODIS/M[O|Y]D09A1.006", identDoi);
        }
        return false;
    }

    private static boolean hasModisResultion(MetadataElement globalAttributes, String modis500mResolutionValue) {
        if (globalAttributes != null && globalAttributes.containsAttribute("spatialResolution")) {
            final String sensor_name = globalAttributes.getAttributeString("spatialResolution");
            return sensor_name.contains(modis500mResolutionValue);
        }
        return false;
    }

    private static boolean isModis(MetadataElement globalAttributes) {
        String attribName = "title";
        if (globalAttributes != null && globalAttributes.containsAttribute(attribName)) {
            final String sensor_name = globalAttributes.getAttributeString(attribName);
            return sensor_name.contains(MODIS_TITLE_VALUE);
        }
        return false;
    }

    private static boolean olci(Product product) {
        boolean isOLCI = false;
        List<Band> collect = Stream.of(product.getBands()).filter(p -> p.getName().matches(OLCI_L2_BANDNAME_PATTERN)).collect(Collectors.toList());
        boolean checkByType = product.getProductType().contains(OLCI_PRODUCT_TYPE_START);
        if (collect.size() > 0 || checkByType) {
            isOLCI = true;
        }
        return isOLCI;
    }

    private static boolean s2amsi(Product product) {
        return spacecraftEquals(product, "Sentinel-2A");
    }

    private static boolean s2bmsi(Product product) {
        return spacecraftEquals(product, "Sentinel-2A");
    }

    private static boolean spacecraftEquals(Product product, String spacecraftName) {
        if (product.getProductType().equals("S2_MSI_Level-1C")) {
            final MetadataElement root = product.getMetadataRoot();
            MetadataElement datatake = getElement(root, "Level-1C_User_Product", "General_Info", "Product_Info", "Datatake");
            if (datatake != null) {
                return spacecraftName.equals(datatake.getAttributeString("SPACECRAFT_NAME"));
            }
        }
        return false;
    }

    private static MetadataElement getElement(MetadataElement root, String... pathElements) {
        if (pathElements.length == 0) {
            return null;
        }
        MetadataElement current = root;
        for (String pathElement : pathElements) {
            if (current.containsElement(pathElement)) {
                current = root.getElement(pathElement);
            } else {
                return null;
            }
        }
        return current;
    }

    private static boolean seawifs(Product product) {
        MetadataElement globalAttributes = product.getMetadataRoot().getElement(GLOBAL_ATTRIBUTES);

        String attribName = "Title";
        if (globalAttributes != null && globalAttributes.containsAttribute(attribName)) {
            final String title = globalAttributes.getAttributeString(attribName);
            if (title.equals(SEAWIFS_TITLE)) {
                return true;
            }
        }

        return false;
    }

    private static boolean czcs(Product product) {
        MetadataElement globalAttributes = product.getMetadataRoot().getElement(GLOBAL_ATTRIBUTES);

        if (globalAttributes != null && globalAttributes.containsAttribute("Title")) {
            final String title = globalAttributes.getAttributeString("Title");
            if (title.equals(CZCS_TITLE)) {
                return true;
            }
        }

        return false;
    }
}
