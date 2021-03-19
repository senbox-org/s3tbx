package org.esa.s3tbx.dataio.s3;

import org.esa.snap.core.datamodel.Product;

/**
 * Interface for adaptation of reprocessed L1 source data (i.e. MERIS 3rd <--> 4th reprocessing)
 *
 */
public interface ReprocessingAdapter {

    /**
     * Provides the conversion to lower version product for given higher version input product
     *
     * @param inputProduct - the higher version input product
     * @return Product lowerVersionProduct
     */
    Product convertToLowerVersion(Product inputProduct);

    /**
     * Provides the conversion to higher version product for given lower version input product
     *
     * @param inputProduct - the lower version input product
     * @return Product higherVersionProduct
     */
    Product convertToHigherVersion(Product inputProduct);
}
