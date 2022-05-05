package org.esa.s3tbx.dataio.landsat.geotiff.c2;

import org.esa.s3tbx.dataio.landsat.geotiff.AbstractLandsatQA;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.Mask;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Definition of Landsat8/9 QA flags (Collection 2)
 * Reference: https://www.usgs.gov/media/files/landsat-8-9-olitirs-collection-2-level-1-data-format-control-book
 *
 * @author Cosmin Cara
 */
public class Collection2OLILandsatQA extends AbstractLandsatQA {

    private final static Map<String, List<FlagCodingArgs>> codings = new HashMap<String, List<FlagCodingArgs>>() {{
        put("FILE_NAME_QUALITY_L1_PIXEL",
            new ArrayList<FlagCodingArgs>() {{
                int position = 1; // 0
                add(new FlagCodingArgs("designated_fill", position, "Designated Fill"));
                position <<= 1; // 1
                add(new FlagCodingArgs("dillated_cloud", position, "Dillated Cloud"));
                position <<= 1; // 2
                add(new FlagCodingArgs("cirrus", position, "Cirrus"));
                position <<= 1; // 3
                add(new FlagCodingArgs("cloud", position, "Cloud"));
                position <<= 1; // 4
                add(new FlagCodingArgs("cloud_shadow", position, "Cloud Shadow"));
                position <<= 1; // 5
                add(new FlagCodingArgs("snow", position, "Snow/Ice Cover"));
                position <<= 1; // 6
                add(new FlagCodingArgs("clear", position, "Clear"));
                position <<= 1; // 7
                add(new FlagCodingArgs("water", position, "Water"));
                position <<= 1; // 8
                add(new FlagCodingArgs("cloud_confidence_one", position, "Cloud confidence bit one"));
                position <<= 1; // 9
                add(new FlagCodingArgs("cloud_confidence_two", position, "Cloud confidence bit two"));
                position <<= 1; // 10
                add(new FlagCodingArgs("cloud_shadow_confidence_one", position, "Cloud shadow confidence bit one"));
                position <<= 1; // 11
                add(new FlagCodingArgs("cloud_shadow_confidence_two", position, "Cloud shadow confidence bit two"));
                position <<= 1; // 12
                add(new FlagCodingArgs("snow_ice_confidence_one", position, "Snow/ice confidence bit one"));
                position <<= 1; // 13
                add(new FlagCodingArgs("snow_ice_confidence_two", position, "Snow/ice confidence bit two"));
                position <<= 1; // 14
                add(new FlagCodingArgs("cirrus_confidence_one", position, "Cirrus confidence bit one"));
                position <<= 1; // 15
                add(new FlagCodingArgs("cirrus_confidence_two", position, "Cirrus confidence bit two"));
            }});
        put("FILE_NAME_QUALITY_L1_RADIOMETRIC_SATURATION",
            new ArrayList<FlagCodingArgs>() {{
                int position = 1;
                add(new FlagCodingArgs("radiometric_saturation_b1", position, "Band 1 data saturation"));
                position <<= 1;
                add(new FlagCodingArgs("radiometric_saturation_b2", position, "Band 2 data saturation"));
                position <<= 1;
                add(new FlagCodingArgs("radiometric_saturation_b3", position, "Band 3 data saturation"));
                position <<= 1;
                add(new FlagCodingArgs("radiometric_saturation_b4", position, "Band 4 data saturation"));
                position <<= 1;
                add(new FlagCodingArgs("radiometric_saturation_b5", position, "Band 5 data saturation"));
                position <<= 1;
                add(new FlagCodingArgs("radiometric_saturation_b6", position, "Band 6 data saturation"));
                position <<= 1;
                add(new FlagCodingArgs("radiometric_saturation_b7", position, "Band 7 data saturation"));
                position <<= 2; // position 7 not used
                add(new FlagCodingArgs("radiometric_saturation_b9", position, "Band 9 data saturation"));
                position <<= 3; // positions 9 and 10 not used
                add(new FlagCodingArgs("terrain_occlusion", position, "Terrain occlusion"));
            }});
    }};

