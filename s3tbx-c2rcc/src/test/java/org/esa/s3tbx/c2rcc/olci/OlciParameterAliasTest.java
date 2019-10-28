package org.esa.s3tbx.c2rcc.olci;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import static org.junit.Assert.*;


/**
 * @author Marco Peters
 * @since
 */
public class OlciParameterAliasTest {

    @Test
    public void testMethod() throws FactoryException, TransformException {

        C2rccOlciOperator operator = new C2rccOlciOperator();
        operator.setParameterDefaultValues();
        operator.setSourceProduct(OlciTestProduct.create());

        assertNotNull(operator.getParameter("TSMfac"));
        assertNotNull(operator.getParameter("TSMexp"));
        // old deprecated parameter
        assertNotNull(operator.getParameter("TSMfakBpart"));
        assertNotNull(operator.getParameter("TSMfakBwit"));

        assertEquals(operator.getParameter("TSMfac"), operator.getParameter("TSMfakBpart"));
        assertEquals(operator.getParameter("TSMexp"), operator.getParameter("TSMfakBwit"));

    }
}