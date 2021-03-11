package org.esa.s3tbx.dataio.s3.olci;

import com.bc.ceres.glevel.MultiLevelImage;
import org.esa.s3tbx.dataio.s3.AbstractProductFactory;
import org.esa.s3tbx.dataio.s3.Manifest;
import org.esa.s3tbx.dataio.s3.Sentinel3ProductReader;
import org.esa.s3tbx.dataio.s3.SentinelTimeCoding;
import org.esa.s3tbx.dataio.s3.util.S3NetcdfReader;
import org.esa.s3tbx.dataio.s3.util.S3NetcdfReaderFactory;
import org.esa.snap.core.dataio.geocoding.ComponentFactory;
import org.esa.snap.core.dataio.geocoding.ComponentGeoCoding;
import org.esa.snap.core.dataio.geocoding.ForwardCoding;
import org.esa.snap.core.dataio.geocoding.GeoChecks;
import org.esa.snap.core.dataio.geocoding.GeoRaster;
import org.esa.snap.core.dataio.geocoding.InverseCoding;
import org.esa.snap.core.dataio.geocoding.forward.PixelForward;
import org.esa.snap.core.dataio.geocoding.forward.PixelInterpolatingForward;
import org.esa.snap.core.dataio.geocoding.forward.TiePointBilinearForward;
import org.esa.snap.core.dataio.geocoding.inverse.PixelQuadTreeInverse;
import org.esa.snap.core.dataio.geocoding.inverse.TiePointInverse;
import org.esa.snap.core.dataio.geocoding.util.RasterUtils;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.dataio.netcdf.util.NetcdfFileOpener;
import org.esa.snap.runtime.Config;
import ucar.ma2.Array;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Tonio Fincke
 */
public abstract class OlciProductFactory extends AbstractProductFactory {

    private final static String[] excludedIDs = new String[]{"removedPixelsData"};

    private Map<String, Float> nameToWavelengthMap;
    private Map<String, Float> nameToBandwidthMap;
    private Map<String, Integer> nameToIndexMap;
    private int subSamplingX;
    private int subSamplingY;

    public final static String OLCI_USE_PIXELGEOCODING = "s3tbx.reader.olci.pixelGeoCoding";
    final static String SYSPROP_OLCI_USE_FRACTIONAL_ACCURACY = "s3tbx.reader.olci.fractionAccuracy";
    final static String SYSPROP_OLCI_PIXEL_CODING_FORWARD = "s3tbx.reader.olci.pixelGeoCoding.forward";
    final static String SYSPROP_OLCI_PIXEL_CODING_INVERSE = "s3tbx.reader.olci.pixelGeoCoding.inverse";
    final static String SYSPROP_OLCI_TIE_POINT_CODING_FORWARD = "s3tbx.reader.olci.tiePointGeoCoding.forward";

    OlciProductFactory(Sentinel3ProductReader productReader) {
        super(productReader);
        nameToWavelengthMap = new HashMap<>();
        nameToBandwidthMap = new HashMap<>();
        nameToIndexMap = new HashMap<>();
    }

    @Override
    protected List<String> getFileNames(Manifest manifest) {
        return manifest.getFileNames(excludedIDs);
    }

    @Override
    protected void processProductSpecificMetadata(MetadataElement metadataElement) {
        final MetadataElement olciInformationElement = metadataElement.getElement("olciProductInformation");
        final MetadataElement samplingParametersElement = olciInformationElement.getElement("samplingParameters");
        subSamplingY = Integer.parseInt(samplingParametersElement.getAttribute("rowsPerTiePoint").getData().toString());
        subSamplingX = Integer.parseInt(samplingParametersElement.getAttribute("columnsPerTiePoint").getData().toString());
        final MetadataElement bandDescriptionsElement = olciInformationElement.getElement("bandDescriptions");
        if (bandDescriptionsElement != null) {
            for (int i = 0; i < bandDescriptionsElement.getNumElements(); i++) {
                final MetadataElement bandDescriptionElement = bandDescriptionsElement.getElementAt(i);
                final String bandName = bandDescriptionElement.getAttribute("name").getData().getElemString();
                final float wavelength =
                        Float.parseFloat(bandDescriptionElement.getAttribute("centralWavelength").getData().getElemString());
                final float bandWidth =
                        Float.parseFloat(bandDescriptionElement.getAttribute("bandWidth").getData().getElemString());
                nameToWavelengthMap.put(bandName, wavelength);
                nameToBandwidthMap.put(bandName, bandWidth);
                nameToIndexMap.put(bandName, i);
            }
        }
    }

