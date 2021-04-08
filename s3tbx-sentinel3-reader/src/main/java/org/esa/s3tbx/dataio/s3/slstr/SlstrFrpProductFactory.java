package org.esa.s3tbx.dataio.s3.slstr;

import com.bc.ceres.glevel.support.DefaultMultiLevelImage;
import com.bc.ceres.glevel.support.DefaultMultiLevelModel;
import com.bc.ceres.glevel.support.DefaultMultiLevelSource;
import org.esa.s3tbx.dataio.s3.Manifest;
import org.esa.s3tbx.dataio.s3.Sentinel3ProductReader;
import org.esa.snap.core.dataio.geocoding.*;
import org.esa.snap.core.dataio.geocoding.forward.PixelForward;
import org.esa.snap.core.dataio.geocoding.forward.TiePointBilinearForward;
import org.esa.snap.core.dataio.geocoding.inverse.PixelQuadTreeInverse;
import org.esa.snap.core.dataio.geocoding.inverse.TiePointInverse;
import org.esa.snap.core.dataio.geocoding.util.RasterUtils;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.runtime.Config;

import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

public class SlstrFrpProductFactory extends SlstrProductFactory {

    private static final double RESOLUTION_IN_KM = 1.0;
    private final static String SYSPROP_SLSTR_FRP_PIXEL_CODING_INVERSE = "s3tbx.reader.slstr.frp.pixelGeoCoding.inverse";
    private final static String SYSPROP_SLSTR_FRP_TIE_POINT_CODING_FORWARD = "s3tbx.reader.slstr.frp.tiePointGeoCoding.forward";

    private Map<String, GeoCoding> geoCodingMap;
    private final Map<String, Double> gridIndexToTrackOffset;
    private final Map<String, Double> gridIndexToStartOffset;

    private final static String LATITUDE_TPG_NAME = "latitude_tx";
    private final static String LONGITUDE_TPG_NAME = "longitude_tx";

    public SlstrFrpProductFactory(Sentinel3ProductReader productReader) {
        super(productReader);
        geoCodingMap = new HashMap<>();
        gridIndexToTrackOffset = new HashMap<>();
        gridIndexToStartOffset = new HashMap<>();
    }

    protected void addProductSpecificMetadata(Product targetProduct) {
        super.addProductSpecificMetadata(targetProduct);
        List<Product> openProductList = getOpenProductList();
        for (final Product p : openProductList) {
            String identifier = getGridIndex(p.getName());
            MetadataElement globalAttributes = p.getMetadataRoot().getElement("Global_Attributes");
            if (!gridIndexToStartOffset.containsKey(identifier)) {
                gridIndexToStartOffset.put(identifier, globalAttributes.getAttributeDouble("start_offset"));
            }
            if (!gridIndexToTrackOffset.containsKey(identifier)) {
                gridIndexToTrackOffset.put(identifier, globalAttributes.getAttributeDouble("track_offset"));
            }
            if (identifier.equals("in")) {
                setReferenceStartOffset(getStartOffset(identifier));
                setReferenceTrackOffset(getTrackOffset(identifier));
                setReferenceResolutions(getResolutions(identifier));
            }
        }
    }

    @Override
    protected Product findMasterProduct() {
        List<Product> openProductList = getOpenProductList();
        for (final Product p : openProductList) {
            if (p.getName().endsWith("in")) {
                return p;
            }
        }
        return null;
    }

    @Override
    protected Double getStartOffset(String gridIndex) {
        if (gridIndexToStartOffset.containsKey(gridIndex)) {
            return gridIndexToStartOffset.get(gridIndex);
        }
        return 0.0;
    }

    @Override
    protected Double getTrackOffset(String gridIndex) {
        if (gridIndexToTrackOffset.containsKey(gridIndex)) {
            return gridIndexToTrackOffset.get(gridIndex);
        }
        return 0.0;
    }

    @Override
    protected List<String> getFileNames(Manifest manifest) {
        return manifest.getFileNames("");
    }

    @Override
    protected void setBandGeoCodings(Product targetProduct) throws IOException {
        final Band[] bands = targetProduct.getBands();
        for (Band band : bands) {
            final GeoCoding bandGeoCoding = getBandGeoCoding(targetProduct, getFrpGridIndex(band));
            band.setGeoCoding(bandGeoCoding);
        }
        final ProductNodeGroup<Mask> maskGroup = targetProduct.getMaskGroup();
        for (int i = 0; i < maskGroup.getNodeCount(); i++) {
            final Mask mask = maskGroup.get(i);
            final GeoCoding bandGeoCoding = getBandGeoCoding(targetProduct, getFrpGridIndex(mask));
            mask.setGeoCoding(bandGeoCoding);
        }
    }

    private static String getFrpGridIndex(Band band) {
        // todo wait for name change. If this happens, we can merge this with the method from SlstrLevel1ProductFactory
        String bandName = band.getName();
        String bandNameStart = bandName.split("_")[0];
        if (bandNameStart.equals("flags")) {
            return  "in";
        }
        return getGridIndex(bandName);
    }

