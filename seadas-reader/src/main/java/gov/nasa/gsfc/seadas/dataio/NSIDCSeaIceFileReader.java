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
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import static java.lang.String.format;

public class NSIDCSeaIceFileReader extends SeadasFileReader {

    protected String title;
    protected int numPixels = 0;
    protected int numScans = 0;

    NSIDCSeaIceFileReader(SeadasProductReader productReader) {
        super(productReader);
    }

    @Override
    public Product createProduct() throws ProductIOException {

//        int sceneWidth = getIntAttribute("Pixels_per_Scan_Line");
//        int sceneHeight = getIntAttribute("Number_of_Scan_Lines");
        String productName = productReader.getInputFile().getName();
        numPixels = getDimension("x");
        numScans = getDimension("y");
        title = getStringAttribute("title");

        SeadasProductReader.ProductType productType = productReader.getProductType();

        Product product = new Product(productName, productType.toString(), numPixels, numScans);
        product.setDescription(productName);

        ProductData.UTC utcStart = getUTCAttribute("time_coverage_start");
        if (utcStart != null) {
            product.setStartTime(utcStart);
        }
        ProductData.UTC utcEnd = getUTCAttribute("time_coverage_end");
        if (utcEnd != null) {
            product.setEndTime(utcEnd);
        }

        product.setFileLocation(productReader.getInputFile());
        product.setProductReader(productReader);

        addGlobalMetadata(product);
        addScientificMetadata(product);

        variableMap = addNSIDCBands(product, ncFile.getVariables());

        addGeocoding(product);

        return product;

    }

    public Map<Band, Variable> addNSIDCBands(Product product,
                                             List<Variable> variables) {
        Map<Band, Variable> bandToVariableMap = new HashMap<Band, Variable>();
        for (Variable variable : variables) {
            int variableRank = variable.getRank();
            if (variableRank == 3) {
                add3DNSIDCBands(product, variable, bandToVariableMap);
            }
        }
        setDateBand(product);

        return bandToVariableMap;
    }

    protected void setDateBand(Product product) {
        int dateBandIndex = 0;
        for (String name : product.getBandNames()) {
            Band band = product.getBandAt(product.getBandIndex(name));
            if (name.matches("\\w+_\\d{6,}")) {
                String[] parts = name.split("_");
                String timestr = parts[parts.length - 1].trim();
                //Some bands have the wvl portion in the middle...
                if (!timestr.matches("^\\d{6,}")) {
                    timestr = parts[parts.length - 2].trim();
                }
//                final float time = Float.parseFloat(timestr);
                final int timeInt = Integer.parseInt(timestr);
                Calendar cal = Calendar.getInstance();
                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                cal.set(Calendar.DAY_OF_MONTH, 1);
                cal.set(Calendar.MONTH, 1);
                cal.set(Calendar.YEAR, 1601);
                cal.add(Calendar.DAY_OF_MONTH, timeInt);
                String date = dateFormat.format(cal.getTime());
//                band.setSpectralWavelength(time);
                band.setDate(date);
                band.setDateBandIndex(dateBandIndex++);
            }
        }
    }

