/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.s3tbx.dataio.modis;

import org.junit.Before;
import org.junit.Test;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ModisUtilsTest_CreateDateFromString {
    private Calendar cal;

    @Before
    public void setUp() {
        cal = GregorianCalendar.getInstance(TimeZone.getTimeZone("UTC"));
    }

    @Test
    public void testOk_MoreThanMillisecondsSnipped() throws ParseException {
        final String date = "2004-07-10";
        // pareable time = "21:55:11.123"
        final String time = "21:55:11.123456";

        final Date testDate = ModisUtils.createDateFromStrings(date, time);

        assertNotNull(testDate);
        cal.setTime(testDate);
        assertEquals(2004, cal.get(Calendar.YEAR));
        assertEquals(7 - 1, cal.get(Calendar.MONTH));
        assertEquals(10, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(21, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(55, cal.get(Calendar.MINUTE));
        assertEquals(11, cal.get(Calendar.SECOND));
        // the trailing after Milliseconds numers "456" where snipped
        assertEquals(123, cal.get(Calendar.MILLISECOND));
    }

    @Test
    public void testOk_exact() throws ParseException {
        final String date = "2005-08-22";
        final String time = "12:22:09.887";

        final Date testDate = ModisUtils.createDateFromStrings(date, time);

        assertNotNull(testDate);
        cal.setTime(testDate);
        assertEquals(2005, cal.get(Calendar.YEAR));
        assertEquals(8 - 1, cal.get(Calendar.MONTH));
        assertEquals(22, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(12, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(22, cal.get(Calendar.MINUTE));
        assertEquals(9, cal.get(Calendar.SECOND));
        assertEquals(887, cal.get(Calendar.MILLISECOND));
    }

    @Test
    public void testOk_WithoutMilliseconds() throws ParseException {
        final String date = "2007-03-25";
        final String time = "15:16:17";

        final Date testDate = ModisUtils.createDateFromStrings(date, time);

        assertNotNull(testDate);
        cal.setTime(testDate);
        assertEquals(2007, cal.get(Calendar.YEAR));
        assertEquals(3 - 1, cal.get(Calendar.MONTH));
        assertEquals(25, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(15, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(16, cal.get(Calendar.MINUTE));
        assertEquals(17, cal.get(Calendar.SECOND));
        assertEquals(0, cal.get(Calendar.MILLISECOND));
    }

    @Test
    public void testOk_WithMillisecondsFragment() throws ParseException {
        final String date = "2001-09-13";
        final String time = "07:08:09.4";

        final Date testDate = ModisUtils.createDateFromStrings(date, time);

        assertNotNull(testDate);
        cal.setTime(testDate);
        assertEquals(2001, cal.get(Calendar.YEAR));
        assertEquals(9 - 1, cal.get(Calendar.MONTH));
        assertEquals(13, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(7, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(8, cal.get(Calendar.MINUTE));
        assertEquals(9, cal.get(Calendar.SECOND));
        assertEquals(400, cal.get(Calendar.MILLISECOND));
    }

    @Test
    public void testOk_WithoutSeconds() throws ParseException {
        final String date = "2008-04-21";
        final String time = "17:18";

        final Date testDate = ModisUtils.createDateFromStrings(date, time);

        assertNotNull(testDate);
        cal.setTime(testDate);
        assertEquals(2008, cal.get(Calendar.YEAR));
        assertEquals(4 - 1, cal.get(Calendar.MONTH));
        assertEquals(21, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(17, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(18, cal.get(Calendar.MINUTE));
        assertEquals(0, cal.get(Calendar.SECOND));
        assertEquals(0, cal.get(Calendar.MILLISECOND));
    }
}
