package org.esa.s3tbx.slstr.pdu.stitching;

import edu.ucar.ral.nujan.netcdf.NhException;
import edu.ucar.ral.nujan.netcdf.NhVariable;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.dataio.netcdf.nc.N4Variable;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SlstrN4Variable {

    // MAX_ATTRIBUTE_LENGTH taken from
    // https://github.com/bcdev/nujan/blob/master/src/main/java/edu/ucar/ral/nujan/hdf/MsgAttribute.java#L185
    private static final int MAX_ATTRIBUTE_LENGTH = 65535 - 1000;

    private final NhVariable variable;
    private final int[] chunkLengths;

    SlstrN4Variable(NhVariable variable, int[] chunkLengths) {
        this.variable = variable;
        this.chunkLengths = chunkLengths;
    }

    public String getName() {
        return variable.getName();
    }

    void addAttribute(String name, String value) throws IOException {
        addAttributeImpl(name, cropStringToMaxAttributeLength(name, value), NhVariable.TP_STRING_VAR);
    }

    void addAttribute(String name, Number value, boolean isUnsigned) throws IOException {
        DataType dataType = DataType.getType(value.getClass());
        int nhType = SlstrNetcdfUtils.convert(dataType, isUnsigned);
        addAttributeImpl(name, value, nhType);
    }

    void addAttribute(String name, Array value) throws IOException {
        DataType dataType = DataType.getType(value.getElementType());
        int nhType = SlstrNetcdfUtils.convert(dataType, value.isUnsigned());

        addAttributeImpl(name, value.getStorage(), nhType);
    }

    private void addAttributeImpl(String name, Object value, int type) throws IOException {
        name = name.replace('.', '_');
        try {
            if (!variable.attributeExists(name)) {
                //attributes can only bet set once
                variable.addAttribute(name, type, value);
            }
        } catch (NhException e) {
            throw new IOException(e);
        }
    }

    public void writeFullyInSections(Array values) throws IOException {
        try {
            int[] indexes = new int[values.getShape().length];
            while (indexes != null) {
                variable.writeData(indexes, values.sectionNoReduce(indexes, chunkLengths, null));
                indexes = getNextIndexes(indexes, values.getShape());
            }
        } catch (InvalidRangeException | NhException e) {
            throw new IOException(e.getMessage());
        }
    }

    private int[] getNextIndexes(int[] indexes, int[] totalSizes) {
        for (int i = indexes.length - 1; i >= 0; i--) {
            indexes[i] += chunkLengths[i];
            indexes[i] %= totalSizes[i];
            if (indexes[i] > 0) {
                return indexes;
            }
        }
        return null;
    }

    int[] getChunkLengths() {
        return chunkLengths;
    }

    private static String cropStringToMaxAttributeLength(String name,  String value) {
        if(value.length() > MAX_ATTRIBUTE_LENGTH) {
            value = value.substring(0, MAX_ATTRIBUTE_LENGTH);
            String msg = String.format("Metadata attribute '%s' has been cropped. Exceeded maximum length of %d", name, MAX_ATTRIBUTE_LENGTH);
            Logger.getLogger(N4Variable.class.getSimpleName()).log(Level.WARNING, msg);
        }
        return value;
    }

}
