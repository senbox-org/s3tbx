package org.esa.s3tbx.dataio.s3.synergy;/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import org.esa.s3tbx.dataio.s3.LonLatFunction;
import org.junit.Ignore;
import org.junit.Test;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class LonLatTiePointFunctionTest {

    @Test
    public void testApproximation() throws IOException, URISyntaxException {
        NcFile ncFile = NcFile.openResource("tiepoints_olci.nc");
        File file = new File(this.getClass().getResource("tiepoints_olci.nc").getFile());
        List<Variable> variables = ncFile.getVariables(".*");
        ArrayList<File> ncFiles = new ArrayList<>();
        for (int i = 0; i < variables.size(); i++) {
            ncFiles.add(file);
        }
        LonLatTiePointFunctionSource source = new LonLatTiePointFunctionSource(variables, ncFiles, 1, 0);
        double[] latData = ncFile.read(variables.get(1).getFullName());
        double[] lonData = ncFile.read(variables.get(0).getFullName());

        LonLatTiePointFunction latFunction = new LonLatTiePointFunction(source, 1);
        LonLatTiePointFunction lonFunction = new LonLatTiePointFunction(source, 0);
        for (int i = 0; i < latData.length; i+=10) {
            final double lon = lonData[i];
            final double lat = latData[i];
            final double retrievedLat = latFunction.getValue(lon, lat);
            final double retrievedLon = lonFunction.getValue(lon, lat);
            assertEquals(lat, retrievedLat, 0.0);
            assertEquals(lon, retrievedLon, 0.0);
        }
    }

}
