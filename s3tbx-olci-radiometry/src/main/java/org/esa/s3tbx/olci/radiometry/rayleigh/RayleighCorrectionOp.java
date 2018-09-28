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

package org.esa.s3tbx.olci.radiometry.rayleigh;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s3tbx.olci.radiometry.Sensor;
import org.esa.s3tbx.olci.radiometry.SensorConstants;
import org.esa.s3tbx.olci.radiometry.gasabsorption.GaseousAbsorptionAux;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.util.BitSetter;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.dataio.envisat.EnvisatConstants;

import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import static org.esa.s3tbx.olci.radiometry.SensorConstants.*;
import static org.esa.s3tbx.olci.radiometry.smilecorr.SmileCorrectionUtils.*;

/**
 * This operator performs radiometric corrections on OLCI, MERIS L1b and S2 MSI data products.
 *
 * @author Marco Peters, Muhammad Bala, Olaf Danne
 */
@OperatorMetadata(alias = "RayleighCorrection",
        description = "Performs radiometric corrections on OLCI, MERIS L1b and S2 MSI data products.",
        authors = "Marco Peters, Muhammad Bala, Olaf Danne (Brockmann Consult)",
        copyright = "(c) 2016 by Brockmann Consult",
        category = "Optical/Pre-Processing",
        version = "1.3")
public class RayleighCorrectionOp extends Operator {

    private static final String AUTO_GROUPING = "rtoa:taur:rtoa_ng:rtoaRay:rBRR";
    private static final int WV_709_FOR_GASEOUS_ABSORPTION_CALCULATION = 709;
    private static final String SOLAR_FLUX_BAND_PATTERN = "solar_flux_band_%d";

    private static final String LATITUDE = "latitude";
    private static final String TP_LATITUDE = "TP_latitude";
    private static final String LONGITUDE = "longitude";
    private static final String TP_LONGITUDE = "TP_longitude";
    private static final String TP_ALTITUDE = "TP_altitude";
    private static final String AIRMASS = "airmass";
    private static final String ALTITUDE_DEM = "dem_alt";

    static final String[] BAND_CATEGORIES = new String[]{
            "taur_%02d",
            "rBRR_%02d",
            "rtoa_ng_%02d",
            "rtoa_%02d",
    };
    static final String R_BRR_PATTERN = "rBRR_\\d{2}";
    static final String RTOA_PATTERN = "rtoa_\\d{2}";
    static final String TAUR_PATTERN = "taur_\\d{2}";
    static final String RTOA_NG_PATTERN = "rtoa_ng_\\d{2}";

    @SourceProduct(label = "OLCI, MERIS or S2 MSI L1b product")
    Product sourceProduct;


    @Parameter(label = "Source bands", description = "The source bands for the computation.", rasterDataNodeType = Band.class)
    private String[] sourceBandNames;

    @Parameter(defaultValue = "false", label = "Compute Rayleigh optical thickness bands")
    private boolean computeTaur;


    @Parameter(defaultValue = "true", label = "Compute bottom of Rayleigh reflectance bands")
    private boolean computeRBrr;

    @Parameter(defaultValue = "false", label = "Compute gaseous absorption corrected TOA reflectance bands")
    private boolean computeRtoaNg;

    @Parameter(defaultValue = "false", label = "Compute TOA reflectance bands")
    private boolean computeRtoa;

    @Parameter(defaultValue = "false", label = "Add air mass")
    private boolean addAirMass;

    @Parameter(defaultValue = "20",
            valueSet = {"10", "20", "60"},
            label = "Image resolution in m in target product (S2 MSI only)")
    private int s2MsiTargetResolution;

//    @Parameter(defaultValue = "true", label = "Add geometry bands to the target product (S2 MSI only)")
//    private boolean s2AddGeometryBands;

    @Parameter(defaultValue = "1013.25", label = "Sea level pressure in hPa (S2 MSI only)")
    private double s2MsiSeaLevelPressure;

    @Parameter(defaultValue = "300.0", label = "Ozone in DU (S2 MSI only)")
    private double s2MsiOzone;


    private RayleighCorrAlgorithm algorithm;
    private Sensor sensor;
    private double[] absorpOzone;
    private double[] crossSectionSigma;

