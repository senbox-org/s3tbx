package org.esa.s3tbx.dataio.s3.util;

import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.image.ResolutionLevel;
import org.esa.snap.core.image.SingleBandedOpImage;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.Variable;

import javax.media.jai.PlanarImage;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.WritableRaster;
import java.io.IOException;

/**
 * @author Tonio Fincke
 */
class S3ReferencingVariableOpImage extends SingleBandedOpImage {

    private final RasterDataNode referencedIndexRasterDataNode;
    private final Variable variable;
    private final DimensionValuesProvider dimensionValuesProvider;

    //todo use this to display fires in SLSTR L2 LST products when data is available
    S3ReferencingVariableOpImage(Variable variable, int dataBufferType, int sourceWidth, int sourceHeight,
                                        Dimension tileSize, ResolutionLevel level, int[] additionalDimensionIndexes,
                                        RasterDataNode referencedIndexRasterDataNode, String nameOfReferencingDimension,
                                        String nameOfDisplayedDimension) {
        super(dataBufferType, sourceWidth, sourceHeight, tileSize, null, level);
        this.variable = variable;
        dimensionValuesProvider = getDimensionValuesProvider();
        int displayedDimensionIndex = variable.findDimensionIndex(nameOfDisplayedDimension);
        int referencingDimensionIndex = variable.findDimensionIndex(nameOfReferencingDimension);
        final int numDetectors = variable.getDimension(referencingDimensionIndex).getLength();
        if (displayedDimensionIndex >= 0) {
            int[] variableOrigin = new int[2];
            variableOrigin[displayedDimensionIndex] = additionalDimensionIndexes[0];
            variableOrigin[referencingDimensionIndex] = 0;
            int[] variableShape = new int[2];
            variableShape[displayedDimensionIndex] = 1;
            variableShape[referencingDimensionIndex] = numDetectors;
            dimensionValuesProvider.readValues(variableOrigin, variableShape);
        } else {
            int[] variableOrigin = new int[1];
            variableOrigin[referencingDimensionIndex] = 0;
            int[] variableShape = new int[1];
            variableShape[referencingDimensionIndex] = numDetectors;
            dimensionValuesProvider.readValues(variableOrigin, variableShape);
        }
        this.referencedIndexRasterDataNode = referencedIndexRasterDataNode;

    }

    @Override
    protected void computeRect(PlanarImage[] sourceImages, WritableRaster tile, Rectangle rectangle) {
        final int[] shape = new int[2];
        final int indexX = getIndexX(2);
        final int indexY = getIndexY(2);
        shape[indexX] = getSourceWidth(rectangle.width);
        shape[indexY] = getSourceHeight(rectangle.height);
        final Array variableValues = Array.factory(variable.getDataType(), shape);
        int[] detectorIndexes = new int[rectangle.width * rectangle.height];
        synchronized (referencedIndexRasterDataNode.getSourceImage().getData(rectangle).getSamples(
                rectangle.x, rectangle.y, rectangle.width, rectangle.height, 0, detectorIndexes)) {
            dimensionValuesProvider.setVariableValues(detectorIndexes, variableValues);
        }
        tile.setDataElements(rectangle.x, rectangle.y, rectangle.width, rectangle.height, transformStorage(variableValues));
    }

    private int getIndexX(int rank) {
        return rank - 1;
    }

    private int getIndexY(int rank) {
        return rank - 2;
    }

    private DimensionValuesProvider getDimensionValuesProvider() {
        switch (variable.getDataType()) {
            case FLOAT:
                return new FloatDimensionValuesProvider();
            case SHORT:
                return new ShortDimensionValuesProvider();
        }
        return new NullDimensionValuesProvider();
    }

    /**
     * Transforms the primitive storage of the array supplied as argument.
     * <p>
     * The default implementation merely returns the primitive storage of
     * the array supplied as argument, which is fine when the sequence of
     * variable dimensions is (..., y, x).
     * <p>
     * Implementations have to transpose the storage when the sequence of
     * variable dimensions is (..., x, y) instead of (..., y, x).
     * <p>
     *
     * @param array An array.
     * @return the transformed primitive storage of the array supplied as
     * argument.
     */
    private Object transformStorage(Array array) {
        return array.getStorage();
    }

    interface DimensionValuesProvider {

        void readValues(int[] variableOrigin, int[] variableShape);

        void setVariableValues(int[] referencedValues, Array variableValues);
    }

    private class FloatDimensionValuesProvider implements DimensionValuesProvider {

        private float[] dimensionValues;

        @Override
        public void readValues(int[] variableOrigin, int[] variableShape) {
            try {
                final Section detectorSection = new Section(variableOrigin, variableShape);
                synchronized (variable.getParentGroup().getNetcdfFile()) {
                    final Array dimensionValuesArray = variable.read(detectorSection);
                    dimensionValues = (float[]) dimensionValuesArray.copyTo1DJavaArray();
                }
            } catch (InvalidRangeException | IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void setVariableValues(int[] referencedValues, Array variableValues) {
            final float noDataValue = (float) referencedIndexRasterDataNode.getNoDataValue();
            for (int i = 0; i < referencedValues.length; i++) {
                if (referencedValues[i] > -1) {
                    variableValues.setFloat(i, dimensionValues[referencedValues[i]]);
                } else {
                    variableValues.setFloat(i, noDataValue);
                }
            }
        }
    }

    private class ShortDimensionValuesProvider implements DimensionValuesProvider {

        private short[] dimensionValues;

        @Override
        public void readValues(int[] variableOrigin, int[] variableShape) {
            try {
                final Section detectorSection = new Section(variableOrigin, variableShape);
                synchronized (variable.getParentGroup().getNetcdfFile()) {
                    final Array dimensionValuesArray = variable.read(detectorSection);
                    dimensionValues = (short[]) dimensionValuesArray.copyTo1DJavaArray();
                }
            } catch (InvalidRangeException | IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void setVariableValues(int[] referencedValues, Array variableValues) {
            final short noDataValue = (short) referencedIndexRasterDataNode.getNoDataValue();
            for (int i = 0; i < referencedValues.length; i++) {
                if (referencedValues[i] > -1) {
                    variableValues.setShort(i, dimensionValues[referencedValues[i]]);
                } else {
                    variableValues.setShort(i, noDataValue);
                }
            }
        }
    }

    private class NullDimensionValuesProvider implements DimensionValuesProvider {

        @Override
        public void readValues(int[] variableOrigin, int[] variableShape) {
        }

        @Override
        public void setVariableValues(int[] referencedValues, Array variableValues) {
        }
    }

}
