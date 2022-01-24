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

import org.esa.s3tbx.dataio.landsat.geotiff.LandsatLegacyMetadata;

import java.io.FileReader;
import java.io.IOException;

/**
 * Adaptation of LandsatReprocessedMetadata for Collection 2 changes.
 *
 * @author Cosmin Cara
 */
public class LandsatC2ReprocessedMetadata extends AbstractLandsatC2Metadata {

    private static final float[] ETM_PLUS_WAVELENGTHS = {
            485.0f,
            560.0f,
            660.0f,
            835.0f,
            1650.0f,
            11450.0f,
            2220.0f,
            710.0f
    };
    private static final float[] ETM_PLUS_BANDWIDTHS = {
            70.0f,
            80.0f,
            60.0f,
            130.0f,
            200.0f,
            2100.0f,
            260.0f,
            380.0f
    };
    private final LandsatLegacyMetadata landsatLegacyMetadataDelegate;

    public LandsatC2ReprocessedMetadata(FileReader fileReader) throws IOException {
        super(fileReader);
        landsatLegacyMetadataDelegate = new LandsatLegacyMetadata(getMetaDataElementRoot());
    }

    @Override
    public float getWavelength(String bandNumber) {
        if (sensorId.startsWith("ETM")) {
            String bandIndexNumber = bandNumber.substring(0, 1);
            int index = Integer.parseInt(bandIndexNumber) - 1;
            return ETM_PLUS_WAVELENGTHS[index];
        }
        return landsatLegacyMetadataDelegate.getWavelength(bandNumber);
    }

    @Override
    public float getBandwidth(String bandNumber) {
        if (sensorId.startsWith("ETM")) {
            String bandIndexNumber = bandNumber.substring(0, 1);
            int index = Integer.parseInt(bandIndexNumber) - 1;
            return ETM_PLUS_BANDWIDTHS[index];
        }
        return landsatLegacyMetadataDelegate.getBandwidth(bandNumber);
    }

    @Override
    public String getBandDescription(String bandNumber) {
        return landsatLegacyMetadataDelegate.getBandDescription(bandNumber);
    }

    @Override
    public String getBandNamePrefix(String bandNumber) {
        return landsatLegacyMetadataDelegate.getBandNamePrefix(bandNumber);
    }
}
