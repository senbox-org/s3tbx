package org.esa.s3tbx.slstr.pdu.stitching;

import com.google.common.primitives.Booleans;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.Chars;
import com.google.common.primitives.Doubles;
import com.google.common.primitives.Floats;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.google.common.primitives.Shorts;
import org.esa.snap.dataio.netcdf.nc.N4Variable;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SlstrN4Variable {

    // MAX_ATTRIBUTE_LENGTH taken from
    // https://github.com/bcdev/nujan/blob/master/src/main/java/edu/ucar/ral/nujan/hdf/MsgAttribute.java#L185
    public static final int MAX_ATTRIBUTE_LENGTH = 65535 - 1000;

    private final Variable variable;
    private final int[] chunkLengths;
    private final NetcdfFileWriter netcdfFileWriter;

    public SlstrN4Variable(Variable variable, int[] chunkLengths, NetcdfFileWriter netcdfFileWriter) {
        this.variable = variable;
        this.chunkLengths = chunkLengths;
        this.netcdfFileWriter = netcdfFileWriter;
    }

    public String getName() {
        return variable.getFullName();
    }

    public Attribute addAttribute(String name, String value) {
        if (value != null) {
            return addAttributeImpl(name, cropStringToMaxAttributeLength(name, value), false);
        } else {
            return addAttributeImpl(name, null, false);
        }
    }

    public Attribute addAttribute(String name, Number value, boolean isUnsigned) {
        return addAttributeImpl(name, value, isUnsigned);
    }

    public Attribute addAttribute(String name, Array value, boolean isUnsigned) {
        return addAttributeImpl(name, value.getStorage(), isUnsigned);
    }


    private Attribute addAttributeImpl(String name, Object value, boolean isUnsigned) {
        name = name.replace('.', '_');
        Attribute existingAttribute = variable.findAttribute(name);
        if (existingAttribute != null) {
            return existingAttribute;
        } else if (value == null) {
            Attribute attribute = new Attribute(name, "");
            return variable.addAttribute(attribute);
        } else if (value instanceof Integer) {
            Attribute attribute = new Attribute(name, (Integer) value, isUnsigned);
            return variable.addAttribute(attribute);
        } else if (value instanceof String) {
            Attribute attribute = new Attribute(name, (String) value);
            return variable.addAttribute(attribute);
        } else if (value instanceof Array) {
            Attribute attribute = new Attribute(name, (Array) value);
            return variable.addAttribute(attribute);
        } else if (value instanceof Float) {
            Attribute attribute = new Attribute(name, (Float) value);
            return variable.addAttribute(attribute);
        } else if (value instanceof List) {
            Attribute attribute = new Attribute(name, (List) value, isUnsigned);
            return variable.addAttribute(attribute);
        } else if (value instanceof Double) {
            Attribute attribute = new Attribute(name, (Double) value);
            return variable.addAttribute(attribute);
        } else if (value instanceof Byte) {
            Attribute attribute = new Attribute(name, (Byte) value, isUnsigned);
            return variable.addAttribute(attribute);
        } else if (value instanceof Short) {
            Attribute attribute = new Attribute(name, (Short) value, isUnsigned);
            return variable.addAttribute(attribute);
        } else if (value instanceof int[]) {
            List<Integer> temp = Ints.asList((int[]) value);
            Attribute attribute = new Attribute(name, temp, isUnsigned);
            return variable.addAttribute(attribute);
        } else if (value instanceof byte[]) {
            List<Byte> temp = Bytes.asList((byte[]) value);
            Attribute attribute = new Attribute(name, temp, isUnsigned);
            return variable.addAttribute(attribute);
        } else if (value instanceof short[]) {
            List<Short> temp = Shorts.asList((short[]) value);
            Attribute attribute = new Attribute(name, temp, isUnsigned);
            return variable.addAttribute(attribute);
        } else if (value instanceof float[]) {
            List<Float> temp = Floats.asList((float[]) value);
            Attribute attribute = new Attribute(name, temp, isUnsigned);
            return variable.addAttribute(attribute);
        } else if (value instanceof double[]) {
            List<Double> temp = Doubles.asList((double[]) value);
            Attribute attribute = new Attribute(name, temp, isUnsigned);
            return variable.addAttribute(attribute);
        } else if (value instanceof long[]) {
            List<Long> temp = Longs.asList((long[]) value);
            Attribute attribute = new Attribute(name, temp, isUnsigned);
            return variable.addAttribute(attribute);
        } else if (value instanceof boolean[]) {
            List<Boolean> temp = Booleans.asList((boolean[]) value);
            Attribute attribute = new Attribute(name, temp, isUnsigned);
            return variable.addAttribute(attribute);
        } else if (value instanceof char[]) {
            List<Character> temp = Chars.asList((char[]) value);
            Attribute attribute = new Attribute(name, temp, isUnsigned);
            return variable.addAttribute(attribute);
        } else if (value instanceof Number) {
            Attribute attribute = new Attribute(name, (Number) value, isUnsigned);
            return variable.addAttribute(attribute);
        } else {
            throw new IllegalArgumentException("wrong type " + value.getClass().toString() + " of the attribute " + name);
        }
    }

    public void writeFullyInSections(Array values) throws IOException {
        try {
            int[] indexes = new int[values.getShape().length];
            while (indexes != null) {
                netcdfFileWriter.setFill(true);
                netcdfFileWriter.write(variable, indexes, values.sectionNoReduce(indexes, chunkLengths, null));
                indexes = getNextIndexes(indexes, values.getShape());
            }
        } catch (IOException | InvalidRangeException e) {
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

    private static String cropStringToMaxAttributeLength(String name, String value) {
        if (value != null && value.length() > MAX_ATTRIBUTE_LENGTH) {
            value = value.substring(0, MAX_ATTRIBUTE_LENGTH);
            String msg = String.format("Metadata attribute '%s' has been cropped. Exceeded maximum length of %d",
                    name, MAX_ATTRIBUTE_LENGTH);
            Logger.getLogger(N4Variable.class.getSimpleName()).log(Level.WARNING, msg);
        }
        return value;
    }

}
