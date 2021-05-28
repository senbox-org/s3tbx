/*
 * Copyright (c) 2021.  Brockmann Consult GmbH (info@brockmann-consult.de)
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
 *
 *
 */

package org.esa.s3tbx.fu;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Marco Peters
 */
public class DominantLambdaLookupTest {

    @Test
    public void getDominantLambda() {

        final DominantLambdaLookup lookup = new DominantLambdaLookup();
        assertEquals(Double.NaN, lookup.getDominantLambda(-5), 1.0e-8);
        assertEquals(610, lookup.getDominantLambda(0.00), 1.0e-8);
        assertEquals(587.92934782, lookup.getDominantLambda(24.21), 1.0e-6);
        assertEquals(587.46739130, lookup.getDominantLambda(25.06), 1.0e-8);
        assertEquals(587, lookup.getDominantLambda(25.92), 1.0e-8);
        assertEquals(491.93946188, lookup.getDominantLambda(179.0), 1.0e-8);
        assertEquals(493.73906105, lookup.getDominantLambda(171.02552198498304), 1.0e-8);
        assertEquals(450, lookup.getDominantLambda(500), 1.0e-8);
    }

}