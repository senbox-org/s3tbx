package org.esa.s3tbx.olci.harmonisation;

import com.bc.ceres.core.ProgressMonitor;
import com.google.common.primitives.Doubles;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.util.ResourceInstaller;
import org.esa.snap.core.util.SystemUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import smile.neighbor.KDTree;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Class providing I/O related methods for OLCI O2A Harmonisation.
 *
 * @author olafd
 */
class OlciHarmonisationIO {

    /**
     * Validates the OLCI L1b source product.
     *
     * @param l1bProduct - the L1b product
     */
    static void validateSourceProduct(Product l1bProduct) {
        boolean containsRequiredBands = l1bProduct.getBand("detector_index") != null &&
                l1bProduct.getBand("altitude") != null;
        for (int i = 1; i <= 21; i++) {
            containsRequiredBands = containsRequiredBands &&
                    l1bProduct.getBand("Oa" + String.format("%02d" , i) + "_radiance") != null &&
                    l1bProduct.getBand("lambda0_band_" + i) != null &&
                    l1bProduct.getBand("FWHM_band_" + i) != null &&
                    l1bProduct.getBand("solar_flux_band_" + i) != null;
        }

        if (!containsRequiredBands) {
            throw new OperatorException("Input product does not seem to be a fully compatible OLCI L1b product. " +
                                                "Bands missing for O2A harmonisation retrieval. Please check input.");
        }
    }

    /**
     * extracts a double variable from a JSON object
     *
     * @param jsonObject   - the JSON object
     * @param variableName - the variable name
     * @return double
     */
    static double parseJSONDouble(JSONObject jsonObject, String variableName) {
        return (double) jsonObject.get(variableName);
    }

    /**
     * extracts an int variable from a JSON object
     *
     * @param jsonObject   - the JSON object
     * @param variableName - the variable name
     * @return int
     */
    static long parseJSONInt(JSONObject jsonObject, String variableName) {
        return (Long) jsonObject.get(variableName);
    }

    /**
     * extracts a one-dimensional double array variable from a JSON object
     *
     * @param jsonObject   - the JSON object
     * @param variableName - the variable name
     * @return double[]
     */
    static double[] parseJSON1DimDoubleArray(JSONObject jsonObject, String variableName) {
        JSONArray jsonArray = (JSONArray) jsonObject.get(variableName);
        List<Double> doubleList = (List<Double>) jsonArray.stream().collect(Collectors.toList());
        return Doubles.toArray(doubleList);
    }

    /**
     * extracts a one-dimensional String array variable from a JSON object
     *
     * @param jsonObject   - the JSON object
     * @param variableName - the variable name
     * @return String[]
     */
    static String[] parseJSON1DimStringArray(JSONObject jsonObject, String variableName) {
        JSONArray jsonArray = (JSONArray) jsonObject.get(variableName);
        List<String> stringList = (List<String>) jsonArray.stream().collect(Collectors.toList());
        return stringList.toArray(new String[stringList.size()]);
    }

    /**
     * extracts a two-dimensional double array variable from a JSON object
     *
     * @param jsonObject   - the JSON object
     * @param variableName - the variable name
     * @return double[][]
     */
    static double[][] parseJSON2DimDoubleArray(JSONObject jsonObject, String variableName) {
        final JSONArray jsonArray1 = (JSONArray) jsonObject.get(variableName);

        final int dim1 = jsonArray1.size();
        final int dim2 = ((JSONArray) jsonArray1.get(0)).size();

        JSONArray[] jsonArray2 = new JSONArray[dim1];

        double[][] doubleArr = new double[dim1][dim2];

        for (int i = 0; i < dim1; i++) {
            jsonArray2[i] = (JSONArray) jsonArray1.get(i);
            for (int j = 0; j < dim2; j++) {
                doubleArr[i][j] = (Double) jsonArray2[i].get(j);
            }
        }

        return doubleArr;
    }

