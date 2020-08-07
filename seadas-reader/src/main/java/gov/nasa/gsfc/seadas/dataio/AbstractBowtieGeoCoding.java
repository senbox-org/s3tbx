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

import org.esa.snap.core.datamodel.AbstractGeoCoding;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.dataop.maptransf.Datum;

import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;

/**
 * The <code>AbstractBowtieGeoCoding</code> class is a special geo-coding for
 * MODIS Level-1B and Level-2 swath products.
 * <p/>
 * <p>It enables BEAM to transform the MODIS swaths to uniformly gridded
 * image that is geographically referenced according to user-specified
 * projection and resampling parameters.
 * Correction for oversampling between scans as a function of increasing
 * (off-nadir) scan angle is performed (correction for bow-tie effect).
 */
public abstract class AbstractBowtieGeoCoding extends AbstractGeoCoding {

    private Datum _datum = Datum.WGS_84;
    protected List<GeoCoding> gcList;
    protected boolean cross180;
    protected List<PolyLine> centerLineList;
    private int lastCenterLineIndex;           // index found on the previous search
    private int smallestValidIndex;
    private int biggestValidIndex;
    private ProductNode gridOwner;

    /**
     * Constructs geo-coding with MODIS Bowtie correction.
     *
     */
    public AbstractBowtieGeoCoding() {
        lastCenterLineIndex = 0;
    }

    /**
     * get the number of line in the whole scene
     * @return lines in the scene
     */
    abstract public int getSceneHeight();

    /**
     * get the number of lines (num detectors) in a scan
     * @return number of lines in a scan
     */
    abstract public int getScanlineHeight();

    /**
     * get the number of lines between the start of a scan and the first line of data
     * @return scan line offset
     */
    abstract public int getScanlineOffset();

    /**
     * Checks whether or not this geo-coding can determine the pixel position from a geodetic position.
     *
     * @return <code>true</code>, if so
     */
    public boolean canGetPixelPos() {
        return true;
    }

    /**
     * Checks whether or not this geo-coding can determine the geodetic position from a pixel position.
     *
     * @return <code>true</code>, if so
     */
    public boolean canGetGeoPos() {
        return true;
    }

    /**
     * Returns the pixel co-ordinates as x/y for a given geographical position given as lat/lon.
     *
     * @param geoPos   the geographical position as lat/lon in the coordinate system determined by {@link #getGeoCRS()}
     * @param pixelPos an instance of <code>Point</code> to be used as return value. If this parameter is
     *                 <code>null</code>, the method creates a new instance which it then returns.
     * @return the pixel co-ordinates as x/y
     */
    public PixelPos getPixelPos(GeoPos geoPos, PixelPos pixelPos) {
        if (pixelPos == null) {
            pixelPos = new PixelPos();
        }
        pixelPos.setInvalid();

        final int index = getGeoCodingIndexfor(geoPos);
        lastCenterLineIndex = index;
        final GeoCoding gc = gcList.get(index);
        if (gc != null) {
            gc.getPixelPos(geoPos, pixelPos);
        }

        if (pixelPos.isValid()) {
            pixelPos.y += (index * getScanlineHeight()) - getScanlineOffset();
            if(pixelPos.y < 0 || pixelPos.y >= getSceneHeight())
                pixelPos.setInvalid();
        }
        return pixelPos;
    }

    /**
     * Returns the latitude and longitude value for a given pixel co-ordinate.
     *
     * @param pixelPos the pixel's co-ordinates given as x,y
     * @param geoPos   an instance of <code>GeoPos</code> to be used as retun value. If this parameter is
     *                 <code>null</code>, the method creates a new instance which it then returns.
     * @return the geographical position as lat/lon in the coodinate system determined by {@link #getGeoCRS()}
     */
    public GeoPos getGeoPos(PixelPos pixelPos, GeoPos geoPos) {
        final int index = computeIndex(pixelPos);
        final GeoCoding gc = gcList.get(index);
        if (gc != null) {
            return gc.getGeoPos(new PixelPos(pixelPos.x, pixelPos.y - getScanlineHeight() * index + getScanlineOffset()), geoPos);
        } else {
            if (geoPos == null) {
                geoPos = new GeoPos();
            }
            geoPos.setInvalid();
            return geoPos;
        }
    }

    /**
     * Gets the datum, the reference point or surface against which {@link GeoPos} measurements are made.
     *
     * @return the datum
     */
    public Datum getDatum() {
        return _datum;
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
        for (GeoCoding gc : gcList) {
            if (gc != null) {
                gc.dispose();
            }
        }
        gcList.clear();
    }

    /**
     * Checks whether or not the longitudes of this geo-coding cross the +/- 180 degree meridian.
     *
     * @return <code>true</code>, if so
     */
    public boolean isCrossingMeridianAt180() {
        return cross180;
    }

    protected void initSmallestAndLargestValidGeocodingIndices() {
        for (int i = 0; i < gcList.size(); i++) {
            if (gcList.get(i) != null) {
                smallestValidIndex = i;
                break;
            }
        }
        for (int i = gcList.size() - 1; i > 0; i--) {
            if (gcList.get(i) != null) {
                biggestValidIndex = i;
                break;
            }
        }
    }

