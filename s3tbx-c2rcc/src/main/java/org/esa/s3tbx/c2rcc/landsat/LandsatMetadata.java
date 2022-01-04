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

package org.esa.s3tbx.c2rcc.landsat;

import org.esa.snap.core.datamodel.MetadataElement;

/**
 * Handling metadata of Level-1 data of L7 and L8 from Collection 1 and 2.
 *
 * @author Marco Peters
 */
public class LandsatMetadata {

    private final MetadataElement root;
    private final int colNum;

    public LandsatMetadata(MetadataElement root) {
        this.root = root;
        colNum = collectionNumber();
    }

    int getCollectionNumber() {
        return colNum;
    }

    double getSunAzimuth() {
        MetadataElement metadataFile = getMetadataFile();
        MetadataElement imageAttributes = metadataFile.getElement("IMAGE_ATTRIBUTES");
        return imageAttributes.getAttribute("SUN_AZIMUTH").getData().getElemDouble();
    }

    double getSunElevation() {
        MetadataElement metadataFile = getMetadataFile();
        MetadataElement imageAttributes = metadataFile.getElement("IMAGE_ATTRIBUTES");
        return imageAttributes.getAttribute("SUN_ELEVATION").getData().getElemDouble();
    }


    double[] getReflectanceScalingOffsets(int bandCount) {
        MetadataElement radiometricRescaling = getRadiometricRescaling();
        double[] reflOffsets = new double[bandCount];
        double sunAngleCorrectionFactor = getSunAngleCorrFactor();
        for (int i = 0; i < reflOffsets.length; i++) {
            // this follows:
            // http://landsat.usgs.gov/Landsat8_Using_Product.php, section 'Conversion to TOA Reflectance'
            // also see org.esa.s3tbx.dataio.landsat.geotiff.Landsat8Metadata#getSunAngleCorrectionFactor
            // For L7: https://landsat.gsfc.nasa.gov/wp-content/uploads/2016/08/Landsat7_Handbook.pdf,
            reflOffsets[i] = radiometricRescaling.getAttributeDouble(String.format("REFLECTANCE_ADD_BAND_%d", i + 1)) / sunAngleCorrectionFactor;
        }
        return reflOffsets;
    }

    double[] getReflectanceScalingValues(int bandCount) {
        MetadataElement radiometricRescaling = getRadiometricRescaling();
        double[] reflScales = new double[bandCount];
        double sunAngleCorrectionFactor = getSunAngleCorrFactor();
        for (int i = 0; i < reflScales.length; i++) {
            // this follows:
            // http://landsat.usgs.gov/Landsat8_Using_Product.php, section 'Conversion to TOA Reflectance'
            // also see org.esa.s3tbx.dataio.landsat.geotiff.Landsat8Metadata#getSunAngleCorrectionFactor
            // For L7: https://landsat.gsfc.nasa.gov/wp-content/uploads/2016/08/Landsat7_Handbook.pdf,
            reflScales[i] = radiometricRescaling.getAttributeDouble(String.format("REFLECTANCE_MULT_BAND_%d", i + 1)) / sunAngleCorrectionFactor;
        }
        return reflScales;
    }

    double getSunAngleCorrFactor() {
        return Math.sin(Math.toRadians(getSunElevation()));
    }

    private MetadataElement getMetadataFile() {
        MetadataElement metadataFile = root.getElement("LANDSAT_METADATA_FILE"); // Collection 2
        if (metadataFile == null) {
            metadataFile = root.getElement("L1_METADATA_FILE"); // Collection 1
        }
        return metadataFile;
    }

    private MetadataElement getRadiometricRescaling() {
        return getMetadataFile().getElement(colNum == 1 ? "RADIOMETRIC_RESCALING" : "LEVEL1_RADIOMETRIC_RESCALING");
    }

    private int collectionNumber() {
        final MetadataElement metadataFile = getMetadataFile();
        final MetadataElement contentsInfo;
        // To get the collection number we need to know the collection????
        if (root.containsElement("LANDSAT_METADATA_FILE")) { // Collection 2
            contentsInfo = metadataFile.getElement("PRODUCT_CONTENTS");
        } else {
            contentsInfo = metadataFile.getElement("METADATA_FILE_INFO");
        }
        final int number = contentsInfo.getAttributeInt("COLLECTION_NUMBER");
        if (number == 1 || number == 2) {
            return number;
        } else {
            throw new IllegalStateException(String.format("Unknown collection number: %d", colNum));
        }
    }
}
