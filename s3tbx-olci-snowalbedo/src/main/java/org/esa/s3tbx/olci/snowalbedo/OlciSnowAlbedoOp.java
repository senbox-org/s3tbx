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
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.gpf.*;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.util.ProductUtils;

import java.awt.*;

/**
 * Computes planar broadband snow albedo from OLCI L1b data products.
 *
 * @author olafd
 */
@OperatorMetadata(alias = "OLCI.SnowAlbedo",
        description = "Computes planar broadband snow albedo from OLCI L1b data products.",
        authors = "Alexander Kokhanovsky (EUMETSAT),  Olaf Danne (Brockmann Consult)",
        copyright = "(c) 2017 by EUMETSAT, Brockmann Consult",
        category = "Optical/Pre-Processing",
        version = "1.2")

public class OlciSnowAlbedoOp extends Operator {

    @Parameter(defaultValue = "false",
            description = "If set, spectral bands from TOA reflectance product are written to target product")
    private boolean copyReflectanceBands;

    @SourceProduct(description = "L1b or Rayleigh corrected product", label = "OLCI L1b or Rayleigh corrected product")
    public Product sourceProduct;

    private Product targetProduct;

    private Band landWaterBand;

    private Product waterMaskProduct;
    private Product reflProduct;


    @Override
    public void initialize() throws OperatorException {
        checkSensorType(sourceProduct);

        createTargetProduct();

        waterMaskProduct = GPF.createProduct("LandWaterMask", GPF.NO_PARAMS, sourceProduct);
        landWaterBand = waterMaskProduct.getBand("land_water_fraction");

        if (isValidL1bSourceProduct(sourceProduct, Sensor.OLCI)) {
            RayleighCorrectionOp rayleighCorrectionOp = new RayleighCorrectionOp();
            rayleighCorrectionOp.setSourceProduct(sourceProduct);
            rayleighCorrectionOp.setParameterDefaultValues();
            rayleighCorrectionOp.setParameter("computeTaur", false);
            rayleighCorrectionOp.setParameter("sourceBandNames", Sensor.OLCI.getRequiredRadianceBandNames());
            reflProduct = rayleighCorrectionOp.getTargetProduct();
        } else if (isValidBrrSourceProduct(sourceProduct, Sensor.OLCI)) {
            reflProduct = sourceProduct;
        } else {
            throw new OperatorException
                    ("Input product not supported - must be " + Sensor.OLCI.getName() +
                             " L1b or Rayleigh corrected BRR product");
        }

    }

