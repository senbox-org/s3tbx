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
 * Definition of Landsat 4-5 TM L1 QA flangs (Collection 2)
 * Reference: https://www.usgs.gov/media/files/landsat-4-5-tm-collection-2-level-1-data-format-control-book
 *
 * @author Cosmin Cara
 */
public class Collection2TMLandsatQA extends AbstractLandsatQA {
    private final static Map<String, List<FlagCodingArgs>> codings = new HashMap<String, List<FlagCodingArgs>>() {{
        put("FILE_NAME_QUALITY_L1_PIXEL",
            new ArrayList<FlagCodingArgs>() {{
                int position = 1; // 0
                add(new FlagCodingArgs("designated_fill", position, "Designated Fill"));
                position <<= 1; // 1
                add(new FlagCodingArgs("dillated_cloud", position, "Dillated Cloud"));
                position <<= 2; // 3 (position 2 unused)
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
                // positions 14-15 unused
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
                position <<= 3; // 9 (positions 7-8 not used)
                add(new FlagCodingArgs("dropped_pixel", position, "Dropped pixel"));
                // positions 10-15 not used
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
        masks.add(Mask.BandMathsType.create("dropped_pixel",
                                            "Dropped Pixel",
                                            width, height,
                                            "satflags.dropped_pixel",
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
                                            "Snow",
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
        masks.addAll(createDefaultConfidenceMasks("snow_ice_confidence", "Snow/ice confidence", width, height));
        masks.addAll(createDefaultConfidenceMasks("cloud_confidence", "Cloud confidence", width, height));
        masks.addAll(createDefaultConfidenceMasks("cloud_shadow_confidence", "Cloud shadow confidence", width, height));


        return masks;
    }

    @Override
    protected List<Mask> createDefaultRadiometricSaturationMasks(String flagMaskBaseName, String descriptionBaseName, int width, int height) {
        List<Mask> masks = new ArrayList<>();
        masks.add(Mask.BandMathsType.create("No saturation",
                                            "No bands contain saturation",
                                            width, height,
                                            "not satflags." + flagMaskBaseName + "_one and not satflags." + flagMaskBaseName + "_two",
                                            colorIterator.next(),
                                            0.5));

        masks.add(Mask.BandMathsType.create("1-2 contain saturation",
                                            "1-2 contain saturation",
                                            width, height,
                                            "satflags." + flagMaskBaseName + "_one and not satflags." + flagMaskBaseName + "_two",
                                            colorIterator.next(),
                                            0.5));
        masks.add(Mask.BandMathsType.create("3-4 contain saturation",
                                            "3-4 contain saturation",
                                            width, height,
                                            "not satflags." + flagMaskBaseName + "_one and satflags." + flagMaskBaseName + "_two",
                                            colorIterator.next(),
                                            0.5));
        masks.add(Mask.BandMathsType.create("5 or more bands contain saturation",
                                            "5 or more bands contain saturation",
                                            width, height,
                                            "satflags." + flagMaskBaseName + "_one and satflags." + flagMaskBaseName + "_two",
                                            colorIterator.next(),
                                            0.5));
        return masks;
    }
}
