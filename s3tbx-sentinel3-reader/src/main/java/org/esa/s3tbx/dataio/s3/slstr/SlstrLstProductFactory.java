package org.esa.s3tbx.dataio.s3.slstr;/*
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

import org.esa.s3tbx.dataio.s3.Manifest;
import org.esa.s3tbx.dataio.s3.Sentinel3ProductReader;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoCodingFactory;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.TiePointGeoCoding;

import java.io.IOException;
import java.util.List;

public class SlstrLstProductFactory extends SlstrProductFactory {

    public SlstrLstProductFactory(Sentinel3ProductReader productReader) {
        super(productReader);
    }

    @Override
    protected List<String> getFileNames(Manifest manifest) {
        return manifest.getFileNames("");
    }

    @Override
    protected Product findMasterProduct() {
        final List<Product> productList = getOpenProductList();
        Product masterProduct = new Product("dummy", "type", 1, 1);
        for (Product product : productList) {
            int masterSize = masterProduct.getSceneRasterWidth() * masterProduct.getSceneRasterHeight();
            int productSize = product.getSceneRasterWidth() * product.getSceneRasterHeight();
            if (productSize > masterSize &&
                    !product.getName().endsWith("tn") &&
                    !product.getName().endsWith("tx")) {
                masterProduct = product;
            }
        }
        return masterProduct;
    }

    @Override
    protected void setAutoGrouping(Product[] sourceProducts, Product targetProduct) {}

    @Override
    protected void setGeoCoding(Product targetProduct) throws IOException {
        final Band latBand = targetProduct.getBand("latitude_in");
        final Band lonBand = targetProduct.getBand("longitude_in");
        if (latBand != null && lonBand != null) {
            targetProduct.setSceneGeoCoding(
                    GeoCodingFactory.createPixelGeoCoding(latBand, lonBand, "!confidence_in_duplicate", 5));
        }
        if (targetProduct.getSceneGeoCoding() == null) {
            if (targetProduct.getTiePointGrid("latitude_tx") != null && targetProduct.getTiePointGrid(
                    "longitude_tx") != null) {
                targetProduct.setSceneGeoCoding(new TiePointGeoCoding(targetProduct.getTiePointGrid("latitude_tx"),
                                                                      targetProduct.getTiePointGrid("longitude_tx")));
            }
        }
    }

    @Override
    protected Double getStartOffset(String gridIndex) {
        return 0.0;
    }

    @Override
    protected Double getTrackOffset(String gridIndex) {
        return 0.0;
    }

    @Override
    protected RasterDataNode addSpecialNode(Product masterProduct, Band sourceBand, Product targetProduct) {
        //todo use sensible values as soon as they are provided
        int subSamplingX = 1;
        int subSamplingY = 1;
        return copyBandAsTiePointGrid(sourceBand, targetProduct, subSamplingX, subSamplingY, 0.0f, 0.0f);
    }
}
