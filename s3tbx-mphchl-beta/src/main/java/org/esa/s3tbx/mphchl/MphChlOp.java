package org.esa.s3tbx.mphchl;

import com.bc.ceres.glevel.MultiLevelImage;
import org.esa.s3tbx.olci.radiometry.SensorConstants;
import org.esa.s3tbx.olci.radiometry.rayleigh.RayleighCorrectionOp;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Kernel;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.converters.BooleanExpressionConverter;

import javax.media.jai.BorderExtender;
import javax.media.jai.BorderExtenderCopy;
import javax.media.jai.JAI;
import javax.media.jai.KernelJAI;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.ConvolveDescriptor;
import java.awt.RenderingHints;

/**
 * Wrapper for MPH CHL pixel operator.
 * Allows for post-processing of whole image (currently: JAI low-pass filtering).
 *
 * @author olafd
 */
@OperatorMetadata(alias = "MphChl-beta",
        version = "1.0",
        category = "Optical/Thematic Water Processing",
        authors = "Mark William Matthews, Daniel Odermatt, Tom Block, Olaf Danne",
        copyright = "(c) 2013, 2014, 2017 by Brockmann Consult",
        description = "This operator computes maximum peak height of chlorophyll (MPH/CHL).")
public class MphChlOp extends Operator {

    @Parameter(defaultValue = "",
            description = "Expression defining pixels considered for processing. " +
                    "If not set, all valid pixels over water are processed.",
            converter = BooleanExpressionConverter.class)
    private String validPixelExpression;

    @Parameter(defaultValue = "1000.0",
            description = "Maximum chlorophyll, arithmetically higher values are capped.")
    private double cyanoMaxValue;

    @Parameter(defaultValue = "500.0",
            description = "Chlorophyll threshold, above which all cyanobacteria dominated waters are 'float'.")
    private double chlThreshForFloatFlag;

    @Parameter(defaultValue = "false",
            description = "Switch to true to write 'mph' band.")
    boolean exportMph;

    @Parameter(defaultValue = "false",
            description = "Switch to true to apply a 3x3 low-pass filter on the result.")
    boolean applyLowPassFilter;

    @Parameter(defaultValue = "false",
            description = "Add additional chl bands.")
    boolean exportAddBands;

    @SourceProduct(description = "L1b or Rayleigh corrected product", label = "OLCI or MERIS L1b or Rayleigh corrected product")
    private Product sourceProduct;

    @Override
    public void initialize() throws OperatorException {
        Product mphChlProduct = createMphChlPixelProduct();

        if (applyLowPassFilter) {
            setTargetProduct(createFilteredProduct(mphChlProduct));
        } else {
            setTargetProduct(mphChlProduct);
        }
    }

    private Product createFilteredProduct(Product mphChlProduct) {
        Product filteredProduct = new Product(mphChlProduct.getName(),
                                              mphChlProduct.getProductType(),
                                              mphChlProduct.getSceneRasterWidth(),
                                              mphChlProduct.getSceneRasterHeight());

        ProductUtils.copyMetadata(mphChlProduct, filteredProduct);
        ProductUtils.copyGeoCoding(mphChlProduct, filteredProduct);
        ProductUtils.copyFlagCodings(mphChlProduct, filteredProduct);
        ProductUtils.copyFlagBands(mphChlProduct, filteredProduct, true);
        ProductUtils.copyMasks(mphChlProduct, filteredProduct);
        filteredProduct.setStartTime(mphChlProduct.getStartTime());
        filteredProduct.setEndTime(mphChlProduct.getEndTime());

        for (int i = 0; i < mphChlProduct.getNumTiePointGrids(); i++) {
            TiePointGrid srcTPG = mphChlProduct.getTiePointGridAt(i);
            if (!filteredProduct.containsTiePointGrid(srcTPG.getName())) {
                filteredProduct.addTiePointGrid(srcTPG.cloneTiePointGrid());
            }
        }

        for (Band b : mphChlProduct.getBands()) {
            if (!b.isFlagBand()) {
                // currently we have chl as only meaningful band to filter
                if (b.getName().equals("chl")) {
                    final KernelJAI jaiKernel = getJaiKernel();
                    RenderingHints rh = new RenderingHints(JAI.KEY_BORDER_EXTENDER, BorderExtender.createInstance(
                            BorderExtenderCopy.BORDER_COPY));
                    final MultiLevelImage sourceImage = b.getSourceImage();
                    final RenderedOp filteredImage = ConvolveDescriptor.create(sourceImage, jaiKernel, rh);
                    ProductUtils.copyBand(b.getName(), mphChlProduct, filteredProduct, false);
                    filteredProduct.getBand(b.getName()).setSourceImage(filteredImage);
                } else {
                    // just copy bands
                    if (!filteredProduct.containsBand(b.getName())) {
                        ProductUtils.copyBand(b.getName(), mphChlProduct, filteredProduct, true);
                    }
                }
            }
        }

        return filteredProduct;
    }


