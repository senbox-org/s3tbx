package org.esa.s3tbx.c2rcc;

import org.esa.s3tbx.c2rcc.ancillary.AtmosphericAuxdata;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ConstantTimeCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.TimeCoding;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.pointop.Sample;

public class C2rccCommons {

    public static TimeCoding getTimeCoding(Product product) {
        if (product.getSceneTimeCoding() != null) {
            return product.getSceneTimeCoding();
        }
        final ProductData.UTC startTime = product.getStartTime();
        final ProductData.UTC endTime = product.getEndTime();
        if (endTime == null || startTime == null) {
            throw new OperatorException("Could not retrieve time information from source product");
        }
        return getTimeCoding(startTime, endTime);
    }

    public static TimeCoding getTimeCoding(ProductData.UTC startTime, ProductData.UTC endTime) {
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("Arguments startTime and endTime must be given.");
        } else {
            double startTimeMJD = startTime.getMJD();
            double constantTime = (endTime.getMJD() - startTimeMJD) / 2.0 + startTimeMJD;
            return new ConstantTimeCoding(constantTime);
        }
    }

    public static void ensureTimeInformation(Product product, ProductData.UTC startTime, ProductData.UTC endTime, TimeCoding timeCoding) {
        if(product.getStartTime() == null) {
            product.setStartTime(startTime);
        }
        if(product.getEndTime() == null) {
            product.setEndTime(endTime);
        }
        if (product.getSceneTimeCoding() == null) {
            product.setSceneTimeCoding(timeCoding);
        }
    }

    public static boolean areSamplesValid(Sample[] sourceSamples, int x, int y) {
        boolean samplesValid = true;
        for (Sample sourceSample : sourceSamples) {
            // can be null because samples for ozone and atm_pressure might be missing
            RasterDataNode node = sourceSample.getNode();
            if (node != null) {
                if (!node.isPixelValid(x, y)) {
                    samplesValid = false;
                    break;
                }
            }
        }
        return samplesValid;
    }

    public static Band addBand(Product targetProduct, String name, String unit, String description) {
        Band targetBand = targetProduct.addBand(name, ProductData.TYPE_FLOAT32);
        targetBand.setUnit(unit);
        targetBand.setDescription(description);
        targetBand.setGeophysicalNoDataValue(Double.NaN);
        targetBand.setNoDataValueUsed(true);
        return targetBand;
    }

    public static Band addVirtualBand(Product targetProduct, String name, String expression, String unit, String description) {
        Band band = targetProduct.addBand(name, expression);
        band.setUnit(unit);
        band.setDescription(description);
        band.getSourceImage(); // trigger source image creation
        band.setGeophysicalNoDataValue(Double.NaN);
        band.setNoDataValueUsed(true);
        return band;
    }

    public static double fetchSurfacePressure(AtmosphericAuxdata atmosphericAuxdata, double timeMJD, int x, int y, double lat, double lon) {
        try {
            return atmosphericAuxdata.getSurfacePressure(timeMJD, x, y, lat, lon);
        } catch (Exception e) {
            throw new OperatorException("Unable to fetch surface pressure value from auxdata.", e);
        }
    }

    public static double fetchOzone(final AtmosphericAuxdata atmosphericAuxdata, double timeMJD, int x, int y, double lat, double lon) {
        try {
            return atmosphericAuxdata.getOzone(timeMJD, x, y, lat, lon);
        } catch (Exception e) {
            throw new OperatorException("Unable to fetch ozone value from auxdata.", e);
        }
    }
}
