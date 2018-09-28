package org.esa.s3tbx.idepix.algorithms.olcislstr;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s3tbx.idepix.core.operators.CloudBuffer;
import org.esa.s3tbx.idepix.core.IdepixConstants;
import org.esa.s3tbx.idepix.core.util.IdepixIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.util.ProductUtils;

import java.awt.*;

/**
 * Post-processes the OLCI/SLSTR pixel classification. Actually just adds cloud buffer.
 *
 * @author olafd
 */
@OperatorMetadata(alias = "Idepix.OlciSlstr.Postprocess",
        version = "1.0",
        internal = true,
        authors = "Olaf Danne",
        copyright = "(c) 2017 by Brockmann Consult",
        description = "Post-processes the OLCI/SLSTR pixel classification. Actually just adds cloud buffer.")
public class OlciSlstrPostProcessOp extends Operator {

    @Parameter(defaultValue = "2", label = "Width of cloud buffer (# of pixels)")
    private int cloudBufferWidth;

    @SourceProduct(alias = "olciSlstrCloud")
    private Product olciSlstrCloudProduct;

    private Band origCloudFlagBand;

    @Override
    public void initialize() throws OperatorException {
        Product postProcessedCloudProduct = IdepixIO.createCompatibleTargetProduct(olciSlstrCloudProduct,
                                                                                   "postProcessedCloud",
                                                                                   "postProcessedCloud",
                                                                                   true);

        origCloudFlagBand = olciSlstrCloudProduct.getBand(IdepixConstants.CLASSIF_BAND_NAME);
        ProductUtils.copyBand(IdepixConstants.CLASSIF_BAND_NAME, olciSlstrCloudProduct, postProcessedCloudProduct, false);
        setTargetProduct(postProcessedCloudProduct);
    }

    @Override
    public void computeTile(Band targetBand, final Tile targetTile, ProgressMonitor pm) throws OperatorException {
        Rectangle targetRectangle = targetTile.getRectangle();
        final Tile sourceFlagTile = getSourceTile(origCloudFlagBand, targetRectangle);

        for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
            checkForCancellation();
            for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {

                boolean isCloud = sourceFlagTile.getSampleBit(x, y, IdepixConstants.IDEPIX_CLOUD);
                combineFlags(x, y, sourceFlagTile, targetTile);
                if (isCloud) {
                    CloudBuffer.computeSimpleCloudBuffer(x, y,
                                                         targetTile, targetTile,
                                                         cloudBufferWidth,
                                                         IdepixConstants.IDEPIX_CLOUD,
                                                         IdepixConstants.IDEPIX_CLOUD_BUFFER);
                }
            }
        }
    }

    private void combineFlags(int x, int y, Tile sourceFlagTile, Tile targetTile) {
        int sourceFlags = sourceFlagTile.getSampleInt(x, y);
        int computedFlags = targetTile.getSampleInt(x, y);
        targetTile.setSample(x, y, sourceFlags | computedFlags);
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(OlciSlstrPostProcessOp.class);
        }
    }

}