    private Product productToProcess;


    @Override
    public void initialize() throws OperatorException {

        if (this.sourceBandNames == null || this.sourceBandNames.length == 0) {
            throw new OperatorException("Please select at least one source band.");
        }

        sensor = getSensorType(sourceProduct);
        productToProcess = sourceProduct;
        if (sensor == Sensor.S2_MSI) {
            if (S2Utils.getNumBandsToRcCorrect(sourceBandNames) <= 0) {
                throw new OperatorException
                        ("Please select spectral band(s) between B1 and B9 for S2 MSI Rayleigh Correction.");
            }
            if (sourceProduct.isMultiSize()) {
                productToProcess = S2Rescaling.getS2ProductWithRescaledSourceBands(sourceProduct,
                                                                                   sourceBandNames,
                                                                                   s2MsiTargetResolution);
            }
        }

        algorithm = new RayleighCorrAlgorithm(sensor);
        absorpOzone = GaseousAbsorptionAux.getInstance().absorptionOzone(sensor.getName());
        crossSectionSigma = getCrossSectionSigma(sourceProduct, sensor.getNumBands(), sensor.getNameFormat());

        Product targetProduct = new Product(productToProcess.getName() + "_rayleigh", productToProcess.getProductType(),
                                            productToProcess.getSceneRasterWidth(), productToProcess.getSceneRasterHeight());

        RayleighAux.initDefaultAuxiliary();
        addTargetBands(productToProcess, targetProduct);
        ProductUtils.copyProductNodes(productToProcess, targetProduct);
        ProductUtils.copyFlagBands(productToProcess, targetProduct, true);

        targetProduct.setAutoGrouping(AUTO_GROUPING);
        setTargetProduct(targetProduct);
    }


    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {
        checkForCancellation();

        Set<Map.Entry<Band, Tile>> entries = targetTiles.entrySet();
        entries.forEach(targetTileStream -> {
            Tile targetTile = targetTileStream.getValue();
            Band targetBand = targetTileStream.getKey();

            String targetBandName = targetBand.getName();
            double[] targetData = null;

            Tile qualityFlagsTile = null;
            final String l1bFlagsName = sensor.getL1bFlagsName();
            if (l1bFlagsName != null) {
                qualityFlagsTile = getSourceTile(productToProcess.getBand(l1bFlagsName), targetRectangle);
            }
            RayleighAux rayleighAux = createAuxiliary(productToProcess, sensor, targetRectangle);

            if (targetBandName.equals(AIRMASS) && addAirMass) {
                targetData = rayleighAux.getAirMass();
                setTargetSamples(qualityFlagsTile, targetTile, targetData);
            }

            final int sourceBandIndex = getSourceBandIndex(targetBandName);
            // note that sourceBandIndex is 8 for both S2 B8 and B8A!
            if (sourceBandIndex != -1) {
                double[] rayleighOpticalThickness;
                addAuxiliaryData(productToProcess, rayleighAux, targetRectangle, sourceBandIndex, targetBandName);

                if (targetBandNameMatches(targetBandName, RTOA_PATTERN) && computeRtoa) {
                    if (sensor == Sensor.S2_MSI) {
                        targetData = rayleighAux.getSourceSampleRad();
                    } else {
                        targetData = getReflectance(rayleighAux);
                    }
                } else if (targetBandNameMatches(targetBandName, TAUR_PATTERN) && computeTaur) {
                    targetData = algorithm.getRayleighThickness(rayleighAux, crossSectionSigma, sourceBandIndex, targetBandName);
                } else if (computeRBrr || computeRtoaNg) {
                    double[] reflectance;
                    if (sensor == Sensor.S2_MSI) {
                        reflectance = rayleighAux.getSourceSampleRad();
                    } else {
                        reflectance = getReflectance(rayleighAux);
                    }

                    if (Math.ceil(rayleighAux.getWaveLength()) == WV_709_FOR_GASEOUS_ABSORPTION_CALCULATION) {
                        reflectance = waterVaporCorrection709(reflectance, targetRectangle, sensor);
                    }
                    double[] corrOzoneRefl = getCorrectOzone(rayleighAux, reflectance, sourceBandIndex, targetBandName);
                    if (targetBandNameMatches(targetBandName, RTOA_NG_PATTERN) && computeRtoaNg) {
                        targetData = corrOzoneRefl;
                    }
                    if (targetBandNameMatches(targetBandName, R_BRR_PATTERN) && computeRBrr) {
                        if (sensor == Sensor.S2_MSI && sourceBandIndex >= 10) {
                            // skip B10-12 before we better know what to do
                            targetData = corrOzoneRefl;
                        } else {
                            rayleighOpticalThickness = algorithm.getRayleighThickness(rayleighAux, crossSectionSigma, sourceBandIndex, targetBandName);
                            targetData = getRhoBrr(rayleighAux, rayleighOpticalThickness, corrOzoneRefl);
                        }
                    }
                }

                setTargetSamples(qualityFlagsTile, targetTile, targetData);
            }
        });
    }

