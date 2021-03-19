package org.esa.s3tbx.c2rcc.msi;

import static org.esa.s3tbx.c2rcc.msi.C2rccMsiAlgorithm.IDX_iop_rw;
import static org.esa.s3tbx.c2rcc.msi.C2rccMsiAlgorithm.IDX_iop_unciop;
import static org.esa.s3tbx.c2rcc.msi.C2rccMsiAlgorithm.IDX_iop_uncsumiop_unckd;
import static org.esa.s3tbx.c2rcc.msi.C2rccMsiAlgorithm.IDX_rtosa_aann;
import static org.esa.s3tbx.c2rcc.msi.C2rccMsiAlgorithm.IDX_rtosa_rpath;
import static org.esa.s3tbx.c2rcc.msi.C2rccMsiAlgorithm.IDX_rtosa_rw;
import static org.esa.s3tbx.c2rcc.msi.C2rccMsiAlgorithm.IDX_rtosa_trans;
import static org.esa.s3tbx.c2rcc.msi.C2rccMsiAlgorithm.IDX_rw_iop;
import static org.esa.s3tbx.c2rcc.msi.C2rccMsiAlgorithm.IDX_rw_kd;
import static org.esa.s3tbx.c2rcc.msi.C2rccMsiAlgorithm.IDX_rw_rwnorm;

/**
 * 
 * Holds the paths to the neural net resources.
 */
public final class NnPaths {

    static String[] getStandard() {
        String[] standardNets = new String[10];
        standardNets[IDX_iop_rw] = "msi/std_s2_20160502/iop_rw/17x97x47_125.5.net";
        standardNets[IDX_iop_unciop] = "msi/std_s2_20160502/iop_unciop/17x77x37_11486.7.net";
        standardNets[IDX_iop_uncsumiop_unckd] = "msi/std_s2_20160502/iop_uncsumiop_unckd/17x77x37_9113.1.net";
        standardNets[IDX_rtosa_aann] = "msi/std_s2_20160502/rtosa_aann/31x7x31_78.0.net";
        standardNets[IDX_rtosa_rpath] = "msi/std_s2_20160502/rtosa_rpath/31x77x57x37_1564.4.net";
        standardNets[IDX_rtosa_rw] = "msi/std_s2_20160502/rtosa_rw/33x73x53x33_291140.4.net";
        standardNets[IDX_rtosa_trans] = "msi/std_s2_20160502/rtosa_trans/31x77x57x37_37537.6.net";
        standardNets[IDX_rw_iop] = "msi/std_s2_20160502/rw_iop/97x77x37_17515.9.net";
        standardNets[IDX_rw_kd] = "msi/std_s2_20160502/rw_kd/97x77x7_306.8.net";
        standardNets[IDX_rw_rwnorm] = "msi/std_s2_20160502/rw_rwnorm/27x7x27_28.0.net";
        return standardNets;
    }

    static String[] getExtreme() {
        String[] extremeNets = new String[10];
        extremeNets[IDX_iop_rw] = "msi/ext_s2_elbetsm_20170320/iop_rw/77x77x77_28.3.net";
        extremeNets[IDX_iop_unciop] = "msi/ext_s2_elbetsm_20170320/iop_unciop/17x77x37_11486.7.net";
        extremeNets[IDX_iop_uncsumiop_unckd] = "msi/ext_s2_elbetsm_20170320/iop_uncsumiop_unckd/17x77x37_9113.1.net";
        extremeNets[IDX_rtosa_aann] = "msi/ext_s2_elbetsm_20170320/rtosa_aann/31x7x31_7.2.net";
        extremeNets[IDX_rtosa_rpath] = "msi/ext_s2_elbetsm_20170320/rtosa_rpath/37x37x37_175.7.net";
        extremeNets[IDX_rtosa_rw] = "msi/ext_s2_elbetsm_20170320/rtosa_rw/77x77x77x77_10688.3.net";
        extremeNets[IDX_rtosa_trans] = "msi/ext_s2_elbetsm_20170320/rtosa_trans/77x77x77_7809.2.net";
        extremeNets[IDX_rw_iop] = "msi/ext_s2_elbetsm_20170320/rw_iop/77x77x77_785.6.net";
        extremeNets[IDX_rw_kd] = "msi/ext_s2_elbetsm_20170320/rw_kd/77x77x77_61.6.net";
        extremeNets[IDX_rw_rwnorm] = "msi/ext_s2_elbetsm_20170320/rw_rwnorm/27x7x27_28.0.net";
        return extremeNets;
    }

    static String[] getComplex() {
        String[] complex = new String[10];
        complex[IDX_iop_rw] = "msi/complex_20200321/iop_rw/55x55x55_2775.5.net";
        complex[IDX_iop_unciop] = "msi/complex_20200321/iop_unciop/55x55x55_23294.6.net";
        complex[IDX_iop_uncsumiop_unckd] = "msi/complex_20200321/iop_uncsumiop_unckd/55x55x55_23199.4.net";
        complex[IDX_rtosa_aann] = "msi/complex_20200321/rtosa_aann/55x7x55_51.2.net";
        complex[IDX_rtosa_rpath] = "msi/complex_20200321/rtosa_rpath/55x55x55_1247.8.net";
        complex[IDX_rtosa_rw] = "msi/complex_20200321/rtosa_rw/99x99x99_9210.2.net";
        complex[IDX_rtosa_trans] = "msi/complex_20200321/rtosa_trans/55x55x55_107567.4.net";
        complex[IDX_rw_iop] = "msi/complex_20200321/rw_iop/39x39x39_1935.5.net";
        complex[IDX_rw_kd] = "msi/complex_20200321/rw_kd/55x55x55_315.5.net";
        complex[IDX_rw_rwnorm] = "msi/complex_20200321/rw_rwnorm/27x7x27_28.0.net";
        return complex;
    }
}
