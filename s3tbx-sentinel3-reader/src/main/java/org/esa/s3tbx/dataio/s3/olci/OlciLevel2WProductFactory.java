package org.esa.s3tbx.dataio.s3.olci;

import org.esa.s3tbx.dataio.s3.Sentinel3ProductReader;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;

/**
 * @author Tonio Fincke
 */
public class OlciLevel2WProductFactory extends OlciProductFactory {

    public OlciLevel2WProductFactory(Sentinel3ProductReader productReader) {
        super(productReader);
    }

    @Override
    protected String getValidExpression() {
        return "!WQSF_lsb.INVALID";
    }

    @Override
    protected void setAutoGrouping(Product[] sourceProducts, Product targetProduct) {
        targetProduct.setAutoGrouping("Oa*_reflectance:Oa*_reflectance_err:A865:ADG:CHL:IWV:KD490:PAR:T865:TSM:" +
                "atmospheric_temperature_profile:lambda0:FWHM:solar_flux");
    }

    @Override
    protected void setMasks(Product targetProduct) {
        super.setMasks(targetProduct);

        String oceanColourExpression = "(WQSF_lsb.WATER or WQSF_lsb.INLAND_WATER) and not (WQSF_lsb.CLOUD or " +
                "WQSF_lsb.CLOUD_AMBIGUOUS or WQSF_lsb.CLOUD_MARGIN or WQSF_lsb.INVALID or WQSF_lsb.COSMETIC or " +
                "WQSF_lsb.SATURATED or WQSF_lsb.SUSPECT or WQSF_lsb.HISOLZEN or WQSF_lsb.HIGHGLINT or WQSF_lsb.SNOW_ICE)";
        String openWaterExpression = "not (WQSF_lsb.AC_FAIL or WQSF_lsb.WHITECAPS or WQSF_lsb.ADJAC or " +
                "WQSF_msb.RWNEG_O2 or WQSF_msb.RWNEG_O3 or WQSF_msb.RWNEG_O4 or WQSF_msb.RWNEG_O5 or " +
                "WQSF_msb.RWNEG_O6 or WQSF_msb.RWNEG_O7 or WQSF_msb.RWNEG_O8)";

        addMask(targetProduct,
                maskName("Oa**reflectance"),
                "not(" + getExpressionForBand(targetProduct, "Oa01_reflectance") + " and " + oceanColourExpression
                        + " and " + openWaterExpression + ")",
                "Not Open Waters Water Leaving Reflectances. Flag recommended by QWG.");
        addMask(targetProduct,
                maskName("CHL_OC4ME"),
                "not(" + getExpressionForBand(targetProduct, "CHL_OC4ME") + " and " + oceanColourExpression + " and "
                        + openWaterExpression + " and not WQSF_lsb.OC4ME_FAIL)",
                "Not Open Waters Algal Pigment Concentration. Flag recommended by QWG.");
        addMask(targetProduct,
                maskName("KD490_M07"),
                "not(" + getExpressionForBand(targetProduct, "KD490_M07") + " and " + oceanColourExpression + " and "
                        + openWaterExpression + " and not WQSF_lsb.KDM_FAIL)",
                "Not Open Waters Diffuse Attenuation Coefficient. Flag recommended by QWG.");
        addMask(targetProduct,
                maskName("PAR"),
                "not(" + getExpressionForBand(targetProduct, "PAR") + " and " + oceanColourExpression + " and "
                        + openWaterExpression + " and not WQSF_lsb.PAR_FAIL)",
                "Not Open Waters Photosynthetically Active Radiation. Flag recommended by QWG.");
        addMask(targetProduct,
                maskName("W_AER"),
                "not(" + getExpressionForBand(targetProduct, "T865") + " and "
                        + getExpressionForBand(targetProduct, "A865") + " and " + oceanColourExpression + " and "
                        + openWaterExpression  + ")",
                "Not Open Waters Aerosol Optical Thickness and Angstrom exponent. Flag recommended by QWG.");
        addMask(targetProduct,
                maskName("CHL_NN"),
                "not(" + getExpressionForBand(targetProduct, "CHL_NN") + " and " + oceanColourExpression +
                        " and not WQSF_lsb.OCNN_FAIL)",
                "Not Complex Waters Algal Pigment Concentration. Flag recommended by QWG");
        addMask(targetProduct,
                maskName("TSM_NN"),
                "not(" + getExpressionForBand(targetProduct, "TSM_NN") + " and " + oceanColourExpression +
                        " and not WQSF_lsb.OCNN_FAIL)",
                "Not Complex Waters Total Suspended Matter Concentration. Flag recommended by QWG");
        addMask(targetProduct,
                maskName("ADG443_NN"),
                "not(" + getExpressionForBand(targetProduct, "ADG443_NN") + " and " + oceanColourExpression +
                        " and not WQSF_lsb.OCNN_FAIL)",
                "Not Complex Waters Coloured Detrital and Dissolved Material Absorption. Flag recommended by QWG");
        addMask(targetProduct,
                maskName("IWV"),
                "not(" + getExpressionForBand(targetProduct, "IWV") + " and not WQSF_lsb.MEGLINT " +
                        "and not WQSF_lsb.WV_FAIL)",
                "Not Integrated Water Vapour Column. Flag recommended by QWG");
    }

    private String getExpressionForBand(Product targetProduct, String bandName) {
        Band band = targetProduct.getBand(bandName);
        double noDataValue = band.getGeophysicalNoDataValue();
        // we need to subtract an epsilon to account for rounding errors
        return bandName + " < " + noDataValue + " - 1e-5";
    }

    private void addMask(Product targetProduct, String maskName, String expression, String description) {
        targetProduct.addMask(maskName, expression, description, getColorProvider().getMaskColor(maskName), 0.5);
    }

    private String maskName(String maskNamePart) {
        return maskNamePart + "_RECOM";
    }
}
