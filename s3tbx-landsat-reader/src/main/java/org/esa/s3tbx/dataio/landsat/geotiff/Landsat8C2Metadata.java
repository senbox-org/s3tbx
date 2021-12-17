/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.s3tbx.dataio.landsat.geotiff;

import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.runtime.Config;

import java.awt.Dimension;
import java.io.IOException;
import java.io.Reader;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;

/**
 * @author Thomas Storm
 */
class Landsat8C2Metadata extends AbstractLandsatC2Metadata {
    private static final double DEFAULT_SCALE_FACTOR = 1.0;
    private static final double DEFAULT_OFFSET = 0.0;

    private static final Preferences PREFERENCES = Config.instance("s3tbx").load().preferences();
    private static final Logger LOG = Logger.getLogger(Landsat8Metadata.class.getName());

    private static final String[] BAND_DESCRIPTIONS = {
            "Coastal Aerosol (Operational Land Imager (OLI))",
            "Blue (OLI)",
            "Green (OLI)",
            "Red (OLI)",
            "Near-Infrared (NIR) (OLI)",
            "Short Wavelength Infrared (SWIR) 1 (OLI)",
            "SWIR 2 (OLI)",
            "Panchromatic (OLI)",
            "Cirrus (OLI)",
            "Thermal Infrared Sensor (TIRS) 1",
            "TIRS 2"
    };

    private static final float[] WAVELENGTHS = {
            440,
            480,
            560,
            655,
            865,
            1610,
            2200,
            590,
            1370,
            10895,
            12005
    };
    private static final String[] BAND_NAMES = {
            "coastal_aerosol",
            "blue",
            "green",
            "red",
            "near_infrared",
            "swir_1",
            "swir_2",
            "panchromatic",
            "cirrus",
            "thermal_infrared_(tirs)_1",
            "thermal_infrared_(tirs)_2",
    };

    private static final float[] BANDWIDTHS = {
            20,
            60,
            60,
            30,
            30,
            80,
            180,
            180,
            20,
            590,
            1010
    };

    public Landsat8C2Metadata(Reader fileReader) throws IOException {
        super(fileReader);
    }

    public Landsat8C2Metadata(MetadataElement root) throws IOException {
        super(root);
    }

    @Override
    public MetadataElement getImageAttributes() {
        return getMetaDataElementRoot().getElement("IMAGE_ATTRIBUTES");
    }

    @Override
    public MetadataElement getProductContents() {
        return getMetaDataElementRoot().getElement("PRODUCT_CONTENTS");
    }

    @Override
    public MetadataElement getProjectionAttributes() {
        return getMetaDataElementRoot().getElement("PROJECTION_ATTRIBUTES");
    }

    @Override
    public Dimension getReflectanceDim() {
        return getDimension("REFLECTIVE_SAMPLES", "REFLECTIVE_LINES");
    }

    @Override
    public Dimension getThermalDim() {
        return getDimension("THERMAL_SAMPLES", "THERMAL_LINES");
    }

    @Override
    public Dimension getPanchromaticDim() {
        return getDimension("PANCHROMATIC_SAMPLES", "PANCHROMATIC_LINES");
    }

    @Override
    public String getProductType() {
        return getProductType("PROCESSING_LEVEL");
    }

    @Override
    public double getScalingFactor(String bandId) {
        final String spectralInput = getSpectralInputString();
        String attributeKey = String.format("%s_MULT_BAND_%s", spectralInput, bandId);
        MetadataElement radiometricRescalingElement = getMetaDataElementRoot().getElement("LEVEL1_RADIOMETRIC_RESCALING");
        if (radiometricRescalingElement.getAttribute(attributeKey) == null) {
            return DEFAULT_SCALE_FACTOR;
        }

        final double scalingFactor = radiometricRescalingElement.getAttributeDouble(attributeKey);
        final double sunAngleCorrectionFactor = getSunAngleCorrectionFactor(spectralInput);

        return scalingFactor / sunAngleCorrectionFactor;
    }

    @Override
    public double getScalingOffset(String bandId) {
        final String spectralInput = getSpectralInputString();
        String attributeKey = String.format("%s_ADD_BAND_%s", spectralInput, bandId);
        MetadataElement radiometricRescalingElement = getMetaDataElementRoot().getElement("LEVEL1_RADIOMETRIC_RESCALING");
        if (radiometricRescalingElement.getAttribute(attributeKey) == null) {
            return DEFAULT_OFFSET;
        }

        final double scalingOffset = radiometricRescalingElement.getAttributeDouble(attributeKey);
        final double sunAngleCorrectionFactor = getSunAngleCorrectionFactor(spectralInput);

        return scalingOffset / sunAngleCorrectionFactor;
    }

    @Override
    public ProductData.UTC getCenterTime() {
        return getCenterTime("DATE_ACQUIRED", "SCENE_CENTER_TIME");
    }

    @Override
    public Pattern getOpticalBandFileNamePattern() {
        return Pattern.compile("FILE_NAME_BAND_(\\d{1,2})");
    }

    @Override
    public String getQualityBandNameKey() {
        return "FILE_NAME_QUALITY_L1_PIXEL";
    }

    @Override
    public float getWavelength(String bandIndexNumber) {
        int index = getIndex(bandIndexNumber);
        return WAVELENGTHS[index];
    }

    @Override
    public float getBandwidth(String bandIndexNumber) {
        int index = getIndex(bandIndexNumber);
        return BANDWIDTHS[index];
    }

    @Override
    public String getBandDescription(String bandNumber) {
        int index = getIndex(bandNumber);
        return BAND_DESCRIPTIONS[index];
    }

    @Override
    public String getBandNamePrefix(String bandNumber) {
        int index = getIndex(bandNumber);
        return BAND_NAMES[index];
    }

    private static int getIndex(String bandIndexNumber) {
        return Integer.parseInt(bandIndexNumber) - 1;
    }

    static String getSpectralInputString() {
        final Preferences preferences = Config.instance("s3tbx").load().preferences();
        final String readAs = preferences.get(LandsatGeotiffReader.SYSPROP_READ_AS, null);
        String spectralInput;
        if (readAs != null) {
            switch (readAs.toLowerCase()) {
                case "reflectance":
                    spectralInput = "REFLECTANCE";
                    break;
                case "radiance":
                    spectralInput = "RADIANCE";
                    break;
                default:
                    spectralInput = "RADIANCE";
                    LOG.warning(String.format("Property '%s' has unsupported value '%s'.%n" +
                                    "Interpreting values as radiance.",
                            LandsatGeotiffReader.SYSPROP_READ_AS, readAs));

            }
        }else {
            spectralInput = "RADIANCE";
        }
        return spectralInput;
    }

    private double getSunAngleCorrectionFactor(String spectralInput) {
        // this follows:
        // http://landsat.usgs.gov/Landsat8_Using_Product.php, section 'Conversion to TOA Reflectance'
        double sunAngleCorrectionFactor = 1.0;
        if (spectralInput.equals("REFLECTANCE")) {
            MetadataElement imageAttributesElement = getMetaDataElementRoot().getElement("IMAGE_ATTRIBUTES");
            if (imageAttributesElement != null) {
                final String sunElevationAttributeKey = "SUN_ELEVATION";
                if (imageAttributesElement.getAttribute(sunElevationAttributeKey) != null) {
                    final double sunElevationAngle = imageAttributesElement.getAttributeDouble(sunElevationAttributeKey);
                    sunAngleCorrectionFactor = Math.sin(Math.toRadians(sunElevationAngle));
                }
            }
        }
        return sunAngleCorrectionFactor;
    }
}
