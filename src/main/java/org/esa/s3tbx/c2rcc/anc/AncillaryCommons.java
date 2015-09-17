package org.esa.s3tbx.c2rcc.anc;

import org.esa.snap.framework.gpf.OperatorException;

public class AncillaryCommons {

    public static final String ANC_DATA_URI = "http://oceandata.sci.gsfc.nasa.gov/cgi/getfile/";

    public static AncDataFormat createPressureFormat(final double pressure_default) {
        return new AncDataFormat(
                    new String[]{
                                "_MET_NCEPR2_6h.hdf",
                                "_MET_NCEPR2_6h.hdf.bz2",
                                "_MET_NCEPN_6h.hdf",
                                "_MET_NCEPN_6h.hdf.bz2",
                    },
                    "press", pressure_default, new InterpolationBorderComputer6H());
    }

    public static AncDataFormat createOzoneFormat(final double ozone_default) {
        return new AncDataFormat(
                    new String[]{
                                "_O3_TOMSOMI_24h.hdf",
                                "_O3_TOMSOMI_24h.hdf.bz2",
                                "_O3_N7TOMS_24h.hdf",
                                "_O3_N7TOMS_24h.hdf.bz2",
                                "_O3_EPTOMS_24h.hdf",
                                "_O3_EPTOMS_24h.hdf.bz2",
                                "_O3_AURAOMI_24h.hdf",
                                "_O3_AURAOMI_24h.hdf.bz2",
                    },
                    "ozone", ozone_default, new InterpolationBorderComputer24H());
    }

    public static double fetchSurfacePressure(AtmosphericAuxdata atmosphericAuxdata, double timeMJD, double lat, double lon) {
        try {
            return atmosphericAuxdata.getSurfacePressure(timeMJD, lat, lon);
        } catch (Exception e) {
            throw new OperatorException("Unable to fetch surface pressure value from auxdata.", e);
        }
    }

    public static double fetchOzone(final AtmosphericAuxdata atmosphericAuxdata, double timeMJD, double lat, double lon) {
        try {
            return atmosphericAuxdata.getOzone(timeMJD, lat, lon);
        } catch (Exception e) {
            throw new OperatorException("Unable to fetch ozone value from auxdata.", e);
        }
    }
}
