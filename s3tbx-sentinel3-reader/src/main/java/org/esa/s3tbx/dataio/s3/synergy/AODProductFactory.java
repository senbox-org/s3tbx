package org.esa.s3tbx.dataio.s3.synergy;

import com.bc.ceres.glevel.MultiLevelImage;
import org.esa.s3tbx.dataio.s3.AbstractProductFactory;
import org.esa.s3tbx.dataio.s3.Manifest;
import org.esa.s3tbx.dataio.s3.Sentinel3ProductReader;
import org.esa.s3tbx.dataio.s3.util.S3NetcdfReader;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoCodingFactory;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.TiePointGrid;

import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class AODProductFactory extends AbstractProductFactory {

    public AODProductFactory(Sentinel3ProductReader productReader) {
        super(productReader);
    }

    @Override
    protected List<String> getFileNames(Manifest manifest) {
        return manifest.getFileNames(new String[0]);
    }

    @Override
    protected Product readProduct(String fileName, Manifest manifest) throws IOException {
        final File file = new File(getInputFileParentDirectory(), fileName);
        if (!file.exists()) {
            return null;
        }
        return new S3NetcdfReader().readProductNodes(file, null);
    }

    @Override
    protected void setGeoCoding(Product targetProduct) {
        // @todo 1 tb/tb replace this with the new implementation 2020-01-27
        // @todo 1 tb/tb implement masking decorator 2020-01-27
        if (targetProduct.containsBand("latitude") && targetProduct.containsBand("longitude")) {
            GeoCoding geoCoding = GeoCodingFactory.createPixelGeoCoding(
                    targetProduct.getBand("latitude"), targetProduct.getBand("longitude"),
                    "latitude > -999 and longitude > -999", 5);
            targetProduct.setSceneGeoCoding(geoCoding);
        }
    }

    @Override
    protected void setAutoGrouping(Product[] sourceProducts, Product targetProduct) {
        targetProduct.setAutoGrouping("AOD:SSA:Surface_reflectance");
    }

    protected double[] loadTiePointData(TiePointGrid tiePointGrid) {
        final MultiLevelImage mlImage = getImageForTpg(tiePointGrid);
        final Raster tpData = mlImage.getImage(0).getData();
        final double[] tiePoints = new double[tpData.getWidth() * tpData.getHeight()];
        tpData.getPixels(0, 0, tpData.getWidth(), tpData.getHeight(), tiePoints);
        return tiePoints;
    }
}
