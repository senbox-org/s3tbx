package org.esa.s3tbx.dataio.landsat.geotiff.level2;

import java.io.IOException;

/**
 * Created by obarrile on 10/02/2019.
 */
public class LandsatL2AerosolFactory {
    static LandsatL2Aerosol create(LandsatLevel2Metadata metadata) throws IOException {

        if(metadata.getSatellite().equals("LANDSAT_8")) {
            return new Landsat8L2Aerosol();
        }

        return null;
    }
}
