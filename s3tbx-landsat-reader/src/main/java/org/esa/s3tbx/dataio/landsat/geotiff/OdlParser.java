/*
 *
 * Copyright (c) 2022.  Brockmann Consult GmbH (info@brockmann-consult.de)
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
 *
 */

package org.esa.s3tbx.dataio.landsat.geotiff;

import org.esa.snap.core.dataio.IllegalFileFormatException;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.ProductData;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

/**
 * This parser turns Landsat ODL (Object Description Language) files ()  into metadata elements and metadata attributes.
 *
 * @author Sabine Embacher
 */
public class OdlParser {

    public static MetadataElement parse(File mtlFile) throws IOException {
        try (final FileReader reader = new FileReader(mtlFile)) {
            return parse(reader);
        }
    }

    public static MetadataElement parse(Reader mtlReader) throws IOException {
        MetadataElement base = new MetadataElement("base");
        MetadataElement currentElement = base;
        BufferedReader reader = new BufferedReader(mtlReader);
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("GROUP")) {
                    int i = line.indexOf('=');
                    String groupName = line.substring(i + 1).trim();
                    MetadataElement element = new MetadataElement(groupName);
                    currentElement.addElement(element);
                    currentElement = element;
                } else if (line.startsWith("END_GROUP") && currentElement != null) {
                    currentElement = currentElement.getParentElement();
                } else if (line.equals("END")) {
                    return base;
                } else if (currentElement != null) {
                    MetadataAttribute attribute = createAttribute(line, reader);
                    currentElement.addAttribute(attribute);
                }
            }
        } finally {
            reader.close();
        }
        return base;
    }

    private static MetadataAttribute createAttribute(String line, BufferedReader reader) throws IOException {
        int i = line.indexOf('=');
        String name = line.substring(0, i).trim();
        String value = line.substring(i + 1).trim();
        ProductData pData;
        if (value.startsWith("\"")) {
            value = value.substring(1, value.length() - 1);
            pData = ProductData.createInstance(value);
        } else if (value.startsWith("(")) {
            pData = createArrayData(value, reader);
        } else if (value.contains(".")) {
            try {
                double d = Double.parseDouble(value);
                pData = ProductData.createInstance(new double[]{d});
            } catch (NumberFormatException e) {
                pData = ProductData.createInstance(value);
            }
        } else {
            try {
                int integer = Integer.parseInt(value);
                pData = ProductData.createInstance(new int[]{integer});
            } catch (NumberFormatException e) {
                pData = ProductData.createInstance(value);
            }
        }
        return new MetadataAttribute(name, pData, true);
    }

    private static ProductData createArrayData(String value, BufferedReader reader) throws IOException {
        while (value.indexOf(")") == -1) {
            value += reader.readLine().trim();
        }
        final long openCount = value.chars().filter(ch -> ch == '(').count();
        final long closeCount = value.chars().filter(ch -> ch == ')').count();
        if (openCount != closeCount || openCount > 1 || closeCount > 2) {
            throw new IllegalFileFormatException("The number of opening and closing brackets of an attribute value within an ODL file must be equal to and less than 2.");
        }
        final int openIndex = value.indexOf("(");
        final int closeIndex = value.indexOf(")");
        if (openIndex > 0 || (closeIndex > -1 && closeIndex != (value.length() - 1))) {
            throw new IllegalFileFormatException("This ODL parser does not allow nested arrays");
        }
        value = value.substring(1, value.length() - 1);
        final boolean isDouble = value.contains(".");
        final String[] values = value.split(",");
        if (isDouble) {
            final ProductData productData = ProductData.createInstance(ProductData.TYPE_FLOAT64, values.length);
            for (int i = 0; i < values.length; i++) {
                String s = values[i].trim();
                productData.setElemDoubleAt(i, Double.parseDouble(s));
            }
            return productData;
        }
        final ProductData productData = ProductData.createInstance(ProductData.TYPE_INT32, values.length);
        for (int i = 0; i < values.length; i++) {
            String s = values[i].trim();
            productData.setElemIntAt(i, Integer.parseInt(s));
        }
        return productData;
    }
}
