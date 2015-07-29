package org.esa.s3tbx.c2rcc;

import org.esa.s3tbx.c2rcc.meris.C2rccMerisOperator;
import org.esa.s3tbx.c2rcc.modis.C2rccModisOperator;
import org.esa.s3tbx.c2rcc.seawifs.C2rccSeaWiFSOperator;
import org.esa.snap.dataio.envisat.EnvisatConstants;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.gpf.Operator;
import org.esa.snap.framework.gpf.OperatorException;
import org.esa.snap.framework.gpf.OperatorSpi;
import org.esa.snap.framework.gpf.annotations.OperatorMetadata;
import org.esa.snap.framework.gpf.annotations.Parameter;
import org.esa.snap.framework.gpf.annotations.SourceProduct;
import org.esa.snap.framework.gpf.annotations.TargetProduct;
import org.esa.snap.util.converters.BooleanExpressionConverter;

/**
 * The Case 2 Regional / CoastColour Operator for MERIS, MODIS, SeaWiFS, and VIIRS.
 * <p>
 * Computes AC-reflectances and IOPs from MERIS, MODIS, SeaWiFS, and VIIRS L1b data products using
 * an neural-network approach.
 *
 * @author Norman Fomferra
 * @author Sabine Embacher
 */
@OperatorMetadata(alias = "c2rcc", version = "0.2",
        authors = "Roland Doerffer, Norman Fomferra, Sabine Embacher (Brockmann Consult)",
        category = "Optical Processing/Thematic Water Processing",
        copyright = "Copyright (C) 2015 by Brockmann Consult",
        description = "Performs atmospheric correction and IOP retrieval on MERIS, MODIS, SeaWiFS, and VIIRS L1b data products.")
public class C2rccOperator extends Operator {

    @SourceProduct(description = "MERIS, MODIS, SeaWiFS, or VIIRS L1b product")
    private Product sourceProduct;

    @TargetProduct
    private Product targetProduct;

    @Parameter(label = "Valid-pixel expression",
            converter = BooleanExpressionConverter.class)
    private String validPixelExpression;

    @Parameter(defaultValue = "35.0", unit = "DU", interval = "(0, 100)")
    private double salinity;

    @Parameter(defaultValue = "15.0", unit = "C", interval = "(-50, 50)")
    private double temperature;

    @Parameter(defaultValue = "false", label = "Output top-of-standard-atmosphere (TOSA) reflectances")
    private boolean outputRtosa;

    @Parameter(defaultValue = "false")
    private boolean useDefaultSolarFlux;

    @Override
    public void initialize() throws OperatorException {
        final String productType = sourceProduct.getProductType();
        final String formatName = sourceProduct.getProductReader().getReaderPlugIn().getFormatNames()[0];
        if (EnvisatConstants.ENVISAT_FORMAT_NAME.equals(formatName) && productType.startsWith("MER_RR__1P")) {
            C2rccMerisOperator c2rccMerisOperator = new C2rccMerisOperator();
            c2rccMerisOperator.setSourceProduct(sourceProduct);
            c2rccMerisOperator.setTemperature(temperature);
            c2rccMerisOperator.setSalinity(salinity);
            c2rccMerisOperator.setUseDefaultSolarFlux(useDefaultSolarFlux);
            c2rccMerisOperator.setValidPixelExpression(validPixelExpression);
            c2rccMerisOperator.setOutputRtosa(outputRtosa);
            targetProduct = c2rccMerisOperator.getTargetProduct();
        } else if ("SeaDAS-L2".equals(formatName) && productType.startsWith("Level 2")) {
            final C2rccModisOperator c2rccModisOperator = new C2rccModisOperator();
            c2rccModisOperator.setSourceProduct(sourceProduct);
            c2rccModisOperator.setTemperature(temperature);
            c2rccModisOperator.setSalinity(salinity);
            c2rccModisOperator.setValidPixelExpression(validPixelExpression);
            c2rccModisOperator.setOutputRtosa(outputRtosa);
            targetProduct = c2rccModisOperator.getTargetProduct();
        } else if ("SeaDAS-L1".equals(formatName)&& productType.startsWith("Generic Level 1B")) {
            final C2rccSeaWiFSOperator c2RccSeaWiFSOperator = new C2rccSeaWiFSOperator();
            c2RccSeaWiFSOperator.setSourceProduct(sourceProduct);
            c2RccSeaWiFSOperator.setTemperature(temperature);
            c2RccSeaWiFSOperator.setSalinity(salinity);
//            c2RccSeaWiFSOperator.setUseDefaultSolarFlux(useDefaultSolarFlux); // todo needed?
            c2RccSeaWiFSOperator.setValidPixelExpression(validPixelExpression);
            c2RccSeaWiFSOperator.setOutputRtosa(outputRtosa);
            targetProduct = c2RccSeaWiFSOperator.getTargetProduct();
        } else {
            throw new OperatorException("Illegal source product.");
        }
    }

    public static class Spi extends OperatorSpi {
        public Spi() {
            super(C2rccOperator.class);
        }
    }
}
