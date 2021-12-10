package org.esa.s3tbx.dataio.landsat.geotiff;

import org.esa.snap.core.dataio.ProductIOException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Created by obarrile on 02/01/2019.
 */
public class LandsatQAFactory {

    static LandsatQA create(File mtlFile) throws IOException {

        boolean isCollectionProduct = false;
        boolean isOLI = false;
        boolean isMSS = false;
        boolean isTM = false;

        BufferedReader reader = null;
        try {
            FileReader fileReader = new FileReader(mtlFile);
            reader = new BufferedReader(fileReader);
            String line = reader.readLine();
            while (line != null) {
                if (line.contains("COLLECTION_NUMBER")) {
                    isCollectionProduct = true;
                }
                if (line.contains("SENSOR_ID")) {
                    if (line.contains("OLI")) {
                        isOLI = true;
                    } else if (line.contains("MSS")) {
                        isMSS = true;
                    } else if (line.contains("TM")) {
                        isTM = true;
                    }
                }

                if(isCollectionProduct && isMSS) return new CollectionMSSLandsatQA();
                if(isCollectionProduct && isOLI) return new CollectionOLILandsatQA();
                if(isCollectionProduct && isTM) return new CollectionTMLandsatQA();
                if(!isCollectionProduct && isOLI) return new PreCollectionLandsatQA();

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
