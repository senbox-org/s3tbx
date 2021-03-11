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
package org.esa.s3tbx.operator.cloud;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.ceres.glevel.MultiLevelImage;
import org.esa.s3tbx.operator.cloud.internal.ProcessingNode;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.jexp.ParseException;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.math.MathUtils;
import org.esa.snap.dataio.envisat.EnvisatConstants;

import java.awt.Color;
import java.awt.Rectangle;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * A processing node to compute a cloud_probability mask using a neural network.
 */
class CloudPN extends ProcessingNode {

    public static final String CONFIG_FILE_NAME = "config_file_name";
    public static final String INVALID_EXPRESSION = "invalid_expression";

    public static final String CLOUD_PROP_BAND = "cloud_prob";
    public static final String CLOUD_FLAG_BAND = "cloud_flag";

    private static final String DEFAULT_OUTPUT_PRODUCT_NAME = "MER_CLOUD";
    private static final String PRODUCT_TYPE = "MER_L2_CLOUD";

    private static final String DEFAULT_VALID_LAND_EXP = "not l1_flags.INVALID and dem_alt > -50";
    private static final String DEFAULT_VALID_OCEAN_EXP = "not l1_flags.INVALID and dem_alt <= -50";
    private static final float SCALING_FACTOR = 0.0001f;

    private static final String PRESS_SCALE_HEIGHT_KEY = "press_scale_height";


    private static final int FLAG_CLOUDY = 1;
    private static final int FLAG_CLOUDFREE = 2;
    private static final int FLAG_UNCERTAIN = 4;
    public static final String ATM_PRESS = "atm_press";
    private final String auxdataDir;

    private RasterDataNode szaGrid;
    private RasterDataNode saaGrid;
    private RasterDataNode vzaGrid;
    private RasterDataNode vaaGrid;
    private RasterDataNode pressGrid;
    private RasterDataNode altitude;


    private float[] centralWavelength;
    private CentralWavelengthProvider centralWavelengthProvider;

    private String validLandExpression;
    private String validOceanExpression;

    private Logger logger;
    private Band cloudBand;
    private Band cloudFlagBand;
    private Band detectorBand;
    private Band[] radianceBands;

    private CloudAlgorithm landAlgo;
    private CloudAlgorithm oceanAlgo;
    /**
     * Pressure scale height to account for altitude.
     */
    private int pressScaleHeight;
    private MultiLevelImage validLandImage;
    private MultiLevelImage validOceanImage;
    private MultiLevelImage landImage;

    public CloudPN(String auxdataDir) {
        super();
        this.auxdataDir = auxdataDir;
        logger = SystemUtils.LOG;
    }

    @Override
    public void setUp(final Map config) throws IOException {

        final File propertiesFile = new File(auxdataDir, (String) config.get(CONFIG_FILE_NAME));
        final InputStream propertiesStream = new FileInputStream(propertiesFile);
        Properties properties = new Properties();
        properties.load(propertiesStream);

        landAlgo = new CloudAlgorithm(new File(auxdataDir), properties.getProperty("land"));
        oceanAlgo = new CloudAlgorithm(new File(auxdataDir), properties.getProperty("ocean"));

        pressScaleHeight = Integer.parseInt(properties.getProperty(PRESS_SCALE_HEIGHT_KEY));

        centralWavelengthProvider = new CentralWavelengthProvider();
        centralWavelengthProvider.readAuxData(new File(auxdataDir));

        validLandExpression = landAlgo.getValidExpression();
        if (validLandExpression.isEmpty()) {
            validLandExpression = DEFAULT_VALID_LAND_EXP;
        }

        validOceanExpression = oceanAlgo.getValidExpression();
        if (validOceanExpression.isEmpty()) {
            validOceanExpression = DEFAULT_VALID_OCEAN_EXP;
        }
    }

