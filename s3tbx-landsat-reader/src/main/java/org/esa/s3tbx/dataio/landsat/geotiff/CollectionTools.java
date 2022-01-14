/*
 *
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
 */

package org.esa.s3tbx.dataio.landsat.geotiff;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CollectionTools {

    private static final Pattern COLLECTION_FILENAME_PATTERN = Pattern.compile("L[COTEM]\\d{2}_L\\d\\w{2}_\\d{3}\\d{3}_\\d{8}_\\d{8}_\\d{2}_(T1|T2|RT)");
    private static final Pattern ESA_PATTERN = Pattern.compile("_(MTI|KIS)");
    private static final int COLL_PATTERN_END = 40;

    static boolean isLandsatCollection(String filename) {
        if (filename == null) {
            return false;
        }
        final Matcher matcher = COLLECTION_FILENAME_PATTERN.matcher(filename);
        return matcher.find() && matcher.start() == 0 && matcher.end() == COLL_PATTERN_END;
    }

    static boolean isEsaLandsatCollection(String filename) {
        if (!isLandsatCollection(filename)) {
            return false;
        }
        final Matcher matcher = ESA_PATTERN.matcher(filename.substring(COLL_PATTERN_END));
        return matcher.find() && matcher.start() == 0;
    }

    static int getCollectionFromFilename(String filename) {
        if (isLandsatCollection(filename)) {
            final String s = filename.substring(35, 37);
            return Integer.parseInt(s);
        }
        return -1;
    }

    static int getLevelFromFilename(String filename) {
        if (isLandsatCollection(filename)) {
            final String s = filename.substring(6, 7);
            return Integer.parseInt(s);
        }
        return -1;
    }

    static int getSatelliteFromFilename(String filename) {
        if (isLandsatCollection(filename)) {
            final String s = filename.substring(2, 4);
            return Integer.parseInt(s);
        }
        return -1;
    }
}
