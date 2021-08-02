package org.esa.s3tbx.dataio.s3.aatsr;

import com.bc.ceres.glevel.MultiLevelImage;
import org.esa.s3tbx.dataio.s3.Manifest;
import org.esa.s3tbx.dataio.s3.slstr.SlstrLevel1ProductFactory;
import org.esa.s3tbx.dataio.s3.util.MetTxReader;
import org.esa.s3tbx.dataio.s3.util.S3NetcdfReader;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.TiePointGrid;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author Sabine Embacher
 * @author Thomas Storm
 */
public class AatsrLevel1ProductFactory extends SlstrLevel1ProductFactory {

    private Product masterProduct;

//    private static final double ANGLE_FILL_VALUE = 9969209968386869000000000000000000000.0;
//    private static final double FILL_VALUE = -1.0E9;

    public AatsrLevel1ProductFactory(AatsrLevel1ProductReader productReader) {
        super(productReader);
    }

    @Override
    protected List<String> getFileNames(Manifest manifest) {
        final List<String> fileNames = manifest.getFileNames(new String[0]);
        fileNames.sort(String::compareTo);
        return fileNames;
    }

    @Override
    protected Product readProduct(String fileName, Manifest manifest) throws IOException {
        final File file = new File(getInputFileParentDirectory(), fileName);
        if (!file.exists()) {
            return null;
        }
        final S3NetcdfReader reader;
        if (fileName.equals("met_tx.nc")) {
            reader = new MetTxReader();
        } else {
            reader = new AatsrS3NetcdfReader();
            addSeparatingDimensions(reader.getSuffixesForSeparatingDimensions());
        }
        return reader.readProductNodes(file, null);
    }

    @Override
    protected Product findMasterProduct() {
        if (masterProduct != null) {
            return masterProduct;
        }
        masterProduct = findMasterProduct(getOpenProductList());
        return masterProduct;
    }

    @Override
    protected void setUncertaintyBands(Product product) {
        final Band[] bands = product.getBands();
        for (Band band : bands) {
            final String bandName = band.getName();
            final int last_ = bandName.lastIndexOf("_");
            final String uncertaintyBandName = bandName.substring(0, last_) + "_uncert" + bandName.substring(last_);
            if (product.containsBand(uncertaintyBandName)) {
                final Band uncertaintyBand = product.getBand(uncertaintyBandName);
                band.addAncillaryVariable(uncertaintyBand, "uncertainty");
                addUncertaintyImageInfo(uncertaintyBand);
            }
        }
    }

    @Override
    protected String getProductSpecificMetadataElementName() {
        return "atsrProductInformation";
    }

    @Override
    protected void setAutoGrouping(Product[] sourceProducts, Product targetProduct) {
        String bandGrouping = getAutoGroupingString(sourceProducts);
        targetProduct.setAutoGrouping("radiance_uncert_in:BT_uncert_in:" +
                                              "radiance_in:BT_in:" +
                                              "radiance_uncert_io:BT_uncert_io:" +
                                              "radiance_io:BT_io:" +
                                              "exception_in:exception_io:" +
                                              "x_i:y_i:" +
                                              "elevation_i:latitude_i:longitude_i:" +
                                              "specific_humidity:temperature_profile:" +
                                              "cloud_in_:cloud_io_:" +
                                              "bayes_in_:bayes_io_:" +
                                              "bayes_in_:bayes_io_:" +
                                              "pointing_in_:pointing_io_:" +
                                              "confidence_in_:confidence_io_:" +
                                              bandGrouping);
    }

