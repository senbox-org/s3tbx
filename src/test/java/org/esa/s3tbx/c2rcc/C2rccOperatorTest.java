package org.esa.s3tbx.c2rcc;

import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.dataop.maptransf.Datum;
import org.esa.snap.dataio.envisat.EnvisatConstants;
import org.junit.Test;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Created by Norman on 14.07.2015.
 */
public class C2rccOperatorTest {
    @Test
    public void testMeris() throws Exception {
        final ProductReaderPlugIn readerPlugInMock = mock(ProductReaderPlugIn.class);
        when(readerPlugInMock.getFormatNames()).thenReturn(new String[]{EnvisatConstants.ENVISAT_FORMAT_NAME});

        final ProductReader readerMock = mock(ProductReader.class);
        when(readerMock.getReaderPlugIn()).thenReturn(readerPlugInMock);

        Product source = new Product("test", "MER_RR__1P", 10, 10);
        source.setProductReader(readerMock);
        source.setGeoCoding(getDummyGeoCoding());
        source.setStartTime(ProductData.UTC.parse("23-MAY-2010 09:59:12.278508"));
        source.setEndTime(ProductData.UTC.parse("23-MAY-2010 10:02:32.200875"));
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
        Band flagBand = source.addBand("l1_flags", "-1", ProductData.TYPE_UINT8);
        FlagCoding l1FlagCoding = new FlagCoding("l1_flags");
        l1FlagCoding.addFlag("INVALID", 1, "INVALID");
        l1FlagCoding.addFlag("LAND_OCEAN", 2, "LAND or OCEAN");
        source.getFlagCodingGroup().add(l1FlagCoding);
        flagBand.setSampleCoding(l1FlagCoding);

        C2rccOperator operator = new C2rccOperator();
        operator.setSourceProduct(source);
        operator.setParameter("useDefaultSolarFlux", Boolean.TRUE);
        operator.setParameter("useEcmwfAuxData", Boolean.TRUE);
        Product target = operator.getTargetProduct();

        assertNotNull(target);
    }

    @Test
    public void testModis() throws Exception {
        //todo to be implemented
//        Product source = new Product("test", "MER_RR__1P", 10, 10);
//        source.addBand("radiance_1", "20.0");
//        source.addBand("radiance_2", "20.0");
//        source.addBand("radiance_3", "20.0");
//        source.addBand("radiance_4", "20.0");
//        source.addBand("radiance_5", "20.0");
//        source.addBand("radiance_6", "20.0");
//        source.addBand("radiance_7", "20.0");
//        source.addBand("radiance_8", "20.0");
//        source.addBand("radiance_9", "20.0");
//        source.addBand("radiance_10", "20.0");
//        source.addBand("radiance_11", "20.0");
//        source.addBand("radiance_12", "20.0");
//        source.addBand("radiance_13", "20.0");
//        source.addBand("radiance_14", "20.0");
//        source.addBand("radiance_15", "20.0");
//        source.addBand("sun_zenith", "20.0");
//        source.addBand("sun_azimuth", "20.0");
//        source.addBand("view_zenith", "20.0");
//        source.addBand("view_azimuth", "20.0");
//        source.addBand("atm_press", "20.0");
//        source.addBand("dem_alt", "20.0");
//        source.addBand("ozone", "20.0");
//        source.addBand("l1_flags", "-1", ProductData.TYPE_UINT8);
//        source.setGeoCoding(getDummyGeoCoding());
//
//        C2rccOperator operator = new C2rccOperator();
//        operator.setSourceProduct(source);
//        Product target = operator.getTargetProduct();
//
//        assertNotNull(target);
    }

    private GeoCoding getDummyGeoCoding() {
        return new GeoCoding() {
            @Override
            public boolean isCrossingMeridianAt180() {
                return false;  //Todo change body of created method. Use File | Settings | File Templates to change
            }

            @Override
            public boolean canGetPixelPos() {
                return false;  //Todo change body of created method. Use File | Settings | File Templates to change
            }

            @Override
            public boolean canGetGeoPos() {
                return false;  //Todo change body of created method. Use File | Settings | File Templates to change
            }

            @Override
            public PixelPos getPixelPos(GeoPos geoPos, PixelPos pixelPos) {
                return null;  //Todo change body of created method. Use File | Settings | File Templates to change
            }

            @Override
            public GeoPos getGeoPos(PixelPos pixelPos, GeoPos geoPos) {
                return null;  //Todo change body of created method. Use File | Settings | File Templates to change
            }

            @Override
            public Datum getDatum() {
                return null;  //Todo change body of created method. Use File | Settings | File Templates to change
            }

            @Override
            public void dispose() {
                //Todo change body of created method. Use File | Settings | File Templates to change
            }

            @Override
            public CoordinateReferenceSystem getImageCRS() {
                return null;  //Todo change body of created method. Use File | Settings | File Templates to change
            }

            @Override
            public CoordinateReferenceSystem getMapCRS() {
                return null;  //Todo change body of created method. Use File | Settings | File Templates to change
            }

            @Override
            public CoordinateReferenceSystem getGeoCRS() {
                return null;  //Todo change body of created method. Use File | Settings | File Templates to change
            }

            @Override
            public MathTransform getImageToMapTransform() {
                return null;  //Todo change body of created method. Use File | Settings | File Templates to change
            }
        };
    }
}
