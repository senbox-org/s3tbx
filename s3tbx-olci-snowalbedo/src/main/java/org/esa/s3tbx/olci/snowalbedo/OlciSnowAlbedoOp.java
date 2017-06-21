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

package org.esa.s3tbx.olci.snowalbedo;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s3tbx.olci.radiometry.rayleigh.RayleighCorrectionOp;
import org.esa.s3tbx.processor.rad2refl.Rad2ReflOp;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.gpf.*;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.util.ProductUtils;

import java.awt.*;
import java.util.Map;

/**
 * Computes snow albedo quantities from OLCI L1b data products.
 *
 * @author olafd
 */
@OperatorMetadata(alias = "OLCI.SnowAlbedo",
        description = "Computes snow albedo quantities from OLCI L1b data products.",
        authors = "Alexander Kokhanovsky (EUMETSAT),  Olaf Danne (Brockmann Consult)",
        copyright = "(c) 2017 by EUMETSAT, Brockmann Consult",
        category = "Optical/Thematic Land Processing",
        version = "1.0")

public class OlciSnowAlbedoOp extends Operator {

    public static final String SPECTRAL_SPHERICAL_ALBEDO_OUTPUT_PREFIX = "spectral_albedo_spherical_";
    public static final String SPECTRAL_PLANAR_ALBEDO_OUTPUT_PREFIX = "spectral_albedo_planar_";
    public static final String PLANAR_BBA_BAND_NAME = "planar_BBA";
    public static final String SPHERICAL_BBA_BAND_NAME = "spherical_BBA";
    public static final String GRAIN_DIAMETER_BAND_NAME = "grain_diameter";

    @Parameter(defaultValue = "false",
            description = "If selected, Rayleigh corrected reflectances are written to target product")
    private boolean copyReflectanceBands;

    @Parameter(defaultValue = "false",
            description = "If selected, albedo computation is done for land pixels only")
    private boolean computeLandPixelsOnly;

    @SourceProduct(description = "OLCI L1b product", label = "OLCI L1b product")
    public Product sourceProduct;

    private Product targetProduct;

    private Band landWaterBand;

    private Product waterMaskProduct;
    private Product reflProduct;


