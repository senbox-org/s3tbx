package org.esa.s3tbx.olci.o2a.harmonisation;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Before;
import org.junit.Test;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.text.ParseException;

import static org.junit.Assert.*;


public class OlciO2aHarmonisationIOTest {

    private Path installAuxdataPath;

    @Before
    public void setUp() throws Exception {
        installAuxdataPath = OlciO2aHarmonisationIO.installAuxdata();
    }

    @Test
    public void testParseJsonFile_desmileLut() throws Exception {
        final Path pathJSON = installAuxdataPath.resolve("O2_desmile_lut_SMALL_TEST.json");
        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject = (JSONObject) jsonParser.parse(new FileReader(pathJSON.toString()));

        // parse JSON file...
        final long L = OlciO2aHarmonisationIO.parseJSONInt(jsonObject, "L");
        final long M = OlciO2aHarmonisationIO.parseJSONInt(jsonObject, "M");
        final long N = OlciO2aHarmonisationIO.parseJSONInt(jsonObject, "N");
        final double[][][] jacobians = OlciO2aHarmonisationIO.parseJSON3DimDoubleArray(jsonObject, "JACO");
        final double[][] X = OlciO2aHarmonisationIO.parseJSON2DimDoubleArray(jsonObject, "X");
        final double[][] Y = OlciO2aHarmonisationIO.parseJSON2DimDoubleArray(jsonObject, "Y");
        final double[] VARI = OlciO2aHarmonisationIO.parseJSON1DimDoubleArray(jsonObject, "VARI");
        final double cbwd = OlciO2aHarmonisationIO.parseJSONDouble(jsonObject, "cbwd");
        final double cwvl = OlciO2aHarmonisationIO.parseJSONDouble(jsonObject, "cwvl");
        final long leafsize = OlciO2aHarmonisationIO.parseJSONInt(jsonObject, "leafsize");
        final String[] sequ = OlciO2aHarmonisationIO.parseJSON1DimStringArray(jsonObject, "sequ");
        final double[] MEAN = OlciO2aHarmonisationIO.parseJSON1DimDoubleArray(jsonObject, "MEAN");

        // test all parsed objects...
        assertJSONParsedObjects(L, M, N, jacobians, X, Y, VARI, cbwd, cwvl, leafsize, sequ, MEAN);

        // test DesmileLut holder...
        DesmileLut lut = new DesmileLut(L, M, N, X, Y, jacobians, MEAN, VARI, cwvl, cbwd, leafsize, sequ);
        assertNotNull(lut);
        assertJSONParsedObjects(lut.getL(), lut.getM(), lut.getN(), lut.getJACO(), lut.getX(), lut.getY(), lut.getVARI(),
                lut.getCbwd(), lut.getCwvl(), lut.getLeafsize(), lut.getSequ(), lut.getMEAN());
    }

