/*
 *
 *  * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
 *  *
 *  * This program is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU General Public License as published by the Free
 *  * Software Foundation; either version 3 of the License, or (at your option)
 *  * any later version.
 *  * This program is distributed in the hope that it will be useful, but WITHOUT
 *  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 *  * more details.
 *  *
 *  * You should have received a copy of the GNU General Public License along
 *  * with this program; if not, see http://www.gnu.org/licenses/
 *
 */

package org.esa.s3tbx.olci.radiometry.smilecorr;

import org.esa.s3tbx.olci.radiometry.Sensor;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author muhammad.bc.
 */
public class SmileCorrectionAuxdataTest {


    @Test
    public void testAuxDataIsInstall() throws Exception {
        Path auxDataPath = SmileCorrectionAuxdata.installAuxdata();
        List<Path> collect = Files.list(auxDataPath).collect(Collectors.toList());

        assertTrue(auxDataPath.isAbsolute());
        assertTrue(collect.stream().anyMatch(path -> path.getFileName().toString().equals("band_info_olci.txt")));
        assertTrue(collect.stream().anyMatch(path -> path.getFileName().toString().equals("band_value.txt")));
    }

    @Test
    public void testAuxDataAreLoadedInFlatTable() throws Exception {
        List<String[]> loadAuxdata = SmileCorrectionAuxdata.loadAuxdata(Sensor.OLCI.getBandInfoFileName());
        double[][] auxDataInFlatTable = SmileCorrectionAuxdata.auxDataInFlatTable(loadAuxdata, 9);
        assertEquals(22, auxDataInFlatTable.length);
        assertEquals(9, auxDataInFlatTable[0].length);

        assertEquals(1, auxDataInFlatTable[0][0], 1e-8);
        assertEquals(1513.6257, auxDataInFlatTable[0][8], 1e-8);

        assertEquals(6.0, auxDataInFlatTable[5][0], 1e-8);
        assertEquals(1796.8542, auxDataInFlatTable[5][8], 1e-8);
    }

    @Test
    public void testAuxDataValueForOlci() {
        SmileCorrectionAuxdata smileCorrectionAuxdata = new SmileCorrectionAuxdata(Sensor.OLCI);

        int[] expectedBands = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 0};
        assertArrayEquals(expectedBands, smileCorrectionAuxdata.getBandIndices());

        int[] expectedWaterLower = {1, 1, 2, 3, 4, 5, 6, 7, 8, 0, 11, 11, 0, 0, 0, 16, 16, 17, 0, 0, 18, 0};
        assertArrayEquals(expectedWaterLower, smileCorrectionAuxdata.getWaterLowerBands());

        int[] expectedWaterUpper = {2, 3, 4, 5, 6, 7, 8, 9, 9, 0, 12, 12, 0, 0, 0, 17, 18, 18, 0, 0, 21, 0};
        assertArrayEquals(expectedWaterUpper, smileCorrectionAuxdata.getWaterUpperBands());

        int[] expectedLandLowerBands = {1, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 0, 0, 0, 16, 16, 17, 0, 0, 18, 0};
        assertArrayEquals(expectedLandLowerBands, smileCorrectionAuxdata.getLandLowerBands());

        float[] expectedCentralWvl = {
                400f, 412.5f, 442.5f, 490f, 510f, 560f, 620f, 665f,
                673.75f, 681.25f, 708.75f, 753.75f, 761.25f, 764.375f,
                767.5f, 778.75f, 865f, 885f, 900f, 940f, 1020f, 0f
        };
        assertArrayEquals(expectedCentralWvl, smileCorrectionAuxdata.getRefCentralWaveLengths(), 1e-6f);

