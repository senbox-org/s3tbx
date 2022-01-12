package org.esa.s3tbx.dataio.landsat.geotiff.c2;

import org.esa.s3tbx.dataio.landsat.geotiff.AbstractLandsatMetadata;
import org.esa.s3tbx.dataio.landsat.geotiff.Landsat8Metadata;
import org.esa.s3tbx.dataio.landsat.geotiff.LandsatGeotiffReader;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.runtime.Config;

import java.awt.*;
import java.io.IOException;
import java.io.Reader;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;

/**
 * Base class for Landsat Collection 2 products metadata.
 *
 * @author Cosmin Cara
 */
abstract class AbstractLandsatC2Metadata extends AbstractLandsatMetadata {

    protected final Logger logger = Logger.getLogger(Landsat8Metadata.class.getName());
    protected final String sensorId;

    public AbstractLandsatC2Metadata(Reader fileReader) throws IOException {
        super(fileReader);
        sensorId = getImageAttributes().getAttribute("SENSOR_ID").getData().getElemString();
    }

    public AbstractLandsatC2Metadata(MetadataElement root) throws IOException {
        super(root);
        sensorId = root.getAttribute("SENSOR_ID").getData().getElemString();
    }

    protected MetadataElement getProductContents() {
        return getMetaDataElementRoot().getElement("PRODUCT_CONTENTS");
    }

    protected MetadataElement getImageAttributes() {
        return getMetaDataElementRoot().getElement("IMAGE_ATTRIBUTES");
    }

    protected MetadataElement getProjectionAttributes() {
        return getMetaDataElementRoot().getElement("PROJECTION_ATTRIBUTES");
    }

    protected MetadataElement getProcessingRecord() {
        return getMetaDataElementRoot().getElement("LEVEL1_PROCESSING_RECORD");
    }

    @Override
    public Pattern getOpticalBandFileNamePattern() {
        return Pattern.compile("FILE_NAME_BAND_(\\d{1,2})");
    }

    @Override
    public String getQualityBandNameKey() {
        return "FILE_NAME_QUALITY_L1";
    }

    @Override
    public String getAngleSensorAzimuthBandName() {
        final MetadataAttribute attribute = getProductContents().getAttribute("FILE_NAME_ANGLE_SENSOR_AZIMUTH_BAND_4");
        return attribute != null ? attribute.getData().getElemString() : null;
    }

    @Override
    public String getAngleSensorZenithBandName() {
        final MetadataAttribute attribute = getProductContents().getAttribute("FILE_NAME_ANGLE_SENSOR_ZENITH_BAND_4");
        return attribute != null ? attribute.getData().getElemString() : null;
    }

    @Override
    public String getAngleSolarAzimuthBandName() {
        final MetadataAttribute attribute = getProductContents().getAttribute("FILE_NAME_ANGLE_SOLAR_AZIMUTH_BAND_4");
        return attribute != null ? attribute.getData().getElemString() : null;
    }

    @Override
    public String getAngleSolarZenithBandName() {
        final MetadataAttribute attribute = getProductContents().getAttribute("FILE_NAME_ANGLE_SOLAR_ZENITH_BAND_4");
        return attribute != null ? attribute.getData().getElemString() : null;
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
    public MetadataElement getProductMetadata() {
        return getProductContents();
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
    protected Dimension getDimension(String widthAttributeName, String heightAttributeName) {
        MetadataElement metadata = getProjectionAttributes();
        MetadataAttribute widthAttribute = metadata.getAttribute(widthAttributeName);
        MetadataAttribute heightAttribute = metadata.getAttribute(heightAttributeName);
        if (widthAttribute != null && heightAttribute != null) {
            int width = widthAttribute.getData().getElemInt();
            int height = heightAttribute.getData().getElemInt();
            return new Dimension(width, height);
        } else {
            return null;
        }
    }

    @Override
    protected ProductData.UTC getCenterTime(String acquisitionDateKey, String sceneCenterScanTimeKey) {
        MetadataElement metadata = getImageAttributes();
        String dateString = metadata.getAttributeString(acquisitionDateKey);
        String timeString = metadata.getAttributeString(sceneCenterScanTimeKey);

        try {
            if (dateString != null && timeString != null) {
                timeString = timeString.substring(0, 12);
                final DateFormat dateFormat = ProductData.UTC.createDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                final Date date = dateFormat.parse(dateString + " " + timeString);
                String milliSeconds = timeString.substring(timeString.length() - 3);
                return ProductData.UTC.create(date, Long.parseLong(milliSeconds) * 1000);
            }
        } catch (ParseException ignored) {
            // ignore
        }
        return null;
    }

    @Override
    protected String getProductType(String productTypeKey) {
        final MetadataAttribute product_type = getProductMetadata().getAttribute(productTypeKey);
        final MetadataAttribute spacecraft_id = getImageAttributes().getAttribute("SPACECRAFT_ID");
        final MetadataAttribute sensor_id = getImageAttributes().getAttribute("SENSOR_ID");

        return spacecraft_id.getData().getElemString() +
                "_" +
                sensor_id.getData().getElemString() +
                "_" +
                product_type.getData().getElemString();
    }

    protected double getSunAngleCorrectionFactor(String spectralInput) {
        // this follows:
        // http://landsat.usgs.gov/Landsat8_Using_Product.php, section 'Conversion to TOA Reflectance'
        double sunAngleCorrectionFactor = 1.0;
        if (spectralInput.equals("REFLECTANCE")) {
            MetadataElement imageAttributesElement = getImageAttributes();
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

    protected String getSpectralInputString() {
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
                    logger.warning(String.format("Property '%s' has unsupported value '%s'.%n" +
                                                      "Interpreting values as radiance.",
                                                 LandsatGeotiffReader.SYSPROP_READ_AS, readAs));

            }
        }else {
            spectralInput = "RADIANCE";
        }
        return spectralInput;
    }

    protected static int getIndex(String bandIndexNumber) {
        return Integer.parseInt(bandIndexNumber) - 1;
    }
}
