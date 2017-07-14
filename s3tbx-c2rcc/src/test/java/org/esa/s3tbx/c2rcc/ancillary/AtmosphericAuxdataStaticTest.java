package org.esa.s3tbx.c2rcc.ancillary;

import org.junit.Test;

import java.util.Calendar;

import static org.junit.Assert.*;

/**
 * @author Marco Peters
 */
public class AtmosphericAuxdataStaticTest {

    @Test
    public void createCalendarByFilename() throws Exception {
        Calendar calendar;
        calendar = AtmosphericAuxdataStatic.createCalendarByFilename("N201519412_O3_AURAOMI_24h.hdf");
        verifyCalendar(calendar, 194, 2015, 6, 13, 12);

        calendar = AtmosphericAuxdataStatic.createCalendarByFilename("N201606006_O3_AURAOMI_24h.hdf");
        verifyCalendar(calendar, 60, 2016, 1, 29, 6);
        calendar = AtmosphericAuxdataStatic.createCalendarByFilename("N201606108_O3_AURAOMI_24h.hdf");
        verifyCalendar(calendar, 61, 2016, 2, 1, 8);
    }

    private void verifyCalendar(Calendar calendar, int doy, int year, int month, int dayOfMonth, int hour) {
        assertEquals(doy,calendar.get(Calendar.DAY_OF_YEAR));
        assertEquals(year, calendar.get(Calendar.YEAR));
        assertEquals(month, calendar.get(Calendar.MONTH));
        assertEquals(dayOfMonth, calendar.get(Calendar.DAY_OF_MONTH));
        assertEquals(hour, calendar.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, calendar.get(Calendar.MINUTE));
        assertEquals(0, calendar.get(Calendar.SECOND));
        assertEquals(0, calendar.get(Calendar.MILLISECOND));
    }

}