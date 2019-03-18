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
public class LandsatLEDAPSCloud implements LandsatL2Cloud {

    protected final LandsatColorIterator colorIterator;

    public LandsatLEDAPSCloud() {
        colorIterator = new LandsatColorIterator();
    }

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
                                            colorIterator.next(),
                                            0.5));
        masks.add(Mask.BandMathsType.create("cloud_qa_Cloud",
                                            "Cloud",
                                            width, height,
                                            "cloud_qa.cloud",
                                            colorIterator.next(),
                                            0.5));
        masks.add(Mask.BandMathsType.create("cloud_qa_cloud_shadow",
                                            "Cloud shadow",
                                            width, height,
                                            "cloud_qa.cloud_shadow",
                                            colorIterator.next(),
                                            0.5));
        masks.add(Mask.BandMathsType.create("cloud_qa_adjacent_cloud",
                                            "Adjacent to cloud",
                                            width, height,
                                            "cloud_qa.adjacent_cloud",
                                            colorIterator.next(),
                                            0.5));
        masks.add(Mask.BandMathsType.create("cloud_qa_snow",
                                            "Snow",
                                            width, height,
                                            "cloud_qa.snow",
                                            colorIterator.next(),
                                            0.5));
        masks.add(Mask.BandMathsType.create("cloud_qa_land_water",
                                            "Land/water",
                                            width, height,
                                            "cloud_qa.land_water",
                                            colorIterator.next(),
                                            0.5));
        return masks;
    }
}
