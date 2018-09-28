package org.esa.s3tbx.processor.rad2refl;

import org.esa.snap.core.util.math.RsMathUtils;

/**
 * Radiance/reflectance conversion for MERIS
 *
 * @author olafd
 */
public class MerisRadReflConverter implements RadReflConverter {

    private String conversionMode;

    MerisRadReflConverter(String conversionMode) {
        this.conversionMode = conversionMode;
    }

    @Override
    public float convert(float spectralInputValue, float sza, float solarFlux) {
        if (conversionMode.equals("RAD_TO_REFL")) {
            return RsMathUtils.radianceToReflectance(spectralInputValue, sza, solarFlux);
        } else {
            return RsMathUtils.reflectanceToRadiance(spectralInputValue, sza, solarFlux);
        }
    }

}
