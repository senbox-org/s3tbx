/*
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

package org.esa.s3tbx.fu;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import static org.esa.s3tbx.fu.Instrument.Constants.CZCS_BAND_NAME_PATTERN;
import static org.esa.s3tbx.fu.Instrument.Constants.CZCS_POLYFACTORS;
import static org.esa.s3tbx.fu.Instrument.Constants.CZCS_VALID_EXPRESSIONS;
import static org.esa.s3tbx.fu.Instrument.Constants.CZCS_WAVELENGTHS;
import static org.esa.s3tbx.fu.Instrument.Constants.CZCS_XFACTORS;
import static org.esa.s3tbx.fu.Instrument.Constants.CZCS_YFACTORS;
import static org.esa.s3tbx.fu.Instrument.Constants.CZCS_ZFACTORS;
import static org.esa.s3tbx.fu.Instrument.Constants.MERIS_BAND_NAME_PATTERN;
import static org.esa.s3tbx.fu.Instrument.Constants.MERIS_POLYFACTORS;
import static org.esa.s3tbx.fu.Instrument.Constants.MERIS_VALID_EXPRESSIONS;
import static org.esa.s3tbx.fu.Instrument.Constants.MERIS_WAVELENGTHS;
import static org.esa.s3tbx.fu.Instrument.Constants.MERIS_XFACTORS;
import static org.esa.s3tbx.fu.Instrument.Constants.MERIS_YFACTORS;
import static org.esa.s3tbx.fu.Instrument.Constants.MERIS_ZFACTORS;
import static org.esa.s3tbx.fu.Instrument.Constants.MODIS500_BAND_NAME_PATTERN;
import static org.esa.s3tbx.fu.Instrument.Constants.MODIS500_POLYFACTORS;
import static org.esa.s3tbx.fu.Instrument.Constants.MODIS500_VALID_EXPRESSIONS;
import static org.esa.s3tbx.fu.Instrument.Constants.MODIS500_WAVELENGTHS;
import static org.esa.s3tbx.fu.Instrument.Constants.MODIS500_XFACTORS;
import static org.esa.s3tbx.fu.Instrument.Constants.MODIS500_YFACTORS;
import static org.esa.s3tbx.fu.Instrument.Constants.MODIS500_ZFACTORS;
import static org.esa.s3tbx.fu.Instrument.Constants.MODIS_BAND_NAME_PATTERN;
import static org.esa.s3tbx.fu.Instrument.Constants.MODIS_POLYFACTORS;
import static org.esa.s3tbx.fu.Instrument.Constants.MODIS_VALID_EXPRESSIONS;
import static org.esa.s3tbx.fu.Instrument.Constants.MODIS_WAVELENGTHS;
import static org.esa.s3tbx.fu.Instrument.Constants.MODIS_XFACTORS;
import static org.esa.s3tbx.fu.Instrument.Constants.MODIS_YFACTORS;
import static org.esa.s3tbx.fu.Instrument.Constants.MODIS_ZFACTORS;
import static org.esa.s3tbx.fu.Instrument.Constants.MSI_BAND_NAME_PATTERN;
import static org.esa.s3tbx.fu.Instrument.Constants.MSI_VALID_EXPRESSIONS;
import static org.esa.s3tbx.fu.Instrument.Constants.MSI_WAVELENGTHS;
import static org.esa.s3tbx.fu.Instrument.Constants.MSI_XFACTORS;
import static org.esa.s3tbx.fu.Instrument.Constants.MSI_YFACTORS;
import static org.esa.s3tbx.fu.Instrument.Constants.MSI_ZFACTORS;
import static org.esa.s3tbx.fu.Instrument.Constants.OLCI_BAND_NAME_PATTERN;
import static org.esa.s3tbx.fu.Instrument.Constants.OLCI_POLYFACTORS;
import static org.esa.s3tbx.fu.Instrument.Constants.OLCI_VALID_EXPRESSIONS;
import static org.esa.s3tbx.fu.Instrument.Constants.OLCI_WAVELENGTHS;
import static org.esa.s3tbx.fu.Instrument.Constants.OLCI_XFACTORS;
import static org.esa.s3tbx.fu.Instrument.Constants.OLCI_YFACTORS;
import static org.esa.s3tbx.fu.Instrument.Constants.OLCI_ZFACTORS;
import static org.esa.s3tbx.fu.Instrument.Constants.S2A_MSI_POLYFACTORS;
import static org.esa.s3tbx.fu.Instrument.Constants.S2B_MSI_POLYFACTORS;
import static org.esa.s3tbx.fu.Instrument.Constants.SEAWIFS_BAND_NAME_PATTERN;
import static org.esa.s3tbx.fu.Instrument.Constants.SEAWIFS_POLYFACTORS;
import static org.esa.s3tbx.fu.Instrument.Constants.SEAWIFS_VALID_EXPRESSIONS;
import static org.esa.s3tbx.fu.Instrument.Constants.SEAWIFS_WAVELENGTHS;
import static org.esa.s3tbx.fu.Instrument.Constants.SEAWIFS_XFACTORS;
import static org.esa.s3tbx.fu.Instrument.Constants.SEAWIFS_YFACTORS;
import static org.esa.s3tbx.fu.Instrument.Constants.SEAWIFS_ZFACTORS;

// todo (mp/20170720) - next refactoring should be to extract Instrument objects and init the enum with the objects. Both might implement the same interface.
enum Instrument {
    AUTO_DETECT(new double[0], new String[0], new double[0], new double[0], new double[0], new double[0], new String[0], false),
    OLCI(OLCI_WAVELENGTHS, OLCI_BAND_NAME_PATTERN, OLCI_XFACTORS, OLCI_YFACTORS, OLCI_ZFACTORS, OLCI_POLYFACTORS, OLCI_VALID_EXPRESSIONS, false),
    S2A_MSI(MSI_WAVELENGTHS, MSI_BAND_NAME_PATTERN, MSI_XFACTORS, MSI_YFACTORS, MSI_ZFACTORS, S2A_MSI_POLYFACTORS, MSI_VALID_EXPRESSIONS, true),
    S2B_MSI(MSI_WAVELENGTHS, MSI_BAND_NAME_PATTERN, MSI_XFACTORS, MSI_YFACTORS, MSI_ZFACTORS, S2B_MSI_POLYFACTORS, MSI_VALID_EXPRESSIONS, true),
    MERIS(MERIS_WAVELENGTHS, MERIS_BAND_NAME_PATTERN, MERIS_XFACTORS, MERIS_YFACTORS, MERIS_ZFACTORS, MERIS_POLYFACTORS, MERIS_VALID_EXPRESSIONS,
          true),
    MODIS(MODIS_WAVELENGTHS, MODIS_BAND_NAME_PATTERN, MODIS_XFACTORS, MODIS_YFACTORS, MODIS_ZFACTORS, MODIS_POLYFACTORS, MODIS_VALID_EXPRESSIONS,
          false),
    MODIS500(MODIS500_WAVELENGTHS, MODIS500_XFACTORS, MODIS500_YFACTORS, MODIS500_ZFACTORS, MODIS500_POLYFACTORS, MODIS500_VALID_EXPRESSIONS,
             new ModisReflectancesSelector(MODIS500_BAND_NAME_PATTERN), (p, nodes, spectrum) -> spectrum, false),
    SEAWIFS(SEAWIFS_WAVELENGTHS, SEAWIFS_BAND_NAME_PATTERN, SEAWIFS_XFACTORS, SEAWIFS_YFACTORS, SEAWIFS_ZFACTORS, SEAWIFS_POLYFACTORS,
            SEAWIFS_VALID_EXPRESSIONS, false),
    CZCS(CZCS_WAVELENGTHS, CZCS_BAND_NAME_PATTERN, CZCS_XFACTORS, CZCS_YFACTORS, CZCS_ZFACTORS, CZCS_POLYFACTORS, CZCS_VALID_EXPRESSIONS, false);
    // TODO - DISABLED SENSOR
//    LANDSAT8(LANDSAT8_WAVELENGTHS, LANDSAT8_XFACTORS, LANDSAT8_YFACTORS, LANDSAT8_ZFACTORS, LANDSAT8_POLYFACTORS, LANDSAT8_VALID_EXPRESSIONS,
//             LANDSAT_8_PREPROCESS, true);


    private final double[] wavelengths;
    private final double[] xFactors;
    private final double[] yFactors;
    private final double[] zFactors;
    private final double[] polynomCoefficients;
    private final String[] validExpressions;
    private final boolean isIrradiance;
    private final ReflectancesSelector reflectanceSelector;
    private final PreProcess preProcess;

    Instrument(double[] wavelengths, String[] bandNamePattern, double[] xFactors, double[] yFactors, double[] zFactors, double[] polynomCoefficients,
               String[] validExpressions,
               boolean isIrradiance) {
        this(wavelengths, xFactors, yFactors, zFactors, polynomCoefficients, validExpressions,
             new DefaultReflectancesSelector(wavelengths, bandNamePattern), (p, nodes, spectrum) -> spectrum, isIrradiance);
    }

    Instrument(double[] wavelengths, double[] xFactors, double[] yFactors, double[] zFactors, double[] polynomCoefficients, String[] validExpressions,
               ReflectancesSelector reflSelector, PreProcess preProcess, boolean isIrradiance) {
        this.wavelengths = wavelengths;
        this.xFactors = xFactors;
        this.yFactors = yFactors;
        this.zFactors = zFactors;
        this.reflectanceSelector = reflSelector;
        this.preProcess = preProcess;
        this.polynomCoefficients = polynomCoefficients;
        this.validExpressions = validExpressions;
        this.isIrradiance = isIrradiance;
    }

    public String[] getValidExpressions() {
        return validExpressions;
    }

    public double[] getWavelengths() {
        return wavelengths;
    }

    public boolean isIrradiance() {
        return isIrradiance;
    }

    public double[] getXFactors() {
        return xFactors;
    }

    public double[] getYFactors() {
        return yFactors;
    }

    public double[] getZFactors() {
        return zFactors;
    }

    public double[] getPolynomCoefficients() {
        return polynomCoefficients;
    }

    public double[] preProcess(Product sourceProduct, RasterDataNode[] sourceNodes, double[] spectrum) {
        return preProcess.process(sourceProduct, sourceNodes, spectrum);
    }

    public String[] getReflectanceBandNames(Product sourceProduct, String userBandNameRegex) {
        String[] selected = reflectanceSelector.select(sourceProduct, userBandNameRegex);
        final int bandNum = selected.length;
        if (bandNum != getWavelengths().length) {
            throw new IllegalStateException("Could not find all necessary wavelengths for processing the instrument " + name() + ".");
        }
        return selected;
    }

    private interface PreProcess {

        double[] process(Product p, RasterDataNode[] nodes, double[] spectrum);
    }

    private interface ReflectancesSelector {

        String FALLBACK_PATTERN = ".*";

        String[] select(Product product, String userBandNameRegex);
    }

    static class Constants {

        // MERIS Constants
        static final double[] MERIS_WAVELENGTHS = {412.691, 442.55902, 489.88202, 509.81903, 559.69403, 619.601, 664.57306, 680.82104, 708.32904};
        static final String[] MERIS_BAND_NAME_PATTERN = new String[]{"reflectance_\\d{1,2}"};
        static final String[] MERIS_VALID_EXPRESSIONS = new String[]{
                "l2_flags.WATER",
                "not l1p_flags.CC_LAND and not l1p_flags.CC_CLOUD",
                "NOT l1_flags.LAND_OCEAN"
        };
        static final double[] MERIS_XFACTORS = new double[]{2.957, 10.861, 3.744, 3.750, 34.687, 41.853, 7.619, 0.844, 0.189};
        static final double[] MERIS_YFACTORS = new double[]{0.112, 1.711, 5.672, 23.263, 48.791, 23.949, 2.944, 0.307, 0.068};
        static final double[] MERIS_ZFACTORS = new double[]{14.354, 58.356, 28.227, 4.022, 0.618, 0.026, 0.000, 0.000, 0.000};
        static final double[] MERIS_POLYFACTORS = new double[]{-12.0506, 88.9325, -244.6960, 305.2361, -164.6960, 28.5255};
        // OLCI Constants
        static final double[] OLCI_WAVELENGTHS = {400.0, 412.5, 442.5, 490.0, 510.0, 560.0, 620.0, 665.0, 673.75, 681.25, 708.75};
        static final String[] OLCI_BAND_NAME_PATTERN = {"Oa\\d{2}_reflectance"};
        static final String[] OLCI_VALID_EXPRESSIONS = {"WQSF_lsb.WATER and not WQSF_lsb.CLOUD", "LQSF.WATER"};
        static final double[] OLCI_XFACTORS = new double[]{0.154, 2.957, 10.861, 3.744, 3.750, 34.687, 41.853, 7.323, 0.591, 0.549, 0.189};
        static final double[] OLCI_YFACTORS = new double[]{0.004, 0.112, 1.711, 5.672, 23.263, 48.791, 23.949, 2.836, 0.216, 0.199, 0.068};
        static final double[] OLCI_ZFACTORS = new double[]{0.731, 14.354, 58.356, 28.227, 4.022, 0.618, 0.026, 0.000, 0.000, 0.000, 0.000};
        static final double[] OLCI_POLYFACTORS = new double[]{-12.5076, 91.6345, -249.848, 308.6561, -165.4818, 28.5608};
        // MSI Constants
        static final double[] MSI_WAVELENGTHS = {443, 490, 560, 665, 705};
        static final String[] MSI_BAND_NAME_PATTERN = {"B\\d"};
        static final String[] MSI_VALID_EXPRESSIONS = {};
        static final double[] MSI_XFACTORS = new double[]{11.756, 6.423, 53.696, 32.028, 0.529};
        static final double[] MSI_YFACTORS = new double[]{1.744, 22.289, 65.702, 16.808, 0.192};
        static final double[] MSI_ZFACTORS = new double[]{62.696, 31.101, 1.778, 0.015, 0.000};
        static final double[] S2A_MSI_POLYFACTORS = new double[]{-68.76, 495.18, -1315.60, 1547.60, -748.36, 113.25};
        static final double[] S2B_MSI_POLYFACTORS = new double[]{-70.78, 510.49, -1360.3, 1608.6, -785.63, 121.34};
        // MODIS Constants
        static final double[] MODIS_WAVELENGTHS = {412.0, 443.0, 488.0, 531.0, 555.0, 667.0, 678.0};
        static final String[] MODIS_BAND_NAME_PATTERN = {"rrs_\\d{3}"};
        static final String[] MODIS_VALID_EXPRESSIONS = {"not l2_flags.LAND and not l2_flags.CLDICE"};
        static final double[] MODIS_XFACTORS = new double[]{2.957, 10.861, 4.031, 3.989, 49.037, 34.586, 0.829};
        static final double[] MODIS_YFACTORS = new double[]{0.112, 1.711, 11.106, 22.579, 51.477, 19.452, 0.301};
        static final double[] MODIS_ZFACTORS = new double[]{14.354, 58.356, 29.993, 2.618, 0.262, 0.000, 0.000};
        static final double[] MODIS_POLYFACTORS = new double[]{-48.0880, 362.6179, -1011.7151, 1262.0348, -666.5981, 113.9215};
        // SEAWIFS Constants
        static final double[] SEAWIFS_WAVELENGTHS = {412.0, 443.0, 490.0, 510.0, 555.0, 670.0};
        static final String[] SEAWIFS_BAND_NAME_PATTERN = {"Rrs_\\d{3}"};
        static final String[] SEAWIFS_VALID_EXPRESSIONS = {"not l2_flags.LAND and not l2_flags.CLDICE"};
        static final double[] SEAWIFS_XFACTORS = new double[]{2.957, 10.861, 3.744, 3.455, 52.304, 32.825};
        static final double[] SEAWIFS_YFACTORS = new double[]{0.112, 1.711, 5.672, 21.929, 59.454, 17.810};
        static final double[] SEAWIFS_ZFACTORS = new double[]{14.354, 58.356, 28.227, 3.967, 0.682, 0.018};
        static final double[] SEAWIFS_POLYFACTORS = new double[]{-49.4377, 363.2770, -978.1648, 1154.6030, -552.2701, 78.2940};
        // CZCS Constants
        static final double[] CZCS_WAVELENGTHS = {443.0, 520.0, 550.0, 670.0};
        static final String[] CZCS_BAND_NAME_PATTERN = {"Rrs_\\d{3}"};
        static final String[] CZCS_VALID_EXPRESSIONS = {"not l2_flags.LAND and not l2_flags.CLDICE"};
        static final double[] CZCS_XFACTORS = new double[]{13.237, 5.195, 50.856, 34.797};
        static final double[] CZCS_YFACTORS = new double[]{4.825, 25.217, 56.997, 19.571};
        static final double[] CZCS_ZFACTORS = new double[]{74.083, 21.023, 0.462, 0.022};
        static final double[] CZCS_POLYFACTORS = new double[]{-65.9452, 510.3687, -1475.8008, 1927.6141, -1078.6236, 202.2455};
        // MODIS 500m Constants
        static final double[] MODIS500_WAVELENGTHS = {466.0, 553.0, 647.0};
        static final String[] MODIS500_BAND_NAME_PATTERN = {"sur_refl_b0[134]"};
        static final String[] MODIS500_VALID_EXPRESSIONS = {}; 
        static final double[] MODIS500_XFACTORS = new double[]{13.3280, 46.3789, 40.2774};
        static final double[] MODIS500_YFACTORS = new double[]{15.756, 67.793, 22.459};
        static final double[] MODIS500_ZFACTORS = new double[]{73.374, 6.111, 0.024};
        static final double[] MODIS500_POLYFACTORS = new double[]{-68.3622, 534.0367, -1552.7614, 2042.4187, -1156.9981, 223.0369};
        // TODO - DISABLED SENSOR
        // Landsat-8 Constants
//        static final double[] LANDSAT8_WAVELENGTHS = {440.0, 480.0, 560.0, 655};
//        static final String[] LANDSAT8_VALID_EXPRESSIONS = {"water_confidence_mid || water_confidence_high"};
//        static final double[] LANDSAT8_XFACTORS = new double[]{11.053, 6.950, 51.135, 34.457};
//        static final double[] LANDSAT8_YFACTORS = new double[]{1.320, 21.053, 66.023, 18.034};
//        static final double[] LANDSAT8_ZFACTORS = new double[]{58.038, 34.931, 2.606, 0.016};
//        static final double[] LANDSAT8_POLYFACTORS = new double[]{-52.1571, 373.8083, -981.8317, 1134.1947, -533.6077, 76.7203};
//        public static final PreProcess LANDSAT_8_PREPROCESS = new L8PreProcess();
//
//        private static class L8PreProcess implements PreProcess {
//
//            Map<Product, double[][]> convMap = new WeakHashMap<>();
//
//            @Override
//            public double[] process(Product p, RasterDataNode[] nodes, double[] spectrum) {
//                String description = p.getBandAt(0).getDescription();
//                boolean isToaRefl = description != null && description.contains("TOA Reflectance");
//                if (isToaRefl) {
//                    return spectrum;
//                } else {
//                    double[] reflectance_offsets;
//                    double[] reflectance_scalings;
//                    if(!convMap.containsKey(p)) {
//                        reflectance_offsets = getConverValues(p, nodes, "REFLECTANCE_ADD_BAND_%d");
//                        reflectance_scalings = getConverValues(p, nodes, "REFLECTANCE_MULT_BAND_%d");
//                        convMap.put(p, new double[][]{reflectance_offsets, reflectance_scalings});
//                    }else {
//                        double[][] convValues = convMap.get(p);
//                        reflectance_offsets = convValues[0];
//                        reflectance_scalings = convValues[1];
//                    }
//                    for (int i = 0; i < spectrum.length; i++) {
//                        double radianceValue = spectrum[i];
//                        RasterDataNode dataNode = nodes[i];
//                        double radianceOffset = dataNode.getScalingOffset();
//                        double radianceScaling = dataNode.getScalingFactor();
//                        spectrum[i] = toReflectances(radianceValue, radianceOffset, radianceScaling,
//                                       reflectance_offsets[i], reflectance_scalings[i]);
//                    }
//                }
//                return spectrum;
//            }
//
//            private double[] getConverValues(Product p, RasterDataNode[] nodes, String attributeNamePattern) {
//                MetadataElement l1MetadataFile = p.getMetadataRoot().getElement("L1_METADATA_FILE");
//                MetadataElement imageAttributes = l1MetadataFile.getElement("IMAGE_ATTRIBUTES");
//                double sunElevation = imageAttributes.getAttribute("SUN_ELEVATION").getData().getElemDouble();
//                double sunAngleCorrectionFactor = Math.sin(Math.toRadians(sunElevation));
//
//                MetadataElement radiometricRescaling = l1MetadataFile.getElement("RADIOMETRIC_RESCALING");
//                double[] convValues = new double[nodes.length];
//                for (int i = 0; i < nodes.length; i++) {
//                    // this follows:
//                    // http://landsat.usgs.gov/Landsat8_Using_Product.php, section 'Conversion to TOA Reflectance'
//                    // also see org.esa.s3tbx.dataio.landsat.geotiff.Landsat8Metadata#getSunAngleCorrectionFactor
//                    int index = p.getBandIndex(nodes[i].getName());
//                    double convValue = radiometricRescaling.getAttributeDouble(String.format(attributeNamePattern, index + 1));
//                    convValues[i] = convValue / sunAngleCorrectionFactor;
//                }
//                return convValues;
//            }
//
//            private double toReflectances(double source, double radiance_offset, double radiance_scale,
//                                          double reflectance_offset, double reflectance_scale) {
//                double count = (source - radiance_offset) / radiance_scale;
//                return count * reflectance_scale + reflectance_offset;
//            }
//
//        }
    }

    static class DefaultReflectancesSelector implements ReflectancesSelector {

        private static final int MAX_DELTA_WAVELENGTH = 3;
        private final double[] wavelengths;
        private final String[] defaultNameRegex;


        public DefaultReflectancesSelector(double[] wavelengths, String[] defaultNameRegex) {
            this.wavelengths = wavelengths;
            this.defaultNameRegex = defaultNameRegex;
        }

        @Override
        public String[] select(Product product, String userBandNameRegex) {
            final Band[] bands = product.getBands();
            final ArrayList<String> band_Names = new ArrayList<>();
            List<String> regularexpressions = new ArrayList<>();
            if (StringUtils.isNotNullAndNotEmpty(userBandNameRegex)) { // use it only if set
                regularexpressions.add(userBandNameRegex);
            }
            regularexpressions.addAll(Arrays.asList(defaultNameRegex));
            regularexpressions.add(FALLBACK_PATTERN); // Fallback - Matches any name
            for (String regex : regularexpressions) {
                Pattern pattern = Pattern.compile(regex);
                for (double centralWl : wavelengths) {
                    String name = null;
                    double minDelta = Double.MAX_VALUE;
                    for (Band band : bands) {
                        if (!pattern.matcher(band.getName()).matches()) {
                            continue; // skip this band if it doesn't match
                        }
                        double bandWavelength = band.getSpectralWavelength();
                        if (bandWavelength > 0.0) {
                            double delta = Math.abs(bandWavelength - centralWl);
                            if (delta < minDelta && delta <= MAX_DELTA_WAVELENGTH) {
                                name = band.getName();
                                minDelta = delta;
                            }
                        }
                    }
                    if (name != null) {
                        band_Names.add(name);
                    }
                }
                if (band_Names.size() == wavelengths.length) { // found all necessary bands
                    return band_Names.toArray(new String[0]);
                }
                band_Names.clear();
            }
            return band_Names.toArray(new String[0]);
        }
    }

    private static class ModisReflectancesSelector implements ReflectancesSelector {

        private final String[] defaultBandNamePattern;

        public ModisReflectancesSelector(String[] modis500BandNamePattern) {
            defaultBandNamePattern = modis500BandNamePattern;
        }

        @Override
        public String[] select(Product product, String userBandNameRegex) {
            List<String> regularexpressions = new ArrayList<>();
            if (StringUtils.isNotNullAndNotEmpty(userBandNameRegex)) { // use it only if set
                regularexpressions.add(userBandNameRegex);
            }
            regularexpressions.addAll(Arrays.asList(defaultBandNamePattern));
            regularexpressions.add(FALLBACK_PATTERN); // Fallback - Matches any name
            final Band[] bands = product.getBands();
            List<String> potentialBandNames = new ArrayList<>();
            for (String regex : regularexpressions) {
                Pattern pattern = Pattern.compile(regex);
                for (Band band : bands) {
                    if (!pattern.matcher(band.getName()).matches()) {
                        continue; // skip this band if it doesn't match
                    }
                    potentialBandNames.add(band.getName());
                }
            }


            /// That's ugly
            String band3 = getNameWithSuffix(potentialBandNames, "3");
            String band4 = getNameWithSuffix(potentialBandNames, "4");
            String band1 = getNameWithSuffix(potentialBandNames, "1");
            List<String> selected = new ArrayList<>();
            if(band3 != null){
                selected.add(band3);
            }
            if(band4 != null){
                selected.add(band4);
            }
            if(band1 != null){
                selected.add(band1);
            }
            return selected.toArray(new String[0]);
        }

        private String getNameWithSuffix(List<String> potentialBandNames, String suffix) {
            for (String potentialBandName : potentialBandNames) {
                if(potentialBandName.endsWith(suffix)) {
                    return potentialBandName;
                }
            }
            return null;
        }

    }
}