    private void assertJSONParsedObjects(long l, long m, long n,
                                         double[][][] jacobians, double[][] x, double[][] y,
                                         double[] VARI, double cbwd, double cwvl, long leafsize,
                                         String[] sequ, double[] MEAN) {
        final long expectedL = 3;
        assertEquals(expectedL, l);

        final long expectedM = 1;
        assertEquals(expectedM, m);

        final long expectedN = 4;
        assertEquals(expectedN, n);

        final double[][][] expectedJacobians = {
                {
                        {
                                -0.197668526954583,
                                0.0,
                                -0.0395649820755298,
                                0.0
                        }
                },
                {
                        {
                                -2.007899810240903138,
                                3.0026280660476688256,
                                4.5873694823399553,
                                5.02119888053260749
                        }
                },
                {
                        {
                                -0.007899810240903138,
                                0.0026280660476688256,
                                1.5873694823399553,
                                0.02119888053260749
                        }
                }
        };
        assertNotNull(jacobians);
        assertEquals(3, jacobians.length);
        assertEquals(1, jacobians[1].length);
        assertEquals(4, jacobians[2][0].length);
        assertArrayEquals(expectedJacobians, jacobians);

        final double[][] expectedX = {
                {
                        -1.6514456476894992,
                        -1.5811388300810618,
                        -0.267609642092725,
                        -1.5491933384829668
                },
                {
                        -5.6514456476894992,
                        -6.5811388300810618,
                        -7.267609642092725,
                        -8.5491933384829668
                },
                {
                        1.6514456476894992,
                        1.5811388300869134,
                        2.088818413372202,
                        1.5491933384829668
                }
        };
        assertNotNull(x);
        assertEquals(3, x.length);
        assertEquals(4, x[1].length);
        assertArrayEquals(expectedX, x);


        final double[][] expectedY = {
                {
                        0.9293298058188657
                },
                {
                        0.9088407606665322
                },
                {
                        1.0582987928525662
                }
        };
        assertNotNull(y);
        assertEquals(3, y.length);
        assertEquals(1, y[1].length);
        assertArrayEquals(expectedY, y);


        final double[] expectedVARI = {
                0.6055300708195136,
                0.09486832980506345,
                0.19662785879946731,
                2.581988897471611
        };
        assertNotNull(VARI);
        assertEquals(4, VARI.length);
        assertArrayEquals(expectedVARI, VARI, 1e-8);


        final double expectedCbwd = 2.65;
        assertEquals(expectedCbwd, cbwd, 1e-8);


        final double expectedCwvl = 761.726;
        assertEquals(expectedCwvl, cwvl, 1e-8);


        final long expectedLeafsize = 4;
        assertEquals(expectedLeafsize, leafsize);


        final String[] expectedSequ = {
                "dwvl,bwd,tra,amf",
                "tra/zero"
        };
        assertNotNull(sequ);
        assertEquals(2, sequ.length);
        assertArrayEquals(expectedSequ, sequ);


        final double[] expectedMEAN = {
                1.5257651681785976e-20,
                2.6499999999997224,
                0.35496667905858464,
                6.0
        };
        assertNotNull(MEAN);
        assertEquals(4, MEAN.length);
        assertArrayEquals(expectedMEAN, MEAN, 1e-8);
    }

    @Test
    public void testInstallAuxdata() throws Exception {
        Path auxPath = OlciO2aHarmonisationIO.installAuxdata();
        assertNotNull(auxPath);

    }

    @Test
    public void testGetDateTimeFromS3SynFilename() throws IOException {
        final String synFileNameA =
                "S3A_SY_1_SYN____20210613T095432_20210613T095732_20220113T121245_0179_073_022_2160_LN2_O_NT____.SEN3.nc";
        assertEquals("20210613 095432", OlciO2aHarmonisationIO.getDateTimeStringFromS3SynFilename(synFileNameA));

        final String synFileNameB =
                "subset_of_S3B_SY_1_SYN____20210613T095432_20210613T095732_20220113T121245_0179_246_345_2160_LN2_O_NT____.SEN3.nc";
        assertEquals("20210613 095432", OlciO2aHarmonisationIO.getDateTimeStringFromS3SynFilename(synFileNameB));
    }

    @Test
    public void testGetCycleAndRelOrbitFromS3SynFilename() throws IOException {
        final String synFileNameA =
                "S3A_SY_1_SYN____20210613T095432_20210613T095732_20220113T121245_0179_073_022_2160_LN2_O_NT____.SEN3.nc";
        assertEquals(73, OlciO2aHarmonisationIO.getCycleFromS3SynFilename(synFileNameA));
        assertEquals(22, OlciO2aHarmonisationIO.getRelOrbitFromS3SynFilename(synFileNameA));

        final String synFileNameB =
                "S3B_SY_1_SYN____20210613T095432_20210613T095732_20220113T121245_0179_246_345_2160_LN2_O_NT____.SEN3.nc";
        assertEquals(246, OlciO2aHarmonisationIO.getCycleFromS3SynFilename(synFileNameB));
        assertEquals(345, OlciO2aHarmonisationIO.getRelOrbitFromS3SynFilename(synFileNameB));

        final String synFileNameB2 =
                "S3B_SY_1_SYN____20210613T095432_20210613T095732_20220113T121245_0179_001_007_2160_LN2_O_NT____.SEN3.nc";
        assertEquals(1, OlciO2aHarmonisationIO.getCycleFromS3SynFilename(synFileNameB2));
        assertEquals(7, OlciO2aHarmonisationIO.getRelOrbitFromS3SynFilename(synFileNameB2));
    }

