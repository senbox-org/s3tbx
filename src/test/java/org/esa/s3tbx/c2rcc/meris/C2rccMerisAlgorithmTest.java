package org.esa.s3tbx.c2rcc.meris;

import static org.esa.s3tbx.c2rcc.meris.C2rccMerisAlgorithm.IDX_iop_rw;
import static org.esa.s3tbx.c2rcc.meris.C2rccMerisAlgorithm.IDX_iop_unciop;
import static org.esa.s3tbx.c2rcc.meris.C2rccMerisAlgorithm.IDX_iop_uncsumiop_unckd;
import static org.esa.s3tbx.c2rcc.meris.C2rccMerisAlgorithm.IDX_rtosa_aann;
import static org.esa.s3tbx.c2rcc.meris.C2rccMerisAlgorithm.IDX_rtosa_rpath;
import static org.esa.s3tbx.c2rcc.meris.C2rccMerisAlgorithm.IDX_rtosa_rw;
import static org.esa.s3tbx.c2rcc.meris.C2rccMerisAlgorithm.IDX_rtosa_trans;
import static org.esa.s3tbx.c2rcc.meris.C2rccMerisAlgorithm.IDX_rw_iop;
import static org.esa.s3tbx.c2rcc.meris.C2rccMerisAlgorithm.IDX_rw_kd;
import static org.esa.s3tbx.c2rcc.meris.C2rccMerisAlgorithm.IDX_rw_rwnorm;
import static org.junit.Assert.*;

import org.junit.*;

import java.util.Arrays;


public class C2rccMerisAlgorithmTest {
    @Test
    public void testIt() throws Exception {
        final String[] paths = new String[10];
        paths[IDX_rtosa_aann] = "test_meris/rtoa_aaNN7/31x7x31_555.6.net";
        paths[IDX_rtosa_rw] = "test_meris/rtoa_rw_nn3/33x73x53x33_470639.6.net";
        paths[IDX_rw_iop] = "test_meris/inv_meris_logrw_logiop_20140318_noise_p5_fl/97x77x37_11671.0.net";
        paths[IDX_iop_rw] = "test_meris/for_meris_logrw_logiop_20140318_p5_fl/17x97x47_335.3.net";
        paths[IDX_rw_kd] = "test_meris/inv_meris_kd/97x77x7_232.4.net";
        paths[IDX_iop_unciop] = "test_meris/uncertain_log_abs_biasc_iop/17x77x37_11486.7.net";
        paths[IDX_iop_uncsumiop_unckd] = "test_meris/uncertain_log_abs_tot_kd/17x77x37_9113.1.net";
        paths[IDX_rw_rwnorm] = "test_meris/norma_net_20150307/37x57x17_76.8.net";
        paths[IDX_rtosa_trans] = "test_meris/rtoa_trans_nn2/31x77x57x37_37087.4.net";
        paths[IDX_rtosa_rpath] = "test_meris/rtoa_rpath_nn2/31x77x57x37_2388.6.net";
        C2rccMerisAlgorithm algo = new C2rccMerisAlgorithm(paths, true);

        algo.setOutputRhow(true);
        C2rccMerisAlgorithm.Result result1 = algo.processPixel(
                250, 575, 7.9456024, 54.150196,
                new double[]{55.086716, 49.46522, 38.112446, 33.45525, 23.108776, 14.337405, 11.306171, 10.365329, 8.529731, 6.4291587, 2.262602, 5.485246, 3.394396, 3.1312065, 2.291696},
                C2rccMerisAlgorithm.DEFAULT_SOLAR_FLUX,
                64.19979, 158.32169, 24.818445, 102.8721, -34.242188, true, 1019.4312, 277.9019
        );
        assertNotNull(result1);
        assertEquals(12, result1.rwa.length);
        assertEquals(5, result1.iops_nn.length);


        double[] rwExpected = new double[]{0.053810319820504535, 0.04413918181778166, 0.035504458785884443,
                0.023936706823420303, 0.01350643050490403, 0.0029522696043451356,
                0.0016961727412098864, 0.0017174658465812802, 7.666880266252653E-4,
                2.0007023897925736E-4, 2.0668168950962044E-4, 7.45657675019802E-5};
        assertArrayEquals(rwExpected, result1.rwa, 1e-4);

        double[] iopsExpected1 = new double[]{
                0.012507978418952223, 0.0021061919389812633, 9.761536702823946E-5,
                0.31943759388761156, 0.028129863341225206};
        assertArrayEquals(iopsExpected1, result1.iops_nn, 1e-2);

        assertEquals(-2147450879, result1.flags);

    }
}