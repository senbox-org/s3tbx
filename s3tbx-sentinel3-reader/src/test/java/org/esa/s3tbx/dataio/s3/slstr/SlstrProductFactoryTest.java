package org.esa.s3tbx.dataio.s3.slstr;

import org.junit.Test;
import static org.junit.Assert.*;

public class SlstrProductFactoryTest {

    @Test
    public void testGetGridIndex() {
        assertEquals("in", SlstrProductFactory.getGridIndex("a_name_ending_in_in"));
        assertEquals("io", SlstrProductFactory.getGridIndex("a_name_ending_in_io"));
        assertEquals("fn", SlstrProductFactory.getGridIndex("a_name_ending_in_fn"));
        assertEquals("tx", SlstrProductFactory.getGridIndex("a_name_ending_in_tx"));
        assertEquals("in", SlstrProductFactory.getGridIndex("a_name_ending_in_in_lsb"));
        assertEquals("io", SlstrProductFactory.getGridIndex("a_name_ending_in_io_msb"));
        assertEquals("y", SlstrProductFactory.getGridIndex("y"));
        assertEquals("th", SlstrProductFactory.getGridIndex("th_fetrzgh"));
        assertEquals("gh", SlstrProductFactory.getGridIndex("the_fetrzgh"));
        assertEquals("f", SlstrProductFactory.getGridIndex("thtcfzghj_f"));
        assertEquals("-f", SlstrProductFactory.getGridIndex("thtcfzghj-f"));
        assertEquals("ao", SlstrProductFactory.getGridIndex("S1_exception_ao_no_parameters"));
    }

}
