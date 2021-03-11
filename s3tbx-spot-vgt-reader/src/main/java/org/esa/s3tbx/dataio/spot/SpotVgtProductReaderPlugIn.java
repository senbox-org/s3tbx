/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.s3tbx.dataio.spot;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.core.VirtualDir;
import org.apache.commons.io.FilenameUtils;
import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.RGBImageProfile;
import org.esa.snap.core.datamodel.RGBImageProfileManager;
import org.esa.snap.core.util.io.SnapFileFilter;

import java.io.*;
import java.util.Locale;

public class SpotVgtProductReaderPlugIn implements ProductReaderPlugIn {

    private boolean isVgtPCollection3Product = false;

    public SpotVgtProductReaderPlugIn() {
        RGBImageProfileManager manager = RGBImageProfileManager.getInstance();
        manager.addProfile(new RGBImageProfile("SPOT VGT RGB-1", new String[]{"MIR", "0.5 * (B2 + B3)", "B0 + 0.1 * MIR"}));
        manager.addProfile(new RGBImageProfile("SPOT VGT RGB-2", new String[]{"B3", "0.5 * (B2 + B3)", "B0 + 0.1 * B3"}));
    }

    /**
     * Checks whether the given object is an acceptable input for this product reader and if so, the method checks if it
     * is capable of decoding the input's content.
     */
    @Override
    public DecodeQualification getDecodeQualification(Object input) {
        File file = getFileInput(input);
        if (file == null) {
            return DecodeQualification.UNABLE;
        }

        VirtualDir virtualDir = VirtualDir.create(file);
        if (virtualDir == null) {
            return DecodeQualification.UNABLE;
        }

        try {
            try {
                Reader reader = virtualDir.getReader(SpotVgtConstants.PHYS_VOL_FILENAME);
                if (reader == null) {
                    return DecodeQualification.UNABLE;
                }
                try {
                    PhysVolDescriptor descriptor = new PhysVolDescriptor(reader);
                    String[] strings = virtualDir.list(descriptor.getLogVolDirName());
                    if (strings.length == 0) {
                        return DecodeQualification.UNABLE;
                    }
                } finally {
                    reader.close();
                }
                return DecodeQualification.INTENDED;
            } catch (IOException e) {
                // check for new structure of VGT P Collection3 products:
                // can be a directory or a zip file, e.g. V220140304096 or V220140304096.zip:
                // contains files <dirname>_*.hdf, <dirname>_LOG.txt, <dirname>_RIG.txt
                // --> check if <dirname>_LOG.txt and all <dirname>_*.hdf exist:
                final String productName = FilenameUtils.getBaseName(file.getName());
                try {
                    // is directory?
                    virtualDir.getReader(productName + "_LOG.TXT");
                } catch (IOException e1) {
                    try {
                        // is zip file?
                        virtualDir.getReader(productName + "/" + productName + "_LOG.TXT");
                    } catch (IOException e2) {
                        return DecodeQualification.UNABLE;
                    }
                }

                String[] filesInProduct;
                if (file.list() != null) {
                    // dir
                    filesInProduct = file.list();
                } else {
                    // zip
                    try {
                        filesInProduct = virtualDir.list(productName);
                    } catch (IOException e1) {
                        return DecodeQualification.UNABLE;
                    }
                }

                if (filesInProduct != null) {
                    for (int i = 0; i < SpotVgtConstants.BANDS_IN_VGT_P_COLLECTION3_PRODUCT.length; i++) {
                        boolean vgtPCollection3ProductFile = false;
                        for (int j = 0; j < filesInProduct.length; j++) {
                            if (filesInProduct[j].toUpperCase().
                                    equals(productName + "_" + SpotVgtConstants.BANDS_IN_VGT_P_COLLECTION3_PRODUCT[i] + ".HDF")) {
                                vgtPCollection3ProductFile = true;
                                break;
                            }
                        }
                        if (!vgtPCollection3ProductFile) {
                            return DecodeQualification.UNABLE;
                        }
                    }
                    isVgtPCollection3Product = true;
                    return DecodeQualification.INTENDED;
                } else {
                    return DecodeQualification.UNABLE;
                }
            }
        } finally {
            virtualDir.close();
        }
    }


