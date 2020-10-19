package org.esa.s3tbx.dataio;

import org.esa.s3tbx.dataio.s3.Sentinel3ProductReaderPlugIn;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.dataio.ProductIOPlugIn;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.util.StopWatch;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class SpeedTestSLSTR {

    @Test
    public void testOpen() throws IOException {
        final Sentinel3ProductReaderPlugIn plugIn = new Sentinel3ProductReaderPlugIn();
        final ProductReader reader = plugIn.createReaderInstance();

        Product product = null;
        try {
            final StopWatch stopWatch = new StopWatch();

            product = ProductIO.readProduct("D:\\Satellite\\SST-CCI\\SLSTR\\S3A_SL_1_RBT____20170508T072701_20170508T073201_20180629T141902_0299_017_234______LR1_R_NT_003.SEN3\\xfdumanifest.xml");

            final TiePointGrid lonGrid = product.getTiePointGrid("longitude_tx");
            final TiePointGrid latGrid = product.getTiePointGrid("latitude_tx");

            ProductData data = lonGrid.getGridData();
            assertEquals(190000, data.getNumElems());

            data = latGrid.getGridData();
            assertEquals(190000, data.getNumElems());

            stopWatch.stop();
            System.out.println(stopWatch.getTimeDiffString());
        } finally {
            if (product != null) {
                product.dispose();
            }
        }
    }
}