    private float getWavelength(String name) {
        return nameToWavelengthMap.get(name);
    }

    private float getBandwidth(String name) {
        return nameToBandwidthMap.get(name);
    }

    private int getBandindex(String name) {
        return nameToIndexMap.get(name);
    }

    @Override
    protected RasterDataNode addSpecialNode(Product masterProduct, Band sourceBand, Product targetProduct) {
        final String sourceBandName = sourceBand.getName();
        if (targetProduct.containsBand(sourceBandName)) {
            sourceBand.setName("TP_" + sourceBandName);
        }
        return copyBandAsTiePointGrid(sourceBand, targetProduct, subSamplingX, subSamplingY, 0.0f, 0.0f);
    }

    @Override
    protected void setGeoCoding(Product targetProduct) throws IOException {
        if (Config.instance("s3tbx").load().preferences().getBoolean(OLCI_USE_PIXELGEOCODING, false)) {
            setPixelGeoCoding(targetProduct);
        } else {
            setTiePointGeoCoding(targetProduct);
        }
    }

    private void setPixelGeoCoding(Product targetProduct) throws IOException {
        final String lonVariableName = "longitude";
        final String latVariableName = "latitude";
        final Band lonBand = targetProduct.getBand(lonVariableName);
        final Band latBand = targetProduct.getBand(latVariableName);
        if (lonBand == null || latBand == null) {
            return;
        }

        final double[] longitudes = RasterUtils.loadDataScaled(lonBand);
        lonBand.unloadRasterData();
        final double[] latitudes = RasterUtils.loadDataScaled(latBand);
        latBand.unloadRasterData();

        final double resolutionInKilometers = getResolutionInKm(targetProduct.getProductType());
        final GeoRaster geoRaster = new GeoRaster(longitudes, latitudes, lonVariableName, latVariableName,
                                                  lonBand.getRasterWidth(), lonBand.getRasterHeight(), resolutionInKilometers);

        final String[] codingKeys = getForwardAndInverseKeys_pixelCoding();
        final ForwardCoding forward = ComponentFactory.getForward(codingKeys[0]);
        final InverseCoding inverse = ComponentFactory.getInverse(codingKeys[1]);

        final ComponentGeoCoding geoCoding = new ComponentGeoCoding(geoRaster, forward, inverse, GeoChecks.POLES);
        geoCoding.initialize();

        targetProduct.setSceneGeoCoding(geoCoding);
    }

    private void setTiePointGeoCoding(Product targetProduct) {
        String lonVarName = "longitude";
        String latVarName = "latitude";
        TiePointGrid lonGrid = targetProduct.getTiePointGrid(lonVarName);
        TiePointGrid latGrid = targetProduct.getTiePointGrid(latVarName);

        if (latGrid == null || lonGrid == null) {
            lonVarName = "TP_longitude";
            latVarName = "TP_latitude";
            lonGrid = targetProduct.getTiePointGrid(lonVarName);
            latGrid = targetProduct.getTiePointGrid(latVarName);
            if (latGrid == null || lonGrid == null) {
                return;
            }
        }

        final double[] longitudes = loadTiePointData(lonVarName);
        final double[] latitudes = loadTiePointData(latVarName);
        final double resolutionInKilometers = getResolutionInKm(targetProduct.getProductType());

        final GeoRaster geoRaster = new GeoRaster(longitudes, latitudes, lonVarName, latVarName,
                                                  lonGrid.getGridWidth(), lonGrid.getGridHeight(),
                                                  targetProduct.getSceneRasterWidth(), targetProduct.getSceneRasterHeight(), resolutionInKilometers,
                                                  lonGrid.getOffsetX(), lonGrid.getOffsetY(),
                                                  lonGrid.getSubSamplingX(), lonGrid.getSubSamplingY());

        final String[] codingKeys = getForwardAndInverseKeys_tiePointCoding();
        final ForwardCoding forward = ComponentFactory.getForward(codingKeys[0]);
        final InverseCoding inverse = ComponentFactory.getInverse(codingKeys[1]);

        final ComponentGeoCoding geoCoding = new ComponentGeoCoding(geoRaster, forward, inverse, GeoChecks.POLES);
        geoCoding.initialize();

        targetProduct.setSceneGeoCoding(geoCoding);
    }

