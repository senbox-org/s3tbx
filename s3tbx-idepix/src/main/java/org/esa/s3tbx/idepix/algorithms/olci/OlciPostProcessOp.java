package org.esa.s3tbx.idepix.algorithms.olci;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s3tbx.idepix.core.operators.CloudBuffer;
import org.esa.s3tbx.idepix.core.CloudShadowFronts;
import org.esa.s3tbx.idepix.core.IdepixConstants;
import org.esa.s3tbx.idepix.core.util.IdepixIO;
import org.esa.s3tbx.idepix.core.util.IdepixUtils;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.TiePointGrid;
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
 * - cloud buffer
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

    @Parameter(defaultValue = "true",
            label = " Compute a cloud buffer",
            description = " Compute a cloud buffer")
    private boolean computeCloudBuffer;

    @Parameter(defaultValue = "2", interval = "[0,100]",
            description = "The width of a cloud 'safety buffer' around a pixel which was classified as cloudy.",
            label = "Width of cloud buffer (# of pixels)")
    private int cloudBufferWidth;

    @Parameter(defaultValue = "false",
            label = " Compute cloud shadow",
            description = " Compute cloud shadow with latest 'fronts' algorithm. Requires CTP.")
    private boolean computeCloudShadow;

    @SourceProduct(alias = "l1b")
    private Product l1bProduct;

    @SourceProduct(alias = "olciCloud")
    private Product olciCloudProduct;

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


        if (computeCloudBuffer) {
            rectCalculator = new RectangleExtender(new Rectangle(l1bProduct.getSceneRasterWidth(),
                                                                 l1bProduct.getSceneRasterHeight()),
                                                   cloudBufferWidth, cloudBufferWidth
            );
        }
        if (computeCloudShadow) {
            ctpBand = olciCloudProduct.getBand("ctp");
            int extendedWidth;
            int extendedHeight;
            if (l1bProduct.getName().contains("FR____")) {
                extendedWidth = 64;     // todo: check these values
                extendedHeight = 64;
            } else {
                extendedWidth = 16;
                extendedHeight = 16;
            }
            rectCalculator = new RectangleExtender(new Rectangle(l1bProduct.getSceneRasterWidth(),
                                                                 l1bProduct.getSceneRasterHeight()),
                                                   extendedWidth, extendedHeight
            );
        }


        ProductUtils.copyBand(IdepixConstants.CLASSIF_BAND_NAME, olciCloudProduct, postProcessedCloudProduct, false);
        setTargetProduct(postProcessedCloudProduct);
    }

    @Override
    public void computeTile(Band targetBand, final Tile targetTile, ProgressMonitor pm) throws OperatorException {
        Rectangle targetRectangle = targetTile.getRectangle();
        final Rectangle srcRectangle = rectCalculator.extend(targetRectangle);

        final Tile sourceFlagTile = getSourceTile(origCloudFlagBand, srcRectangle);

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

        if (computeCloudBuffer) {
            for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
                checkForCancellation();
                for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {
                    if (targetRectangle.contains(x, y)) {
                        final boolean isCloud = targetTile.getSampleBit(x, y, IdepixConstants.IDEPIX_CLOUD);
                        if (isCloud) {
                            CloudBuffer.computeSimpleCloudBuffer(x, y,
                                                                 targetTile,
                                                                 srcRectangle,
                                                                 cloudBufferWidth,
                                                                 IdepixConstants.IDEPIX_CLOUD_BUFFER);
                        }
                    }
                }
            }

            for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
                checkForCancellation();
                for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {
                    IdepixUtils.consolidateCloudAndBuffer(targetTile, x, y);
                }
            }
        }

        if (computeCloudShadow) {
            Tile szaTile = getSourceTile(szaTPG, srcRectangle);
            Tile saaTile = getSourceTile(saaTPG, srcRectangle);
            Tile ctpTile = getSourceTile(ctpBand, srcRectangle);
            Tile altTile = getSourceTile(altBand, targetRectangle);
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
