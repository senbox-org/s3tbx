package org.esa.s3tbx.dataio.s3.slstr;

import org.esa.s3tbx.dataio.s3.Manifest;
import org.esa.s3tbx.dataio.s3.Sentinel3ProductReader;
import org.esa.snap.core.datamodel.Product;

import java.io.IOException;
import java.util.List;

/**
 * @author Tonio Fincke
 */
public class SlstrLevel1B1kmProductFactory extends SlstrLevel1FixedResolutionProductFactory {

    public SlstrLevel1B1kmProductFactory(Sentinel3ProductReader productReader) {
        super(productReader);
    }

    @Override
    protected String getProductName() {
        return super.getProductName() + "_1km";
    }

    @Override
    protected List<String> getFileNames(Manifest manifest) {
        List <String> fileNames = super.getFileNames(manifest);
        fileNames.removeIf(name -> name.endsWith("an.nc") || name.endsWith("ao.nc") ||
                                   name.endsWith("bn.nc") || name.endsWith("bo.nc") ||
                                   name.endsWith("cn.nc") || name.endsWith("co.nc"));
        return fileNames;
    }

    @Override
    protected Product findMasterProduct() {
        final List<Product> productList = getOpenProductList();
        Product masterProduct = new Product("dummy", "type", 1, 1);
        for (Product product : productList) {
            int masterSize = masterProduct.getSceneRasterWidth() * masterProduct.getSceneRasterHeight();
            int productSize = product.getSceneRasterWidth() * product.getSceneRasterHeight();
            if (productSize > masterSize &&
                !product.getName().contains("flags") &&
                !product.getName().endsWith("tn") &&
                !product.getName().endsWith("tx") &&
                !product.getName().endsWith("to")) {
                masterProduct = product;
            }
        }
        return masterProduct;
    }

    @Override
    protected void setAutoGrouping(Product[] sourceProducts, Product targetProduct) {
        String bandGrouping = getAutoGroupingString(sourceProducts);
        targetProduct.setAutoGrouping("F*BT_in:F*exception_in:" +
                                      "F*BT_io:F*exception_io:" +
                                      "S*BT_in:S*exception_in:" +
                                      "S*BT_io:S*exception_io:" +
                                      "specific_humidity:temperature_profile:" +
                                      bandGrouping);
    }

    @Override
    protected boolean isOrphanPixelsAllowed() {
        return false;
    }

    @Override
    protected void setTimeCoding(Product targetProduct) throws IOException {
        setTimeCoding(targetProduct, "time_in.nc", "time_stamp_i");
    }
}
