package org.esa.s3tbx.slstr.pdu.stitching;

import edu.ucar.ral.nujan.netcdf.NhDimension;
import edu.ucar.ral.nujan.netcdf.NhException;
import edu.ucar.ral.nujan.netcdf.NhFileWriter;
import edu.ucar.ral.nujan.netcdf.NhGroup;
import edu.ucar.ral.nujan.netcdf.NhVariable;
import org.esa.snap.core.util.jai.JAIUtils;
import org.esa.snap.dataio.netcdf.nc.N4Variable;
import ucar.ma2.DataType;

import java.awt.Dimension;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SlstrNFileWritable {

    private static final int DEFAULT_COMPRESSION = 6;
    private final NhFileWriter nhFileWriter;
    private Map<String, SlstrN4Variable> variables;

    public static SlstrNFileWritable create(String filename) throws IOException {
        try {
            return new SlstrNFileWritable(new NhFileWriter(filename, NhFileWriter.OPT_OVERWRITE));
        } catch (NhException e) {
            throw new IOException(e);
        }
    }

    private SlstrNFileWritable(NhFileWriter nhFileWriter) {
        this.nhFileWriter = nhFileWriter;
        this.variables = new HashMap<>();
    }

    void addDimension(String name, int length) throws IOException {
        try {
            nhFileWriter.getRootGroup().addDimension(name, length);
        } catch (NhException e) {
            throw new IOException(e);
        }
    }

    void addGlobalAttribute(String name, String value) throws IOException {
        try {
            nhFileWriter.getRootGroup().addAttribute(name, NhVariable.TP_STRING_VAR,
                    cropStringToMaxAttributeLength(name, value));
        } catch (NhException e) {
            throw new IOException(e);
        }
    }

    private static String cropStringToMaxAttributeLength(String name,  String value) {
        if(value.length() > N4Variable.MAX_ATTRIBUTE_LENGTH) {
            value = value.substring(0, N4Variable.MAX_ATTRIBUTE_LENGTH);
            String msg = String.format("Metadata attribute '%s' has been cropped. Exceeded maximum length of %d", name, N4Variable.MAX_ATTRIBUTE_LENGTH);
            Logger.getLogger(N4Variable.class.getSimpleName()).log(Level.WARNING, msg);
        }
        return value;
    }

    SlstrN4Variable addVariable(String name, DataType dataType, boolean unsigned, int[] chunkLens, String dims) throws IOException {
        return addVariable(name, dataType, unsigned, chunkLens, dims, DEFAULT_COMPRESSION);
    }

    SlstrN4Variable addVariable(String name, DataType dataType, boolean unsigned, int[] chunkLens, String dimensions, int compressionLevel) throws
            IOException {
        NhGroup rootGroup = nhFileWriter.getRootGroup();
        int nhType = SlstrNetcdfUtils.convert(dataType, unsigned);
        String[] dims = dimensions.split(" ");
        int numDims = dims.length;
        NhDimension[] nhDims = new NhDimension[numDims];
        for (int i = 0; i < numDims; i++) {
            nhDims[i] = rootGroup.findLocalDimension(dims[i]);
        }
        if (chunkLens != null) {
            if (chunkLens.length != nhDims.length) {
                throw new IllegalArgumentException("Number of chunk sizes must be same as number of dimensions");
            }
            int imageSize = 1;
            int chunkSize = 1;
            for (int i = 0; i < numDims; i++) {
                imageSize *= nhDims[i].getLength();
                chunkSize *= chunkLens[i];
            }
            while (imageSize / chunkSize > Short.MAX_VALUE / 2) {
                chunkSize = 1;
                for (int i = 0; i < numDims; i++) {
                    chunkLens[i] *= 2;
                    chunkLens[i] = Math.min(nhDims[i].getLength(), chunkLens[i]);
                    chunkSize *= chunkLens[i];
                }
            }
        } else {
            chunkLens = new int[numDims];
            if (numDims == 1) {
                chunkLens[0] = nhDims[0].getLength();
            } else {
                for (int i = 0; i < numDims - 1; i++) {
                    Dimension tileSize = JAIUtils.computePreferredTileSize(nhDims[i].getLength(),
                            nhDims[i + 1].getLength(), 1);
                    chunkLens[i] = (int) tileSize.getWidth();
                    chunkLens[i + 1] = (int) tileSize.getHeight();
                }
            }
        }
        Object fillValue = null; // TODO
        try {
            NhVariable variable = rootGroup.addVariable(name, nhType, nhDims, chunkLens, fillValue,
                    compressionLevel);
            SlstrN4Variable nVariable = new SlstrN4Variable(variable, chunkLens);
            variables.put(name, nVariable);
            return nVariable;
        } catch (NhException e) {
            throw new IOException(e);
        }
    }

    SlstrN4Variable findVariable(String variableName) {
        return variables.get(variableName);
    }

    public void create() throws IOException {
        try {
            nhFileWriter.endDefine();
        } catch (NhException e) {
            throw new IOException(e);
        }
    }

    void close() throws IOException {
        try {
            nhFileWriter.close();
        } catch (NhException e) {
            throw new IOException(e);
        }
    }

}
