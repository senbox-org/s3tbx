package org.esa.s3tbx.dataio.landsat.geotiff.level2;

import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.Mask;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by obarrile on 10/02/2019.
 */
public class LandsatLEDAPSCloud implements LandsatL2Cloud {
    @Override
    public FlagCoding createFlagCoding() {
        FlagCoding flagCoding = new FlagCoding("cloud_qa");
        flagCoding.addFlag("ddv", 1, "Dense dark vegetation");
        flagCoding.addFlag("cloud", 2, "Cloud");
        flagCoding.addFlag("cloud_shadow", 4, "Cloud Shadow");
        flagCoding.addFlag("adjacent_cloud", 8, "Adjacent to cloud");
        flagCoding.addFlag("snow", 16, "Snow");
        flagCoding.addFlag("land_water", 32, "Land/water");
        return flagCoding;
    }

    @Override
    public List<Mask> createMasks(int width, int height) {
        List<Mask> masks = new ArrayList<>();
        masks.add(Mask.BandMathsType.create("cloud_qa_DDV",
                                            "Dense dark vegetation",
                                            width, height,
                                            "cloud_qa.ddv",
                                            ColorIterator.next(),
                                            0.5));
        masks.add(Mask.BandMathsType.create("cloud_qa_Cloud",
                                            "Cloud",
                                            width, height,
                                            "cloud_qa.cloud",
                                            ColorIterator.next(),
                                            0.5));
        masks.add(Mask.BandMathsType.create("cloud_qa_cloud_shadow",
                                            "Cloud shadow",
                                            width, height,
                                            "cloud_qa.cloud_shadow",
                                            ColorIterator.next(),
                                            0.5));
        masks.add(Mask.BandMathsType.create("cloud_qa_adjacent_cloud",
                                            "Adjacent to cloud",
                                            width, height,
                                            "cloud_qa.adjacent_cloud",
                                            ColorIterator.next(),
                                            0.5));
        masks.add(Mask.BandMathsType.create("cloud_qa_snow",
                                            "Snow",
                                            width, height,
                                            "cloud_qa.snow",
                                            ColorIterator.next(),
                                            0.5));
        masks.add(Mask.BandMathsType.create("cloud_qa_land_water",
                                            "Land/water",
                                            width, height,
                                            "cloud_qa.land_water",
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
