package org.esa.s3tbx.mphchl;

import org.esa.snap.core.gpf.pointop.TargetSampleConfigurer;

import java.util.HashMap;

public class TestTargetSampleConfigurer implements TargetSampleConfigurer {

    private HashMap<Integer, String> sampleMap = new HashMap<>();

    @Override
    public void defineSample(int index, String name) {
        sampleMap.put(index, name);
    }

    public HashMap<Integer, String> getSampleMap() {
        return sampleMap;
    }
}