    /**
     * Returns an array containing the classes that represent valid input types for this reader.
     * <p>
     * <p> Intances of the classes returned in this array are valid objects for the <code>setInput</code> method of the
     * <code>ProductReader</code> interface (the method will not throw an <code>InvalidArgumentException</code> in this
     * case).
     *
     * @return an array containing valid input types, never <code>null</code>
     */
    @Override
    public Class[] getInputTypes() {
        return new Class[]{String.class, File.class};
    }

    /**
     * Creates an instance of the actual product reader class. This method should never return <code>null</code>.
     *
     * @return a new reader instance, never <code>null</code>
     */
    @Override
    public ProductReader createReaderInstance() {
        return new SpotVgtProductReader(this, isVgtPCollection3Product);
    }

    /**
     * Gets an instance of {@link SnapFileFilter} for use in a {@link javax.swing.JFileChooser JFileChooser}.
     *
     * @return a file filter
     */
    @Override
    public SnapFileFilter getProductFileFilter() {
        return new SnapFileFilter(SpotVgtConstants.FORMAT_NAME,
                                  getDefaultFileExtensions(),
                                  getDescription(null));
    }

    /**
     * Gets the default file extensions associated with each of the format names returned by the <code>{@link
     * #getFormatNames}</code> method. <p>The string array returned shall always have the same lenhth as the array
     * returned by the <code>{@link #getFormatNames}</code> method. <p>The extensions returned in the string array shall
     * always include a leading colon ('.') character, e.g. <code>".hdf"</code>
     *
     * @return the default file extensions for this product I/O plug-in, never <code>null</code>
     */
    @Override
    public String[] getDefaultFileExtensions() {
        return new String[]{
                ".txt", ".TXT", ".zip", ".ZIP"
        };
    }

    /**
     * Gets a short description of this plug-in. If the given locale is set to <code>null</code> the default locale is
     * used.
     * <p>
     * <p> In a GUI, the description returned could be used as tool-tip text.
     *
     * @param locale the local for the given decription string, if <code>null</code> the default locale is used
     * @return a textual description of this product reader/writer
     */
    @Override
    public String getDescription(Locale locale) {
        return SpotVgtConstants.READER_DESCRIPTION;
    }

    /**
     * Gets the names of the product formats handled by this product I/O plug-in.
     *
     * @return the names of the product formats handled by this product I/O plug-in, never <code>null</code>
     */
    @Override
    public String[] getFormatNames() {
        return new String[]{SpotVgtConstants.FORMAT_NAME};
    }

    static String getBandName(String name) {
        int p1 = name.indexOf("_");
        int p2 = name.lastIndexOf('.');
        return name.substring(p1 == -1 ? 0 : p1 + 1, p2);
    }

    static PropertySet readPhysVolDescriptor(File inputFile) throws IOException {
        return readKeyValuePairs(inputFile);
    }

    static PropertySet readKeyValuePairs(Reader reader) throws IOException {
        try (BufferedReader breader = new BufferedReader(reader)) {
            PropertySet headerProperties = new PropertyContainer();
            String line;
            while ((line = breader.readLine()) != null) {
                line = line.trim();
                int i = line.indexOf(' ');
                String key, value;
                if (i > 0) {
                    key = line.substring(0, i);
                    value = line.substring(i + 1).trim();
                } else {
                    key = line;
                    value = "";
                }
                headerProperties.addProperty(Property.create(key, value));
            }
            return headerProperties;
        }
    }

    static File getFileInput(Object input) {
        if (input instanceof String) {
            return getFileInput(new File((String) input));
        } else if (input instanceof File) {
            return getFileInput((File) input);
        }
        return null;
    }

    static File getFileInput(File file) {
        if (file.isDirectory()) {
            return file;
        } else if (file.getName().endsWith(".zip") || file.getName().endsWith(".ZIP")) {
            return file;
        } else if (file.getName().endsWith(".txt") || file.getName().endsWith(".TXT")) {
            return file.getParentFile();
        }
        return null;
    }

    private static PropertySet readKeyValuePairs(File inputFile) throws IOException {
        return readKeyValuePairs(new FileReader(inputFile));
    }
}
