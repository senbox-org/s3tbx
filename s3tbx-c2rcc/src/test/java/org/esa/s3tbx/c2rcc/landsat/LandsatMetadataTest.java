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

import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.esa.s3tbx.c2rcc.landsat.C2rccLandsat8Operator.L8_BAND_COUNT;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * @author Marco Peters
 */
@RunWith(Parameterized.class)
public class LandsatMetadataTest {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{{0}, {1}, {2}});
    }

    private final int colNum;
    private final LandsatMetadata metadata;

    public LandsatMetadataTest(int colNum) {
        final MetadataElement testRoot = createMetadata(colNum);
        this.colNum = colNum;
        metadata = new LandsatMetadata(testRoot);
    }

    @Test
    public void getCollectionNumber() {
        assertEquals(colNum, metadata.getCollectionNumber());
    }

    @Test
    public void getSunAzimuth() {
        assertEquals(123.45, metadata.getSunAzimuth(), 1.e-8);
    }

    @Test
    public void getSunElevation() {
        assertEquals(67.89, metadata.getSunElevation(), 1.e-8);
    }

    @Test
    public void getReflectanceScalingOffsets() {
        final double[] reflectanceScalingOffsets = metadata.getReflectanceScalingOffsets(L8_BAND_COUNT);
        double sunAngleCorrectionFactor = metadata.getSunAngleCorrFactor();

        final double[] expected = Arrays.stream(new double[]{0, 1, 2, 3, 4}).map(value -> value / sunAngleCorrectionFactor).toArray();
        assertArrayEquals(expected, reflectanceScalingOffsets, 1.e-8);
    }

    @Test
    public void getReflectanceScalingValues() {
        final double[] reflectanceScalingValues = metadata.getReflectanceScalingValues(L8_BAND_COUNT);
        double sunAngleCorrectionFactor = metadata.getSunAngleCorrFactor();

        final double[] expected = Arrays.stream(new double[]{0, 1, 2, 3, 4}).map(value -> value / sunAngleCorrectionFactor).toArray();
        assertArrayEquals(expected, reflectanceScalingValues, 1.e-8);
    }

    // 0 = pre-collection data; 1 = collection 1; 2 = collection 2
    private static MetadataElement createMetadata(int collection) {
        final MetadataElement root = new MetadataElement("root");
        final MetadataElement metadataFile = new MetadataElement(collection == 2 ? "LANDSAT_METADATA_FILE" : "L1_METADATA_FILE");
        root.addElement(metadataFile);

        final MetadataElement contentsInfo = new MetadataElement(collection == 2 ? "PRODUCT_CONTENTS" : "METADATA_FILE_INFO");
        final MetadataAttribute colNumAttribute = new MetadataAttribute("COLLECTION_NUMBER", ProductData.createInstance(new double[]{collection}), true);
        if (collection >= 1) { // only add if collection 1 or 2
            contentsInfo.addAttribute(colNumAttribute);
        }
        metadataFile.addElement(contentsInfo);

        final MetadataElement rescaling = new MetadataElement(collection == 2 ? "LEVEL1_RADIOMETRIC_RESCALING" : "RADIOMETRIC_RESCALING");
        for (int i = 0; i < L8_BAND_COUNT; i++) {
            final MetadataAttribute addAttribute = new MetadataAttribute(String.format("REFLECTANCE_ADD_BAND_%d", i + 1), ProductData.createInstance(new double[]{i}), true);
            final MetadataAttribute multAttribute = new MetadataAttribute(String.format("REFLECTANCE_MULT_BAND_%d", i + 1), ProductData.createInstance(new double[]{i}), true);
            rescaling.addAttribute(addAttribute);
            rescaling.addAttribute(multAttribute);
        }
        metadataFile.addElement(rescaling);

        final MetadataElement imageAttributes = new MetadataElement("IMAGE_ATTRIBUTES");
        imageAttributes.addAttribute(new MetadataAttribute("SUN_AZIMUTH", ProductData.createInstance(new double[]{123.45}), true));
        imageAttributes.addAttribute(new MetadataAttribute("SUN_ELEVATION", ProductData.createInstance(new double[]{67.89}), true));
        metadataFile.addElement(imageAttributes);

        return root;
    }
}