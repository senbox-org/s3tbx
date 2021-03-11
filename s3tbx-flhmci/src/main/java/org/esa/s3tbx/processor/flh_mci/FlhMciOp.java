/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.s3tbx.processor.flh_mci;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.Validator;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.pointop.PixelOperator;
import org.esa.snap.core.gpf.pointop.ProductConfigurer;
import org.esa.snap.core.gpf.pointop.Sample;
import org.esa.snap.core.gpf.pointop.SourceSampleConfigurer;
import org.esa.snap.core.gpf.pointop.TargetSampleConfigurer;
import org.esa.snap.core.gpf.pointop.WritableSample;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.converters.BooleanExpressionConverter;

/**
 * An operator for computing fluorescence line height (FLH) or maximum chlorophyll index (MCI).
 *
 * @author Tom Block
 * @author Ralf Quast
 * @author Marco Peters
 */
@OperatorMetadata(alias = "FlhMci", authors = "Tom Block, Ralf Quast, Marco Peters", copyright = "Brockmann Consult GmbH",
        category = "Optical/Thematic Water Processing",
        version = "3.1",
        description = "Computes fluorescence line height (FLH) or maximum chlorophyll index (MCI).")
public class FlhMciOp extends PixelOperator {

    @SourceProduct(alias = "source", label = "Source product", description = "The source product.")
    private Product sourceProduct;

    @Parameter(defaultValue = "NONE", label = "Preset", description = "Sets default values according to the selected preset. " +
            "The specific parameters have precedence and override the ones from the preset")
    private Presets preset;
    @Parameter(description = "The name for the lower wavelength band defining the baseline",
            rasterDataNodeType = Band.class)
    private String lowerBaselineBandName;
    @Parameter(description = "The name of the upper wavelength band defining the baseline",
            rasterDataNodeType = Band.class)
    private String upperBaselineBandName;
    @Parameter(description = " The name of the signal band, i.e. the band for which the baseline height is calculated",
            rasterDataNodeType = Band.class)
    private String signalBandName;
    @Parameter(description = "The name of the line height band in the target product",
               validator = NodeNameValidator.class)
    private String lineHeightBandName;
    @Parameter(description = "Activates or deactivates calculating the slope parameter",
               defaultValue = "true",
               label = "Generate slope parameter")
    private boolean slope;
    @Parameter(description = "The name of the slope band in the target product",
               validator = NodeNameValidator.class)
    private String slopeBandName;
    @Parameter(description = "A ROI-mask expression used to identify pixels of interest",
               converter = BooleanExpressionConverter.class)
    private String maskExpression;
    @Parameter(description = "The cloud correction factor used during calculation",
               defaultValue = "1.005")
    private float cloudCorrectionFactor;
    @Parameter(defaultValue = "NaN",
               label = "Invalid FLH/MCI value",
               description = "Value used to fill invalid FLH/MCI pixels")
    private float invalidFlhMciValue;

    private transient BaselineAlgorithm algorithm;

    private transient int currentPixel = 0;

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {
        checkCancellation();

        final float signal = sourceSamples[0].getFloat();
        final float lower = sourceSamples[1].getFloat();
        final float upper = sourceSamples[2].getFloat();

        targetSamples[0].set(algorithm.computeLineHeight(lower, upper, signal));
        if (slope) {
            targetSamples[1].set(algorithm.computeSlope(lower, upper));
        }
    }

    private void checkCancellation() {
        if (currentPixel % 1000 == 0) {
            checkForCancellation();
            currentPixel = 0;
        }
        currentPixel++;
    }

    @Override
    protected void configureSourceSamples(SourceSampleConfigurer sc) throws OperatorException {
        sc.setValidPixelMask(maskExpression);
        sc.defineSample(0, signalBandName);
        sc.defineSample(1, lowerBaselineBandName);
        sc.defineSample(2, upperBaselineBandName);
    }

    @Override
    protected void configureTargetSamples(TargetSampleConfigurer sc) throws OperatorException {
        sc.defineSample(0, lineHeightBandName);
        if (slope) {
            sc.defineSample(1, slopeBandName);
        }
    }

