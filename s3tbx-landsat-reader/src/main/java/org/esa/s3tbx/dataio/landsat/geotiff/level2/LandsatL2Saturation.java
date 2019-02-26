package org.esa.s3tbx.dataio.landsat.geotiff.level2;

import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.Mask;

import java.awt.*;
import java.util.List;

/**
 * Created by obarrile on 10/02/2019.
 */
public interface LandsatL2Saturation {
    FlagCoding createFlagCoding(String bandName);
    List<Mask> createMasks(int width, int height);
}
