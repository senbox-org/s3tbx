package org.esa.s3tbx.olci.harmonisation;

import com.bc.ceres.core.ProgressMonitor;
import com.google.common.primitives.Doubles;
import org.apache.commons.lang.ArrayUtils;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.util.ResourceInstaller;
import org.esa.snap.core.util.SystemUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import smile.neighbor.KDTree;

import java.io.File;
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
                    l1bProduct.getBand("Oa" + String.format("%02d", i) + "_radiance") != null &&
                    l1bProduct.getBand("lambda0_band_" + i) != null &&
                    l1bProduct.getBand("FWHM_band_" + i) != null &&
                    l1bProduct.getBand("solar_flux_band_" + i) != null;
        }

        if (!containsRequiredBands) {
            throw new OperatorException("Input product does not seem to be a fully compatible OLCI L1b product. " +
                    "Bands missing for O2A harminisation retrieval. Please check input.");
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
        final String jsonFilename = "O2_v4_desmile_lut_" + bandIndex + ".json";
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

    static SpectralCharacteristics getSpectralCharacteristics(int orbitNumber, Product modelProduct) {
        final double logOrbitNum = Math.log(orbitNumber * 1.);
        final int numCoefs = 3;
        final int numCameras = 5;
        final int w = modelProduct.getSceneRasterWidth();
        final int h = modelProduct.getSceneRasterHeight();
        final Band[][] cwvlCoefBands = new Band[numCoefs][numCameras];
        final Band[][] fwhmCoefBands = new Band[numCoefs][numCameras];
        for (int i = 0; i < numCoefs; i++) {
            for (int j = 0; j < numCameras; j++) {
                cwvlCoefBands[i][j] = modelProduct.getBand("cwvl_coef_coef" + (i + 1) + "_camera" + (j + 1));
                fwhmCoefBands[i][j] = modelProduct.getBand("fwhm_coef_coef" + (i + 1) + "_camera" + (j + 1));
            }
        }

        final float[][][][] cwvlCoefs = new float[numCoefs][numCameras][h][w];
        final float[][][][] fwhmCoefs = new float[numCoefs][numCameras][h][w];
        for (int i = 0; i < numCoefs; i++) {
            for (int j = 0; j < numCameras; j++) {
                for (int k = 0; k < h; k++) {
                    for (int l = 0; l < w; l++) {
                        final double cwvlCoef = cwvlCoefBands[i][j].getSampleFloat(l, k);
                        cwvlCoefs[i][j][k][l] = (float) (cwvlCoef * Math.pow(logOrbitNum, i));
                        final double fwhmCoef = fwhmCoefBands[i][j].getSampleFloat(l, k);
                        fwhmCoefs[i][j][k][l] = (float) (fwhmCoef * Math.pow(logOrbitNum, i));
                    }
                }
            }
        }

        final float[][][] cwvl = new float[numCameras][h][w];
        final float[][][] fwhm = new float[numCameras][h][w];
        for (int j = 0; j < numCameras; j++) {
            for (int k = 0; k < h; k++) {
                for (int m = 0; m < w; m++) {
                    cwvl[k][j][m] = cwvlCoefs[0][j][k][m] + cwvlCoefs[1][j][k][m] + cwvlCoefs[2][j][k][m];
                    fwhm[k][j][m] = fwhmCoefs[0][j][k][m] + fwhmCoefs[1][j][k][m] + fwhmCoefs[2][j][k][m];
                }
            }
        }

        // transpose and revert sequence
        for (int j = 0; j < numCameras; j++) {
            for (int k = 0; k < h; k++) {
                ArrayUtils.reverse(cwvl[k][j]);  // np.transpose(cwvlCoefs,(1,0,2)[...,::-1]
                ArrayUtils.reverse(fwhm[k][j]);  // np.transpose(cwvlCoefs,(1,0,2)[...,::-1]
            }
        }

        // merge k and m dimensions
        final float[][] cwvl_2D = new float[numCameras][h * w];
        final float[][] fwhm_2D = new float[numCameras][h * w];
        for (int j = 0; j < numCameras; j++) {
            for (int k = 0; k < h; k++) {
                for (int m = 0; m < w; m++) {
                    final int index = k * w + m;
                    cwvl_2D[j][index] = cwvl[j][k][m];  // reshape((5,-1))
                    fwhm_2D[j][index] = fwhm[j][k][m];  // reshape((5,-1))
                }
            }
        }

        return new SpectralCharacteristics(cwvl_2D, fwhm_2D);
    }

    static int getOrbitNumber(Product product) {
        final MetadataElement metadataRoot = product.getMetadataRoot();
        if (metadataRoot != null) {
            final MetadataElement manifestElement = metadataRoot.getElement("Manifest");
            if (manifestElement != null) {
                final MetadataElement metadataSectionElement = manifestElement.getElement("metadataSection");
                if (metadataSectionElement != null) {
                    final MetadataElement orbitReferenceElement = metadataSectionElement.getElement("orbitReference");
                    if (orbitReferenceElement != null) {
                        final MetadataElement orbitNumberElement = orbitReferenceElement.getElement("orbitNumber");
                        if (orbitNumberElement != null) {
                            final MetadataAttribute orbitNumberAttr = orbitNumberElement.getAttribute("orbitNumber");
                            if (orbitNumberAttr != null) {
                                final String s = orbitNumberAttr.getData().getElemString();
                                return Integer.parseInt(s);
                            }
                        }
                    }
                }
            }
        }
        return -1;
    }

    static String getPlatform(Product product) {
        String platform = null;
        final MetadataElement metadataRoot = product.getMetadataRoot();
        if (metadataRoot != null) {
            final MetadataElement manifestElement = metadataRoot.getElement("Manifest");
            if (manifestElement != null) {
                final MetadataElement metadataSectionElement = manifestElement.getElement("metadataSection");
                if (metadataSectionElement != null) {
                    final MetadataElement platformElement = metadataSectionElement.getElement("platform");
                    if (platformElement != null) {
                        final MetadataAttribute nssdcIdentifierAttr = platformElement.getAttribute("nssdcIdentifier");
                        if (nssdcIdentifierAttr != null) {
                            final String nssdcIdentifier = nssdcIdentifierAttr.getData().getElemString();
                            switch (nssdcIdentifier) {
                                case "2016-011A":
                                    platform = "A";
                                    break;
                                case "2018-039A":
                                    platform = "B";
                                    break;
                                default:
                                    final MetadataAttribute numberAttr = platformElement.getAttribute("number");
                                    if (numberAttr != null) {
                                        platform = numberAttr.getData().getElemString();
                                    }
                                    break;
                            }
                        }
                    }
                }
            }
        }

        if (platform == null || (!(platform.equals("A")) && !(platform.equals("B")))) {
            throw new OperatorException("Cannot identify Sentinel platform from product metadata");
        }
        return platform;
    }

    static Product getModelProduct(String platform) throws IOException {
        // original 'olci_<identifier>_temporal_model_O2_bands_20200227.nc4' are differently interpreted by
        // different SNAP 7.x nc readers. Re-exporting as NetCDF4-CF into
        // 'olci_<identifier>_temporal_model_O2_bands_20200227.nc' seems to have solved the problem
        final Path pathModelFile = installAuxdata().resolve("olci_" + platform + "_temporal_model_O2_bands.nc");
        File filePath = pathModelFile.toFile();
        try {
            return ProductIO.readProduct(filePath.getAbsolutePath());
        } catch (IOException e) {
            throw new OperatorException("Cannot open O2 spectral model product '" + filePath.getAbsolutePath() + "'.");
        }
    }

    static double[][] getDwlCorrOffsets(String platform) {
        return platform.equals("A") ?
                OlciHarmonisationConstants.OLCI_A_DWL_CORR_OFFSET :
                OlciHarmonisationConstants.OLCI_B_DWL_CORR_OFFSET;
    }

    static class SpectralCharacteristics {
        float[][] cwvl;
        float[][] fwhm;

        SpectralCharacteristics(float[][] cwvl, float[][] fwhm) {
            this.cwvl = cwvl;
            this.fwhm = fwhm;
        }

        float[][] getCwvl() {
            return cwvl;
        }

        float[][] getFwhm() {
            return fwhm;
        }
    }
}