    /**
     * Creates the output product skeleton.
     */
    @Override
    protected Product createTargetProductImpl() {
        Product l1Product = getSourceProduct();

        String[] radianceBandNames = EnvisatConstants.MERIS_L1B_SPECTRAL_BAND_NAMES;
        radianceBands = new Band[radianceBandNames.length];
        for (int bandIndex = 0; bandIndex < radianceBandNames.length; bandIndex++) {
            String radianceBandName = radianceBandNames[bandIndex];
            radianceBands[bandIndex] = l1Product.getBand(radianceBandName);
            if (radianceBands[bandIndex] == null) {
                throw new IllegalArgumentException("Source product does not contain band " + radianceBandName);
            }
        }
        detectorBand = l1Product.getBand(EnvisatConstants.MERIS_DETECTOR_INDEX_DS_NAME);
        if (detectorBand == null) {
            throw new IllegalArgumentException("Source product does not contain detector band.");
        }
        // get the scene size from the input product
        final int sceneWidth = l1Product.getSceneRasterWidth();
        final int sceneHeight = l1Product.getSceneRasterHeight();

        // create the output product
        Product outputProduct = new Product(DEFAULT_OUTPUT_PRODUCT_NAME, PRODUCT_TYPE, sceneWidth, sceneHeight);

        cloudBand = new Band(CLOUD_PROP_BAND, ProductData.TYPE_INT16, sceneWidth, sceneHeight);
        cloudBand.setDescription("Probability of clouds");
        cloudBand.setScalingFactor(SCALING_FACTOR);
        cloudBand.setNoDataValueUsed(true);
        cloudBand.setNoDataValue(-1);
        outputProduct.addBand(cloudBand);

        // create and add the flags coding
        FlagCoding cloudFlagCoding = addCloudFlagCoding(outputProduct);

        // create and add the SDR flags dataset
        cloudFlagBand = new Band(CLOUD_FLAG_BAND, ProductData.TYPE_UINT8, sceneWidth, sceneHeight);
        cloudFlagBand.setDescription("Cloud specific flags");
        cloudFlagBand.setSampleCoding(cloudFlagCoding);
        outputProduct.addBand(cloudFlagBand);

        logger.info("Output product successfully created");
        return outputProduct;
    }

    private static FlagCoding addCloudFlagCoding(Product outputProduct) {
        MetadataAttribute cloudAttr;
        final FlagCoding flagCoding = new FlagCoding(CLOUD_FLAG_BAND);
        flagCoding.setDescription("Cloud Flag Coding");
        outputProduct.getFlagCodingGroup().add(flagCoding);

        cloudAttr = new MetadataAttribute("cloudy", ProductData.TYPE_UINT8);
        cloudAttr.getData().setElemInt(FLAG_CLOUDY);
        cloudAttr.setDescription("is with more than 80% cloudy");
        flagCoding.addAttribute(cloudAttr);
        outputProduct.addMask(cloudAttr.getName(),
                              flagCoding.getName() + "." + cloudAttr.getName(), cloudAttr.getDescription(),
                              createBitmaskColor(1, 3), 0.5);

        cloudAttr = new MetadataAttribute("cloudfree", ProductData.TYPE_UINT8);
        cloudAttr.getData().setElemInt(FLAG_CLOUDFREE);
        cloudAttr.setDescription("is with less than 20% cloudy");
        flagCoding.addAttribute(cloudAttr);
        outputProduct.addMask(cloudAttr.getName(),
                              flagCoding.getName() + "." + cloudAttr.getName(), cloudAttr.getDescription(),
                              createBitmaskColor(2, 3), 0.5);

        cloudAttr = new MetadataAttribute("cloud_uncertain", ProductData.TYPE_UINT8);
        cloudAttr.getData().setElemInt(FLAG_UNCERTAIN);
        cloudAttr.setDescription("is with between 20% and 80% cloudy");
        flagCoding.addAttribute(cloudAttr);
        outputProduct.addMask(cloudAttr.getName(),
                              flagCoding.getName() + "." + cloudAttr.getName(), cloudAttr.getDescription(),
                              createBitmaskColor(3, 3), 0.5);

        return flagCoding;
    }

