package org.esa.s3tbx.idepix.algorithms.olci;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s3tbx.idepix.algorithms.CloudBuffer;
import org.esa.s3tbx.idepix.core.IdepixConstants;
import org.esa.s3tbx.idepix.core.util.IdepixIO;
import org.esa.s3tbx.idepix.core.util.IdepixUtils;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.util.RectangleExtender;

import java.awt.*;

/**
 * Idepix water/land merge operator for OLCI.
 *
 * @author olafd
 */
@OperatorMetadata(alias = "Idepix.Olci.Merge.Landwater",
        version = "1.0",
        internal = true,
        authors = "Olaf Danne",
        copyright = "(c) 2016 by Brockmann Consult",
        description = "Idepix water/land merge operator for OLCI.")
public class OlciMergeLandWaterOp extends Operator {

    @Parameter(defaultValue = "true", label = " Compute a cloud buffer")
    private boolean computeCloudBuffer;

    @Parameter(defaultValue = "2", interval = "[0,100]",
            description = "The width of a cloud 'safety buffer' around a pixel which was classified as cloudy.",
            label = "Width of cloud buffer (# of pixels)")
    private int cloudBufferWidth;

    @SourceProduct(alias = "landClassif")
    private Product landClassifProduct;

    @SourceProduct(alias = "waterClassif")
    private Product waterClassifProduct;

    private Band waterClassifBand;

    private Band landClassifBand;
    private Band landNNBand;
    private Band waterNNBand;
    private Band mergedClassifBand;

    private Band landTrans13BaselineBand;
    private Band waterTrans13BaselineBand;
    private Band mergedTrans13BaselineBand;
    private Band landTrans13BaselineAmfcorrBand;
    private Band waterTrans13BaselineAmfcorrBand;
    private Band mergedTrans13BaselineAmfcorrBand;
    private Band landTrans13ExcessBand;
    private Band waterTrans13ExcessBand;
    private Band mergedTrans13ExcessBand;

    private Band mergedNNBand;
    private RectangleExtender rectCalculator;

    private boolean hasNNOutput;
    private boolean computeO2CorrectedTransmissions;

    @Override
    public void initialize() throws OperatorException {
        Product mergedClassifProduct = IdepixIO.createCompatibleTargetProduct(landClassifProduct,
                                                                              "mergedClassif", "mergedClassif", true);

        landClassifBand = landClassifProduct.getBand(IdepixConstants.CLASSIF_BAND_NAME);
        waterClassifBand = waterClassifProduct.getBand(IdepixConstants.CLASSIF_BAND_NAME);

        mergedClassifBand = mergedClassifProduct.addBand(IdepixConstants.CLASSIF_BAND_NAME, ProductData.TYPE_INT16);
        FlagCoding flagCoding = OlciUtils.createOlciFlagCoding(IdepixConstants.CLASSIF_BAND_NAME);
        mergedClassifBand.setSampleCoding(flagCoding);
        mergedClassifProduct.getFlagCodingGroup().add(flagCoding);

        hasNNOutput = landClassifProduct.containsBand(IdepixConstants.NN_OUTPUT_BAND_NAME) &&
                waterClassifProduct.containsBand(IdepixConstants.NN_OUTPUT_BAND_NAME);
        computeO2CorrectedTransmissions = landClassifProduct.containsBand("trans13_baseline") &&
                waterClassifProduct.containsBand("trans13_baseline");
        if (hasNNOutput) {
            landNNBand = landClassifProduct.getBand(IdepixConstants.NN_OUTPUT_BAND_NAME);
            waterNNBand = waterClassifProduct.getBand(IdepixConstants.NN_OUTPUT_BAND_NAME);
            mergedNNBand = mergedClassifProduct.addBand(IdepixConstants.NN_OUTPUT_BAND_NAME, ProductData.TYPE_FLOAT32);
        }
        if (computeO2CorrectedTransmissions) {
            landTrans13BaselineBand = landClassifProduct.getBand("trans13_baseline");
            waterTrans13BaselineBand = waterClassifProduct.getBand("trans13_baseline");
            mergedTrans13BaselineBand = mergedClassifProduct.addBand("trans13_baseline", ProductData.TYPE_FLOAT32);
            landTrans13BaselineAmfcorrBand = landClassifProduct.getBand("trans13_baseline_AMFcorr");
            waterTrans13BaselineAmfcorrBand = waterClassifProduct.getBand("trans13_baseline_AMFcorr");
            mergedTrans13BaselineAmfcorrBand = mergedClassifProduct.addBand("trans13_baseline_AMFcorr", ProductData.TYPE_FLOAT32);
            landTrans13ExcessBand = landClassifProduct.getBand("trans13_excess");
            waterTrans13ExcessBand = waterClassifProduct.getBand("trans13_excess");
            mergedTrans13ExcessBand = mergedClassifProduct.addBand("trans13_excess", ProductData.TYPE_FLOAT32);
        }

        setTargetProduct(mergedClassifProduct);

        if (computeCloudBuffer) {
            rectCalculator = new RectangleExtender(new Rectangle(mergedClassifProduct.getSceneRasterWidth(),
                                                                 mergedClassifProduct.getSceneRasterHeight()),
                                                   cloudBufferWidth, cloudBufferWidth);
        }
    }

