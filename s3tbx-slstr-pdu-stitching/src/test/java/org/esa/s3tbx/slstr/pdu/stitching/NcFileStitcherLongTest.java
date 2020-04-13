package org.esa.s3tbx.slstr.pdu.stitching;

import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.dataio.netcdf.util.NetcdfFileOpener;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.esa.s3tbx.slstr.pdu.stitching.NcFileStitcherTest.getNcFiles;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Tonio Fincke
 */
//@RunWith(LongTestRunner.class)
public class NcFileStitcherLongTest {

    private File targetDirectory;
    private NetcdfFile netcdfFile;

    @Before
    public void setUp() {
        targetDirectory = new File("test_out");
        if (!targetDirectory.mkdirs()) {
            fail("Unable to create test target directory");
        }
        netcdfFile = null;
    }

    @After
    public void tearDown() throws IOException {
        if (netcdfFile != null) {
            netcdfFile.close();
        }
        if (targetDirectory.isDirectory()) {
            if (!FileUtils.deleteTree(targetDirectory)) {
                fail("Unable to delete test directory");
            }
        }
    }

    @Test
    public void testStitchF1_BT_io() throws Exception {
        final String ncFileName = "F1_BT_io.nc";
        final ImageSize targetImageSize = new ImageSize("io", 21687, 450, 6000, 900);
        final ImageSize[] imageSizes = new ImageSize[1];
        imageSizes[0] = new ImageSize("io", 23687, 450, 2000, 900);
        final Date now = Calendar.getInstance().getTime();
        final File[] ncFiles = new File[]{NcFileStitcherTest.getSecondNcFile(ncFileName)};

        final File stitchedFile = NcFileStitcher.stitchNcFiles(ncFileName, targetDirectory, now, ncFiles,
                                                               targetImageSize, imageSizes);

        assertNotNull(stitchedFile);
        assertTrue(stitchedFile.exists());
        assertEquals(ncFileName, stitchedFile.getName());
        netcdfFile = NetcdfFileOpener.open(stitchedFile);
        assertNotNull(netcdfFile);
        final List<Variable> variables = netcdfFile.getVariables();
        assertEquals(4, variables.size());
        assertEquals("F1_BT_io", variables.get(0).getFullName());
        assertEquals(DataType.SHORT, variables.get(0).getDataType());
        assertEquals("rows columns", variables.get(0).getDimensionsString());
        final List<Attribute> F1_BT_io_attributes = variables.get(0).getAttributes();
        assertEquals("_ChunkSize", F1_BT_io_attributes.get(0).getFullName());
        Array chunkLengths = F1_BT_io_attributes.get(0).getValues();
        assertEquals(1, chunkLengths.getShape().length);
        assertEquals(2, chunkLengths.getShape()[0]);
        assertEquals(600, chunkLengths.getInt(0));
        assertEquals(450, chunkLengths.getInt(1));
        assertEquals("standard_name", F1_BT_io_attributes.get(1).getFullName());
        assertEquals("toa_brightness_temperature", F1_BT_io_attributes.get(1).getStringValue());
        assertEquals("long_name", F1_BT_io_attributes.get(2).getFullName());
        assertEquals("Gridded pixel brightness temperature for channel F1 (1km TIR grid, oblique view)",
                     F1_BT_io_attributes.get(2).getStringValue());
        assertEquals("units", F1_BT_io_attributes.get(3).getFullName());
        assertEquals("K", F1_BT_io_attributes.get(3).getStringValue());
        assertEquals("_FillValue", F1_BT_io_attributes.get(4).getFullName());
        assertEquals((short) -32768, F1_BT_io_attributes.get(4).getNumericValue());
        assertEquals("scale_factor", F1_BT_io_attributes.get(5).getFullName());
        assertEquals(0.01, F1_BT_io_attributes.get(5).getNumericValue());
        assertEquals("add_offset", F1_BT_io_attributes.get(6).getFullName());
        assertEquals(283.73, F1_BT_io_attributes.get(6).getNumericValue());

        assertEquals("F1_exception_io", variables.get(1).getFullName());
        assertEquals("F1_BT_orphan_io", variables.get(2).getFullName());

        assertEquals("F1_exception_orphan_io", variables.get(3).getFullName());
        assertEquals(DataType.UBYTE, variables.get(3).getDataType());
        assertTrue(variables.get(3).getDataType().isUnsigned());
        assertEquals("rows orphan_pixels", variables.get(3).getDimensionsString());
        final List<Attribute> F1_exception_orphan_io_attributes = variables.get(3).getAttributes();
        assertEquals("_ChunkSize", F1_exception_orphan_io_attributes.get(0).getFullName());
        chunkLengths = F1_exception_orphan_io_attributes.get(0).getValues();
        assertEquals(1, chunkLengths.getShape().length);
        assertEquals(2, chunkLengths.getShape()[0]);
        assertEquals(600, chunkLengths.getInt(0));
        assertEquals(112, chunkLengths.getInt(1));
        assertEquals("standard_name", F1_exception_orphan_io_attributes.get(1).getFullName());
        assertEquals("toa_brightness_temperature_status_flag", F1_exception_orphan_io_attributes.get(1).getStringValue());
        assertEquals("flag_masks", F1_exception_orphan_io_attributes.get(2).getFullName());
        assertTrue(F1_exception_orphan_io_attributes.get(2).isArray());
        final Array F1_exception_orphan_io_values = F1_exception_orphan_io_attributes.get(2).getValues();
        assertEquals(8, F1_exception_orphan_io_values.getSize());
        assertTrue(F1_exception_orphan_io_values.isUnsigned());
        assertEquals((byte) 1, F1_exception_orphan_io_values.getByte(0));
        assertEquals((byte) 2, F1_exception_orphan_io_values.getByte(1));
        assertEquals((byte) 4, F1_exception_orphan_io_values.getByte(2));
        assertEquals((byte) 8, F1_exception_orphan_io_values.getByte(3));
        assertEquals((byte) 16, F1_exception_orphan_io_values.getByte(4));
        assertEquals((byte) 32, F1_exception_orphan_io_values.getByte(5));
        assertEquals((byte) 64, F1_exception_orphan_io_values.getByte(6));
        assertEquals((byte) 128, F1_exception_orphan_io_values.getByte(7));
        assertEquals("flag_meanings", F1_exception_orphan_io_attributes.get(3).getFullName());
        assertEquals("ISP_absent pixel_absent not_decompressed no_signal saturation invalid_radiance no_parameters unfilled_pixel",
                     F1_exception_orphan_io_attributes.get(3).getStringValue());
        assertEquals("_FillValue", F1_exception_orphan_io_attributes.get(4).getFullName());
        assertEquals((byte) -128, F1_exception_orphan_io_attributes.get(4).getNumericValue());

        List<Variable> inputFileVariables = new ArrayList<>();
        for (int i = 0; i < 1; i++) {
            final NetcdfFile ncfile = NetcdfFileOpener.open(ncFiles[i]);
            assertNotNull(ncfile);
            inputFileVariables = ncfile.getVariables();
        }
        for (int i = 0; i < variables.size(); i++) {
            Variable variable = variables.get(i);
            int offset = 2000;
            final Section section = new Section(new int[]{offset, 0}, new int[]{offset, variable.getDimension(1).getLength()});
            final Array stitchedArrayPart = variable.read(section);
            final Array fileArray = inputFileVariables.get(i).read();
            if (variable.getDataType() == DataType.SHORT) {
                final short[] expectedArray = (short[]) fileArray.copyTo1DJavaArray();
                final short[] actualArray = (short[]) stitchedArrayPart.copyTo1DJavaArray();
                assertArrayEquals(expectedArray, actualArray);
            } else {
                final byte[] expectedArray = (byte[]) fileArray.copyTo1DJavaArray();
                final byte[] actualArray = (byte[]) stitchedArrayPart.copyTo1DJavaArray();
                assertArrayEquals(expectedArray, actualArray);
            }
        }
    }

