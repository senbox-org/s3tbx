package org.esa.s3tbx.olci.radiometry.rayleigh;

import org.esa.s3tbx.olci.radiometry.Sensor;
import org.esa.snap.core.util.io.CsvReader;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * todo: add comment
 * To change this template use File | Settings | File Templates.
 * Date: 24.07.2017
 * Time: 14:22
 *
 * @author olafd
 */
public class S2ResponseFunctions {

    private static final String S2_RESPONSE_FUNCTION_DEFAULT_FILE_NAME = "s2a_msi_spectral_responses.csv";

    private static final char[] SEPARATOR = new char[]{';'};

    private final List<ResponseFunction> s2ResponseFunctions;
    private String s2ResponseFunctionsFileName;

    public S2ResponseFunctions() {
        this(S2_RESPONSE_FUNCTION_DEFAULT_FILE_NAME);
    }

    public S2ResponseFunctions(String filename) {
        this.s2ResponseFunctionsFileName = filename;
        this.s2ResponseFunctions = loadAuxdata();
    }

    public List<ResponseFunction> getS2ResponseFunctions() {
        return s2ResponseFunctions;
    }

    public int getSpectralResponseFunctionRecords() {
        return s2ResponseFunctions.size();
    }

    private List<ResponseFunction> loadAuxdata() {
//        InputStream inputStream = S2ResponseFunction.class.getResourceAsStream(s2ResponseFunctionsFileName);
//        InputStreamReader streamReader = new InputStreamReader(inputStream);
//        CsvReader csvReader = new CsvReader(streamReader, SEPARATOR);
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

        public double getWvl() {
            return wvl;
        }

        public double[] getRfs() {
            return rfs;
        }

        public double getRf(int bandIndex) {
            return rfs[bandIndex];
        }
    }
}
