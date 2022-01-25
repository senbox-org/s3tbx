package org.esa.s3tbx.dataio.landsat.geotiff;

import org.esa.snap.core.datamodel.Mask;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by obarrile on 01/01/2019.
 */
public abstract class AbstractLandsatQA implements LandsatQA {

    protected final LandsatColorIterator colorIterator;

    public AbstractLandsatQA() {
        colorIterator = new LandsatColorIterator();
    }


    protected List<Mask> createDefaultConfidenceMasks(String flagMaskBaseName, String descriptionBaseName, int width, int height) {
        List<Mask> masks = new ArrayList<>();
        masks.add(Mask.BandMathsType.create(flagMaskBaseName + "_low",
                                            descriptionBaseName + " 0-35%",
                                            width, height,
                                            "flags." + flagMaskBaseName + "_one and not flags." + flagMaskBaseName + "_two",
                                            colorIterator.next(),
                                            0.5));
        masks.add(Mask.BandMathsType.create(flagMaskBaseName + "_mid",
                                            descriptionBaseName + " 36-64%",
                                            width, height,
                                            "not flags." + flagMaskBaseName + "_one and flags." + flagMaskBaseName + "_two",
                                            colorIterator.next(),
                                            0.5));
        masks.add(Mask.BandMathsType.create(flagMaskBaseName + "_high",
                                            descriptionBaseName + " 65-100%",
                                            width, height,
                                            "flags." + flagMaskBaseName + "_one and flags." + flagMaskBaseName + "_two",
                                            colorIterator.next(),
                                            0.5));
        return masks;
    }

    protected List<Mask> createDefaultRadiometricSaturationMasks(String flagMaskBaseName, String descriptionBaseName, int width, int height) {
        List<Mask> masks = new ArrayList<>();
        masks.add(Mask.BandMathsType.create("No saturation",
                                            "No bands contain saturation",
                                            width, height,
                                            "not flags." + flagMaskBaseName + "_one and not flags." + flagMaskBaseName + "_two",
                                            colorIterator.next(),
                                            0.5));

        masks.add(Mask.BandMathsType.create("1-2 contain saturation",
                                            "1-2 contain saturation",
                                            width, height,
                                            "flags." + flagMaskBaseName + "_one and not flags." + flagMaskBaseName + "_two",
                                            colorIterator.next(),
                                            0.5));
        masks.add(Mask.BandMathsType.create("3-4 contain saturation",
                                            "3-4 contain saturation",
                                            width, height,
                                            "not flags." + flagMaskBaseName + "_one and flags." + flagMaskBaseName + "_two",
                                            colorIterator.next(),
                                            0.5));
        masks.add(Mask.BandMathsType.create("5 or more bands contain saturation",
                                            "5 or more bands contain saturation",
                                            width, height,
                                            "flags." + flagMaskBaseName + "_one and flags." + flagMaskBaseName + "_two",
                                            colorIterator.next(),
                                            0.5));
        return masks;
    }
}
