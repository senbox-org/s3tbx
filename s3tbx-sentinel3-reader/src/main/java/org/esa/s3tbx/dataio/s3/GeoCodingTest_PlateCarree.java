/*
 * Copyright (c) 2021.  Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.s3tbx.dataio.s3;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.dataio.dimap.DimapProductConstants;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.GeoUtils;
import org.esa.snap.runtime.Config;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import java.io.IOException;

import static org.esa.s3tbx.dataio.s3.olci.OlciProductFactory.OLCI_USE_PIXELGEOCODING;

public class GeoCodingTest_PlateCarree {

    public static void main(String[] args) throws IOException, FactoryException, TransformException {
        final String inputFilePath = args[0];
        final String outputFilePath = args[1];

        // read input product and ensure pixel geocoding
        // ---------------------------------------------
        Config.instance("s3tbx").load().preferences().putBoolean(OLCI_USE_PIXELGEOCODING, false);
        // D:\Satellite\reader_tests\sensors_platforms\SENTINEL-3\olci\L1\S3A_OL_1_ERR____20180608T084209_20180608T090412_20180609T114251_1322_032_107______MAR_O_NT_002.SEN3\xfdumanifest.xml
        final Product sourceProduct = ProductIO.readProduct(inputFilePath);

        Product targetProduct = null;
        try {
            // calculate target raster
            // -----------------------
            final ProjectionDescriptor descriptor = calculateRasterDimension(sourceProduct);

            // create target product
            // ---------------------
            targetProduct = new Product("projected", "test-type", descriptor.rasterWidth, descriptor.rasterHeight);
            final CrsGeoCoding crsGeoCoding = new CrsGeoCoding(DefaultGeographicCRS.WGS84,
                                                               descriptor.rasterWidth, descriptor.rasterHeight,
                                                               descriptor.easting, descriptor.northing,
                                                               descriptor.pixelSize, descriptor.pixelSize);
            targetProduct.setSceneGeoCoding(crsGeoCoding);

            final Band xBand = targetProduct.addBand("x_coord", ProductData.TYPE_FLOAT32);
            final Band yBand = targetProduct.addBand("y_coord", ProductData.TYPE_FLOAT32);

            // allocate target raster
            // ----------------------
            final int rasterSize = descriptor.rasterWidth * descriptor.rasterHeight;

            final float[] xData = new float[rasterSize];
            final float[] yData = new float[rasterSize];

            // and loop
            // --------
            final GeoPos geoPos = new GeoPos();
            final PixelPos pixelPos = new PixelPos();
            final GeoCoding sceneGeoCoding = sourceProduct.getSceneGeoCoding();
            for (int y = 0; y < descriptor.rasterHeight; y++) {
                int lineOffset = y * descriptor.rasterWidth;
                final double lat = descriptor.northing - y * descriptor.pixelSize;

                for (int x = 0; x < descriptor.rasterWidth; x++) {
                    final int writeIndex = x + lineOffset;

                    geoPos.lon = descriptor.easting + x * descriptor.pixelSize;
                    geoPos.lat = lat;
                    sceneGeoCoding.getPixelPos(geoPos, pixelPos);

                    xData[writeIndex] = (float) pixelPos.x;
                    yData[writeIndex] = (float) pixelPos.y;
                }
            }

            xBand.setRasterData(ProductData.createInstance(xData));
            yBand.setRasterData(ProductData.createInstance(yData));

            // "D:\\Satellite\\DELETE\\projected.dim"
            ProductIO.writeProduct(targetProduct, outputFilePath, DimapProductConstants.DIMAP_FORMAT_NAME);
        } finally {
            if (sourceProduct != null) {
                sourceProduct.dispose();
            }
            if (targetProduct != null) {
                targetProduct.dispose();
            }
        }
    }

    private static ProjectionDescriptor calculateRasterDimension(Product product) {
        final GeoPos[] geoBoundary = GeoUtils.createGeoBoundary(product, 10);
        double lonMin = Float.MAX_VALUE;
        double lonMax = -Float.MAX_VALUE;
        double latMin = Float.MAX_VALUE;
        double latMax = -Float.MAX_VALUE;

        for (GeoPos geoPos : geoBoundary) {
            if (geoPos.lon > lonMax) {
                lonMax = geoPos.lon;
            }
            if (geoPos.lon < lonMin) {
                lonMin = geoPos.lon;
            }
            if (geoPos.lat > latMax) {
                latMax = geoPos.lat;
            }
            if (geoPos.lat < latMin) {
                latMin = geoPos.lat;
            }
        }

        System.out.println("lon " + lonMin + " -> " + lonMax);
        System.out.println("lat " + latMin + " -> " + latMax);

        final double mapWidth = lonMax - lonMin;
        final double mapHeight = latMax - latMin;

        double pixelSize = Math.min(mapWidth / (double) product.getSceneRasterWidth(), mapHeight / (double) product.getSceneRasterHeight());
        System.out.println("pixelSize " + pixelSize);

        final int rasterWidth = 1 + (int) (mapWidth / pixelSize);
        final int rasterHeight = 1 + (int) (mapHeight / pixelSize);

        System.out.println("raster w: " + rasterWidth + " h: " + rasterHeight);

        final ProjectionDescriptor descriptor = new ProjectionDescriptor();

        descriptor.rasterWidth = rasterWidth;
        descriptor.rasterHeight = rasterHeight;
        descriptor.pixelSize = pixelSize;
        descriptor.easting = lonMin;
        descriptor.northing = latMax;
        return descriptor;
    }

    private static class ProjectionDescriptor {
        int rasterWidth;
        int rasterHeight;
        double pixelSize;
        double easting;
        double northing;
    }
}
