/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.s3tbx.dataio.merisl3;

import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.datamodel.AbstractGeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Scene;
import org.esa.snap.core.dataop.maptransf.Datum;

// TODO: 20.02.2020 SE -- Marked GETGEOPOS ... remove this class?

/**
 * Experimental ISIN geo-coding for the MERIS binned Level-2 product.
 * This is not public API.
 */
public class ISINGeoCoding extends AbstractGeoCoding {

    private ISINGrid _grid;

    public ISINGeoCoding(ISINGrid grid) {
        _grid = grid;
    }

    @Override
    public boolean transferGeoCoding(Scene srcScene, Scene destScene, ProductSubsetDef subsetDef) {
        return false;
    }

    public boolean isCrossingMeridianAt180() {
        return false;
    }

    public boolean canGetPixelPos() {
        return true;
    }

    public boolean canGetGeoPos() {
        return true;
    }

    public PixelPos getPixelPos(final GeoPos geoPos, PixelPos pixelPos) {
        pixelPos = (pixelPos == null) ? new PixelPos() : pixelPos;
        pixelPos.x = -1;
        pixelPos.y = -1;
        int rowIndex = (int) ((90.0f - geoPos.lat) * _grid.getRowCount() / 180.0f);
        if (rowIndex >= 0 && rowIndex < _grid.getRowCount()) {
            int colIndex = (int) ((180.0f + geoPos.lon) / _grid.getDeltaLon(rowIndex));
            int rowLength = _grid.getRowLength(rowIndex);
            if (colIndex >= 0 && colIndex < rowLength) {
                pixelPos.x = _grid.getRowCount() - (rowLength / 2) + colIndex;
                pixelPos.y = rowIndex;
            }
        }
        return pixelPos;
    }

    public GeoPos getGeoPos(final PixelPos pixelPos, GeoPos geoPos) {
        geoPos = (geoPos == null) ? new GeoPos() : geoPos;
        // TODO: 20.02.2020 SE fixed -- Marked GETGEOPOS ... -1 ?  NAN ?
        geoPos.setInvalid();
        // TODO: 20.02.2020 SE fixed -- Marked GETGEOPOS abort condition
        int rowIndex = calcIndex(pixelPos.y, _grid.getRowCount());
        if (rowIndex != -1) {
            int rowLength = _grid.getRowLength(rowIndex);
            // TODO: 20.02.2020 SE -- Marked GETGEOPOS abort condition
            double colIndex = pixelPos.x - _grid.getRowCount() + (rowLength / 2);
            if (colIndex >= 0 && colIndex < rowLength) {
                geoPos.lat = 90.0f - 180.0f * (pixelPos.y) / _grid.getRowCount();
                geoPos.lon = (float) (colIndex * _grid.getDeltaLon(rowIndex) - 180f);
            }
        }
        return geoPos;
    }

    // TODO: 20.02.2020 SE fixed -- Marked GETGEOPOS abort condition
    public int calcIndex(double v, int maxVal) {
        if (v < 0 || v > maxVal) {
            return -1;
        }
        return Math.min(maxVal - 1, (int) v);
    }

    public Datum getDatum() {
        return Datum.WGS_84;
    }

    public void dispose() {
        _grid = null;
    }
}
