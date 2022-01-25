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

import org.junit.BeforeClass;
import org.junit.Test;

//import static org.junit.Assert.fail;
//import static org.junit.Assert.assertEquals;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;

public class CollectionToolsTest {

    private final static String validCollectionFileName = "LC08_L5TP_197023_20211202_20211209_02_T1_MTL.json";
    private final static String validEsaCollectionFileName = "LC08_L5TP_197023_20211202_20211209_02_T1_MTI_MTL.json";

    private final static String invalidCollectionFileName = "invalid_LC08_L5TP_197023_20211202_20211209_02_T1_MTL.json";

    @Test
    public void isLandsatCollection() {
        assertThat(CollectionTools.isLandsatCollection("A filename which does not match the expected pattern"), is(false));
        assertThat(CollectionTools.isLandsatCollection(validCollectionFileName), is(true));
        assertThat(CollectionTools.isLandsatCollection(validEsaCollectionFileName), is(true));
    }

    @Test
    public void isEsaLandsatCollection() {
        assertThat(CollectionTools.isEsaLandsatCollection(validCollectionFileName), is(false));
        assertThat(CollectionTools.isEsaLandsatCollection(validEsaCollectionFileName), is(true));
    }

    @Test
    public void getCollectionFromFilename() {
        assertThat(CollectionTools.getCollectionFromFilename(validCollectionFileName), is(2));
        assertThat(CollectionTools.getCollectionFromFilename(invalidCollectionFileName), is(-1));
    }

    @Test
    public void getSatelliteFromFilename() {
        assertThat(CollectionTools.getSatelliteFromFilename(validCollectionFileName), is(8));
        assertThat(CollectionTools.getSatelliteFromFilename(invalidCollectionFileName), is(-1));
    }

    @Test
    public void getLevelFromFilename() {
        assertThat(CollectionTools.getLevelFromFilename(validCollectionFileName), is(5));
        assertThat(CollectionTools.getLevelFromFilename(invalidCollectionFileName), is(-1));
    }
}