    @Test
    public void testStitchMet_tx() throws IOException, PDUStitchingException, InvalidRangeException, URISyntaxException {
        final String ncFileName = "met_tx.nc";
        final ImageSize targetImageSize = new ImageSize("in", 21687, 64, 6000, 130);
        final ImageSize[] imageSizes = new ImageSize[3];
        imageSizes[0] = new ImageSize("in", 21687, 64, 2000, 130);
        imageSizes[1] = new ImageSize("in", 23687, 64, 2000, 130);
        imageSizes[2] = new ImageSize("in", 25687, 64, 2000, 130);
        final Date now = Calendar.getInstance().getTime();
        final File[] ncFiles = getNcFiles(ncFileName);

        final File stitchedFile = NcFileStitcher.stitchNcFiles(ncFileName, targetDirectory, now, ncFiles,
                                                               targetImageSize, imageSizes);

        assertNotNull(stitchedFile);
        assertTrue(stitchedFile.exists());
        assertEquals(ncFileName, stitchedFile.getName());
        netcdfFile = NetcdfFileOpener.open(stitchedFile);
        assertNotNull(netcdfFile);
        final List<Variable> variables = netcdfFile.getVariables();
        assertEquals(28, variables.size());
        assertEquals("t_single", variables.get(0).getFullName());
        assertEquals(DataType.SHORT, variables.get(0).getDataType());
        assertEquals("sea_ice_fraction_tx", variables.get(9).getFullName());
        assertEquals(DataType.FLOAT, variables.get(9).getDataType());
        assertEquals("u_wind_tx", variables.get(10).getFullName());
        assertEquals(DataType.FLOAT, variables.get(10).getDataType());
        assertEquals("snow_depth_tx", variables.get(27).getFullName());
        assertEquals(DataType.FLOAT, variables.get(27).getDataType());
        List<Attribute> snowLiquidAttributes = variables.get(27).getAttributes();
        assertEquals("_ChunkSize", snowLiquidAttributes.get(0).getFullName());
        assertEquals("1 600 130 ", snowLiquidAttributes.get(0).getValues().toString());
        assertEquals("long_name", snowLiquidAttributes.get(1).getFullName());
        assertEquals("Snow liquid water equivalent depth", snowLiquidAttributes.get(1).getStringValue());
        assertEquals("standard_name", snowLiquidAttributes.get(2).getFullName());
        assertEquals("lwe_thickness_of_surface_snow_amount", snowLiquidAttributes.get(2).getStringValue());
        assertEquals("units", snowLiquidAttributes.get(3).getFullName());
        assertEquals("metre", snowLiquidAttributes.get(3).getStringValue());
        assertEquals("model", snowLiquidAttributes.get(4).getFullName());
        assertEquals("ECMWF_F", snowLiquidAttributes.get(4).getStringValue());
        assertEquals("parameter", snowLiquidAttributes.get(5).getFullName());
        assertEquals("141", snowLiquidAttributes.get(5).getStringValue());

        final List<Variable>[] inputFileVariables = new ArrayList[3];
        for (int i = 0; i < inputFileVariables.length; i++) {
            final NetcdfFile ncFile = NetcdfFileOpener.open(ncFiles[i]);
            assertNotNull(ncFile);
            inputFileVariables[i] = ncFile.getVariables();
        }
        for (int i = 0; i < variables.size(); i++) {
            Variable variable = variables.get(i);
            for (int j = 0; j < inputFileVariables.length; j++) {
                int rowOffset = j * 2000;
                final Variable inputFileVariable = inputFileVariables[j].get(i + 1);

                final List<Dimension> inputFileVariableDimensions = inputFileVariable.getDimensions();
                int[] origin = new int[inputFileVariableDimensions.size()];
                int[] shape = new int[inputFileVariableDimensions.size()];
                for (int k = 0; k < inputFileVariableDimensions.size(); k++) {
                    if (inputFileVariableDimensions.get(k).getFullName().equals("rows")) {
                        origin[k] = rowOffset;
                        shape[k] = 2000;
                    } else {
                        origin[k] = 0;
                        shape[k] = inputFileVariableDimensions.get(k).getLength();
                    }
                }
                final Section section = new Section(origin, shape);

                final Array stitchedArrayPart = variable.read(section);
                final Array fileArray = inputFileVariable.read();
                if (variable.getDataType() == DataType.SHORT) {
                    final short[] expectedArray = (short[]) fileArray.copyTo1DJavaArray();
                    final short[] actualArray = (short[]) stitchedArrayPart.copyTo1DJavaArray();
                    assertArrayEquals(expectedArray, actualArray);
                } else {
                    final float[] expectedArray = (float[]) fileArray.copyTo1DJavaArray();
                    final float[] actualArray = (float[]) stitchedArrayPart.copyTo1DJavaArray();
                    assertArrayEquals(expectedArray, actualArray, 1e-8f);
                }
            }
        }
    }





}