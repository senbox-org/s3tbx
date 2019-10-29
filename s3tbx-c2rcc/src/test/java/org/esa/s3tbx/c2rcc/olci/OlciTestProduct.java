package org.esa.s3tbx.c2rcc.olci;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import java.util.Date;

/**
 * @author Marco Peters
 */
class OlciTestProduct {
    static Product create() throws FactoryException, TransformException {
        Product product = new Product("test-olci", "t", 1, 1);
        for (int i = 1; i <= C2rccOlciOperator.BAND_COUNT; i++) {
            String expression = String.valueOf(i);
            product.addBand(String.format("Oa%02d_radiance", i), expression);
            product.addBand("solar_flux_band_" + i, expression);
        }

        Date time = new Date();
        product.setStartTime(ProductData.UTC.create(time, 0));
        product.setEndTime(ProductData.UTC.create(time, 500));


        product.addBand(C2rccOlciOperator.RASTER_NAME_ALTITUDE, "500");
        product.addBand(C2rccOlciOperator.RASTER_NAME_SUN_AZIMUTH, "42");
        product.addBand(C2rccOlciOperator.RASTER_NAME_SUN_ZENITH, "42");
        product.addBand(C2rccOlciOperator.RASTER_NAME_VIEWING_AZIMUTH, "42");
        product.addBand(C2rccOlciOperator.RASTER_NAME_VIEWING_ZENITH, "42");
        product.addBand(C2rccOlciOperator.RASTER_NAME_SEA_LEVEL_PRESSURE, "999");
        product.addBand(C2rccOlciOperator.RASTER_NAME_TOTAL_OZONE, "0.004");
        Band flagBand = product.addBand(C2rccOlciOperator.RASTER_NAME_QUALITY_FLAGS, ProductData.TYPE_INT8);
        FlagCoding l1FlagsCoding = new FlagCoding(C2rccOlciOperator.RASTER_NAME_QUALITY_FLAGS);
        product.getFlagCodingGroup().add(l1FlagsCoding);
        flagBand.setSampleCoding(l1FlagsCoding);

        product.setSceneGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84, 1, 1, 10, 50, 1, 1));

        return product;
    }
}
