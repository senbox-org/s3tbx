/*
 * Copyright (c) 2022.  Brockmann Consult GmbH (info@brockmann-consult.de)
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
 *
 *
 */

package org.esa.s3tbx.olci.radiometry.rayleigh;

import org.esa.s3tbx.olci.radiometry.SensorConstants;

import java.util.ArrayList;
import java.util.Collections;

/**
 * @author Marco Peters
 */
public class L8Utils {
    public final static ArrayList<String> BAND_NAME_LIST = new ArrayList<>();

    static {
        Collections.addAll(BAND_NAME_LIST, SensorConstants.L8_SPECTRAL_BAND_NAMES);
    }

    static int getSpectralBandIndex(String bandName) {
        return BAND_NAME_LIST.indexOf(bandName);
    }

    public static double toReflectances(double source, double radiance_offset, double radiance_scale,
                                        double reflectance_offset, double reflectance_scale) {
        double count = (source - radiance_offset) / radiance_scale;
        return count * reflectance_scale + reflectance_offset;
    }

}
