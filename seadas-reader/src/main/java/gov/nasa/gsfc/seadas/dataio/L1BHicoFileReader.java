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

import java.awt.*;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: seadas
 * Date: 11/14/11
 * Time: 2:23 PM
 */
public class L1BHicoFileReader extends SeadasFileReader {

    L1BHicoFileReader(SeadasProductReader productReader) {
        super(productReader);
    }

    Array wavlengths = ncFile.findVariable("products/Lt").findAttribute("wavelengths").getValues();

    @Override
    public Product createProduct() throws ProductIOException {

        int[] dims = ncFile.findVariable("products/Lt").getShape();
        int sceneWidth = dims[1];
        int sceneHeight = dims[0];
        String productName;

        try {
            productName = getStringAttribute("metadata_FGDC_Identification_Information_Dataset_Identifier");
        } catch (Exception ignored) {
            productName = productReader.getInputFile().getName();
        }
        String hicoOrientation = getStringAttribute("metadata_HICO_Calibration_hico_orientation_from_quaternion");

        mustFlipX = mustFlipY = false;
        if(hicoOrientation.trim().equals("-XVV")) {
            mustFlipY = true;
        }
        SeadasProductReader.ProductType productType = productReader.getProductType();

        Product product = new Product(productName, productType.toString(), sceneWidth, sceneHeight);
        product.setDescription(productName);

        ProductData.UTC utcStart = getUTCAttribute("Start",globalAttributes);
        if (utcStart != null) {
            if (mustFlipY){
                product.setEndTime(utcStart);
            } else {
                product.setStartTime(utcStart);
            }
        }
        ProductData.UTC utcEnd = getUTCAttribute("End",globalAttributes);
        if (utcEnd != null) {
            if (mustFlipY) {
                product.setStartTime(utcEnd);
            } else {
                product.setEndTime(utcEnd);
            }
        }

        product.setFileLocation(productReader.getInputFile());
        product.setProductReader(productReader);

        addGlobalMetadata(product);
        variableMap = addHicoBands(product, ncFile.getVariables());

        addGeocoding(product);
        addMetadata(product, "products", "Band_Metadata");
        addMetadata(product, "navigation", "Navigation_Metadata");
        addMetadata(product, "images", "Image_Metadata");
        addMetadata(product,"quality","Quality_Metadata");

        /*
        todo add ability to read the true_color image inculded in the file
        todo the flag bit variable is in width x height not height x width as the other bands...so need to figure
             out how to read it...
        */
        addQualityFlags(product);


        product.setAutoGrouping("Lt");

        return product;
    }

