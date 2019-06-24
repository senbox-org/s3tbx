package org.esa.s3tbx.olci.harmonisation;

import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.OperatorSpiRegistry;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

public class OlciHarmonisationOpTest {

    @Test
    public void testOperatorSpiIsLoaded() {
        OperatorSpiRegistry registry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        OperatorSpi operatorSpi = registry.getOperatorSpi("OlciHarmonisation");
        assertNotNull(operatorSpi);
        assertEquals("OlciHarmonisation", operatorSpi.getOperatorAlias());
        assertNotNull(operatorSpi.getOperatorDescriptor());
        assertSame(operatorSpi.getOperatorClass(), operatorSpi.getOperatorDescriptor().getOperatorClass());
    }

}
