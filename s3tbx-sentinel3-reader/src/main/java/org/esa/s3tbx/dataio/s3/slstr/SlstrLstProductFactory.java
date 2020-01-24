package org.esa.s3tbx.dataio.s3.slstr;/*
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

import org.esa.s3tbx.dataio.s3.Manifest;
import org.esa.s3tbx.dataio.s3.Sentinel3ProductReader;
import org.esa.snap.core.dataio.geocoding.*;
import org.esa.snap.core.dataio.geocoding.util.RasterUtils;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.runtime.Config;

import java.io.IOException;
import java.util.List;
import java.util.prefs.Preferences;

public class SlstrLstProductFactory extends SlstrProductFactory {

    private static final double RESOLUTION_IN_KM = 1.0;
    private final static String SYSPROP_SLSTR_LST_PIXEL_TIE_POINT_FORWARD = "s3tbx.reader.slstr.lst.tiePointGeoCoding.forward";

    public SlstrLstProductFactory(Sentinel3ProductReader productReader) {
        super(productReader);
    }

    @Override
    protected List<String> getFileNames(Manifest manifest) {
        return manifest.getFileNames("");
    }

    @Override
    protected Product findMasterProduct() {
        final List<Product> productList = getOpenProductList();
        Product masterProduct = new Product("dummy", "type", 1, 1);
        for (Product product : productList) {
            int masterSize = masterProduct.getSceneRasterWidth() * masterProduct.getSceneRasterHeight();
            int productSize = product.getSceneRasterWidth() * product.getSceneRasterHeight();
            if (productSize > masterSize &&
                    !product.getName().endsWith("tn") &&
                    !product.getName().endsWith("tx")) {
                masterProduct = product;
            }
        }
        return masterProduct;
    }

    @Override
    protected void setAutoGrouping(Product[] sourceProducts, Product targetProduct) {}

    @Override
    protected void setGeoCoding(Product targetProduct) throws IOException {
        final Band lonBand = targetProduct.getBand("longitude_in");
        final Band latBand = targetProduct.getBand("latitude_in");

        if (lonBand != null && latBand != null) {
            setPixelGeoCoding(targetProduct, lonBand, latBand);
        } else {
            final TiePointGrid lonGrid = targetProduct.getTiePointGrid("longitude_tx");
            final TiePointGrid latGrid = targetProduct.getTiePointGrid("latitude_tx");
            if ( lonGrid == null || latGrid == null){
                // no way to create a geo-coding tb 2020-01-22
                return;
            }

            setTiePointGeoCoding(targetProduct, lonGrid, latGrid);
        }
    }

    private void setPixelGeoCoding(Product targetProduct, Band lonBand, Band latBand) throws IOException {
        final double[] longitudes = RasterUtils.loadDataScaled(lonBand);
        final double[] latitudes = RasterUtils.loadDataScaled(latBand);

        final GeoRaster geoRaster = new GeoRaster(longitudes, latitudes,
                targetProduct.getSceneRasterWidth(), targetProduct.getSceneRasterHeight(),
                targetProduct.getSceneRasterWidth(), targetProduct.getSceneRasterHeight(), RESOLUTION_IN_KM,
                0.5, 0.5,
                1.0, 1.0);

        // @todo 1 tb/tb parametrise this 020-01-22
        final ForwardCoding forward = ComponentFactory.getForward("FWD_PIXEL");
        final InverseCoding inverse = ComponentFactory.getInverse("INV_PIXEL_QUAD_TREE");

        final ComponentGeoCoding geoCoding = new ComponentGeoCoding(geoRaster, forward, inverse, GeoChecks.ANTIMERIDIAN);
        geoCoding.initialize();

        targetProduct.setSceneGeoCoding(geoCoding);
    }

    private void setTiePointGeoCoding(Product targetProduct, TiePointGrid lonGrid, TiePointGrid latGrid) {
        final double[] longitudes = loadTiePointData(lonGrid);
        final double[] latitudes = loadTiePointData(latGrid);

        final GeoRaster geoRaster = new GeoRaster(longitudes, latitudes, lonGrid.getGridWidth(), lonGrid.getGridHeight(),
                targetProduct.getSceneRasterWidth(), targetProduct.getSceneRasterHeight(), RESOLUTION_IN_KM,
                lonGrid.getOffsetX(), lonGrid.getOffsetY(),
                lonGrid.getSubSamplingX(), lonGrid.getSubSamplingY());

        final Preferences preferences = Config.instance("s3tbx").preferences();
        final String fwdKey = preferences.get(SYSPROP_SLSTR_LST_PIXEL_TIE_POINT_FORWARD, "FWD_TIE_POINT_BILINEAR");

        final ForwardCoding forward = ComponentFactory.getForward(fwdKey);
        final InverseCoding inverse = ComponentFactory.getInverse("INV_TIE_POINT");

        final ComponentGeoCoding geoCoding = new ComponentGeoCoding(geoRaster, forward, inverse, GeoChecks.ANTIMERIDIAN);
        geoCoding.initialize();

        targetProduct.setSceneGeoCoding(geoCoding);
    }

    @Override
    protected Double getStartOffset(String gridIndex) {
        return 0.0;
    }

    @Override
    protected Double getTrackOffset(String gridIndex) {
        return 0.0;
    }

    @Override
    protected short[] getResolutions(String gridIndex) {
        short[] resolutions;
        if (gridIndex.equals("tx") || gridIndex.equals("tn")) {
            resolutions = new short[]{16000, 1000};
        } else {
            resolutions = new short[]{1000, 1000};
        }
        return resolutions;
    }

    @Override
    protected short[] getReferenceResolutions() {
        return new short[]{1000, 1000};
    }

    @Override
    protected RasterDataNode addSpecialNode(Product masterProduct, Band sourceBand, Product targetProduct) {
        //todo extract values from metadata file as soon as they are provided
        int subSamplingX = 16;
        int subSamplingY = 1;
        float offsetX = -26.0f;
        float offsetY = 0.0f;
        return copyBandAsTiePointGrid(sourceBand, targetProduct, subSamplingX, subSamplingY, offsetX, offsetY);
    }
}
