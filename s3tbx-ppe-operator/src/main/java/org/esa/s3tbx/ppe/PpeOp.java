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
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.converters.BooleanExpressionConverter;

import java.awt.*;
import java.util.Arrays;

/**
 *
 *
 */
@OperatorMetadata(
        alias = "PpeOperator",
        version = "1.1",
        category = "Optical/Preprocessing",
        description = "Performs Prompt Particle Event (PPE) filtering",
        authors = " ",
        copyright = "(c) 2018 by Brockmann Consult GmbH")

public class PpeOp extends Operator {


    private static final String VALID_PIXEL_EXPRESSION = "not quality_flags_land";


    @SourceProduct(alias = "source", description = "The source product")
    private Product sourceProduct;

    @TargetProduct()
    private Product targetProduct;

    @Parameter(label =  "Filtering cut-off, [mW.m-2.sr-1.nm-1]",defaultValue = "0.7",
            description = "Minimum threshold to differentiate with the neighboring pixels in [mW.m-2.sr-1.nm-1]")
    private double cutOff;

    @Parameter(label =  "Filtering cut-off, number of Median Absolute Deviation",defaultValue = "10",
            description = "Multiplier of Median Absolute Deviation used for the threshold.")
    private double numberOfMAD;

    @Parameter(label = "Valid pixel expression", description = "An expression to filter which pixel are considered.",
            converter = BooleanExpressionConverter.class,defaultValue = VALID_PIXEL_EXPRESSION)
    private String validExpression;

    @Parameter(label = "Band keyword", description = "An expression which has to be part of band name(s). Case insensitive.",
            converter = BooleanExpressionConverter.class,defaultValue = "radiance")
    private String bandKeyword;


    Mask validPixelMask;

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
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        Rectangle targetRectangle = targetTile.getRectangle();
        Tile sourceTile;
        if (!targetBand.getName().toLowerCase().contains("_ppe_flag")) {
            Tile flagTile = getSourceTile(targetProduct.getRasterDataNode(targetBand.getName()+"_ppe_flag"), targetRectangle);
            sourceTile = getSourceTile(sourceProduct.getRasterDataNode(targetBand.getName()), targetRectangle);
            Tile landTile=getSourceTile(validPixelMask, targetRectangle);
            for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
                checkForCancellation();
                for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {
                    double pixel = sourceTile.getSampleDouble(x, y);
                    if (pixel > 0 && (landTile.getSampleBoolean(x, y) == true)) {
                        double[] pixelList = getPixelList(x, y, sourceTile);
                        double median = getMedian(pixelList);
                        double MAD = getMAD(pixelList);
                        setBandTile(x, y, median, MAD, sourceTile, targetTile);
                        setFlagBandTile(x, y, median, MAD, sourceTile, flagTile);
                    } else {
                            targetTile.setSample(x, y, pixel);
                    }
                }
            }
        }
    }

    private void createTargetProduct()  {
        targetProduct = new Product(sourceProduct.getName(), sourceProduct.getProductType(), sourceProduct.getSceneRasterWidth(), sourceProduct.getSceneRasterHeight());

        final FlagCoding flagCoding = new FlagCoding("ppe_applied");
        flagCoding.addFlag("PPE applied",1,"PPE applied");
        flagCoding.setDescription("PPE proccesor flag");
        targetProduct.getFlagCodingGroup().add(flagCoding);

        for (Band band : sourceProduct.getBands()) {
            if (band.getName().toLowerCase().contains(bandKeyword.toLowerCase()) && (band.getSpectralWavelength()!=0.0f)){
                ProductUtils.copyBand(band.getName(), sourceProduct, targetProduct, false);
                if (!"mW.m-2.sr-1.nm-1".equals(band.getUnit())){
                    SystemUtils.LOG.warning("The units of "+band.getName()+" are not mW.m-2.sr-1.nm-1. Changing cut-off parameter is suggested.");
                }
                Band ppeBand = new Band(band.getName()+"_ppe_flag", ProductData.TYPE_INT8,sourceProduct.getSceneRasterWidth(),sourceProduct.getSceneRasterHeight());
                ppeBand.setSampleCoding(flagCoding);

                targetProduct.addBand(ppeBand);
            }
            else{
                ProductUtils.copyBand(band.getName(), sourceProduct, targetProduct, true);
            }
        }

        ProductUtils.copyMetadata(sourceProduct, targetProduct);
        ProductUtils.copyTiePointGrids(sourceProduct, targetProduct);
        ProductUtils.copyMasks(sourceProduct, targetProduct);
        ProductUtils.copyFlagBands(sourceProduct, targetProduct, true);
        ProductUtils.copyGeoCoding(sourceProduct, targetProduct);
    }

    void setBandTile(int x, int y,double median,double MAD, Tile sourceTile, Tile targetTile){
        double pixel = sourceTile.getSampleDouble(x, y);
        if (Math.abs(pixel - median) > cutOff && Math.abs(pixel - median) > (numberOfMAD * MAD)) {
            targetTile.setSample(x, y, median);
        }
        else{
            targetTile.setSample(x, y, pixel);
        }
    }


    void setFlagBandTile(int x, int y,double median,double MAD, Tile sourceTile, Tile targetTile){
        double pixel = sourceTile.getSampleDouble(x, y);
        if (Math.abs(pixel - median) > cutOff && Math.abs(pixel - median) > (numberOfMAD * MAD)) {
            targetTile.setSample(x, y, 1);
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
        if (pixelList[0]<0){
            throw new OperatorException("Radiance bands contain values lower than zero at x="+x+" y="+y);
        }
        return pixelList;
    }

     static Double getPixelValue(Tile tile, int x, int y) {
        if ( (0 <= y) && (y < tile.getHeight())) {
            return tile.getSampleDouble(x, y);
        } else {
            return 0d;
        }
    }

    static Double getMedian(double[] listDoubles) {
        Arrays.sort(listDoubles);
        if (listDoubles[1]==0) {
            return (listDoubles[3]);
        } else if (listDoubles[0]==0) {
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
        if (listMAD[1]==-1) {
            return listMAD[3];
        }
        else if (listMAD[0]==-1){
            return (listMAD[2]+listMAD[3])/2;
        }
        else {
            return listMAD[2];
        }
    }


    public static class Spi extends OperatorSpi {
        public Spi() {
            super(PpeOp.class);
        }
    }
}

