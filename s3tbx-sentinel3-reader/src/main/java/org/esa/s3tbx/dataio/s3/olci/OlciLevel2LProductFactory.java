package org.esa.s3tbx.dataio.s3.olci;

import org.esa.s3tbx.dataio.s3.Sentinel3ProductReader;
import org.esa.snap.core.datamodel.Product;

/**
 * @author Tonio Fincke
 */
public class OlciLevel2LProductFactory extends OlciProductFactory {

    public OlciLevel2LProductFactory(Sentinel3ProductReader productReader) {
        super(productReader);
    }

    @Override
    protected String getValidExpression() {
        return "!LQSF.INVALID";
    }

    @Override
    protected void setAutoGrouping(Product[] sourceProducts, Product targetProduct) {
        targetProduct.setAutoGrouping("IWV:OGVI:OTCI:RC681:RC865:atmospheric_temperature_profile:" +
                "lambda0:FWHM:solar_flux");
    }

    @Override
    protected void setMasks(Product targetProduct) {
        super.setMasks(targetProduct);
        String octiMaskName = "OTCI_Unreliable_RECOM";
        targetProduct.addMask(octiMaskName, "LQSF.CLOUD or LQSF.CLOUD_AMBIGUOUS or LQSF.CLOUD_MARGIN or " +
                "LQSF.SNOW_ICE or LQSF.OTCI_FAIL or LQSF.OTCI_CLASS_CLSN",
                "Excluding pixels that are deemed unreliable for OTCI. Flag recommended by QWG.",
                getColorProvider().getMaskColor(octiMaskName), 0.5);
        String ogviMaskName = "OGVI_Unreliable_RECOM";
        targetProduct.addMask(ogviMaskName, "LQSF.CLOUD or LQSF.CLOUD_AMBIGUOUS or LQSF.CLOUD_MARGIN or " +
                        "LQSF.SNOW_ICE or LQSF.OGVI_FAIL",
                "Excluding pixels that are  deemed unreliable for OGVI. Flag recommended by QWG.",
                getColorProvider().getMaskColor(ogviMaskName), 0.5);
    }
}
