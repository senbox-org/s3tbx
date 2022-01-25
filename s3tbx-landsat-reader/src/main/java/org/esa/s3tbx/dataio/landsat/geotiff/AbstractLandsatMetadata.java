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

import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.ProductData;

import java.awt.Dimension;
import java.io.IOException;
import java.io.Reader;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

/**
 * @author Thomas Storm
 */
public abstract class AbstractLandsatMetadata implements LandsatMetadata {
    protected static final double DEFAULT_SCALE_FACTOR = 1.0;
    protected static final double DEFAULT_OFFSET = 0.0;

    private final MetadataElement root;

    public AbstractLandsatMetadata(Reader fileReader) throws IOException {
        root = OdlParser.parse(fileReader).getElementAt(0);
    }

    public AbstractLandsatMetadata(MetadataElement root) {
        this.root = root;
    }

    @Override
    public MetadataElement getMetaDataElementRoot() {
        return root;
    }

    protected Dimension getDimension(String widthAttributeName, String heightAttributeName) {
        MetadataElement metadata = getProductMetadata();
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

    protected ProductData.UTC getCenterTime(String acquisitionDateKey, String sceneCenterScanTimeKey) {
        MetadataElement metadata = getProductMetadata();
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

    protected String getProductType(String productTypeKey) {
        final MetadataAttribute product_type = getProductMetadata().getAttribute(productTypeKey);
        final MetadataAttribute spacecraft_id = getProductMetadata().getAttribute("SPACECRAFT_ID");
        final MetadataAttribute sensor_id = getProductMetadata().getAttribute("SENSOR_ID");

        return String.format("%s_%s_%s",
                             spacecraft_id.getData().getElemString(),
                             sensor_id.getData().getElemString(),
                             product_type.getData().getElemString());
    }

    protected double getScalingFactor(String bandId, String minMaxRadianceKey, String minRadianceBandPrefix, String maxRadianceBandPrefix, String minMaxPixelValueKey, String minPixelValuePrefix, String maxPixelValuePrefix) {
        try {
            MetadataElement minMaxRadiance = getMetaDataElementRoot().getElement(minMaxRadianceKey);
            double lMax = minMaxRadiance.getAttributeDouble(maxRadianceBandPrefix + bandId);
            double lMin = minMaxRadiance.getAttributeDouble(minRadianceBandPrefix + bandId);

            MetadataElement minMaxPixels = getMetaDataElementRoot().getElement(minMaxPixelValueKey);
            double qMin = minMaxPixels.getAttributeDouble(minPixelValuePrefix + bandId);
            double qMax = minMaxPixels.getAttributeDouble(maxPixelValuePrefix + bandId);

            return (lMax - lMin) / (qMax - qMin);
        } catch (Exception e) {
            return 1;
        }
    }

    protected double getScalingOffset(String bandId, String minMaxRadianceKey, String minRadianceBandPrefix, String maxRadianceBandPrefix, String minMaxPixelValueKey, String minPixelValuePrefix, String maxPixelValuePrefix) {
        try {
            MetadataElement minMaxRadiance = getMetaDataElementRoot().getElement(minMaxRadianceKey);
            double lMax = minMaxRadiance.getAttributeDouble(maxRadianceBandPrefix + bandId);
            double lMin = minMaxRadiance.getAttributeDouble(minRadianceBandPrefix + bandId);

            MetadataElement minMaxPixels = getMetaDataElementRoot().getElement(minMaxPixelValueKey);
            double qMax = minMaxPixels.getAttributeDouble(maxPixelValuePrefix + bandId);
            double qMin = minMaxPixels.getAttributeDouble(minPixelValuePrefix + bandId);

            return lMin - ((lMax - lMin) / (qMax - qMin)) * qMin;
        } catch (Exception e) {
            return 0;
        }
    }
}