    @Test
    public void testGetSynAbsoluteOrbitNumber_twoMethods() throws IOException, ParseException {
        // make sure orbit numbers retrieved from cycle/relOrbit or  product datetime are the same (+- 1)
        String synFileNameA =
                "S3A_SY_1_SYN____20180116T090334_20180116T094726_20180117T132354_2632_026_378______MAR_O_NT_002.nc";
        assertEquals(9976, OlciO2aHarmonisationIO.getSynAbsoluteOrbitNumberFromCycleAndRelativeOrbit(synFileNameA));
        assertEquals(9976, OlciO2aHarmonisationIO.getSynAbsoluteOrbitNumberFromDateTime(synFileNameA), 1);

        String synFileNameB =
                "S3B_SY_1_SYN____20210116T095018_20210116T103411_20210117T151304_2633_048_079______MAR_O_NT_002.nc";
        assertEquals(14211, OlciO2aHarmonisationIO.getSynAbsoluteOrbitNumberFromCycleAndRelativeOrbit(synFileNameB));
        assertEquals(14211, OlciO2aHarmonisationIO.getSynAbsoluteOrbitNumberFromDateTime(synFileNameB), 1);
    }

    @Test
    public void testGetSynAbsoluteOrbitNumberFromDateTime_SEN3A() throws IOException, ParseException {
        final int orbit_delta = 1;
        // test results verified with ESOV NG v2.5.3
        String synFileNameA =
                "S3A_SY_1_SYN____20160216T191843_bla.nc";
        assertEquals(1, OlciO2aHarmonisationIO.getSynAbsoluteOrbitNumberFromDateTime(synFileNameA), orbit_delta);

        synFileNameA =
                "S3A_SY_1_SYN____20160224T191459_bla.nc";
        assertEquals(114, OlciO2aHarmonisationIO.getSynAbsoluteOrbitNumberFromDateTime(synFileNameA), orbit_delta);

        synFileNameA =
                "S3A_SY_1_SYN____20160304T015301_bla.nc";
        assertEquals(233, OlciO2aHarmonisationIO.getSynAbsoluteOrbitNumberFromDateTime(synFileNameA), orbit_delta);

        synFileNameA =
                "S3A_SY_1_SYN____20170101T005656_bla.nc";
        assertEquals(4553, OlciO2aHarmonisationIO.getSynAbsoluteOrbitNumberFromDateTime(synFileNameA), orbit_delta);

        synFileNameA =
                "S3A_SY_1_SYN____20170103T215727_bla.nc";
        assertEquals(4594, OlciO2aHarmonisationIO.getSynAbsoluteOrbitNumberFromDateTime(synFileNameA), orbit_delta);

        synFileNameA =
                "S3A_SY_1_SYN____20170116T101314_bla.nc";
        assertEquals(4772, OlciO2aHarmonisationIO.getSynAbsoluteOrbitNumberFromDateTime(synFileNameA), orbit_delta);

        synFileNameA =
                "S3A_SY_1_SYN____20180116T091119_bla.nc";
        assertEquals(9976, OlciO2aHarmonisationIO.getSynAbsoluteOrbitNumberFromDateTime(synFileNameA), orbit_delta);

        synFileNameA =
                "S3A_SY_1_SYN____20190116T094703_bla.nc";
        assertEquals(15181, OlciO2aHarmonisationIO.getSynAbsoluteOrbitNumberFromDateTime(synFileNameA), orbit_delta);

        synFileNameA =
                "S3A_SY_1_SYN____20200116T084452_bla.nc";
        assertEquals(20385, OlciO2aHarmonisationIO.getSynAbsoluteOrbitNumberFromDateTime(synFileNameA), orbit_delta);

        synFileNameA =
                "S3A_SY_1_SYN____20210116T085559_bla.nc";
        assertEquals(25604, OlciO2aHarmonisationIO.getSynAbsoluteOrbitNumberFromDateTime(synFileNameA), orbit_delta);

        synFileNameA =
                "S3A_SY_1_SYN____20220116T093315_bla.nc";
        assertEquals(30809, OlciO2aHarmonisationIO.getSynAbsoluteOrbitNumberFromDateTime(synFileNameA), orbit_delta);
    }