    protected static PolyLine createCenterPolyLine(GeoCoding geoCoding, final int sceneWidth,
                                                 final int sceneHeight) {

        final double numberOfSegments = 100.0;
        final double stepX = sceneWidth / numberOfSegments;

        final PixelPos pixelPos = new PixelPos();
        final GeoPos geoPos = new GeoPos();
        final PolyLine polyLine = new PolyLine();

        pixelPos.y = sceneHeight / 2f;

        for (pixelPos.x = 0; pixelPos.x < sceneWidth + 0.5; pixelPos.x += stepX) {
            geoCoding.getGeoPos(pixelPos, geoPos);
            if (pixelPos.x == 0) {
                polyLine.moveTo(geoPos.lon, geoPos.lat);
            } else {
                polyLine.lineTo(geoPos.lon, geoPos.lat);
            }
        }

        return polyLine;
    }

    private int computeIndex(PixelPos pixelPos) {
        final int y = (int) pixelPos.getY() + getScanlineOffset();
        final int index = y / getScanlineHeight();
        if (index < smallestValidIndex) {
            return smallestValidIndex;
        } else if (index > biggestValidIndex) {
            return biggestValidIndex;
        } else {
            return index;
        }
    }

    private int getGeoCodingIndexfor(final GeoPos geoPos) {
        int index = lastCenterLineIndex;
        index = getNextCenterLineIndex(index, 1);
        final PolyLine centerLine1 = centerLineList.get(index);
        double v = centerLine1.getDistance(geoPos.lon, geoPos.lat);
        int vIndex = index;

        int direction = -1;
        if (index == smallestValidIndex) {
            direction = +1;
        }
        while (true) {
            index += direction;
            index = getNextCenterLineIndex(index, direction);
            final PolyLine centerLine2 = centerLineList.get(index);
            final double v2 = centerLine2.getDistance(geoPos.lon, geoPos.lat);
            if (v2 < v) {
                if (index == smallestValidIndex || index == biggestValidIndex) {
                    return index;
                }
                v = v2;
                vIndex = index;
            } else if (direction == -1) {
                index++;
                direction = +1;
                index = getNextCenterLineIndex(index, direction);
                if (index == biggestValidIndex) {
                    return index;
                }
            } else if (direction == +1) {
                return vIndex;
            }
        }
    }

    private int getNextCenterLineIndex(int index, final int direction) {
        while (centerLineList.get(index) == null) {
            index += direction;
            if (index < smallestValidIndex) {
                index = biggestValidIndex;
            } else if (index > biggestValidIndex) {
                index = smallestValidIndex;
            }
        }
        return index;
    }

    static int calculateStartLine(int scanlineHeight, Rectangle region) {
        return region.y / scanlineHeight * scanlineHeight;
    }

    static int calculateStopLine(int scanlineHeight, Rectangle region) {
        return (region.y + region.height) / scanlineHeight * scanlineHeight + scanlineHeight;
    }

    protected static class PolyLine {

        private double _x1;
        private double _y1;
        private boolean _started;
        private ArrayList<Line2D.Double> _lines;

        public PolyLine() {
            _started = false;
        }

        public void lineTo(final double x, final double y) {
            _lines.add(new Line2D.Double(_x1, _y1, x, y));
            setXY1(x, y);
        }

        public void moveTo(final double x, final double y) {
            if (_started) {
                throw new IllegalStateException("Polyline already started");
            }
            setXY1(x, y);
            _lines = new ArrayList<>();
            _started = true;
        }

        private void setXY1(final double x, final double y) {
            _x1 = x;
            _y1 = y;
        }

        public double getDistance(final double x, final double y) {
            double smallestDistPoints = Double.MAX_VALUE;
            double pointsDist = smallestDistPoints;
            if (_lines != null && _lines.size() > 0) {
                for (final Line2D.Double line : _lines) {
                    final double distPoints = line.ptSegDistSq(x, y);
                    if (distPoints < smallestDistPoints) {
                        smallestDistPoints = distPoints;
                    }
                }

                pointsDist = Math.sqrt(smallestDistPoints);
            }

            return pointsDist;
        }
    }

    protected void setGridOwner(ProductNode gridOwner) {
        this.gridOwner = gridOwner;
    }

    protected class ModisTiePointGrid extends TiePointGrid {

        public ModisTiePointGrid(String name, int gridWidth, int gridHeight, double offsetX, double offsetY, double subSamplingX, double subSamplingY, float[] tiePoints) {
            super(name, gridWidth, gridHeight, offsetX, offsetY, subSamplingX, subSamplingY, tiePoints);
        }

        public ModisTiePointGrid(String name, int gridWidth, int gridHeight, double offsetX, double offsetY, double subSamplingX, double subSamplingY, float[] tiePoints, int discontinuity) {
            super(name, gridWidth, gridHeight, offsetX, offsetY, subSamplingX, subSamplingY, tiePoints, discontinuity);
        }

        @Override
        public ProductNode getOwner() {
            return gridOwner;
        }
    }

}
