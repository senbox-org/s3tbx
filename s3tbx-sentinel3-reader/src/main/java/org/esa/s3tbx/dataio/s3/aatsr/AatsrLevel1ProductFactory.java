package org.esa.s3tbx.dataio.s3.aatsr;

import com.bc.ceres.glevel.MultiLevelImage;
import org.esa.s3tbx.dataio.s3.Manifest;
import org.esa.s3tbx.dataio.s3.slstr.SlstrLevel1ProductFactory;
import org.esa.s3tbx.dataio.s3.util.MetTxReader;
import org.esa.s3tbx.dataio.s3.util.S3NetcdfReader;
import org.esa.snap.core.dataio.geocoding.ComponentFactory;
import org.esa.snap.core.dataio.geocoding.ComponentGeoCoding;
import org.esa.snap.core.dataio.geocoding.ForwardCoding;
import org.esa.snap.core.dataio.geocoding.GeoChecks;
import org.esa.snap.core.dataio.geocoding.GeoRaster;
import org.esa.snap.core.dataio.geocoding.InverseCoding;
import org.esa.snap.core.dataio.geocoding.forward.TiePointBilinearForward;
import org.esa.snap.core.dataio.geocoding.inverse.TiePointInverse;
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
        final int rasterWidth = sourceBand.getRasterWidth();
        final int subSamplingXY = 16;
        final int tpSceneWith = (rasterWidth - 1) * subSamplingXY;
        final int sceneRasterWidth = targetProduct.getSceneRasterWidth();
        final int diffX = tpSceneWith - sceneRasterWidth;
        final double offsetX = diffX / 2.0;
        final String unit = sourceBand.getUnit();

        final TiePointGrid tiePointGrid = new TiePointGrid(tpgName, sourceBand.getRasterWidth(), sourceBand.getRasterHeight(),
                                                           -offsetX, 0, subSamplingXY, subSamplingXY);
        if (unit != null && unit.toLowerCase().contains("degree")) {
            tiePointGrid.setDiscontinuity(TiePointGrid.DISCONT_AUTO);
        }
        tiePointGrid.setDescription(sourceBand.getDescription());
        tiePointGrid.setGeophysicalNoDataValue(sourceBand.getGeophysicalNoDataValue());
        tiePointGrid.setNoDataValueUsed(sourceBand.isNoDataValueUsed());
        tiePointGrid.setUnit(unit);
        targetProduct.addTiePointGrid(tiePointGrid);
        return tiePointGrid;
    }

    @Override
    protected void setGeoCoding(Product targetProduct) {
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
    }

    @Override
    protected void setTimeCoding(Product targetProduct) throws IOException {
        setTimeCoding(targetProduct, "time_in.nc", "time_stamp_i");
    }

