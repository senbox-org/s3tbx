/*
 * Copyright (C) 2015 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package gov.nasa.gsfc.seadas.dataio;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoCodingFactory;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.Scene;
import org.esa.snap.core.subset.PixelSubsetRegion;
import org.esa.snap.core.util.Guardian;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.math.IndexValidator;
import org.esa.snap.core.util.math.Range;

import java.io.IOException;
import java.util.ArrayList;

/**
 * The <code>BowtiePixelGeoCoding</code> class is a special geo-coding for
 * MODIS Level-1B and Level-2 swath products.
 * <p/>
 * <p>It enables BEAM to transform the MODIS swaths to uniformly gridded
 * image that is geographically referenced according to user-specified
 * projection and resampling parameters.
 * Correction for oversampling between scans as a function of increasing
 * (off-nadir) scan angle is performed (correction for bow-tie effect).
 */
public class BowtiePixelGeoCoding extends AbstractBowtieGeoCoding {
    private Band latBand;
    private Band lonBand;
    int _scanlineHeight;
    int _scanlineOffset;

    /**
     * Constructs geo-coding based on two given tie-point grids.
     *
     * @param latBand       the latitude band, must not be <code>null</code>
     * @param lonBand       the longitude band, must not be <code>null</code>
     * @param scanlineHeight the number of detectors in a scan
     */
    public BowtiePixelGeoCoding(Band latBand, Band lonBand, int scanlineHeight) {
        super();
        Guardian.assertNotNull("latBand", latBand);
        Guardian.assertNotNull("lonBand", lonBand);
        if (latBand.getRasterWidth() != lonBand.getRasterWidth() ||
                latBand.getRasterHeight() != lonBand.getRasterHeight()) {
            throw new IllegalArgumentException("latBand is not compatible with lonBand");
        }
        this.latBand = latBand;
        this.lonBand = lonBand;
        setGridOwner(this.lonBand.getOwner());
        _scanlineHeight = scanlineHeight;
        _scanlineOffset = 0;
        try {
            init();
        } catch (IOException e) {
            throw new IllegalArgumentException("can not init geocode", e);
        }
    }

    /**
     * get the number of line in the whole scene
     * @return lines in the scene
     */
    public int getSceneHeight() {
        return lonBand.getRasterHeight();
    }

    /**
     * get the number of lines (num detectors) in a scan
     * @return number of lines in a scan
     */
    public int getScanlineHeight() {
        return _scanlineHeight;
    }

    /**
     * get the number of lines between the start of a scan and the first line of data
     * @return scan line offset
     */
    public int getScanlineOffset() {
        return _scanlineOffset;
    }

    public Band getLatBand() {
        return latBand;
    }