    @Override
    protected RasterDataNode addSpecialNode(Product masterProduct, Band sourceBand, Product targetProduct) {
        final String sourceBandName = sourceBand.getName();
        String gridIndex = getFrpGridIndex(sourceBand);
        final Double sourceStartOffset = getStartOffset(gridIndex);
        final Double sourceTrackOffset = getTrackOffset(gridIndex);
        if (sourceStartOffset != null && sourceTrackOffset != null) {
            final short[] sourceResolutions = getResolutions(gridIndex);
            if (gridIndex.startsWith("t")) {
                return copyTiePointGrid(sourceBand, targetProduct, sourceStartOffset, sourceTrackOffset, sourceResolutions);
            } else {
                final Band targetBand = new Band(sourceBandName, sourceBand.getDataType(),
                        sourceBand.getRasterWidth(), sourceBand.getRasterHeight());
                targetProduct.addBand(targetBand);
                ProductUtils.copyRasterDataNodeProperties(sourceBand, targetBand);
                final RenderedImage sourceRenderedImage = sourceBand.getSourceImage().getImage(0);
                //todo remove commented lines when resampling works with scenetransforms
                //if pixel band geo-codings are used, scenetransforms are set
//                if (Config.instance("s3tbx").load().preferences().getBoolean(SLSTR_L1B_USE_PIXELGEOCODINGS, false)) {
//                    targetBand.setSourceImage(sourceRenderedImage);
//                } else {
                final AffineTransform imageToModelTransform = new AffineTransform();
                final float[] offsets = getOffsets(sourceStartOffset, sourceTrackOffset, sourceResolutions);
                imageToModelTransform.translate(offsets[0], offsets[1]);
                final short[] referenceResolutions = getReferenceResolutions();
                final int subSamplingX = sourceResolutions[0] / referenceResolutions[0];
                final int subSamplingY = sourceResolutions[1] / referenceResolutions[1];
                imageToModelTransform.scale(subSamplingX, subSamplingY);
                final DefaultMultiLevelModel targetModel =
                        new DefaultMultiLevelModel(imageToModelTransform,
                                sourceRenderedImage.getWidth(), sourceRenderedImage.getHeight());
                final DefaultMultiLevelSource targetMultiLevelSource =
                        new DefaultMultiLevelSource(sourceRenderedImage, targetModel);
                targetBand.setSourceImage(new DefaultMultiLevelImage(targetMultiLevelSource));
//                }
                return targetBand;
            }
        }
        return sourceBand;
    }

    @Override
    protected void setGeoCoding(Product targetProduct) {
        final TiePointGrid lonGrid = targetProduct.getTiePointGrid(LONGITUDE_TPG_NAME);
        final TiePointGrid latGrid = targetProduct.getTiePointGrid(LATITUDE_TPG_NAME);

        if (latGrid == null || lonGrid == null) {
            return;
        }

        final double[] longitudes = loadTiePointData(LONGITUDE_TPG_NAME);
        final double[] latitudes = loadTiePointData(LATITUDE_TPG_NAME);

        final GeoRaster geoRaster = new GeoRaster(longitudes, latitudes, LONGITUDE_TPG_NAME, LATITUDE_TPG_NAME,
                lonGrid.getGridWidth(), lonGrid.getGridHeight(),
                targetProduct.getSceneRasterWidth(), targetProduct.getSceneRasterHeight(), RESOLUTION_IN_KM,
                lonGrid.getOffsetX(), lonGrid.getOffsetY(),
                lonGrid.getSubSamplingX(), lonGrid.getSubSamplingY());

        final Preferences preferences = Config.instance("s3tbx").preferences();
        final String forwardKey = preferences.get(SYSPROP_SLSTR_FRP_TIE_POINT_CODING_FORWARD, TiePointBilinearForward.KEY);
        final ForwardCoding forward = ComponentFactory.getForward(forwardKey);
        final InverseCoding inverse = ComponentFactory.getInverse(TiePointInverse.KEY);

        final ComponentGeoCoding geoCoding = new ComponentGeoCoding(geoRaster, forward, inverse, GeoChecks.POLES);
        geoCoding.initialize();

        targetProduct.setSceneGeoCoding(geoCoding);
    }

    private GeoCoding getBandGeoCoding(Product product, String end) throws IOException {
        if (geoCodingMap.containsKey(end)) {
            return geoCodingMap.get(end);
        } else {
            // @todo 1 tb/tb extract method for switch and test 2020-02-14
            String lonVariableName;
            String latVariableName;
            switch (end) {
                case "in":
                    lonVariableName = "longitude_in";
                    latVariableName = "latitude_in";
                    break;
                case "fn":
                    lonVariableName = "longitude_fn";
                    latVariableName = "latitude_fn";
                    break;
                default:
                    return null;
            }

            final Band lonBand = product.getBand(lonVariableName);
            final Band latBand = product.getBand(latVariableName);

            if (latBand != null && lonBand != null) {
                final double[] longitudes = RasterUtils.loadDataScaled(lonBand);
                lonBand.unloadRasterData();
                final double[] latitudes = RasterUtils.loadDataScaled(latBand);
                latBand.unloadRasterData();

                final int sceneRasterWidth = product.getSceneRasterWidth();
                final int sceneRasterHeight = product.getSceneRasterHeight();
                final GeoRaster geoRaster = new GeoRaster(longitudes, latitudes, lonVariableName, latVariableName,
                        sceneRasterWidth, sceneRasterHeight, RESOLUTION_IN_KM);

                final Preferences preferences = Config.instance("s3tbx").preferences();
                final String inverseKey = preferences.get(SYSPROP_SLSTR_FRP_PIXEL_CODING_INVERSE, PixelQuadTreeInverse.KEY);
                final String[] keys = getForwardAndInverseKeys_pixelCoding(inverseKey);
                final ForwardCoding forward = ComponentFactory.getForward(keys[0]);
                final InverseCoding inverse = ComponentFactory.getInverse(keys[1]);

                final ComponentGeoCoding geoCoding = new ComponentGeoCoding(geoRaster, forward, inverse, GeoChecks.POLES);
                geoCoding.initialize();
                geoCodingMap.put(end, geoCoding);
                return geoCoding;
            }
        }
        return null;
    }

    @Override
    protected void setAutoGrouping(Product[] sourceProducts, Product targetProduct) {
        targetProduct.setAutoGrouping("temperature_profile:specific_humidity:time");
    }

}
