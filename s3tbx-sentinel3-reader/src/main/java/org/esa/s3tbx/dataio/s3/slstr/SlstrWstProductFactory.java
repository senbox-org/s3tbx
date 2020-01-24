package org.esa.s3tbx.dataio.s3.slstr;

/* Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import org.esa.s3tbx.dataio.s3.Sentinel3ProductReader;
import org.esa.snap.core.dataio.geocoding.*;
import org.esa.snap.core.dataio.geocoding.util.RasterUtils;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.BasicPixelGeoCoding;
import org.esa.snap.core.datamodel.GeoCodingFactory;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.RasterDataNode;

import java.io.IOException;

public class SlstrWstProductFactory extends SlstrSstProductFactory {

    private static final short[] RESOLUTIONS = new short[]{1000, 1000};
    private static final double RESOLUTION_IN_KM = 1.0;

    public SlstrWstProductFactory(Sentinel3ProductReader productReader) {
        super(productReader);
    }

    @Override
    protected void setGeoCoding(Product targetProduct) throws IOException {
        final Band lonBand = targetProduct.getBand("lon");
        final Band latBand = targetProduct.getBand("lat");
        if (lonBand == null || latBand == null) {
            // no way to create a geocoding tb 2020-01-24
            return;
        }

        final double[] longitudes = RasterUtils.loadDataScaled(lonBand);
        final double[] latitudes = RasterUtils.loadDataScaled(latBand);

        final GeoRaster geoRaster = new GeoRaster(longitudes, latitudes, lonBand.getRasterWidth(), lonBand.getRasterHeight(),
                targetProduct.getSceneRasterWidth(), targetProduct.getSceneRasterHeight(), RESOLUTION_IN_KM,
                0.5, 0.5,
                1.0, 1.0);

        // @todo 1 tb/tb parametrise this 2020-01-24
        final ForwardCoding forward = ComponentFactory.getForward("FWD_PIXEL");
        final InverseCoding inverse = ComponentFactory.getInverse("INV_PIXEL_QUAD_TREE");

        final ComponentGeoCoding geoCoding = new ComponentGeoCoding(geoRaster, forward, inverse, GeoChecks.ANTIMERIDIAN);
        geoCoding.initialize();

        targetProduct.setSceneGeoCoding(geoCoding);

    }

    @Override
    protected void setAutoGrouping(Product[] sourceProducts, Product targetProduct) {
        targetProduct.setAutoGrouping("brightness_temperature:nedt");
    }

    @Override
    protected void setUncertaintyBands(Product product) {
        super.setUncertaintyBands(product);
        // sst_theoretical_error is the old name
        String[] possibleUncBandNames = new String[]{"sst_theoretical_uncertainty", "sst_theoretical_error"};
        if(product.containsBand("sea_surface_temperature")) {
            final Band seaSurfaceTemperatureBand = product.getBand("sea_surface_temperature");
            for (String bandName : possibleUncBandNames) {
                if (product.containsBand(bandName)) {
                    final Band band = product.getBand(bandName);
                    seaSurfaceTemperatureBand.addAncillaryVariable(band, "uncertainty");
                    addUncertaintyImageInfo(band);
                    break;
                }
            }
        }
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
        return RESOLUTIONS;
    }

    @Override
    protected short[] getReferenceResolutions() {
        return RESOLUTIONS;
    }

    @Override
    protected void configureTargetNode(Band sourceBand, RasterDataNode targetNode) {
    }


}
