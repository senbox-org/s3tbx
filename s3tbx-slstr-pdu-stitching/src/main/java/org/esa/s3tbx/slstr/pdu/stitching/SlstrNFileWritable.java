package org.esa.s3tbx.slstr.pdu.stitching;

import org.esa.snap.core.util.jai.JAIUtils;
import org.esa.snap.dataio.netcdf.NetCDF4Chunking;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SlstrNFileWritable {

    private String dimensions = "";
    protected Map<String, Dimension> dimensionsMap = new HashMap<>();

    protected NetcdfFileWriter netcdfFileWriter;
    protected Map<String, SlstrN4Variable> variables = new HashMap<>();

    SlstrNFileWritable(String filename) throws IOException {
        netcdfFileWriter = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf4, filename, new NetCDF4Chunking());
    }

    void addDimension(String name, int length) throws IOException {
        try {
            dimensionsMap.put(name, netcdfFileWriter.addDimension(null, name, length));
        } catch (Exception e) {
            throw new IOException(e);
        }
        boolean firstDimension = dimensions.length() == 0;
        if (firstDimension) {
            dimensions = name;
        } else {
            dimensions = dimensions + " " + name;
        }
    }

    SlstrN4Variable addVariable(String name, DataType dataType, boolean unsigned, String dimensions) {
        String[] dims = dimensions.split(" ");
        int numDims = dims.length;
        ucar.nc2.Dimension[] nhDims = new ucar.nc2.Dimension[dims.length];
        for (int i = 0; i < dims.length; i++) {
            nhDims[i] = dimensionsMap.get(dims[i]);
        }
        int[] chunkLens = new int[numDims];
        if (!dims[0].equals("")) {
            if (numDims == 1) {
                chunkLens[0] = nhDims[0].getLength();
            } else {
                for (int i = 0; i < numDims - 1; i++) {
                    java.awt.Dimension tileSize = JAIUtils.computePreferredTileSize(nhDims[i].getLength(),
                            nhDims[i + 1].getLength(), 1);
                    chunkLens[i] = (int) tileSize.getWidth();
                    chunkLens[i + 1] = (int) tileSize.getHeight();
                }
            }
        } else {
            chunkLens[0] = 1;
        }
        Variable variable = netcdfFileWriter.addVariable(null, name,
                dataType.withSignedness((unsigned ? DataType.Signedness.UNSIGNED : DataType.Signedness.SIGNED)), dimensions);
        SlstrN4Variable nVariable = new SlstrN4Variable(variable, chunkLens, netcdfFileWriter);
        variables.put(name, nVariable);
        return nVariable;
    }

    void addGlobalAttribute(String name, String value) throws IOException {
        try {
            Attribute attribute = new Attribute(name, value);
            if (value!=null) {
                netcdfFileWriter.addGroupAttribute(null, attribute);
            }
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    void addGlobalAttribute(String name, Number value) throws IOException {
        try {
            Attribute attribute = new Attribute(name, value);
            netcdfFileWriter.addGroupAttribute(null, attribute);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    SlstrN4Variable findVariable(String variableName) {
        return variables.get(variableName);
    }

    public void create() throws IOException {
        netcdfFileWriter.create();
    }

    void close() throws IOException {
        try {
            netcdfFileWriter.close();
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

}