    @Override
    public void initialize() throws OperatorException {
        checkSensorType(sourceProduct);

        waterMaskProduct = GPF.createProduct("LandWaterMask", GPF.NO_PARAMS, sourceProduct);
        landWaterBand = waterMaskProduct.getBand("land_water_fraction");

        if (isValidL1bSourceProduct(sourceProduct, Sensor.OLCI)) {
//            RayleighCorrectionOp rayleighCorrectionOp = new RayleighCorrectionOp();
//            rayleighCorrectionOp.setSourceProduct(sourceProduct);
//            rayleighCorrectionOp.setParameterDefaultValues();
//            rayleighCorrectionOp.setParameter("computeTaur", false);
//            rayleighCorrectionOp.setParameter("sourceBandNames", Sensor.OLCI.getRequiredRadianceBandNames());
//            reflProduct = rayleighCorrectionOp.getTargetProduct();

            Rad2ReflOp rad2ReflOp = new Rad2ReflOp();
            rad2ReflOp.setSourceProduct(sourceProduct);
            rad2ReflOp.setParameterDefaultValues();
            rad2ReflOp.setParameter("sensor", org.esa.s3tbx.processor.rad2refl.Sensor.OLCI);
            rad2ReflOp.setParameter("copyNonSpectralBands", false);
            reflProduct = rad2ReflOp.getTargetProduct();
        } else {
            throw new OperatorException
                    ("Input product not supported - must be " + Sensor.OLCI.getName() +
                             " L1b or Rayleigh corrected BRR product");
        }

        createTargetProduct();
    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {
        try {
            Tile[] rhoToaTiles = new Tile[Sensor.OLCI.getRequiredBrrBandNames().length];
            for (int i = 0; i < Sensor.OLCI.getRequiredBrrBandNames().length; i++) {
                final Band rhoToaBand = reflProduct.getBand(Sensor.OLCI.getRequiredBrrBandNames()[i]);
                rhoToaTiles[i] = getSourceTile(rhoToaBand, targetRectangle);
            }

            Tile szaTile = getSourceTile(sourceProduct.getRasterDataNode(Sensor.OLCI.getSzaName()), targetRectangle);
            Tile vzaTile = getSourceTile(sourceProduct.getRasterDataNode(Sensor.OLCI.getVzaName()), targetRectangle);
            Tile saaTile = getSourceTile(sourceProduct.getRasterDataNode(Sensor.OLCI.getSaaName()), targetRectangle);
            Tile vaaTile = getSourceTile(sourceProduct.getRasterDataNode(Sensor.OLCI.getVaaName()), targetRectangle);
            Tile l1FlagsTile = getSourceTile(sourceProduct.getRasterDataNode(Sensor.OLCI.getL1bFlagsName()), targetRectangle);
            Tile waterFractionTile = getSourceTile(landWaterBand, targetRectangle);

            for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
                checkForCancellation();
                for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {
                    if (!l1FlagsTile.getSampleBit(x, y, Sensor.OLCI.getInvalidBit())) {
                        final int waterFraction = waterFractionTile.getSampleInt(x, y);

                        // we compute snow albedo over land only
                        if (computeLandPixelsOnly && !isLandPixel(x, y, l1FlagsTile, waterFraction)) {
                            setTargetTilesInvalid(targetTiles, x, y);
                        } else {
                            double[] rhoToa = new double[Sensor.OLCI.getRequiredBrrBandNames().length];
                            for (int i = 0; i < Sensor.OLCI.getRequiredBrrBandNames().length; i++) {
                                rhoToa[i] = rhoToaTiles[i].getSampleDouble(x, y);
                            }

                            final double rhoToa21 = rhoToa[Sensor.OLCI.getRequiredBrrBandNames().length - 1];
                            final double sza = szaTile.getSampleDouble(x, y);
                            final double vza = vzaTile.getSampleDouble(x, y);
                            final double saa = saaTile.getSampleDouble(x, y);
                            final double vaa = vaaTile.getSampleDouble(x, y);

                            final double[] spectralSphericalAlbedos =
                                    OlciSnowAlbedoAlgorithm.computeSpectralSphericalAlbedos(rhoToa, sza, vza, saa, vaa);
                            setTargetTilesSpectralSphericalAlbedos(spectralSphericalAlbedos, targetTiles, x, y);

                            final double[] spectralPlanarAlbedos =
                                    OlciSnowAlbedoAlgorithm.computePlanarFromSphericalAlbedos(spectralSphericalAlbedos, sza);
                            setTargetTilesSpectralPlanarAlbedos(spectralPlanarAlbedos, targetTiles, x, y);

                            final OlciSnowAlbedoAlgorithm.SphericalBroadbandAlbedo sbbaTerms =
                                    OlciSnowAlbedoAlgorithm.computeSphericalBroadbandAlbedoTerms(spectralSphericalAlbedos, rhoToa21);

                            final double sbba = sbbaTerms.getR_b1() + sbbaTerms.getR_b2();
                            final double planarBroadbandAlbedo =
                                    OlciSnowAlbedoAlgorithm.computePlanarFromSphericalAlbedo(sbba, sza);
                            final Band sphericalBBABand = targetProduct.getBand(SPHERICAL_BBA_BAND_NAME);
                            final Band planarBBABand = targetProduct.getBand(PLANAR_BBA_BAND_NAME);
                            final Band grainDiameterBand = targetProduct.getBand(GRAIN_DIAMETER_BAND_NAME);
                            targetTiles.get(sphericalBBABand).setSample(x, y, sbba);
                            targetTiles.get(planarBBABand).setSample(x, y, planarBroadbandAlbedo);
                            final double grainDiameterInMillimeter = sbbaTerms.getGrainDiameter() * 0.001;
                            targetTiles.get(grainDiameterBand).setSample(x, y, grainDiameterInMillimeter);
                        }
                    } else {
                        setTargetTilesInvalid(targetTiles, x, y);
                    }
                }
            }
        } catch (Exception e) {
            throw new OperatorException(e);
        }
    }

    private void createTargetProduct() {
        targetProduct = new Product(sourceProduct.getName(), sourceProduct.getProductType(),
                                    sourceProduct.getSceneRasterWidth(), sourceProduct.getSceneRasterHeight());

        if (copyReflectanceBands) {
            for (Band band : reflProduct.getBands()) {
//                if (band.getName().startsWith(SensorConstants.OLCI_BRR_BAND_PREFIX)) {
                if (band.getName().endsWith(SensorConstants.OLCI_REFL_BAND_SUFFIX)) {
                    ProductUtils.copyBand(band.getName(), reflProduct, targetProduct, true);
                    ProductUtils.copyRasterDataNodeProperties(band, targetProduct.getBand(band.getName()));
                }
            }
        }

        targetProduct.addBand(PLANAR_BBA_BAND_NAME, ProductData.TYPE_FLOAT32);
        targetProduct.addBand(SPHERICAL_BBA_BAND_NAME, ProductData.TYPE_FLOAT32);
        targetProduct.addBand(GRAIN_DIAMETER_BAND_NAME, ProductData.TYPE_FLOAT32);

        for (int i = 0; i < OlciSnowAlbedoConstants.SPECTRAL_ALBEDO_OUTPUT_WAVELENGTHS.length; i++) {
            int wvl = OlciSnowAlbedoConstants.SPECTRAL_ALBEDO_OUTPUT_WAVELENGTHS[i];
            targetProduct.addBand(SPECTRAL_SPHERICAL_ALBEDO_OUTPUT_PREFIX + wvl, ProductData.TYPE_FLOAT32);
        }
        for (int i = 0; i < OlciSnowAlbedoConstants.SPECTRAL_ALBEDO_OUTPUT_WAVELENGTHS.length; i++) {
            int wvl = OlciSnowAlbedoConstants.SPECTRAL_ALBEDO_OUTPUT_WAVELENGTHS[i];
            targetProduct.addBand(SPECTRAL_PLANAR_ALBEDO_OUTPUT_PREFIX + wvl, ProductData.TYPE_FLOAT32);
        }

        for (Band band : targetProduct.getBands()) {
            band.setUnit("dl");
            band.setNoDataValue(Float.NaN);
            band.setNoDataValueUsed(true);
        }
        targetProduct.getBand(GRAIN_DIAMETER_BAND_NAME).setUnit("mm");

        ProductUtils.copyMetadata(sourceProduct, targetProduct);
        ProductUtils.copyTiePointGrids(sourceProduct, targetProduct);
        ProductUtils.copyMasks(sourceProduct, targetProduct);
        ProductUtils.copyFlagBands(sourceProduct, targetProduct, true);
        ProductUtils.copyGeoCoding(sourceProduct, targetProduct);
        targetProduct.setAutoGrouping("rBRR:spectral_albedo_spherical:spectral_albedo_planar");

        setTargetProduct(targetProduct);
    }