    @Override
    public void computeTile(Band targetBand, final Tile targetTile, ProgressMonitor pm) throws OperatorException {
        final Rectangle rectangle = targetTile.getRectangle();

        final Tile waterClassifTile = getSourceTile(waterClassifBand, rectangle);
        final Tile landClassifTile = getSourceTile(landClassifBand, rectangle);

        Tile waterNNTile = null;
        Tile landNNTile = null;
        if (hasNNOutput) {
            waterNNTile = getSourceTile(waterNNBand, rectangle);
            landNNTile = getSourceTile(landNNBand, rectangle);
        }

        Tile waterTrans13BaselineTile = null;
        Tile landTrans13BaselineTile = null;
        Tile waterTrans13BaselineAmfcorrTile = null;
        Tile landTrans13BaselineAmfcorrTile = null;
        Tile waterTrans13ExcessTile = null;
        Tile landTrans13ExcessTile = null;
        if (computeO2CorrectedTransmissions) {
            waterTrans13BaselineTile = getSourceTile(waterTrans13BaselineBand, rectangle);
            landTrans13BaselineTile = getSourceTile(landTrans13BaselineBand, rectangle);
            waterTrans13BaselineAmfcorrTile = getSourceTile(waterTrans13BaselineAmfcorrBand, rectangle);
            landTrans13BaselineAmfcorrTile = getSourceTile(landTrans13BaselineAmfcorrBand, rectangle);
            waterTrans13ExcessTile = getSourceTile(waterTrans13ExcessBand, rectangle);
            landTrans13ExcessTile = getSourceTile(landTrans13ExcessBand, rectangle);
        }

        Rectangle extendedRectangle = null;
        if (computeCloudBuffer) {
            extendedRectangle = rectCalculator.extend(rectangle);
        }

        if (targetBand == mergedClassifBand) {
            for (int y = rectangle.y; y < rectangle.y + rectangle.height; y++) {
                checkForCancellation();
                for (int x = rectangle.x; x < rectangle.x + rectangle.width; x++) {
                    final boolean isLand = landClassifTile.getSampleBit(x, y, IdepixConstants.IDEPIX_LAND);
                    final Tile classifTile = isLand ? landClassifTile : waterClassifTile;
                    final int sample = classifTile.getSampleInt(x, y);
                    targetTile.setSample(x, y, sample);
                }
            }

            // potential post processing after merge, e.g. cloud buffer:
            if (computeCloudBuffer) {
                for (int y = rectangle.y; y < rectangle.y + rectangle.height; y++) {
                    checkForCancellation();
                    for (int x = rectangle.x; x < rectangle.x + rectangle.width; x++) {

                        final boolean isCloud = targetTile.getSampleBit(x, y, IdepixConstants.IDEPIX_CLOUD);
                        if (isCloud) {
                            CloudBuffer.computeSimpleCloudBuffer(x, y,
                                                                 targetTile,
                                                                 extendedRectangle,
                                                                 cloudBufferWidth,
                                                                 IdepixConstants.IDEPIX_CLOUD_BUFFER);
                        }
                    }
                }

                for (int y = rectangle.y; y < rectangle.y + rectangle.height; y++) {
                    checkForCancellation();
                    for (int x = rectangle.x; x < rectangle.x + rectangle.width; x++) {
                        IdepixUtils.consolidateCloudAndBuffer(targetTile, x, y);
                    }
                }
            }
        } else if (hasNNOutput && targetBand == mergedNNBand) {
            for (int y = rectangle.y; y < rectangle.y + rectangle.height; y++) {
                checkForCancellation();
                for (int x = rectangle.x; x < rectangle.x + rectangle.width; x++) {
                    boolean isLand = landClassifTile.getSampleBit(x, y, IdepixConstants.IDEPIX_LAND);
                    final float sample = isLand ? landNNTile.getSampleFloat(x, y) : waterNNTile.getSampleFloat(x, y);
                    targetTile.setSample(x, y, sample);
                }
            }
        } else if (computeO2CorrectedTransmissions &&
                (targetBand == mergedTrans13BaselineBand || targetBand == mergedTrans13BaselineAmfcorrBand ||
                        targetBand == mergedTrans13ExcessBand)) {
            for (int y = rectangle.y; y < rectangle.y + rectangle.height; y++) {
                checkForCancellation();
                for (int x = rectangle.x; x < rectangle.x + rectangle.width; x++) {
                    boolean isLand = landClassifTile.getSampleBit(x, y, IdepixConstants.IDEPIX_LAND);
                    float sample;
                    if (targetBand == mergedTrans13BaselineBand) {
                        sample = isLand ? landTrans13BaselineTile.getSampleFloat(x, y) :
                                waterTrans13BaselineTile.getSampleFloat(x, y);
                    } else if (targetBand == mergedTrans13BaselineAmfcorrBand) {
                        sample = isLand ? landTrans13BaselineAmfcorrTile.getSampleFloat(x, y) :
                                waterTrans13BaselineAmfcorrTile.getSampleFloat(x, y);
                    } else {
                        sample = isLand ? landTrans13ExcessTile.getSampleFloat(x, y) :
                                waterTrans13ExcessTile.getSampleFloat(x, y);
                    }
                    targetTile.setSample(x, y, sample);
                }
            }
        }
    }

    /**
     * The Service Provider Interface (SPI) for the operator.
     * It provides operator meta-data and is a factory for new operator instances.
     */
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(OlciMergeLandWaterOp.class);
        }
    }

}
