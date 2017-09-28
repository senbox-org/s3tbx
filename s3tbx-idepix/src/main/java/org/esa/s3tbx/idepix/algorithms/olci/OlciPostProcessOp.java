package org.esa.s3tbx.idepix.algorithms.olci;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s3tbx.idepix.algorithms.CloudShadowFronts;
import org.esa.s3tbx.idepix.core.IdepixConstants;
import org.esa.s3tbx.idepix.core.util.IdepixIO;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.RectangleExtender;

import java.awt.*;

/**
 * Operator used to consolidate Idepix classification flag for OLCI:
 * - coastline refinement
 *
 * @author olafd
 */
@OperatorMetadata(alias = "Idepix.Olci.Postprocess",
        version = "1.0",
        internal = true,
        authors = "Olaf Danne",
        copyright = "(c) 2016 by Brockmann Consult",
        description = "Refines the OLCI pixel classification over both land and water.")
public class OlciPostProcessOp extends Operator {

    @Parameter(defaultValue = "false",
            label = " Compute cloud shadow",
            description = " Compute cloud shadow with latest 'fronts' algorithm. Requires CTP.")
    private boolean computeCloudShadow;

    @SourceProduct(alias = "l1b")
    private Product l1bProduct;

    @SourceProduct(alias = "olciCloud")
    private Product olciCloudProduct;

    private Band waterFractionBand;
    private Band origCloudFlagBand;

    private Band ctpBand;
    private TiePointGrid szaTPG;
    private TiePointGrid saaTPG;
    private Band altBand;

    private GeoCoding geoCoding;

    private RectangleExtender rectCalculator;

    @Override
    public void initialize() throws OperatorException {
        Product postProcessedCloudProduct = IdepixIO.createCompatibleTargetProduct(olciCloudProduct,
                                                                                   "postProcessedCloud",
                                                                                   "postProcessedCloud",
                                                                                   true);

        geoCoding = l1bProduct.getSceneGeoCoding();

        origCloudFlagBand = olciCloudProduct.getBand(IdepixConstants.CLASSIF_BAND_NAME);

        szaTPG = l1bProduct.getTiePointGrid("SZA");
        saaTPG = l1bProduct.getTiePointGrid("SAA");
        altBand = l1bProduct.getBand("altitude");

        ctpBand = olciCloudProduct.getBand("ctp");

        int extendedWidth;
        int extendedHeight;
        if (l1bProduct.getProductType().startsWith("MER_F")) {
            extendedWidth = 64;
            extendedHeight = 64;
        } else {
            extendedWidth = 16;
            extendedHeight = 16;
        }

        rectCalculator = new RectangleExtender(new Rectangle(l1bProduct.getSceneRasterWidth(),
                                                             l1bProduct.getSceneRasterHeight()),
                                               extendedWidth, extendedHeight
        );


        ProductUtils.copyBand(IdepixConstants.CLASSIF_BAND_NAME, olciCloudProduct, postProcessedCloudProduct, false);
        setTargetProduct(postProcessedCloudProduct);
    }

    @Override
    public void computeTile(Band targetBand, final Tile targetTile, ProgressMonitor pm) throws OperatorException {
        Rectangle targetRectangle = targetTile.getRectangle();
        final Rectangle srcRectangle = rectCalculator.extend(targetRectangle);

        final Tile sourceFlagTile = getSourceTile(origCloudFlagBand, srcRectangle);
        Tile szaTile = getSourceTile(szaTPG, srcRectangle);
        Tile saaTile = getSourceTile(saaTPG, srcRectangle);
        Tile ctpTile = getSourceTile(ctpBand, srcRectangle);

        Tile altTile = getSourceTile(altBand, targetRectangle);

        for (int y = srcRectangle.y; y < srcRectangle.y + srcRectangle.height; y++) {
            checkForCancellation();
            for (int x = srcRectangle.x; x < srcRectangle.x + srcRectangle.width; x++) {

                if (targetRectangle.contains(x, y)) {
                    boolean isCloud = sourceFlagTile.getSampleBit(x, y, IdepixConstants.IDEPIX_CLOUD);
                    combineFlags(x, y, sourceFlagTile, targetTile);
                    if (isCloud) {
                        targetTile.setSample(x, y, IdepixConstants.IDEPIX_SNOW_ICE, false);   // necessary??
                    }
                }
            }
        }

        if (computeCloudShadow) {
            CloudShadowFronts cloudShadowFronts = new CloudShadowFronts(
                    geoCoding,
                    srcRectangle,
                    targetRectangle,
                    szaTile, saaTile, ctpTile, altTile) {

                @Override
                protected boolean isCloudForShadow(int x, int y) {
                    if (!targetTile.getRectangle().contains(x, y)) {
                        return sourceFlagTile.getSampleBit(x, y, IdepixConstants.IDEPIX_CLOUD);
                    } else {
                        return targetTile.getSampleBit(x, y, IdepixConstants.IDEPIX_CLOUD);
                    }
                }

                @Override
                protected boolean isCloudFree(int x, int y) {
                    return !sourceFlagTile.getSampleBit(x, y, IdepixConstants.IDEPIX_CLOUD);
                }

                @Override
                protected boolean isSurroundedByCloud(int x, int y) {
                    return isPixelSurrounded(x, y, sourceFlagTile, IdepixConstants.IDEPIX_CLOUD);
                }

                @Override
                protected void setCloudShadow(int x, int y) {
                    targetTile.setSample(x, y, IdepixConstants.IDEPIX_CLOUD_SHADOW, true);
                }
            };
            cloudShadowFronts.computeCloudShadow();
        }
    }

    private void combineFlags(int x, int y, Tile sourceFlagTile, Tile targetTile) {
        int sourceFlags = sourceFlagTile.getSampleInt(x, y);
        int computedFlags = targetTile.getSampleInt(x, y);
        targetTile.setSample(x, y, sourceFlags | computedFlags);
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(OlciPostProcessOp.class);
        }
    }
    
}
