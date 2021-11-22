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

package org.esa.s3tbx.olci.radiometry;

import static org.esa.s3tbx.olci.radiometry.SensorConstants.MERIS_4TH_ALT_NAME;
import static org.esa.s3tbx.olci.radiometry.SensorConstants.MERIS_4TH_BAND_INFO_FILE_NAME;
import static org.esa.s3tbx.olci.radiometry.SensorConstants.MERIS_4TH_BOUNDS;
import static org.esa.s3tbx.olci.radiometry.SensorConstants.MERIS_4TH_INVALID_BIT;
import static org.esa.s3tbx.olci.radiometry.SensorConstants.MERIS_4TH_L1B_FLAGS_NAME;
import static org.esa.s3tbx.olci.radiometry.SensorConstants.MERIS_4TH_LAT_NAME;
import static org.esa.s3tbx.olci.radiometry.SensorConstants.MERIS_4TH_LON_NAME;
import static org.esa.s3tbx.olci.radiometry.SensorConstants.MERIS_4TH_NAME_FORMAT;
import static org.esa.s3tbx.olci.radiometry.SensorConstants.MERIS_4TH_NUM_BANDS;
import static org.esa.s3tbx.olci.radiometry.SensorConstants.MERIS_4TH_OZONE_NAME;
import static org.esa.s3tbx.olci.radiometry.SensorConstants.MERIS_4TH_SAA_NAME;
import static org.esa.s3tbx.olci.radiometry.SensorConstants.MERIS_4TH_SLP_NAME;
import static org.esa.s3tbx.olci.radiometry.SensorConstants.MERIS_4TH_SPECTRAL_BAND_NAMES;
import static org.esa.s3tbx.olci.radiometry.SensorConstants.MERIS_4TH_SZA_NAME;
import static org.esa.s3tbx.olci.radiometry.SensorConstants.MERIS_4TH_VAA_NAME;
import static org.esa.s3tbx.olci.radiometry.SensorConstants.MERIS_4TH_VZA_NAME;
import static org.esa.s3tbx.olci.radiometry.SensorConstants.MERIS_ALT_NAME;
import static org.esa.s3tbx.olci.radiometry.SensorConstants.MERIS_BAND_INFO_FILE_NAME;
import static org.esa.s3tbx.olci.radiometry.SensorConstants.MERIS_BOUNDS;
import static org.esa.s3tbx.olci.radiometry.SensorConstants.MERIS_INVALID_BIT;
import static org.esa.s3tbx.olci.radiometry.SensorConstants.MERIS_L1B_FLAGS_NAME;
import static org.esa.s3tbx.olci.radiometry.SensorConstants.MERIS_LAT_NAME;
import static org.esa.s3tbx.olci.radiometry.SensorConstants.MERIS_LON_NAME;
import static org.esa.s3tbx.olci.radiometry.SensorConstants.MERIS_NAME_FORMAT;
import static org.esa.s3tbx.olci.radiometry.SensorConstants.MERIS_NUM_BANDS;
import static org.esa.s3tbx.olci.radiometry.SensorConstants.MERIS_OZONE_NAME;
import static org.esa.s3tbx.olci.radiometry.SensorConstants.MERIS_SAA_NAME;
import static org.esa.s3tbx.olci.radiometry.SensorConstants.MERIS_SLP_NAME;
import static org.esa.s3tbx.olci.radiometry.SensorConstants.MERIS_SPECTRAL_BAND_NAMES;
import static org.esa.s3tbx.olci.radiometry.SensorConstants.MERIS_SZA_NAME;
import static org.esa.s3tbx.olci.radiometry.SensorConstants.MERIS_VAA_NAME;
import static org.esa.s3tbx.olci.radiometry.SensorConstants.MERIS_VZA_NAME;
import static org.esa.s3tbx.olci.radiometry.SensorConstants.OLCI_ALT_NAME;
import static org.esa.s3tbx.olci.radiometry.SensorConstants.OLCI_BAND_INFO_FILE_NAME;
import static org.esa.s3tbx.olci.radiometry.SensorConstants.OLCI_BOUNDS;
import static org.esa.s3tbx.olci.radiometry.SensorConstants.OLCI_INVALID_BIT;
import static org.esa.s3tbx.olci.radiometry.SensorConstants.OLCI_L1B_FLAGS_NAME;
import static org.esa.s3tbx.olci.radiometry.SensorConstants.OLCI_LAT_NAME;
import static org.esa.s3tbx.olci.radiometry.SensorConstants.OLCI_LON_NAME;
import static org.esa.s3tbx.olci.radiometry.SensorConstants.OLCI_NAME_FORMAT;
import static org.esa.s3tbx.olci.radiometry.SensorConstants.OLCI_NUM_BANDS;
import static org.esa.s3tbx.olci.radiometry.SensorConstants.OLCI_OZONE_NAME;
import static org.esa.s3tbx.olci.radiometry.SensorConstants.OLCI_SAA_NAME;
import static org.esa.s3tbx.olci.radiometry.SensorConstants.OLCI_SLP_NAME;
import static org.esa.s3tbx.olci.radiometry.SensorConstants.OLCI_SPECTRAL_BAND_NAMES;
import static org.esa.s3tbx.olci.radiometry.SensorConstants.OLCI_SZA_NAME;
import static org.esa.s3tbx.olci.radiometry.SensorConstants.OLCI_VAA_NAME;
import static org.esa.s3tbx.olci.radiometry.SensorConstants.OLCI_VZA_NAME;
import static org.esa.s3tbx.olci.radiometry.SensorConstants.S2_MSI_ALT_NAME;
import static org.esa.s3tbx.olci.radiometry.SensorConstants.S2_MSI_BAND_INFO_FILE_NAME;
import static org.esa.s3tbx.olci.radiometry.SensorConstants.S2_MSI_BOUNDS;
import static org.esa.s3tbx.olci.radiometry.SensorConstants.S2_MSI_INVALID_BIT;
import static org.esa.s3tbx.olci.radiometry.SensorConstants.S2_MSI_L1B_FLAGS_NAME;
import static org.esa.s3tbx.olci.radiometry.SensorConstants.S2_MSI_LAT_NAME;
import static org.esa.s3tbx.olci.radiometry.SensorConstants.S2_MSI_LON_NAME;
import static org.esa.s3tbx.olci.radiometry.SensorConstants.S2_MSI_NAME_FORMAT;
import static org.esa.s3tbx.olci.radiometry.SensorConstants.S2_MSI_NUM_BANDS;
import static org.esa.s3tbx.olci.radiometry.SensorConstants.S2_MSI_OZONE_NAME;
import static org.esa.s3tbx.olci.radiometry.SensorConstants.S2_MSI_SAA_NAME;
import static org.esa.s3tbx.olci.radiometry.SensorConstants.S2_MSI_SLP_NAME;
import static org.esa.s3tbx.olci.radiometry.SensorConstants.S2_MSI_SPECTRAL_BAND_NAMES;
import static org.esa.s3tbx.olci.radiometry.SensorConstants.S2_MSI_SZA_NAME;
import static org.esa.s3tbx.olci.radiometry.SensorConstants.S2_MSI_VAA_NAME;
import static org.esa.s3tbx.olci.radiometry.SensorConstants.S2_MSI_VZA_NAME;

