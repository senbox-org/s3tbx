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
public class Landsat8L2QA implements LandsatL2QA {

    protected final LandsatColorIterator colorIterator;

    public Landsat8L2QA() {
        colorIterator = new LandsatColorIterator();
    }
    @Override
    public FlagCoding createFlagCoding() {
        FlagCoding flagCoding = new FlagCoding("pixel_qa");
        flagCoding.addFlag("fill", 1, "Fill");
        flagCoding.addFlag("clear", 2, "Clear");
        flagCoding.addFlag("water", 4, "Water");
        flagCoding.addFlag("cloud_shadow", 8, "Cloud Shadow");
        flagCoding.addFlag("snow", 16, "Snow");
        flagCoding.addFlag("cloud", 32, "Cloud");
        flagCoding.addFlag("cloud_confidence_one", 64, "Cloud confidence bit one");
        flagCoding.addFlag("cloud_confidence_two", 128, "Cloud confidence bit two");
        flagCoding.addFlag("cirrus_confidence_one", 256, "Cirrus confidence bit one");
        flagCoding.addFlag("cirrus_confidence_two", 512, "Cirrus confidence bit two");
        flagCoding.addFlag("terrain_oclusion", 1024, "Terrain Occlusion");
        return flagCoding;
    }

    @Override
    public List<Mask> createMasks(int width, int height) {
        List<Mask> masks = new ArrayList<>();
        masks.add(Mask.BandMathsType.create("Fill",
                                            "Fill",
                                            width, height,
                                            "pixel_qa.fill",
                                            colorIterator.next(),
                                            0.5));
        masks.add(Mask.BandMathsType.create("Clear",
                                            "Clear",
                                            width, height,
                                            "pixel_qa.clear",
                                            colorIterator.next(),
                                            0.5));
        masks.add(Mask.BandMathsType.create("Water",
                                            "Water",
                                            width, height,
                                            "pixel_qa.water",
                                            colorIterator.next(),
                                            0.5));
        masks.add(Mask.BandMathsType.create("Cloud_Shadow",
                                            "Cloud Shadow",
                                            width, height,
                                            "pixel_qa.cloud_shadow",
                                            colorIterator.next(),
                                            0.5));
        masks.add(Mask.BandMathsType.create("Snow",
                                            "Snow",
                                            width, height,
                                            "pixel_qa.snow",
                                            colorIterator.next(),
                                            0.5));
        masks.add(Mask.BandMathsType.create("Cloud",
                                            "Cloud",
                                            width, height,
                                            "pixel_qa.cloud",
                                            colorIterator.next(),
                                            0.5));
        masks.add(Mask.BandMathsType.create("Cloud_Confidence_None",
                                            "Cloud Confidence None",
                                            width, height,
                                            "not pixel_qa.cloud_confidence_one and not pixel_qa.cloud_confidence_two",
                                            colorIterator.next(),
                                            0.5));
        masks.add(Mask.BandMathsType.create("Cloud_Confidence_Low",
                                            "Cloud Confidence Low",
                                            width, height,
                                            "not pixel_qa.cloud_confidence_one and pixel_qa.cloud_confidence_two",
                                            colorIterator.next(),
                                            0.5));
        masks.add(Mask.BandMathsType.create("Cloud_Confidence_Medium",
                                            "Cloud Confidence Medium",
                                            width, height,
                                            "pixel_qa.cloud_confidence_one and not pixel_qa.cloud_confidence_two",
                                            colorIterator.next(),
                                            0.5));
        masks.add(Mask.BandMathsType.create("Cloud_Confidence_High",
                                            "Cloud Confidence High",
                                            width, height,
                                            "pixel_qa.cloud_confidence_one and pixel_qa.cloud_confidence_two",
                                            colorIterator.next(),
                                            0.5));
        masks.add(Mask.BandMathsType.create("Cirrus_Confidence_None",
                                            "Cirrus Confidence None",
                                            width, height,
                                            "not pixel_qa.cirrus_confidence_one and not pixel_qa.cirrus_confidence_two",
                                            colorIterator.next(),
                                            0.5));
        masks.add(Mask.BandMathsType.create("Cirrus_Confidence_Low",
                                            "Cirrus Confidence Low",
                                            width, height,
                                            "not pixel_qa.cirrus_confidence_one and pixel_qa.cirrus_confidence_two",
                                            colorIterator.next(),
                                            0.5));
        masks.add(Mask.BandMathsType.create("Cirrus_Confidence_Medium",
                                            "Cirrus Confidence Medium",
                                            width, height,
                                            "pixel_qa.cirrus_confidence_one and not pixel_qa.cirrus_confidence_two",
                                            colorIterator.next(),
                                            0.5));
        masks.add(Mask.BandMathsType.create("Cirrus_Confidence_High",
                                            "Cirrus Confidence High",
                                            width, height,
                                            "pixel_qa.cirrus_confidence_one and pixel_qa.cirrus_confidence_two",
                                            colorIterator.next(),
                                            0.5));
        masks.add(Mask.BandMathsType.create("Terrain_Oclusion",
                                            "Terrain Oclusion",
                                            width, height,
                                            "pixel_qa.terrain_oclusion",
                                            colorIterator.next(),
                                            0.5));
        return masks;
    }
}