    private void createTargetProduct() {
        targetProduct = new Product(sourceProduct.getName(), sourceProduct.getProductType(),
                                    sourceProduct.getSceneRasterWidth(), sourceProduct.getSceneRasterHeight());

        if (copyReflectanceBands) {
            for (Band band : reflProduct.getBands()) {
                if (band.getName().matches(Sensor.OLCI.getNamePattern())) {
                    final Band targetBand = targetProduct.addBand(band.getName(), band.getDataType());
                    ProductUtils.copyRasterDataNodeProperties(band, targetBand);
                }
            }
        }

        final Band planarAlbedoBand = targetProduct.addBand("planar_albedo", ProductData.TYPE_FLOAT32);
        planarAlbedoBand.setUnit("dl");
        planarAlbedoBand.setNoDataValue(Float.NaN);
        planarAlbedoBand.setNoDataValueUsed(true);

        ProductUtils.copyMetadata(sourceProduct, targetProduct);
        ProductUtils.copyMasks(sourceProduct, targetProduct);
        ProductUtils.copyFlagBands(sourceProduct, targetProduct, true);
        ProductUtils.copyGeoCoding(sourceProduct, targetProduct);
        targetProduct.setAutoGrouping(sourceProduct.getAutoGrouping());
        setTargetProduct(targetProduct);
    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        Rectangle targetRectangle = targetTile.getRectangle();
        try {
            Tile[] rhoToaTiles = new Tile[Sensor.OLCI.getRequiredBrrBandNames().length];
//            Tile[] solarFluxTiles = new Tile[Rad2ReflConstants.OLCI_REFL_BAND_NAMES.length];
            for (int i = 0; i < Sensor.OLCI.getRequiredBrrBandNames().length; i++) {
                final Band rhoToaBand = reflProduct.getBand(Sensor.OLCI.getRequiredBrrBandNames()[i]);
                rhoToaTiles[i] = getSourceTile(rhoToaBand, targetRectangle);
//                final Band solarFluxBand =
//                        reflProduct.getBand(Rad2ReflConstants.OLCI_SOLAR_FLUX_BAND_NAMES[i]);
//                solarFluxTiles[i] = getSourceTile(solarFluxBand, targetRectangle);
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
                        if (!isLandPixel(x, y, l1FlagsTile, waterFraction)) {
                            targetTile.setSample(x, y, Float.NaN);
                        } else {

                            // implementation following AK

//                            double[] rhoToa = new double[Rad2ReflConstants.OLCI_NUM_SPECTRAL_BANDS];
//                            float[] solarFlux = new float[Rad2ReflConstants.OLCI_NUM_SPECTRAL_BANDS];
//                            for (int i = 0; i < Rad2ReflConstants.OLCI_NUM_SPECTRAL_BANDS; i++) {
//                                rhoToa[i] = rhoToaTiles[i].getSampleDouble(x, y);
//                                solarFlux[i] = solarFluxTiles[i].getSampleFloat(x, y);
//                            }

                            double[] rhoToa = new double[Sensor.OLCI.getRequiredBrrBandNames().length];
                            for (int i = 0; i < Sensor.OLCI.getRequiredBrrBandNames().length; i++) {
                                rhoToa[i] = rhoToaTiles[i].getSampleDouble(x, y);
                            }

                            final double rhoToa21 = rhoToa[Sensor.OLCI.getRequiredBrrBandNames().length-1];
                            final double sza = szaTile.getSampleDouble(x, y);
                            final double vza = vzaTile.getSampleDouble(x, y);
                            final double saa = saaTile.getSampleDouble(x, y);
                            final double vaa = vaaTile.getSampleDouble(x, y);

                            final double[] spectralAlbedos =
                                    OlciSnowAlbedoAlgorithm.computeSpectralAlbedos(rhoToa, sza, vza, saa, vaa);
                            final double sphericalBroadbandAlbedo =
                                    OlciSnowAlbedoAlgorithm.computeSphericalBroadbandAlbedo(spectralAlbedos, rhoToa21);
                            final double planarBroadbandAlbedo =
                                    OlciSnowAlbedoAlgorithm.computePlanarBroadbandAlbedo(sphericalBroadbandAlbedo, sza);

                            targetTile.setSample(x, y, planarBroadbandAlbedo);
                        }
                    } else {
                        targetTile.setSample(x, y, Float.NaN);
                    }
                }
            }
        } catch (Exception e) {
            throw new OperatorException(e);
        }

    }

    static void checkSensorType(Product sourceProduct) {
        boolean isOlci = isValidL1bSourceProduct(sourceProduct, Sensor.OLCI) ||
                isValidBrrSourceProduct(sourceProduct, Sensor.OLCI);
        if (!isOlci) {
            throw new OperatorException("Source product not applicable to this operator.\n" +
                                                "Only OLCI is currently supported");
        }
    }

    static boolean isValidL1bSourceProduct(Product sourceProduct, Sensor sensor) {
        for (String bandName : sensor.getRequiredRadianceBandNames()) {
            if (!sourceProduct.containsBand(bandName)) {
                return false;
            }
        }
        return true;
    }

    static boolean isValidBrrSourceProduct(Product sourceProduct, Sensor sensor) {
        final String[] requiredBrrBands = sensor.getRequiredBrrBandNames();
        for (String bandName : requiredBrrBands) {
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
                return l1FlagsTile.getSampleBit(x, y, Sensor.OLCI.getInvalidBit());
            }
        } else {
            return l1FlagsTile.getSampleBit(x, y, Sensor.OLCI.getInvalidBit());
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
