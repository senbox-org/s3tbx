package org.esa.s3tbx.dataio.landsat.geotiff.level2;

import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.Mask;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by obarrile on 10/02/2019.
 */
public class Landsat8L2Aerosol implements LandsatL2Aerosol{
    @Override
    public FlagCoding createFlagCoding() {
        FlagCoding flagCoding = new FlagCoding("aerosol_flags");
        flagCoding.addFlag("fill", 1, "Fill");
        flagCoding.addFlag("valid_aerosol_retrieval", 2, "valid aerosol retrieval (center pixel of NxN window)");
        flagCoding.addFlag("water", 4, "water pixel (or water pixel was used in the fill-the-window interpolation)");
        flagCoding.addFlag("cloud_cirrus", 8, "Cloud or cirrus");
        flagCoding.addFlag("cloud_shadow", 16, "Cloud Shadow");
        flagCoding.addFlag("non_center", 32, "non-center window pixel for which aerosol was interpolated from surrounding NxN center pixels");
        flagCoding.addFlag("aerosol_level_one", 64, "Aerosol level bit one");
        flagCoding.addFlag("aerosol_level_two", 128, "Aerosol level bit two");
        return flagCoding;
    }

    @Override
    public List<Mask> createMasks(int width, int height) {
        List<Mask> masks = new ArrayList<>();
        masks.add(Mask.BandMathsType.create("Aerosol_Fill",
                                            "Fill",
                                            width, height,
                                            "aerosol_flags.fill",
                                            ColorIterator.next(),
                                            0.5));
        masks.add(Mask.BandMathsType.create("Aerosol_Valid",
                                            "Valid aerosol retrieval",
                                            width, height,
                                            "aerosol_flags.valid_aerosol_retrieval",
                                            ColorIterator.next(),
                                            0.5));
        masks.add(Mask.BandMathsType.create("Aerosol_Water",
                                            "water pixel",
                                            width, height,
                                            "aerosol_flags.water",
                                            ColorIterator.next(),
                                            0.5));
        masks.add(Mask.BandMathsType.create("Aerosol_cloud_cirrus",
                                            "cloud/cirrus",
                                            width, height,
                                            "aerosol_flags.cloud_cirrus",
                                            ColorIterator.next(),
                                            0.5));
        masks.add(Mask.BandMathsType.create("Aerosol_cloud_shadow",
                                            "cloud shadow",
                                            width, height,
                                            "aerosol_flags.cloud_shadow",
                                            ColorIterator.next(),
                                            0.5));
        masks.add(Mask.BandMathsType.create("Aerosol_non_center",
                                            "non-center window pixel for which aerosol was interpolated from surrounding NxN center pixels",
                                            width, height,
                                            "aerosol_flags.non_center",
                                            ColorIterator.next(),
                                            0.5));
        masks.add(Mask.BandMathsType.create("aerosol_level_climatology",
                                            "Aerosol level climatology",
                                            width, height,
                                            "not aerosol_flags.aerosol_level_one and not aerosol_flags.aerosol_level_two",
                                            ColorIterator.next(),
                                            0.5));
        masks.add(Mask.BandMathsType.create("aerosol_level_low",
                                            "Aerosol level low",
                                            width, height,
                                            "not aerosol_flags.aerosol_level_one and aerosol_flags.aerosol_level_two",
                                            ColorIterator.next(),
                                            0.5));
        masks.add(Mask.BandMathsType.create("aerosol_level_medium",
                                            "Aerosol level medium",
                                            width, height,
                                            "aerosol_flags.aerosol_level_one and not aerosol_flags.aerosol_level_two",
                                            ColorIterator.next(),
                                            0.5));
        masks.add(Mask.BandMathsType.create("aerosol_level_high",
                                            "Aerosol level high",
                                            width, height,
                                            "aerosol_flags.aerosol_level_one and aerosol_flags.aerosol_level_two",
                                            ColorIterator.next(),
                                            0.5));
        return masks;
    }

    protected static class ColorIterator {

        static ArrayList<Color> colors;
        static Iterator<Color> colorIterator;

        static {
            colors = new ArrayList<>();
            colors.add(Color.red);
            colors.add(Color.red.darker());
            colors.add(Color.red.darker().darker());
            colors.add(Color.blue);
            colors.add(Color.blue.darker());
            colors.add(Color.blue.darker().darker());
            colors.add(Color.green);
            colors.add(Color.green.darker());
            colors.add(Color.green.darker().darker());
            colors.add(Color.yellow);
            colors.add(Color.yellow.darker());
            colors.add(Color.yellow.darker().darker());
            colors.add(Color.magenta);
            colors.add(Color.magenta.darker());
            colors.add(Color.magenta.darker().darker());
            colors.add(Color.pink);
            colors.add(Color.pink.darker());
            colors.add(Color.pink.darker().darker());
            colorIterator = colors.iterator();
        }

        static Color next() {
            if (!colorIterator.hasNext()) {
                colorIterator = colors.iterator();
            }
            return colorIterator.next();
        }
    }
}
