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

    public static final String collectionExpression = "L[COTEM]\\d{2}_L\\d\\w{2}_\\d{3}\\d{3}_\\d{8}_\\d{8}_\\d{2}_(T1|T2|RT)";
    public static final String esaCollectionExpression = collectionExpression + "_(MTI|KIS)";
    public static final Pattern LANDSAT_COLLECTION_PATTERN = Pattern.compile(collectionExpression);
    public static final Pattern ESA_LANDSAT_COLLECTION_PATTERN = Pattern.compile(esaCollectionExpression);
    public static final int COLL_PATTERN_END = 40;
    public static final int ESA_COLL_PATTERN_END = 44;

    static boolean isLandsatCollection(String filename) {
        return isCollection(filename, LANDSAT_COLLECTION_PATTERN, COLL_PATTERN_END);
    }

    static boolean isEsaLandsatCollection(String filename) {
        return isCollection(filename, ESA_LANDSAT_COLLECTION_PATTERN, ESA_COLL_PATTERN_END);
    }

    private static boolean isCollection(String filename, Pattern pattern, int patternEnd) {
        if (filename == null) {
            return false;
        }
        final Matcher matcher = pattern.matcher(filename);
        return matcher.find() && matcher.start() == 0 && matcher.end() == patternEnd;
    }

    static String getPatternSubtractedFilename(String filename) {
        if (isEsaLandsatCollection(filename)) {
            return filename.substring(ESA_COLL_PATTERN_END);
        }
        if(isLandsatCollection(filename)) {
            return filename.substring(COLL_PATTERN_END);
        }
        return null;
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