/**
 * Enumeration for supported sensors
 *
 * @author muhammad.bc, olafd
 */
public enum Sensor {

    MERIS("MERIS", MERIS_NUM_BANDS, MERIS_SPECTRAL_BAND_NAMES, MERIS_SZA_NAME, MERIS_VZA_NAME, MERIS_SAA_NAME, MERIS_VAA_NAME, MERIS_OZONE_NAME,
          MERIS_LAT_NAME, MERIS_LON_NAME, MERIS_ALT_NAME, MERIS_SLP_NAME,
          MERIS_BOUNDS, MERIS_NAME_FORMAT, MERIS_BAND_INFO_FILE_NAME,
          MERIS_L1B_FLAGS_NAME, MERIS_INVALID_BIT),

    MERIS_4TH("MERIS", MERIS_4TH_NUM_BANDS, MERIS_4TH_SPECTRAL_BAND_NAMES, MERIS_4TH_SZA_NAME, MERIS_4TH_VZA_NAME, MERIS_4TH_SAA_NAME, MERIS_4TH_VAA_NAME,
              MERIS_4TH_OZONE_NAME, MERIS_4TH_LAT_NAME, MERIS_4TH_LON_NAME, MERIS_4TH_ALT_NAME, MERIS_4TH_SLP_NAME,
              MERIS_4TH_BOUNDS, MERIS_4TH_NAME_FORMAT, MERIS_4TH_BAND_INFO_FILE_NAME,
              MERIS_4TH_L1B_FLAGS_NAME, MERIS_4TH_INVALID_BIT),

