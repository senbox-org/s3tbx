package org.esa.s3tbx.idepix.algorithms.modis;

import org.esa.s3tbx.idepix.core.AlgorithmSelector;
import org.esa.s3tbx.idepix.core.IdepixConstants;
import org.esa.s3tbx.idepix.core.util.IdepixIO;
import org.esa.s3tbx.idepix.core.operators.BasisOp;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.util.ProductUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Idepix operator for pixel identification and classification for MODIS
 *
 * @author olafd
 */
@SuppressWarnings({"FieldCanBeLocal"})
@OperatorMetadata(alias = "Idepix.TerraAqua.Modis",
        category = "Optical/Pre-Processing",
        version = "2.2",
        authors = "Olaf Danne, Marco Zuehlke",
        copyright = "(c) 2016 by Brockmann Consult",
        description = "Pixel identification and classification for MODIS.")
public class ModisOp extends BasisOp {

    @Parameter(defaultValue = "CLOUD_CONSERVATIVE",
            valueSet = {"CLEAR_SKY_CONSERVATIVE", "CLOUD_CONSERVATIVE"},
            label = " Strength of cloud flagging",
            description = "Strength of cloud flagging. In case of 'CLOUD_CONSERVATIVE', more pixels might be flagged as cloud.")
    private String cloudFlaggingStrength;

    @Parameter(defaultValue = "1", label = " Width of cloud buffer (# of pixels)")
    private int cloudBufferWidth;

    @Parameter(defaultValue = "150", valueSet = {"1000", "150", "50"},
            label = " Resolution of land-water mask (m/pixel)",
            description = "Resolution of used land-water mask in meters per pixel")
    private int waterMaskResolution;

    @Parameter(defaultValue = "false",
            label = " Write reflective solar bands",
            description = "Write TOA reflective solar bands (RefSB) to target product.")
    private boolean outputRad2Refl = false;

    @Parameter(defaultValue = "false",
            description = "Write CAWA RefSB (bands 2, 5, 17-19) to the target product.",
            label = " Write CAWA RefSB bands (experimental!)")
    private boolean outputCawaRefSB = false;

    @Parameter(defaultValue = "false",
            label = " Write emissive bands",
            description = "Write 'Emissive' bands to target product.")
    private boolean outputEmissive = false;

    @Parameter(defaultValue = "1.035",     // this does not work over land!
            label = " NN cloud ambiguous lower boundary",
            description = " NN cloud ambiguous lower boundary")
    double nnCloudAmbiguousLowerBoundaryValue;

    @Parameter(defaultValue = "3.35",
            label = " NN cloud ambiguous/sure separation value",
            description = " NN cloud ambiguous cloud ambiguous/sure separation value")
    double nnCloudAmbiguousSureSeparationValue;

    @Parameter(defaultValue = "4.2",
            label = " NN cloud sure/snow separation value",
            description = " NN cloud ambiguous cloud sure/snow separation value")
    double nnCloudSureSnowSeparationValue;

//    @Parameter(defaultValue = "0.08",
//            label = " 'B_NIR' threshold at 859nm (MODIS)",
//            description = "'B_NIR' threshold: 'Cloud B_NIR' set if EV_250_Aggr1km_RefSB_2 > THRESH.")
    private double bNirThresh859 = 0.08;

//    @Parameter(defaultValue = "0.15",
//            label = " 'Dark glint' threshold at 859nm for 'cloud sure' (MODIS)",
//            description = "'Dark glint' threshold: 'Cloud sure' possible only if EV_250_Aggr1km_RefSB_2 > THRESH.")
    private double glintThresh859forCloudSure = 0.15;

//    @Parameter(defaultValue = "0.06",
//            label = " 'Dark glint' threshold at 859nm for 'cloud ambiguous' (MODIS)",
//            description = "'Dark glint' threshold: 'Cloud ambiguous' possible only if EV_250_Aggr1km_RefSB_2 > THRESH.")
    private double glintThresh859forCloudAmbiguous = 0.06;

//    @Parameter(defaultValue = "true",
//            label = " Apply brightness test (MODIS)",
//            description = "Apply brightness test: EV_250_Aggr1km_RefSB_1 > THRESH (MODIS).")
    private boolean applyBrightnessTest = true;

//    @Parameter(defaultValue = "true",
//            label = " Apply 'OR' logic in cloud test (MODIS)",
//            description = "Apply 'OR' logic instead of 'AND' logic in cloud test (MODIS).")
    private boolean applyOrLogicInCloudTest = true;

    //    @Parameter(defaultValue = "0.125",
//               label = " Brightness test 'cloud ambiguous' threshold (MODIS)",
//               description = "Brightness test 'cloud ambiguous' threshold: EV_250_Aggr1km_RefSB_1 > THRESH (MODIS).")
    private double brightnessThreshCloudAmbiguous = 0.125;


    @SourceProduct(alias = "sourceProduct", label = "Name (MODIS L1b product)", description = "The source product.")
    private Product sourceProduct;

    private Product waterMaskProduct;
    private Product classifProduct;
    private Map<String, Object> waterClassificationParameters;

    // former user options, now fix

//    private final double glintThresh859 = 0.15;
//    private boolean applyBrightnessTest = true;
//    private boolean applyOrLogicInCloudTest;


