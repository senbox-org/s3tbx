/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.s3tbx.dataio.landsat.geotiff.c2;

import org.esa.snap.core.datamodel.MetadataElement;

import java.io.IOException;
import java.io.Reader;

/**
 * @author Thomas Storm
 */
public class Landsat8C2Metadata extends AbstractLandsatC2Metadata {

    private static final String[] BAND_DESCRIPTIONS = {
            "Coastal Aerosol (Operational Land Imager (OLI))",
            "Blue (OLI)",
            "Green (OLI)",
            "Red (OLI)",
            "Near-Infrared (NIR) (OLI)",
            "Short Wavelength Infrared (SWIR) 1 (OLI)",
            "SWIR 2 (OLI)",
            "Panchromatic (OLI)",
            "Cirrus (OLI)",
            "Thermal Infrared Sensor (TIRS) 1",
            "TIRS 2"
    };

    private static final float[] WAVELENGTHS = {
            440,
            480,
            560,
            655,
            865,
            1610,
            2200,
            590,
            1370,
            10895,
            12005
    };
    private static final String[] BAND_NAMES = {
            "coastal_aerosol",
            "blue",
            "green",
            "red",
            "near_infrared",
            "swir_1",
            "swir_2",
            "panchromatic",
            "cirrus",
            "thermal_infrared_(tirs)_1",
            "thermal_infrared_(tirs)_2",
    };

    private static final float[] BANDWIDTHS = {
            20,
            60,
            60,
            30,
            30,
            80,
            180,
            180,
            20,
            590,
            1010
    };

    public Landsat8C2Metadata(Reader fileReader) throws IOException {
        super(fileReader);
    }

    public Landsat8C2Metadata(MetadataElement root) throws IOException {
        super(root);
    }

    @Override
    public float getWavelength(String bandIndexNumber) {
        int index = getIndex(bandIndexNumber);
        return WAVELENGTHS[index];
    }

    @Override
    public float getBandwidth(String bandIndexNumber) {
        int index = getIndex(bandIndexNumber);
        return BANDWIDTHS[index];
    }

    @Override
    public String getBandDescription(String bandNumber) {
        int index = getIndex(bandNumber);
        return BAND_DESCRIPTIONS[index];
    }

    @Override
    public String getBandNamePrefix(String bandNumber) {
        int index = getIndex(bandNumber);
        return BAND_NAMES[index];
    }
}
