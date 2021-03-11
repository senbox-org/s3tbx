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
package org.esa.s3tbx.processor.rad2refl;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.image.ResolutionLevel;
import org.esa.snap.core.image.VirtualBandOpImage;
import org.esa.snap.core.util.ProductUtils;

import java.awt.Rectangle;
import java.awt.image.Raster;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.esa.snap.dataio.envisat.EnvisatConstants.MERIS_DETECTOR_INDEX_DS_NAME;

/**
 * An operator to provide conversion from radiances to reflectances or backwards.
 * Currently supports MERIS, OLCI and SLSTR 500m L1 products.
 *
 * @author Olaf Danne
 * @author Marco Peters
 */
@OperatorMetadata(alias = "Rad2Refl",
        authors = "Olaf Danne, Marco Peters",
        copyright = "Brockmann Consult GmbH",
        category = "Optical/Preprocessing",
        version = "2.0",
        description = "Provides conversion from radiances to reflectances or backwards.")
public class Rad2ReflOp extends Operator {

    @Parameter(defaultValue = "OLCI", description = "The sensor", valueSet = {"MERIS", "OLCI", "SLSTR_500m"})
    private Sensor sensor;

    @Parameter(description = "Conversion mode: from rad to refl, or backwards", valueSet = {"RAD_TO_REFL", "REFL_TO_RAD"},
            defaultValue = "RAD_TO_REFL")
    private String conversionMode;

    @SourceProduct(alias = "source", label = "Name", description = "The source product.")
    private Product sourceProduct;

    @Parameter(defaultValue = "false", description = "If set, all tie point grids from source product are written to target product")
    private boolean copyTiePointGrids;

    @Parameter(defaultValue = "false", description = "If set, all flag bands and masks from source product are written to target product")
    private boolean copyFlagBandsAndMasks;

    @Parameter(defaultValue = "false", description = "If set, all other non-spectral bands from source product are written to target product")
    private boolean copyNonSpectralBands;


    private RadReflConverter converter;

    private transient int currentPixel = 0;
    private String spectralInputBandPrefix;
    private Product targetProduct;
    private Rad2ReflAuxdata rad2ReflAuxdata;

    private String[] spectralInputBandNames;
    private String[] spectralOutputBandNames;

    private VirtualBandOpImage invalidImage;
    private VirtualBandOpImage[] slstrInvalidImages;

    private Map<String, Float> slstrSolarFluxMap;


    @Override
    public void initialize() throws OperatorException {
        spectralInputBandPrefix = isRadToReflMode() ? "radiance" : "reflectance";
        setInputAndOutputBands();
        boolean spectralBandsAvailable = productHasAllSpectralBands(spectralInputBandNames);
        if (sensor == Sensor.MERIS && spectralBandsAvailable) {
            setupInvalidImage(Sensor.MERIS.getInvalidPixelExpression());
            converter = new MerisRadReflConverter(conversionMode);
            try {
                rad2ReflAuxdata = Rad2ReflAuxdata.loadMERISAuxdata(sourceProduct.getProductType());
            } catch (IOException e) {
                throw new OperatorException(e);
            }
        } else if (sensor == Sensor.OLCI && spectralBandsAvailable) {
            setupInvalidImage(Sensor.OLCI.getInvalidPixelExpression());
            converter = new OlciRadReflConverter(conversionMode);
        } else if (sensor == Sensor.SLSTR_500m) {
            slstrSolarFluxMap = SlstrRadReflConverter.getSolarFluxMapFromQualityMetadata(sourceProduct,
                                                                                         spectralInputBandNames,
                                                                                         isRadToReflMode());
            slstrInvalidImages = SlstrRadReflConverter.
                    createInvalidImages(sourceProduct, spectralInputBandNames, isRadToReflMode());
            converter = new SlstrRadReflConverter(conversionMode);
        } else {
            throw new OperatorException
                    ("Sensor '" + sensor.getName() + "' not compliant with input product. Please check your selection.");
        }
        Product targetProduct = createTargetProduct();
        setTargetProduct(targetProduct);
    }

