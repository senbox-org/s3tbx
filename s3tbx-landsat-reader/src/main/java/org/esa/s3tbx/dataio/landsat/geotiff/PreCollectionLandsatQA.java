package org.esa.s3tbx.dataio.landsat.geotiff;

import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.Mask;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by obarrile on 02/01/2019.
 */
public class PreCollectionLandsatQA extends AbstractLandsatQA {
    @Override
    public FlagCoding createFlagCoding(String bandName) {
        FlagCoding flagCoding = new FlagCoding(bandName);
        flagCoding.addFlag("designated_fill", 1, "Designated Fill");
        flagCoding.addFlag("dropped_frame", 2, "Dropped Frame");
        flagCoding.addFlag("terrain_occlusion", 4, "Terrain Occlusion");
        flagCoding.addFlag("reserved_1", 8, "Reserved for a future 1-bit class artifact designation");
        flagCoding.addFlag("water_confidence_one", 16, "Water confidence bit one");
        flagCoding.addFlag("water_confidence_two", 32, "Water confidence bit two");
        flagCoding.addFlag("reserved_2_one", 64, "Reserved for a future 2-bit class artifact designation");
        flagCoding.addFlag("reserved_2_two", 128, "Reserved for a future 2-bit class artifact designation");
        flagCoding.addFlag("reserved_3_one", 256, "Reserved for a future 2-bit class artifact designation");
        flagCoding.addFlag("reserved_3_two", 512, "Reserved for a future 2-bit class artifact designation");
        flagCoding.addFlag("snow_ice_confidence_one", 1024, "Snow/ice confidence bit one");
        flagCoding.addFlag("snow_ice_confidence_two", 2048, "Snow/ice confidence bit two");
        flagCoding.addFlag("cirrus_confidence_one", 4096, "Cirrus confidence bit one");
        flagCoding.addFlag("cirrus_confidence_two", 8192, "Cirrus confidence bit two");
        flagCoding.addFlag("cloud_confidence_one", 16384, "Cloud confidence bit one");
        flagCoding.addFlag("cloud_confidence_two", 32768, "Cloud confidence bit two");
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
        masks.add(Mask.BandMathsType.create("dropped_frame",
                                            "Dropped Frame",
                                            width, height,
                                            "flags.dropped_frame",
                                            colorIterator.next(),
                                            0.5));
        masks.add(Mask.BandMathsType.create("terrain_occlusion",
                                            "Terrain Occlusion",
                                            width, height,
                                            "flags.terrain_occlusion",
                                            colorIterator.next(),
                                            0.5));
        masks.addAll(createDefaultConfidenceMasks("water_confidence", "Water confidence", width, height));
        masks.addAll(createDefaultConfidenceMasks("snow_ice_confidence", "Snow/ice confidence", width, height));
        masks.addAll(createDefaultConfidenceMasks("cirrus_confidence", "Cirrus confidence", width, height));
        masks.addAll(createDefaultConfidenceMasks("cloud_confidence", "Cloud confidence", width, height));

        return masks;
    }
}
