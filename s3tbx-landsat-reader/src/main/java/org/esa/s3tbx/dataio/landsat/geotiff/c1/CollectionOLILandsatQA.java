package org.esa.s3tbx.dataio.landsat.geotiff.c1;

import org.esa.s3tbx.dataio.landsat.geotiff.AbstractLandsatQA;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.Mask;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by obarrile on 02/01/2019.
 */
public class CollectionOLILandsatQA extends AbstractLandsatQA {

    @Override
    public FlagCoding createFlagCoding(String bandName) {
        FlagCoding flagCoding = new FlagCoding(bandName);
        flagCoding.addFlag("designated_fill", 1, "Designated Fill");
        flagCoding.addFlag("terrain_occlusion", 2, "Terrain Occlusion");
        flagCoding.addFlag("radiometric_saturation_one", 4, "Radiometric Saturation bit one");
        flagCoding.addFlag("radiometric_saturation_two", 8, "Radiometric Saturation bit two");
        flagCoding.addFlag("cloud", 16, "Cloud");
        flagCoding.addFlag("cloud_confidence_one", 32, "Cloud confidence bit one");
        flagCoding.addFlag("cloud_confidence_two", 64, "Cloud confidence bit two");
        flagCoding.addFlag("cloud_shadow_confidence_one", 128, "Cloud shadow confidence bit one");
        flagCoding.addFlag("cloud_shadow_confidence_two", 256, "Cloud shadow confidence bit two");
        flagCoding.addFlag("snow_ice_confidence_one", 512, "Snow/ice confidence bit one");
        flagCoding.addFlag("snow_ice_confidence_two", 1024, "Snow/ice confidence bit two");
        flagCoding.addFlag("cirrus_confidence_one", 2048, "Cirrus confidence bit one");
        flagCoding.addFlag("cirrus_confidence_two", 4096, "Cirrus confidence bit two");
        return flagCoding;
    }

    @Override
    public List<Mask> createMasks(Dimension size) {
        ArrayList<Mask> masks = new ArrayList<>();
        final int width = size.width;
        final int height = size.height;

        masks.add(Mask.BandMathsType.create("designated_fill",
                                            "Designated Fill",
                                            width, height,
                                            "flags.designated_fill",
                                            colorIterator.next(),
                                            0.5));
        masks.add(Mask.BandMathsType.create("terrain_occlusion",
                                            "Terrain Occlusion",
                                            width, height,
                                            "flags.terrain_occlusion",
                                            colorIterator.next(),
                                            0.5));
        masks.add(Mask.BandMathsType.create("cloud",
                                            "Cloud",
                                            width, height,
                                            "flags.cloud",
                                            colorIterator.next(),
                                            0.5));
        masks.addAll(createDefaultRadiometricSaturationMasks("radiometric_saturation", "Radiometric saturation", width, height));
        masks.addAll(createDefaultConfidenceMasks("snow_ice_confidence", "Snow/ice confidence", width, height));
        masks.addAll(createDefaultConfidenceMasks("cloud_confidence", "Cloud confidence", width, height));
        masks.addAll(createDefaultConfidenceMasks("cloud_shadow_confidence", "Cloud shadow confidence", width, height));
        masks.addAll(createDefaultConfidenceMasks("cirrus_confidence", "Cirrus confidence", width, height));


        return masks;
    }
}