    @Override
    public FlagCoding createFlagCoding(String bandName) {
        final FlagCoding flagCoding = new FlagCoding(bandName);
        final List<FlagCodingArgs> args;
        if (bandName.equals("flags")) {
            args = codings.get("FILE_NAME_QUALITY_L1_PIXEL");
        } else {
            args = codings.get("FILE_NAME_QUALITY_L1_RADIOMETRIC_SATURATION");
        }
        args.forEach(a -> flagCoding.addFlag(a.getName(), a.getFlagMask(), a.getDescription()));
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
                                            "satflags.terrain_occlusion",
                                            colorIterator.next(),
                                            0.5));
        masks.add(Mask.BandMathsType.create("cirrus",
                                            "Cirrus",
                                            width, height,
                                            "flags.cirrus",
                                            colorIterator.next(),
                                            0.5));
        masks.add(Mask.BandMathsType.create("cloud",
                                            "Cloud",
                                            width, height,
                                            "flags.cloud",
                                            colorIterator.next(),
                                            0.5));
        masks.add(Mask.BandMathsType.create("cloud_shadow",
                                            "Cloud Shadow",
                                            width, height,
                                            "flags.cloud_shadow",
                                            colorIterator.next(),
                                            0.5));
        masks.add(Mask.BandMathsType.create("snow",
                                            "Snow/Ice Cover",
                                            width, height,
                                            "flags.snow",
                                            colorIterator.next(),
                                            0.5));
        masks.add(Mask.BandMathsType.create("clear",
                                            "Clear",
                                            width, height,
                                            "flags.clear",
                                            colorIterator.next(),
                                            0.5));
        masks.add(Mask.BandMathsType.create("water",
                                            "Water",
                                            width, height,
                                            "flags.water",
                                            colorIterator.next(),
                                            0.5));

        //masks.addAll(createDefaultRadiometricSaturationMasks("radiometric_saturation", "Radiometric saturation", width, height));
        masks.add(Mask.BandMathsType.create("Saturation for band 1", "Saturation for band 1",
                                            width, height, "satflags.radiometric_saturation_b1",
                                            colorIterator.next(), 0.5));
        masks.add(Mask.BandMathsType.create("Saturation for band 2", "Saturation for band 2",
                                            width, height, "satflags.radiometric_saturation_b2",
                                            colorIterator.next(), 0.5));
        masks.add(Mask.BandMathsType.create("Saturation for band 3", "Saturation for band 3",
                                            width, height, "satflags.radiometric_saturation_b3",
                                            colorIterator.next(), 0.5));
        masks.add(Mask.BandMathsType.create("Saturation for band 4", "Saturation for band 4",
                                            width, height, "satflags.radiometric_saturation_b4",
                                            colorIterator.next(), 0.5));
        masks.add(Mask.BandMathsType.create("Saturation for band 5", "Saturation for band 5",
                                            width, height, "satflags.radiometric_saturation_b5",
                                            colorIterator.next(), 0.5));
        masks.add(Mask.BandMathsType.create("Saturation for band 6", "Saturation for band 6",
                                            width, height, "satflags.radiometric_saturation_b6",
                                            colorIterator.next(), 0.5));
        masks.add(Mask.BandMathsType.create("Saturation for band 7", "Saturation for band 7",
                                            width, height, "satflags.radiometric_saturation_b7",
                                            colorIterator.next(), 0.5));
        masks.add(Mask.BandMathsType.create("Saturation for band 9", "Saturation for band 9",
                                            width, height, "satflags.radiometric_saturation_b9",
                                            colorIterator.next(), 0.5));
        masks.addAll(createDefaultConfidenceMasks("snow_ice_confidence", "Snow/ice confidence", width, height));
        masks.addAll(createDefaultConfidenceMasks("cloud_confidence", "Cloud confidence", width, height));
        masks.addAll(createDefaultConfidenceMasks("cloud_shadow_confidence", "Cloud shadow confidence", width, height));
        masks.addAll(createDefaultConfidenceMasks("cirrus_confidence", "Cirrus confidence", width, height));


        return masks;
    }
}
