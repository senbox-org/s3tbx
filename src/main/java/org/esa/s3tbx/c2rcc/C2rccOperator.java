package org.esa.s3tbx.c2rcc;

import static org.esa.snap.util.StringUtils.isNotNullAndNotEmpty;

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
 * <p/>
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

    @Parameter(valueSet = {"", "meris", "modis", "seawifs", "viirs", "olci"})
    private String sensorName;

    @Override
    public void initialize() throws OperatorException {
        if (sourceProductIsMeris()) {
            C2rccMerisOperator c2rccMerisOperator = new C2rccMerisOperator();
            c2rccMerisOperator.setSourceProduct(sourceProduct);
            c2rccMerisOperator.setTemperature(temperature);
            c2rccMerisOperator.setSalinity(salinity);
            c2rccMerisOperator.setUseDefaultSolarFlux(useDefaultSolarFlux);
            c2rccMerisOperator.setValidPixelExpression(validPixelExpression);
            c2rccMerisOperator.setOutputRtosa(outputRtosa);
            targetProduct = c2rccMerisOperator.getTargetProduct();
        } else if (sourceProductIsModis()) {
            final C2rccModisOperator c2rccModisOperator = new C2rccModisOperator();
            c2rccModisOperator.setSourceProduct(sourceProduct);
            c2rccModisOperator.setTemperature(temperature);
            c2rccModisOperator.setSalinity(salinity);
            c2rccModisOperator.setValidPixelExpression(validPixelExpression);
            c2rccModisOperator.setOutputRtosa(outputRtosa);
            targetProduct = c2rccModisOperator.getTargetProduct();
        } else if (sourceProductIsSeawifs()) {
            final C2rccSeaWiFSOperator c2RccSeaWiFSOperator = new C2rccSeaWiFSOperator();
            c2RccSeaWiFSOperator.setSourceProduct(sourceProduct);
            c2RccSeaWiFSOperator.setTemperature(temperature);
            c2RccSeaWiFSOperator.setSalinity(salinity);
//            c2RccSeaWiFSOperator.setUseDefaultSolarFlux(useDefaultSolarFlux); // todo needed?
            c2RccSeaWiFSOperator.setValidPixelExpression(validPixelExpression);
            c2RccSeaWiFSOperator.setOutputRtosa(outputRtosa);
            targetProduct = c2RccSeaWiFSOperator.getTargetProduct();
        } else if (isNotNullAndNotEmpty(sensorName) && "viirs".equalsIgnoreCase(sensorName)) {
            throw new OperatorException("the VIIRS operator not implemented now.");
        } else if (isNotNullAndNotEmpty(sensorName) && "olci".equalsIgnoreCase(sensorName)) {
            throw new OperatorException("the OLCI operator not implemented now.");
        } else {
            throw new OperatorException("Illegal source product.");
        }
    }

    private boolean sourceProductIsMeris() {
        final String productType = sourceProduct.getProductType();
        final String formatName = sourceProduct.getProductReader().getReaderPlugIn().getFormatNames()[0];
        if (isNotNullAndNotEmpty(sensorName)) {
            return "meris".equalsIgnoreCase(sensorName);
        } else {
            return EnvisatConstants.ENVISAT_FORMAT_NAME.equals(formatName) && productType.startsWith("MER_RR__1P");
        }
    }
    private boolean sourceProductIsModis() {
        final String productType = sourceProduct.getProductType();
        final String formatName = sourceProduct.getProductReader().getReaderPlugIn().getFormatNames()[0];
        if (isNotNullAndNotEmpty(sensorName)) {
            return "modis".equalsIgnoreCase(sensorName);
        } else {
            return "SeaDAS-L2".equals(formatName) && productType.startsWith("Level 2");
        }

    }
    private boolean sourceProductIsSeawifs() {
        final String productType = sourceProduct.getProductType();
        final String formatName = sourceProduct.getProductReader().getReaderPlugIn().getFormatNames()[0];
        if (isNotNullAndNotEmpty(sensorName)) {
            return "seawifs".equalsIgnoreCase(sensorName);
        } else {
            return "SeaDAS-L1".equals(formatName) && productType.startsWith("Generic Level 1B");
        }
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(C2rccOperator.class);
        }
    }
}
