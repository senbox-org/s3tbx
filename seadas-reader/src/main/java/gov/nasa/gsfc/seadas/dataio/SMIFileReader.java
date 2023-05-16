package gov.nasa.gsfc.seadas.dataio;

import org.esa.snap.core.dataio.ProductIOException;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
import ucar.ma2.Array;
import ucar.ma2.Index;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Group;
import ucar.nc2.Variable;

import java.awt.Dimension;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.*;

/**
 * Reader for "SMI-like" file formats
 */
public class SMIFileReader extends SeadasFileReader {

    SMIFileReader(SeadasProductReader productReader) {
        super(productReader);
    }

    @Override
    public Product createProduct() throws ProductIOException {

        int[] dims;
        int sceneHeight = 0;
        int sceneWidth = 0;
        Group geodata = ncFile.findGroup("geophysical_data");
        if (geodata == null) {
            geodata = ncFile.findGroup("Geophysical_Data");
        }
        if (productReader.getProductType() == SeadasProductReader.ProductType.OISST) {
            dims = ncFile.getVariables().get(4).getShape();
            sceneHeight = dims[2];
            sceneWidth = dims[3];
            mustFlipY = true;
        } else if (productReader.getProductType() == SeadasProductReader.ProductType.ANCCLIM) {
            List<Variable> vars = ncFile.getVariables();
            for (Variable v : vars) {
                if (v.getRank() == 2) {
                    dims = v.getShape();
                    sceneHeight = dims[0];
                    sceneWidth = dims[1];
                }
            }
        } else {
            if (geodata != null){
                dims = geodata.getVariables().get(0).getShape();
                sceneHeight = dims[0];
                sceneWidth = dims[1];
            } else {
                ucar.nc2.Dimension latdim = ncFile.findDimension("lat");
                ucar.nc2.Dimension londim = ncFile.findDimension("lon");
                if (latdim != null) {
                    sceneHeight = latdim.getLength();
                    sceneWidth = londim.getLength();
                } else {
                    dims = ncFile.getVariables().get(0).getShape();
                    sceneHeight = dims[0];
                    sceneWidth = dims[1];
                }
            }
        }

        String productName = productReader.getInputFile().getName();
        try {
            productName = getStringAttribute("Product_Name");
        } catch (Exception ignored) {

        }

        SeadasProductReader.ProductType productType = productReader.getProductType();

        Product product = new Product(productName, productType.toString(), sceneWidth, sceneHeight);
        product.setDescription(productName);

        product.setFileLocation(productReader.getInputFile());
        product.setProductReader(productReader);

        setStartTime(product);
        setEndTime(product);

        addGlobalMetadata(product);
        addSmiMetadata(product);
//        variableMap = addBands(product, ncFile.getVariables());
        variableMap = addSmiBands(product, ncFile.getVariables());
        try {
            addGeocoding(product);
        } catch (Exception ignored) {
        }
        addFlagsAndMasks(product);
        if (productReader.getProductType() == SeadasProductReader.ProductType.Bathy) {
            mustFlipY = true;
            Dimension tileSize = new Dimension(640, 320);
            product.setPreferredTileSize(tileSize);
        }
        product.setAutoGrouping("Rrs:nLw:Lt:La:Lr:Lw:L_q:L_u:Es:TLg:rhom:rhos:rhot:Taua:Kd:aot:adg:aph_:bbp:vgain:BT:tg_sol:tg_sen");
        return product;
    }

