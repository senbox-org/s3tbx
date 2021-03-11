/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.s3tbx.dataio.spot;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertySet;
import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;


@RunWith(TestDirRunner.class)
public class SpotVgtProductReaderPlugInTest {

    @Test
    public void testGetBandName() {
        assertEquals("VZA", SpotVgtProductReaderPlugIn.getBandName("VZA.HDF"));
        assertEquals("VZA", SpotVgtProductReaderPlugIn.getBandName("_VZA.HDF"));
        assertEquals("B2", SpotVgtProductReaderPlugIn.getBandName("V220050513179_B2.HDF"));
        assertEquals("MIR", SpotVgtProductReaderPlugIn.getBandName("V220050513179_MIR.HDF"));
    }

    @Test
    public void testPhysVolumeDescriptor() throws IOException {
        File dir = TestDataDir.get();
        File file = new File(dir, "decode_qual_intended/PHYS_VOL.TXT");

        PropertySet physVolDescriptor = SpotVgtProductReaderPlugIn.readPhysVolDescriptor(file);
        assertNotNull(physVolDescriptor);
        Property[] properties = physVolDescriptor.getProperties();
        assertEquals(24, properties.length);
        assertEquals("1", physVolDescriptor.getValue("PHYS_VOL_NUMBER"));
        assertEquals("V2KRNS10__20060721E", physVolDescriptor.getValue("PRODUCT_#0001_ID"));
        assertEquals("_MIR.HDF", physVolDescriptor.getValue("PRODUCT_#0001_PLAN_09"));
    }

    @Test
    public void testDecodeQualification() {
        SpotVgtProductReaderPlugIn plugIn = new SpotVgtProductReaderPlugIn();

        assertEquals(DecodeQualification.UNABLE, plugIn.getDecodeQualification(null));

        File dir = TestDataDir.get();
        File file;

        file = new File(dir, "decode_qual_intended.zip");
        assertEquals(DecodeQualification.INTENDED, plugIn.getDecodeQualification(file));
        assertEquals(DecodeQualification.INTENDED, plugIn.getDecodeQualification(file.getPath()));

        file = new File(dir, "decode_qual_intended");
        assertEquals(DecodeQualification.INTENDED, plugIn.getDecodeQualification(file));
        assertEquals(DecodeQualification.INTENDED, plugIn.getDecodeQualification(file.getPath()));

        file = new File(dir, "decode_qual_intended/PHYS_VOL.TXT");
        assertEquals(DecodeQualification.INTENDED, plugIn.getDecodeQualification(file));
        assertEquals(DecodeQualification.INTENDED, plugIn.getDecodeQualification(file.getPath()));

        file = new File(dir, "decode_qual_unable_1");
        assertEquals(DecodeQualification.UNABLE, plugIn.getDecodeQualification(file));
        assertEquals(DecodeQualification.UNABLE, plugIn.getDecodeQualification(file.getPath()));

        file = new File(dir, "decode_qual_unable_1/PHYS_VOL.TXT");
        assertEquals(DecodeQualification.UNABLE, plugIn.getDecodeQualification(file));
        assertEquals(DecodeQualification.UNABLE, plugIn.getDecodeQualification(file.getPath()));

        file = new File(dir, "decode_qual_unable_2");
        assertEquals(DecodeQualification.UNABLE, plugIn.getDecodeQualification(file));
        assertEquals(DecodeQualification.UNABLE, plugIn.getDecodeQualification(file.getPath()));

        file = new File(dir, "decode_qual_unable_2/PHYS_VOL.TXT");
        assertEquals(DecodeQualification.UNABLE, plugIn.getDecodeQualification(file));
        assertEquals(DecodeQualification.UNABLE, plugIn.getDecodeQualification(file.getPath()));

        file = new File(dir, "decode_qual_unable_3");
        assertEquals(DecodeQualification.UNABLE, plugIn.getDecodeQualification(file));
        assertEquals(DecodeQualification.UNABLE, plugIn.getDecodeQualification(file.getPath()));

        file = new File(dir, "decode_qual_unable_3/TEST.TXT");
        assertEquals(DecodeQualification.UNABLE, plugIn.getDecodeQualification(file));
        assertEquals(DecodeQualification.UNABLE, plugIn.getDecodeQualification(file.getPath()));

        file = new File(dir, "decode_qual_unable_3/NON_EXISTENT");
        assertEquals(DecodeQualification.UNABLE, plugIn.getDecodeQualification(file));
        assertEquals(DecodeQualification.UNABLE, plugIn.getDecodeQualification(file.getPath()));

        // new for VGT 1P Collection3 products:
        file = new File(dir, "decode_qual_intended_collection3/zip/V220050513179.zip");
        assertEquals(DecodeQualification.INTENDED, plugIn.getDecodeQualification(file));
        assertEquals(DecodeQualification.INTENDED, plugIn.getDecodeQualification(file.getPath()));

        file = new File(dir, "decode_qual_intended_collection3/V220050513179");
        assertEquals(DecodeQualification.INTENDED, plugIn.getDecodeQualification(file));
        assertEquals(DecodeQualification.INTENDED, plugIn.getDecodeQualification(file.getPath()));

        file = new File(dir, "decode_qual_unable_1_missing_bands_collection3/V220050513179");
        assertEquals(DecodeQualification.UNABLE, plugIn.getDecodeQualification(file));
        assertEquals(DecodeQualification.UNABLE, plugIn.getDecodeQualification(file.getPath()));

        file = new File(dir, "decode_qual_unable_2_missing_logdescr_collection3/V220050513179");
        assertEquals(DecodeQualification.UNABLE, plugIn.getDecodeQualification(file));
        assertEquals(DecodeQualification.UNABLE, plugIn.getDecodeQualification(file.getPath()));
    }

    public static void main(String[] args) throws IOException {
//        final NetcdfFile netcdfFile = SimpleNetcdfFile.openNetcdf("C:\\Users\\Norman\\EOData\\SPOT-VGT\\V2KRNS10__20060721_RADIO_Europe\\0001\\V220050513179_B3.HDF");
//        netcdfFile.writeCDL(System.out, false);

        Product product = ProductIO.readProduct(new File(args[0]));
        System.out.println("product = " + product);
        Band[] bands = product.getBands();
        for (int i = 0; i < bands.length; i++) {
            Band band = bands[i];
            System.out.println("band[" + i + "] = " + band);
        }
        product.closeIO();
    }
}