    protected Map<Band, Variable> add3DNSIDCBands(Product product, Variable variable, Map<Band, Variable> bandToVariableMap) {
        final int sceneRasterWidth = product.getSceneRasterWidth();
        final int sceneRasterHeight = product.getSceneRasterHeight();

        int spectralBandIndex = 0;
        Array times = null;

        final int[] dimensions = variable.getShape();
        final int bands = dimensions[0];
        final int height = dimensions[1];
        final int width = dimensions[2];

        if (height == sceneRasterHeight && width == sceneRasterWidth) {
            // final List<Attribute> list = variable.getAttributes();

            Variable time = ncFile.findVariable("time");
            // wavenlengths for modis L2 files
//            if (time == null) {
//                if (bands == 2 || bands == 3) {
//                    wvl = ncFile.findVariable("HDFEOS/SWATHS/Aerosol_NearUV_Swath/Data_Fields/Wavelength");
//                    // wavelenghs for DSCOVR EPIC L2 files
//                }
//            }
            if (time != null) {
                try {
                    times = time.read();
                } catch (IOException e) {
                }

                for (int i = 0; i < bands; i++) {
                    final String shortname = variable.getShortName();
                    StringBuilder longname = new StringBuilder(shortname);
                    longname.append("_");
                    longname.append(times.getInt(i));
                    String name = longname.toString();
                    final int dataType = getProductDataType(variable);

                    if (!product.containsBand(name)) {

                        final Band band = new Band(name, dataType, width, height);
                        product.addBand(band);

                        Variable sliced = null;
                        try {
                            sliced = variable.slice(0, i);
                        } catch (InvalidRangeException e) {
                            e.printStackTrace();  //Todo change body of catch statement.
                        }
                        bandToVariableMap.put(band, sliced);

                        try {
                            Attribute fillValue = variable.findAttribute("_FillValue");
                            if (fillValue == null) {
                                fillValue = variable.findAttribute("bad_value_scaled");
                            }
                            band.setNoDataValue((double) fillValue.getNumericValue().floatValue());
                            band.setNoDataValueUsed(true);
                        } catch (Exception ignored) {
                        }

                        final List<Attribute> list = variable.getAttributes();
                        double[] validMinMax = {0.0, 0.0};
                        for (Attribute attribute : list) {
                            final String attribName = attribute.getShortName();
                            if ("units".equals(attribName)) {
                                band.setUnit(attribute.getStringValue());
                            } else if ("long_name".equals(attribName)) {
                                band.setDescription(attribute.getStringValue());
                            } else if ("slope".equals(attribName)) {
                                band.setScalingFactor(attribute.getNumericValue(0).doubleValue());
                            } else if ("intercept".equals(attribName)) {
                                band.setScalingOffset(attribute.getNumericValue(0).doubleValue());
                            } else if ("scale_factor".equals(attribName)) {
                                band.setScalingFactor(attribute.getNumericValue(0).doubleValue());
                            } else if ("add_offset".equals(attribName)) {
                                band.setScalingOffset(attribute.getNumericValue(0).doubleValue());
                            } else if (attribName.startsWith("valid_")) {
                                if ("valid_min".equals(attribName)) {
                                    if (attribute.getDataType().isUnsigned()) {
                                        validMinMax[0] = getUShortAttribute(attribute);
                                    } else {
                                        validMinMax[0] = attribute.getNumericValue(0).doubleValue();
                                    }
                                } else if ("valid_max".equals(attribName)) {
                                    if (attribute.getDataType().isUnsigned()) {
                                        validMinMax[1] = getUShortAttribute(attribute);
                                    } else {
                                        validMinMax[1] = attribute.getNumericValue(0).doubleValue();
                                    }
                                } else if ("valid_range".equals(attribName)) {
                                    validMinMax[0] = attribute.getNumericValue(0).doubleValue();
                                    validMinMax[1] = attribute.getNumericValue(1).doubleValue();
                                }
                            }
                        }
                        if (validMinMax[0] != validMinMax[1]) {
                            String validExp;
                            if (ncFile.getFileTypeId().equalsIgnoreCase("HDF4")) {
                                validExp = format("%s >= %.05f && %s <= %.05f", name, validMinMax[0], name, validMinMax[1]);

                            } else {
                                double[] minmax = {0.0, 0.0};
                                minmax[0] = validMinMax[0];
                                minmax[1] = validMinMax[1];

                                if (band.getScalingFactor() != 1.0) {
                                    minmax[0] *= band.getScalingFactor();
                                    minmax[1] *= band.getScalingFactor();
                                }
                                if (band.getScalingOffset() != 0.0) {
                                    minmax[0] += band.getScalingOffset();
                                    minmax[1] += band.getScalingOffset();
                                }
                                validExp = format("%s >= %.05f && %s <= %.05f", name, minmax[0], name, minmax[1]);

                            }
                            band.setValidPixelExpression(validExp);//.format(name, validMinMax[0], name, validMinMax[1]));
                        }
                    } else {
                        logger.log(Level.WARNING, "The Product '" + product.getName() + "' contains duplicate bands" +
                                " with the name '" + name + "', one will be ignored.");
                    }
                }
            }
        }
        return bandToVariableMap;
    }