    protected Map<Band, Variable> addSmiBands(Product product, List<Variable> variables) {
        final int sceneRasterWidth = product.getSceneRasterWidth();
        final int sceneRasterHeight = product.getSceneRasterHeight();
        Map<Band, Variable> bandToVariableMap = new HashMap<>();
        for (Variable variable : variables) {
            int variableRank = variable.getRank();
            if (variableRank == 2) {
                final int[] dimensions = variable.getShape();
                final int height = dimensions[0];
                final int width = dimensions[1];
                if (height == sceneRasterHeight && width == sceneRasterWidth) {
                    String name = variable.getShortName();
                    if (name.equals("l3m_data")) {
                        try {
                            name = getStringAttribute("Parameter") + " " + getStringAttribute("Measure");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    final int dataType = getProductDataType(variable);
                    final Band band = new Band(name, dataType, width, height);
//                    band = new Band(name, dataType, width, height);

                    product.addBand(band);

                    try {
                        Attribute fillvalue = variable.findAttribute("_FillValue");
                        if (fillvalue == null) {
                            fillvalue = variable.findAttribute("Fill");
                        }
                        if (fillvalue != null) {
                            band.setNoDataValue(fillvalue.getNumericValue().doubleValue());
                            band.setNoDataValueUsed(true);
                        }
                    } catch (Exception ignored) {

                    }
                    bandToVariableMap.put(band, variable);
                    // Set units, if defined
                    try {
                        band.setUnit(getStringAttribute("Units"));
                    } catch (Exception ignored) {

                    }

                    final List<Attribute> list = variable.getAttributes();
                    double[] validMinMax = {0.0,0.0};
                    for (Attribute hdfAttribute : list) {
                        final String attribName = hdfAttribute.getShortName();
                        if ("units".equals(attribName)) {
                            band.setUnit(hdfAttribute.getStringValue());
                        } else if ("long_name".equalsIgnoreCase(attribName)) {
                            band.setDescription(hdfAttribute.getStringValue());
                        } else if ("slope".equalsIgnoreCase(attribName)) {
                            band.setScalingFactor(hdfAttribute.getNumericValue(0).doubleValue());
                        } else if ("intercept".equalsIgnoreCase(attribName)) {
                            band.setScalingOffset(hdfAttribute.getNumericValue(0).doubleValue());
                        } else if ("scale_factor".equals(attribName)) {
                            band.setScalingFactor(hdfAttribute.getNumericValue(0).doubleValue());
                        } else if ("add_offset".equals(attribName)) {
                            band.setScalingOffset(hdfAttribute.getNumericValue(0).doubleValue());
                        } else if (attribName.startsWith("valid_")){
                            if ("valid_min".equals(attribName)){
                                validMinMax[0] = hdfAttribute.getNumericValue(0).doubleValue();
                            } else if ("valid_max".equals(attribName)){
                                validMinMax[1] = hdfAttribute.getNumericValue(0).doubleValue();
                            } else if ("valid_range".equals(attribName)){
                                validMinMax[0] = hdfAttribute.getNumericValue(0).doubleValue();
                                validMinMax[1] = hdfAttribute.getNumericValue(1).doubleValue();
                            }
                        }
                    }
                    if (validMinMax[0] != validMinMax[1]){
                        double[] minmax = {0.0,0.0};
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

                        String validExp = format("%s >= %.2f && %s <= %.2f", name, minmax[0], name, minmax[1]);
                        band.setValidPixelExpression(validExp);//.format(name, validMinMax[0], name, validMinMax[1]));
                    }
                }
            } else if (variableRank == 4) {
                final int[] dimensions = variable.getShape();
                final int height = dimensions[2];
                final int width = dimensions[3];
                if (height == sceneRasterHeight && width == sceneRasterWidth) {
                    String name = variable.getShortName();

                    final int dataType = getProductDataType(variable);
                    final Band band = new Band(name, dataType, width, height);
//                    band = new Band(name, dataType, width, height);

                    Variable sliced = null;
                    try {
                        sliced = variable.slice(0, 0).slice(0, 0);
                    } catch (InvalidRangeException e) {
                        e.printStackTrace();  //Todo change body of catch statement.
                    }

                    bandToVariableMap.put(band, sliced);
                    product.addBand(band);

                    try {
                        Attribute fillvalue = variable.findAttribute("_FillValue");
                        if (fillvalue != null) {
                            band.setNoDataValue(fillvalue.getNumericValue().doubleValue());
                            band.setNoDataValueUsed(true);
                        }
                    } catch (Exception ignored) {

                    }
                    // Set units, if defined
                    try {
                        band.setUnit(getStringAttribute("units"));
                    } catch (Exception ignored) {

                    }

                    final List<Attribute> list = variable.getAttributes();
                    for (Attribute hdfAttribute : list) {
                        final String attribName = hdfAttribute.getShortName();
                        if ("scale_factor".equals(attribName)) {
                            band.setScalingFactor(hdfAttribute.getNumericValue(0).doubleValue());
                        } else if ("add_offset".equals(attribName)) {
                            band.setScalingOffset(hdfAttribute.getNumericValue(0).doubleValue());
                        }
                    }
                }
            }
        }
        return bandToVariableMap;
    }

    public void addGeocoding(Product product) throws IOException {

        double pixelX = 0.5;
        double pixelY = 0.5;
        double easting;
        double northing;
        double pixelSizeX;
        double pixelSizeY;
        boolean pixelRegistered = true;
        if (productReader.getProductType() == SeadasProductReader.ProductType.ANCNRT) {
            pixelRegistered = false;
        }
        if (productReader.getProductType() == SeadasProductReader.ProductType.OISST) {

            Variable lon = ncFile.findVariable("lon");
            Variable lat = ncFile.findVariable("lat");
            Array lonData = lon.read();
            // TODO: handle the 180 degree shift with the NOAA products - need to modify  SeaDasFileReader:readBandData
// Below is a snippet from elsewhere in BEAM that deals with this issue...
// SPECIAL CASE: check if we have a global geographic lat/lon with lon from 0..360 instead of -180..180
//            if (isShifted180(lonData)) {
//                // if this is true, subtract 180 from all longitudes and
//                // add a global attribute which will be analyzed when setting up the image(s)
//                final List<Variable> variables = ncFile.getVariables();
//                for (Variable next : variables) {
//                    next.getAttributes().add(new Attribute("LONGITUDE_SHIFTED_180", 1));
//                }
//                for (int i = 0; i < lonData.getSize(); i++) {
//                    final Index ii = lonData.getIndex().set(i);
//                    final double theLon = lonData.getDouble(ii) - 180.0;
//                    lonData.setDouble(ii, theLon);
//                }
//            }
            final Array latData = lat.read();

            final int lonSize = lon.getShape(0);
            final Index i0 = lonData.getIndex().set(0);
            final Index i1 = lonData.getIndex().set(lonSize - 1);


            pixelSizeX = (lonData.getDouble(i1) - lonData.getDouble(i0)) / (product.getSceneRasterWidth() - 1);
            easting = lonData.getDouble(i0);

            final int latSize = lat.getShape(0);
            final Index j0 = latData.getIndex().set(0);
            final Index j1 = latData.getIndex().set(latSize - 1);
            pixelSizeY = (latData.getDouble(j1) - latData.getDouble(j0)) / (product.getSceneRasterHeight() - 1);

            // this should be the 'normal' case
            if (pixelSizeY < 0) {
                pixelSizeY = -pixelSizeY;
                northing = latData.getDouble(latData.getIndex().set(0));
            } else {
                northing = latData.getDouble(latData.getIndex().set(latSize - 1));
            }
            northing -= pixelSizeX / 2.0;
            easting += pixelSizeY / 2.0;

            try {
                product.setSceneGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84,
                                                           product.getSceneRasterWidth(),
                                                           product.getSceneRasterHeight(),
                                                           easting, northing,
                                                           pixelSizeX, pixelSizeY,
                                                           pixelX, pixelY));
            } catch (FactoryException | TransformException e) {
                throw new IllegalStateException(e);
            }

        } else {


            String east = "Easternmost_Longitude";
            String west = "Westernmost_Longitude";
            String north = "Northernmost_Latitude";
            String south = "Southernmost_Latitude";
            Attribute latmax = ncFile.findGlobalAttributeIgnoreCase("geospatial_lat_max");
            if (latmax != null) {
                east = "geospatial_lon_max";
                west = "geospatial_lon_min";
                north = "geospatial_lat_max";
                south = "geospatial_lat_min";
            } else {
                latmax = ncFile.findGlobalAttributeIgnoreCase("upper_lat");
                if (latmax != null) {
                    east = "right_lon";
                    west = "left_lon";
                    north = "upper_lat";
                    south = "lower_lat";
                }
            }

            final MetadataElement globalAttributes = product.getMetadataRoot().getElement("Global_Attributes");
            easting = (float) globalAttributes.getAttribute(east).getData().getElemDouble();
            float westing = (float) globalAttributes.getAttribute(west).getData().getElemDouble();
            pixelSizeX = Math.abs(easting - westing) / product.getSceneRasterWidth();
            northing = (float) globalAttributes.getAttribute(north).getData().getElemDouble();
            float southing = (float) globalAttributes.getAttribute(south).getData().getElemDouble();
            if (northing < southing) {
                mustFlipY = true;
                northing = (float) globalAttributes.getAttribute(south).getData().getElemDouble();
                southing = (float) globalAttributes.getAttribute(north).getData().getElemDouble();
            }
            pixelSizeY = Math.abs(northing - southing) / product.getSceneRasterHeight();
            if (pixelRegistered) {
                northing -= pixelSizeY / 2.0;
                westing += pixelSizeX / 2.0;
            } else {
                pixelX = 0.0;
                pixelY = 0.0;
            }
            try {
                product.setSceneGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84,
                                                           product.getSceneRasterWidth(),
                                                           product.getSceneRasterHeight(),
                                                           westing, northing,
                                                           pixelSizeX, pixelSizeY,
                                                           pixelX, pixelY));
            } catch (FactoryException | TransformException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private void setEndTime(Product product) {
        ProductData.UTC coverageEndTime = getUTCAttribute("time_coverage_end");
        if(coverageEndTime != null) {
            product.setEndTime(coverageEndTime);
        }
    }

    private void setStartTime(Product product) {
        ProductData.UTC coverageStartTime = getUTCAttribute("time_coverage_start");
        if(coverageStartTime != null) {
            product.setStartTime(coverageStartTime);
        }
    }

    private static boolean isShifted180(Array lonData) {
        final Index i0 = lonData.getIndex().set(0);
        final Index i1 = lonData.getIndex().set(1);
        final Index iN = lonData.getIndex().set((int) lonData.getSize() - 1);
        double lonDelta = (lonData.getDouble(i1) - lonData.getDouble(i0));

        return (lonData.getDouble(0) < lonDelta && lonData.getDouble(iN) > 360.0 - lonDelta);
    }

    public void addSmiMetadata(final Product product) {
//        Variable l3mvar = ncFile.findVariable("l3m_data");
        final MetadataElement bandAttributes = new MetadataElement("Band_Attributes");
        List<Variable> variables = ncFile.getVariables();
        for (Variable variable : variables) {
            final String name = variable.getShortName();
            final MetadataElement sdsElement = new MetadataElement(name);
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

    @Override
    protected void addFlagsAndMasks(Product product) {
        Band QFBand = product.getBand("l3m_qual");
        if (QFBand != null) {
            FlagCoding flagCoding = new FlagCoding("SST_Quality");
            flagCoding.addFlag("Best", 0x00, "Highest quality retrieval");
            flagCoding.addFlag("Good", 0x01, "Good quality retrieval");
            flagCoding.addFlag("Questionable", 0x02, "Questionable quality retrieval");
            flagCoding.addFlag("Bad", 0x03, "Bad quality retrieval");
            product.getFlagCodingGroup().add(flagCoding);
            QFBand.setSampleCoding(flagCoding);

            product.getMaskGroup().add(Mask.BandMathsType.create("Best", "Highest quality retrieval",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "l3m_qual == 0",
                    SeadasFileReader.Cornflower, 0.6));
            product.getMaskGroup().add(Mask.BandMathsType.create("Good", "Good quality retrieval",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "l3m_qual == 1",
                    SeadasFileReader.LightPurple, 0.6));
            product.getMaskGroup().add(Mask.BandMathsType.create("Questionable", "Questionable quality retrieval",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "l3m_qual == 2",
                    SeadasFileReader.BurntUmber, 0.6));
            product.getMaskGroup().add(Mask.BandMathsType.create("Bad", "Bad quality retrieval",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "l3m_qual == 3",
                    SeadasFileReader.FailRed, 0.6));

        }
        QFBand = product.getBand("qual_sst");
        if (QFBand != null) {
            FlagCoding flagCoding = new FlagCoding("SST_Quality");
            flagCoding.addFlag("Best", 0x00, "Highest quality retrieval");
            flagCoding.addFlag("Good", 0x01, "Good quality retrieval");
            flagCoding.addFlag("Questionable", 0x02, "Questionable quality retrieval");
            flagCoding.addFlag("Bad", 0x03, "Bad quality retrieval");
            product.getFlagCodingGroup().add(flagCoding);
            QFBand.setSampleCoding(flagCoding);

            product.getMaskGroup().add(Mask.BandMathsType.create("Best", "Highest quality retrieval",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "qual_sst == 0",
                    SeadasFileReader.Cornflower, 0.6));
            product.getMaskGroup().add(Mask.BandMathsType.create("Good", "Good quality retrieval",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "qual_sst == 1",
                    SeadasFileReader.LightPurple, 0.6));
            product.getMaskGroup().add(Mask.BandMathsType.create("Questionable", "Questionable quality retrieval",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "qual_sst == 2",
                    SeadasFileReader.BurntUmber, 0.6));
            product.getMaskGroup().add(Mask.BandMathsType.create("Bad", "Bad quality retrieval",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "qual_sst == 3",
                    SeadasFileReader.FailRed, 0.6));
            product.getMaskGroup().add(Mask.BandMathsType.create("No Data", "No data retrieval",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "qual_sst == -1",
                    SeadasFileReader.MediumGray, 0.6));
        }
        QFBand = product.getBand("qual_sst4");
        if (QFBand != null) {
            FlagCoding flagCoding = new FlagCoding("SST_Quality");
            flagCoding.addFlag("Best", 0x00, "Highest quality retrieval");
            flagCoding.addFlag("Good", 0x01, "Good quality retrieval");
            flagCoding.addFlag("Questionable", 0x02, "Questionable quality retrieval");
            flagCoding.addFlag("Bad", 0x03, "Bad quality retrieval");
            product.getFlagCodingGroup().add(flagCoding);
            QFBand.setSampleCoding(flagCoding);

            product.getMaskGroup().add(Mask.BandMathsType.create("Best", "Highest quality retrieval",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "qual_sst4 == 0",
                    SeadasFileReader.Cornflower, 0.6));
            product.getMaskGroup().add(Mask.BandMathsType.create("Good", "Good quality retrieval",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "qual_sst4 == 1",
                    SeadasFileReader.LightPurple, 0.6));
            product.getMaskGroup().add(Mask.BandMathsType.create("Questionable", "Questionable quality retrieval",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "qual_sst4 == 2",
                    SeadasFileReader.BurntUmber, 0.6));
            product.getMaskGroup().add(Mask.BandMathsType.create("Bad", "Bad quality retrieval",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "qual_sst4 == 3",
                    SeadasFileReader.FailRed, 0.6));
            product.getMaskGroup().add(Mask.BandMathsType.create("No Data", "No data retrieval",
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(), "qual_sst4 == -1",
                    SeadasFileReader.MediumGray, 0.6));
        }
    }
}