        // values are taken from the mean Spectral Response Function dataset
        // on this page: https://sentinel.esa.int/web/sentinel/technical-guides/sentinel-3-olci/olci-instrument/spectral-response-function-data
        float[] expectedSolarIrradiance = {
                1513.6257f, 1708.0474f, 1889.9923f, 1936.2612f, 1919.649f, 1796.8542f, 1649.14f, 1530.1553f,
                1494.7185f, 1468.8616f, 1403.1105f, 1266.3196f, 1247.4586f, 1238.9945f, 1229.769f, 1173.4987f,
                959.71075f, 930.863f, 895.767f, 826.40735f, 699.70306f, 0.0f
        };
        assertArrayEquals(expectedSolarIrradiance, smileCorrectionAuxdata.getSolarIrradiances(), 1e-6f);

    }

    @Test
    public void testAuxDataValueForMeris() {
        SmileCorrectionAuxdata smileCorrectionAuxdata = new SmileCorrectionAuxdata(Sensor.MERIS);

        int[] expectedBands = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 0};
        assertArrayEquals(expectedBands, smileCorrectionAuxdata.getBandIndices());

        int[] expectedWaterLower = {1, 1, 2, 3, 4, 5, 6, 7, 8, 10, 10, 10, 13, 13, 13, 0};
        assertArrayEquals(expectedWaterLower, smileCorrectionAuxdata.getWaterLowerBands());

        int[] expectedWaterUpper = {2, 3, 4, 5, 6, 7, 9, 9, 9, 12, 12, 12, 14, 14, 14, 0};
        assertArrayEquals(expectedWaterUpper, smileCorrectionAuxdata.getWaterUpperBands());

        int[] expectedLandLowerBands = {1, 1, 2, 3, 4, 5, 6, 7, 9, 10, 10, 10, 13, 13, 13, 0};
        assertArrayEquals(expectedLandLowerBands, smileCorrectionAuxdata.getLandLowerBands());

        int[] expectedLandUpperBands = {2, 3, 4, 5, 6, 7, 9, 8, 10, 12, 12, 12, 14, 14, 14, 0};
        assertArrayEquals(expectedLandUpperBands, smileCorrectionAuxdata.getLandUpperBands());


        float[] expectedCentralWvl = {
                412.5f, 442.5f, 490f, 510f, 560f, 620f,
                665f, 681.25f, 708.75f, 753.75f, 761.875f,
                778.75f, 865f, 885f, 900f, 0f
        };
        assertArrayEquals(expectedCentralWvl, smileCorrectionAuxdata.getRefCentralWaveLengths(), 1e-6f);

        // values are taken from the Specification of the Scientific Contents of the MERIS Level-1b & 2 Auxiliary Data Products page 131
        // https://earth.esa.int/documents/700255/2042855/PO-RS-PAR-GS-0002+3C+-+Prod+Spec.pdf/cb0d20b0-c1f4-4903-a5a7-c250fedda700
        float[] expectedSolarIrradiance = {
                1713.692017f, 1877.56604f, 1929.26294f, 1926.890991f, 1800.458008f, 1649.704956f,
                1530.927002f, 1470.229004f, 1405.473999f, 1266.199951f, 1253.004028f, 1175.737061f,
                958.7630005f, 929.7860107f, 895.460022f, 0f
        };
        assertArrayEquals(expectedSolarIrradiance, smileCorrectionAuxdata.getSolarIrradiances(), 1e-6f);

    }

    @Test
    public void testAuxDataValueForMeris4th() {
        SmileCorrectionAuxdata smileCorrectionAuxdata = new SmileCorrectionAuxdata(Sensor.MERIS_4TH);

        int[] expectedBands = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 0};
        assertArrayEquals(expectedBands, smileCorrectionAuxdata.getBandIndices());

        int[] expectedWaterLower = {1, 1, 2, 3, 4, 5, 6, 7, 8, 10, 10, 10, 13, 13, 13, 0};
        assertArrayEquals(expectedWaterLower, smileCorrectionAuxdata.getWaterLowerBands());

        int[] expectedWaterUpper = {2, 3, 4, 5, 6, 7, 9, 9, 9, 12, 12, 12, 14, 14, 14, 0};
        assertArrayEquals(expectedWaterUpper, smileCorrectionAuxdata.getWaterUpperBands());

        int[] expectedLandLowerBands = {1, 1, 2, 3, 4, 5, 6, 7, 9, 10, 10, 10, 13, 13, 13, 0};
        assertArrayEquals(expectedLandLowerBands, smileCorrectionAuxdata.getLandLowerBands());

        int[] expectedLandUpperBands = {2, 3, 4, 5, 6, 7, 9, 8, 10, 12, 12, 12, 14, 14, 14, 0};
        assertArrayEquals(expectedLandUpperBands, smileCorrectionAuxdata.getLandUpperBands());


        float[] expectedCentralWvl = {
                412.5f, 442.5f, 490f, 510f, 560f, 620f,
                665f, 681.25f, 708.75f, 753.75f, 761.875f,
                778.75f, 865f, 885f, 900f, 0f
        };
        assertArrayEquals(expectedCentralWvl, smileCorrectionAuxdata.getRefCentralWaveLengths(), 1e-6f);

        // values are taken from the Specification of the Scientific Contents of the MERIS Level-1b & 2 Auxiliary Data Products page 131
        // https://earth.esa.int/documents/700255/2042855/PO-RS-PAR-GS-0002+3C+-+Prod+Spec.pdf/cb0d20b0-c1f4-4903-a5a7-c250fedda700
        float[] expectedSolarIrradiance = {
                1713.692017f, 1877.56604f, 1929.26294f, 1926.890991f, 1800.458008f, 1649.704956f,
                1530.927002f, 1470.229004f, 1405.473999f, 1266.199951f, 1253.004028f, 1175.737061f,
                958.7630005f, 929.7860107f, 895.460022f, 0f
        };
        assertArrayEquals(expectedSolarIrradiance, smileCorrectionAuxdata.getSolarIrradiances(), 1e-6f);

    }


    @Test
    public void testReadSolarFluxMER_F() throws Exception {
        SmileCorrectionAuxdata sCorrectAux = new SmileCorrectionAuxdata(Sensor.MERIS);
        sCorrectAux.loadFluxWaven("MER_F");
        double[][] detSunSpectralFlux = sCorrectAux.getDetectorSunSpectralFluxes();
        assertNotNull(detSunSpectralFlux);
        assertEquals(3700, detSunSpectralFlux.length);

        assertEquals(1715.95068068023, detSunSpectralFlux[0][0], 1e-8);
        assertEquals(1715.94499537724, detSunSpectralFlux[1][0], 1e-8);
        assertEquals(1715.87048338401, detSunSpectralFlux[14][0], 1e-8);
    }

    @Test
    public void testReadSolarFluxMER_R() throws Exception {
        SmileCorrectionAuxdata sCorrectAux = new SmileCorrectionAuxdata(Sensor.MERIS);
        sCorrectAux.loadFluxWaven("MER_R");
        double[][] detSunSpectralFlux = sCorrectAux.getDetectorSunSpectralFluxes();
        assertNotNull(detSunSpectralFlux);
        assertEquals(925, detSunSpectralFlux.length);

        assertEquals(1715.92504199224, detSunSpectralFlux[0][0], 1e-8);
        assertEquals(1884.8629040149, detSunSpectralFlux[0][1], 1e-8);
        assertEquals(895.328636362898, detSunSpectralFlux[0][14], 1e-8);
    }
}