    @Override
    protected RasterDataNode copyTiePointGrid(Band sourceBand, Product targetProduct, double sourceStartOffset, double sourceTrackOffset, short[] sourceResolutions) {
        final MultiLevelImage sourceImage = sourceBand.getGeophysicalImage();
        final String tpgName = sourceBand.getName();
        putTiePointSourceImage(tpgName, sourceImage);
        // offset computation is done according to page 9-11 of (A)ATSR Expert Support Laboratory FAST Level 1b Product Definition (Ref: PO-TN-RAL-AT-0568 Issue: 1.4)
        // https://earth.esa.int/documents/700255/2482719/PO-TN-RAL-AT-0568+-+FAST+Level+1b+Product+Definition+Issue+1.4/ecb0344b-32c4-4172-b7a7-e32bddf40309
        final int subSamplingXY = 16;
        final float[] tiePointGridOffsets = getTiePointGridOffsets(sourceStartOffset, sourceTrackOffset, subSamplingXY, subSamplingXY);
        final TiePointGrid tiePointGrid = new TiePointGrid(tpgName, sourceBand.getRasterWidth(), sourceBand.getRasterHeight(),
                                                           tiePointGridOffsets[0] + 0.5, tiePointGridOffsets[1] + 0.5, subSamplingXY, subSamplingXY);

        final String unit = sourceBand.getUnit();
        tiePointGrid.setUnit(unit);
        if (unit != null && unit.toLowerCase().contains("degree")) {
            tiePointGrid.setDiscontinuity(TiePointGrid.DISCONT_AUTO);
        }
        tiePointGrid.setDescription(sourceBand.getDescription());
        if (("latitude_tx".equals(tpgName) || "longitude_tx".equals(tpgName)) && !sourceBand.isNoDataValueUsed()) {
            tiePointGrid.setGeophysicalNoDataValue(-9.99999999E8);
            tiePointGrid.setNoDataValueUsed(true);
        } else {
            tiePointGrid.setGeophysicalNoDataValue(sourceBand.getGeophysicalNoDataValue());
            tiePointGrid.setNoDataValueUsed(sourceBand.isNoDataValueUsed());
        }
        targetProduct.addTiePointGrid(tiePointGrid);
        return tiePointGrid;
    }

    @Override
    protected void setGeoCoding(Product targetProduct) {
// TODO https://senbox.atlassian.net/browse/SIIITBX-394
// Not setting GeoCoding. We can't get a good geolocation with the tie-points and the geo-location bands are not usable
// because they contain fill_values.
/*
        final String lonVariableName = "longitude_tx";
        final String latVariableName = "latitude_tx";
        final TiePointGrid lonGrid = targetProduct.getTiePointGrid(lonVariableName);
        final TiePointGrid latGrid = targetProduct.getTiePointGrid(latVariableName);
        if (lonGrid == null || latGrid == null) {
            return;
        }

        final double[] longitudes = loadTiePointData(lonVariableName);
        final double[] latitudes = loadTiePointData(latVariableName);
        final GeoRaster geoRaster = new GeoRaster(longitudes, latitudes, lonVariableName, latVariableName,
                                                  lonGrid.getGridWidth(), lonGrid.getGridHeight(),
                                                  targetProduct.getSceneRasterWidth(), targetProduct.getSceneRasterHeight(), 1.0,
                                                  lonGrid.getOffsetX(), lonGrid.getOffsetY(),
                                                  lonGrid.getSubSamplingX(), lonGrid.getSubSamplingY());

        final ForwardCoding forward = ComponentFactory.getForward(TiePointBilinearForward.KEY);
        final InverseCoding inverse = ComponentFactory.getInverse(TiePointInverse.KEY);

        final ComponentGeoCoding sceneGeoCoding = new ComponentGeoCoding(geoRaster, forward, inverse, GeoChecks.ANTIMERIDIAN);
        sceneGeoCoding.initialize();

        targetProduct.setSceneGeoCoding(sceneGeoCoding);
*/
    }

    @Override
    protected void setBandGeoCodings(Product product) throws IOException {
        // empty implementation to prevent using pixel-based GeoCodings for AATSR.
        // They are not usable due to no-data in the bands.
    }

    @Override
    protected void setTimeCoding(Product targetProduct) throws IOException {
        setTimeCoding(targetProduct, "time_in.nc", "time_stamp_i");
    }

    protected short[] getResolutions(String gridIndex) {
        short[] resolutions;
        if (gridIndex.startsWith("i")) {
            resolutions = new short[]{1000, 1000};
        } else if (gridIndex.startsWith("t")) {
            resolutions = new short[]{16000, 16000};
        } else {
            resolutions = new short[]{500, 500};
        }
        return resolutions;
    }

    static Product findMasterProduct(List<Product> openProductList) {
        int maxWidth = 0;
        int maxHeight = 0;
        Product masterProduct = null;
        for (Product product : openProductList) {
            final int width = product.getSceneRasterWidth();
            final int height = product.getSceneRasterHeight();
            if (width > maxWidth || height > maxHeight) {
                masterProduct = product;
                maxWidth = Math.max(maxWidth, width);
                maxHeight = Math.max(maxHeight, height);
            }
        }
        return masterProduct;
    }
}
