package org.esa.s3tbx.mphchl;

import static org.esa.s3tbx.mphchl.MphChlConstants.*;

/**
 * Enumeration for supported sensors (OLCI, MERIS 3rd and 4th reprocessing)
 *
 * @author olafd
 */
public enum Sensor {
    MERIS_3RD("MERIS_3RD", MERIS_NUM_SPECTRAL_BANDS, MERIS_RAD_BAND_NAMES_3RD,
              MERIS_REQUIRED_RADIANCE_BAND_NAMES, MERIS_REQUIRED_BRR_BAND_NAMES, MERIS_VALID_PIXEL_EXPR_3RD),
    MERIS_4TH("MERIS_4TH", MERIS_NUM_SPECTRAL_BANDS, MERIS_RAD_BAND_NAMES_4TH,
              MERIS_REQUIRED_RADIANCE_BAND_NAMES_4TH, MERIS_REQUIRED_BRR_BAND_NAMES, MERIS_VALID_PIXEL_EXPR_4TH),
    OLCI("OLCI", OLCI_NUM_SPECTRAL_BANDS, OLCI_RAD_BAND_NAMES,
         OLCI_REQUIRED_RADIANCE_BAND_NAMES, OLCI_REQUIRED_BRR_BAND_NAMES, OLCI_VALID_PIXEL_EXPR);

    private String name;
    private int numSpectralBands;
    private String[] radBandNames;
    private String requiredRadianceBandNames;
    private String requiredBrrBandNames;
    private String validPixelExpression;

    Sensor(String name, int numSpectralBands, String[] radBandNames, String requiredRadianceBandNames,
           String requiredBrrBandNames, String validPixelExpression) {
        this.name = name;
        this.numSpectralBands = numSpectralBands;
        this.radBandNames = radBandNames;
        this.requiredRadianceBandNames = requiredRadianceBandNames;
        this.requiredBrrBandNames = requiredBrrBandNames;
        this.validPixelExpression = validPixelExpression;
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

    public String getRequiredRadianceBandNames() {
        return requiredRadianceBandNames;
    }

    public String getRequiredBrrBandNames() {
        return requiredBrrBandNames;
    }

    public String getValidPixelExpression() {
        return validPixelExpression;
    }
}
