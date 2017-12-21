package org.esa.s3tbx.idepix.algorithms.olci;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s3tbx.idepix.core.IdepixConstants;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;

import java.awt.*;
import java.util.Map;

/**
 * Operator providing cloud flag derived from O2 correction quantities.
 *
 * @author olafd
 */
@OperatorMetadata(alias = "Idepix.Olci.O2cloud",
        version = "1.0",
        internal = true,
        authors = "Olaf Danne",
        copyright = "(c) 2016 by Brockmann Consult",
        description = "Operator providing cloud flag derived from O2 correction quantities.")
public class O2CloudOp extends Operator {

    @SourceProduct(alias = "l1b", description = "The source product.")
    Product sourceProduct;

    @SourceProduct(alias = "o2", optional = true)
    private Product o2Product;

    @TargetProduct(description = "The target product.")
    Product targetProduct;

    private Band trans13Band;
    private Band altitudeBand;
    private TiePointGrid szaTpg;
    private TiePointGrid ozaTpg;

    private int[] cameraBounds;


    @Override
    public void initialize() throws OperatorException {

        createTargetProduct();

        trans13Band = o2Product.getBand("trans_13");
        altitudeBand = sourceProduct.getBand("altitude");
        szaTpg = sourceProduct.getTiePointGrid("SZA");
        ozaTpg = sourceProduct.getTiePointGrid("OZA");

        if (sourceProduct.getName().contains("_EFR") || sourceProduct.getProductType().contains("_EFR")) {
            cameraBounds = OlciConstants.CAMERA_BOUNDS_FR;
        } else if (sourceProduct.getName().contains("_ERR") || sourceProduct.getProductType().contains("_ERR")) {
            cameraBounds = OlciConstants.CAMERA_BOUNDS_RR;
        } else {
            throw new OperatorException(IdepixConstants.INPUT_INCONSISTENCY_ERROR_MESSAGE);
        }

    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {

        final Tile trans13Tile = getSourceTile(trans13Band, targetRectangle);
        final Tile altitudeTile = getSourceTile(altitudeBand, targetRectangle);
        final Tile szaTile = getSourceTile(szaTpg, targetRectangle);
        final Tile ozaTile = getSourceTile(ozaTpg, targetRectangle);
        final Tile trans13BaselineTargetTile = targetTiles.get(targetProduct.getBand("trans13_baseline"));
        final Tile trans13BaselineAMFCorrTargetTile = targetTiles.get(targetProduct.getBand("trans13_baseline_AMFcorr"));
        final Tile trans13ExcessTargetTile = targetTiles.get(targetProduct.getBand("trans13_excess"));
        final Tile o2CloudTargetTile = targetTiles.get(targetProduct.getBand("o2_cloud"));

        final Band olciQualityFlagBand = sourceProduct.getBand(OlciConstants.OLCI_QUALITY_FLAGS_BAND_NAME);
        final Tile olciQualityFlagTile = getSourceTile(olciQualityFlagBand, targetRectangle);

        try {
            for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
                checkForCancellation();
                for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {
                    final double trans13 = trans13Tile.getSampleDouble(x, y);
                    final double altitude = altitudeTile.getSampleDouble(x, y);
                    final double sza = szaTile.getSampleDouble(x, y);
                    final double oza = ozaTile.getSampleDouble(x, y);
                    final boolean isBright =
                            olciQualityFlagTile.getSampleBit(x, y, OlciConstants.L1_F_BRIGHT);

                    final O2Correction o2Corr =
                            O2Correction.computeO2CorrectionTerms(cameraBounds, x, trans13, altitude,
                                                                  sza, oza, isBright);

                    o2CloudTargetTile.setSample(x, y, o2Corr.isO2Cloud() ? 1 : 0);
                    trans13BaselineTargetTile.setSample(x, y, o2Corr.getTrans13Baseline());
                    trans13BaselineAMFCorrTargetTile.setSample(x, y, o2Corr.getTrans13BaselineAmfCorr());
                    trans13ExcessTargetTile.setSample(x, y, o2Corr.getTrans13Excess());
                }
            }
        } catch (Exception e) {
            throw new OperatorException("Failed to provide O2 cloud flag and parameters:\n" + e.getMessage(), e);
        }

    }

    void createTargetProduct() throws OperatorException {
        int sceneWidth = sourceProduct.getSceneRasterWidth();
        int sceneHeight = sourceProduct.getSceneRasterHeight();

        targetProduct = new Product(sourceProduct.getName(), sourceProduct.getProductType(), sceneWidth, sceneHeight);

        ProductUtils.copyTiePointGrids(sourceProduct, targetProduct);

        ProductUtils.copyGeoCoding(sourceProduct, targetProduct);
        targetProduct.setStartTime(sourceProduct.getStartTime());
        targetProduct.setEndTime(sourceProduct.getEndTime());
        ProductUtils.copyMetadata(sourceProduct, targetProduct);

        targetProduct.addBand("trans13_baseline", ProductData.TYPE_FLOAT32);
        targetProduct.addBand("trans13_baseline_AMFcorr", ProductData.TYPE_FLOAT32);
        targetProduct.addBand("trans13_excess", ProductData.TYPE_FLOAT32);
        targetProduct.addBand("o2_cloud", ProductData.TYPE_INT8);
    }

    /**
     * The Service Provider Interface (SPI) for the operator.
     * It provides operator meta-data and is a factory for new operator instances.
     */
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(O2CloudOp.class);
        }
    }
}
