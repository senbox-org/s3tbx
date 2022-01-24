package org.esa.s3tbx.dataio.landsat.geotiff.c2;

import org.esa.s3tbx.dataio.landsat.geotiff.AbstractLandsatQA;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.Mask;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Definition of Landsat MSS L1 QA flags (Collection 2)
 * Reference: https://www.usgs.gov/media/files/landsat-1-5-mss-collection-2-level-1-data-format-control-book
 *
 * @author Cosmin Cara
 */
public class Collection2MSSLandsatQA extends AbstractLandsatQA {
    private final static Map<String, List<FlagCodingArgs>> codings = new HashMap<String, List<FlagCodingArgs>>() {{
        put("FILE_NAME_QUALITY_L1_PIXEL",
            new ArrayList<FlagCodingArgs>() {{
                int position = 1;
                add(new FlagCodingArgs("designated_fill", position, "Fill"));
                position <<= 3; // positions 1-2 not used
                add(new FlagCodingArgs("cloud", position, "Cloud"));
                position <<= 5; // positions 4-7 not used
                add(new FlagCodingArgs("cloud_confidence_one", position, "Cloud confidence bit one"));
                position <<= 1;
                add(new FlagCodingArgs("cloud_confidence_two", position, "Cloud confidence bit two"));
                // positions 10-15 not used
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
                position <<= 3; // position 7-8 not used
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
                                            "Fill",
                                            width, height,
                                            "flags.designated_fill",
                                            colorIterator.next(),
                                            0.5));
        masks.add(Mask.BandMathsType.create("cloud",
                                            "Cloud",
                                            width, height,
                                            "flags.cloud",
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
        masks.addAll(createDefaultConfidenceMasks("cloud_confidence", "Cloud confidence", width, height));

        return masks;
    }

}