    /**
     * Creates a new color object to be used in the bitmaskDef.
     * The given indices start with 1.
     */
    private static Color createBitmaskColor(int index, int maxIndex) {
        final double rf1 = 0.0;
        final double gf1 = 0.5;
        final double bf1 = 1.0;

        final double a = 2 * Math.PI * index / maxIndex;

        return new Color((float) (0.5 + 0.5 * Math.sin(a + rf1 * Math.PI)),
                         (float) (0.5 + 0.5 * Math.sin(a + gf1 * Math.PI)),
                         (float) (0.5 + 0.5 * Math.sin(a + bf1 * Math.PI)));
    }

    private void ensureCentralWavelengthIsSet() {
        if (centralWavelength == null) {
            centralWavelength = centralWavelengthProvider.getCentralWavelength(getSourceProduct().getProductType());
        }
    }

    @Override
    protected void processFrame(int frameX, int frameY, int frameW, int frameH, ProgressMonitor pm) throws IOException {
        ensureCentralWavelengthIsSet();
        final int frameSize = frameW * frameH;
        final int numBands = radianceBands.length;
        final double[] cloudIn = new double[15];

        float[] szaScanLine;
        float[] saaScanLine;
        float[] vzaScanLine;
        float[] vaaScanLine;
        float[] pressScanLine;
        float[] altitudeScanLine;
        int[] detectorScanLine;
        float[][] radianceScanLine;
        int[] validLandScanLine;
        int[] validOceanScanLine;
        int[] landScanLine;

        szaScanLine = new float[frameSize];
        saaScanLine = new float[frameSize];
        vzaScanLine = new float[frameSize];
        vaaScanLine = new float[frameSize];
        pressScanLine = new float[frameSize];
        altitudeScanLine = new float[frameSize];
        detectorScanLine = new int[frameSize];
        radianceScanLine = new float[numBands][frameSize];
        validLandScanLine = new int[frameSize];
        validOceanScanLine = new int[frameSize];
        landScanLine = new int[frameSize];

        pm.beginTask("Processing frame...", 9+radianceBands.length + frameSize);
        try {
            szaGrid.readPixels(frameX, frameY, frameW, frameH, szaScanLine, SubProgressMonitor.create(pm, 1));
            saaGrid.readPixels(frameX, frameY, frameW, frameH, saaScanLine, SubProgressMonitor.create(pm, 1));
            vzaGrid.readPixels(frameX, frameY, frameW, frameH, vzaScanLine, SubProgressMonitor.create(pm, 1));
            vaaGrid.readPixels(frameX, frameY, frameW, frameH, vaaScanLine, SubProgressMonitor.create(pm, 1));
            pressGrid.readPixels(frameX, frameY, frameW, frameH, pressScanLine, SubProgressMonitor.create(pm, 1));
            altitude.readPixels(frameX, frameY, frameW, frameH, altitudeScanLine, SubProgressMonitor.create(pm, 1));


            ProgressMonitor subPM = SubProgressMonitor.create(pm, 1);
            try {
                subPM.beginTask("Reading radiance bands...", radianceBands.length);
                for (int i = 0; i < radianceBands.length; i++) {
                    final Band radianceBand = radianceBands[i];
                    radianceBand.readPixels(frameX, frameY, frameW, frameH, radianceScanLine[i],
                                            SubProgressMonitor.create(pm, 1));
                }
            } finally {
                subPM.done();
            }
            detectorBand.readPixels(frameX, frameY, frameW, frameH, detectorScanLine, SubProgressMonitor.create(pm, 1));

            validLandScanLine = validLandImage.getData(new Rectangle(frameX, frameY, frameW, frameH)).getSamples(frameX, frameY, frameW, frameH, 0, validLandScanLine);
            pm.worked(1);
//            getSourceProduct().readBitmask(frameX, frameY, frameW, frameH, validLandTerm, validLandScanLine,
//                                           SubProgressMonitor.create(pm, 1));
            validOceanScanLine = validOceanImage.getData(new Rectangle(frameX, frameY, frameW, frameH)).getSamples(frameX, frameY, frameW, frameH, 0, validOceanScanLine);
            pm.worked(1);
//            getSourceProduct().readBitmask(frameX, frameY, frameW, frameH, validOceanTerm, validOceanScanLine,
//                                           SubProgressMonitor.create(pm, 1));
            landScanLine = landImage.getData(new Rectangle(frameX, frameY, frameW, frameH)).getSamples(frameX, frameY, frameW, frameH, 0, landScanLine);
            pm.worked(1);
//            getSourceProduct().readBitmask(frameX, frameY, frameW, frameH, landTerm, landScanLine,
//                                           SubProgressMonitor.create(pm, 1));

            ProductData data = getFrameData(cloudBand);
            //noinspection MismatchedReadAndWriteOfArray
            short[] cloudScanLine = (short[]) data.getElems();

            ProductData flagData = getFrameData(cloudFlagBand);
            //noinspection MismatchedReadAndWriteOfArray
            byte[] flagScanLine = (byte[]) flagData.getElems();

            for (int i = 0; i < frameSize; i++) {
                if (pm.isCanceled()) {
                    break;
                }
                flagScanLine[i] = 0;
                if (!isValid(validLandScanLine[i]) && !isValid(validOceanScanLine[i])) {
                    cloudScanLine[i] = -1;
                } else {
                    final double aziDiff = computeAda(vaaScanLine[i], saaScanLine[i]) * MathUtils.DTOR;
                    cloudIn[0] = calculateI(radianceScanLine[0][i], radianceBands[0].getSolarFlux(), szaScanLine[i]);
                    cloudIn[1] = calculateI(radianceScanLine[1][i], radianceBands[1].getSolarFlux(), szaScanLine[i]);
                    cloudIn[2] = calculateI(radianceScanLine[2][i], radianceBands[2].getSolarFlux(), szaScanLine[i]);
                    cloudIn[3] = calculateI(radianceScanLine[3][i], radianceBands[3].getSolarFlux(), szaScanLine[i]);
                    cloudIn[4] = calculateI(radianceScanLine[4][i], radianceBands[4].getSolarFlux(), szaScanLine[i]);
                    cloudIn[5] = calculateI(radianceScanLine[5][i], radianceBands[5].getSolarFlux(), szaScanLine[i]);
                    cloudIn[6] = calculateI(radianceScanLine[8][i], radianceBands[8].getSolarFlux(), szaScanLine[i]);
                    cloudIn[7] = calculateI(radianceScanLine[9][i], radianceBands[9].getSolarFlux(), szaScanLine[i]);
                    cloudIn[8] = calculateI(radianceScanLine[12][i], radianceBands[12].getSolarFlux(), szaScanLine[i]);
                    cloudIn[9] = (radianceScanLine[10][i] * radianceBands[9].getSolarFlux()) / (radianceScanLine[9][i] * radianceBands[10].getSolarFlux());
                    cloudIn[10] = altitudeCorrectedPressure(pressScanLine[i], altitudeScanLine[i], isValid(landScanLine[i]));
                    cloudIn[11] = centralWavelength[detectorScanLine[i]]; // central-wavelength channel 11
                    cloudIn[12] = Math.cos(szaScanLine[i] * MathUtils.DTOR);
                    cloudIn[13] = Math.cos(vzaScanLine[i] * MathUtils.DTOR);
                    cloudIn[14] = Math.cos(aziDiff) * Math.sin(vzaScanLine[i] * MathUtils.DTOR);

                    double cloudProbability = 0;
                    if (isValid(validLandScanLine[i])) {
                        cloudProbability = landAlgo.computeCloudProbability(cloudIn);
                    } else if (isValid(validOceanScanLine[i])) {
                        cloudProbability = oceanAlgo.computeCloudProbability(cloudIn);
                    }
                    short cloudProbabilityScaled = (short) (cloudProbability / SCALING_FACTOR);

                    if (cloudProbabilityScaled > 8000) {
                        flagScanLine[i] = FLAG_CLOUDY;
                    } else if (cloudProbabilityScaled < 2000) {
                        flagScanLine[i] = FLAG_CLOUDFREE;
                    } else if (cloudProbabilityScaled >= 2000 && cloudProbabilityScaled <= 8000) {
                        flagScanLine[i] = FLAG_UNCERTAIN;
                    }
                    cloudScanLine[i] = cloudProbabilityScaled;
                }
                pm.worked(1);
            }
        } finally {
            pm.done();
        }

    }

