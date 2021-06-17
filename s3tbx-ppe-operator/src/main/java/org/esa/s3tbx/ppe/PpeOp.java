/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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


package org.esa.s3tbx.ppe;


import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.BitSetter;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.converters.BooleanExpressionConverter;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Operator which performs Prompt Particle Event (PPE) filtering on OLCI L1B data.
 */
@OperatorMetadata(
        alias = "PpeFiltering",
        version = "1.1",
        category = "Optical/Preprocessing",
        description = "Performs Prompt Particle Event (PPE) filtering on OLCI L1B",
        authors = "Juancho Gossn, Roman Shevchuk, Marco Peters",
        copyright = "(c) 2019 by Brockmann Consult GmbH")

public class PpeOp extends Operator {


    private static final String VALID_PIXEL_EXPRESSION = "not quality_flags_land";
    private static final String OLCI_REFLEC_NAME_REGEX = "Oa\\d\\d_radiance";


    @SourceProduct(label = "OLCI L1B", alias = "source", description = "OLCI L1B source product", type = "OL_1_E(F|R)R")
    private Product sourceProduct;

    @TargetProduct()
    private Product targetProduct;

    @Parameter(label = "Filtering cut-off", defaultValue = "0.7",
            description = "Minimum threshold to differentiate with the neighboring pixels.")
    private double cutOff;

    @Parameter(label = "Number of MAD", defaultValue = "10",
            description = "Multiplier of MAD (Median Absolute Deviation) used for the threshold.")
    private double numberOfMAD;

    @Parameter(label = "Valid pixel expression", description = "An expression to filter which pixel are considered.",
            converter = BooleanExpressionConverter.class, defaultValue = VALID_PIXEL_EXPRESSION)
    private String validExpression;


    private Mask validPixelMask;

    @Override
    public void initialize() throws OperatorException {
        validPixelMask = Mask.BandMathsType.create("__valid_pixel_mask", null,
                                                   getSourceProduct().getSceneRasterWidth(),
                                                   getSourceProduct().getSceneRasterHeight(),
                                                   validExpression,
                                                   Color.GREEN, 0.0);
        validPixelMask.setOwner(getSourceProduct());

        createTargetProduct();
    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {

        Map<Band, Tile> internalTargetTiles = new HashMap<>(targetTiles);
        Tile flagTile = internalTargetTiles.remove(targetProduct.getBand("ppe_flags"));
        Tile landTile = getSourceTile(validPixelMask, targetRectangle);
        Tile sourceTile;
        pm.beginTask("Processing PPE", internalTargetTiles.size());
        try {
            for (Map.Entry<Band, Tile> entry : internalTargetTiles.entrySet()) {
                checkForCancellation();
                Band targetBand = entry.getKey();
                Tile targetTile = entry.getValue();

                sourceTile = getSourceTile(sourceProduct.getRasterDataNode(targetBand.getName()), targetRectangle);
                for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
                    checkForCancellation();
                    for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {
                        double reflecValue = sourceTile.getSampleDouble(x, y);
                        if (reflecValue > 0 && landTile.getSampleBoolean(x, y)) {
                            double[] pixelList = getPixelList(x, y, sourceTile);
                            double median = getMedian(pixelList);
                            double mad = getMAD(pixelList);
                            setBandTile(x, y, median, mad, reflecValue, targetTile);
                            setFlagBandTile(x, y, median, mad, reflecValue, targetBand.getSpectralBandIndex(), flagTile);
                        } else {
                            targetTile.setSample(x, y, reflecValue);
                        }
                    }
                }
                pm.worked(1);
            }
        } finally {
            pm.done();
        }
    }

