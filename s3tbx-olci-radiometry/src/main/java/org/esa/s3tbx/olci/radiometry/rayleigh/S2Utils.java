package org.esa.s3tbx.olci.radiometry.rayleigh;

import org.esa.s3tbx.olci.radiometry.Sensor;
import org.esa.s3tbx.olci.radiometry.SensorConstants;

import java.util.List;

/**
 * Utility class for S2 MSI Rayleigh Correction (i.e. methods for rescaling)
 *
 * @author olafd
 */
class S2Utils {


    /**
     * Returns spectral band index of given S2 band.
     * Needs to consider the nasty 'B8A' special case.
     *
     * @param bandName - the S2 spectral band nams
     * @return spectralBandIndex
     */
    static int getS2SpectralBandIndex(String bandName) {
        // S2 spectralBandIndex:
        // bands B1..B8: spectralBandIndex = 0..7
        // band B8A: spectralBandIndex = 8
        // bands B9: spectralBandIndex = 9
        // bands B10..B12: 10..12
        if (bandName.endsWith("B8A")) {
            return 8;
        } else {
            // shall work for patterns like B12, rho_ng_B12, rBRR_B9, ...
            final int lastIndexOfB = bandName.lastIndexOf("B");
            if (lastIndexOfB >= 0) {
                final int bandNumber = Integer.parseInt(bandName.substring(lastIndexOfB + 1, bandName.length()));
                return bandNumber < 9 ? bandNumber - 1 : bandNumber;
            } else {
                return -1;
            }
        }
    }

    /**
     * Returns S2 target band name for given band category and source band name.
     * Needs to consider the nasty 'B8A' special case.
     *
     * @param bandCategory - bandCategory, e.g. "rBRR_%02d"
     * @param bandName   - the S2 source band name
     * @return targetBandName
     */
    static String getS2TargetBandName(String bandCategory, String bandName) {
        // bandCategory e.g. "rBRR_%02d"
        final String bandCategoryPrefix = bandCategory.substring(0, bandCategory.length() - 4); // e.g. "rBRR_"

        if (bandName.equals("B8A")) {
            return bandCategoryPrefix + "B8A";
        } else {
            final int spectralBandIndex = getS2SpectralBandIndex(bandName);
            if (spectralBandIndex < 9) {
                return bandCategoryPrefix + "B" + (spectralBandIndex + 1);
            } else {
                return bandCategoryPrefix + "B" + spectralBandIndex;
            }
        }
    }

    /**
     * Checks if a S2 target band name matches a given pattern
     * Needs to consider the nasty 'B8A' special case.
     *
     * @param targetBandName - the target band name
     * @param pattern        - the pattern
     * @return boolean
     */
    static boolean targetS2BandNameMatches(String targetBandName, String pattern) {
        // pattern e.g. "rtoa_\\d{2}"
        String s2Pattern;
        if (targetBandName.indexOf("_B") == targetBandName.length() - 3) {
            s2Pattern = pattern.replace("\\d{2}", "B\\d{1}");  // e.g. rBRR_B7
        } else {
            s2Pattern = pattern.replace("\\d{2}", "B\\d{2}");  // e.g. rBRR_B12
        }
        final String patternPrefix = pattern.substring(0, pattern.length() - 5); // e.g. "rtoa_"
        if (targetBandName.endsWith("8A")) {
            return targetBandName.equals(patternPrefix + "B8A");
        } else {
            return targetBandName.matches(s2Pattern);
        }
    }

    /**
     * Returns the number of selected source bands to be Rayleigh corrected.
     *
     * @param sourceBandNames - the selected source band names
     *
     * @return number of bands to be Rayleigh corrected
     */
    static int getNumBandsToRcCorrect(String[] sourceBandNames) {
        int numBandsToRcCorrect = 0;
        for (String sourceBandName : sourceBandNames) {
            for (String bandToRcCorrect : SensorConstants.S2_MSI_SPECTRAL_BAND_NAMES) {
                if (sourceBandName.equals(bandToRcCorrect)) {
                    numBandsToRcCorrect++;
                    break;
                }
            }
        }
        return numBandsToRcCorrect;
    }

    /**
     * This method provides the S2 'true' instead of central wavelengths, considering the spectral response functions.
     * The approach follows the CB email from 20170718
     *
     * @return double[] s2TrueWavelengths
     */
    static double[] getS2TrueWavelengths() {
        double[] s2TrueWvls = new double[Sensor.S2_MSI.getNumBands()];

        // the response functions are from https://earth.esa.int/documents/247904/685211/Sentinel-2+MSI+Spectral+Responses/
        final S2ResponseFunctions s2ResponseFunctionAuxdata = new S2ResponseFunctions();
        List<S2ResponseFunctions.ResponseFunction> s2RfList = s2ResponseFunctionAuxdata.getS2ResponseFunctions();

        double wvlPower = 4.05;
        double incr = 1.0;  // wvl step in nm
        for (int i = 0; i < s2TrueWvls.length; i++) {
            s2TrueWvls[i] = 0.0;
            double sum = 0.0;
            double sumRf = 0.0;

            // the integrals over the RFs per band are not normalized to 1 - we have to do this first
            for (int j = 0; j < s2RfList.size(); j++) {
                final double rf = s2RfList.get(j).getRf(i);
                if (rf > 0.0) {
                    sumRf += (rf *incr);
                }
            }
            for (int j = 0; j < s2RfList.size(); j++) {
                final double wvl = s2RfList.get(j).getWvl();
                final double rf = s2RfList.get(j).getRf(i) / sumRf;
                if (rf > 0.0) {
                    sum += (rf *incr * Math.pow(wvl, -wvlPower));
                }
            }
            s2TrueWvls[i] = Math.pow(sum, -1./wvlPower);
        }

        return s2TrueWvls;
    }

}