    private KernelJAI getJaiKernel() {
        final Kernel lowPassKernel = new Kernel(3, 3, 1.0 / 16.0, new double[]{
                +1, +2, +1,
                +2, +4, +2,
                +1, +2, +1,
        });
        final double[] data = lowPassKernel.getKernelData(null);
        final float[] scaledData = new float[data.length];
        final double factor = lowPassKernel.getFactor();
        for (int i = 0; i < data.length; i++) {
            scaledData[i] = (float) (data[i] * factor);
        }
        return new KernelJAI(lowPassKernel.getWidth(), lowPassKernel.getHeight(),
                             lowPassKernel.getXOrigin(), lowPassKernel.getYOrigin(),
                             scaledData);
    }

    private Product createMphChlPixelProduct() {
        MphChlBasisBetaOp mphChlOp = null;
        Sensor sensor = getSensorType(sourceProduct);
        switch (sensor) {
            case OLCI:
                mphChlOp = new MphChlOlciBetaOp();
                break;
            case MERIS_3RD:
            case MERIS_4TH:
                mphChlOp = new MphChlMerisBetaOp();
                break;
        }

        if (isValidL1bSourceProduct(sourceProduct, sensor)) {
            RayleighCorrectionOp rayleighCorrectionOp = new RayleighCorrectionOp();
            rayleighCorrectionOp.setSourceProduct(sourceProduct);
            rayleighCorrectionOp.setParameterDefaultValues();
            rayleighCorrectionOp.setParameter("computeTaur", false);
            rayleighCorrectionOp.setParameter("sourceBandNames", sensor.getRequiredRadianceBandNames());
            mphChlOp.setSourceProduct(rayleighCorrectionOp.getTargetProduct());
        } else if (isValidBrrSourceProduct(sourceProduct, sensor)) {
            mphChlOp.setSourceProduct(sourceProduct);
        } else {
            throw new OperatorException
                    ("Input product not supported - must be " + sensor.getName() + " L1b or Rayleigh corrected BRR product");
        }

        mphChlOp.setParameterDefaultValues();
        if (validPixelExpression != null && validPixelExpression.length() > 0) {
            mphChlOp.setParameter("validPixelExpression", validPixelExpression);
        } else {
            // the default - valid pixels over water
            mphChlOp.setParameter("validPixelExpression", sensor.getValidPixelExpression());
        }
        mphChlOp.setParameter("cyanoMaxValue", cyanoMaxValue);
        mphChlOp.setParameter("chlThreshForFloatFlag", chlThreshForFloatFlag);
        mphChlOp.setParameter("exportMph", exportMph);
        mphChlOp.setParameter("exportAddBands", exportAddBands);

        return mphChlOp.getTargetProduct();
    }

    static Sensor getSensorType(Product sourceProduct) {
        boolean isOlci = isValidL1bSourceProduct(sourceProduct, Sensor.OLCI) ||
                isValidBrrSourceProduct(sourceProduct, Sensor.OLCI);
        if (isOlci) {
            return Sensor.OLCI;
        }

        boolean isMeris3rd = isValidL1bSourceProduct(sourceProduct, Sensor.MERIS_3RD) ||
                (isValidBrrSourceProduct(sourceProduct, Sensor.MERIS_3RD) &&
                        sourceProduct.containsBand(SensorConstants.MERIS_L1B_FLAGS_NAME));
        if (isMeris3rd) {
            return Sensor.MERIS_3RD;
        }

        boolean isMeris4th = isValidL1bSourceProduct(sourceProduct, Sensor.MERIS_4TH) ||
                (isValidBrrSourceProduct(sourceProduct, Sensor.MERIS_4TH) &&
                        sourceProduct.containsBand(SensorConstants.MERIS_4TH_L1B_FLAGS_NAME));
        if (isMeris4th) {
            return Sensor.MERIS_4TH;
        }

        throw new OperatorException("Source product not applicable to this operator.\n" +
                                            "Only OLCI and MERIS are supported");
    }

    static boolean isValidL1bSourceProduct(Product sourceProduct, Sensor sensor) {
        for (String bandName : sensor.getRequiredRadianceBandNames()) {
            if (!sourceProduct.containsBand(bandName)) {
                return false;
            }
        }
        return true;
    }

    static boolean isValidBrrSourceProduct(Product sourceProduct, Sensor sensor) {
        final String[] requiredBrrBands = sensor.getRequiredBrrBandNames();
        for (String bandName : requiredBrrBands) {
            if (!sourceProduct.containsBand(bandName)) {
                return false;
            }
        }
        return true;
    }

    static Sensor getSensorFromBrrSourceProduct(String[] sourceBands) {
        Sensor[] sensors = {Sensor.OLCI, Sensor.MERIS_3RD, Sensor.MERIS_4TH};
        for (Sensor sensor : sensors) {
            boolean containsBand = false;
            for (String requiredBandName : sensor.getRequiredBrrBandNames()) {
                containsBand = false;
                for (String sourceBandName : sourceBands) {
                    if (sourceBandName.equals(requiredBandName)) {
                        containsBand = true;
                    }
                }
                if (!containsBand) {
                    break;
                }
            }
            if (containsBand) {
                return sensor;
            }
        }

        return null;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(MphChlOp.class);
        }
    }
}
