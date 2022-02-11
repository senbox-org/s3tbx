/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.s3tbx.dataio.landsat.geotiff;

import org.esa.s3tbx.dataio.landsat.geotiff.c2.Landsat8C2Metadata;
import org.junit.Test;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static org.junit.Assert.assertTrue;

/**
 * @author Thomas Storm
 */
public class LandsatMetadataFactoryTest {

    @Test
    public void testCreate_L1_MSS_reproc() throws Exception {
        File testFile = getTestFile("test_L1_MSS_MTL.txt");
        LandsatMetadata landsatMetadata = LandsatMetadataFactory.create(testFile);
        assertTrue(landsatMetadata instanceof LandsatReprocessedMetadata);
    }

    @Test
    public void testCreate_L3_MSS_reproc() throws Exception {
        File testFile = getTestFile("test_L3_MSS_MTL.txt");
        LandsatMetadata landsatMetadata = LandsatMetadataFactory.create(testFile);
        assertTrue(landsatMetadata instanceof LandsatReprocessedMetadata);
    }

    @Test
    public void testCreate_5_reproc() throws Exception {
        File testFile = getTestFile("test_5_reproc_MTL.txt");
        LandsatMetadata landsatMetadata = LandsatMetadataFactory.create(testFile);
        assertTrue(landsatMetadata instanceof LandsatReprocessedMetadata);
    }

    @Test
    public void testCreate_7_reproc() throws Exception {
        File testFile = getTestFile("test_7_reproc_MTL.txt");
        LandsatMetadata landsatMetadata = LandsatMetadataFactory.create(testFile);
        assertTrue(landsatMetadata instanceof LandsatReprocessedMetadata);
    }

    @Test
    public void testCreate_8C1L1() throws Exception {
        File testFile = getTestFile("test_L8C1L1_MTL.txt");
        LandsatMetadata landsatMetadata = LandsatMetadataFactory.create(testFile);
        assertTrue(landsatMetadata instanceof Landsat8Metadata);
    }

    @Test
    public void testCreate_8C2L1() throws Exception {
        File testFile = getTestFile("test_L8C2L1_MTL.txt");
        LandsatMetadata landsatMetadata = LandsatMetadataFactory.create(testFile);
        assertTrue(landsatMetadata instanceof Landsat8C2Metadata);
    }

    @Test
    public void testCreate_9C2L1() throws Exception {
        File testFile = getTestFile("test_L9C2L1_MTL.txt");
        LandsatMetadata landsatMetadata = LandsatMetadataFactory.create(testFile);
        assertTrue(landsatMetadata instanceof Landsat8C2Metadata);
    }

    @Test
    public void testCreate_Legacy() throws Exception {
        File testFile = getTestFile("test_L7_MTL.txt");
        LandsatMetadata landsatMetadata = LandsatMetadataFactory.create(testFile);
        assertTrue(landsatMetadata instanceof LandsatLegacyMetadata);
    }

    @Test(expected = IllegalStateException.class)
    public void testFail() throws Exception {
        File testFile = getTestFile("test_broken_MTL.txt");
        LandsatMetadataFactory.create(testFile);
    }

    private File getTestFile(String name) throws URISyntaxException {
        URL url = getClass().getResource(name);
        URI uri = new URI(url.toString());
        return new File(uri.getPath());
    }
}
