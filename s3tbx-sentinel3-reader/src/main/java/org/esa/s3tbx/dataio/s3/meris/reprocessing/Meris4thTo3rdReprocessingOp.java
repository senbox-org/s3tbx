package org.esa.s3tbx.dataio.s3.meris.reprocessing;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;

/**
 * Provides the adaptation of MERIS L1b products from 4th to 3rd reprocessing.
 *
 * @author olafd
 */
@OperatorMetadata(alias = "Meris.Adapt.4To3",
        category = "Optical/Pre-Processing",
        version = "1.0",
        authors = "Olaf Danne",
        copyright = "(c) 2021 by Brockmann Consult",
        description = "Provides the adaptation of MERIS L1b products from 4th to 3rd reprocessing.")
public class Meris4thTo3rdReprocessingOp extends Operator {

    @SourceProduct(alias = "sourceProduct",
            label = "MERIS L1b 4th reprocessing product",
            description = "The MERIS L1b 4th reprocessing source product.")
    private Product sourceProduct;

    @TargetProduct(description = "The target product.")
    private Product targetProduct;



    @Override
    public void initialize() throws OperatorException {
        final boolean inputProductIsValid = validateInputProduct();
        if (!inputProductIsValid) {
            throw new OperatorException("Input is not a valid MERIS L1b 4th reprocessing product");
        }

        // adapt to 3rd reprocessing...
        Meris3rd4thReprocessingAdapter reprocessingAdapter = new Meris3rd4thReprocessingAdapter();
        targetProduct = reprocessingAdapter.convertToLowerVersion(sourceProduct);
    }

    private boolean validateInputProduct() {
        return sourceProduct.getProductType().startsWith("ME_1");  // todo: discuss this criterion
    }

    /**
     * The Service Provider Interface (SPI) for the operator.
     * It provides operator meta-data and is a factory for new operator instances.
     */
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(Meris4thTo3rdReprocessingOp.class);
        }
    }
}
