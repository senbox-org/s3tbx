/*
 * Copyright (c) 2021.  Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.s3tbx.dataio.s3.aatsr;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.junit.Assume;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

/**
 * @author Marco Peters
 * TODO - delete this test after finishing AATSR support
 */
public class ReadingTest {
    private final static String P1 = "H:\\AATSR\\(A)ATSR_4th_Repro_L1B\\v2.0.5\\ENV_AT_1_RBT____20020810T083508_20020810T102042_20210303T040313_6334_008_264______DSI_R_NT_004.SEN3";
    private final static String P2 = "H:\\AATSR\\(A)ATSR_4th_Repro_L1B\\v2.0.5\\ENV_AT_1_RBT____20021129T235200_20021130T013735_20210315T024827_6334_011_359______DSI_R_NT_004.SEN3";

    @Test
    public void read() throws IOException {
        final File p1File = new File(P1);
        Assume.assumeTrue(p1File.exists());

        final Product product = ProductIO.readProduct(p1File);

        final GeoPos geoPos = product.getSceneGeoCoding().getGeoPos(new PixelPos(316.5, 14379.5), null);
        // Expected
        // 58°59'12.60"N
        // 27°43'57.29"E
        // --> 58.98683 N, 27.73258 E

        System.out.printf(Locale.ENGLISH, "lat: %f lon: %f", geoPos.getLat(), geoPos.getLon());

    }
}