    OLCI("OLCI", OLCI_NUM_BANDS, OLCI_SPECTRAL_BAND_NAMES, OLCI_SZA_NAME, OLCI_VZA_NAME, OLCI_SAA_NAME, OLCI_VAA_NAME, OLCI_OZONE_NAME,
         OLCI_LAT_NAME, OLCI_LON_NAME, OLCI_ALT_NAME, OLCI_SLP_NAME,
         OLCI_BOUNDS, OLCI_NAME_FORMAT, OLCI_BAND_INFO_FILE_NAME,
         OLCI_L1B_FLAGS_NAME, OLCI_INVALID_BIT),

    S2_MSI("S2_MSI", S2_MSI_NUM_BANDS, S2_MSI_SPECTRAL_BAND_NAMES, S2_MSI_SZA_NAME, S2_MSI_VZA_NAME, S2_MSI_SAA_NAME, S2_MSI_VAA_NAME, S2_MSI_OZONE_NAME,
           S2_MSI_LAT_NAME, S2_MSI_LON_NAME, S2_MSI_ALT_NAME, S2_MSI_SLP_NAME,
           S2_MSI_BOUNDS, S2_MSI_NAME_FORMAT, S2_MSI_BAND_INFO_FILE_NAME,
           S2_MSI_L1B_FLAGS_NAME, S2_MSI_INVALID_BIT);

    private String name;
    private int numBands;
    private String[] spectralBandNames;
    private String szaName;
    private String vzaName;
    private String saaName;
    private String vaaName;
    private String ozoneName;
    private String latName;
    private String lonName;
    private String altName;
    private String slpName;
    private int[] wvBounds;
    private String nameFormat;
    private String bandInfoFileName;
    private String l1bFlagsName;
    private int invalidBit;

    Sensor(String name, int numBands, String[] spectralBandNames, String szaName, String vzaName, String saaName, String vaaName,
           String ozoneName, String latName, String lonName, String altName, String slpName, int[] wvBoundBandNumbers,
           String nameFormat, String bandInfoFileName, String l1bFlagsName, int invalidBit) {
        this.name = name;
        this.numBands = numBands;
        this.spectralBandNames= spectralBandNames;
        this.szaName = szaName;
        this.vzaName = vzaName;
        this.saaName = saaName;
        this.vaaName = vaaName;
        this.ozoneName = ozoneName;
        this.latName = latName;
        this.lonName = lonName;
        this.altName = altName;
        this.slpName = slpName;
        this.wvBounds = wvBoundBandNumbers;
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

    public String[] getSpectralBandNames() {
        return spectralBandNames;
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

    public String getUpperWvBandName() {
        String bandNameFormat = getNameFormat();
        return String.format(bandNameFormat, wvBounds[1]);
    }

    public String getLowerWvBandName() {
        String bandNameFormat = getNameFormat();
        return String.format(bandNameFormat, wvBounds[0]);
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
