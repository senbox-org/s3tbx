/*
 * Copyright (c) 2021.  Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.s3tbx.olci.anomaly.flagging;

import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.Tile;
import org.junit.Test;

import java.awt.*;
import java.util.Date;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class OlciAnomalyFlaggingOpTest {

    @Test
    public void testToReflectance() {
        double[] solarFlux = new double[]{1284.2445, 1194.77, 1657.9};
        double[] radiance = new double[]{46.044, 202.502, 7.4218};
        double invCosSZA = 1.0 / Math.cos(Math.toRadians(37.220443));

        double[] reflectance = OlciAnomalyFlaggingOp.toReflectance(radiance, solarFlux, invCosSZA);
        assertEquals(0.14144603301041955, reflectance[0], 1e-8);
        assertEquals(0.6686678594204847, reflectance[1], 1e-8);
        assertEquals(0.01766104334391852, reflectance[2], 1e-8);
    }

    @Test
    public void testValidateInputProduct() {
        final Product testProduct = createTestProduct();

        // expect no exception here tb 2021-04-08
        OlciAnomalyFlaggingOp.validateInputProduct(testProduct);
    }

    @Test
    public void testValidateInputProduct_missingRadiance() {
        Product testProduct = createTestProduct();
        testProduct.removeBand(testProduct.getBand("Oa03_radiance"));

        try {
            OlciAnomalyFlaggingOp.validateInputProduct(testProduct);
            fail("OperatorException expected");
        } catch (OperatorException expected) {
        }

        testProduct = createTestProduct();
        testProduct.removeBand(testProduct.getBand("Oa17_radiance"));

        try {
            OlciAnomalyFlaggingOp.validateInputProduct(testProduct);
            fail("OperatorException expected");
        } catch (OperatorException expected) {
        }
    }

    @Test
    public void testValidateInputProduct_missingSolarFlux() {
        Product testProduct = createTestProduct();
        testProduct.removeBand(testProduct.getBand("solar_flux_band_4"));

        try {
            OlciAnomalyFlaggingOp.validateInputProduct(testProduct);
            fail("OperatorException expected");
        } catch (OperatorException expected) {
        }

        testProduct = createTestProduct();
        testProduct.removeBand(testProduct.getBand("solar_flux_band_18"));

        try {
            OlciAnomalyFlaggingOp.validateInputProduct(testProduct);
            fail("OperatorException expected");
        } catch (OperatorException expected) {
        }
    }

    @Test
    public void testValidateInputProduct_missingSZA() {
        Product testProduct = createTestProduct();
        testProduct.removeTiePointGrid(testProduct.getTiePointGrid("SZA"));

        try {
            OlciAnomalyFlaggingOp.validateInputProduct(testProduct);
            fail("OperatorException expected");
        } catch (OperatorException expected) {
        }
    }

    @Test
    public void testValidateInputProduct_missingWavelength() {
        Product testProduct = createTestProduct();
        testProduct.removeBand(testProduct.getBand("lambda0_band_5"));

        try {
            OlciAnomalyFlaggingOp.validateInputProduct(testProduct);
            fail("OperatorException expected");
        } catch (OperatorException expected) {
        }

        testProduct = createTestProduct();
        testProduct.removeBand(testProduct.getBand("lambda0_band_21"));

        try {
            OlciAnomalyFlaggingOp.validateInputProduct(testProduct);
            fail("OperatorException expected");
        } catch (OperatorException expected) {
        }
    }

    @Test
    public void testValidateInputProduct_missingAltitude() {
        Product testProduct = createTestProduct();
        testProduct.removeBand(testProduct.getBand("altitude"));

        try {
            OlciAnomalyFlaggingOp.validateInputProduct(testProduct);
            fail("OperatorException expected");
        } catch (OperatorException expected) {
        }
    }

    @Test
    public void testCreateOutputProduct() {
        final Product testProduct = createTestProduct();

        final Product outputProduct = OlciAnomalyFlaggingOp.createOutputProduct(testProduct, false);
        assertEquals("test_type_ANOM", outputProduct.getProductType());
        assertEquals("test_me_ANOM", outputProduct.getName());
        assertEquals(3, outputProduct.getSceneRasterWidth());
        assertEquals(5, outputProduct.getSceneRasterHeight());

        assertEquals("OLCI anomaly flagged L1b", outputProduct.getDescription());
        assertEquals("24-JAN-2021 18:50:51.000000", outputProduct.getStartTime().format());
        assertEquals("25-JAN-2021 22:40:31.000000", outputProduct.getEndTime().format());

        for (int i = 1; i < 22; i++) {
            assertNotNull(outputProduct.getBand("Oa" + String.format("%02d", i) + "_radiance"));
            assertNotNull(outputProduct.getBand("solar_flux_band_" + i));
            assertNotNull(outputProduct.getBand("lambda0_band_" + i));
        }

        assertNotNull(outputProduct.getTiePointGrid("SZA"));

        final Band anomalyFlags = outputProduct.getBand("anomaly_flags");
        assertEquals(ProductData.TYPE_INT8, anomalyFlags.getDataType());
        assertFalse(anomalyFlags.isNoDataValueUsed());
        assertNull(anomalyFlags.getUnit());
        assertEquals(0, anomalyFlags.getSpectralWavelength(), 1e-8);
        assertEquals(0, anomalyFlags.getSpectralBandwidth(), 1e-8);
        assertEquals("Flags indicating OLCI data anomalies", anomalyFlags.getDescription());

        final FlagCoding flagCoding = anomalyFlags.getFlagCoding();
        assertEquals(4, flagCoding.getFlagNames().length);
        assertEquals(1, flagCoding.getFlagMask("ANOM_SPECTRAL_MEASURE"));
        assertEquals(2, flagCoding.getFlagMask("PARTIALLY_SATURATED"));
        assertEquals(4, flagCoding.getFlagMask("ALT_OUT_OF_RANGE"));
        assertEquals(8, flagCoding.getFlagMask("INPUT_DATA_INVALID"));

        final ProductNodeGroup<Mask> maskGroup = outputProduct.getMaskGroup();
        assertEquals(5, maskGroup.getNodeCount());

        // copied from input
        Mask mask = maskGroup.get(0);
        assertEquals("testing", mask.getName());

        // added by operator
        mask = maskGroup.get(1);
        assertEquals("anomaly_flags_anom_spectral_measure", mask.getName());
        assertEquals("Anomalous spectral sample due to saturation of single microbands", mask.getDescription());

        mask = maskGroup.get(2);
        assertEquals("anomaly_flags_partially_saturated", mask.getName());
        assertEquals("Anomalous spectral sample and no saturation flag in spectral bands", mask.getDescription());

        mask = maskGroup.get(3);
        assertEquals("anomaly_flags_altitude_out_of_range", mask.getName());
        assertEquals("Altitude values are out of nominal data range", mask.getDescription());

        mask = maskGroup.get(4);
        assertEquals("anomaly_flags_input_data_invalid", mask.getName());
        assertEquals("Input data to detection algorithms is out of range/invalid", mask.getDescription());
    }

    @Test
    public void testCreateOutputProduct_withSlopeBands() {
        final Product testProduct = createTestProduct();

        final Product outputProduct = OlciAnomalyFlaggingOp.createOutputProduct(testProduct, true);

        final Band maxSpectralSlope = outputProduct.getBand("max_spectral_slope");
        assertEquals(ProductData.TYPE_FLOAT32, maxSpectralSlope.getDataType());
        assertEquals(Float.NaN, maxSpectralSlope.getNoDataValue(), 1e-8);
        assertTrue(maxSpectralSlope.isNoDataValueUsed());
        assertEquals("1/nm", maxSpectralSlope.getUnit());
        assertEquals(0, maxSpectralSlope.getSpectralWavelength(), 1e-8);
        assertEquals(0, maxSpectralSlope.getSpectralBandwidth(), 1e-8);
        assertEquals("Value of maximal spectral slope for bands 1-12, 16-18, 21 in [reflectance/nm]", maxSpectralSlope.getDescription());

        final Band bandIndex = outputProduct.getBand("max_slope_band_index");
        assertEquals(ProductData.TYPE_INT8, bandIndex.getDataType());
        assertEquals(-1, bandIndex.getNoDataValue(), 1e-8);
        assertTrue(bandIndex.isNoDataValueUsed());
        assertNull(bandIndex.getUnit());
        assertEquals(0, bandIndex.getSpectralWavelength(), 1e-8);
        assertEquals(0, bandIndex.getSpectralBandwidth(), 1e-8);
        assertEquals("Band index where the maximal slope is detected", bandIndex.getDescription());
    }

    @Test
    public void testSetOutOfRangeFlag() {
        assertEquals(4, OlciAnomalyFlaggingOp.setOutOfRangeFlag(0));
        assertEquals(5, OlciAnomalyFlaggingOp.setOutOfRangeFlag(1));
    }

    @Test
    public void testSetAnomalMeasureFlag() {
        assertEquals(1, OlciAnomalyFlaggingOp.setAnomalMeasureFlag(0));
        assertEquals(5, OlciAnomalyFlaggingOp.setAnomalMeasureFlag(4));
    }

    @Test
    public void testSetInvalidInputFlag() {
        assertEquals(8, OlciAnomalyFlaggingOp.setInvalidInputFlag(0));
        assertEquals(10, OlciAnomalyFlaggingOp.setInvalidInputFlag(2));
    }

    @Test
    public void testSetPartiallySaturatedFlag() {
        assertEquals(2, OlciAnomalyFlaggingOp.setPartiallySaturatedFlag(0));
        assertEquals(7, OlciAnomalyFlaggingOp.setPartiallySaturatedFlag(5));
    }

    @Test
    public void testProcessAltitudeOutlierPixel() {
        final Tile altitudeTile = mock(Tile.class);
        when(altitudeTile.getSampleFloat(0, 0)).thenReturn(178.f);
        when(altitudeTile.getSampleFloat(1, 0)).thenReturn(-2343562.f);
        when(altitudeTile.getSampleFloat(0, 1)).thenReturn(108976.f);
        when(altitudeTile.getSampleFloat(1, 1)).thenReturn(38.5f);

        final Tile flagTile = mock(Tile.class);

        OlciAnomalyFlaggingOp.processAltitudeOutlierPixel(flagTile, altitudeTile, 0, 0);
        OlciAnomalyFlaggingOp.processAltitudeOutlierPixel(flagTile, altitudeTile, 1, 0);
        OlciAnomalyFlaggingOp.processAltitudeOutlierPixel(flagTile, altitudeTile, 0, 1);
        OlciAnomalyFlaggingOp.processAltitudeOutlierPixel(flagTile, altitudeTile, 1, 1);


        verify(flagTile, times(1)).setSample(0, 1, 4);
        verify(flagTile, times(1)).setSample(1, 0, 4);
    }

    @Test
    public void testGetRadianceBandName() {
        assertEquals("Oa02_radiance", OlciAnomalyFlaggingOp.getRadianceBandName(2));
        assertEquals("Oa11_radiance", OlciAnomalyFlaggingOp.getRadianceBandName(11));
        assertEquals("Oa21_radiance", OlciAnomalyFlaggingOp.getRadianceBandName(21));
    }

    @Test
    public void testGetInvCosSza() {
        assertEquals(1.780467494581093, OlciAnomalyFlaggingOp.getInvCosSza(55.83), 1e-8);
        assertEquals(1.5400837855709208, OlciAnomalyFlaggingOp.getInvCosSza(49.51), 1e-8);
        assertEquals(6.687382240014953, OlciAnomalyFlaggingOp.getInvCosSza(81.4), 1e-8);
        assertEquals(1.0783067772491086, OlciAnomalyFlaggingOp.getInvCosSza(21.97), 1e-8);
    }

    @Test
    public void testGetMaxSlope() {
        double[] reflectances = new double[]{0.23, 0.003765, 0.28645, 0.10988};
        double[] wavelengths = new double[]{400.26569, 411.82013, 442.95026, 490.50098};

        OlciAnomalyFlaggingOp.SlopeIndex slopeIndex = OlciAnomalyFlaggingOp.getMaxSlope(reflectances, wavelengths);
        assertEquals(-0.019579919061417084, slopeIndex.slope, 1e-8);
        assertEquals(0, slopeIndex.slopeIndex);

        double[] solarFlux = new double[]{1425.37476, 1361.47449, 1229.18555, 1138.59583};
        double invCosSZA = 1.0 / Math.cos(Math.toRadians(39.75679));
        reflectances = new double[]{37.39283, 56.71691, 92.09728, 89.20682};
        OlciAnomalyFlaggingOp.toReflectance(reflectances, solarFlux, invCosSZA);

        wavelengths = new double[]{681.64404, 709.19409, 754.27936, 779.35913};

        slopeIndex = OlciAnomalyFlaggingOp.getMaxSlope(reflectances, wavelengths);
        assertEquals(0.0030153289893280447, slopeIndex.slope, 1e-8);
        assertEquals(1, slopeIndex.slopeIndex);
    }

    @Test
    public void testCanComputeTile() {
        OlciAnomalyFlaggingOp flaggingOp = new OlciAnomalyFlaggingOp();

        assertFalse(flaggingOp.canComputeTile());
    }

    @Test
    public void testCanComputeTileStack() {
        OlciAnomalyFlaggingOp flaggingOp = new OlciAnomalyFlaggingOp();

        assertTrue(flaggingOp.canComputeTileStack());
    }

    @Test
    public void testIsFillValue() {
        assertTrue(OlciAnomalyFlaggingOp.isFillValue(-1.0, -1.0));
        assertTrue(OlciAnomalyFlaggingOp.isFillValue(Float.NaN, Float.NaN));

        assertFalse(OlciAnomalyFlaggingOp.isFillValue(-1.0, 0.0));
        assertFalse(OlciAnomalyFlaggingOp.isFillValue(-1.0, Double.NaN));
    }

    @Test
    public void testIsFillValue_vectorVersion() {
        assertTrue(OlciAnomalyFlaggingOp.isFillValue(new double[]{0.3, -1.0, 11.5}, -1.0));
        assertTrue(OlciAnomalyFlaggingOp.isFillValue(new double[]{0.3, 0.9, Double.NaN}, Float.NaN));

        assertFalse(OlciAnomalyFlaggingOp.isFillValue(new double[]{0.3, 0.6, 11.5}, 0.0));
        assertFalse(OlciAnomalyFlaggingOp.isFillValue(new double[]{-0.8, -0.45, 8.2}, Double.NaN));
    }

    @Test
    public void testCheckFillValues() {
        final double radianceFill = Double.NaN;
        final double wavelengthFill = -1.0;
        final double solarFluxFill = -1.0;
        final double szaFill = -1.0;

        final double[] radiances = {2.0, 3.0, 4.0};
        final double[] wavelengths = {5.0, 6.0, 7.0};
        final double[] solarFluxes = {8.0, 9.0, 10.0};
        double sza = 33.8;

        assertFalse(OlciAnomalyFlaggingOp.checkFillValues(radiances, radianceFill, solarFluxes, solarFluxFill, wavelengths, wavelengthFill, sza, szaFill));

        radiances[0] = radianceFill;
        assertTrue(OlciAnomalyFlaggingOp.checkFillValues(radiances, radianceFill, solarFluxes, solarFluxFill, wavelengths, wavelengthFill, sza, szaFill));
        radiances[0] = 2.0;

        wavelengths[1] = wavelengthFill;
        assertTrue(OlciAnomalyFlaggingOp.checkFillValues(radiances, radianceFill, solarFluxes, solarFluxFill, wavelengths, wavelengthFill, sza, szaFill));
        wavelengths[1] = 6.0;

        solarFluxes[2] = solarFluxFill;
        assertTrue(OlciAnomalyFlaggingOp.checkFillValues(radiances, radianceFill, solarFluxes, solarFluxFill, wavelengths, wavelengthFill, sza, szaFill));
        solarFluxes[2] = 10.0;

        sza = szaFill;
        assertTrue(OlciAnomalyFlaggingOp.checkFillValues(radiances, radianceFill, solarFluxes, solarFluxFill, wavelengths, wavelengthFill, sza, szaFill));

        sza = 18.65;
        assertFalse(OlciAnomalyFlaggingOp.checkFillValues(radiances, radianceFill, solarFluxes, solarFluxFill, wavelengths, wavelengthFill, sza, szaFill));
    }

    @Test
    public void testHasSaturation() {
        final int[] saturationFLagValues = {1, 2, 4, 8, 16, 32};
        assertFalse(OlciAnomalyFlaggingOp.hasSaturation(0, saturationFLagValues));
        assertFalse(OlciAnomalyFlaggingOp.hasSaturation(2097152, saturationFLagValues));

        assertTrue(OlciAnomalyFlaggingOp.hasSaturation(1, saturationFLagValues));
        assertTrue(OlciAnomalyFlaggingOp.hasSaturation(4, saturationFLagValues));
        assertTrue(OlciAnomalyFlaggingOp.hasSaturation(48, saturationFLagValues));
    }

    private Product createTestProduct() {
        final Product product = new Product("test_me", "test_type", 3, 5);
        product.setStartTime(ProductData.UTC.create(new Date(1611514251000L), 0));
        product.setEndTime(ProductData.UTC.create(new Date(1611614431000L), 0));

        for (int i = 1; i < 22; i++) {
            final Band band = product.addBand("Oa" + String.format("%02d", i) + "_radiance", ProductData.TYPE_FLOAT32);
            band.setUnit("mW.m-2.sr-1.nm-1");
            band.setSpectralWavelength(12.5f * i);
            band.setSpectralBandwidth(0.8f * i);
            band.setDescription("whatever");

            product.addBand("solar_flux_band_" + i, ProductData.TYPE_FLOAT32);
            product.addBand("lambda0_band_" + i, ProductData.TYPE_FLOAT32);
        }

        product.addBand("altitude", ProductData.TYPE_FLOAT32);

        final TiePointGrid sza = new TiePointGrid("SZA", 2, 2, 0, 0, 5, 5, new float[]{1, 2, 3, 4});
        product.addTiePointGrid(sza);

        product.addMask(Mask.BandMathsType.create("testing", "theMask", 3, 5, "altitude > 23", Color.CYAN, 0.65));

        return product;
    }
}
