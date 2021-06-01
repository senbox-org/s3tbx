package org.esa.s3tbx.dataio.s3.meris.reprocessing;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.*;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class Meris3rd4thReprocessingAdapterTest {

    private Product fourthReproTestProduct;
    private Meris3rd4thReprocessingAdapter adapter;
    private Product thirdReproProduct;

    @Before
    public void setUp() {
        createFourthReproTestProduct();
        adapter = new Meris3rd4thReprocessingAdapter();
        thirdReproProduct = adapter.convertToLowerVersion(fourthReproTestProduct);
    }

    @Test
    public void testConvertQualityToL1FlagValue() {
        // mapping quality_flags --> l1_flags:
        // ##  cosmetic (2^24) --> COSMETIC (1)
        // ##  duplicated (2^23) --> DUPLICATED (2)
        // ##  sun_glint_risk (2^22) --> GLINT_RISK (4)
        // ##  dubious (2^21) --> SUSPECT (8)
        // ##  land (2^31) --> LAND_OCEAN (16)
        // ##  bright (2^27) --> BRIGHT (32)
        // ##  coastline (2^30) --> COASTLINE (64)
        // ##  invalid (2^25) --> INVALID (128)

        // quality flag: pixel is cosmetic, dubious, coastline
        // NOTE: quality_flag is uint32 in the 4RP product, we treat it as a long here
        long qualityFlagValue = (long) (Math.pow(2.0, 24) + Math.pow(2.0, 21) + Math.pow(2.0, 30));
        int expectedL1FlagValue = 1 + 8 + 64;
        int l1FlagValue = adapter.convertQualityToL1FlagValue(qualityFlagValue);
        assertEquals(expectedL1FlagValue, l1FlagValue);

        // quality flag: pixel is duplicated, sun glint risk, land, bright
        qualityFlagValue = (long) (Math.pow(2.0, 23) + Math.pow(2.0, 22) + Math.pow(2.0, 31) + Math.pow(2.0, 27));
        expectedL1FlagValue = 2 + 4 + 16 + 32;
        l1FlagValue = adapter.convertQualityToL1FlagValue(qualityFlagValue);
        assertEquals(expectedL1FlagValue, l1FlagValue);

        // quality flag: pixel is invalid
        qualityFlagValue = (long) (Math.pow(2.0, 25));
        expectedL1FlagValue = 128;
        l1FlagValue = adapter.convertQualityToL1FlagValue(qualityFlagValue);
        assertEquals(expectedL1FlagValue, l1FlagValue);
    }

    @Test
    public void testConvertToLowerVersion_spectralBands() throws IOException {
        for (int i = 0; i < Meris3rd4thReprocessingAdapter.MERIS_L1B_NUM_SPECTRAL_BANDS; i++) {
            // spectral bands
            final String fourthReproSpectralBandName = "M" + String.format("%02d", i + 1) + "_radiance";
            final String thirdReproSpectralBandName =
                    Meris3rd4thReprocessingAdapter.MERIS_L1B_RADIANCE_BAND_NAME_PREFIX + "_" + (i + 1);
            final Band fourthReproSpectralBand = fourthReproTestProduct.getBand(fourthReproSpectralBandName);
            final Band thirdReproSpectralBand = thirdReproProduct.getBand(thirdReproSpectralBandName);
            assertNotNull(thirdReproSpectralBand);
            float[] fourthReproSpectralData = new float[3 * 2];
            float[] thirdReproSpectralData = new float[3 * 2];
            fourthReproSpectralBand.readPixels(0, 0, 3, 2, fourthReproSpectralData, ProgressMonitor.NULL);
            thirdReproSpectralBand.readPixels(0, 0, 3, 2, thirdReproSpectralData, ProgressMonitor.NULL);
            assertArrayEquals(fourthReproSpectralData, thirdReproSpectralData, 0.0f);

            // solar fluxes
            final String fourthReproSolarFluxBandName = "solar_flux_band_" + (i + 1);
            final Band fourthReproSolarFluxSpectralBand = fourthReproTestProduct.getBand(fourthReproSolarFluxBandName);
            assertNotNull(fourthReproSolarFluxSpectralBand);
            float[] fourthReproSolarFluxData = new float[3 * 2];
            fourthReproSolarFluxSpectralBand.readPixels(0, 0, 3, 2, fourthReproSolarFluxData, ProgressMonitor.NULL);
            float expectedMeanSolarFlux = (fourthReproSolarFluxData[0] + fourthReproSolarFluxData[1] +
                    fourthReproSolarFluxData[2] + fourthReproSolarFluxData[3] +
                    fourthReproSolarFluxData[4] + fourthReproSolarFluxData[5]) / 6.0f;
            float computedMeanSolarFlux =
                    Meris3rd4thReprocessingAdapter.getMeanSolarFluxFrom4thReprocessing(fourthReproSolarFluxSpectralBand);
            assertEquals(expectedMeanSolarFlux, computedMeanSolarFlux, 1.E-6f);
            assertEquals(expectedMeanSolarFlux, thirdReproSpectralBand.getSolarFlux(), 1.E-6f);
        }
    }

    @Test
    public void testConvertToLowerVersion_detectorIndex() throws IOException {
        // detector index
        final Band fourthReproDetectorIndexBand = fourthReproTestProduct.getBand("detector_index");
        final Band thirdReproDetectorIndexBand = thirdReproProduct.getBand("detector_index");
        assertNotNull(thirdReproDetectorIndexBand);
        int[] fourthReproDetectorIndexData = new int[3 * 2];
        int[] thirdReproDetectorIndexData = new int[3 * 2];
        fourthReproDetectorIndexBand.readPixels(0, 0, 3, 2, fourthReproDetectorIndexData, ProgressMonitor.NULL);
        thirdReproDetectorIndexBand.readPixels(0, 0, 3, 2, thirdReproDetectorIndexData, ProgressMonitor.NULL);
        assertArrayEquals(fourthReproDetectorIndexData, thirdReproDetectorIndexData);
    }

    @Test
    public void testConvertToLowerVersion_flagBands() throws IOException {
        // l1b flag
        final Band fourthReproQualityFlagBand = fourthReproTestProduct.getBand("quality_flags");
        final Band thirdReproL1bFlagBand = thirdReproProduct.getBand("l1_flags");
        assertNotNull(thirdReproL1bFlagBand);
        int[] fourthReproQualityFlagData = new int[3 * 2];
        int[] thirdReproL1bFlagData = new int[3 * 2];
        fourthReproQualityFlagBand.readPixels(0, 0, 3, 2, fourthReproQualityFlagData, ProgressMonitor.NULL);
        thirdReproL1bFlagBand.readPixels(0, 0, 3, 2, thirdReproL1bFlagData, ProgressMonitor.NULL);
        assertNotNull(thirdReproL1bFlagData);

        int[] expectedFourthReproQualityFlagData = new int[]{
                (int) (Math.pow(2.0, 24) + Math.pow(2.0, 21) + Math.pow(2.0, 30)),
                (int) (Math.pow(2.0, 22) + Math.pow(2.0, 23) + Math.pow(2.0, 27)),
                (int) (Math.pow(2.0, 22) + Math.pow(2.0, 23) + Math.pow(2.0, 24)),
                (int) (Math.pow(2.0, 24) + Math.pow(2.0, 30)),
                (int) (Math.pow(2.0, 24) + Math.pow(2.0, 27) + Math.pow(2.0, 30)),
                (int) (Math.pow(2.0, 25)),
        };
        assertArrayEquals(expectedFourthReproQualityFlagData, fourthReproQualityFlagData);
        int[] expectedThirdReproL1bFlagData = new int[]{
                1+8+64,
                2+4+32,
                1+2+4,
                1+64,
                1+32+64,
                128,
        };
        assertArrayEquals(expectedThirdReproL1bFlagData, thirdReproL1bFlagData);
    }

    private void createFourthReproTestProduct() {
        fourthReproTestProduct = new Product("ME_1_RRG_4RP_test", "ME_1_RRG", 3, 2);

        // add bands
        for (int i = 0; i < Meris3rd4thReprocessingAdapter.MERIS_L1B_NUM_SPECTRAL_BANDS; i++) {
            final String fourthReproSpectralBandName = "M" + String.format("%02d", i + 1) + "_radiance";
            Band fourthReproSpectralBand = new Band(fourthReproSpectralBandName, ProductData.TYPE_FLOAT32, 3, 2);
            fourthReproSpectralBand.ensureRasterData();
            float[] testFloat32s = new float[]{(i + 1) * 1.f, (i + 1) * 2.f, (i + 1) * 3.f,
                    (i + 1) * 4.f, (i + 1) * 5.f, (i + 1) * 6.f};
            fourthReproSpectralBand.setPixels(0, 0, 3, 2, testFloat32s);
            fourthReproSpectralBand.setSourceImage(fourthReproSpectralBand.getSourceImage());
            fourthReproTestProduct.addBand(fourthReproSpectralBand);

            Band solarFluxBand = new Band("solar_flux_band_" + (i + 1), ProductData.TYPE_FLOAT32, 3, 2);
            solarFluxBand.ensureRasterData();
            float[] testFloat32sSolarFlux = new float[]{1400 + (i + 1) * 1.f, 1400 + (i + 1) * 2.f, 1400 + (i + 1) * 3.f,
                    1400 + (i + 1) * 4.f, 1400 + (i + 1) * 5.f, 1400 + (i + 1) * 6.f};
            solarFluxBand.setPixels(0, 0, 3, 2, testFloat32sSolarFlux);
            solarFluxBand.setSourceImage(solarFluxBand.getSourceImage());
            fourthReproTestProduct.addBand(solarFluxBand);
        }

        Band detectorIndexBand = new Band("detector_index", ProductData.TYPE_INT16, 3, 2);
        detectorIndexBand.ensureRasterData();
        int[] testInt16sDetectorIndex = new int[]{111, 222, 333, 444, 555, 666};
        detectorIndexBand.setPixels(0, 0, 3, 2, testInt16sDetectorIndex);
        detectorIndexBand.setSourceImage(detectorIndexBand.getSourceImage());
        fourthReproTestProduct.addBand(detectorIndexBand);

        Band qualityFlagBand = new Band("quality_flags", ProductData.TYPE_UINT32, 3, 2);
        qualityFlagBand.ensureRasterData();
        int[] testInt16sQualityFlag = new int[]{
                (int) (Math.pow(2.0, 24) + Math.pow(2.0, 21) + Math.pow(2.0, 30)),
                (int) (Math.pow(2.0, 22) + Math.pow(2.0, 23) + Math.pow(2.0, 27)),
                (int) (Math.pow(2.0, 22) + Math.pow(2.0, 23) + Math.pow(2.0, 24)),
                (int) (Math.pow(2.0, 24) + Math.pow(2.0, 30)),
                (int) (Math.pow(2.0, 24) + Math.pow(2.0, 27) + Math.pow(2.0, 30)),
                (int) (Math.pow(2.0, 25)),
        };
        qualityFlagBand.setPixels(0, 0, 3, 2, testInt16sQualityFlag);
        qualityFlagBand.setSourceImage(qualityFlagBand.getSourceImage());
        fourthReproTestProduct.addBand(qualityFlagBand);

        // add tie point grids:
        // TP_latitude --> latitude
        // TP_longitude --> longitude
        // TP_altitude --> dem_alt
        // SZA --> sun_zenith
        // OZA --> view_zenith
        // SAA --> sun_azimuth
        // OAA --> view_azimuth
        // horizontal_wind_vector_1 --> zonal_wind
        // horizontal_wind_vector_2 --> merid_wind
        // sea_level_pressure --> atm_press
        // total_ozone --> ozone
        // humidity_pressure_level_14 --> rel_hum  // relative humidity at 850 hPa

        float[] tpLatTestData = new float[]{31.f, 32.f, 33.f, 27.f, 28.f, 29.f};
        TiePointGrid tpLatTpg = new TiePointGrid("TP_latitude", 3, 2, 0, 0, 1, 1, tpLatTestData);
        fourthReproTestProduct.addTiePointGrid(tpLatTpg);

        float[] tpLonTestData = new float[]{51.f, 52.f, 33.f, 51.f, 52.f, 53.f};
        TiePointGrid tpLonTpg = new TiePointGrid("TP_longitude", 3, 2, 0, 0, 1, 1, tpLonTestData);
        fourthReproTestProduct.addTiePointGrid(tpLonTpg);

        float[] tpAltTestData = new float[]{234.f, 567.f, 789.f, 1133.f, 1244.f, 1355.f};
        TiePointGrid tpAltTpg = new TiePointGrid("TP_altitude", 3, 2, 0, 0, 1, 1, tpAltTestData);
        fourthReproTestProduct.addTiePointGrid(tpAltTpg);

        float[] szaTestData = new float[]{41.f, 42.f, 43.f, 47.f, 48.f, 49.f};
        TiePointGrid szaTpg = new TiePointGrid("SZA", 3, 2, 0, 0, 1, 1, szaTestData);
        fourthReproTestProduct.addTiePointGrid(szaTpg);

        float[] saaTestData = new float[]{-14.f, -24.f, -34.f, 74.f, 84.f, 89.f};
        TiePointGrid saaTpg = new TiePointGrid("SAA", 3, 2, 0, 0, 1, 1, saaTestData);
        fourthReproTestProduct.addTiePointGrid(saaTpg);

        float[] ozaTestData = new float[]{11.f, 22.f, 33.f, 12.f, 23.f, 34.f};
        TiePointGrid ozaTpg = new TiePointGrid("OZA", 3, 2, 0, 0, 1, 1, ozaTestData);
        fourthReproTestProduct.addTiePointGrid(ozaTpg);

        float[] oaaTestData = new float[]{-55.f, -56.f, -57.f, 58.f, 59.f, 60.f};
        TiePointGrid oaaTpg = new TiePointGrid("OAA", 3, 2, 0, 0, 1, 1, oaaTestData);
        fourthReproTestProduct.addTiePointGrid(oaaTpg);

        float[] zonalWindTestData = new float[]{10.f, 11.f, 12.f, 16.f, 17.f, 18.f};
        TiePointGrid zonalWindTpg = new TiePointGrid("horizontal_wind_vector_1", 3, 2, 0, 0, 1, 1, zonalWindTestData);
        fourthReproTestProduct.addTiePointGrid(zonalWindTpg);

        float[] meridionalWindTestData = new float[]{5.f, 6.f, 7.f, 2.f, 3.f, 4.f};
        TiePointGrid meridionalWindTpg = new TiePointGrid("horizontal_wind_vector_2", 3, 2, 0, 0, 1, 1, meridionalWindTestData);
        fourthReproTestProduct.addTiePointGrid(meridionalWindTpg);

        float[] slpTestData = new float[]{1005.f, 1006.f, 1008.f, 1002.f, 1003.f, 1004.f};
        TiePointGrid slpTpg = new TiePointGrid("sea_level_pressure", 3, 2, 0, 0, 1, 1, slpTestData);
        fourthReproTestProduct.addTiePointGrid(slpTpg);

        float[] ozoneTestData = new float[]{0.005f, 0.0055f, 0.006f, 0.0065f, 0.007f, 0.0075f};
        TiePointGrid ozoneTpg = new TiePointGrid("total_ozone", 3, 2, 0, 0, 1, 1, ozoneTestData);
        fourthReproTestProduct.addTiePointGrid(ozoneTpg);

        float[] relHumTestData = new float[]{2.34f, 13.7f, 11.5f, 33.4f, 35.8f, 39.2f};
        TiePointGrid relHumTpg = new TiePointGrid("humidity_pressure_level_14", 3, 2, 0, 0, 1, 1, relHumTestData);
        fourthReproTestProduct.addTiePointGrid(relHumTpg);

        fourthReproTestProduct.setAutoGrouping("M*_radiance:solar_flux");

        // metadata
        final MetadataElement manifestElement = new MetadataElement("Manifest");
        fourthReproTestProduct.getMetadataRoot().addElement(manifestElement);
        final MetadataElement metadataSectionElement = new MetadataElement("metadataSection");
        manifestElement.addElement(metadataSectionElement);
        final MetadataElement generalProductInformationElement = new MetadataElement("generalProductInformation");
        metadataSectionElement.addElement(generalProductInformationElement);
        generalProductInformationElement.addAttribute(new MetadataAttribute("productName",
                ProductData.createInstance(fourthReproTestProduct.getName()), true));
        final MetadataElement acquisitionPeriodElement = new MetadataElement("acquisitionPeriod");
        metadataSectionElement.addElement(acquisitionPeriodElement);
        acquisitionPeriodElement.addAttribute(new MetadataAttribute("startTime",
                ProductData.createInstance("2011-07-02T14:08:01.955726Z"), true));
        acquisitionPeriodElement.addAttribute(new MetadataAttribute("stopTime",
                ProductData.createInstance("2011-07-02T14:51:57.552001Z"), true));
        final MetadataElement orbitReferenceElement = new MetadataElement("orbitReference");
        orbitReferenceElement.addAttribute(new MetadataAttribute("cycleNumber",
                ProductData.createInstance(new int[]{104}), true));
        final MetadataElement orbitNumberElement = new MetadataElement("orbitNumber");
        orbitNumberElement.addAttribute(new MetadataAttribute("orbitNumber",
                ProductData.createInstance(new int[]{48832}), true));
        orbitReferenceElement.addElement(orbitNumberElement);
        final MetadataElement relativeOrbitNumberElement = new MetadataElement("relativeOrbitNumber");
        relativeOrbitNumberElement.addAttribute(new MetadataAttribute("relativeOrbitNumber",
                ProductData.createInstance(new int[]{111}), true));
        orbitReferenceElement.addElement(relativeOrbitNumberElement);
        metadataSectionElement.addElement(orbitReferenceElement);
    }
}
