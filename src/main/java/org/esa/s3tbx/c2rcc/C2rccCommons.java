package org.esa.s3tbx.c2rcc;

import org.esa.snap.core.datamodel.ConstantTimeCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;

public class C2rccCommons {

    public static void ensureTimeCoding_Fallback(Product product) {
        if (product.getTimeCoding() == null) {
            final ProductData.UTC startTime = product.getStartTime();
            final ProductData.UTC endTime = product.getEndTime();
            if (startTime != null && endTime != null) {
                double startTimeMJD = startTime.getMJD();
                double constantTime = (endTime.getMJD() - startTimeMJD) / 2.0 + startTimeMJD;
                product.setTimeCoding(new ConstantTimeCoding(constantTime));
            }
        }
    }
}
