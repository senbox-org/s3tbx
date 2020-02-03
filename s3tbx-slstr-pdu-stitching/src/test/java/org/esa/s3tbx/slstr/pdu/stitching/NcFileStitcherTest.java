package org.esa.s3tbx.slstr.pdu.stitching;

import com.bc.ceres.binding.converters.DateFormatConverter;
import edu.ucar.ral.nujan.netcdf.NhException;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.dataio.netcdf.nc.NFileWriteable;
import org.esa.snap.dataio.netcdf.nc.NVariable;
import org.esa.snap.dataio.netcdf.nc.NWritableFactory;
import org.esa.snap.dataio.netcdf.util.NetcdfFileOpener;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.ArrayByte;
import ucar.ma2.ArrayLong;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertEquals;
import static org.esa.snap.core.util.Debug.*;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.fail;

/**
 * @author Tonio Fincke
 */
public class NcFileStitcherTest {

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
    public void testStitchViscal() throws IOException, PDUStitchingException {
        final String ncFileName = "viscal.nc";
        final ImageSize targetImageSize = new ImageSize("xx", 0, 0, 0, 0);
        final ImageSize[] imageSizes = new ImageSize[3];
        imageSizes[0] = new ImageSize("xx", 0, 0, 0, 0);
        imageSizes[1] = new ImageSize("xx", 0, 0, 0, 0);
        imageSizes[2] = new ImageSize("xx", 0, 0, 0, 0);
        final Date now = Calendar.getInstance().getTime();
        final File[] ncFiles = getNcFiles(ncFileName);

        final File stitchedFile = NcFileStitcher.stitchNcFiles(ncFileName, targetDirectory, now, ncFiles,
                                                               targetImageSize, imageSizes);

        assert (stitchedFile != null);
        assert (stitchedFile.exists());
        assertEquals(ncFileName, stitchedFile.getName());
        netcdfFile = NetcdfFileOpener.open(stitchedFile);
        assertNotNull(netcdfFile);
        final List<Variable> variables = netcdfFile.getVariables();
        assertEquals(40, variables.size());
        assertEquals("ANX_time", variables.get(0).getFullName());
        assertEquals(DataType.STRING, variables.get(0).getDataType());
        assertEquals("calibration_time", variables.get(1).getFullName());
        assertEquals(DataType.STRING, variables.get(1).getDataType());
    }

