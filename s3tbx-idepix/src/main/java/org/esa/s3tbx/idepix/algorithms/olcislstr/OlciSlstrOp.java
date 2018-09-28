package org.esa.s3tbx.idepix.algorithms.olcislstr;

import org.esa.s3tbx.idepix.core.AlgorithmSelector;
import org.esa.s3tbx.idepix.core.IdepixConstants;
import org.esa.s3tbx.idepix.core.util.IdepixIO;
import org.esa.s3tbx.idepix.core.operators.BasisOp;
import org.esa.s3tbx.processor.rad2refl.Sensor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * The Idepix pixel classification operator for OLCI/SLSTR synergy products.
 *
 * @author olafd
 */
@OperatorMetadata(alias = "Idepix.Sentinel3.OlciSlstr",
        category = "Optical/Pre-Processing",
        version = "1.0",
        authors = "Olaf Danne",
        internal = true,
        copyright = "(c) 2016 by Brockmann Consult",
        description = "Pixel identification and classification for OLCI/SLSTR synergy products.")
public class OlciSlstrOp extends BasisOp {

    @SourceProduct(alias = "sourceProduct",
            label = "OLCI/SLSTR Synergy product",
            description = "The OLCI/SLSTR Synergy source product.")
    private Product sourceProduct;

    @TargetProduct(description = "The target product.")
    private Product targetProduct;

    private boolean outputOlciRadiance;
    private boolean outputOlciRad2Refl;
    private boolean outputSlstrRadiance;
    private boolean outputSlstrRad2Refl;

    @Parameter(description = "The list of OLCI radiance bands to write to target product.",
            label = "Select OLCI TOA radiances to write to the target product",
            valueSet = {
                    "Oa01_radiance", "Oa02_radiance", "Oa03_radiance", "Oa04_radiance", "Oa05_radiance",
                    "Oa06_radiance", "Oa07_radiance", "Oa08_radiance", "Oa09_radiance", "Oa10_radiance",
                    "Oa11_radiance", "Oa12_radiance", "Oa13_radiance", "Oa14_radiance", "Oa15_radiance",
                    "Oa16_radiance", "Oa17_radiance", "Oa18_radiance", "Oa19_radiance", "Oa20_radiance",
                    "Oa21_radiance"
            },
            defaultValue = "")
    String[] olciRadianceBandsToCopy;

    @Parameter(description = "The list of OLCI reflectance bands to write to target product.",
            label = "Select OLCI TOA reflectances to write to the target product",
            valueSet = {
                    "Oa01_reflectance", "Oa02_reflectance", "Oa03_reflectance", "Oa04_reflectance", "Oa05_reflectance",
                    "Oa06_reflectance", "Oa07_reflectance", "Oa08_reflectance", "Oa09_reflectance", "Oa10_reflectance",
                    "Oa11_reflectance", "Oa12_reflectance", "Oa13_reflectance", "Oa14_reflectance", "Oa15_reflectance",
                    "Oa16_reflectance", "Oa17_reflectance", "Oa18_reflectance", "Oa19_reflectance", "Oa20_reflectance",
                    "Oa21_reflectance"
            },
            defaultValue = "")
    String[] olciReflBandsToCopy;

    @Parameter(description = "The list of SLSTR radiance bands to write to target product.",
            label = "Select SLSTR TOA radiances to write to the target product",
            valueSet = {
                    "S1_radiance_an", "S2_radiance_an", "S3_radiance_an",
                    "S4_radiance_an", "S5_radiance_an", "S6_radiance_an",
                    "S4_radiance_bn", "S5_radiance_bn", "S6_radiance_bn",
                    "S4_radiance_cn", "S5_radiance_cn", "S6_radiance_cn"
            },
            defaultValue = "")
    String[] slstrRadianceBandsToCopy;

    @Parameter(description = "The list of SLSTR reflectance bands to write to target product.",
            label = "Select SLSTR TOA reflectances to write to the target product",
            valueSet = {
                    "S1_reflectance_an", "S2_reflectance_an", "S3_reflectance_an",
                    "S4_reflectance_an", "S5_reflectance_an", "S6_reflectance_an",
                    "S4_reflectance_bn", "S5_reflectance_bn", "S6_reflectance_bn",
                    "S4_reflectance_cn", "S5_reflectance_cn", "S6_reflectance_cn"
            },
            defaultValue = "")
    String[] slstrReflBandsToCopy;


    @Parameter(defaultValue = "false",
            label = " Write NN value to the target product",
            description = " If applied, write NN value to the target product ")
    private boolean outputSchillerNNValue;

    @Parameter(defaultValue = "true", label = " Compute a cloud buffer")
    private boolean computeCloudBuffer;

    @Parameter(defaultValue = "2", interval = "[0,100]",
            description = "The width of a cloud 'safety buffer' around a pixel which was classified as cloudy.",
            label = "Width of cloud buffer (# of pixels)")
    private int cloudBufferWidth;

    private Product postProcessingProduct;

    private Product olciRad2reflProduct;
    private Product slstrRad2reflProduct;
    private Product waterMaskProduct;

    private Map<String, Product> classificationInputProducts;
    private Map<String, Object> classificationParameters;

