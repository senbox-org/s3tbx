package org.esa.s3tbx.mph_chl;

import com.bc.ceres.glevel.MultiLevelImage;
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

import javax.media.jai.*;
import javax.media.jai.operator.ConvolveDescriptor;
import java.awt.*;

/**
 * Wrapper for MPH CHL pixel operator.
 * Allows for post-processing of whole image (currently: JAI low-pass filtering).
 *
 * @author olafd
 */
@OperatorMetadata(alias = "MphChl",
        version = "1.0",
        authors = "Mark William Matthews, Daniel Odermatt, Tom Block, Olaf Danne",
        copyright = "(c) 2013, 2014, 2017 by Brockmann Consult",
        description = "This operator computes maximum peak height of chlorophyll (MPH/CHL).")
public class MphChlOp extends Operator {

    @Parameter(defaultValue = "not (quality_flags.land or quality_flags.invalid)",
            description = "Expression defining pixels considered for processing.")
    private String validPixelExpression;

    @Parameter(defaultValue = "1000.0",
            description = "Maximum chlorophyll, arithmetically higher values are capped.")
    private double cyanoMaxValue;

    @Parameter(defaultValue = "500.0",
            description = "Chlorophyll threshold, above which all cyanobacteria dominated waters are 'float.")
    private double chlThreshForFloatFlag;

    @Parameter(defaultValue = "false",
            description = "Switch to true to write 'mph' band.")
    boolean exportMph;

    @Parameter(defaultValue = "false",
            description = "Switch to true to apply a 3x3 low-pass filter on the result.")
    boolean applyLowPassFilter;

    @SourceProduct(description = "OLCI L1 or Rayleigh corrected product", label = "OLCI L1b or Rayleigh corrected product")
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
        MphChlOlciOp mphChlOp = new MphChlOlciOp();
        if (isValidL1bSourceProduct()) {
            RayleighCorrectionOp rayleighCorrectionOp = new RayleighCorrectionOp();
            rayleighCorrectionOp.setParameterDefaultValues();
            rayleighCorrectionOp.setParameter("computeTaur", false);
            rayleighCorrectionOp.setSourceProduct(sourceProduct);
            mphChlOp.setSourceProduct(rayleighCorrectionOp.getTargetProduct());
        } else if (isValidBrrSourceProduct()) {
            mphChlOp.setSourceProduct(sourceProduct);
        } else {
            throw new OperatorException("Input product not supported - must be OLCI L1b or Rayleigh corrected BRR product");
        }

        mphChlOp.setParameterDefaultValues();
        mphChlOp.setParameter("validPixelExpression", validPixelExpression);
        mphChlOp.setParameter("cyanoMaxValue", cyanoMaxValue);
        mphChlOp.setParameter("chlThreshForFloatFlag", chlThreshForFloatFlag);
        mphChlOp.setParameter("exportMph", exportMph);

        return mphChlOp.getTargetProduct();
    }

    private boolean isValidL1bSourceProduct() {
        // simple check for required radiance bands
        // todo: distinguish OLCI, MERIS 3rd, MERIS 4th reprocessing
        for (String bandName : MphChlConstants.OLCI_REQUIRED_RAD_BAND_NAMES) {
            if (!sourceProduct.containsBand(bandName)) {
                return false;
            }
        }
        return true;
    }

    private boolean isValidBrrSourceProduct() {
        // simple check for required BRR bands
        for (String bandName : MphChlConstants.OLCI_REQUIRED_BRR_BAND_NAMES) {
            if (!sourceProduct.containsBand(bandName)) {
                return false;
            }
        }
        return true;
    }


    @SuppressWarnings({"UnusedDeclaration"})
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(MphChlOp.class);
        }
    }
}
