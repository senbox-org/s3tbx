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
import org.esa.snap.core.dataio.geocoding.ComponentFactory;
import org.esa.snap.core.dataio.geocoding.ComponentGeoCoding;
import org.esa.snap.core.dataio.geocoding.ForwardCoding;
import org.esa.snap.core.dataio.geocoding.GeoChecks;
import org.esa.snap.core.dataio.geocoding.GeoRaster;
import org.esa.snap.core.dataio.geocoding.InverseCoding;
import org.esa.snap.core.dataio.geocoding.util.RasterUtils;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.RasterDataNode;

import java.io.IOException;

public class SlstrWstProductFactory extends SlstrSstProductFactory {

    private static final short[] RESOLUTIONS = new short[]{1000, 1000};
    private static final double RESOLUTION_IN_KM = 1.0;
    private final static String SYSPROP_SLSTR_WST_PIXEL_INVERSE = "s3tbx.reader.slstr.wst.pixelGeoCoding.inverse";

    public SlstrWstProductFactory(Sentinel3ProductReader productReader) {
        super(productReader);
    }

    @Override
    protected void setGeoCoding(Product targetProduct) throws IOException {
        final String lonVariableName = "lon";
        final String latVariableName = "lat";
        final Band lonBand = targetProduct.getBand(lonVariableName);
        final Band latBand = targetProduct.getBand(latVariableName);
        if (lonBand == null || latBand == null) {
            // no way to create a geocoding tb 2020-01-24
            return;
        }

        final double[] longitudes = RasterUtils.loadGeoData(lonBand);
        final double[] latitudes = RasterUtils.loadGeoData(latBand);

        final GeoRaster geoRaster = new GeoRaster(longitudes, latitudes, lonVariableName, latVariableName,
                                                  targetProduct.getSceneRasterWidth(), targetProduct.getSceneRasterHeight(), RESOLUTION_IN_KM);

        final String[] keys = getForwardAndInverseKeys_pixelCoding(SYSPROP_SLSTR_WST_PIXEL_INVERSE);
        final ForwardCoding forward = ComponentFactory.getForward(keys[0]);
        final InverseCoding inverse = ComponentFactory.getInverse(keys[1]);

        final ComponentGeoCoding geoCoding = new ComponentGeoCoding(geoRaster, forward, inverse, GeoChecks.POLES);
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
        if (product.containsBand("sea_surface_temperature")) {
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

    @Override
    protected void setBandGeoCodings(Product product) {
        // this is intended - we do not have band geo-codings for this product type tb 2020-04-20
    }

    @Override
    protected void setTimeCoding(Product targetProduct) throws IOException {
        // empty by design - prevents the SlstrSstProductFactory implementation from being called tb 2021-01-19
    }
}
