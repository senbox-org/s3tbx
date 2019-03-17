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
public class LandsatLEDAPSSaturation implements LandsatL2Saturation {

    protected final LandsatColorIterator colorIterator;

    public LandsatLEDAPSSaturation() {
        colorIterator = new LandsatColorIterator();
    }

    @Override
    public FlagCoding createFlagCoding(String bandName) {
        FlagCoding flagCoding = new FlagCoding("saturation");
        flagCoding.addFlag("data_fill_flag", 1, "Designated Fill");
        flagCoding.addFlag("Band1_saturated", 2, "Band 1 Data Saturation Flag");
        flagCoding.addFlag("Band2_saturated", 4, "Band 2 Data Saturation Flag");
        flagCoding.addFlag("Band3_saturated", 8, "Band 3 Data Saturation Fla ");
        flagCoding.addFlag("Band4_saturated", 16, "Band 4 Data Saturation Flag");
        flagCoding.addFlag("Band5_saturated", 32, "Band 5 Data Saturation Flag");
        flagCoding.addFlag("Band6_saturated", 64, "Band 6 Data Saturation Flag");
        flagCoding.addFlag("Band7_saturated", 128, "Band 7 Data Saturation Flag");
        return flagCoding;
    }

    @Override
    public List<Mask> createMasks(int width, int height) {
        List<Mask> masks = new ArrayList<>();
        masks.add(Mask.BandMathsType.create("No saturation",
                                            "No bands contain saturation",
                                            width, height,
                                            "not saturation.data_fill_flag",
                                            colorIterator.next(),
                                            0.5));
        masks.add(Mask.BandMathsType.create("Band 1 saturated",
                                            "Band 1 saturated",
                                            width, height,
                                            "saturation.Band1_saturated",
                                            colorIterator.next(),
                                            0.5));
        masks.add(Mask.BandMathsType.create("Band 2 saturated",
                                            "Band 2 saturated",
                                            width, height,
                                            "saturation.Band2_saturated",
                                            colorIterator.next(),
                                            0.5));
        masks.add(Mask.BandMathsType.create("Band 3 saturated",
                                            "Band 3 saturated",
                                            width, height,
                                            "saturation.Band3_saturated",
                                            colorIterator.next(),
                                            0.5));
        masks.add(Mask.BandMathsType.create("Band 4 saturated",
                                            "Band 4 saturated",
                                            width, height,
                                            "saturation.Band4_saturated",
                                            colorIterator.next(),
                                            0.5));
        masks.add(Mask.BandMathsType.create("Band 5 saturated",
                                            "Band 5 saturated",
                                            width, height,
                                            "saturation.Band5_saturated",
                                            colorIterator.next(),
                                            0.5));
        masks.add(Mask.BandMathsType.create("Band 6 saturated",
                                            "Band 6 saturated",
                                            width, height,
                                            "saturation.Band6_saturated",
                                            colorIterator.next(),
                                            0.5));
        masks.add(Mask.BandMathsType.create("Band 7 saturated",
                                            "Band 7 saturated",
                                            width, height,
                                            "saturation.Band7_saturated",
                                            colorIterator.next(),
                                            0.5));

        return masks;
    }
}
