package org.esa.s3tbx.mphchl;

import org.esa.snap.core.datamodel.GeneralFilterBand;
import org.esa.snap.core.datamodel.Kernel;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.gpf.pointop.SourceSampleConfigurer;

import java.util.HashMap;

public class TestSourceSampleConfigurer implements SourceSampleConfigurer {
    private HashMap<Integer, String> sampleMap = new HashMap<>();

    @Override
    public void setValidPixelMask(String maskExpression) {
    }

    @Override
    public void defineSample(int index, String name) {
        sampleMap.put(index, name);
    }

    public HashMap<Integer, String> getSampleMap() {
        return sampleMap;
    }

    @Override
    public void defineSample(int index, String name, Product product) {
    }

    @Override
    public void defineComputedSample(int index, int dataType, String expression, Product... sourceProducts) {
    }

    @Override
    public void defineComputedSample(int index, int sourceIndex, Kernel kernel) {
    }

    @Override
    public void defineComputedSample(int index, int sourceIndex, GeneralFilterBand.OpType opType, Kernel structuringElement) {
    }

    @Override
    public void defineComputedSample(int index, RasterDataNode raster) {
    }
}
