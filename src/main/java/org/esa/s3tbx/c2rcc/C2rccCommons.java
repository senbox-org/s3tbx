package org.esa.s3tbx.c2rcc;

import org.esa.snap.core.datamodel.ConstantTimeCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.OperatorException;

public class C2rccCommons {

    public static void ensureTimeCoding_Fallback(Product product) {
        if (product.getSceneTimeCoding() == null) {
            final ProductData.UTC startTime = product.getStartTime();
            final ProductData.UTC endTime = product.getEndTime();
            if (endTime == null || startTime == null) {
                throw new OperatorException("Could not retrieve time information from source product");
            }
            setTimeCoding(product, startTime, endTime);
        }
    }

    public static void setTimeCoding(Product product, ProductData.UTC startTime, ProductData.UTC endTime) {
        if (startTime != null && endTime != null) {
            product.setStartTime(startTime);
            product.setEndTime(endTime);
            double startTimeMJD = startTime.getMJD();
            double constantTime = (endTime.getMJD() - startTimeMJD) / 2.0 + startTimeMJD;
            product.setSceneTimeCoding(new ConstantTimeCoding(constantTime));
        }
    }
}