    private void setTargetSamples(Tile qualityFlagsTile, Tile targetTile, double[] targetData) {
        Object qualityFlags;
        // we have no quality flag for S2
        if (qualityFlagsTile != null) {
            if (sensor == Sensor.MERIS) {
                qualityFlags = qualityFlagsTile.getSamplesByte();
                filterInvalid(targetData, (byte[]) qualityFlags);
            } else if (sensor == Sensor.MERIS_4TH || sensor == Sensor.OLCI) {
                qualityFlags = qualityFlagsTile.getSamplesInt();
                filterInvalid(targetData, (int[]) qualityFlags);
            }
        }

        targetTile.setSamples(targetData);
    }

    private void filterInvalid(double[] targetData, int[] qualityFlags) {
        if (targetData.length != qualityFlags.length) {
            throw new OperatorException("targetData.length != qualityFlags.length");
        }
        for (int i = 0; i < targetData.length; i++) {
            if (BitSetter.isFlagSet(qualityFlags[i], sensor.getInvalidBit())) {
                targetData[i] = RayleighConstants.INVALID_VALUE;
            }
        }
    }

    private void filterInvalid(double[] targetData, byte[] qualityFlags) {
        if (targetData.length != qualityFlags.length) {
            throw new OperatorException("targetData.length != qualityFlags.length");
        }
        for (int i = 0; i < targetData.length; i++) {
            if (BitSetter.isFlagSet(qualityFlags[i], sensor.getInvalidBit())) {
                targetData[i] = RayleighConstants.INVALID_VALUE;
            }
        }
    }

    private double[] waterVaporCorrection709(double[] reflectances, Rectangle targetRectangle, Sensor sensor) {
        String bandNameFormat = sensor.getNameFormat();
        int[] upperLowerBounds = sensor.getBounds();
        double[] bWVRefTile = getSampleDoubles(getSourceTile(sourceProduct.getBand(String.format(bandNameFormat, upperLowerBounds[1])), targetRectangle));
        double[] bWVTile = getSampleDoubles(getSourceTile(sourceProduct.getBand(String.format(bandNameFormat, upperLowerBounds[0])), targetRectangle));
        return algorithm.waterVaporCorrection709(reflectances, bWVRefTile, bWVTile);
    }

    private double[] getRhoBrr(RayleighAux rayleighAux, double[] rayleighOpticalThickness, double[] corrOzoneRefl) {
        return algorithm.getRhoBrr(rayleighAux, rayleighOpticalThickness, corrOzoneRefl);
    }

    private double[] getCorrectOzone(RayleighAux rayleighAux, double[] reflectance, int sourceBandIndex,
                                     String targetBandName) {

        final int absorpOzoneIndex = getAbsorpOzoneIndex(sourceBandIndex, targetBandName);

        double absorpO = absorpOzone[absorpOzoneIndex];
        double[] totalOzones = rayleighAux.getTotalOzones();
        double[] cosOZARads = rayleighAux.getCosOZARads();
        double[] cosSZARads = rayleighAux.getCosSZARads();

        return algorithm.getCorrOzone(reflectance, absorpO, totalOzones, cosOZARads, cosSZARads);
    }

    private int getAbsorpOzoneIndex(int sourceBandIndex, String targetBandName) {
        if (sensor != null && sensor == Sensor.S2_MSI) {
            return S2Utils.getS2SpectralBandIndex(targetBandName);
        } else {
            return sourceBandIndex - 1;
        }
    }

