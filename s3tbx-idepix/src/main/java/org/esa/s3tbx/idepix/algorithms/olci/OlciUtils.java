package org.esa.s3tbx.idepix.algorithms.olci;

import org.esa.s3tbx.idepix.core.IdepixFlagCoding;
import org.esa.s3tbx.processor.rad2refl.Rad2ReflConstants;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.math.MathUtils;

/**
 * Utility class for Idepix OLCI
 *
 * @author olafd
 */

public class OlciUtils {

    /**
     * Provides OLCI pixel classification flag coding
     *
     * @param flagId - the flag ID
     *
     * @return - the flag coding
     */
    public static FlagCoding createOlciFlagCoding(String flagId) {
        return IdepixFlagCoding.createDefaultFlagCoding(flagId);
    }

    /**
     * Provides OLCI pixel classification flag bitmask
     *
     * @param classifProduct - the pixel classification product
     */
    public static void setupOlciClassifBitmask(Product classifProduct) {
        IdepixFlagCoding.setupDefaultClassifBitmask(classifProduct);
    }

    public static boolean areAllRequiredL1bBandsAvailable(Product l1bProduct) {
        // required: SZA, OZA, Oa*_radiance, quality_flags, solar_flux_band_n (for Rad2Refl)
        return assertBands(l1bProduct, Rad2ReflConstants.OLCI_RAD_BAND_NAMES) &&
                assertBands(l1bProduct, Rad2ReflConstants.OLCI_SOLAR_FLUX_BAND_NAMES) &&
                assertBands(l1bProduct, "SZA") &&
                assertBands(l1bProduct, "OZA") &&
                assertBands(l1bProduct, "quality_flags");
    }

    private static boolean assertBands(Product l1bProduct, String... expectedBands) {
        for (String expectedBand : expectedBands) {
            if (!l1bProduct.containsRasterDataNode(expectedBand)) {
                return false;
            }
        }
        return true;
    }

}
