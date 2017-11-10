package org.esa.s3tbx.c2rcc.landsat;

class SubsetInfo {

    final int center_x;
    int subsampling_x = 1;
    int offset_x = 0;

    SubsetInfo(int center_x) {
        this.center_x = center_x;
    }
}