    private double[] getReflectance(RayleighAux rayleighAux) {
        double[] sourceSampleRad = rayleighAux.getSourceSampleRad();
        double[] solarFluxs = rayleighAux.getSolarFluxs();
        double[] sunZenithAngles = rayleighAux.getSunZenithAngles();

        return algorithm.convertRadsToRefls(sourceSampleRad, solarFluxs, sunZenithAngles);
    }

    private void addTargetBands(Product sourceProduct, Product targetProduct) {
        if (computeTaur) {
            addTargetBands(sourceProduct, targetProduct, BAND_CATEGORIES[0]);
        }
        if (computeRBrr) {
            addTargetBands(sourceProduct, targetProduct, BAND_CATEGORIES[1]);
        }
        if (computeRtoaNg) {
            addTargetBands(sourceProduct, targetProduct, BAND_CATEGORIES[2]);
        }
        if (computeRtoa) {
            addTargetBands(sourceProduct, targetProduct, BAND_CATEGORIES[3]);
        }
        if (addAirMass) {
            final Band targetBand = targetProduct.addBand(AIRMASS, ProductData.TYPE_FLOAT32);
            targetBand.setNoDataValue(RayleighConstants.INVALID_VALUE);
            targetBand.setNoDataValueUsed(true);
        }
//        if (sensor == Sensor.S2_MSI && s2AddGeometryBands) {
//            for (String s2GeometryBand : SensorConstants.S2_GEOMETRY_BANDS) {
//                ProductUtils.copyBand(s2GeometryBand, sourceProduct, targetProduct, true);
//            }
//        }
        for (String s2GeometryBand : SensorConstants.S2_GEOMETRY_BANDS) {
            ProductUtils.copyBand(s2GeometryBand, sourceProduct, targetProduct, true);
        }
    }

    private void addTargetBands(Product sourceProduct, Product targetProduct, String bandCategory) {
        for (String sourceBandName : sourceBandNames) {
            Band sourceBand = sourceProduct.getBand(sourceBandName);
            if (sourceBand != null) {
                final int spectralBandIndex = getSpectralBandIndex(sourceBand);
                if (spectralBandIndex >= 0 && spectralBandIndex < sensor.getNumBands()) {
                    final String targetBandName = getTargetBandName(bandCategory, sourceBand);
                    Band targetBand = targetProduct.addBand(targetBandName,
                                                            ProductData.TYPE_FLOAT32);
                    targetBand.setNoDataValue(RayleighConstants.INVALID_VALUE);
                    targetBand.setNoDataValueUsed(true);
                    ProductUtils.copySpectralBandProperties(sourceBand, targetBand);
                }
            }
        }
    }

    private String getTargetBandName(String bandCategory, Band sourceBand) {
        if (sensor == Sensor.S2_MSI) {
            return S2Utils.getS2TargetBandName(bandCategory, sourceBand.getName());
        } else {
            return String.format(bandCategory, sourceBand.getSpectralBandIndex() + 1);
        }
    }

    private int getSpectralBandIndex(Band sourceBand) {
        if (sensor == Sensor.S2_MSI) {
            return S2Utils.getS2SpectralBandIndex(sourceBand.getName());
        } else {
            return sourceBand.getSpectralBandIndex();
        }
    }

    private boolean targetBandNameMatches(String targetBandName, String pattern) {
        if (sensor == Sensor.S2_MSI) {
            return S2Utils.targetS2BandNameMatches(targetBandName, pattern);
        } else {
            return targetBandName.matches(pattern);
        }
    }

