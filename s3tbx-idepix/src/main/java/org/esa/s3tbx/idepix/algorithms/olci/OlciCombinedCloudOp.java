package org.esa.s3tbx.idepix.algorithms.olci;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s3tbx.idepix.core.IdepixConstants;
import org.esa.s3tbx.idepix.core.util.IdepixIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.SourceProduct;

import java.awt.*;

/**
 * Operator providing 'combined cloud' for OLCI (request DM/JM). Experimental.
 *
 * @author olafd
 */
@OperatorMetadata(alias = "Idepix.Olci.CombinedCloud",
        version = "1.0",
        internal = true,
        authors = "Olaf Danne",
        copyright = "(c) 2018 by Brockmann Consult",
        description = "Operator providing 'combined cloud' for OLCI (request DM/JM). Experimental.")
public class OlciCombinedCloudOp extends Operator {

    @SourceProduct(alias = "rad2refl")
    private Product rad2reflProduct;

    @SourceProduct(alias = "olciCloud")
    private Product olciCloudProduct;

    private Band origCloudFlagBand;
    private Band qualityFlagsBand;

    private Band[] reflBands;
    private Band refl17Band;
    private Band nnValueBand;

    @Override
    public void initialize() throws OperatorException {
        Product combinedCloudProduct = IdepixIO.createCompatibleTargetProduct(olciCloudProduct,
                                                                                   "combinedCloud",
                                                                                   "combinedCloud",
                                                                                   true);

        origCloudFlagBand = olciCloudProduct.getBand(IdepixConstants.CLASSIF_BAND_NAME);
        qualityFlagsBand = olciCloudProduct.getBand(OlciConstants.OLCI_QUALITY_FLAGS_BAND_NAME);

        reflBands = new Band[7];
        for (int i = 0; i < 7; i++) {
            reflBands[i] = rad2reflProduct.getBand("Oa0" + (i + 2) + "_reflectance");
        }
        refl17Band = rad2reflProduct.getBand("Oa17_reflectance");
        nnValueBand = olciCloudProduct.getBand(IdepixConstants.NN_OUTPUT_BAND_NAME);

        combinedCloudProduct.addBand(OlciConstants.OLCI_COMBINED_CLOUD_BAND_NAME, ProductData.TYPE_INT8);
        setTargetProduct(combinedCloudProduct);
    }

    @Override
    public void computeTile(Band targetBand, final Tile targetTile, ProgressMonitor pm) throws OperatorException {
        Rectangle targetRectangle = targetTile.getRectangle();

        final Tile idepixFlagsTile = getSourceTile(origCloudFlagBand, targetRectangle);
        final Tile qualityFlagsTile = getSourceTile(qualityFlagsBand, targetRectangle);

        Tile[] reflTiles = new Tile[7];
        for (int i = 0; i < 7; i++) {
            reflTiles[i] = getSourceTile(reflBands[i], targetRectangle);
        }
        final Tile refl17Tile = getSourceTile(refl17Band, targetRectangle);
        final Tile nnValueTile = getSourceTile(nnValueBand, targetRectangle);

        for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
            checkForCancellation();
            for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {
                final double meanReflVis = getMeanRefl(reflTiles, x, y);
                final double ndvi = getNdvi(refl17Tile, reflTiles[6], x, y);
                final float nnValue = nnValueTile.getSampleFloat(x, y);

                final boolean isIdepixCloud = idepixFlagsTile.getSampleBit(x, y, IdepixConstants.IDEPIX_CLOUD);
                final boolean isIdepixLand = idepixFlagsTile.getSampleBit(x, y, IdepixConstants.IDEPIX_LAND);
                final boolean isFreshInlandWater = qualityFlagsTile.getSampleBit(x, y, OlciConstants.L1_F_FRESH_INLAND_WATER);

                final boolean cloud_1 = isIdepixCloud && ndvi < 0.1 && ndvi > 0.0 && meanReflVis > 0.2;
                final boolean cloud_2 = isIdepixCloud && isIdepixLand;
                final boolean cloud_3 = isIdepixCloud && ndvi < 0.0;
                final boolean cloud_4 = isFreshInlandWater &&
                        ((meanReflVis > 0.2 && nnValue < 5.3) ||
                                (meanReflVis > 0.25 && nnValue > 5.3));

                final int combinedCloud = (cloud_1 || cloud_2 || cloud_3 || cloud_4) ? 1 : 0;
                targetTile.setSample(x, y, combinedCloud);
            }
        }

    }

    private double getMeanRefl(Tile[] reflTiles, int x, int y) {
        double mean = 0.0;
        for (Tile reflTile : reflTiles) {
            mean += reflTile.getSampleDouble(x, y);
        }
        return mean / reflTiles.length;
    }

    private double getNdvi(Tile upperTile, Tile lowerTile, int x, int y) {
        final double upper = upperTile.getSampleDouble(x, y);
        final double lower = lowerTile.getSampleDouble(x, y);

        if (upper + lower == 0.0
                || Double.isNaN(upper) || Double.isNaN(lower)
                || Double.isInfinite(upper) || Double.isInfinite(lower)) {
            return Double.NaN;
        } else {
            return (upper - lower) / (upper + lower);
        }
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(OlciCombinedCloudOp.class);
        }
    }

}