//    @Override
//    protected void fixTiePointGrids(Product targetProduct) {
//
//        String[] ANGLE_NAMES = new String[]{
//                "sat_azimuth_tn",
//                "sat_path_tn", // todo - not an angle grid it is in meter
//                "sat_zenith_tn",
//                "solar_azimuth_tn",
//                "solar_path_tn", // todo - not an angle grid it is in meter
//                "solar_zenith_tn",
//                "sat_azimuth_to",
//                "sat_path_to", // todo - not an angle grid it is in meter
//                "sat_zenith_to",
//                "solar_azimuth_to",
//                "solar_path_to", // todo - not an angle grid it is in meter
//                "solar_zenith_to",
//        };
//
//        for (TiePointGrid grid : targetProduct.getTiePointGrids()) {
//            for (String angleName : ANGLE_NAMES) {
//                if (grid.getName().equals(angleName)) {
//                    TiePointGrid fixedGrid = getFixedAngleGrid(grid);
//                    targetProduct.getTiePointGridGroup().remove(grid);
//                    targetProduct.getTiePointGridGroup().add(fixedGrid);
//                }
//            }
//        }
//
//        TiePointGrid latGrid = targetProduct.getTiePointGrid("latitude_tx");
//        TiePointGrid lonGrid = targetProduct.getTiePointGrid("longitude_tx");
//
//        TiePointGrid fixedLatGrid = getFixedTiePointGrid(latGrid, false);
//        targetProduct.getTiePointGridGroup().remove(latGrid);
//        targetProduct.getTiePointGridGroup().add(fixedLatGrid);
//
//        TiePointGrid fixedLonGrid = getFixedTiePointGrid(lonGrid, false);
//        targetProduct.getTiePointGridGroup().remove(lonGrid);
//        targetProduct.getTiePointGridGroup().add(fixedLonGrid);
//    }
//
//    private TiePointGrid getFixedAngleGrid(TiePointGrid sourceGrid) {
//        // first, remove filled pixels at the end
//        final String gridName = sourceGrid.getName();
//        final boolean containsAngles = !gridName.contains("path");
//        TiePointGrid endFixedGrid = getFixedTiePointGrid(sourceGrid, containsAngles);
//        int gridWidth = endFixedGrid.getGridWidth() - 5;
//        int gridHeight = endFixedGrid.getGridHeight() - 1;
//
//        // second, copy values which are not fill value (everything apart from first 2 and last 3)
//        float[] originalTiePoints = endFixedGrid.getTiePoints();
//        float[] tiePoints = new float[gridWidth * gridHeight];
//
//        for (int y = 0; y < gridHeight; y++) {
//            System.arraycopy(originalTiePoints, 2 + endFixedGrid.getGridWidth() * y, tiePoints, gridWidth * y, gridWidth);
//        }
//
//        final TiePointGrid targetGrid = new TiePointGrid(gridName, gridWidth, gridHeight,
//                                                         sourceGrid.getOffsetX(), sourceGrid.getOffsetY(), sourceGrid.getSubSamplingX(), sourceGrid.getSubSamplingY(),
//                                                         tiePoints, containsAngles);
//        ProductUtils.copyRasterDataNodeProperties(sourceGrid, targetGrid);
//        return targetGrid;
//    }
//
//    private static TiePointGrid getFixedTiePointGrid(TiePointGrid grid, boolean isAngle) {
//        int firstFillIndex = -1;
//        int gridWidth = grid.getGridWidth();
//        float[] originalTiePoints = grid.getTiePoints();
//        for (int i = 0; i < originalTiePoints.length - 6; i++) {
//            if (isAngle) {
//                // check if 6 times fill value in a column: then cut at the end.
//                if (Math.abs(originalTiePoints[i] - ANGLE_FILL_VALUE) < 1E-2
//                        && Math.abs(originalTiePoints[i + 1] - ANGLE_FILL_VALUE) < 1E-2
//                        && Math.abs(originalTiePoints[i + 2] - ANGLE_FILL_VALUE) < 1E-2
//                        && Math.abs(originalTiePoints[i + 3] - ANGLE_FILL_VALUE) < 1E-2
//                        && Math.abs(originalTiePoints[i + 4] - ANGLE_FILL_VALUE) < 1E-2
//                        && Math.abs(originalTiePoints[i + 5] - ANGLE_FILL_VALUE) < 1E-2) {
//                    firstFillIndex = i;
//                    break;
//                }
//            } else {
//                if (originalTiePoints[i] == FILL_VALUE) {
//                    firstFillIndex = i;
//                    break;
//                }
//            }
//        }
//
//        if (firstFillIndex == -1) {
//            return grid;
//        } else {
//            int line = firstFillIndex / grid.getGridWidth();
//            int newHeight = line - 1;
//
//            float[] tiePoints = new float[gridWidth * newHeight];
//            System.arraycopy(originalTiePoints, 0, tiePoints, 0, tiePoints.length);
//            return new TiePointGrid(grid.getName(), gridWidth, newHeight, grid.getOffsetX(), grid.getOffsetY(), grid.getSubSamplingX(), grid.getSubSamplingY(), tiePoints, true);
//        }
//    }

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