    private void addAuxiliaryData(Product sourceProduct, RayleighAux rayleighAux, Rectangle rectangle,
                                  int sourceBandIndex, String targetBandName) {
        String sourceBandName = getSourceBandName(sourceBandIndex, targetBandName);
        Band band = sourceProduct.getBand(sourceBandName);

        rayleighAux.setWavelength(band.getSpectralWavelength());
        if (sensor.equals(Sensor.OLCI)) {
            rayleighAux.setSolarFluxs(getSourceTile(sourceProduct.getBand(String.format(SOLAR_FLUX_BAND_PATTERN, sourceBandIndex)), rectangle));
            rayleighAux.setSourceSampleRad(getSourceTile(sourceProduct.getBand(sourceBandName), rectangle));
        } else if (sensor.equals(Sensor.MERIS)) {
            Band sourceBand = sourceProduct.getBand(sourceBandName);
            rayleighAux.setSourceSampleRad(getSourceTile(sourceBand, rectangle));
            int length = rectangle.width * rectangle.height;

            double[] solarFlux = fillDefaultArray(length, sourceBand.getSolarFlux());

            rayleighAux.setSolarFluxs(solarFlux);
        } else if (sensor.equals(Sensor.MERIS_4TH)) {
            Band sourceBand = sourceProduct.getBand(sourceBandName);
            rayleighAux.setSourceSampleRad(getSourceTile(sourceBand, rectangle));
            int length = rectangle.width * rectangle.height;

            double[] solarFlux = fillDefaultArray(length, EnvisatConstants.MERIS_SOLAR_FLUXES[sourceBand.getSpectralBandIndex()]);

            rayleighAux.setSolarFluxs(solarFlux);
        } else if (sensor.equals(Sensor.S2_MSI)) {
            Band sourceBand = sourceProduct.getBand(sourceBandName);
            rayleighAux.setSourceSampleRad(getSourceTile(sourceBand, rectangle));
            int length = rectangle.width * rectangle.height;

            double[] solarFlux = fillDefaultArray(length, SensorConstants.S2_SOLAR_FLUXES[sourceBand.getSpectralBandIndex()]);

            rayleighAux.setSolarFluxs(solarFlux);
        }
    }

    private String getSourceBandName(int sourceBandIndex, String targetBandName) {
        String sourceBandName = String.format(sensor.getNameFormat(), sourceBandIndex);
        if (sensor == Sensor.S2_MSI && targetBandName.endsWith("8A")) {
            return sourceBandName.concat("A");
        }
        return sourceBandName;
    }

    private double[] fillDefaultArray(int length, double value) {
        double[] createArray = new double[length];
        Arrays.fill(createArray, value);
        return createArray;
    }

