package org.esa.s3tbx.dataio.landsat.geotiff;

import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.Mask;

import java.awt.*;
import java.util.List;

/**
 * Created by obarrile on 01/01/2019.
 */
public interface LandsatQA {
    FlagCoding createFlagCoding(String bandName);
    List<Mask> createMasks(Dimension size);
}
