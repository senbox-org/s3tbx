package org.esa.s3tbx.dataio.landsat.geotiff.level2;

import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.Mask;

import java.util.List;

/**
 * Created by obarrile on 10/02/2019.
 */
public interface LandsatL2Cloud {
    FlagCoding createFlagCoding();
    List<Mask> createMasks(int width, int height);
}