    private RayleighAux createAuxiliary(Product sourceProduct, Sensor sensor, Rectangle rectangle) {
        RayleighAux rayleighAux = new RayleighAux();
        if (sensor.equals(Sensor.MERIS)) {
            rayleighAux.setSunAzimuthAngles(getSourceTile(sourceProduct.getRasterDataNode(MERIS_SAA_NAME), rectangle));
            rayleighAux.setSunZenithAngles(getSourceTile(sourceProduct.getRasterDataNode(MERIS_SZA_NAME), rectangle));
            rayleighAux.setViewZenithAngles(getSourceTile(sourceProduct.getRasterDataNode(MERIS_VZA_NAME), rectangle));
            rayleighAux.setViewAzimuthAngles(getSourceTile(sourceProduct.getRasterDataNode(MERIS_VAA_NAME), rectangle));
            rayleighAux.setSeaLevels(getSourceTile(sourceProduct.getRasterDataNode(MERIS_SLP_NAME), rectangle));
            rayleighAux.setTotalOzones(getSourceTile(sourceProduct.getRasterDataNode(MERIS_OZONE_NAME), rectangle));
            rayleighAux.setLatitudes(getSourceTile(sourceProduct.getRasterDataNode(MERIS_LAT_NAME), rectangle));
            rayleighAux.setLongitude(getSourceTile(sourceProduct.getRasterDataNode(MERIS_LON_NAME), rectangle));
            rayleighAux.setAltitudes(getSourceTile(sourceProduct.getRasterDataNode(ALTITUDE_DEM), rectangle));
        } else if (sensor.equals(Sensor.MERIS_4TH)) {
            rayleighAux.setSunAzimuthAngles(getSourceTile(sourceProduct.getRasterDataNode(MERIS_4TH_SAA_NAME), rectangle));
            rayleighAux.setSunZenithAngles(getSourceTile(sourceProduct.getRasterDataNode(MERIS_4TH_SZA_NAME), rectangle));
            rayleighAux.setViewZenithAngles(getSourceTile(sourceProduct.getRasterDataNode(MERIS_4TH_VZA_NAME), rectangle));
            rayleighAux.setViewAzimuthAngles(getSourceTile(sourceProduct.getRasterDataNode(MERIS_4TH_VAA_NAME), rectangle));
            rayleighAux.setSeaLevels(getSourceTile(sourceProduct.getRasterDataNode(MERIS_4TH_SLP_NAME), rectangle));
            rayleighAux.setTotalOzones(getSourceTile(sourceProduct.getRasterDataNode(MERIS_4TH_OZONE_NAME), rectangle));
            rayleighAux.setLatitudes(getSourceTile(sourceProduct.getRasterDataNode(TP_LATITUDE), rectangle));
            rayleighAux.setLongitude(getSourceTile(sourceProduct.getRasterDataNode(TP_LONGITUDE), rectangle));
            rayleighAux.setAltitudes(getSourceTile(sourceProduct.getRasterDataNode(TP_ALTITUDE), rectangle));
        } else if (sensor.equals(Sensor.OLCI)) {
            rayleighAux.setSunZenithAngles(getSourceTile(sourceProduct.getRasterDataNode(OLCI_SZA_NAME), rectangle));
            rayleighAux.setViewZenithAngles(getSourceTile(sourceProduct.getRasterDataNode(OLCI_VZA_NAME), rectangle));
            rayleighAux.setSunAzimuthAngles(getSourceTile(sourceProduct.getRasterDataNode(OLCI_SAA_NAME), rectangle));
            rayleighAux.setViewAzimuthAngles(getSourceTile(sourceProduct.getRasterDataNode(OLCI_VAA_NAME), rectangle));
            rayleighAux.setSeaLevels(getSourceTile(sourceProduct.getRasterDataNode(OLCI_SLP_NAME), rectangle));
            rayleighAux.setOlciTotalOzones(getSourceTile(sourceProduct.getTiePointGrid(OLCI_OZONE_NAME), rectangle));
            if (sourceProduct.getTiePointGrid(TP_LATITUDE) != null) {
                rayleighAux.setLatitudes(getSourceTile(sourceProduct.getRasterDataNode(TP_LATITUDE), rectangle));
            } else {
                rayleighAux.setLatitudes(getSourceTile(sourceProduct.getRasterDataNode(LATITUDE), rectangle));
            }
            if (sourceProduct.getTiePointGrid(TP_LONGITUDE) != null) {
                rayleighAux.setLongitude(getSourceTile(sourceProduct.getRasterDataNode(TP_LONGITUDE), rectangle));
            } else {
                rayleighAux.setLongitude(getSourceTile(sourceProduct.getRasterDataNode(LONGITUDE), rectangle));
            }
        } else if (sensor.equals(Sensor.S2_MSI)) {
            final Tile szaTile = getSourceTile(sourceProduct.getRasterDataNode(S2_MSI_SZA_NAME), rectangle);
            rayleighAux.setSunZenithAngles(szaTile);
            final Tile vzaTile = getSourceTile(sourceProduct.getRasterDataNode(S2_MSI_VZA_NAME), rectangle);
            rayleighAux.setViewZenithAngles(vzaTile);
            final Tile saaTile = getSourceTile(sourceProduct.getRasterDataNode(S2_MSI_SAA_NAME), rectangle);
            rayleighAux.setSunAzimuthAngles(saaTile);
            final Tile vaaTile = getSourceTile(sourceProduct.getRasterDataNode(S2_MSI_VAA_NAME), rectangle);
            rayleighAux.setViewAzimuthAngles(vaaTile);
            rayleighAux.setS2MsiSeaLevelsPressures(s2MsiSeaLevelPressure, rectangle);
            rayleighAux.setS2MsiTotalOzones(s2MsiOzone, rectangle);

            rayleighAux.setS2MsiAngles(productToProcess.getSceneGeoCoding(), szaTile, vzaTile, saaTile, vaaTile, rectangle);
        }

        return rayleighAux;
    }


    public static class Spi extends OperatorSpi {
        public Spi() {
            super(RayleighCorrectionOp.class);
        }
    }

    private double[] getCrossSectionSigma(Product sourceProduct, int numBands, String getBandNamePattern) {
        return algorithm.getCrossSectionSigma(sourceProduct, numBands, getBandNamePattern);
    }
}
