package org.esa.s3tbx.c2rcc.onns;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.converters.BooleanExpressionConverter;

@OperatorMetadata(alias = "ONNS", version = "0.18",
        authors = "Martin Hieronymi (HZG), Dagmar Müller, Marco Peters (Brockmann Consult)",
        internal = true,
        category = "Optical/Thematic Water Processing",
        copyright = "Copyright (C) 2016 by Brockmann Consult",
        description = "Performs IOP retrieval on atmospherically corrected OLCI product.")
public class OnnsOperator extends Operator {
    /*
        c2rcc ops have been removed from Graph Builder. In the layer xml they are disabled
        see https://senbox.atlassian.net/browse/SNAP-395
    */


    @SourceProduct(description = "OLCI atm. corr rrs product", label = "OLCI RRS product")
    private Product sourceProduct;

    @SourceProduct(description = "A second source product which is congruent to the L1b source product but contains cloud flags. " +
                                 "So the user can define a valid pixel expression referring both, the L1b and the cloud flag " +
                                 "containing source product. Expression example: '!quality_flags.invalid && !quality_flags.land && !$cloudProduct.l2_flags.CLOUD' ",
            optional = true,
            label = "Product with cloud flag")
    private Product cloudProduct;

    @TargetProduct
    private Product targetProduct;

    @Parameter(label = "Valid-pixel expression",
            defaultValue = "!quality_flags.invalid && (!quality_flags.land || quality_flags.fresh_inland_water)",
            description = "Defines the pixels which are valid for processing",
            converter = BooleanExpressionConverter.class)
    private String validPixelExpression;

    @Parameter(description = "Path to alternative class defintion file",
            label = "Alternative class defintion file")
    private String alternativeClassPath;

    @Parameter(description = "Path to an alternative set of neuronal nets. Use this to replace the standard " +
                             "set of neuronal nets with the ones in the given directory.",
            label = "Alternative NN path")
    private String alternativeNNPath;

    @Parameter(defaultValue = "RRS", description = "The type of the input reflectance",
            label = "Input AC reflectances in")
    private REFLECTANCE inputReflectanceIs;

    @Parameter(defaultValue = "OA$$_reflec", description = "Name pattern of reflectance bands",
            label = "Input reflectance name pattern")
    private String inputReflPattern;

    @Parameter(defaultValue = "10e-4", description = "Minimum class membership value",
            label = "Min. class membership")
    private float minClassMembership;

    @Parameter(defaultValue = "false", description = "Passthrough the AC reflectances",
            label = "Output AC reflectances")
    private boolean outputAcReflectances;

    @Parameter(defaultValue = "true", description = "Output IOPs",
            label = "Output IOPs")
    private boolean outputIops;

    @Parameter(defaultValue = "true", description = "Output concentrations (CHL and TSM)",
            label = "Output concentrations")
    private boolean outputConcentrations;

    @Parameter(defaultValue = "false", description = "Output Kd and FU",
            label = "Output Kd and FU")
    private boolean outputKdFu;

    @Parameter(defaultValue = "false", description = "Output Uncertainties",
            label = "Output uncertainties")
    private boolean outputUncertainties;

    @Override
    public void initialize() throws OperatorException {
        targetProduct = new Product(sourceProduct.getName(), sourceProduct.getProductType() + "_ONNS",
                                    sourceProduct.getSceneRasterWidth(), sourceProduct.getSceneRasterHeight());
        ProductUtils.copyGeoCoding(sourceProduct, targetProduct);
    }

    public enum REFLECTANCE {
        RRS,
        RHOW
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(OnnsOperator.class);
        }
    }
}