    @Override
    protected void configureTargetProduct(ProductConfigurer productConfigurer) {
        super.configureTargetProduct(productConfigurer);

        final Band lineHeightBand = productConfigurer.addBand(lineHeightBandName, ProductData.TYPE_FLOAT32);

        final Band signalBand = sourceProduct.getBand(signalBandName);
        lineHeightBand.setUnit(signalBand.getUnit());
        lineHeightBand.setDescription("Line height band");
        lineHeightBand.setValidPixelExpression(maskExpression);
        lineHeightBand.setNoDataValueUsed(true);
        lineHeightBand.setNoDataValue(invalidFlhMciValue);

        ProductUtils.copySpectralBandProperties(signalBand, lineHeightBand);

        if (slope) {
            final Band slopeBand = productConfigurer.addBand(slopeBandName, ProductData.TYPE_FLOAT32);
            slopeBand.setUnit(signalBand.getUnit() + " nm-1");
            slopeBand.setDescription("Baseline slope band");
            slopeBand.setNoDataValueUsed(true);
            slopeBand.setNoDataValue(invalidFlhMciValue);
            slopeBand.setValidPixelExpression(maskExpression);
        }

        ProductUtils.copyFlagBands(sourceProduct, productConfigurer.getTargetProduct(), true);
    }

    @Override
    protected void prepareInputs() throws OperatorException {
        super.prepareInputs();
        setParametersFromPreset();
        validateParameters();

        float lowerLambda = getWavelength(lowerBaselineBandName);
        float signalLambda = getWavelength(signalBandName);
        float upperLambda = getWavelength(upperBaselineBandName);

        algorithm = new BaselineAlgorithm();
        algorithm.setWavelengths(lowerLambda, upperLambda, signalLambda);
        algorithm.setCloudCorrectionFactor(cloudCorrectionFactor);
    }

    private void validateParameters() throws OperatorException {
        assertParameterBandNameValid(lowerBaselineBandName, "lowerBaselineBandName");
        assertBandValid(lowerBaselineBandName);
        assertParameterBandNameValid(upperBaselineBandName, "upperBaselineBandName");
        assertBandValid(upperBaselineBandName);
        assertParameterBandNameValid(signalBandName, "signalBandName");
        assertBandValid(signalBandName);
        assertParameterBandNameValid(lineHeightBandName, "lineHeightBandName");
        if (slope) {
            assertParameterBandNameValid(slopeBandName, "slopeBandName");
        }
    }

    private void assertBandValid(String bandName) throws OperatorException {
        final Band band = sourceProduct.getBand(bandName);
        if (band == null) {
            throw new OperatorException(bandName + " can not be found in source product");
        }
        if (!band.getRasterSize().equals(sourceProduct.getSceneRasterSize())) {
            throw new OperatorException(bandName + " is not of same size as source product");
        }
    }

    private void assertParameterBandNameValid(String parameterValue, String parameterName) {
        if (StringUtils.isNullOrEmpty(parameterValue)) {
            throw new OperatorException(String.format("Parameter '%s' not specified", parameterName));
        }
    }

    private float getWavelength(String bandName) {
        final Band band = sourceProduct.getBand(bandName);
        final float wavelength = band.getSpectralWavelength();
        if (wavelength == 0.0f) {
            throw new OperatorException(
                    "The band '" + band.getName() + "' is not a spectral band.\nPlease select a spectral band for processing.");
        }
        return wavelength;
    }

    private void setParametersFromPreset() {
        if (preset != Presets.NONE) {
            if (upperBaselineBandName == null) {
                upperBaselineBandName = preset.getUpperBaselineBandName();
            }
            if (lowerBaselineBandName == null) {
                lowerBaselineBandName = preset.getLowerBaselineBandName();
            }
            if (signalBandName == null) {
                signalBandName = preset.getSignalBandName();
            }
            if (lineHeightBandName == null) {
                lineHeightBandName = preset.getLineHeightBandName();
            }
            if (slopeBandName == null) {
                slopeBandName = preset.getSlopeBandName();
            }
            if (maskExpression == null) {
                maskExpression = preset.getMaskExpression();
            }
        }
    }

    public static class NodeNameValidator implements Validator {

        @Override
        public void validateValue(Property property, Object value) {
            ProductNode.isValidNodeName(value.toString());
        }
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(FlhMciOp.class);
        }
    }
}