    @Override
    public void initialize() throws OperatorException {

        final boolean inputProductIsValid = IdepixIO.validateInputProduct(sourceProduct, AlgorithmSelector.OLCISLSTR);
        if (!inputProductIsValid) {
            throw new OperatorException(IdepixConstants.INPUT_INCONSISTENCY_ERROR_MESSAGE);
        }

        outputOlciRadiance = olciRadianceBandsToCopy != null && olciRadianceBandsToCopy.length > 0;
        outputOlciRad2Refl = olciReflBandsToCopy != null && olciReflBandsToCopy.length > 0;

        outputSlstrRadiance = slstrRadianceBandsToCopy != null && slstrRadianceBandsToCopy.length > 0;
        outputSlstrRad2Refl = slstrReflBandsToCopy != null && slstrReflBandsToCopy.length > 0;

        preProcess();

        setClassificationInputProducts();
        Product olciSlstrIdepixProduct = computeClassificationProduct();

        olciSlstrIdepixProduct.setName(sourceProduct.getName() + "_IDEPIX");
        olciSlstrIdepixProduct.setAutoGrouping("Oa*_radiance:Oa*_reflectance:S*_radiance:S*_reflectance");

        if (computeCloudBuffer) {
            postProcess(olciSlstrIdepixProduct);
        }

        targetProduct = createTargetProduct(olciSlstrIdepixProduct);
        targetProduct.setAutoGrouping(olciSlstrIdepixProduct.getAutoGrouping());

        if (postProcessingProduct != null) {
            Band cloudFlagBand = targetProduct.getBand(IdepixConstants.CLASSIF_BAND_NAME);
            cloudFlagBand.setSourceImage(postProcessingProduct.getBand(IdepixConstants.CLASSIF_BAND_NAME).getSourceImage());
        }
    }

    private Product createTargetProduct(Product idepixProduct) {
        Product targetProduct = new Product(idepixProduct.getName(),
                                            idepixProduct.getProductType(),
                                            idepixProduct.getSceneRasterWidth(),
                                            idepixProduct.getSceneRasterHeight());

        ProductUtils.copyMetadata(idepixProduct, targetProduct);
        ProductUtils.copyGeoCoding(idepixProduct, targetProduct);
        ProductUtils.copyFlagCodings(idepixProduct, targetProduct);
        ProductUtils.copyFlagBands(idepixProduct, targetProduct, true);
        ProductUtils.copyMasks(idepixProduct, targetProduct);
        ProductUtils.copyTiePointGrids(idepixProduct, targetProduct);
        targetProduct.setStartTime(idepixProduct.getStartTime());
        targetProduct.setEndTime(idepixProduct.getEndTime());

        OlciSlstrUtils.setupOlciClassifBitmask(targetProduct);

        if (outputOlciRadiance) {
            IdepixIO.addRadianceBands(sourceProduct, targetProduct, olciRadianceBandsToCopy);
        }

        if (outputOlciRad2Refl) {
            OlciSlstrUtils.addOlciRadiance2ReflectanceBands(olciRad2reflProduct, targetProduct, olciReflBandsToCopy);
        }

        if (outputSlstrRadiance) {
            IdepixIO.addRadianceBands(sourceProduct, targetProduct, slstrRadianceBandsToCopy);
        }

        if (outputSlstrRad2Refl) {
            OlciSlstrUtils.addSlstrRadiance2ReflectanceBands(slstrRad2reflProduct, targetProduct, slstrReflBandsToCopy);
        }

        if (outputSchillerNNValue) {
            ProductUtils.copyBand(IdepixConstants.NN_OUTPUT_BAND_NAME, idepixProduct, targetProduct, true);
        }

        return targetProduct;
    }

    private void preProcess() {
        olciRad2reflProduct = OlciSlstrUtils.computeRadiance2ReflectanceProduct(sourceProduct, Sensor.OLCI);
        slstrRad2reflProduct = OlciSlstrUtils.computeRadiance2ReflectanceProduct(sourceProduct, Sensor.SLSTR_500m);

        HashMap<String, Object> waterMaskParameters = new HashMap<>();
        waterMaskParameters.put("resolution", IdepixConstants.LAND_WATER_MASK_RESOLUTION);
        waterMaskParameters.put("subSamplingFactorX", IdepixConstants.OVERSAMPLING_FACTOR_X);
        waterMaskParameters.put("subSamplingFactorY", IdepixConstants.OVERSAMPLING_FACTOR_Y);
        waterMaskProduct = GPF.createProduct("LandWaterMask", waterMaskParameters, sourceProduct);
    }

    private void setClassificationParameters() {
        classificationParameters = new HashMap<>();
        classificationParameters.put("copyAllTiePoints", true);
        classificationParameters.put("outputSchillerNNValue", outputSchillerNNValue);
    }

    private void setClassificationInputProducts() {
        classificationInputProducts = new HashMap<>();
        classificationInputProducts.put("l1b", sourceProduct);
        classificationInputProducts.put("reflOlci", olciRad2reflProduct);
        classificationInputProducts.put("reflSlstr", slstrRad2reflProduct);
        classificationInputProducts.put("waterMask", waterMaskProduct);
    }

    private Product computeClassificationProduct() {
        setClassificationParameters();
        return GPF.createProduct(OperatorSpi.getOperatorAlias(OlciSlstrClassificationOp.class),
                                 classificationParameters, classificationInputProducts);
    }

    private void postProcess(Product olciIdepixProduct) {
        HashMap<String, Product> input = new HashMap<>();
        input.put("olciSlstrCloud", olciIdepixProduct);

        Map<String, Object> params = new HashMap<>();
        params.put("cloudBufferWidth", cloudBufferWidth);

        postProcessingProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(OlciSlstrPostProcessOp.class),
                                                  params, input);
    }

    /**
     * The Service Provider Interface (SPI) for the operator.
     * It provides operator meta-data and is a factory for new operator instances.
     */
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(OlciSlstrOp.class);
        }
    }
}