    /**
     * extracts a three-dimensional double array variable from a JSON object
     *
     * @param jsonObject   - the JSON object
     * @param variableName - the variable name
     * @return double[][][]
     */
    static double[][][] parseJSON3DimDoubleArray(JSONObject jsonObject, String variableName) {
        final JSONArray jsonArray1 = (JSONArray) jsonObject.get(variableName);

        final int dim1 = jsonArray1.size();
        final int dim2 = ((JSONArray) jsonArray1.get(0)).size();
        final int dim3 = ((JSONArray) ((JSONArray) jsonArray1.get(0)).get(0)).size();

        JSONArray[] jsonArray2 = new JSONArray[dim1];
        JSONArray[][] jsonArray3 = new JSONArray[dim1][dim2];

        double[][][] doubleArr = new double[dim1][dim2][dim3];

        for (int i = 0; i < dim1; i++) {
            jsonArray2[i] = (JSONArray) jsonArray1.get(i);
            for (int j = 0; j < dim2; j++) {
                jsonArray3[i][j] = (JSONArray) jsonArray2[i].get(j);
                for (int k = 0; k < dim3; k++) {
                    doubleArr[i][j][k] = (Double) jsonArray3[i][j].get(k);
                }
            }
        }

        return doubleArr;
    }

    /**
     * Creates a KDTree object from a given {@link DesmileLut} lookup table object.
     *
     * @param desmileLut - the lookup table for desmiling
     * @return the KDTree object
     */
    static KDTree<double[]> createKDTreeForDesmileInterpolation(DesmileLut desmileLut) {
        return new KDTree<>(desmileLut.getX(), desmileLut.getX());
    }

    /**
     * Creates a {@link DesmileLut} lookup table object for given band
     *
     * @param auxdataPath - the path where the auxdata was installed before
     * @param bandIndex   - the band index
     * @return the DesmileLut object
     * @throws IOException    -
     * @throws ParseException -
     */
    static DesmileLut createDesmileLut(Path auxdataPath, int bandIndex) throws IOException, ParseException {
        final String jsonFilename = "O2_desmile_lut_" + bandIndex + ".json";
        final Path jsonPath = auxdataPath.resolve(jsonFilename);
        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject = (JSONObject) jsonParser.parse(new FileReader(jsonPath.toString()));

        // parse JSON file...
        final long L = OlciHarmonisationIO.parseJSONInt(jsonObject, "L");
        final long M = OlciHarmonisationIO.parseJSONInt(jsonObject, "M");
        final long N = OlciHarmonisationIO.parseJSONInt(jsonObject, "N");
        final double[][][] jacobians = OlciHarmonisationIO.parseJSON3DimDoubleArray(jsonObject, "JACO");
        final double[][] X = OlciHarmonisationIO.parseJSON2DimDoubleArray(jsonObject, "X");
        final double[][] Y = OlciHarmonisationIO.parseJSON2DimDoubleArray(jsonObject, "Y");
        final double[] VARI = OlciHarmonisationIO.parseJSON1DimDoubleArray(jsonObject, "VARI");
        final double cbwd = OlciHarmonisationIO.parseJSONDouble(jsonObject, "cbwd");
        final double cwvl = OlciHarmonisationIO.parseJSONDouble(jsonObject, "cwvl");
        final long leafsize = OlciHarmonisationIO.parseJSONInt(jsonObject, "leafsize");
        final String[] sequ = OlciHarmonisationIO.parseJSON1DimStringArray(jsonObject, "sequ");
        final double[] MEAN = OlciHarmonisationIO.parseJSON1DimDoubleArray(jsonObject, "MEAN");

        return new DesmileLut(L, M, N, X, Y, jacobians, MEAN, VARI, cwvl, cbwd, leafsize, sequ);
    }

    /**
     * Installs auxiliary data (i.e. lookup tables for desmiling).
     *
     * @return - the auxdata path for Harmonisation
     * @throws IOException -
     */
    static Path installAuxdata() throws IOException {
        Path auxdataDirectory = SystemUtils.getAuxDataPath().resolve("harmonisation");
        final Path sourceDirPath = ResourceInstaller.findModuleCodeBasePath(OlciO2aHarmonisationOp.class).resolve("auxdata/luts");
        final ResourceInstaller resourceInstaller = new ResourceInstaller(sourceDirPath, auxdataDirectory);
        resourceInstaller.install(".*", ProgressMonitor.NULL);
        return auxdataDirectory;
    }

}