    private boolean isValid(int maskValue) {
        return maskValue == 255;
    }

    protected double calculateI(double radiance, float sunSpectralFlux, double sunZenith) {
        return (radiance / (sunSpectralFlux * Math.cos(sunZenith * MathUtils.DTOR)));
    }

    protected double altitudeCorrectedPressure(double pressure, double altitude, boolean isLand) {
        double correctedPressure;
        if (isLand) {
            // ECMWF pressure is only corrected for positive altitudes and only for land pixels */
            double f = Math.exp(-Math.max(0.0, altitude) / pressScaleHeight);
            correctedPressure = pressure * f;
        } else {
            correctedPressure = pressure;
        }
        return correctedPressure;
    }

    /**
     * Computes the azimuth difference from the given
     *
     * @param vaa viewing azimuth angle [degree]
     * @param saa sun azimuth angle [degree]
     * @return the azimuth difference [degree]
     */
    private static double computeAda(final double vaa, final double saa) {
        double ada = vaa - saa;
        if (ada <= -180.0) {
            ada = +360.0 + ada;
        } else if (ada > +180.0) {
            ada = -360.0 + ada;
        }
        if (ada >= 0.0) {
            ada = +180.0 - ada;
        } else {
            ada = -180.0 - ada;
        }
        return ada;
    }