    private void addQualityFlags(Product product) {
        Band QFBand = product.getBand("flags");
        if(QFBand != null) {
            FlagCoding flagCoding = new FlagCoding("Quality_Flags");
            flagCoding.addFlag("LAND", 0x01, "Land");
            flagCoding.addFlag("NAVFAIL", 0x02, "Navigation failure");
            flagCoding.addFlag("NAVWARN", 0x04, "Navigation suspect");
            flagCoding.addFlag("HISOLZEN", 0x08, "High solar zenith angle");
            flagCoding.addFlag("HISATZEN", 0x10, "Large satellite zenith angle");
            flagCoding.addFlag("SPARE", 0x20, "Unused");
            flagCoding.addFlag("CALFAIL", 0x40, "Calibration failure");
            flagCoding.addFlag("CLOUD", 0x80, "Cloud determined");

            product.getFlagCodingGroup().add(flagCoding);
            QFBand.setSampleCoding(flagCoding);

            product.getMaskGroup().add(Mask.BandMathsType.create("LAND", "Land",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "flags.LAND",
                    LandBrown, 0.0));
            product.getMaskGroup().add(Mask.BandMathsType.create("HISATZEN", "Large satellite zenith angle",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "flags.HISATZEN",
                    LightCyan, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("CLOUD", "Cloud determined",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "flags.CLOUD",
                    Color.WHITE, 0.0));
            product.getMaskGroup().add(Mask.BandMathsType.create("HISOLZEN", "High solar zenith angle",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "flags.HISOLZEN",
                    Purple, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("CALFAIL", "Calibration failure",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "flags.CALFAIL",
                    FailRed, 0.0));
            product.getMaskGroup().add(Mask.BandMathsType.create("NAVWARN", "Navigation suspect",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "flags.NAVWARN",
                    Color.MAGENTA, 0.5));
            product.getMaskGroup().add(Mask.BandMathsType.create("NAVFAIL", "Navigation failure",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "flags.NAVFAIL",
                    FailRed, 0.0));
        }
    }

    private ProductData.UTC getUTCAttribute(String key, List<Attribute> globalAttributes) {
        String timeString = null;
        try {
            if (key.equals("Start")){
                Attribute date_attribute = findAttribute("metadata_FGDC_Identification_Information_Time_Period_of_Content_Beginning_Date", globalAttributes);
                Attribute time_attribute = findAttribute("metadata_FGDC_Identification_Information_Time_Period_of_Content_Beginning_Time", globalAttributes);
                StringBuilder tstring = new StringBuilder(date_attribute.getStringValue().trim());
                tstring.append(time_attribute.getStringValue().trim());
                tstring.append("000");
                timeString = tstring.toString();
            }
            if (key.equals("End")){
                Attribute date_attribute = findAttribute("metadata_FGDC_Identification_Information_Time_Period_of_Content_Ending_Date", globalAttributes);
                Attribute time_attribute = findAttribute("metadata_FGDC_Identification_Information_Time_Period_of_Content_Ending_Time", globalAttributes);
                StringBuilder tstring = new StringBuilder(date_attribute.getStringValue().trim());
                tstring.append(time_attribute.getStringValue().trim());
                tstring.append("000");
                timeString = tstring.toString();
            }
        } catch (Exception ignored) {
        }

        if (timeString != null) {

            final DateFormat dateFormat = ProductData.UTC.createDateFormat("yyyyDDDHHmmssSSS");
            try {
                final Date date = dateFormat.parse(timeString);
                String milliSeconds = timeString.substring(timeString.length() - 3);
                return ProductData.UTC.create(date, Long.parseLong(milliSeconds) * 1000);

            } catch (ParseException ignored) {
            }
        }
        return null;
    }

    public void addMetadata(Product product, String groupname, String meta_element) throws ProductIOException {
        Group group =  ncFile.findGroup(groupname);

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

    private Map<Band, Variable> addHicoBands(Product product, List<Variable> variables) {
        final int sceneRasterWidth = product.getSceneRasterWidth();
        final int sceneRasterHeight = product.getSceneRasterHeight();
        Band band;

        Map<Band, Variable> bandToVariableMap = new HashMap<Band, Variable>();
        int spectralBandIndex = 0;
        for (Variable variable : variables) {
            if ((variable.getShortName().contains("latitude")) || (variable.getShortName().contains("longitude"))
                    || (variable.getShortName().equals("true_color")))
                continue;
            int variableRank = variable.getRank();
            if (variableRank == 2) {
                final int[] dimensions = variable.getShape();
                final int height = dimensions[0] - leadLineSkip - tailLineSkip;
                final int width = dimensions[1];

                if (height == sceneRasterHeight && width == sceneRasterWidth) {
                    String units = variable.getUnitsString();
                    final String name = variable.getShortName();
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
                    band.setDescription(name);
                }
            }
            if (variableRank == 3) {
                final int[] dimensions = variable.getShape();
                final int bands = dimensions[2];
                final int height = dimensions[0];
                final int width = dimensions[1];

                if (height == sceneRasterHeight && width == sceneRasterWidth) {
                    // final List<Attribute> list = variable.getAttributes();

                    String units = variable.getUnitsString();
                    String description = variable.getShortName();

                    for (int i = 0; i < bands; i++) {
                        final float wavelength = getHicoWvl(i);
                        StringBuilder longname = new StringBuilder(description);
                        longname.append("_");
                        longname.append(wavelength);
                        String name = longname.toString();
                        final int dataType = getProductDataType(variable);
                        band = new Band(name, dataType, width, height);
                        product.addBand(band);

                        band.setSpectralWavelength(wavelength);
                        band.setSpectralBandIndex(spectralBandIndex++);

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
        }
        return bandToVariableMap;
    }

    float getHicoWvl(int index) {
        return wavlengths.getFloat(index);
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
        Variable latVar = ncFile.findVariable("navigation/latitude");
        if (latVar == null) {
            latVar = ncFile.findVariable("navigation/latitudes");
        }
        Variable lonVar = ncFile.findVariable("navigation/longitude");
        if (lonVar == null) {
            lonVar = ncFile.findVariable("navigation/longitudes");
        }

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