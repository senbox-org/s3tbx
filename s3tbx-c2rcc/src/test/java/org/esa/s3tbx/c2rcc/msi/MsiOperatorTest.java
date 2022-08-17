/*
 * Copyright (c) 2022.  Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.s3tbx.c2rcc.msi;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.gpf.Operator;
import org.junit.Test;

import java.io.IOException;

import static org.esa.s3tbx.c2rcc.msi.ExpectedSignature.EXPECTED_CONC_CHL;
import static org.esa.s3tbx.c2rcc.msi.ExpectedSignature.EXPECTED_CONC_TSM;
import static org.esa.s3tbx.c2rcc.msi.ExpectedSignature.EXPECTED_IOP_APIG;
import static org.esa.s3tbx.c2rcc.msi.ExpectedSignature.EXPECTED_IOP_BWIT;
import static org.esa.s3tbx.c2rcc.msi.ExpectedSignature.EXPECTED_RHOW_BANDS;
import static org.junit.Assert.assertEquals;


/**
 * @author Marco Peters
 */
public class MsiOperatorTest {

    @Test
    public void testWithDefaults() throws IOException {
        final Operator operator = initOperator();
        operator.setSourceProduct(MsiTestProduct.createInput());
        final Product target = operator.getTargetProduct();

        assertEquals(14.441857f, getSampleFloat(target, EXPECTED_CONC_CHL, 0, 0), 1.0e-6);
        assertEquals(5.062996f, getSampleFloat(target, EXPECTED_CONC_TSM, 0, 0), 1.0e-6);
        assertEquals(0.6976819f, getSampleFloat(target, EXPECTED_IOP_APIG, 0, 0), 1.0e-6);
        assertEquals(4.0867243f, getSampleFloat(target, EXPECTED_IOP_BWIT, 0, 0), 1.0e-6);

        float[] EXPECTED_RHOW_VALUES = new float[]{0.003311f, 0.004983f, 0.010308f, 0.007630f, 0.006501f, 0.002323f, 0.002366f, 0.000986f};
        for (int i = 0; i < EXPECTED_RHOW_BANDS.length; i++) {
            assertEquals(EXPECTED_RHOW_VALUES[i], getSampleFloat(target, EXPECTED_RHOW_BANDS[i], 0, 0), 1.0e-6);
        }
    }

    @Test
    public void testWithInternalECMWFAuxdata_ButAuxDataNotAvailable() throws IOException {
        final Operator operator = initOperator();
        operator.setParameter("useEcmwfAuxData", true);
        final Product input = MsiTestProduct.createInput();
        input.removeBand(input.getBand("tco3"));
        input.removeBand(input.getBand("msl"));
        input.removeBand(input.getBand("tcwv"));
        operator.setSourceProduct(input);
        final Product target = operator.getTargetProduct();

        // values should be the same as in testWithDefaults
        assertEquals(14.441857f, getSampleFloat(target, EXPECTED_CONC_CHL, 0, 0), 1.0e-6);
        assertEquals(5.062996f, getSampleFloat(target, EXPECTED_CONC_TSM, 0, 0), 1.0e-6);
        assertEquals(0.6976819f, getSampleFloat(target, EXPECTED_IOP_APIG, 0, 0), 1.0e-6);
        assertEquals(4.0867243f, getSampleFloat(target, EXPECTED_IOP_BWIT, 0, 0), 1.0e-6);

        float[] EXPECTED_RHOW_VALUES = new float[]{0.003311f, 0.004983f, 0.010308f, 0.007630f, 0.006501f, 0.002323f, 0.002366f, 0.000986f};
        for (int i = 0; i < EXPECTED_RHOW_BANDS.length; i++) {
            assertEquals(EXPECTED_RHOW_VALUES[i], getSampleFloat(target, EXPECTED_RHOW_BANDS[i], 0, 0), 1.0e-6);
        }

    }

    @Test
    public void testWithInternalECMWFAuxdata() throws IOException {
        final Operator operator = initOperator();
        operator.setParameter("useEcmwfAuxData", true);
        final Product input = MsiTestProduct.createInput();
        operator.setSourceProduct(input);
        final Product target = operator.getTargetProduct();

        // values should be the same as in testWithDefaults
        assertEquals(14.657781f, getSampleFloat(target, EXPECTED_CONC_CHL, 0, 0), 1.0e-6);
        assertEquals(4.911448f, getSampleFloat(target, EXPECTED_CONC_TSM, 0, 0), 1.0e-6);
        assertEquals(0.7077091f, getSampleFloat(target, EXPECTED_IOP_APIG, 0, 0), 1.0e-6);
        assertEquals(4.0339804f, getSampleFloat(target, EXPECTED_IOP_BWIT, 0, 0), 1.0e-6);

        float[] EXPECTED_RHOW_VALUES = new float[]{0.003032f, 0.004516f, 0.009282f, 0.007060f, 0.006061f, 0.002194f, 0.002237f, 0.000937f};

        for (int i = 0; i < EXPECTED_RHOW_BANDS.length; i++) {
            assertEquals(EXPECTED_RHOW_VALUES[i], getSampleFloat(target, EXPECTED_RHOW_BANDS[i], 0, 0), 1.0e-6);
        }
    }

    @Test
    public void testWithInternalECMWFAuxdata_WithIssue_SIITBX_497() throws IOException {
        final Operator operator = initOperator();
        operator.setParameter("useEcmwfAuxData", true);
        final Product input = MsiTestProduct.createInput();
        final Band tco3 = input.getBand("tco3");
        tco3.setName("temp");
        final Band tcwv = input.getBand("tcwv");
        tcwv.setName("tco3");
        tco3.setName("tcwv");

        operator.setSourceProduct(input);
        final Product target = operator.getTargetProduct();

        // values should be the same as in testWithInternalECMWFAuxdata, because the swap in data is considered and corrected
        assertEquals(14.657781f, getSampleFloat(target, EXPECTED_CONC_CHL, 0, 0), 1.0e-6);
        assertEquals(4.911448f, getSampleFloat(target, EXPECTED_CONC_TSM, 0, 0), 1.0e-6);
        assertEquals(0.7077091f, getSampleFloat(target, EXPECTED_IOP_APIG, 0, 0), 1.0e-6);
        assertEquals(4.0339804f, getSampleFloat(target, EXPECTED_IOP_BWIT, 0, 0), 1.0e-6);

        float[] EXPECTED_RHOW_VALUES = new float[]{0.003032f, 0.004516f, 0.009282f, 0.007060f, 0.006061f, 0.002194f, 0.002237f, 0.000937f};

        for (int i = 0; i < EXPECTED_RHOW_BANDS.length; i++) {
            assertEquals(EXPECTED_RHOW_VALUES[i], getSampleFloat(target, EXPECTED_RHOW_BANDS[i], 0, 0), 1.0e-6);
        }
    }

    @SuppressWarnings("SameParameterValue")
    private static float getSampleFloat(Product target, String rasterName, int x, int y) throws IOException {
        final RasterDataNode raster = target.getRasterDataNode(rasterName);
        raster.readRasterDataFully(ProgressMonitor.NULL);
        return raster.getSampleFloat(x, y);
    }

    private Operator initOperator() {
        return new C2rccMsiOperator.Spi().createOperator();
    }
}