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

package org.esa.s3tbx.olci.radiometry.rayleigh;

/**
 * @author muhammad.bc.
 */
public class RayleighInput {
    private float[] sourceRefls;
    private float[] lowerRefls;
    private float[] upperRefls;

    private int sourceIndex;
    private int lowerWaterIndex;
    private int upperWaterIndex;

    public RayleighInput(float[] sourceRefl, float[] lowerRefl, float[] upperRefl, int sourceIndx, int lowerWaterIndx, int upperWaterIndx) {
        this.sourceRefls = sourceRefl;
        this.lowerRefls = lowerRefl;
        this.upperRefls = upperRefl;
        this.sourceIndex = sourceIndx;
        this.lowerWaterIndex = lowerWaterIndx;
        this.upperWaterIndex = upperWaterIndx;
    }

    int getSourceIndex() {
        return sourceIndex;
    }

    int getLowerWaterIndex() {
        return lowerWaterIndex;
    }

    int getUpperWaterIndex() {
        return upperWaterIndex;
    }

    float[] getSourceReflectences() {
        return sourceRefls;
    }

    float[] getLowerReflectences() {
        return lowerRefls;
    }

    float[] getUpperReflectences() {
        return upperRefls;
    }
}
