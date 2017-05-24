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
import org.esa.s3tbx.processor.rad2refl.Rad2ReflConstants;
import org.esa.s3tbx.processor.rad2refl.Rad2ReflOp;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.gpf.*;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.util.ProductUtils;

import java.awt.*;

/**
 * @author olafd
 */
@OperatorMetadata(alias = "OLCI.SnowAlbedo",
        description = "Computes snow albedo measurements from OLCI L1b data products.",
        authors = "Alexander Kokhanovsky (EUMETSAT),  Olaf Danne (Brockmann Consult)",
        copyright = "(c) 2017 by EUMETSAT, Brockmann Consult",
        category = "Optical/Pre-Processing",
        version = "1.2")

public class OlciSnowAlbedoOp extends Operator {

    @Parameter(defaultValue = "false",
            description = "If set, spectral bands from TOA reflectance product are written to target product")
    private boolean copyReflectanceBands;

    @SourceProduct
    public Product sourceProduct;

    private Product targetProduct;

    private Band landWaterBand;

    private Product waterMaskProduct;
    private Product reflProduct;


    @Override
    public void initialize() throws OperatorException {
        createTargetProduct();

        waterMaskProduct = GPF.createProduct("LandWaterMask", GPF.NO_PARAMS, sourceProduct);
        landWaterBand = waterMaskProduct.getBand("land_water_fraction");

        Rad2ReflOp rad2ReflOp = new Rad2ReflOp();
        rad2ReflOp.setSourceProduct(sourceProduct);
        rad2ReflOp.setParameterDefaultValues();
        rad2ReflOp.setParameter("copyNonSpectralBands", true);
        reflProduct = rad2ReflOp.getTargetProduct();

    }

    private void createTargetProduct() {
        targetProduct = new Product(sourceProduct.getName(), sourceProduct.getProductType(),
                                    sourceProduct.getSceneRasterWidth(), sourceProduct.getSceneRasterHeight());

        if (copyReflectanceBands) {
            for (Band band : reflProduct.getBands()) {
                if (band.getName().matches(SensorConstants.OLCI_NAME_PATTERN)) {
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
            Tile[] rhoToaTiles = new Tile[Rad2ReflConstants.OLCI_REFL_BAND_NAMES.length];
            for (int i = 0; i < Rad2ReflConstants.OLCI_REFL_BAND_NAMES.length; i++) {
                final Band rhoToaBand =
                        reflProduct.getBand(String.format(SensorConstants.OLCI_NAME_FORMAT + "reflectance", i));
                rhoToaTiles[i] = getSourceTile(rhoToaBand, targetRectangle);
            }

            Tile l1FlagsTile = getSourceTile(sourceProduct.getBand(SensorConstants.OLCI_L1B_FLAGS_NAME),
                                             targetRectangle);
            Tile waterFractionTile = getSourceTile(landWaterBand, targetRectangle);

            for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
                checkForCancellation();
                for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {
                    if (!l1FlagsTile.getSampleBit(x, y, SensorConstants.OLCI_INVALID_BIT)) {
                        final int waterFraction = waterFractionTile.getSampleInt(x, y);

                        // we compute snow albedo over land only
                        if (!isLandPixel(x, y, l1FlagsTile, waterFraction)) {
                            targetTile.setSample(x, y, Float.NaN);
                        } else {
                            float planarSnowAlbedo = 0.0f;

                            // todo: implementation following AK algorithm here

                            // for the moment we assume that the TOA reflectances are what Alex calls
                            // the 'spectral spherical albedo r_s as measured by OLCI'
                            // todo clarify what we really need and how to convert



                            targetTile.setSample(x, y, planarSnowAlbedo);
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

    private boolean isLandPixel(int x, int y, Tile l1FlagsTile, int waterFraction) {
        // the water mask ends at 59 Degree south, stop earlier to avoid artefacts
        if (getGeoPos(x, y).lat > -58f) {
            // values bigger than 100 indicate no data
            if (waterFraction <= 100) {
                // todo: this does not work if we have a PixelGeocoding. In that case, waterFraction
                // is always 0 or 100!! (TS, OD, 20140502)
                return waterFraction == 0;
            } else {
                return l1FlagsTile.getSampleBit(x, y, SensorConstants.OLCI_INVALID_BIT);
            }
        } else {
            return l1FlagsTile.getSampleBit(x, y, SensorConstants.OLCI_INVALID_BIT);
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