    @Override
    public void startProcessing() {
        final Product l1bProduct = getSourceProduct();

        szaGrid = getRasterData(EnvisatConstants.MERIS_SUN_ZENITH_DS_NAME);
        saaGrid = getRasterData(EnvisatConstants.MERIS_SUN_AZIMUTH_DS_NAME);
        vzaGrid = getRasterData(EnvisatConstants.MERIS_VIEW_ZENITH_DS_NAME);
        vaaGrid = getRasterData(EnvisatConstants.MERIS_VIEW_AZIMUTH_DS_NAME);
        altitude = getRasterData(EnvisatConstants.MERIS_DEM_ALTITUDE_DS_NAME);
        pressGrid = getRasterData(ATM_PRESS);

        if (altitude == null) {
            altitude = l1bProduct.getBand(EnvisatConstants.MERIS_AMORGOS_L1B_ALTIUDE_BAND_NAME);
        }

        validLandImage = l1bProduct.getMaskImage(validLandExpression, null);
        validOceanImage = l1bProduct.getMaskImage(validOceanExpression, null);
        landImage = l1bProduct.getMaskImage("l1_flags.LAND_OCEAN", null);

    }


    private RasterDataNode getRasterData(String tieOrBandName) {
        Product sourceProduct = getSourceProduct();
        if (isTiePoint(tieOrBandName)) {
            return sourceProduct.getTiePointGrid(tieOrBandName);
        } else {
            return sourceProduct.getBand(tieOrBandName);
        }
    }

    private boolean isTiePoint(String name) {
        List<String> tiePointNameList = Arrays.asList(getSourceProduct().getTiePointGridNames());
        return tiePointNameList.contains(name);
    }
}
