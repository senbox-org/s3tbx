package org.esa.s3tbx.dataio.landsat.geotiff.level2;

import org.esa.s3tbx.dataio.landsat.geotiff.LandsatColorIterator;
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

    protected final LandsatColorIterator colorIterator;

    public Landsat8L2Aerosol() {
        colorIterator = new LandsatColorIterator();
    }

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
                                            colorIterator.next(),
                                            0.5));
        masks.add(Mask.BandMathsType.create("Aerosol_Valid",
                                            "Valid aerosol retrieval",
                                            width, height,
                                            "aerosol_flags.valid_aerosol_retrieval",
                                            colorIterator.next(),
                                            0.5));
        masks.add(Mask.BandMathsType.create("Aerosol_Water",
                                            "water pixel",
                                            width, height,
                                            "aerosol_flags.water",
                                            colorIterator.next(),
                                            0.5));
        masks.add(Mask.BandMathsType.create("Aerosol_cloud_cirrus",
                                            "cloud/cirrus",
                                            width, height,
                                            "aerosol_flags.cloud_cirrus",
                                            colorIterator.next(),
                                            0.5));
        masks.add(Mask.BandMathsType.create("Aerosol_cloud_shadow",
                                            "cloud shadow",
                                            width, height,
                                            "aerosol_flags.cloud_shadow",
                                            colorIterator.next(),
                                            0.5));
        masks.add(Mask.BandMathsType.create("Aerosol_non_center",
                                            "non-center window pixel for which aerosol was interpolated from surrounding NxN center pixels",
                                            width, height,
                                            "aerosol_flags.non_center",
                                            colorIterator.next(),
                                            0.5));
        masks.add(Mask.BandMathsType.create("aerosol_level_climatology",
                                            "Aerosol level climatology",
                                            width, height,
                                            "not aerosol_flags.aerosol_level_one and not aerosol_flags.aerosol_level_two",
                                            colorIterator.next(),
                                            0.5));
        masks.add(Mask.BandMathsType.create("aerosol_level_low",
                                            "Aerosol level low",
                                            width, height,
                                            "not aerosol_flags.aerosol_level_one and aerosol_flags.aerosol_level_two",
                                            colorIterator.next(),
                                            0.5));
        masks.add(Mask.BandMathsType.create("aerosol_level_medium",
                                            "Aerosol level medium",
                                            width, height,
                                            "aerosol_flags.aerosol_level_one and not aerosol_flags.aerosol_level_two",
                                            colorIterator.next(),
                                            0.5));
        masks.add(Mask.BandMathsType.create("aerosol_level_high",
                                            "Aerosol level high",
                                            width, height,
                                            "aerosol_flags.aerosol_level_one and aerosol_flags.aerosol_level_two",
                                            colorIterator.next(),
                                            0.5));
        return masks;
    }
}
