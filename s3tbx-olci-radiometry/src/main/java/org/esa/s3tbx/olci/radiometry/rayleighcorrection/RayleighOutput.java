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

package org.esa.s3tbx.olci.radiometry.rayleighcorrection;

/**
 * @author muhammad.bc.
 */
public class RayleighOutput {
    float sourceRayRefl;
    float lowerRayRefl;
    float upperRayRefl;

    public RayleighOutput(float sourceRayRefl, float lowerRayRefl, float upperRayRefl) {
        this.sourceRayRefl = sourceRayRefl;
        this.lowerRayRefl = lowerRayRefl;
        this.upperRayRefl = upperRayRefl;
    }

    public float getSourceRayRefl() {
        return sourceRayRefl;
    }

    public float getLowerRayRefl() {
        return lowerRayRefl;
    }

    public float getUpperRayRefl() {
        return upperRayRefl;
    }
}

