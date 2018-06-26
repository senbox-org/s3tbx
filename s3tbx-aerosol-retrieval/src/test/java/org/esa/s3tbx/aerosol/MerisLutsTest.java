/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.s3tbx.aerosol;

import junit.framework.TestCase;
import org.esa.s3tbx.aerosol.lut.MerisLuts;
import org.esa.snap.core.util.math.LookupTable;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

/**
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
public class MerisLutsTest extends TestCase {

    public void testLutAot() throws IOException {
        try (ImageInputStream iis = MerisLuts.getAotLutData()) {
            // read LUT dimensions and values
            float[] vza = MerisLuts.readDimension(iis);
            int nVza = vza.length;
            float[] sza = MerisLuts.readDimension(iis);
            int nSza = sza.length;
            float[] azi = MerisLuts.readDimension(iis);
            int nAzi = azi.length;
            float[] hsf = MerisLuts.readDimension(iis);
            int nHsf = hsf.length;
            // conversion from surf.pressure to elevation ASL
            for (int i = 0; i < nHsf; i++) {
                if (hsf[i] != -1) {
                    // 1.e-3 * (1.d - (xnodes[3, wh_nod] / 1013.25)^(1./5.25588)) / 2.25577e-5
                    final double a = hsf[i] / 1013.25;
                    final double b = 1. / 5.25588;
                    hsf[i] = (float) (0.001 * (1.0 - Math.pow(a, b)) / 2.25577E-5);
                }
            }
            float[] aot = MerisLuts.readDimension(iis);
            int nAot = aot.length;

            float[] parameters = new float[]{1.0f, 2.0f, 3.0f, 4.0f, 5.0f};
            int nParameters = parameters.length;

            float[] wvl = {
                    412.0f, 442.0f, 490.0f, 510.0f, 560.0f,
                    620.0f, 665.0f, 681.0f, 708.0f, 753.0f,
                    760.0f, 778.0f, 865.0f, 885.0f, 900.0f
            };
            final int nWvl = wvl.length;

            float[] tgLut = new float[nParameters * nVza * nSza * nAzi * nHsf * nAot * nWvl];

            for (int iWvl = 0; iWvl < nWvl; iWvl++) {
                for (int iAot = 0; iAot < nAot; iAot++) {
                    for (int iHsf = 0; iHsf < nHsf; iHsf++) {
                        for (int iAzi = 0; iAzi < nAzi; iAzi++) {
                            for (int iSza = 0; iSza < nSza; iSza++) {
                                for (int iVza = 0; iVza < nVza; iVza++) {
                                    for (int iParams = 0; iParams < nParameters; iParams++) {
                                        int iAziTemp = nAzi - iAzi - 1;
                                        int i = iParams + nParameters * (iVza + nVza * (iSza + nSza * (iAziTemp + nAzi * (iHsf + nHsf * (iAot + nAot * iWvl)))));
                                        tgLut[i] = iis.readFloat();
                                    }
                                }
                            }
                        }
                    }
                }
            }

            MerisLuts.readDimension(iis, nWvl); // skip wavelengths

            LookupTable lut = new LookupTable(tgLut, wvl, aot, hsf, azi, sza, vza, parameters);
            assertNotNull(lut);

            assertEquals(7, lut.getDimensionCount());

            final double[] parametersArray = lut.getDimension(6).getSequence();
            final int nnParameters = parametersArray.length;
            assertEquals(5, nnParameters);     //  Parameters
            assertEquals(1.0, parametersArray[0], 1.E-4);
            assertEquals(2.0, parametersArray[1], 1.E-4);
            assertEquals(3.0, parametersArray[2], 1.E-4);
            assertEquals(5.0, parametersArray[4], 1.E-4);

            final double[] vzaArray = lut.getDimension(5).getSequence();
            final int nnVza = vzaArray.length;
            assertEquals(13, nnVza);     //  VZA
            assertEquals(0.0, vzaArray[0], 1.E-4);
            assertEquals(12.76, vzaArray[3], 1.E-4);
            assertEquals(24.24, vzaArray[5], 1.E-4);
            assertEquals(35.68, vzaArray[7], 1.E-4);
            assertEquals(64.2799987, vzaArray[12], 1.E-4);

            final double[] szaArray = lut.getDimension(4).getSequence();
            final int nnSza = szaArray.length;
            assertEquals(14, nnSza);     //  SZA
            assertEquals(0.0, szaArray[0], 1.E-4);
            assertEquals(6.97, szaArray[2], 1.E-4);
            assertEquals(18.51, szaArray[4], 1.E-4);
            assertEquals(29.96, szaArray[6], 1.E-4);
            assertEquals(69.9899, szaArray[13], 1.E-4);

            final double[] aziArray = lut.getDimension(3).getSequence();
            final int nnAzi = aziArray.length;
            assertEquals(19, nnAzi);     //  AZI
            assertEquals(10.0, aziArray[1], 1.E-4);
            assertEquals(130.0, aziArray[13], 1.E-4);
            assertEquals(150.0, aziArray[15], 1.E-4);

            final double[] hsfArray = lut.getDimension(2).getSequence();
            final int nnHsf = hsfArray.length;
            assertEquals(4, nnHsf);     //  HSF
            assertEquals(0.0, hsfArray[0], 1.E-3);
            assertEquals(1.0, hsfArray[1], 1.E-3);
            assertEquals(2.5, hsfArray[2], 1.E-3);
            assertEquals(8.0, hsfArray[3], 1.E-3);

            final double[] aotArray = lut.getDimension(1).getSequence();
            final int nnAot = aotArray.length;
            assertEquals(9, nnAot);     //  AOT
            assertEquals(0.1, aotArray[2], 1.E-3);
            assertEquals(0.2, aotArray[3], 1.E-3);
            assertEquals(1.5, aotArray[7], 1.E-3);

            final double[] wvlArray = lut.getDimension(0).getSequence();
            final int nnWvl = wvlArray.length;
            assertEquals(15, nnWvl);     //  AOT
            assertEquals(412.0, wvlArray[0], 1.E-3);
            assertEquals(442.0, wvlArray[1], 1.E-3);
            assertEquals(900.0, wvlArray[14], 1.E-3);

            // first values in LUT
            // iWvl=0, iAot0, iHsf0, iAzi=0, iSza=0, iVza=0, iParameters=0..4:
            double[] coord = new double[]{412.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0};
            double value = lut.getValue(coord);
            assertEquals(0.03574, value, 1.E-4);

            coord = new double[]{412.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0};
            value = lut.getValue(coord);
            assertEquals(0.74368, value, 1.E-4);

            coord = new double[]{412.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3.0};
            value = lut.getValue(coord);
            assertEquals(0.21518, value, 1.E-4);

            coord = new double[]{412.0, 0.0, 0.0, 0.0, 0.0, 0.0, 4.0};
            value = lut.getValue(coord);
            assertEquals(0.84468, value, 1.E-4);

            coord = new double[]{412.0, 0.0, 0.0, 0.0, 0.0, 0.0, 5.0};
            value = lut.getValue(coord);
            assertEquals(0.84468, value, 1.E-4);

            // iWvl=0, iAot0, iHsf0, iAzi=0, iSza=0, iVza=1, iParameters=4:
            coord = new double[]{412.0, 0.0, 0.0, 0.0, 0.0, 2.56, 5.0};
            value = lut.getValue(coord);
            assertEquals(0.84454, value, 1.E-4);

            // values somewhere inside LUT:
            coord = new double[]{
                    wvlArray[7], aotArray[2], hsfArray[1], aziArray[6], szaArray[9], vzaArray[11], parametersArray[3]
            };
            value = lut.getValue(coord);
            assertEquals(0.930663, value, 1.E-4);

            coord = new double[]{
                    wvlArray[4], aotArray[1], hsfArray[2], aziArray[14], szaArray[5], vzaArray[3], parametersArray[2]
            };
            value = lut.getValue(coord);
            assertEquals(0.060401, value, 1.E-4);

            coord = new double[]{
                    wvlArray[8], aotArray[0], hsfArray[1], aziArray[16], szaArray[3], vzaArray[10], parametersArray[1]
            };
            value = lut.getValue(coord);
            assertEquals(0.944405, value, 1.E-4);

            // last values in LUT:
            coord = new double[]{900.0, 2.0, 1013.25, 180.0, 69.989, 64.279, 3.0};
            value = lut.getValue(coord);
            assertEquals(0.192216, value, 1.E-4);

            coord = new double[]{900.0, 2.0, 1013.25, 180.0, 69.989, 64.279, 4.0};
            value = lut.getValue(coord);
            assertEquals(0.998259, value, 1.E-4);

            coord = new double[]{900.0, 2.0, 1013.25, 180.0, 69.989, 64.279, 5.0};
            value = lut.getValue(coord);
            assertEquals(0.998625, value, 1.E-4);
        }

    }

    public void testLutAotKx() throws IOException {
        try (ImageInputStream iis = MerisLuts.getAotKxLutData()) {
            // read LUT dimensions and values
            float[] vza = MerisLuts.readDimension(iis);
            int nVza = vza.length;
            float[] sza = MerisLuts.readDimension(iis);
            int nSza = sza.length;
            float[] azi = MerisLuts.readDimension(iis);
            int nAzi = azi.length;
            float[] hsf = MerisLuts.readDimension(iis);
            int nHsf = hsf.length;
            float[] aot = MerisLuts.readDimension(iis);
            int nAot = aot.length;

            float[] kx = new float[]{1.0f, 2.0f};
            int nKx = kx.length;

            float[] wvl = {
                    412.0f, 442.0f, 490.0f, 510.0f, 560.0f,
                    620.0f, 665.0f, 681.0f, 708.0f, 753.0f,
                    760.0f, 778.0f, 865.0f, 885.0f, 900.0f
            };
            final int nWvl = wvl.length;

            float[] parms = new float[nKx * nVza * nSza * nAzi * nHsf * nAot * nWvl];
            iis.readFully(parms, 0, parms.length);

            LookupTable lut = new LookupTable(parms, wvl, aot, hsf, azi, sza, vza, kx);

            assertNotNull(lut);

            assertEquals(7, lut.getDimensionCount());

            final double[] kxArray = lut.getDimension(6).getSequence();
            final int nnKx = kxArray.length;
            assertEquals(2, nnKx);     //  Parameters
            assertEquals(1.0, kxArray[0], 1.E-4);
            assertEquals(2.0, kxArray[1], 1.E-4);

            final double[] vzaArray = lut.getDimension(5).getSequence();
            final int nnVza = vzaArray.length;
            assertEquals(13, nnVza);     //  VZA
            assertEquals(12.76, vzaArray[3], 1.E-4);
            assertEquals(24.24, vzaArray[5], 1.E-4);
            assertEquals(35.68, vzaArray[7], 1.E-4);

            final double[] szaArray = lut.getDimension(4).getSequence();
            final int nnSza = szaArray.length;
            assertEquals(14, nnSza);     //  SZA
            assertEquals(6.97, szaArray[2], 1.E-4);
            assertEquals(18.51, szaArray[4], 1.E-4);
            assertEquals(29.96, szaArray[6], 1.E-4);

            final double[] aziArray = lut.getDimension(3).getSequence();
            final int nnAzi = aziArray.length;
            assertEquals(19, nnAzi);     //  AZI
            assertEquals(10.0, aziArray[1], 1.E-4);
            assertEquals(130.0, aziArray[13], 1.E-4);
            assertEquals(150.0, aziArray[15], 1.E-4);

            final double[] hsfArray = lut.getDimension(2).getSequence();
            final int nnHsf = hsfArray.length;
            assertEquals(4, nnHsf);     //  HSF
            assertEquals(1.0, hsfArray[1], 1.E-3);
            assertEquals(2.5, hsfArray[2], 1.E-3);
            assertEquals(7.998, hsfArray[3], 1.E-3);

            final double[] aotArray = lut.getDimension(1).getSequence();
            final int nnAot = aotArray.length;
            assertEquals(9, nnAot);     //  AOT
            assertEquals(0.1, aotArray[2], 1.E-3);
            assertEquals(0.2, aotArray[3], 1.E-3);
            assertEquals(1.5, aotArray[7], 1.E-3);

            final double[] wvlArray = lut.getDimension(0).getSequence();
            final int nnWvl = wvlArray.length;
            assertEquals(15, nnWvl);     //  AOT
            assertEquals(412.0, wvlArray[0], 1.E-3);
            assertEquals(442.0, wvlArray[1], 1.E-3);
            assertEquals(900.0, wvlArray[14], 1.E-3);

            // first values in LUT
            // iWvl=0, iAot0, iHsf0, iAzi=0, iSza=0, iVza=0..1, iKx=0..1:
            double[] coord = new double[]{412.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0};
            double value = lut.getValue(coord);
            assertEquals(-0.06027, value, 1.E-4);

            coord = new double[]{412.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0};
            value = lut.getValue(coord);
            assertEquals(0.056184, value, 1.E-4);

            coord = new double[]{412.0, 0.0, 0.0, 0.0, 0.0, 2.56, 1.0};
            value = lut.getValue(coord);
            assertEquals(-0.059273, value, 1.E-4);

            coord = new double[]{412.0, 0.0, 0.0, 0.0, 0.0, 2.56, 2.0};
            value = lut.getValue(coord);
            assertEquals(0.055945, value, 1.E-4);

            // values somewhere inside LUT:
            coord = new double[]{wvlArray[7], aotArray[2], hsfArray[1], aziArray[6], szaArray[9], vzaArray[11], kxArray[0]};
            value = lut.getValue(coord);
            assertEquals(-0.082877, value, 1.E-4);

            coord = new double[]{wvlArray[4], aotArray[1], hsfArray[2], aziArray[14], szaArray[5], vzaArray[3], kxArray[1]};
            value = lut.getValue(coord);
            assertEquals(-0.3205, value, 1.E-4);

            coord = new double[]{
                    wvlArray[8], aotArray[0], hsfArray[1], aziArray[16], szaArray[3], vzaArray[10], kxArray[0]
            };
            value = lut.getValue(coord);
            assertEquals(-0.01571, value, 1.E-4);

            // last values in LUT:
            coord = new double[]{900.0, 2.0, 1013.25, 180.0, 69.989, 64.279, 1.0};
            value = lut.getValue(coord);
            assertEquals(-0.197412, value, 1.E-4);

            coord = new double[]{900.0, 2.0, 1013.25, 180.0, 69.989, 64.279, 2.0};
            value = lut.getValue(coord);
            assertEquals(0.007395, value, 1.E-4);
        }
    }

    public void testLutInterpolation1D() {
        final double[] dimension = new double[]{0, 1, 2, 3, 4};
        final double[] values = new double[]{0, 2, 5, 10, 22};

        final LookupTable lut = new LookupTable(values, dimension);
        assertEquals(1, lut.getDimensionCount());

        assertEquals(0.0, lut.getDimension(0).getMin(), 0.0);
        assertEquals(4.0, lut.getDimension(0).getMax(), 0.0);

        assertEquals(0.0, lut.getValue(0.0), 0.0);
        assertEquals(2.0, lut.getValue(1.0), 0.0);
        assertEquals(5.0, lut.getValue(2.0), 0.0);
        assertEquals(7.5, lut.getValue(2.5), 0.0);
        assertEquals(0.2469, lut.getValue(0.12345), 0.0);
    }


}