    @Override
    protected void configureTargetNode(Band sourceBand, RasterDataNode targetNode) {
        if (targetNode.getName().matches("Oa[0-2][0-9].*")) {
            if (targetNode instanceof Band) {
                final Band targetBand = (Band) targetNode;
                String cutName = targetBand.getName().substring(0, 4);
                targetBand.setSpectralBandIndex(getBandindex(cutName));
                targetBand.setSpectralWavelength(getWavelength(cutName));
                targetBand.setSpectralBandwidth(getBandwidth(cutName));
                applyCustomCalibration(targetBand);
            }
        }
        // convert log10 scaled variables int concentrations and also their error bands
        // the unit string follows the CF conventions.
        // See: http://www.unidata.ucar.edu/software/udunits/udunits-2.0.4/udunits2lib.html#Syntax
        if (targetNode.getName().startsWith("ADG443_NN") ||
                targetNode.getName().startsWith("CHL_NN") ||
                targetNode.getName().startsWith("CHL_OC4ME") ||
                targetNode.getName().startsWith("KD490_M07") ||
                targetNode.getName().startsWith("TSM_NN")) {
            if (targetNode instanceof Band) {
                final Band targetBand = (Band) targetNode;
                String unit = targetBand.getUnit();
                Pattern pattern = Pattern.compile("lg\\s*\\(\\s*re:?\\s*(.*)\\)");
                final Matcher m = pattern.matcher(unit);
                if (m.matches()) {
                    targetBand.setLog10Scaled(true);
                    targetBand.setUnit(m.group(1));
                    String description = targetBand.getDescription();
                    description = description.replace("log10 scaled ", "");
                    targetBand.setDescription(description);
                } else {
                    getLogger().log(Level.WARNING, "Unit extraction not working for band " + targetNode.getName());
                }
                targetNode.setValidPixelExpression(getValidExpression());
            }
        }
    }

    protected void applyCustomCalibration(Band targetBand) {
        //empty implementation
    }

    @Override
    protected void setTimeCoding(Product targetProduct) throws IOException {
        setTimeCoding(targetProduct, "time_coordinates.nc", "time_stamp");
    }

    protected abstract String getValidExpression();

    @Override
    protected Product readProduct(String fileName, Manifest manifest) throws IOException {
        final File file = new File(getInputFileParentDirectory(), fileName);
        if (!file.exists()) {
            return null;
        }
        final S3NetcdfReader reader = S3NetcdfReaderFactory.createS3NetcdfProduct(file);
        addSeparatingDimensions(reader.getSuffixesForSeparatingDimensions());
        return reader.readProductNodes(file, null);
    }

    static double getResolutionInKm(String productType) {
        switch (productType) {
            case "OL_1_EFR":
            case "OL_2_LFR":
            case "OL_2_WFR":
                return 0.3;

            case "OL_1_ERR":
            case "OL_2_LRR":
            case "OL_2_WRR":
                return 1.2;

            default:
                throw new IllegalArgumentException("unsupported product of type: " + productType);
        }
    }

    static String[] getForwardAndInverseKeys_pixelCoding() {
        final String[] codingNames = new String[2];

        final Preferences preferences = Config.instance("s3tbx").preferences();
        final boolean useFractAccuracy = preferences.getBoolean(SYSPROP_OLCI_USE_FRACTIONAL_ACCURACY, false);
        if (useFractAccuracy) {
            codingNames[0] = PixelInterpolatingForward.KEY;
        } else {
            codingNames[0] = preferences.get(SYSPROP_OLCI_PIXEL_CODING_FORWARD, PixelForward.KEY);
        }
        codingNames[1] = preferences.get(SYSPROP_OLCI_PIXEL_CODING_INVERSE, PixelQuadTreeInverse.KEY);

        return codingNames;
    }

    static String[] getForwardAndInverseKeys_tiePointCoding() {
        final String[] codingNames = new String[2];

        final Preferences preferences = Config.instance("s3tbx").preferences();
        codingNames[0] = preferences.get(SYSPROP_OLCI_TIE_POINT_CODING_FORWARD, TiePointBilinearForward.KEY);
        codingNames[1] = TiePointInverse.KEY;

        return codingNames;
    }

    protected double[] loadTiePointData(String tpgName) {
        final MultiLevelImage mlImage = getImageForTpg(tpgName);
        final Raster tpData = mlImage.getImage(0).getData();
        final double[] tiePoints = new double[tpData.getWidth() * tpData.getHeight()];
        tpData.getPixels(0, 0, tpData.getWidth(), tpData.getHeight(), tiePoints);
        return tiePoints;
    }
}