    public Band getLonBand() {
        return lonBand;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        BowtiePixelGeoCoding that = (BowtiePixelGeoCoding) o;
        if (latBand == null || that.latBand == null) {
            return false;
        }
        if (!latBand.equals(that.latBand)) {
            return false;
        }
        if (lonBand == null || that.lonBand == null) {
            return false;
        }
        if (!lonBand.equals(that.lonBand)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = latBand != null ? latBand.hashCode() : 0;
        result = 31 * result + (lonBand != null ? lonBand.hashCode() : 0);
        return result;
    }

    /**
     * Releases all of the resources used by this object instance and all of its owned children. Its primary use is to
     * allow the garbage collector to perform a vanilla job.
     * <p/>
     * <p>This method should be called only if it is for sure that this object instance will never be used again. The
     * results of referencing an instance of this class after a call to <code>dispose()</code> are undefined.
     */
    @Override
    public void dispose() {
        super.dispose();
        latBand = null;
        lonBand = null;
    }

    /**
     * walk through the latitude and find the edge of the scan
     * where the lat overlaps the previous lat.  set _scanlineOffset
     */
    private void calculateScanlineOffset() {
        // look at first pixel in each line
        int start = findStart(0);
        // if not found try end of line
        if(start == -1) {
            start = findStart(latBand.getRasterWidth() - 1);
        }

        if(start == -1) {       // did not find an overlap
            _scanlineOffset = 0;
        } else {
            _scanlineOffset = start % _scanlineHeight;
        }
    }

    private int findStart(int x) {
        int increaseDecreaseCount = 0;

        for (int i = 1; i < latBand.getRasterHeight(); i++) {
            float p0 = latBand.getPixelFloat(x, i - 1);
            float p1 = latBand.getPixelFloat(x, i);
            if (Float.isNaN(p0) || p0 > 90.0 || p0 < -90.0 || Float.isNaN(p1) || p1 > 90.0 || p1 < -90.0) {
                continue;
            }
            int change = 0;
            if((p0 - p1) < -0.001) {
                // increase
                change = +1;
            } else if((p1 - p0) < -0.001) {
                // decrease
                change = -1;
            }
            if (increaseDecreaseCount > 1 && change == -1) {
                return i;
            } else if (increaseDecreaseCount < -1 && change == +1) {
                return i;
            } else {
                increaseDecreaseCount += change;
            }
        }
        return -1;
    }

    private void init() throws IOException {
        gcList = new ArrayList<GeoCoding>();
        centerLineList = new ArrayList<PolyLine>();

        latBand.readRasterDataFully(ProgressMonitor.NULL);
        lonBand.readRasterDataFully(ProgressMonitor.NULL);

        final float[] latFloats = (float[]) latBand.getDataElems();
        final float[] lonFloats = (float[]) lonBand.getDataElems();

        calculateScanlineOffset();

        final int scanW = lonBand.getRasterWidth();
        final int sceneH = lonBand.getRasterHeight();

        final int gcRawWidth = scanW * _scanlineHeight;

        int firstY = 0;

        // create first if needed
        // use the delta from the neighboring stripe to extrapolate the data
        if (_scanlineOffset != 0) {
            firstY = _scanlineHeight - _scanlineOffset;
            final float[] lats = new float[gcRawWidth];
            final float[] lons = new float[gcRawWidth];
            System.arraycopy(lonFloats, 0, lons, _scanlineOffset * scanW, firstY * scanW);
            System.arraycopy(latFloats, 0, lats, _scanlineOffset * scanW, firstY * scanW);
            for (int x = 0; x < scanW; x++) {
                float deltaLat;
                float deltaLon;
                float refLat = latFloats[x];
                float refLon = lonFloats[x];
                if (firstY > 1) {
                    deltaLat = latFloats[scanW + x] - latFloats[x];
                    deltaLon = lonFloats[scanW + x] - lonFloats[x];
                } else {
                    deltaLat = latFloats[(firstY + 1) * scanW + x] - latFloats[firstY * scanW + x];
                    deltaLon = lonFloats[(firstY + 1) * scanW + x] - lonFloats[firstY * scanW + x];
                }
                for (int y = 0; y < _scanlineOffset; y++) {
                    lons[y * scanW + x] = refLon - (deltaLon * (_scanlineOffset - y));
                    lats[y * scanW + x] = refLat - (deltaLat * (_scanlineOffset - y));
                }
            }
            addStripeGeocode(lats, lons, 0, scanW, _scanlineHeight);
        }

        // add all of the normal scans
        for (; firstY + _scanlineHeight <= sceneH; firstY += _scanlineHeight) {
            final float[] lats = new float[gcRawWidth];
            final float[] lons = new float[gcRawWidth];
            System.arraycopy(lonFloats, firstY * scanW, lons, 0, gcRawWidth);
            System.arraycopy(latFloats, firstY * scanW, lats, 0, gcRawWidth);
            addStripeGeocode(lats, lons, firstY, scanW, _scanlineHeight);
        }

        // create last stripe
        int lastStripeH = sceneH - firstY;
        if (lastStripeH > 0) {
            int lastStripeY = sceneH - lastStripeH; // y coord of first y of last stripe
            final float[] lats = new float[gcRawWidth];
            final float[] lons = new float[gcRawWidth];
            System.arraycopy(lonFloats, lastStripeY * scanW, lons, 0, lastStripeH * scanW);
            System.arraycopy(latFloats, lastStripeY * scanW, lats, 0, lastStripeH * scanW);
            for (int x = 0; x < scanW; x++) {
                float deltaLat;
                float deltaLon;
                float refLat = latFloats[(sceneH-1) * scanW + x];
                float refLon = lonFloats[(sceneH-1) * scanW + x];
                if (lastStripeH > 1) {
                    deltaLat = refLat - latFloats[(sceneH - 2) * scanW + x];
                    deltaLon = refLon - lonFloats[(sceneH - 2) * scanW + x];
                } else {
                    deltaLat = latFloats[(firstY - 1) * scanW + x] - latFloats[(firstY - 2) * scanW + x];
                    deltaLon = lonFloats[(firstY - 1) * scanW + x] - lonFloats[(firstY - 2) * scanW + x];
                }
                for (int y = 0; y < _scanlineHeight - lastStripeH; y++) {
                    lats[(y + lastStripeH) * scanW + x] = refLat + (deltaLat * y);
                    lons[(y + lastStripeH) * scanW + x] = refLon + (deltaLon * y);
                }
            }
            addStripeGeocode(lats, lons, lastStripeY, scanW, _scanlineHeight);
        }

        initSmallestAndLargestValidGeocodingIndices();
    }

    private void addStripeGeocode(float[] lats, float[] lons, int y, int stripeW, int stripeH) throws IOException {
        GeoCoding gc = createStripeGeocode(lats, lons, y, stripeW, stripeH);
        if (gc != null) {
            gcList.add(gc);
            centerLineList.add(createCenterPolyLine(gc, stripeW, stripeH));
        } else {
            gcList.add(gcList.size(), null);
            centerLineList.add(centerLineList.size(), null);
        }
    }

    private GeoCoding createStripeGeocode(float[] lats, float[] lons, int y, int stripeW, int stripeH) throws IOException {
        final Range range = Range.computeRangeFloat(lats, IndexValidator.TRUE, null, ProgressMonitor.NULL);
        if (range.getMin() < -90) {
            return null;
        } else {
            final BowtiePixelScanGeoCoding geoCoding = new BowtiePixelScanGeoCoding(lats, lons, stripeW, stripeH);
            cross180 = cross180 || geoCoding.isCrossingMeridianAt180();
            return geoCoding;
        }
    }

    /**
     * Transfers the geo-coding of the {@link Scene srcScene} to the {@link Scene destScene} with respect to the given
     * {@link ProductSubsetDef subsetDef}.
     *
     * @param srcScene  the source scene
     * @param destScene the destination scene
     * @param subsetDef the definition of the subset, may be <code>null</code>
     * @return true, if the geo-coding could be transferred.
     */
    @Override
    public boolean transferGeoCoding(final Scene srcScene, final Scene destScene, final ProductSubsetDef subsetDef) {

        BowtiePixelGeoCoding srcGeocoding = (BowtiePixelGeoCoding)srcScene.getGeoCoding();
        final String latBandName = srcGeocoding.latBand.getName();
        final String lonBandName = srcGeocoding.lonBand.getName();

        try {
            ensureLatLonBands(destScene, subsetDef);
        } catch (IOException e) {
            return false;
        }
        final Band targetLatBand = destScene.getProduct().getBand(latBandName);
        final Band targetLonBand = destScene.getProduct().getBand(lonBandName);
        if(subsetDef != null) {
            if(subsetDef.getSubSamplingY() != 1) {
                destScene.setGeoCoding(GeoCodingFactory.createPixelGeoCoding(targetLatBand, targetLonBand, null, 5));
                return true;
            }
        }

        if (targetLatBand != null && targetLonBand != null) {
            destScene.setGeoCoding(new BowtiePixelGeoCoding(targetLatBand, targetLonBand, srcGeocoding._scanlineHeight));
            return true;
        }
        return false;
    }

    private void ensureLatLonBands(Scene destScene, ProductSubsetDef subsetDef) throws IOException {
        Band destLatBand = destScene.getProduct().getBand(latBand.getName());
        Band destLonBand = destScene.getProduct().getBand(lonBand.getName());
        if (destLatBand == null || destLonBand == null) {
            Band tempLatBand;
            Band tempLonBand;
            if (subsetDef != null) {
                ProductSubsetDef bandSubset = new ProductSubsetDef();
                bandSubset.addNodeName(latBand.getName());
                bandSubset.addNodeName(lonBand.getName());
                bandSubset.setSubsetRegion(new PixelSubsetRegion(subsetDef.getRegion(), 0));
                bandSubset.setSubSampling(subsetDef.getSubSamplingX(), subsetDef.getSubSamplingY());
                Product temp = latBand.getProduct().createSubset(bandSubset, "__temp", "");
                tempLatBand = temp.getBand(latBand.getName());
                tempLonBand = temp.getBand(lonBand.getName());
            }else {
                tempLatBand = latBand;
                tempLonBand = lonBand;
            }


            ProductUtils.copyBand(tempLatBand.getName(), tempLatBand.getProduct(), destScene.getProduct(), true);
            ProductUtils.copyBand(tempLonBand.getName(), tempLonBand.getProduct(), destScene.getProduct(), true);
        }
     }

}
