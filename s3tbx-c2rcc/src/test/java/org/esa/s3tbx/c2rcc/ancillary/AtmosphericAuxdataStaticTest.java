package org.esa.s3tbx.c2rcc.ancillary;

import org.junit.Test;

import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

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
        assertEquals(doy, calendar.get(Calendar.DAY_OF_YEAR));
        assertEquals(year, calendar.get(Calendar.YEAR));
        assertEquals(month, calendar.get(Calendar.MONTH));
        assertEquals(dayOfMonth, calendar.get(Calendar.DAY_OF_MONTH));
        assertEquals(hour, calendar.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, calendar.get(Calendar.MINUTE));
        assertEquals(0, calendar.get(Calendar.SECOND));
        assertEquals(0, calendar.get(Calendar.MILLISECOND));
    }

    @Test
    public void testHoursSince1900ToCalendar() throws Exception {
        int hoursSince1900_start = 1054812;
        long year_start = 2020;
        long month_start = 4; //Calendar month are from 0 to 11
        long dayOfMonth_start = 1;
        long hour_start = 12;

        int hoursSince1900_end = 1055532;
        long year_end = 2020;
        long month_end = 4; //Calendar month are from 0 to 11
        long dayOfMonth_end = 31;
        long hour_end = 12;

        Calendar calendar_start = AtmosphericAuxdataStatic.createCalendarByHoursSince1900(hoursSince1900_start);

        assertEquals(year_start, calendar_start.get(Calendar.YEAR));
        assertEquals(month_start, calendar_start.get(Calendar.MONTH));
        assertEquals(dayOfMonth_start, calendar_start.get(Calendar.DAY_OF_MONTH));
        assertEquals(hour_start, calendar_start.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, calendar_start.get(Calendar.MINUTE));
        assertEquals(0, calendar_start.get(Calendar.SECOND));
        assertEquals(0, calendar_start.get(Calendar.MILLISECOND));

        Calendar calendar_end = AtmosphericAuxdataStatic.createCalendarByHoursSince1900(hoursSince1900_end);

        assertEquals(year_end, calendar_end.get(Calendar.YEAR));
        assertEquals(month_end, calendar_end.get(Calendar.MONTH));
        assertEquals(dayOfMonth_end, calendar_end.get(Calendar.DAY_OF_MONTH));
        assertEquals(hour_end, calendar_end.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, calendar_end.get(Calendar.MINUTE));
        assertEquals(0, calendar_end.get(Calendar.SECOND));
        assertEquals(0, calendar_end.get(Calendar.MILLISECOND));
    }
}