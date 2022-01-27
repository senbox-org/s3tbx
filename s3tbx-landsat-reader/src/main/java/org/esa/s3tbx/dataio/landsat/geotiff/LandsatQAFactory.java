package org.esa.s3tbx.dataio.landsat.geotiff;

import org.esa.s3tbx.dataio.landsat.geotiff.c1.CollectionMSSLandsatQA;
import org.esa.s3tbx.dataio.landsat.geotiff.c1.CollectionOLILandsatQA;
import org.esa.s3tbx.dataio.landsat.geotiff.c1.CollectionTMLandsatQA;
import org.esa.s3tbx.dataio.landsat.geotiff.c2.Collection2MSSLandsatQA;
import org.esa.s3tbx.dataio.landsat.geotiff.c2.Collection2OLILandsatQA;
import org.esa.s3tbx.dataio.landsat.geotiff.c2.Collection2TMLandsatQA;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Created by obarrile on 02/01/2019.
 */
public class LandsatQAFactory {

    static LandsatQA create(File mtlFile) throws IOException {
        boolean isOLI = false;
        boolean isMSS = false;
        boolean isTM = false;

        BufferedReader reader = null;
        int collection = LandsatTypeInfo.getCollectionNumber(mtlFile.getName());
        boolean isCollectionProduct = collection > 0;
        try {
            FileReader fileReader = new FileReader(mtlFile);
            reader = new BufferedReader(fileReader);
            String line = reader.readLine();
            while (line != null) {
                if (line.contains("SENSOR_ID")) {
                    if (line.contains("OLI")) {
                        isOLI = true;
                    } else if (line.contains("MSS")) {
                        isMSS = true;
                    } else if (line.contains("TM")) {
                        isTM = true;
                    }
                }
                if (isCollectionProduct && isMSS)
                    return collection == 1 ? new CollectionMSSLandsatQA() : new Collection2MSSLandsatQA();
                if (isCollectionProduct && isOLI)
                    return collection == 1 ? new CollectionOLILandsatQA() : new Collection2OLILandsatQA();
                if (isCollectionProduct && isTM)
                    return collection == 1 ? new CollectionTMLandsatQA() : new Collection2TMLandsatQA();
                if (!isCollectionProduct && isOLI) return new PreCollectionLandsatQA();

                line = reader.readLine();
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        return null;
    }
}
