package org.esa.s3tbx.processor.rad2refl;

import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.image.ResolutionLevel;
import org.esa.snap.core.image.VirtualBandOpImage;
import org.esa.snap.core.util.math.RsMathUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Radiance/reflectance conversion for SLSTR
 *
 * @author olafd
 */
public class SlstrRadReflConverter implements RadReflConverter {

    private String conversionMode;

    SlstrRadReflConverter(String conversionMode) {
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

    static Map<String, Float> getSolarFluxMapFromQualityMetadata(Product sourceProduct,
                                                                 String[] spectralInputBandNames,
                                                                 boolean radToReflMode) {
        Map<String, Float> map = new HashMap<>();

        for (final String bandName : spectralInputBandNames) {
            final int spectralIndex = Integer.parseInt(bandName.substring(1, 2)) - 1;
            final String stringToReplace = radToReflMode ? "radiance" : "reflectance";
            final String qualityElementName = bandName.replace(stringToReplace, "quality");
            final MetadataElement qualityElement = sourceProduct.getMetadataRoot().getElement(qualityElementName);
            if (qualityElement != null) {
                final MetadataElement variableAttributesElement = qualityElement.getElement("Variable_Attributes");
                if (variableAttributesElement != null) {
                    final String solarIrradianceElementName = bandName.replace(stringToReplace, "solar_irradiance");
                    final MetadataElement solarIrradianceElement = variableAttributesElement.getElement(solarIrradianceElementName);
                    if (solarIrradianceElement != null) {
                        final MetadataAttribute solarIrradianceValueAttribute = solarIrradianceElement.getAttribute("value");
                        map.put(bandName, solarIrradianceValueAttribute.getData().getElemFloat());
                    } else {
                        map.put(bandName, Sensor.SLSTR_500m.getSolarFluxesDefault()[spectralIndex]);
                    }
                } else {
                    map.put(bandName, Sensor.SLSTR_500m.getSolarFluxesDefault()[spectralIndex]);
                }
            } else {
                map.put(bandName, Sensor.SLSTR_500m.getSolarFluxesDefault()[spectralIndex]);
            }
        }
        return map;
    }

    static VirtualBandOpImage[] createInvalidImages(Product sourceProduct, String[] inputBandNames,
                                                    boolean isRad2ReflMode) {
        VirtualBandOpImage[] invalidImages = new VirtualBandOpImage[inputBandNames.length];
        String replacer = isRad2ReflMode ? "radiance" : "reflectance";
        for (int i = 0; i < inputBandNames.length; i++) {
            final String bandName = inputBandNames[i];
            String unfilledPixelExpression = bandName.replace(replacer, "exception") + ".unfilled_pixel";
            String saturationExpression = bandName.replace(replacer, "exception") + ".saturation";
            String invalidRadianceExpression = bandName.replace(replacer, "exception") + ".invalid_radiance";
            String invalidExpression = unfilledPixelExpression + " || " +
                    saturationExpression + " || " + invalidRadianceExpression;
            invalidImages[i] = VirtualBandOpImage.builder(invalidExpression, sourceProduct)
                    .dataType(ProductData.TYPE_FLOAT32)
                    .fillValue(0.0f)
                    .tileSize(sourceProduct.getPreferredTileSize())
                    .mask(false)
                    .level(ResolutionLevel.MAXRES)
                    .create();
        }
        return invalidImages;
    }
}