    @Override
    public void initialize() throws OperatorException {
        applyOrLogicInCloudTest = cloudFlaggingStrength.equals("CLOUD_CONSERVATIVE");

        final boolean inputProductIsValid = IdepixIO.validateInputProduct(sourceProduct, AlgorithmSelector.MODIS);
        if (!inputProductIsValid) {
            throw new OperatorException(IdepixConstants.INPUT_INCONSISTENCY_ERROR_MESSAGE);
        }

        processModis();
    }

    private void processModis() {
        Map<String, Product> occciClassifInput = new HashMap<>(4);
        computeAlgorithmInputProducts(occciClassifInput);
        Map<String, Object> occciCloudClassificationParameters = createModisPixelClassificationParameters();

        // post processing input:
        // - cloud buffer
        // - cloud shadow todo (currently exisis only for Meris)
        Map<String, Object> postProcessParameters = new HashMap<>();
        postProcessParameters.put("cloudBufferWidth", cloudBufferWidth);
        Map<String, Product> postProcessInput = new HashMap<>();
        postProcessInput.put("waterMask", waterMaskProduct);

        postProcessInput.put("refl", sourceProduct);
        classifProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(ModisClassificationOp.class),
                                           occciCloudClassificationParameters, occciClassifInput);

        postProcessInput.put("classif", classifProduct);

        Product postProcessProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(ModisPostProcessOp.class),
                                                       postProcessParameters, postProcessInput);

        ProductUtils.copyMetadata(sourceProduct,postProcessProduct);
        setTargetProduct(postProcessProduct);
        addBandsToTargetProduct(postProcessProduct);
    }

    private void computeAlgorithmInputProducts(Map<String, Product> occciClassifInput) {
        createWaterMaskProduct();
        occciClassifInput.put("waterMask", waterMaskProduct);
        occciClassifInput.put("refl", sourceProduct);
    }


    private void createWaterMaskProduct() {
        HashMap<String, Object> waterParameters = new HashMap<>();
        waterParameters.put("resolution", waterMaskResolution);
        waterParameters.put("subSamplingFactorX", 3);
        waterParameters.put("subSamplingFactorY", 3);
        waterMaskProduct = GPF.createProduct("LandWaterMask", waterParameters, sourceProduct);
    }

    private Map<String, Object> createModisPixelClassificationParameters() {
        Map<String, Object> occciCloudClassificationParameters = new HashMap<>(1);
        occciCloudClassificationParameters.put("cloudBufferWidth", cloudBufferWidth);
        occciCloudClassificationParameters.put("wmResolution", waterMaskResolution);
        occciCloudClassificationParameters.put("applyBrightnessTest", applyBrightnessTest);
        occciCloudClassificationParameters.put("applyOrLogicInCloudTest", applyOrLogicInCloudTest);
        occciCloudClassificationParameters.put("nnCloudAmbiguousLowerBoundaryValue", nnCloudAmbiguousLowerBoundaryValue);
        occciCloudClassificationParameters.put("nnCloudAmbiguousSureSeparationValue", nnCloudAmbiguousSureSeparationValue);
        occciCloudClassificationParameters.put("nnCloudSureSnowSeparationValue", nnCloudSureSnowSeparationValue);

        occciCloudClassificationParameters.put("brightnessThreshCloudAmbiguous", brightnessThreshCloudAmbiguous);
        occciCloudClassificationParameters.put("glintThresh859forCloudSure", glintThresh859forCloudSure);
        occciCloudClassificationParameters.put("glintThresh859forCloudAmbiguous", glintThresh859forCloudAmbiguous);
        occciCloudClassificationParameters.put("bNirThresh859", bNirThresh859);

        return occciCloudClassificationParameters;
    }

    private void addBandsToTargetProduct(Product targetProduct) {
        if (outputCawaRefSB) {
            outputRad2Refl = false;
            outputEmissive = false;
            copySourceBands(sourceProduct, targetProduct, "EV_250_Aggr1km_RefSB_2");
            copySourceBands(sourceProduct, targetProduct, "EV_500_Aggr1km_RefSB_5");
            copySourceBands(sourceProduct, targetProduct, "EV_1KM_RefSB_17");
            copySourceBands(sourceProduct, targetProduct, "EV_1KM_RefSB_18");
            copySourceBands(sourceProduct, targetProduct, "EV_1KM_RefSB_19");
            for (int i = 0; i < sourceProduct.getNumTiePointGrids(); i++) {
                TiePointGrid srcTPG = sourceProduct.getTiePointGridAt(i);
                if ((srcTPG.getName().contains("Zenith") || srcTPG.getName().contains("Azimuth")) &&
                        !targetProduct.containsTiePointGrid(srcTPG.getName())) {
                    targetProduct.addTiePointGrid(srcTPG.cloneTiePointGrid());
                }
            }
        }
        if (outputRad2Refl) {
            copySourceBands(sourceProduct, targetProduct, "RefSB");
        }
        if (outputEmissive) {
            copySourceBands(sourceProduct, targetProduct, "Emissive");
        }

    }

    private static void copySourceBands(Product rad2reflProduct, Product targetProduct, String bandNameSubstring) {
        for (String bandname : rad2reflProduct.getBandNames()) {
            if (bandname.contains(bandNameSubstring) && !targetProduct.containsBand(bandname)) {
                ProductUtils.copyBand(bandname, rad2reflProduct, targetProduct, true);
            }
        }
    }


    /**
     * The Service Provider Interface (SPI) for the operator.
     * It provides operator meta-data and is a factory for new operator instances.
     */
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(ModisOp.class);
        }
    }
}
