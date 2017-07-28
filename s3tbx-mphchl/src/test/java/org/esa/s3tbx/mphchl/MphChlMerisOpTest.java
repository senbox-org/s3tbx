package org.esa.s3tbx.mphchl;

import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.*;

public class MphChlMerisOpTest {

    @Test
    public void testOperatorMetadata() {
        final OperatorMetadata operatorMetadata = MphChlMerisOp.class.getAnnotation(OperatorMetadata.class);
        assertNotNull(operatorMetadata);
        assertEquals("MphChlMeris", operatorMetadata.alias());
        assertEquals("1.0", operatorMetadata.version());
        assertEquals("Mark William Matthews, Daniel Odermatt, Tom Block, Olaf Danne", operatorMetadata.authors());
        assertEquals("(c) 2013, 2014, 2017 by Brockmann Consult", operatorMetadata.copyright());
        assertEquals("Computes maximum peak height of chlorophyll for MERIS. Implements MERIS-specific parts.",
                     operatorMetadata.description());
    }

    @Test
    public void testConfigureSourceSample() {
        final TestSourceSampleConfigurer sampleConfigurer = new TestSourceSampleConfigurer();
        MphChlMerisOp mphChlOp = new MphChlMerisOp();
        mphChlOp.configureSourceSamples(sampleConfigurer);

        final HashMap<Integer, String> sampleMap = sampleConfigurer.getSampleMap();
        assertEquals("rBRR_06", sampleMap.get(0));
        assertEquals("rBRR_07", sampleMap.get(1));
        assertEquals("rBRR_08", sampleMap.get(2));
        assertEquals("rBRR_09", sampleMap.get(3));
        assertEquals("rBRR_10", sampleMap.get(4));
        assertEquals("rBRR_14", sampleMap.get(5));
    }

}
