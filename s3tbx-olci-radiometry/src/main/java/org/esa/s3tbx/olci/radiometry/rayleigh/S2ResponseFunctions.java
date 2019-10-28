package org.esa.s3tbx.olci.radiometry.rayleigh;

import org.esa.s3tbx.olci.radiometry.Sensor;
import org.esa.snap.core.util.io.CsvReader;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Holder for Sentinel-2 MSI spectral response functions
 *
 * @author olafd
 */
class S2ResponseFunctions {

    //private static final String S2_RESPONSE_FUNCTION_DEFAULT_FILE_NAME = "s2a_msi_spectral_responses.csv";

    // switch to version 3.0,
    // see https://sentinel.esa.int/web/sentinel/user-guides/sentinel-2-msi/
    //                           document-library/-/asset_publisher/Wk0TKajiISaR/content/sentinel-2a-spectral-responses
    private static final String S2_RESPONSE_FUNCTION_DEFAULT_FILE_NAME = "s2a_msi_spectral_responses_v30.csv";

    private static final char[] SEPARATOR = new char[]{';'};

    private final List<ResponseFunction> s2ResponseFunctions;
    private String s2ResponseFunctionsFileName;

    S2ResponseFunctions() {
        this(S2_RESPONSE_FUNCTION_DEFAULT_FILE_NAME);
    }

    S2ResponseFunctions(String filename) {
        this.s2ResponseFunctionsFileName = filename;
        this.s2ResponseFunctions = loadAuxdata();
    }

    List<ResponseFunction> getS2ResponseFunctions() {
        return s2ResponseFunctions;
    }

    int getSpectralResponseFunctionRecords() {
        return s2ResponseFunctions.size();
    }

    private List<ResponseFunction> loadAuxdata() {
        List<String[]> records;
        try {
            Path s2ResponseFunctionsAuxdataPath = RayleighAux.installAuxdata().resolve(s2ResponseFunctionsFileName);
            final FileReader fileReader = new FileReader(s2ResponseFunctionsAuxdataPath.toString());
            final CsvReader csvReader = new CsvReader(fileReader, SEPARATOR);
            csvReader.readLine(); // skip header line
            records = csvReader.readStringRecords();
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not load S2 MSI spectral responses auxdata: ", e);
        }
        List<ResponseFunction> rfList = new ArrayList<>(records.size());
        for (String[] record : records) {
            double wvl = Double.parseDouble(record[0].trim());
            double[] rfs = new double[Sensor.S2_MSI.getNumBands()];
            for (int i = 0; i < Sensor.S2_MSI.getNumBands(); i++) {
                rfs[i] = Double.parseDouble(record[i+1].trim());

            }
            rfList.add(new ResponseFunction(wvl, rfs));
        }
        return rfList;
    }

    static class ResponseFunction {
        private final double wvl;
        private final double[] rfs;

        ResponseFunction(double wvl, double[] rf) {
            this.wvl = wvl;
            this.rfs = rf;
        }

        double getWvl() {
            return wvl;
        }

        double[] getRfs() {
            return rfs;
        }

        double getRf(int bandIndex) {
            return rfs[bandIndex];
        }
    }
}
