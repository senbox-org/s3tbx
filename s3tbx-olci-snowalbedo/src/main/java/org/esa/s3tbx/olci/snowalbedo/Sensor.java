package org.esa.s3tbx.olci.snowalbedo;

import static org.esa.s3tbx.olci.snowalbedo.SensorConstants.*;

/**
 * Enumeration for supported sensors for snow albedo retrieval (just OLCI so far).
 *
 * @author olafd
 */
public enum Sensor {

    OLCI("OLCI", OLCI_SZA_NAME, OLCI_VZA_NAME, OLCI_SAA_NAME, OLCI_VAA_NAME,
         OLCI_L1B_FLAGS_NAME, OLCI_INVALID_BIT, OLCI_REQUIRED_RADIANCE_BAND_NAMES, OLCI_REQUIRED_BRR_BAND_NAMES);

    private String name;
    private String szaName;
    private String vzaName;
    private String saaName;
    private String vaaName;
    private String l1bFlagsName;
    private int invalidBit;
    private String[] requiredRadianceBandNames;
    private String[] requiredBrrBandNames;

    Sensor(String name, String szaName, String vzaName, String saaName, String vaaName,
           String l1bFlagsName, int invalidBit, String requiredRadianceBandNames[], String[] requiredBrrBandNames) {
        this.name = name;
        this.szaName = szaName;
        this.vzaName = vzaName;
        this.saaName = saaName;
        this.vaaName = vaaName;
        this.l1bFlagsName = l1bFlagsName;
        this.invalidBit = invalidBit;
        this.requiredRadianceBandNames = requiredRadianceBandNames;
        this.requiredBrrBandNames = requiredBrrBandNames;
    }

    public String getName() {
        return name;
    }

    public String getSzaName() {
        return szaName;
    }

    public String getVzaName() {
        return vzaName;
    }

    public String getSaaName() {
        return saaName;
    }

    public String getVaaName() {
        return vaaName;
    }

    public String getL1bFlagsName() {
        return l1bFlagsName;
    }

    public int getInvalidBit() {
        return invalidBit;
    }

    public String[] getRequiredRadianceBandNames() {
        return requiredRadianceBandNames;
    }

    public String[] getRequiredBrrBandNames() {
        return requiredBrrBandNames;
    }

}