    private void createTargetProduct() {
        targetProduct = new Product(sourceProduct.getName(), sourceProduct.getProductType(),
                                    sourceProduct.getSceneRasterWidth(),
                                    sourceProduct.getSceneRasterHeight());

        ProductUtils.copyProductNodes(sourceProduct, targetProduct);

        final FlagCoding flagCoding = new FlagCoding("PPE_Applied");
        flagCoding.setDescription("PPE processor flag");
        targetProduct.getFlagCodingGroup().add(flagCoding);

        Pattern compile = Pattern.compile(OLCI_REFLEC_NAME_REGEX);
        for (Band band : sourceProduct.getBands()) {
            String bandName = band.getName();
            if (compile.matcher(bandName).matches()) {
                ProductUtils.copyBand(bandName, sourceProduct, targetProduct, false);
                int flagMask = BitSetter.setFlag(0, band.getSpectralBandIndex());
                flagCoding.addFlag("PPE_" + bandName, flagMask, "PPE applied on " + bandName);
            } else {
                boolean alreadyCopied = targetProduct.containsBand(bandName);
                if (!alreadyCopied) {
                    // TODO: don't copy the band again - see above TODO
                    ProductUtils.copyBand(bandName, sourceProduct, targetProduct, true);
                }
            }
        }

        Band ppeFlags = targetProduct.addBand("ppe_flags", ProductData.TYPE_UINT32);
        ppeFlags.setSampleCoding(flagCoding);

        targetProduct.addMask("PPE_operator_applied",  "ppe_flags != 0",
                              "PPE operator has been applied on one of the spectral bands", Color.BLUE, 0.5);

    }

    private void setBandTile(int x, int y, double median, double mad, double reflecValue, Tile targetTile) {
        if (Math.abs(reflecValue - median) > cutOff && Math.abs(reflecValue - median) > (numberOfMAD * mad)) {
            targetTile.setSample(x, y, median);
        } else {
            targetTile.setSample(x, y, reflecValue);
        }
    }


    private void setFlagBandTile(int x, int y, double median, double mad, double reflecValue, int bandIndex, Tile targetTile) {
        if (Math.abs(reflecValue - median) > cutOff && Math.abs(reflecValue - median) > (numberOfMAD * mad)) {
            targetTile.setSample(x, y, bandIndex, true);
        }
    }


    static double[] getPixelList(int x, int y, Tile sourceTile) {
        double[] pixelList = new double[5];
        pixelList[0] = getPixelValue(sourceTile, x, y - 2);
        pixelList[1] = getPixelValue(sourceTile, x, y - 1);
        pixelList[2] = getPixelValue(sourceTile, x, y);
        pixelList[3] = getPixelValue(sourceTile, x, y + 1);
        pixelList[4] = getPixelValue(sourceTile, x, y + 2);
        Arrays.sort(pixelList);
        if (pixelList[0] < 0) {
            throw new OperatorException("Radiance bands contain values lower than zero at x=" + x + " y=" + y);
        }
        return pixelList;
    }

    static Double getPixelValue(Tile tile, int x, int y) {
        if ((y >= tile.getMinY()) && (y <= tile.getMaxY())) {
            return tile.getSampleDouble(x, y);
        } else {
            return 0d;
        }
    }

    static Double getMedian(double[] listDoubles) {
        Arrays.sort(listDoubles);
        if (listDoubles[1] == 0) {
            return (listDoubles[3]);
        } else if (listDoubles[0] == 0) {
            return ((listDoubles[2] + listDoubles[3]) / 2);
        } else {
            return (listDoubles[2]);
        }
    }

    static Double getMAD(double[] listDoubles) {
        Double median = getMedian(listDoubles);
        double[] listMAD = new double[5];
        for (int i = 0; i < 5; i++) {
            if (listDoubles[i] != 0) {
                listMAD[i] = Math.abs(listDoubles[i] - median);
            } else {
                listMAD[i] = -1;
            }
        }
        Arrays.sort(listMAD);
        if (listMAD[1] == -1) {
            return listMAD[3];
        } else if (listMAD[0] == -1) {
            return (listMAD[2] + listMAD[3]) / 2;
        } else {
            return listMAD[2];
        }
    }


    public static class Spi extends OperatorSpi {

        public Spi() {
            super(PpeOp.class);
        }
    }
}