    public void addGeocoding(Product product) {

        //This gets VERY close, based on one scene's coastline matching.  Not quite as good as the l2gen output,
        // but can't figure out where the discrepancy lies...so going with this as good enough.

        double pixelX = 0.0;
        double pixelY = 0.0;

//        String westName = "HDFEOS_POINTS_Scene_Header_Scene_upper-left_longitude";
//        String northName = "HDFEOS_POINTS_Scene_Header_Scene_upper-left_latitude";
//        String centralLatName = "HDFEOS_POINTS_Map_Projection_Central_Latitude_(parallel)";
//        String centralLonName = "HDFEOS_POINTS_Map_Projection_Central_Longitude_(meridian)";
//        String sceneCenterLatName = "HDFEOS_POINTS_Scene_Header_Scene_center_latitude";
//        String sceneCenterLonName = "HDFEOS_POINTS_Scene_Header_Scene_center_longitude";
//
//        final MetadataElement globalAttributes = product.getMetadataRoot().getElement("Global_Attributes");
//        float projCentralLon = (float) globalAttributes.getAttribute(centralLonName).getData().getElemDouble();
//        float projCentralLat = (float) globalAttributes.getAttribute(centralLatName).getData().getElemDouble();
//        float sceneCenterLon = (float) globalAttributes.getAttribute(sceneCenterLonName).getData().getElemDouble();
//        float sceneCenterLat = (float) globalAttributes.getAttribute(sceneCenterLatName).getData().getElemDouble();
//        float east = (float) globalAttributes.getAttribute(westName).getData().getElemDouble();
//        float north = (float) globalAttributes.getAttribute(northName).getData().getElemDouble();

        float easting = (float) product.getMetadataRoot().getElement("Global_Attributes").getAttribute("geospatial_lon_max").getData().getElemDouble();
        float westing = (float) product.getMetadataRoot().getElement("Global_Attributes").getAttribute("geospatial_lon_min").getData().getElemDouble();
        float pixelSizeX = (easting - westing) / product.getSceneRasterWidth();
        float northing = (float) product.getMetadataRoot().getElement("Global_Attributes").getAttribute("geospatial_lat_max").getData().getElemDouble();
        float southing = (float) product.getMetadataRoot().getElement("Global_Attributes").getAttribute("geospatial_lat_min").getData().getElemDouble();
        float pixelSizeY = (northing - southing) / product.getSceneRasterHeight();
        // The projection information contained in the GOCI data file suggests slightly odd
        // Earth sphericity values, so not quite WGS84.  Using these, the transform needs the "lenient" keyword
        // set to true
        // the WKT used was derived from gdal/proj4 using the projection info (and ESRI format):
        // gdalsrsinfo -o wkt_esri -p "+proj=ortho +lon_0=130 +lat_0=36 +a=6378169.0 +b=6356584.0"
        //
        String wkt =
                "PROJCS[\"NSIDC Sea Ice Polar Stereographic North\","
                        + "GEOGCS[\"Unspecified datum based upon the Hughes 1980 ellipsoid\","
                        + "     DATUM[\"Not_specified_based_on_Hughes_1980_ellipsoid\","
                        + "         SPHEROID[\"Hughes 1980\",6378273,298.279411123061,"
                        + "             AUTHORITY[\"EPSG\",\"7058\"]],"
                        + "         AUTHORITY[\"EPSG\",\"6054\"]],"
                        + "     PRIMEM[\"Greenwich\",0,"
                        + "         AUTHORITY[\"EPSG\",\"8901\"]],"
                        + "     UNIT[\"degree\",0.01745329251994328,"
                        + "         AUTHORITY[\"EPSG\",\"9122\"]],"
                        + "     AUTHORITY[\"EPSG\",\"4054\"]],"
                        + "     UNIT[\"metre\",1,"
                        + "         AUTHORITY[\"EPSG\",\"9001\"]],"
                        + "PROJECTION[\"Polar_Stereographic\"],"
                        + "PARAMETER[\"latitude_of_origin\",70],"
                        + "PARAMETER[\"central_meridian\",-45],"
                        + "PARAMETER[\"scale_factor\",1],"
                        + "PARAMETER[\"false_easting\",0],"
                        + "PARAMETER[\"false_northing\",0],"
                        + "     AUTHORITY[\"EPSG\",\"3411\"]],";

//        String wkt =
//                "PROJCS[\"Orthographic\","
//                        + "GEOGCS[\"WGS 84\","
//                        + "  DATUM[\"WGS_1984\","
//                        + "     SPHEROID[\"WGS 84\",6378137,298.257223563,"
//                        + "         AUTHORITY[\"EPSG\",\"7030\"]],"
//                        + "     AUTHORITY[\"EPSG\",\"6326\"]],"
//                        + "  PRIMEM[\"Greenwich\",0,"
//                        + "     AUTHORITY[\"EPSG\",\"8901\"]],"
//                        + "  UNIT[\"degree\",0.0174532925199433,"
//                        + "     AUTHORITY[\"EPSG\",\"9122\"]],"
//                        + "  AUTHORITY[\"EPSG\",\"4326\"]]";

        // OK, using WGS84...seems to not make a hill 'o beans difference - but stickin' with the ugly.
//        String wkt =
//                "PROJCS[\"Orthographic\","
//                        + "  GEOGCS[\"GCS_unnamed ellipse\","
//                        + "    DATUM["+"\"D_unknown\","
//                        + "      SPHEROID[\"Unknown\",6378169,295.4908037989359]],"
//                        + "    PRIMEM[\"Greenwich\", 0.0],"
//                        + "    UNIT[\"Degree\", 0.017453292519943295]],"
//                        + "  PROJECTION[\"Orthographic\"],"
//                        + "  PARAMETER[\"Latitude_Of_Center\","+projCentralLat +"],"
//                        + "  PARAMETER[\"Longitude_Of_Center\","+ projCentralLon +"],"
//                        + "  PARAMETER[\"false_easting\", 0.0],"
//                        + "  PARAMETER[\"false_northing\", 0.0],"
//                        + "  UNIT[\"Meter\", 1]]";

        try {
            CoordinateReferenceSystem orthoCRS = CRS.parseWKT(wkt);
//            CoordinateReferenceSystem wgs84crs = org.geotools.referencing.crs.DefaultGeographicCRS.WGS84;
//            MathTransform transform = CRS.findMathTransform(wgs84crs, orthoCRS,true);
//            double[] sceneCenter = new double[2];
//            double [] sceneTopRight = new double[2];
////            transform.transform(new double[] {projCentralLon,projCentralLat}, 0, sceneCenter, 0, 1);
//            transform.transform(new double[] {sceneCenterLon,sceneCenterLat}, 0, sceneCenter, 0, 1);
//            transform.transform(new double[] {east,north}, 0, sceneTopRight, 0, 1);
//            double northing = Math.round(sceneTopRight[1]);
//            double easting = Math.round(sceneTopRight[0]);
//
//            double pixelSize = Math.round((sceneCenter[0]-sceneTopRight[0])/(product.getSceneRasterWidth()/2.0));

            product.setSceneGeoCoding(new CrsGeoCoding(orthoCRS,
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(),
                    easting, northing, pixelSizeX, pixelSizeY,
                    pixelX, pixelY));
        } catch (FactoryException e) {
            throw new IllegalStateException(e);
        } catch (TransformException e) {
            throw new IllegalStateException(e);
        }
    }

    private int getDimension(String dimensionName) {
        final List<Dimension> dimensions = ncFile.getDimensions();
        for (Dimension dimension : dimensions) {
            if (dimension.getShortName().equals(dimensionName)) {
                return dimension.getLength();
            }
        }
        return -1;
    }
}
