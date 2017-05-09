package org.esa.s3tbx.mph_chl;

import static org.esa.s3tbx.mph_chl.MphChlConstants.*;

/**
 * todo: add comment
 * To change this template use File | Settings | File Templates.
 * Date: 09.05.2017
 * Time: 11:00
 *
 * @author olafd
 */
public enum Sensor {
    MERIS("MERIS", MERIS_NUM_SPECTRAL_BANDS, MERIS_RAD_BAND_NAMES, MERIS_REFL_BAND_NAMES,
          null, MERIS_SZA_BAND_NAMES, MERIS_AUTOGROUPING_RAD_STRING, MERIS_AUTOGROUPING_REFL_STRING,
          MERIS_SOLAR_FLUXES_DEFAULT, MERIS_INVALID_PIXEL_EXPR),
    OLCI("OLCI", OLCI_NUM_SPECTRAL_BANDS, OLCI_RAD_BAND_NAMES, OLCI_REFL_BAND_NAMES,
         OLCI_SOLAR_FLUX_BAND_NAMES, OLCI_SZA_BAND_NAMES, OLCI_AUTOGROUPING_RAD_STRING, OLCI_AUTOGROUPING_REFL_STRING,
         OLCI_SOLAR_FLUXES_DEFAULT, OLCI_INVALID_PIXEL_EXPR);

    private String name;
    private int numSpectralBands;
    private String[] radBandNames;
    private String[] reflBandNames;
    private String[] solarFluxBandNames;
    private String[] szaBandNames;
    private String radAutogroupingString;
    private String reflAutogroupingString;
    private float[] solarFluxesDefault;
    private String invalidPixelExpression;

    Sensor(String name, int numSpectralBands, String[] radBandNames, String[] reflBandNames,
           String[] solarFluxBandNames, String[] szaBandNames, String radAutogroupingString,
           String reflAutogroupingString, float[] solarFluxesDefault, String invalidPixelExpression) {
        this.name = name;
        this.numSpectralBands = numSpectralBands;
        this.radBandNames = radBandNames;
        this.reflBandNames = reflBandNames;
        this.solarFluxBandNames = solarFluxBandNames;
        this.szaBandNames = szaBandNames;
        this.radAutogroupingString = radAutogroupingString;
        this.reflAutogroupingString = reflAutogroupingString;
        this.solarFluxesDefault = solarFluxesDefault;
        this.invalidPixelExpression = invalidPixelExpression;
    }

    public String getName() {
        return name;
    }

    public int getNumSpectralBands() {
        return numSpectralBands;
    }

    public String[] getRadBandNames() {
        return radBandNames;
    }

    public String[] getReflBandNames() {
        return reflBandNames;
    }

    public String[] getSolarFluxBandNames() {
        return solarFluxBandNames;
    }

    public String[] getSzaBandNames() {
        return szaBandNames;
    }

    public String getRadAutogroupingString() {
        return radAutogroupingString;
    }

    public String getReflAutogroupingString() {
        return reflAutogroupingString;
    }

    public float[] getSolarFluxesDefault() {
        return solarFluxesDefault;
    }

    public String getInvalidPixelExpression() {
        return invalidPixelExpression;
    }
}
