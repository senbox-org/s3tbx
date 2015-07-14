package org.esa.s3tbx.c2rcc;

import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.datamodel.ProductData;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * Created by Norman on 14.07.2015.
 */
public class C2rccOperatorTest {
    @Test
    public void testMeris() throws Exception {
        Product source = new Product("test", "MER_RR__1P", 10, 10);
        source.addBand("radiance_1", "20.0");
        source.addBand("radiance_2", "20.0");
        source.addBand("radiance_3", "20.0");
        source.addBand("radiance_4", "20.0");
        source.addBand("radiance_5", "20.0");
        source.addBand("radiance_6", "20.0");
        source.addBand("radiance_7", "20.0");
        source.addBand("radiance_8", "20.0");
        source.addBand("radiance_9", "20.0");
        source.addBand("radiance_10", "20.0");
        source.addBand("radiance_11", "20.0");
        source.addBand("radiance_12", "20.0");
        source.addBand("radiance_13", "20.0");
        source.addBand("radiance_14", "20.0");
        source.addBand("radiance_15", "20.0");
        source.addBand("sun_zenith", "20.0");
        source.addBand("sun_azimuth", "20.0");
        source.addBand("view_zenith", "20.0");
        source.addBand("view_azimuth", "20.0");
        source.addBand("atm_press", "20.0");
        source.addBand("dem_alt", "20.0");
        source.addBand("ozone", "20.0");
        source.addBand("l1_flags", "-1", ProductData.TYPE_UINT8);

        C2rccOperator operator = new C2rccOperator();
        operator.setSourceProduct(source);
        Product target = operator.getTargetProduct();

        assertNotNull(target);
    }
}
