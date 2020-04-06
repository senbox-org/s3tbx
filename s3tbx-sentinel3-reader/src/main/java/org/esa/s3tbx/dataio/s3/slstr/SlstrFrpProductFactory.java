package org.esa.s3tbx.dataio.s3.slstr;

import com.bc.ceres.glevel.support.DefaultMultiLevelImage;
import com.bc.ceres.glevel.support.DefaultMultiLevelModel;
import com.bc.ceres.glevel.support.DefaultMultiLevelSource;
import org.esa.s3tbx.dataio.s3.Manifest;
import org.esa.s3tbx.dataio.s3.Sentinel3ProductReader;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.BasicPixelGeoCoding;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoCodingFactory;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.TiePointGeoCoding;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.util.ProductUtils;

import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SlstrFrpProductFactory extends SlstrProductFactory {

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
            if (identifier.equals("fn")) {
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
            if (p.getName().endsWith("fn")) {
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
    protected void setBandGeoCodings(Product targetProduct) {
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
        if (targetProduct.containsTiePointGrid(LATITUDE_TPG_NAME) && targetProduct.containsTiePointGrid(LONGITUDE_TPG_NAME)) {
            TiePointGrid latitudeTiePointGrid = targetProduct.getTiePointGrid(LATITUDE_TPG_NAME);
            TiePointGrid longitudeTiePointGrid = targetProduct.getTiePointGrid(LONGITUDE_TPG_NAME);
            TiePointGeoCoding tiePointGeoCoding = new TiePointGeoCoding(latitudeTiePointGrid, longitudeTiePointGrid);
            targetProduct.setSceneGeoCoding(tiePointGeoCoding);
        }
    }

    private GeoCoding getBandGeoCoding(Product product, String end) {
        if (geoCodingMap.containsKey(end)) {
            return geoCodingMap.get(end);
        } else {
            Band latBand = null;
            Band lonBand = null;
            switch (end) {
                case "in":
                    latBand = product.getBand("latitude_in");
                    lonBand = product.getBand("longitude_in");
                    break;
                case "fn":
                    latBand = product.getBand("latitude_fn");
                    lonBand = product.getBand("longitude_fn");
                    break;
            }
            if (latBand != null && lonBand != null) {
                final BasicPixelGeoCoding geoCoding = GeoCodingFactory.createPixelGeoCoding(latBand, lonBand, "", 5);
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
