package org.esa.s3tbx.c2rcc.util;

import org.esa.snap.core.datamodel.ProductData;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.text.ParseException;
import java.util.Calendar;

import static org.junit.Assert.*;

public class SolarFluxCorrectionFactorCalculatorTest {

    private final int endJan = 31;
    private final int endFeb = endJan + 28;
    private final int endMar = endFeb + 31;
    private final int endApr = endMar + 30;
    private final int endMay = endApr + 31;
    private final int endJun = endMay + 30;
    private final int endJul = endJun + 31;
    private final int endAug = endJul + 31;
    private final int endSep = endAug + 30;
    private final int endOct = endSep + 31;
    private final int endNov = endOct + 30;

    private ProductData.UTC time1;
    private ProductData.UTC time2;

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() throws Exception {
        time1 = new ProductData.UTC(3.5);
        time2 = new ProductData.UTC(5.5);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testUTCCreation() throws ParseException {
        final long millis1 = new ProductData.UTC(3.5).getAsCalendar().getTimeInMillis();
        final long millis2 = ProductData.UTC.parse("04-JAN-2000 12:00:00.000").getAsCalendar().getTimeInMillis();
        assertEquals(millis1, millis2);
    }

    @Test
    public void testGetCenterUTC() {
        final ProductData.UTC centerTime1 = SolarFluxCorrectionFactorCalculator.getCenterUTC(time1, time2);
        // center time reverse order
        final ProductData.UTC centerTime2 = SolarFluxCorrectionFactorCalculator.getCenterUTC(time2, time1);

        assertEquals("04-JAN-2000 12:00:00.000000", time1.format());
        assertEquals("06-JAN-2000 12:00:00.000000", time2.format());

        assertEquals("05-JAN-2000 12:00:00.000000", centerTime1.format());
        assertEquals("05-JAN-2000 12:00:00.000000", centerTime2.format());
    }

    @Test
    public void testSolFluxCorrFactor() {
        assertEquals(1.0351, SolarFluxCorrectionFactorCalculator.computeFactorFor(time1, time2), 1e-4);
    }

    @Test
    public void testGetSolarFluxDayCorrectionFactorForADay() {
        final int nonLeapYear = 2007;
        assertEquals(1.0350, SolarFluxCorrectionFactorCalculator.getDayCorrectionFactorFor(1, nonLeapYear), 1e-4);
        assertEquals(1.0306, SolarFluxCorrectionFactorCalculator.getDayCorrectionFactorFor(1 + endJan, nonLeapYear), 1e-4);
        assertEquals(1.0190, SolarFluxCorrectionFactorCalculator.getDayCorrectionFactorFor(1 + endFeb, nonLeapYear), 1e-4);
        assertEquals(1.0014, SolarFluxCorrectionFactorCalculator.getDayCorrectionFactorFor(1 + endMar, nonLeapYear), 1e-4);
        assertEquals(0.9845, SolarFluxCorrectionFactorCalculator.getDayCorrectionFactorFor(1 + endApr, nonLeapYear), 1e-4);
        assertEquals(0.9717, SolarFluxCorrectionFactorCalculator.getDayCorrectionFactorFor(1 + endMay, nonLeapYear), 1e-4);
        assertEquals(0.9666, SolarFluxCorrectionFactorCalculator.getDayCorrectionFactorFor(1 + endJun, nonLeapYear), 1e-4);
        assertEquals(0.9700, SolarFluxCorrectionFactorCalculator.getDayCorrectionFactorFor(1 + endJul, nonLeapYear), 1e-4);
        assertEquals(0.9814, SolarFluxCorrectionFactorCalculator.getDayCorrectionFactorFor(1 + endAug, nonLeapYear), 1e-4);
        assertEquals(0.9976, SolarFluxCorrectionFactorCalculator.getDayCorrectionFactorFor(1 + endSep, nonLeapYear), 1e-4);
        assertEquals(1.0155, SolarFluxCorrectionFactorCalculator.getDayCorrectionFactorFor(1 + endOct, nonLeapYear), 1e-4);
        assertEquals(1.0292, SolarFluxCorrectionFactorCalculator.getDayCorrectionFactorFor(1 + endNov, nonLeapYear), 1e-4);

        assertEquals(1.0309, SolarFluxCorrectionFactorCalculator.getDayCorrectionFactorFor(endJan, nonLeapYear), 1e-4);
        assertEquals(1.0195, SolarFluxCorrectionFactorCalculator.getDayCorrectionFactorFor(endFeb, nonLeapYear), 1e-4);
        assertEquals(1.0020, SolarFluxCorrectionFactorCalculator.getDayCorrectionFactorFor(endMar, nonLeapYear), 1e-4);
        assertEquals(0.9851, SolarFluxCorrectionFactorCalculator.getDayCorrectionFactorFor(endApr, nonLeapYear), 1e-4);
        assertEquals(0.9720, SolarFluxCorrectionFactorCalculator.getDayCorrectionFactorFor(endMay, nonLeapYear), 1e-4);
        assertEquals(0.9667, SolarFluxCorrectionFactorCalculator.getDayCorrectionFactorFor(endJun, nonLeapYear), 1e-4);
        assertEquals(0.9698, SolarFluxCorrectionFactorCalculator.getDayCorrectionFactorFor(endJul, nonLeapYear), 1e-4);
        assertEquals(0.9809, SolarFluxCorrectionFactorCalculator.getDayCorrectionFactorFor(endAug, nonLeapYear), 1e-4);
        assertEquals(0.9970, SolarFluxCorrectionFactorCalculator.getDayCorrectionFactorFor(endSep, nonLeapYear), 1e-4);
        assertEquals(1.0150, SolarFluxCorrectionFactorCalculator.getDayCorrectionFactorFor(endOct, nonLeapYear), 1e-4);
        assertEquals(1.0288, SolarFluxCorrectionFactorCalculator.getDayCorrectionFactorFor(endNov, nonLeapYear), 1e-4);
        assertEquals(1.0350, SolarFluxCorrectionFactorCalculator.getDayCorrectionFactorFor(31 + endNov, nonLeapYear), 1e-4);
    }

    @Test
    public void testSolFluxCorrectionForADate() throws ParseException {
        assertEquals(1, getDayOfYearFor("01-JAN-2011 12:00:00.000"));
        assertEquals(endJan + 1, getDayOfYearFor("01-FEB-2011 12:00:00.000"));
        assertEquals(endFeb + 1, getDayOfYearFor("01-MAR-2011 12:00:00.000"));
        assertEquals(endMar + 1, getDayOfYearFor("01-APR-2011 12:00:00.000"));
        assertEquals(endApr + 1, getDayOfYearFor("01-MAY-2011 12:00:00.000"));
        assertEquals(endMay + 1, getDayOfYearFor("01-JUN-2011 12:00:00.000"));
        assertEquals(endJun + 1, getDayOfYearFor("01-JUL-2011 12:00:00.000"));
        assertEquals(endJul + 1, getDayOfYearFor("01-AUG-2011 12:00:00.000"));
        assertEquals(endAug + 1, getDayOfYearFor("01-SEP-2011 12:00:00.000"));
        assertEquals(endSep + 1, getDayOfYearFor("01-OCT-2011 12:00:00.000"));
        assertEquals(endOct + 1, getDayOfYearFor("01-NOV-2011 12:00:00.000"));
        assertEquals(endNov + 1, getDayOfYearFor("01-DEC-2011 12:00:00.000"));

        // !!! leap year 2012 !!!
        int leapYearOffset = 1;
        int o = leapYearOffset;
        assertEquals(1, getDayOfYearFor("01-JAN-2012 12:00:00.000"));
        assertEquals(endJan + 1, getDayOfYearFor("01-FEB-2012 12:00:00.000"));
        assertEquals(endFeb + 1 + o, getDayOfYearFor("01-MAR-2012 12:00:00.000"));
        assertEquals(endMar + 1 + o, getDayOfYearFor("01-APR-2012 12:00:00.000"));
        assertEquals(endApr + 1 + o, getDayOfYearFor("01-MAY-2012 12:00:00.000"));
        assertEquals(endMay + 1 + o, getDayOfYearFor("01-JUN-2012 12:00:00.000"));
        assertEquals(endJun + 1 + o, getDayOfYearFor("01-JUL-2012 12:00:00.000"));
        assertEquals(endJul + 1 + o, getDayOfYearFor("01-AUG-2012 12:00:00.000"));
        assertEquals(endAug + 1 + o, getDayOfYearFor("01-SEP-2012 12:00:00.000"));
        assertEquals(endSep + 1 + o, getDayOfYearFor("01-OCT-2012 12:00:00.000"));
        assertEquals(endOct + 1 + o, getDayOfYearFor("01-NOV-2012 12:00:00.000"));
        assertEquals(endNov + 1 + o, getDayOfYearFor("01-DEC-2012 12:00:00.000"));
    }

    @Test
    public void testGetNumDaysForTheYear() {
        assertEquals(365, SolarFluxCorrectionFactorCalculator.getNumDaysInTheYear(2007));
        assertEquals(366, SolarFluxCorrectionFactorCalculator.getNumDaysInTheYear(2008));
        assertEquals(365, SolarFluxCorrectionFactorCalculator.getNumDaysInTheYear(2009));
        assertEquals(365, SolarFluxCorrectionFactorCalculator.getNumDaysInTheYear(2010));
        assertEquals(365, SolarFluxCorrectionFactorCalculator.getNumDaysInTheYear(2011));
        assertEquals(366, SolarFluxCorrectionFactorCalculator.getNumDaysInTheYear(2012));
        assertEquals(365, SolarFluxCorrectionFactorCalculator.getNumDaysInTheYear(2013));
        assertEquals(365, SolarFluxCorrectionFactorCalculator.getNumDaysInTheYear(2014));
        assertEquals(365, SolarFluxCorrectionFactorCalculator.getNumDaysInTheYear(2015));
        assertEquals(366, SolarFluxCorrectionFactorCalculator.getNumDaysInTheYear(2016));
        assertEquals(365, SolarFluxCorrectionFactorCalculator.getNumDaysInTheYear(2017));
    }

    @Test
    public void testTimeDiff() {
        final long diffMillies = time2.getAsCalendar().getTimeInMillis() - time1.getAsCalendar().getTimeInMillis();
        final double diffSeconds = diffMillies / 1000.0;
        final double diffMinutes = diffSeconds / 60.0;
        final double diffHours = diffMinutes / 60.0;
        final double diffDays = diffHours / 24.0;

        assertEquals("2.0", "" + diffDays);
        assertEquals("48.0", "" + diffHours);
        assertEquals("2880.0", "" + diffMinutes);
        assertEquals("172800.0", "" + diffSeconds);
        assertEquals("172800000", "" + diffMillies);
    }

    private int getDayOfYearFor(final String date) throws ParseException {
        return ProductData.UTC.parse(date).getAsCalendar().get(Calendar.DAY_OF_YEAR);
    }
}

