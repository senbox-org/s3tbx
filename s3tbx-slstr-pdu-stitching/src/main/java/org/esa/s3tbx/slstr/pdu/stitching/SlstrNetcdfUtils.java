package org.esa.s3tbx.slstr.pdu.stitching;

import edu.ucar.ral.nujan.netcdf.NhVariable;
import ucar.ma2.DataType;

class SlstrNetcdfUtils {

    static int convert(DataType tp, boolean isUnsigned) {
        int nhType = 0;
        if (tp == DataType.BYTE) {
            if (isUnsigned) {
                nhType = NhVariable.TP_UBYTE;
            } else {
                nhType = NhVariable.TP_SBYTE;
            }
        } else if (tp == DataType.SHORT) {
            nhType = NhVariable.TP_SHORT;
        } else if (tp == DataType.INT) {
            nhType = NhVariable.TP_INT;
        } else if (tp == DataType.LONG) {
            nhType = NhVariable.TP_LONG;
        } else if (tp == DataType.FLOAT) {
            nhType = NhVariable.TP_FLOAT;
        } else if (tp == DataType.DOUBLE) {
            nhType = NhVariable.TP_DOUBLE;
        } else if (tp == DataType.CHAR) {
            nhType = NhVariable.TP_CHAR;
        } else if (tp == DataType.STRING) {
            nhType = NhVariable.TP_STRING_VAR;
        }
        return nhType;
    }

}
