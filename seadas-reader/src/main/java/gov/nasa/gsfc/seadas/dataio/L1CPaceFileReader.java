/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package gov.nasa.gsfc.seadas.dataio;

import org.esa.snap.core.dataio.ProductIOException;
import org.esa.snap.core.datamodel.*;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Group;
import ucar.nc2.Variable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class L1CPaceFileReader extends SeadasFileReader {

    L1CPaceFileReader(SeadasProductReader productReader) {
        super(productReader);
    }

//    enum WvlType {
//        RED("red_wavelengths"),
//        BLUE("blue_wavelengths"),
//        SWIR("swir_wavelengths");
//
//        private String name;
//
//        private WvlType(String nm) {
//            name = nm;
//        }
//
//        public String toString() {
//            return name;
//        }
//    }
//
//    Array blue_wavlengths = null;
//    Array red_wavlengths = null;
//    Array swir_wavlengths = null;


    @Override
    public Product createProduct() throws ProductIOException {

        int sceneHeight = ncFile.findDimension("bins_along_track").getLength();
        int sceneWidth = ncFile.findDimension("bins_across_track").getLength();

        String productName = productReader.getInputFile().getName();

//        try {
//            productName = getStringAttribute("product_name");
//        } catch (Exception ignored) {
//            productName = productReader.getInputFile().getName();
//        }


        mustFlipY = mustFlipX = false;
        SeadasProductReader.ProductType productType = productReader.getProductType();

        Product product = new Product(productName, productType.toString(), sceneWidth, sceneHeight);
        product.setDescription(productName);

        Attribute startTime = findAttribute("time_coverage_start");
//        ProductData.UTC utcStart = getUTCAttribute("time_coverage_start");
//        ProductData.UTC utcEnd = getUTCAttribute("time_coverage_end");
//        if (startTime == null) {
//            utcStart = getUTCAttribute("Start_Time");
//            utcEnd = getUTCAttribute("End_Time");
//        }
//        // only needed as a stop-gap to handle an intermediate version of l2gen metadata
//        if (utcEnd == null) {
//            utcEnd = getUTCAttribute("time_coverage_stop");
//        }

        product.setFileLocation(productReader.getInputFile());
        product.setProductReader(productReader);

        addGlobalMetadata(product);
        Attribute scene_title = ncFile.findGlobalAttributeIgnoreCase("Title");

        if (scene_title.toString().contains("PACE OCI Level-1C Data")) {
        variableMap = addOciBands(product, ncFile.getVariables());
        } else if (scene_title.toString().contains("HARP2 Level-1C Data")) {
            variableMap = addHarpBands(product, ncFile.getVariables());
        } else if (scene_title.toString().contains("PACE SPEXone Level-1C Data")) {
            variableMap = addSPEXoneBands(product, ncFile.getVariables());
        }


        addGeocoding(product);
        addMetadata(product, "products", "Band_Metadata");
        addMetadata(product, "navigation", "Navigation_Metadata");

        if (scene_title.toString().contains("PACE OCI Level-1C Data")) {
            product.setAutoGrouping("I_-20:I_20:obs_per_view:");
        } else if (scene_title.toString().contains("HARP2 Level-1C Data")) {
            product.setAutoGrouping("I:Q:U:DOLP:I_noise:Q_noise:U_noise:DOLP_noise:Sensor_Zenith:Sensor_Azimuth:Solar_Zenith:Solar_Azimuth:obs_per_view:view_time_offsets");
        } else if (scene_title.toString().contains("PACE SPEXone Level-1C Data")) {
//            product.setAutoGrouping("QC_58:QC_22:QC_4:QC_-22:QC_-58:QC_bitwise_58:QC_bitwise_22:QC_bitwise_4:QC_bitwise_-22:QC_bitwise_-58:" +
//                    "QC_polsample_bitwise_58:QC_polsample_bitwise_22:QC_polsample_bitwise_4:QC_polsample_bitwise_-22:QC_polsample_bitwise_-58:" +
//                    "QC_polsample_58:QC_polsample_22:QC_polsample_4:QC_polsample_-22:QC_polsample_-58:" +
//                    "I_58:I_22:I_4:I_-22:I_-58:" +
//                    "I_noise_58:I_noise_22:I_noise_4:I_noise_-22:I_noise_-58:" +
//                    "I_noisefree_58:I_noisefree_22:I_noisefree_4:I_noisefree_-22:I_noisefree_-58:" +
//                    "I_polsample_58:I_polsample_22:I_polsample_4:I_polsample_-22:I_polsample_-58:" +
//                    "I_polsample_noise_58:I_polsample_noise_22:I_polsample_noise_4:I_polsample_noise_-22:I_polsample_noise_-58:" +
//                    "I_noisefree_polsample_58:I_noisefree_polsample_22:I_noisefree_polsample_4:I_noisefree_polsample_-22:I_noisefree_polsample_-58:" +
//                    "DOLP_58:DOLP_22:DOLP_4:DOLP_-22:DOLP_-58:" +
//                    "DOLP_noise_58:DOLP_noise_22:DOLP_noise_4:DOLP_noise_-22:DOLP_noise_-58:" +
//                    "DOLP_noisefree_58:DOLP_noisefree_22:DOLP_noisefree_4:DOLP_noisefree_-22:DOLP_noisefree_-58:" +
//                    "Q_over_I_58:Q_over_I_22:Q_over_I_4:Q_over_I_-22:Q_over_I_-58:" +
//                    "Q_over_I_noise_58:Q_over_I_noise_22:Q_over_I_noise_4:Q_over_I_noise_-22:Q_over_I_noise_-58:" +
//                    "Q_over_I_noisefree_58:Q_over_I_noisefree_22:Q_over_I_noisefree_4:Q_over_I_noisefree_-22:Q_over_I_noisefree_-58:" +
//                    "AOLP:AOLP_58:AOLP:AOLP_22:AOLP:AOLP_4:AOLP:AOLP_-22:AOLP:AOLP_-58:" +
//                    "AOLP_noisefree_58:AOLP_noisefree_22:AOLP_noisefree_4:AOLP_noisefree_-22:AOLP_noisefree_-58:" +
//                    "U_over_I_58:U_over_I_22:U_over_I_4:U_over_I_-22:U_over_I_-58:" +
//                    "U_over_I_noise_58:U_over_I_noise_22:U_over_I_noise_4:U_over_I_noise_-22:U_over_I_noise_-58:" +
//                    "U_over_I_noisefree_58:U_over_I_noisefree_22:U_over_I_noisefree_4:U_over_I_noisefree_-22:U_over_I_noisefree_-58:" +
//                    "scattering_angle:sensor_azimuth:sensor_zenith:solar_azimuth:solar_zenith:obs_per_view:view_time_offsets");
            product.setAutoGrouping("QC:QC_bitwise:QC_polsample_bitwise:QC_polsample:I:I_noise:I_noisefree:I_polsample:I_polsample_noise:" +
                    "I_noisefree_polsample:DOLP:DOLP_noise:DOLP_noisefree:Q_over_I:Q_over_I_noise:Q_over_I_noisefree:AOLP:AOLP_noisefree:U_over_I:" +
                    "U_over_I_noise:U_over_I_noisefree:scattering_angle:sensor_azimuth:sensor_zenith:solar_azimuth:solar_zenith:obs_per_view:view_time_offsets");
        }
//        product.setAutoGrouping("I_-20:I_20");

        return product;
    }


    public void addMetadata(Product product, String groupname, String meta_element) throws ProductIOException {
        Group group = ncFile.findGroup(groupname);

        if (group != null) {
            final MetadataElement bandAttributes = new MetadataElement(meta_element);
            List<Variable> variables = group.getVariables();
            for (Variable variable : variables) {
                final String name = variable.getShortName();
                final MetadataElement sdsElement = new MetadataElement(name + ".attributes");
                final int dataType = getProductDataType(variable);
                final MetadataAttribute prodtypeattr = new MetadataAttribute("data_type", dataType);

                sdsElement.addAttribute(prodtypeattr);
                bandAttributes.addElement(sdsElement);

                final List<Attribute> list = variable.getAttributes();
                for (Attribute varAttribute : list) {
                    addAttributeToElement(sdsElement, varAttribute);
                }
            }
            final MetadataElement metadataRoot = product.getMetadataRoot();
            metadataRoot.addElement(bandAttributes);
        }
    }

    private Map<Band, Variable> addHarpBands(Product product, List<Variable> variables) {
        final int sceneRasterWidth = product.getSceneRasterWidth();
        final int sceneRasterHeight = product.getSceneRasterHeight();
        Band band;

        Array wavelengths = null;
        Array view_angles = null;

        Map<Band, Variable> bandToVariableMap = new HashMap<Band, Variable>();
        int spectralBandIndex = 0;
        for (Variable variable : variables) {
            if (variable.getParentGroup().equals("sensor_views_bands"))
                continue;
            if ((variable.getShortName().equals("latitude")) || (variable.getShortName().equals("longitude")))
                continue;
            int variableRank = variable.getRank();

            if (variableRank == 2) {
                final int[] dimensions = variable.getShape();
                final int height = dimensions[0];
                final int width = dimensions[1];

                if (height == sceneRasterHeight && width == sceneRasterWidth) {
                    // final List<Attribute> list = variable.getAttributes();

                    String units = variable.getUnitsString();
                    String name = variable.getShortName();
                    final int dataType = getProductDataType(variable);
                    band = new Band(name, dataType, width, height);
                    product.addBand(band);

                    final List<Attribute> list = variable.getAttributes();
                    for (Attribute hdfAttribute : list) {
                        final String attribName = hdfAttribute.getShortName();
                        if ("units".equals(attribName)) {
                            band.setUnit(hdfAttribute.getStringValue());
                        } else if ("long_name".equals(attribName)) {
                            band.setDescription(hdfAttribute.getStringValue());
                        } else if ("slope".equals(attribName)) {
                            band.setScalingFactor(hdfAttribute.getNumericValue(0).doubleValue());
                        } else if ("intercept".equals(attribName)) {
                            band.setScalingOffset(hdfAttribute.getNumericValue(0).doubleValue());
                        } else if ("scale_factor".equals(attribName)) {
                            band.setScalingFactor(hdfAttribute.getNumericValue(0).doubleValue());
                        } else if ("add_offset".equals(attribName)) {
                            band.setScalingOffset(hdfAttribute.getNumericValue(0).doubleValue());
                        } else if ("bad_value_scaled".equals(attribName)) {
                            band.setNoDataValue(hdfAttribute.getNumericValue(0).doubleValue());
                            band.setNoDataValueUsed(true);
                        }
                    }
                    bandToVariableMap.put(band, variable);
                    band.setUnit(units);
                    band.setDescription(variable.getDescription());
                }
            } else if (variableRank == 3) {
                spectralBandIndex = -1;
                final int[] dimensions = variable.getShape();
                final int views = dimensions[0];
                final int height = dimensions[1];
                final int width = dimensions[2];

                if (height == sceneRasterHeight && width == sceneRasterWidth) {
                    // final List<Attribute> list = variable.getAttributes();

                    String units = variable.getUnitsString();
                    String description = variable.getShortName();

                    Variable view_angle = ncFile.findVariable("sensor_views_bands/view_angles");
                    Variable wvl = ncFile.findVariable("sensor_views_bands/intensity_wavelengths");
                    Variable wvl_sliced = null;

                    if (view_angle != null && wvl != null) {
                        try {
                            view_angles = view_angle.read();
                            try {
                                wvl_sliced = wvl.slice(0,0);
                            } catch (InvalidRangeException e) {
                                e.printStackTrace();  //Todo change body of catch statement.
                            }
                            wavelengths = wvl_sliced.read();
                        } catch (IOException e) {
                        }
                        ArrayList wavelength_list = new ArrayList();
                        for (int i = 0; i < views; i++) {
                            StringBuilder longname = new StringBuilder(description);
                            longname.append("_");
                            longname.append(view_angles.getInt(i));
                            longname.append("_");
                            longname.append(wavelengths.getInt(i));
                            String name = longname.toString();

                            final int dataType = getProductDataType(variable);
                            band = new Band(name, dataType, width, height);
                            product.addBand(band);

                            band.setSpectralWavelength(wavelengths.getFloat(i));
                            if (!wavelength_list.contains(wavelengths.getInt(i))) {
                                wavelength_list.add(wavelengths.getInt(i));
                                spectralBandIndex++;
                            }
                            band.setSpectralBandIndex(spectralBandIndex);

                            Variable sliced = null;
                            try {
                                sliced = variable.slice(0, i);
                            } catch (InvalidRangeException e) {
                                e.printStackTrace();  //Todo change body of catch statement.
                            }

                            final List<Attribute> list = variable.getAttributes();
                            for (Attribute hdfAttribute : list) {
                                final String attribName = hdfAttribute.getShortName();
                                if ("units".equals(attribName)) {
                                    band.setUnit(hdfAttribute.getStringValue());
                                } else if ("long_name".equals(attribName)) {
                                    band.setDescription(hdfAttribute.getStringValue());
                                } else if ("slope".equals(attribName)) {
                                    band.setScalingFactor(hdfAttribute.getNumericValue(0).doubleValue());
                                } else if ("intercept".equals(attribName)) {
                                    band.setScalingOffset(hdfAttribute.getNumericValue(0).doubleValue());
                                } else if ("scale_factor".equals(attribName)) {
                                    band.setScalingFactor(hdfAttribute.getNumericValue(0).doubleValue());
                                } else if ("add_offset".equals(attribName)) {
                                    band.setScalingOffset(hdfAttribute.getNumericValue(0).doubleValue());
                                } else if ("bad_value_scaled".equals(attribName)) {
                                    band.setNoDataValue(hdfAttribute.getNumericValue(0).doubleValue());
                                    band.setNoDataValueUsed(true);
                                }
                            }
                            bandToVariableMap.put(band, sliced);
                            band.setUnit(units);
                            band.setDescription(description);
                        }
                    }
                }
            }


        }
        return bandToVariableMap;
    }

    private Map<Band, Variable> addOciBands(Product product, List<Variable> variables) {
        final int sceneRasterWidth = product.getSceneRasterWidth();
        final int sceneRasterHeight = product.getSceneRasterHeight();
        Band band;

        Array wavelengths = null;
        Array view_angles = null;

        Map<Band, Variable> bandToVariableMap = new HashMap<Band, Variable>();
        int spectralBandIndex = 0;
        for (Variable variable : variables) {
            if (variable.getParentGroup().equals("sensor_views_bands"))
                continue;
            if ((variable.getShortName().equals("latitude")) || (variable.getShortName().equals("longitude")))
                continue;
            int variableRank = variable.getRank();

            if (variableRank == 2) {
                final int[] dimensions = variable.getShape();
                final int height = dimensions[0];
                final int width = dimensions[1];

                if (height == sceneRasterHeight && width == sceneRasterWidth) {
                    // final List<Attribute> list = variable.getAttributes();

                    String units = variable.getUnitsString();
                    String name = variable.getShortName();
                    final int dataType = getProductDataType(variable);
                    band = new Band(name, dataType, width, height);
                    product.addBand(band);

                    final List<Attribute> list = variable.getAttributes();
                    for (Attribute hdfAttribute : list) {
                        final String attribName = hdfAttribute.getShortName();
                        if ("units".equals(attribName)) {
                            band.setUnit(hdfAttribute.getStringValue());
                        } else if ("long_name".equals(attribName)) {
                            band.setDescription(hdfAttribute.getStringValue());
                        } else if ("slope".equals(attribName)) {
                            band.setScalingFactor(hdfAttribute.getNumericValue(0).doubleValue());
                        } else if ("intercept".equals(attribName)) {
                            band.setScalingOffset(hdfAttribute.getNumericValue(0).doubleValue());
                        } else if ("scale_factor".equals(attribName)) {
                            band.setScalingFactor(hdfAttribute.getNumericValue(0).doubleValue());
                        } else if ("add_offset".equals(attribName)) {
                            band.setScalingOffset(hdfAttribute.getNumericValue(0).doubleValue());
                        } else if ("bad_value_scaled".equals(attribName)) {
                            band.setNoDataValue(hdfAttribute.getNumericValue(0).doubleValue());
                            band.setNoDataValueUsed(true);
                        }
                    }
                    bandToVariableMap.put(band, variable);
                    band.setUnit(units);
                    band.setDescription(variable.getDescription());
                }
            } else if (variableRank == 3) {
                final int[] dimensions = variable.getShape();
                final int views = dimensions[2];
                final int height = dimensions[0];
                final int width = dimensions[1];

                if (height == sceneRasterHeight && width == sceneRasterWidth) {
                    // final List<Attribute> list = variable.getAttributes();

                    String units = variable.getUnitsString();
                    String description = variable.getShortName();

                    Variable view_angle = ncFile.findVariable("sensor_views_bands/view_angles");

                    if (view_angle != null) {
                        try {
                            view_angles = view_angle.read();
                        } catch (IOException e) {
                        }

                        for (int i = 0; i < views; i++) {
                            StringBuilder longname = new StringBuilder(description);
                            longname.append("_");
                            longname.append(view_angles.getInt(i));
                            String name = longname.toString();
                            final int dataType = getProductDataType(variable);
                            band = new Band(name, dataType, width, height);
                            product.addBand(band);

                            Variable sliced = null;
                            try {
                                sliced = variable.slice(2, i);
                            } catch (InvalidRangeException e) {
                                e.printStackTrace();  //Todo change body of catch statement.
                            }

                            final List<Attribute> list = variable.getAttributes();
                            for (Attribute hdfAttribute : list) {
                                final String attribName = hdfAttribute.getShortName();
                                if ("units".equals(attribName)) {
                                    band.setUnit(hdfAttribute.getStringValue());
                                } else if ("long_name".equals(attribName)) {
                                    band.setDescription(hdfAttribute.getStringValue());
                                } else if ("slope".equals(attribName)) {
                                    band.setScalingFactor(hdfAttribute.getNumericValue(0).doubleValue());
                                } else if ("intercept".equals(attribName)) {
                                    band.setScalingOffset(hdfAttribute.getNumericValue(0).doubleValue());
                                } else if ("scale_factor".equals(attribName)) {
                                    band.setScalingFactor(hdfAttribute.getNumericValue(0).doubleValue());
                                } else if ("add_offset".equals(attribName)) {
                                    band.setScalingOffset(hdfAttribute.getNumericValue(0).doubleValue());
                                } else if ("bad_value_scaled".equals(attribName)) {
                                    band.setNoDataValue(hdfAttribute.getNumericValue(0).doubleValue());
                                    band.setNoDataValueUsed(true);
                                }
                            }
                            bandToVariableMap.put(band, sliced);
                            band.setUnit(units);
                            band.setDescription(description);
                        }
                    }
                }
            } else if (variableRank == 4) {
                final int[] dimensions = variable.getShape();
                final int views = dimensions[2];
                final int height = dimensions[0];
                final int width = dimensions[1];
                final int bands = dimensions[3];

                if (height == sceneRasterHeight && width == sceneRasterWidth) {

                    String units = variable.getUnitsString();
                    String description = variable.getShortName();

                    Variable view_angle = ncFile.findVariable("sensor_views_bands/view_angles");
                    Variable wvl = ncFile.findVariable("sensor_views_bands/intensity_wavelengths");
                    Variable wvl_sliced = null;

                    if (view_angle != null && wvl != null) {
                        try {
                            view_angles = view_angle.read();
                            try {
                                wvl_sliced = wvl.slice(0,1);
                            } catch (InvalidRangeException e) {
                                e.printStackTrace();  //Todo change body of catch statement.
                            }
                            wavelengths = wvl_sliced.read();
                            wavelengths.setInt(120, wavelengths.getInt(120) + 1);
                            wavelengths.setInt(121, wavelengths.getInt(121) + 1);
                            wavelengths.setInt(122, wavelengths.getInt(122) + 1);
                            wavelengths.setInt(123, wavelengths.getInt(123) + 1);
                            wavelengths.setInt(243, wavelengths.getInt(243) + 1);
                            wavelengths.setInt(246, wavelengths.getInt(246) + 1);

                        } catch (IOException e) {
                        }


                        for (int i = 0; i < views; i++) {
                            for (int j = 0; j < bands; j++) {
                                StringBuilder longname = new StringBuilder(description);
                                longname.append("_");
                                longname.append(view_angles.getInt(i));
                                longname.append("_");

                                longname.append(wavelengths.getInt(j));
                                String name = longname.toString();
                                final int dataType = getProductDataType(variable);
                                band = new Band(name, dataType, width, height);
                                product.addBand(band);

                                band.setSpectralWavelength(wavelengths.getFloat(j));
                                band.setSpectralBandIndex(j);

                                Variable sliced1 = null;
                                Variable sliced = null;
                                try {
                                    sliced1 = variable.slice(2, i);
                                    sliced = sliced1.slice(2, j);
                                } catch (InvalidRangeException e) {
                                    e.printStackTrace();  //Todo change body of catch statement.
                                }

                                final List<Attribute> list = variable.getAttributes();
                                for (Attribute hdfAttribute : list) {
                                    final String attribName = hdfAttribute.getShortName();
                                    if ("units".equals(attribName)) {
                                        band.setUnit(hdfAttribute.getStringValue());
                                    } else if ("long_name".equals(attribName)) {
                                        band.setDescription(hdfAttribute.getStringValue());
                                    } else if ("slope".equals(attribName)) {
                                        band.setScalingFactor(hdfAttribute.getNumericValue(0).doubleValue());
                                    } else if ("intercept".equals(attribName)) {
                                        band.setScalingOffset(hdfAttribute.getNumericValue(0).doubleValue());
                                    } else if ("scale_factor".equals(attribName)) {
                                        band.setScalingFactor(hdfAttribute.getNumericValue(0).doubleValue());
                                    } else if ("add_offset".equals(attribName)) {
                                        band.setScalingOffset(hdfAttribute.getNumericValue(0).doubleValue());
                                    } else if ("bad_value_scaled".equals(attribName)) {
                                        band.setNoDataValue(hdfAttribute.getNumericValue(0).doubleValue());
                                        band.setNoDataValueUsed(true);
                                    }
                                }
                                bandToVariableMap.put(band, sliced);
                                band.setUnit(units);
                                band.setDescription(description);
                            }
                        }
                    }
                }
            }


        }
        return bandToVariableMap;
    }

    private Map<Band, Variable> addSPEXoneBands(Product product, List<Variable> variables) {
        final int sceneRasterWidth = product.getSceneRasterWidth();
        final int sceneRasterHeight = product.getSceneRasterHeight();
        Band band;

        Array wavelengths = null;
        Array wavelengths_pol = null;
        Array view_angles = null;

        Map<Band, Variable> bandToVariableMap = new HashMap<Band, Variable>();
        int spectralBandIndex = 0;
        for (Variable variable : variables) {
            if (variable.getParentGroup().equals("sensor_views_bands"))
                continue;
            if ((variable.getShortName().equals("latitude")) || (variable.getShortName().equals("longitude")))
                continue;
            int variableRank = variable.getRank();

            if (variableRank == 2) {
                final int[] dimensions = variable.getShape();
                final int height = dimensions[0];
                final int width = dimensions[1];

                if (height == sceneRasterHeight && width == sceneRasterWidth) {
                    // final List<Attribute> list = variable.getAttributes();

                    String units = variable.getUnitsString();
                    String name = variable.getShortName();
                    final int dataType = getProductDataType(variable);
                    band = new Band(name, dataType, width, height);
                    product.addBand(band);

                    final List<Attribute> list = variable.getAttributes();
                    for (Attribute hdfAttribute : list) {
                        final String attribName = hdfAttribute.getShortName();
                        if ("units".equals(attribName)) {
                            band.setUnit(hdfAttribute.getStringValue());
                        } else if ("long_name".equals(attribName)) {
                            band.setDescription(hdfAttribute.getStringValue());
                        } else if ("slope".equals(attribName)) {
                            band.setScalingFactor(hdfAttribute.getNumericValue(0).doubleValue());
                        } else if ("intercept".equals(attribName)) {
                            band.setScalingOffset(hdfAttribute.getNumericValue(0).doubleValue());
                        } else if ("scale_factor".equals(attribName)) {
                            band.setScalingFactor(hdfAttribute.getNumericValue(0).doubleValue());
                        } else if ("add_offset".equals(attribName)) {
                            band.setScalingOffset(hdfAttribute.getNumericValue(0).doubleValue());
                        } else if ("bad_value_scaled".equals(attribName)) {
                            band.setNoDataValue(hdfAttribute.getNumericValue(0).doubleValue());
                            band.setNoDataValueUsed(true);
                        }
                    }
                    bandToVariableMap.put(band, variable);
                    band.setUnit(units);
                    band.setDescription(variable.getDescription());
                }
            } else if (variableRank == 3) {
                final int[] dimensions = variable.getShape();
                final int views = dimensions[2];
                final int height = dimensions[0];
                final int width = dimensions[1];

                if (height == sceneRasterHeight && width == sceneRasterWidth) {
                    // final List<Attribute> list = variable.getAttributes();

                    String units = variable.getUnitsString();
                    String description = variable.getShortName();

                    Variable view_angle = ncFile.findVariable("sensor_views_bands/view_angles");

                    if (view_angle != null) {
                        try {
                            view_angles = view_angle.read();
                        } catch (IOException e) {
                        }

                        for (int i = 0; i < views; i++) {
                            StringBuilder longname = new StringBuilder(description);
                            longname.append("_");
                            if ((i > views / 2) && (view_angles.getInt(i) == view_angles.getInt(views - 1 - i))) {
                                longname.append(-view_angles.getInt(i));
                            } else {
                                longname.append(view_angles.getInt(i));
                            }
                            String name = longname.toString();
                            final int dataType = getProductDataType(variable);
                            band = new Band(name, dataType, width, height);
                            product.addBand(band);

                            Variable sliced = null;
                            try {
                                sliced = variable.slice(2, i);
                            } catch (InvalidRangeException e) {
                                e.printStackTrace();  //Todo change body of catch statement.
                            }

                            final List<Attribute> list = variable.getAttributes();
                            for (Attribute hdfAttribute : list) {
                                final String attribName = hdfAttribute.getShortName();
                                if ("units".equals(attribName)) {
                                    band.setUnit(hdfAttribute.getStringValue());
                                } else if ("long_name".equals(attribName)) {
                                    band.setDescription(hdfAttribute.getStringValue());
                                } else if ("slope".equals(attribName)) {
                                    band.setScalingFactor(hdfAttribute.getNumericValue(0).doubleValue());
                                } else if ("intercept".equals(attribName)) {
                                    band.setScalingOffset(hdfAttribute.getNumericValue(0).doubleValue());
                                } else if ("scale_factor".equals(attribName)) {
                                    band.setScalingFactor(hdfAttribute.getNumericValue(0).doubleValue());
                                } else if ("add_offset".equals(attribName)) {
                                    band.setScalingOffset(hdfAttribute.getNumericValue(0).doubleValue());
                                } else if ("bad_value_scaled".equals(attribName)) {
                                    band.setNoDataValue(hdfAttribute.getNumericValue(0).doubleValue());
                                    band.setNoDataValueUsed(true);
                                }
                            }
                            bandToVariableMap.put(band, sliced);
                            band.setUnit(units);
                            band.setDescription(description);
                        }
                    }
                }
            } else if (variableRank == 4) {
                final int[] dimensions = variable.getShape();
                final int views = dimensions[2];
                final int height = dimensions[0];
                final int width = dimensions[1];
                final int bands = dimensions[3];

                if (height == sceneRasterHeight && width == sceneRasterWidth) {

                    String units = variable.getUnitsString();
                    String description = variable.getShortName();

                    Variable view_angle = ncFile.findVariable("sensor_views_bands/view_angles");
                    Variable wvl = ncFile.findVariable("sensor_views_bands/intensity_wavelengths");
                    Variable wvl_sliced = null;
                    Variable wvl_pol = ncFile.findVariable("sensor_views_bands/polarization_wavelengths");
                    Variable wvl_sliced_pol = null;

                    if (view_angle != null && wvl != null) {
                        try {
                            view_angles = view_angle.read();
                            try {
                                wvl_sliced = wvl.slice(0,1);
                                wvl_sliced_pol = wvl_pol.slice(0,1);
                            } catch (InvalidRangeException e) {
                                e.printStackTrace();  //Todo change body of catch statement.
                            }
                            wavelengths = wvl_sliced.read();
                            wavelengths_pol = wvl_sliced_pol.read();

                        } catch (IOException e) {
                        }


                        for (int i = 0; i < views; i++) {
                            for (int j = 0; j < bands; j++) {
                                StringBuilder longname = new StringBuilder(description);
                                longname.append("_");
                                if ((i > views / 2) && (view_angles.getInt(i) == view_angles.getInt(views - 1 - i))) {
                                    longname.append(-view_angles.getInt(i));
                                } else {
                                    longname.append(view_angles.getInt(i));
                                }
                                longname.append("_");
                                if (bands == 400) {
                                    longname.append(wavelengths.getInt(j));
                                } else {
                                    longname.append(wavelengths_pol.getInt(j));
                                }
                                String name = longname.toString();
                                final int dataType = getProductDataType(variable);
                                band = new Band(name, dataType, width, height);
                                product.addBand(band);

                                if (bands == 400) {
                                    band.setSpectralWavelength(wavelengths.getFloat(j));
                                } else {
                                    band.setSpectralWavelength(wavelengths_pol.getFloat(j));
                                }
                                band.setSpectralBandIndex(j);


                                Variable sliced1 = null;
                                Variable sliced = null;
                                try {
                                    sliced1 = variable.slice(2, i);
                                    sliced = sliced1.slice(2, j);
                                } catch (InvalidRangeException e) {
                                    e.printStackTrace();  //Todo change body of catch statement.
                                }

                                final List<Attribute> list = variable.getAttributes();
                                for (Attribute hdfAttribute : list) {
                                    final String attribName = hdfAttribute.getShortName();
                                    if ("units".equals(attribName)) {
                                        band.setUnit(hdfAttribute.getStringValue());
                                    } else if ("long_name".equals(attribName)) {
                                        band.setDescription(hdfAttribute.getStringValue());
                                    } else if ("slope".equals(attribName)) {
                                        band.setScalingFactor(hdfAttribute.getNumericValue(0).doubleValue());
                                    } else if ("intercept".equals(attribName)) {
                                        band.setScalingOffset(hdfAttribute.getNumericValue(0).doubleValue());
                                    } else if ("scale_factor".equals(attribName)) {
                                        band.setScalingFactor(hdfAttribute.getNumericValue(0).doubleValue());
                                    } else if ("add_offset".equals(attribName)) {
                                        band.setScalingOffset(hdfAttribute.getNumericValue(0).doubleValue());
                                    } else if ("bad_value_scaled".equals(attribName)) {
                                        band.setNoDataValue(hdfAttribute.getNumericValue(0).doubleValue());
                                        band.setNoDataValueUsed(true);
                                    }
                                }
                                bandToVariableMap.put(band, sliced);
                                band.setUnit(units);
                                band.setDescription(description);
                            }
                        }
                    }
                }
            }


        }
        return bandToVariableMap;
    }

    public ProductData readDataFlip(Variable variable) throws ProductIOException {
        final int dataType = getProductDataType(variable);
        Array array;
        Object storage;
        try {
            array = variable.read();
            storage = array.flip(0).copyTo1DJavaArray();
        } catch (IOException e) {
            throw new ProductIOException(e.getMessage(), e);
        }
        return ProductData.createInstance(dataType, storage);
    }

    public void addGeocoding(final Product product) throws ProductIOException {
        final String longitude = "longitude";
        final String latitude = "latitude";
        String navGroup = "geolocation_data";

        Variable latVar = ncFile.findVariable(navGroup + "/" + latitude);
        Variable lonVar = ncFile.findVariable(navGroup + "/" + longitude);

        if (latVar != null && lonVar != null) {
            final ProductData lonRawData;
            final ProductData latRawData;
            if (mustFlipY) {
                lonRawData = readDataFlip(lonVar);
                latRawData = readDataFlip(latVar);
            } else {
                lonRawData = readData(lonVar);
                latRawData = readData(latVar);
            }

            Band latBand = product.addBand(latVar.getShortName(), ProductData.TYPE_FLOAT32);
            Band lonBand = product.addBand(lonVar.getShortName(), ProductData.TYPE_FLOAT32);
            latBand.setNoDataValue(-999.);
            lonBand.setNoDataValue(-999.);
            latBand.setNoDataValueUsed(true);
            lonBand.setNoDataValueUsed(true);
            latBand.setData(latRawData);
            lonBand.setData(lonRawData);

            product.setSceneGeoCoding(GeoCodingFactory.createPixelGeoCoding(latBand, lonBand, null, 5));

        }
    }
}