    private void setInputAndOutputBands() {
        if (sensor == Sensor.SLSTR_500m) {
            String[] allSpectralInputBandNames = isRadToReflMode() ? sensor.getRadBandNames() : sensor.getReflBandNames();
            String[] allSpectralOutputBandNames = isRadToReflMode() ? sensor.getReflBandNames() : sensor.getRadBandNames();
            List<String> spectralInputBandNameList = new ArrayList<>();
            List<String> spectralOutputBandNameList = new ArrayList<>();
            for (int i = 0; i < allSpectralInputBandNames.length; i++) {
                if (sourceProduct.containsBand(allSpectralInputBandNames[i])) {
                    spectralInputBandNameList.add(allSpectralInputBandNames[i]);
                    spectralOutputBandNameList.add(allSpectralOutputBandNames[i]);
                }
            }
            spectralInputBandNames = spectralInputBandNameList.toArray(new String[0]);
            spectralOutputBandNames = spectralOutputBandNameList.toArray(new String[0]);
        } else {
            spectralInputBandNames = isRadToReflMode() ? sensor.getRadBandNames() : sensor.getReflBandNames();
            spectralOutputBandNames = isRadToReflMode() ? sensor.getReflBandNames() : sensor.getRadBandNames();
        }
    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        checkCancellation();
        int bandIndex = -1;
        for (int i = 0; i < spectralOutputBandNames.length; i++) {
            if (spectralOutputBandNames[i].equals(targetBand.getName())) {
                bandIndex = i;
            }
        }

        if (bandIndex >= 0) {
            final Rectangle rectangle = targetTile.getRectangle();

            Raster isInvalid;
            if (sensor == Sensor.SLSTR_500m) {
                isInvalid = slstrInvalidImages[bandIndex].getData(rectangle);
            } else {
                isInvalid = invalidImage.getData(rectangle);
            }

            final Band spectralBandToConvert = sourceProduct.getBand(spectralInputBandNames[bandIndex]);
            final Tile[] szaTiles = getSzaSourceTiles(rectangle);
            final Tile spectralBandToConvertTile = getSourceTile(spectralBandToConvert, rectangle);

            Tile detectorIndexTile = null;
            if (sensor == Sensor.MERIS) {
                detectorIndexTile = getSourceTile(sourceProduct.getBand(MERIS_DETECTOR_INDEX_DS_NAME), rectangle);
            }
            Tile solarFluxTile = null;
            if (sensor == Sensor.OLCI) {
                solarFluxTile = getSourceTile(sourceProduct.getBand(sensor.getSolarFluxBandNames()[bandIndex]), rectangle);
            }

            for (int y = rectangle.y; y < rectangle.y + rectangle.height; y++) {
                checkForCancellation();
                for (int x = rectangle.x; x < rectangle.x + rectangle.width; x++) {
                    if (isInvalid.getSample(x, y, 0) != 0) {
                        targetTile.setSample(x, y, Rad2ReflConstants.RAD_TO_REFL_NODATA);
                    } else {
                        float solarFlux = Float.NaN;
                        float sza = Float.NaN;
                        if (solarFluxTile != null && sensor == Sensor.OLCI) {
                            solarFlux = solarFluxTile.getSampleFloat(x, y);
                            sza = szaTiles[0].getSampleFloat(x, y);
                        } else if (detectorIndexTile != null && sensor == Sensor.MERIS) {
                            final int detectorIndex = detectorIndexTile.getSampleInt(x, y);
                            if (detectorIndex >= 0) {
                                solarFlux = (float) rad2ReflAuxdata.getDetectorSunSpectralFluxes()[detectorIndex][bandIndex];
                            } else {
                                solarFlux = spectralBandToConvert.getSolarFlux();
                            }
                            sza = szaTiles[0].getSampleFloat(x, y);
                        } else if (sensor == Sensor.SLSTR_500m) {
                            solarFlux = slstrSolarFluxMap.get(spectralBandToConvert.getName());
                            if (spectralBandToConvert.getName().endsWith("o")) {
                                sza = szaTiles[1].getSampleFloat(x, y);
                            } else {
                                sza = szaTiles[0].getSampleFloat(x, y);
                            }
                        }

                        final float spectralValueToConvert = spectralBandToConvertTile.getSampleFloat(x, y);
                        final float spectralValueConverted = converter.convert(spectralValueToConvert, sza, solarFlux);
                        targetTile.setSample(x, y, Float.isNaN(spectralValueConverted) ?
                                Rad2ReflConstants.RAD_TO_REFL_NODATA : spectralValueConverted);
                    }
                }
            }
        }
    }

    private boolean productHasAllSpectralBands(String[] spectralInputBandNames) {
        // check if input product contains all expected spectral bands
        List<String> allBandsName = Arrays.asList(sourceProduct.getBandNames());
        boolean checker = false;
        for (String bandName : spectralInputBandNames) {
            checker = allBandsName.contains(bandName);
        }

        return checker;
    }

    private Tile[] getSzaSourceTiles(Rectangle rectangle) {
        try {
            if (sensor.equals(Sensor.MERIS) || sensor.equals(Sensor.OLCI)) {
                RasterDataNode tiePointGrid = sourceProduct.getRasterDataNode(sensor.getSzaBandNames()[0]);
                if (tiePointGrid == null) {
                    throw new OperatorException("No Sun Zenith Angle available from source product - cannot proceed.");
                }
                return new Tile[]{getSourceTile(tiePointGrid, rectangle)};
            } else {
                // SLSTR
                final RasterDataNode szaTpgN = sourceProduct.getRasterDataNode(sensor.getSzaBandNames()[0]);
                final RasterDataNode szaTpgO = sourceProduct.getRasterDataNode(sensor.getSzaBandNames()[1]);
                if (szaTpgN != null && szaTpgO != null) {
                    final Tile sourceTileN = getSourceTile(szaTpgN, rectangle);
                    final Tile sourceTileO = getSourceTile(szaTpgO, rectangle);
                    return new Tile[]{sourceTileN, sourceTileO};
                } else {
                    throw new OperatorException("No Sun Zenith Angle available from source product - cannot proceed.");
                }
            }
        } catch (OperatorException e) {
            throw new OperatorException("No Sun Zenith Angle available from source product - cannot proceed.");
        }
    }