    @Test
    public void testGetSynAbsoluteOrbitNumberFromDateTime_SEN3B() throws IOException, ParseException {
        final int orbit_delta = 1;
        // test results verified with ESOV NG v2.5.3
        String synFileNameB =
                "S3B_SY_1_SYN____20180425T191855_bla.nc";
        assertEquals(1, OlciO2aHarmonisationIO.getSynAbsoluteOrbitNumberFromDateTime(synFileNameB), orbit_delta);

        synFileNameB =
                "S3B_SY_1_SYN____20180502T112446_bla.nc";
        assertEquals(96, OlciO2aHarmonisationIO.getSynAbsoluteOrbitNumberFromDateTime(synFileNameB), orbit_delta);

        synFileNameB =
                "S3B_SY_1_SYN____20180524T130130_bla.nc";
        assertEquals(410, OlciO2aHarmonisationIO.getSynAbsoluteOrbitNumberFromDateTime(synFileNameB), orbit_delta);

        synFileNameB =
                "S3B_SY_1_SYN____20180529T041632_bla.nc";
        assertEquals(476, OlciO2aHarmonisationIO.getSynAbsoluteOrbitNumberFromDateTime(synFileNameB), orbit_delta);

        synFileNameB =
                "S3B_SY_1_SYN____20180530T135732_bla.nc";
        assertEquals(496, OlciO2aHarmonisationIO.getSynAbsoluteOrbitNumberFromDateTime(synFileNameB), orbit_delta);

        synFileNameB =
                "S3B_SY_1_SYN____20180606T023007_bla.nc";
        assertEquals(589, OlciO2aHarmonisationIO.getSynAbsoluteOrbitNumberFromDateTime(synFileNameB), orbit_delta);

        synFileNameB =
                "S3B_SY_1_SYN____20181016T103237_bla.nc";
        assertEquals(2476, OlciO2aHarmonisationIO.getSynAbsoluteOrbitNumberFromDateTime(synFileNameB), orbit_delta);

        synFileNameB =
                "S3B_SY_1_SYN____20181024T102233_bla.nc";
        assertEquals(2590, OlciO2aHarmonisationIO.getSynAbsoluteOrbitNumberFromDateTime(synFileNameB), orbit_delta);

        synFileNameB =
                "S3B_SY_1_SYN____20181109T213240_bla.nc";
        assertEquals(2825, OlciO2aHarmonisationIO.getSynAbsoluteOrbitNumberFromDateTime(synFileNameB), orbit_delta);

        synFileNameB =
                "S3B_SY_1_SYN____20181120T144927_bla.nc";
        assertEquals(2978, OlciO2aHarmonisationIO.getSynAbsoluteOrbitNumberFromDateTime(synFileNameB), orbit_delta);

        synFileNameB =
                "S3B_SY_1_SYN____20181123T104511_bla.nc";
        assertEquals(3018, OlciO2aHarmonisationIO.getSynAbsoluteOrbitNumberFromDateTime(synFileNameB), orbit_delta);

        synFileNameB =
                "S3B_SY_1_SYN____20181211T093534_bla.nc";
        assertEquals(3274, OlciO2aHarmonisationIO.getSynAbsoluteOrbitNumberFromDateTime(synFileNameB), orbit_delta);

        synFileNameB =
                "S3B_SY_1_SYN____20190116T090130_bla.nc";
        assertEquals(3787, OlciO2aHarmonisationIO.getSynAbsoluteOrbitNumberFromDateTime(synFileNameB), orbit_delta);

        synFileNameB =
                "S3B_SY_1_SYN____20200116T093856_bla.nc";
        assertEquals(8992, OlciO2aHarmonisationIO.getSynAbsoluteOrbitNumberFromDateTime(synFileNameB), orbit_delta);

        synFileNameB =
                "S3B_SY_1_SYN____20210116T095018_bla.nc";
        assertEquals(14211, OlciO2aHarmonisationIO.getSynAbsoluteOrbitNumberFromDateTime(synFileNameB), orbit_delta);

        synFileNameB =
                "S3B_SY_1_SYN____20220116T084649_bla.nc";
        assertEquals(19415, OlciO2aHarmonisationIO.getSynAbsoluteOrbitNumberFromDateTime(synFileNameB), orbit_delta);
    }

}