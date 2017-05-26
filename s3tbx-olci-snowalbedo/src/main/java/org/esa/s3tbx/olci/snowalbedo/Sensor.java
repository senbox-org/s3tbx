/*
 *
 *  * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
 *  *
 *  * This program is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU General Public License as published by the Free
 *  * Software Foundation; either version 3 of the License, or (at your option)
 *  * any later version.
 *  * This program is distributed in the hope that it will be useful, but WITHOUT
 *  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 *  * more details.
 *  *
 *  * You should have received a copy of the GNU General Public License along
 *  * with this program; if not, see http://www.gnu.org/licenses/
 *
 */

package org.esa.s3tbx.olci.snowalbedo;

import static org.esa.s3tbx.olci.snowalbedo.SensorConstants.*;

/**
 * Enumeration for supported sensors. todo: do we need this?
 *
 * @author olafd
 */
public enum Sensor {

    OLCI("OLCI", OLCI_NUM_BANDS, OLCI_SZA_NAME, OLCI_VZA_NAME, OLCI_SAA_NAME, OLCI_VAA_NAME, OLCI_OZONE_NAME,
         OLCI_LAT_NAME, OLCI_LON_NAME, OLCI_ALT_NAME, OLCI_SLP_NAME,
         OLCI_BOUNDS, OLCI_NAME_FORMAT, OLCI_BAND_INFO_FILE_NAME,
         OLCI_L1B_FLAGS_NAME, OLCI_INVALID_BIT);

    private String name;
    private int numBands;
    private String szaName;
    private String vzaName;
    private String saaName;
    private String vaaName;
    private String ozoneName;
    private String latName;
    private String lonName;
    private String altName;
    private String slpName;
    private int[] bounds;
    private String nameFormat;
    private String bandInfoFileName;
    private String l1bFlagsName;
    private int invalidBit;

    Sensor(String name, int numBands, String szaName, String vzaName, String saaName, String vaaName,
           String ozoneName, String latName, String lonName, String altName, String slpName, int[] bounds,
           String nameFormat, String bandInfoFileName, String l1bFlagsName, int invalidBit) {
        this.name = name;
        this.numBands = numBands;
        this.szaName = szaName;
        this.vzaName = vzaName;
        this.saaName = saaName;
        this.vaaName = vaaName;
        this.ozoneName = ozoneName;
        this.latName = latName;
        this.lonName = lonName;
        this.altName = altName;
        this.slpName = slpName;
        this.bounds = bounds;
        this.nameFormat = nameFormat;
        this.bandInfoFileName = bandInfoFileName;
        this.l1bFlagsName = l1bFlagsName;
        this.invalidBit = invalidBit;
    }

    public String getName() {
        return name;
    }

    public int getNumBands() {
        return numBands;
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

    public String getOzoneName() {
        return ozoneName;
    }

    public String getLatName() {
        return latName;
    }

    public String getLonName() {
        return lonName;
    }

    public String getAltName() {
        return altName;
    }

    public String getSlpName() {
        return slpName;
    }

    public int[] getBounds() {
        return bounds;
    }

    public String getNameFormat() {
        return nameFormat;
    }

    public String getBandInfoFileName() {
        return bandInfoFileName;
    }

    public String getL1bFlagsName() {
        return l1bFlagsName;
    }

    public int getInvalidBit() {
        return invalidBit;
    }
}