    private void setTargetTilesSpectralSphericalAlbedos(double[] spectralSphericalAlbedos, Map<Band, Tile> targetTiles, int x, int y) {
        for (int i = 0; i < OlciSnowAlbedoConstants.SPECTRAL_ALBEDO_OUTPUT_WAVELENGTHS.length; i++) {
            int wvl = OlciSnowAlbedoConstants.SPECTRAL_ALBEDO_OUTPUT_WAVELENGTHS[i];
            final Band spectralSphericalAlbedoBand = targetProduct.getBand(SPECTRAL_SPHERICAL_ALBEDO_OUTPUT_PREFIX + wvl);
            targetTiles.get(spectralSphericalAlbedoBand).setSample(x, y, spectralSphericalAlbedos[i]);
        }
    }

    private void setTargetTilesSpectralPlanarAlbedos(double[] spectralPlanarAlbedos, Map<Band, Tile> targetTiles, int x, int y) {
        for (int i = 0; i < OlciSnowAlbedoConstants.SPECTRAL_ALBEDO_OUTPUT_WAVELENGTHS.length; i++) {
            int wvl = OlciSnowAlbedoConstants.SPECTRAL_ALBEDO_OUTPUT_WAVELENGTHS[i];
            final Band spectralPlanarAlbedoBand = targetProduct.getBand(SPECTRAL_PLANAR_ALBEDO_OUTPUT_PREFIX + wvl);
            targetTiles.get(spectralPlanarAlbedoBand).setSample(x, y, spectralPlanarAlbedos[i]);
        }
    }

    private void setTargetTilesInvalid(Map<Band, Tile> targetTiles, int x, int y) {
        for (Tile tile : targetTiles.values()) {
            tile.setSample(x, y, Float.NaN);
        }
    }

    private static void checkSensorType(Product sourceProduct) {
        boolean isOlci = isValidL1bSourceProduct(sourceProduct, Sensor.OLCI);
        if (!isOlci) {
            throw new OperatorException("Source product not applicable to this operator.\n" +
                                                "Only OLCI is currently supported");
        }
    }

    private static boolean isValidL1bSourceProduct(Product sourceProduct, Sensor sensor) {
        for (String bandName : sensor.getRequiredRadianceBandNames()) {
            if (!sourceProduct.containsBand(bandName)) {
                return false;
            }
        }
        return true;
    }

    private boolean isLandPixel(int x, int y, Tile l1FlagsTile, int waterFraction) {
        // the water mask ends at 59 Degree south, stop earlier to avoid artefacts
        if (getGeoPos(x, y).lat > -58f) {
            // values bigger than 100 indicate no data
            if (waterFraction <= 100) {
                // todo: this does not work if we have a PixelGeocoding. In that case, waterFraction
                // is always 0 or 100!! (TS, OD, 20140502)
                return waterFraction == 0;
            } else {
                return l1FlagsTile.getSampleBit(x, y, Sensor.OLCI.getLandBit());
            }
        } else {
            return l1FlagsTile.getSampleBit(x, y, Sensor.OLCI.getLandBit());
        }
    }

    private GeoPos getGeoPos(int x, int y) {
        final GeoPos geoPos = new GeoPos();
        final GeoCoding geoCoding = getSourceProduct().getSceneGeoCoding();
        final PixelPos pixelPos = new PixelPos(x, y);
        geoCoding.getGeoPos(pixelPos, geoPos);
        return geoPos;
    }

    /**
     * The Service Provider Interface (SPI) for the operator.
     * It provides operator meta-data and is a factory for new operator instances.
     */
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(OlciSnowAlbedoOp.class);
        }
    }
}