    @Test
    @Ignore("takes a few seconds")
    public void testStitchMet_tx() throws IOException, PDUStitchingException, InvalidRangeException {
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

        assert (stitchedFile != null);
        assert (stitchedFile.exists());
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
        for (int i = 0; i < 3; i++) {
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

    @Test
    public void testStitchF1_BT_io() throws Exception {
        final String ncFileName = "F1_BT_io.nc";
        final ImageSize targetImageSize = new ImageSize("io", 21687, 450, 6000, 900);
        final ImageSize[] imageSizes = new ImageSize[1];
        imageSizes[0] = new ImageSize("io", 23687, 450, 2000, 900);
        final Date now = Calendar.getInstance().getTime();
        final File[] ncFiles = new File[]{getSecondNcFile(ncFileName)};

        final File stitchedFile = NcFileStitcher.stitchNcFiles(ncFileName, targetDirectory, now, ncFiles,
                                                               targetImageSize, imageSizes);

        assert (stitchedFile != null);
        assert (stitchedFile.exists());
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
        assertEquals(DataType.BYTE, variables.get(3).getDataType());
        assertEquals(true, variables.get(3).isUnsigned());
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
        assertEquals(true, F1_exception_orphan_io_attributes.get(2).isArray());
        final Array F1_exception_orphan_io_values = F1_exception_orphan_io_attributes.get(2).getValues();
        assertEquals(8, F1_exception_orphan_io_values.getSize());
        assertEquals(true, F1_exception_orphan_io_values.isUnsigned());
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
    public void testSetGlobalAttributes() throws IOException {
        final File file = new File(targetDirectory, "something.nc");
        SlstrNFileWritable netcdfWriteable = SlstrNFileWritable.create(file.getAbsolutePath());
        List<Attribute>[] attributeLists = new List[2];
        attributeLists[0] = new ArrayList<>();
        attributeLists[0].add(new Attribute("xyz", "yz"));
        attributeLists[0].add(new Attribute("abc", "23"));
        final ArrayByte someArray = new ArrayByte(new int[]{2});
        someArray.setByte(0, (byte) 5);
        someArray.setByte(0, (byte) 5);
        someArray.setUnsigned(true);
        attributeLists[0].add(new Attribute("def", someArray));
        attributeLists[1] = new ArrayList<>();
        attributeLists[1].add(new Attribute("xyz", "yz"));
        attributeLists[1].add(new Attribute("abc", "44"));
        attributeLists[1].add(new Attribute("defg", someArray));
        final DateFormatConverter globalAttributesDateFormatConverter =
                new DateFormatConverter(new SimpleDateFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ss'Z'"));
        final String someTimeAsString = globalAttributesDateFormatConverter.format(new GregorianCalendar(2013, 6, 7, 15, 32, 52).getTime());
        attributeLists[1].add(new Attribute("creation_time", someTimeAsString));

        final Date now = Calendar.getInstance().getTime();
        final String nowAsString = globalAttributesDateFormatConverter.format(now);
        NcFileStitcher.setGlobalAttributes(netcdfWriteable, attributeLists, targetDirectory.getName(), now);
        netcdfWriteable.create();
        netcdfWriteable.close();
        netcdfFile = NetcdfFileOpener.open(file);
        assertNotNull(netcdfFile);
        final List<Attribute> globalAttributes = netcdfFile.getGlobalAttributes();

        assertEquals(6, globalAttributes.size());
        assertEquals("xyz", globalAttributes.get(0).getFullName());
        assertEquals("yz", globalAttributes.get(0).getStringValue());
        assertEquals("abc_1", globalAttributes.get(1).getFullName());
        assertEquals("44", globalAttributes.get(1).getStringValue());
        assertEquals("abc", globalAttributes.get(2).getFullName());
        assertEquals("23", globalAttributes.get(2).getStringValue());
        assertEquals("def", globalAttributes.get(3).getFullName());
        assertEquals(someArray.toString(), globalAttributes.get(3).getStringValue());
        assertEquals("defg", globalAttributes.get(4).getFullName());
        assertEquals(someArray.toString(), globalAttributes.get(4).getStringValue());
        assertEquals("creation_time", globalAttributes.get(5).getFullName());
        assertEquals(nowAsString, globalAttributes.get(5).getStringValue());
    }

    @Test
    public void testSetDimensions() throws IOException, NhException, PDUStitchingException {
        List<Dimension>[] dimensionLists = new ArrayList[2];
        List<Variable>[] variableLists = new ArrayList[2];

        final File inputFile1 = new File(targetDirectory, "input_1.nc");
        SlstrNFileWritable inputWriteable1 = SlstrNFileWritable.create(inputFile1.getAbsolutePath());
        inputWriteable1.addDimension("rows", 5);
        inputWriteable1.addDimension("columns", 7);
        inputWriteable1.addDimension("the_twilight_zone", 12);
        final SlstrN4Variable abVariable1 = inputWriteable1.addVariable("ab", DataType.BYTE, true, null, "rows columns");
        final SlstrN4Variable cdVariable1 = inputWriteable1.addVariable("cd", DataType.LONG, false, null, "columns the_twilight_zone");
        inputWriteable1.create();
        abVariable1.writeFullyInSections(new ArrayByte(new int[]{5, 7}));
        cdVariable1.writeFullyInSections(new ArrayLong(new int[]{7, 12}));
        inputWriteable1.close();
        final NetcdfFile inputnc1 = NetcdfFileOpener.open(inputFile1);
        assertNotNull(inputnc1);
        dimensionLists[0] = inputnc1.getDimensions();
        variableLists[0] = inputnc1.getVariables();
        inputnc1.close();

        final File inputFile2 = new File(targetDirectory, "input_2.nc");
        final NFileWriteable inputWriteable2 = NWritableFactory.create(inputFile2.getAbsolutePath(), "netcdf4");
        inputWriteable2.addDimension("rows", 5);
        inputWriteable2.addDimension("columns", 7);
        inputWriteable2.addDimension("outer_limits", 25);
        final NVariable abVariable2 = inputWriteable2.addVariable("ab", DataType.BYTE, true, null, "rows columns");
        final NVariable cdVariable2 = inputWriteable2.addVariable("ef", DataType.LONG, false, null, "columns outer_limits");
        inputWriteable2.create();
        abVariable2.writeFully(new ArrayByte(new int[]{5, 7}));
        cdVariable2.writeFully(new ArrayLong(new int[]{7, 25}));
        inputWriteable2.close();
        final NetcdfFile inputnc2 = NetcdfFileOpener.open(inputFile2);
        assertNotNull(inputnc2);
        dimensionLists[1] = inputnc2.getDimensions();
        variableLists[1] = inputnc2.getVariables();
        inputnc2.close();

        final File file = new File(targetDirectory, "something.nc");
        SlstrNFileWritable netcdfWriteable = SlstrNFileWritable.create(file.getAbsolutePath());
        ImageSize targetImageSize = new ImageSize("id", 10, 20, 10, 20);
        NcFileStitcher.setDimensions(netcdfWriteable, dimensionLists, targetImageSize, variableLists);
        netcdfWriteable.create();
        netcdfWriteable.close();
        netcdfFile = NetcdfFileOpener.open(file);
        assertNotNull(netcdfFile);
        final List<Dimension> dimensions = netcdfFile.getDimensions();
        assertEquals(4, dimensions.size());
        Map<String, Integer> expectedDimensions = new HashMap<>();
        expectedDimensions.put("rows", 10);
        expectedDimensions.put("columns", 20);
        expectedDimensions.put("the_twilight_zone", 12);
        expectedDimensions.put("outer_limits", 25);
        for (Dimension dimension : dimensions) {
            assertTrue(expectedDimensions.containsKey(dimension.getFullName()));
            assertEquals(expectedDimensions.get(dimension.getFullName()).intValue(), dimension.getLength());
        }
    }

    @Test
    public void testSetDimensions_VaryingDimensionLengths() throws IOException, NhException, PDUStitchingException {
        List<Dimension>[] dimensionLists = new ArrayList[2];
        List<Variable>[] variableLists = new ArrayList[2];

        final File inputFile1 = new File(targetDirectory, "input_1.nc");
        final NFileWriteable inputWriteable1 = NWritableFactory.create(inputFile1.getAbsolutePath(), "netcdf4");
        inputWriteable1.addDimension("rows", 5);
        inputWriteable1.addDimension("columns", 7);
        inputWriteable1.addDimension("the_twilight_zone", 12);
        final NVariable abVariable1 = inputWriteable1.addVariable("ab", DataType.BYTE, true, null, "rows columns");
        final NVariable cdVariable1 = inputWriteable1.addVariable("cd", DataType.LONG, false, null, "columns the_twilight_zone");
        inputWriteable1.create();
        abVariable1.writeFully(new ArrayByte(new int[]{5, 7}));
        cdVariable1.writeFully(new ArrayLong(new int[]{7, 12}));
        inputWriteable1.close();
        final NetcdfFile inputnc1 = NetcdfFileOpener.open(inputFile1);
        assertNotNull(inputnc1);
        dimensionLists[0] = inputnc1.getDimensions();
        variableLists[0] = inputnc1.getVariables();
        inputnc1.close();

        final File inputFile2 = new File(targetDirectory, "input_2.nc");
        final NFileWriteable inputWriteable2 = NWritableFactory.create(inputFile2.getAbsolutePath(), "netcdf4");
        inputWriteable2.addDimension("rows", 5);
        inputWriteable2.addDimension("columns", 7);
        inputWriteable2.addDimension("the_twilight_zone", 25);
        final NVariable abVariable2 = inputWriteable2.addVariable("ab", DataType.BYTE, true, null, "rows columns");
        final NVariable cdVariable2 = inputWriteable2.addVariable("ef", DataType.LONG, false, null, "columns the_twilight_zone");
        inputWriteable2.create();
        abVariable2.writeFully(new ArrayByte(new int[]{5, 7}));
        cdVariable2.writeFully(new ArrayLong(new int[]{7, 25}));
        inputWriteable2.close();
        final NetcdfFile inputnc2 = NetcdfFileOpener.open(inputFile2);
        assertNotNull(inputnc2);
        dimensionLists[1] = inputnc2.getDimensions();
        variableLists[1] = inputnc2.getVariables();
        inputnc2.close();

        final File file = new File(targetDirectory, "something.nc");
        SlstrNFileWritable netcdfWriteable = SlstrNFileWritable.create(file.getAbsolutePath());
        netcdfWriteable.create();
        ImageSize targetImageSize = new ImageSize("id", 10, 20, 10, 20);
        try {
            NcFileStitcher.setDimensions(netcdfWriteable, dimensionLists, targetImageSize, variableLists);
            fail("Exception expected");
        } catch (Exception e) {
            assertEquals("Dimension the_twilight_zone has different lengths across input files", e.getMessage());
        } finally {
            netcdfWriteable.close();
        }
    }

    @Test
    public void testSetDimensions_VaryingValues() throws IOException, NhException, PDUStitchingException {
        List<Dimension>[] dimensionLists = new ArrayList[2];
        List<Variable>[] variableLists = new ArrayList[2];

        final File inputFile1 = new File(targetDirectory, "input_1.nc");
        final NFileWriteable inputWriteable1 = NWritableFactory.create(inputFile1.getAbsolutePath(), "netcdf4");
        inputWriteable1.addDimension("rows", 5);
        inputWriteable1.addDimension("columns", 7);
        inputWriteable1.addDimension("cd", 12);
        final NVariable abVariable1 = inputWriteable1.addVariable("ab", DataType.BYTE, true, null, "rows columns");
        final NVariable cdVariable1 = inputWriteable1.addVariable("cd", DataType.LONG, false, null, "cd");
        inputWriteable1.create();
        abVariable1.writeFully(new ArrayByte(new int[]{5, 7}));
        cdVariable1.writeFully(new ArrayLong(new int[]{12}));
        inputWriteable1.close();
        final NetcdfFile inputnc1 = NetcdfFileOpener.open(inputFile1);
        assertNotNull(inputnc1);
        dimensionLists[0] = inputnc1.getDimensions();
        variableLists[0] = inputnc1.getVariables();

        final File inputFile2 = new File(targetDirectory, "input_2.nc");
        final NFileWriteable inputWriteable2 = NWritableFactory.create(inputFile2.getAbsolutePath(), "netcdf4");
        inputWriteable2.addDimension("rows", 5);
        inputWriteable2.addDimension("columns", 7);
        inputWriteable2.addDimension("cd", 12);
        final NVariable abVariable2 = inputWriteable2.addVariable("ab", DataType.BYTE, true, null, "rows columns");
        final NVariable cdVariable2 = inputWriteable2.addVariable("cd", DataType.LONG, false, null, "cd");
        inputWriteable2.create();
        abVariable2.writeFully(new ArrayByte(new int[]{5, 7}));
        final ArrayLong values = new ArrayLong(new int[]{12});
        values.setLong(10, 10);
        cdVariable2.writeFully(values);
        inputWriteable2.close();
        final NetcdfFile inputnc2 = NetcdfFileOpener.open(inputFile2);
        assertNotNull(inputnc2);
        dimensionLists[1] = inputnc2.getDimensions();
        variableLists[1] = inputnc2.getVariables();

        final File file = new File(targetDirectory, "something.nc");
        SlstrNFileWritable netcdfWriteable = SlstrNFileWritable.create(file.getAbsolutePath());
        netcdfWriteable.create();
        ImageSize targetImageSize = new ImageSize("id", 10, 20, 10, 20);
        try {
            NcFileStitcher.setDimensions(netcdfWriteable, dimensionLists, targetImageSize, variableLists);
            fail("Exception expected");
        } catch (Exception e) {
            assertEquals("Values for cd are different across input files", e.getMessage());
        } finally {
            inputnc1.close();
            inputnc2.close();
            netcdfWriteable.close();
        }
    }

    @Test
    public void testDetermineDestinationOffsets_f1_BT_in() throws IOException {
        int[][] expected_f1_BT_in_DestinationOffsets = {{0}, {3000000}, {6000000}};

        int[] rowOffsets = new int[] {0, 2000, 4000};
        int[] numberOfRows = new int[]{2000, 2000, 2000};
        int[] sectionSizes_f1_BT_in = new int[]{3000000, 3000000, 3000000};
        int[][] sourceOffsets = new int[][]{{0}, {0}, {0}};

        final int[][] actual_f1_BT_in_DestinationOffsets =
                NcFileStitcher.determineDestinationOffsets(rowOffsets, numberOfRows, sectionSizes_f1_BT_in, sourceOffsets);

        for (int i = 0; i < expected_f1_BT_in_DestinationOffsets.length; i++) {
            assertArrayEquals(expected_f1_BT_in_DestinationOffsets[i], actual_f1_BT_in_DestinationOffsets[i]);
        }
    }

    @Test
    public void testDetermineDestinationOffsets_met_tx() throws IOException {
        int[][] expected_met_tx_DestinationOffsets = {{0, 780000, 1560000, 2340000, 3120000},
                {260000, 1040000, 1820000, 2600000, 3380000}, {520000, 1300000, 2080000, 2860000, 3640000}};
        int[] sectionSizes_met_tx = new int[]{260000, 260000, 260000};
        int[] rowOffsets = new int[] {0, 2000, 4000};
        int[] numberOfRows = new int[]{2000, 2000, 2000};
        int[][] sourceOffsets = new int[][]{{0, 260000, 520000, 780000, 1040000}, {0, 260000, 520000, 780000, 1040000},
                {0, 260000, 520000, 780000, 1040000}};

        final int[][] actual_met_tx_DestinationOffsets =
                NcFileStitcher.determineDestinationOffsets(rowOffsets, numberOfRows, sectionSizes_met_tx, sourceOffsets);

        for (int i = 0; i < expected_met_tx_DestinationOffsets.length; i++) {
            assertArrayEquals(expected_met_tx_DestinationOffsets[i], actual_met_tx_DestinationOffsets[i]);
        }
    }

    @Test
    public void testDetermineDestinationOffsets_S1_quality_an() throws IOException {
        int[][] expectedDestinationOffsets =
                {{0, 3396, 6792, 10188, 13584, 16980, 20376, 23772},
                {1601, 4997, 8393, 11789, 15185, 18581, 21977, 25373},
                {3201, 6597, 9993, 13389, 16785, 20181, 23577, 26973}};
        int[] sectionSizes = new int[]{1601, 1600, 195};
        int[] rowOffsets = new int[] {0, 1601, 3201};
        int[] numberOfRows = new int[]{1601, 1600, 195};
        int[][] sourceOffsets = new int[][]{{0, 1601, 3202, 4803, 6404, 8005, 9606, 11207},
                {0, 1600, 3200, 4800, 6400, 8000, 9600, 11200}, {0, 195, 390, 585, 780, 975, 1170, 1365}};

        final int[][] actualDestinationOffsets =
                NcFileStitcher.determineDestinationOffsets(rowOffsets, numberOfRows, sectionSizes, sourceOffsets);

        for (int i = 0; i < expectedDestinationOffsets.length; i++) {
            assertArrayEquals(expectedDestinationOffsets[i], actualDestinationOffsets[i]);
        }
    }

    @Test
    public void testDetermineDestinationOffsets_differentSectionSizes() throws IOException {
        int[][] expectedDestinationOffsets = {{0}, {2000000}, {6500000}};
        int[] rowOffsets = new int[] {0, 2000, 6500};
        int[] numberOfRows = new int[]{2000, 4500, 1500};
        int[] sectionSizes = new int[]{2000000, 4500000, 1500000};
        int[][] sourceOffsets = new int[][]{{0}, {0}, {0}};

        final int[][] actual_f1_BT_in_DestinationOffsets =
                NcFileStitcher.determineDestinationOffsets(rowOffsets, numberOfRows, sectionSizes, sourceOffsets);

        for (int i = 0; i < expectedDestinationOffsets.length; i++) {
            assertArrayEquals(expectedDestinationOffsets[i], actual_f1_BT_in_DestinationOffsets[i]);
        }
    }

    @Test
    public void testDetermineSourceOffsets() throws IOException {
        final File f1_BT_io_file = getSecondNcFile("F1_BT_io.nc");
        final NetcdfFile f1_BT_io_netcdfFile = NetcdfFileOpener.open(f1_BT_io_file);
        assertNotNull(f1_BT_io_netcdfFile);
        final Variable f1_BT_io_variable = f1_BT_io_netcdfFile.getVariables().get(0);

        assertArrayEquals(new int[]{0}, NcFileStitcher.determineSourceOffsets(1800000, f1_BT_io_variable));

        final File met_tx_file = getFirstNcFile("met_tx.nc");
        final NetcdfFile met_tx_netcdfFile = NetcdfFileOpener.open(met_tx_file);
        assertNotNull(met_tx_netcdfFile);
        final Variable u_wind_tx_variable = met_tx_netcdfFile.getVariables().get(11);

        assertArrayEquals(new int[]{0, 260000, 520000, 780000, 1040000},
                          NcFileStitcher.determineSourceOffsets(260000, u_wind_tx_variable));
    }

    @Test
    public void testDetermineSectionSize() throws IOException {
        final File f1_BT_io_file = getSecondNcFile("F1_BT_io.nc");
        final NetcdfFile f1_BT_io_netcdfFile = NetcdfFileOpener.open(f1_BT_io_file);
        assertNotNull(f1_BT_io_netcdfFile);
        final Variable f1_BT_io_variable = f1_BT_io_netcdfFile.getVariables().get(0);

        assertEquals(1800000, NcFileStitcher.determineSectionSize(0, f1_BT_io_variable));

        final File met_tx_file = getFirstNcFile("met_tx.nc");
        final NetcdfFile met_tx_netcdfFile = NetcdfFileOpener.open(met_tx_file);
        assertNotNull(met_tx_netcdfFile);
        final Variable u_wind_tx_variable = met_tx_netcdfFile.getVariables().get(11);

        assertEquals(260000, NcFileStitcher.determineSectionSize(2, u_wind_tx_variable));
    }

    private static File[] getNcFiles(String fileName) {
        return new File[]{getFirstNcFile(fileName), getSecondNcFile(fileName), getThirdNcFile(fileName)};
    }

    private static File getFirstNcFile(String fileName) {
        return getNcFile(TestUtils.FIRST_FILE_NAME, fileName);
    }

    private static File getSecondNcFile(String fileName) {
        return getNcFile(TestUtils.SECOND_FILE_NAME, fileName);
    }

    private static File getThirdNcFile(String fileName) {
        return getNcFile(TestUtils.THIRD_FILE_NAME, fileName);
    }

    private static File getNcFile(String fileName, String name) {
        final String fullFileName = fileName + "/" + name;
        final URL resource = NcFileStitcherTest.class.getResource(fullFileName);
        return new File(resource.getFile());
    }
}