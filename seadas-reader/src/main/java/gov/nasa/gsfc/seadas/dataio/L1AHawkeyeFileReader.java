/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.nasa.gsfc.seadas.dataio;

import org.esa.snap.core.dataio.ProductIOException;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoCodingFactory;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.dataio.netcdf.util.NetcdfFileOpener;
import ucar.ma2.Array;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class L1AHawkeyeFileReader extends SeadasFileReader {

    private static final int[] HAWKEYE_WVL = new int[]{412, 447, 488, 510, 556, 670, 752, 867};

    protected String title;
    protected int numPixels = 0;
    protected int numScans = 0;

    L1AHawkeyeFileReader(SeadasProductReader productReader) {
        super(productReader);
    }

    @Override
    public Product createProduct() throws ProductIOException {

        String productName = productReader.getInputFile().getName();
        numPixels = getDimension("number_of_pixels");
        numScans = getDimension("number_of_scans");
        title = getStringAttribute("title");

        SeadasProductReader.ProductType productType = productReader.getProductType();

        Product product = new Product(productName, productType.toString(), numPixels, numScans);
        product.setDescription(productName);

        mustFlipX = mustFlipY = getDefaultFlip();
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

        variableMap = addHawkeyeBands(product, ncFile.findGroup("earth_view_data").getVariables());

        addGeocoding(product);

        // todo - think about maybe possibly sometime creating a flag for questionable data
        addFlagsAndMasks(product);

        return product;

    }
//
//    private float getScaleFactor(Variable variable) {
//        float scale_factor = 1.f;
//        final Attribute scale_factor_attribute = findAttribute("scale_factor", variable.getAttributes());
//        if (scale_factor_attribute != null) {
//            scale_factor = scale_factor_attribute.getNumericValue().floatValue();
//        }
//        return scale_factor;
//    }

    public void addGeocoding(final Product product) throws ProductIOException {

        // read latitudes and longitudes

        boolean externalGeo = false;
        NetcdfFile geoNcFile = null;
        Variable lats = null;
        Variable lons = null;
        int subSample = 1;
        float offsetX = 0f;
        float offsetY = 0f;
        try {
            File inputFile = productReader.getInputFile();
            String geoFileName = inputFile.getName().replaceAll("L1A", "GEO");
            String path = inputFile.getParent();
            File geocheck = new File(path, geoFileName);
            if (geocheck.exists()) {
                externalGeo = true;
                geoNcFile = NetcdfFileOpener.open(geocheck.getPath());


                lats = geoNcFile.findVariable("geolocation_data/latitude");
                lons = geoNcFile.findVariable("geolocation_data/longitude");

                //Use lat/lon with TiePointGeoCoding
                int[] dims = lats.getShape();
                float[] latTiePoints;
                float[] lonTiePoints;
                Array latarr = lats.read();
                Array lonarr = lons.read();

                latTiePoints = (float[]) latarr.getStorage();
                lonTiePoints = (float[]) lonarr.getStorage();

                Band latBand = new Band("latitude", ProductData.TYPE_FLOAT32, numPixels, numScans);
                Band lonBand = new Band("longitude", ProductData.TYPE_FLOAT32, numPixels, numScans);
                latBand.setNoDataValue(999.0);
                latBand.setNoDataValueUsed(true);
                lonBand.setNoDataValue(999.0);
                lonBand.setNoDataValueUsed(true);
                product.addBand(latBand);
                product.addBand(lonBand);

                ProductData lattitudes = ProductData.createInstance(latTiePoints);
                latBand.setData(lattitudes);
                ProductData longitudes = ProductData.createInstance(lonTiePoints);
                lonBand.setData(longitudes);
                product.setSceneGeoCoding(GeoCodingFactory.createPixelGeoCoding(latBand, lonBand, null, 10));

                geoNcFile.close();

            }

        } catch (Exception e) {
            throw new ProductIOException(e.getMessage());
        }

    }

    public Map<Band, Variable> addHawkeyeBands(Product product,
                                               List<Variable> variables) {
        final Map<Band, Variable> bandToVariableMap = new HashMap<Band, Variable>();
        int spectralBandIndex = 0;
        for (Variable variable : variables) {
            Band band = addNewBand(product, variable);
            if (band != null) {
                bandToVariableMap.put(band, variable);
            }
            if (spectralBandIndex < 8) {
                final float wavelength = Float.valueOf(HAWKEYE_WVL[spectralBandIndex]);
                band.setSpectralWavelength(wavelength);
                band.setSpectralBandIndex(spectralBandIndex++);
            }
        }
        return bandToVariableMap;
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


