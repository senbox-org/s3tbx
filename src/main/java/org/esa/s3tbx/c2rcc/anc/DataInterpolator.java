package org.esa.s3tbx.c2rcc.anc;

import org.esa.snap.framework.dataio.ProductSubsetDef;
import org.esa.snap.framework.datamodel.AbstractGeoCoding;
import org.esa.snap.framework.datamodel.Band;
import org.esa.snap.framework.datamodel.GeoCoding;
import org.esa.snap.framework.datamodel.GeoPos;
import org.esa.snap.framework.datamodel.PixelPos;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.datamodel.ProductData;
import org.esa.snap.framework.datamodel.Scene;
import org.esa.snap.framework.dataop.maptransf.Datum;
import org.esa.snap.util.SystemUtils;

import java.io.IOException;

public abstract class DataInterpolator {

    abstract double getValue(double timeMJD, double latitude, double longitude) throws IOException;

    abstract void dispose();

    protected static Band ensureInterpolation(final String startOrEnd, Product product, String bandName, double defaultValue) throws IOException {
        final Band band = product != null ? product.getBand(bandName) : null;
        if (band == null) {
            SystemUtils.LOG.info("Fallback interpolation " + startOrEnd + " for " + bandName + " --> default value = " + defaultValue);
            return new Band(bandName, ProductData.TYPE_FLOAT64, 1, 1) {
                @Override
                public double getPixelDouble(int x, int y) {
                    return defaultValue;
                }
            };
        }
        if (!band.hasRasterData()) {
            band.readRasterDataFully();
        }
        return band;
    }

    protected static double getValue(Band band, GeoCoding geoCoding, double latitude, double longitude) throws IOException {
        final PixelPos pp = geoCoding.getPixelPos(new GeoPos(latitude, longitude), null);
        return band.getPixelDouble((int) pp.x, (int) pp.y);
    }

    protected static GeoCoding ensureGeocoding(Product product) {
        final int width;
        final int height;
        if (product != null) {
            final GeoCoding geoCoding = product.getGeoCoding();
            if (geoCoding != null) {
                return geoCoding;
            }
            width = product.getSceneRasterWidth();
            height = product.getSceneRasterHeight();
        } else {
            width = 1;
            height = 1;
        }
        return new GlobalGeoCoding(width, height);
    }

    static class GlobalGeoCoding extends AbstractGeoCoding {

        private final int width;

        private final int height;

        public GlobalGeoCoding(int width, int height) {
            this.width = width;
            this.height = height;
        }

        @Override
        public boolean transferGeoCoding(Scene srcScene, Scene destScene, ProductSubsetDef subsetDef) {
            return false;
        }

        @Override
        public boolean isCrossingMeridianAt180() {
            return false;
        }

        @Override
        public boolean canGetPixelPos() {
            return true;
        }

        @Override
        public boolean canGetGeoPos() {
            return false;
        }

        @Override
        public PixelPos getPixelPos(GeoPos geoPos, PixelPos pixelPos) {
            if (pixelPos == null) {
                pixelPos = new PixelPos();
            }
            final double lat = geoPos.getLat() - 90;
            final double lon = geoPos.getLon() + 180;

            double xFloor = Math.floor(lon / 360 * width);
            if (xFloor >= width) {
                xFloor--;
            }
            final double x = xFloor + 0.5;

            double yFloor = Math.floor(lat / -180 * height);
            if (yFloor >= height) {
                yFloor--;
            }
            final double y = yFloor + 0.5;

            pixelPos.setLocation(x, y);
            return pixelPos;
        }

        @Override
        public GeoPos getGeoPos(PixelPos pixelPos, GeoPos geoPos) {
            return null;
        }

        @Override
        public Datum getDatum() {
            return null;
        }

        @Override
        public void dispose() {
        }
    }
}