    private void setupInvalidImage(String expression) {
        invalidImage = VirtualBandOpImage.builder(expression, sourceProduct)
                .dataType(ProductData.TYPE_FLOAT32)
                .fillValue(0.0f)
                .tileSize(sourceProduct.getPreferredTileSize())
                .mask(false)
                .level(ResolutionLevel.MAXRES)
                .create();
    }

    private Product createTargetProduct() {
        targetProduct = new Product(sourceProduct.getName(), sourceProduct.getProductType(),
                                    sourceProduct.getSceneRasterWidth(), sourceProduct.getSceneRasterHeight());

        for (int i = 0; i < spectralInputBandNames.length; i++) {
            Band sourceBand = sourceProduct.getBand(spectralInputBandNames[i]);
            if (isRadToReflMode()) {
                createReflectanceBand(sourceBand, spectralOutputBandNames[i]);
            } else {
                createRadianceBand(sourceBand, spectralOutputBandNames[i]);
            }
        }

        ProductUtils.copyMetadata(sourceProduct, targetProduct);
        ProductUtils.copyGeoCoding(sourceProduct, targetProduct);
        if (copyTiePointGrids) {
            ProductUtils.copyTiePointGrids(sourceProduct, targetProduct);
        }
        if (copyFlagBandsAndMasks) {
            ProductUtils.copyFlagBands(sourceProduct, targetProduct, true);
        }
        if (copyNonSpectralBands) {
            for (Band b : sourceProduct.getBands()) {
                if (!b.isFlagBand() &&
                        !b.getName().contains(spectralInputBandPrefix) && !targetProduct.containsBand(b.getName())) {
                    ProductUtils.copyBand(b.getName(), sourceProduct, targetProduct, true);
                }
            }
        }

        targetProduct.setStartTime(sourceProduct.getStartTime());
        targetProduct.setEndTime(sourceProduct.getEndTime());
        final String autogroupingExt = isRadToReflMode() ? sensor.getReflAutogroupingString() : sensor.getRadAutogroupingString();
        targetProduct.setAutoGrouping(sourceProduct.getAutoGrouping() + ":" + autogroupingExt);

        return targetProduct;
    }

    private void createRadianceBand(Band sourceBand, String targetBandName) {
        Band targetBand = new Band(targetBandName, ProductData.TYPE_FLOAT32,
                                   sourceBand.getRasterWidth(), sourceBand.getRasterHeight());

        targetProduct.addBand(targetBand);
        ProductUtils.copyRasterDataNodeProperties(sourceBand, targetBand);
        targetBand.setDescription(Rad2ReflConstants.REFL_TO_RAD_DESCR);
        targetBand.setUnit(Rad2ReflConstants.RAD_UNIT);
        targetBand.setScalingFactor(1.0);
        targetBand.setNoDataValue(Float.NaN);
        targetBand.setNoDataValueUsed(true);
        targetBand.setSpectralBandIndex(sourceBand.getSpectralBandIndex());
        targetBand.setSpectralWavelength(sourceBand.getSpectralWavelength());
        targetBand.setSpectralBandwidth(sourceBand.getSpectralBandwidth());
    }

    private void createReflectanceBand(Band sourceBand, String targetBandName) {
//        Band targetBand = new Band(targetBandName, ProductData.TYPE_FLOAT32,
//                                   sourceBand.getRasterWidth(), sourceBand.getRasterHeight());
//        targetBand.setScalingFactor(1);

        // We significantly save resources if we write reflectances as INT16 with suitable scaling factor.
        // We lose precision behind 5th decimal place, but that is ok for our purposes.
        // agreed by CB/MP/OD, 20170406
        Band targetBand = new Band(targetBandName, ProductData.TYPE_INT16,
                                   sourceBand.getRasterWidth(), sourceBand.getRasterHeight());
        targetProduct.addBand(targetBand);
        ProductUtils.copySpectralBandProperties(sourceBand, targetBand);
        targetBand.setDescription(Rad2ReflConstants.RAD_TO_REFL_DESCR);
        targetBand.setUnit(Rad2ReflConstants.REFL_UNIT);
        targetBand.setNoDataValueUsed(true);
        final double scalingFactor = 1. / 10000.0f;
        targetBand.setScalingFactor(scalingFactor);
        targetBand.setGeophysicalNoDataValue(Rad2ReflConstants.RAD_TO_REFL_NODATA);
        targetBand.setSpectralBandIndex(sourceBand.getSpectralBandIndex());
        targetBand.setSpectralWavelength(sourceBand.getSpectralWavelength());
        targetBand.setSpectralBandwidth(sourceBand.getSpectralBandwidth());
    }

    private boolean isRadToReflMode() {
        return conversionMode.equals("RAD_TO_REFL");
    }

    private void checkCancellation() {
        if (currentPixel % 1000 == 0) {
            checkForCancellation();
            currentPixel = 0;
        }
        currentPixel++;
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(Rad2ReflOp.class);
        }
    }
}
