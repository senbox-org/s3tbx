package org.esa.s3tbx.dataio.landsat.geotiff.level2;

import java.io.IOException;

/**
 * Created by obarrile on 10/02/2019.
 */
public class LandsatL2QAFactory {
    static LandsatL2QA create(LandsatLevel2Metadata metadata) throws IOException {

        if(metadata.getSatellite().equals("LANDSAT_8")) {
            return new Landsat8L2QA();
        } else if (metadata.getSatellite().equals("LANDSAT_4") || metadata.getSatellite().equals("LANDSAT_5") || metadata.getSatellite().equals("LANDSAT_7")) {
            return new LandsatLEDAPSQA();
        }

        return null;
    }
}
