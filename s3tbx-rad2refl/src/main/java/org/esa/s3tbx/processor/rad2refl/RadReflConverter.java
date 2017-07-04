package org.esa.s3tbx.processor.rad2refl;

/**
 * Interface for Radiance/reflectance conversion. To be implemented sensor-dependent.
 *
 * @author olafd
 */
public interface RadReflConverter {

    float convert(float spectralInputValue, float sza, float solarFlux);
}
