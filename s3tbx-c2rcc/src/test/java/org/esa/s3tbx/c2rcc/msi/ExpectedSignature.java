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

/**
 * @author Marco Peters
 */
final class ExpectedSignature {
    static final String[] EXPECTED_RHOW_BANDS = {
            "rhow_B1", "rhow_B2", "rhow_B3", "rhow_B4", "rhow_B5",
            "rhow_B6", "rhow_B7", "rhow_B8A"};
    static final String[] EXPECTED_RRS_BANDS = {
            "rrs_B1", "rrs_B2", "rrs_B3", "rrs_B4", "rrs_B5",
            "rrs_B6", "rrs_B7", "rrs_B8A"};
    static final String[] EXPECTED_NORM_REFLEC_BANDS = {
            "rhown_B1", "rhown_B2", "rhown_B3", "rhown_B4", "rhown_B5",
            "rhown_B6"};
    static final String EXPECTED_IOP_APIG = "iop_apig";
    static final String EXPECTED_IOP_ADET = "iop_adet";
    static final String EXPECTED_IOP_AGELB = "iop_agelb";
    static final String EXPECTED_IOP_BPART = "iop_bpart";
    static final String EXPECTED_IOP_BWIT = "iop_bwit";
    static final String EXPECTED_IOP_ADG = "iop_adg";
    static final String EXPECTED_IOP_ATOT = "iop_atot";
    static final String EXPECTED_IOP_BTOT = "iop_btot";
    static final String EXPECTED_CONC_CHL = "conc_chl";
    static final String EXPECTED_CONC_TSM = "conc_tsm";
    static final String[] EXPECTED_KD_BANDS = {"kd489", "kdmin", "kd_z90max"};
    static final String EXPECTED_OOS_RTOSA = "oos_rtosa";
    static final String EXPECTED_OOS_RHOW = "oos_rhow";
    static final String EXPECTED_OOS_RRS = "oos_rrs";
    static final String[] EXPECTED_IOP_UNC_BANDS = {
            "unc_apig", "unc_adet", "unc_agelb", "unc_bpart",
            "unc_bwit", "unc_adg", "unc_atot", "unc_btot"};
    static final String[] EXPECTED_KD_UNC_BANDS = {"unc_kd489", "unc_kdmin"};
    static final String[] EXPECTED_RTOSA_GC_BANDS = {
            "rtosa_gc_B1", "rtosa_gc_B2", "rtosa_gc_B3", "rtosa_gc_B4", "rtosa_gc_B5",
            "rtosa_gc_B6", "rtosa_gc_B7", "rtosa_gc_B8A"};
    static final String[] EXPECTED_RTOSA_GCAANN_BANDS = {
            "rtosagc_aann_B1", "rtosagc_aann_B2", "rtosagc_aann_B3", "rtosagc_aann_B4", "rtosagc_aann_B5",
            "rtosagc_aann_B6", "rtosagc_aann_B7", "rtosagc_aann_B8A"};
    static final String[] EXPECTED_RTOA_BANDS = {
            "rtoa_B1", "rtoa_B2", "rtoa_B3", "rtoa_B4", "rtoa_B5",
            "rtoa_B6", "rtoa_B7", "rtoa_B8", "rtoa_B8A", "rtoa_B9",
            "rtoa_B10", "rtoa_B11", "rtoa_B12"};
    static final String[] EXPECTED_RPATH_BANDS = {
            "rpath_B1", "rpath_B2", "rpath_B3", "rpath_B4", "rpath_B5",
            "rpath_B6", "rpath_B7", "rpath_B8A"};
    static final String[] EXPECTED_TDOWN_BANDS = {
            "tdown_B1", "tdown_B2", "tdown_B3", "tdown_B4", "tdown_B5",
            "tdown_B6", "tdown_B7", "tdown_B8A"};
    static final String[] EXPECTED_TUP_BANDS = {
            "tup_B1", "tup_B2", "tup_B3", "tup_B4", "tup_B5",
            "tup_B6", "tup_B7", "tup_B8A"};
    static final String EXPECTED_C2RCC_FLAGS = "c2rcc_flags";
    static final String EXPECTED_PRODUCT_TYPE = "C2RCC_S2-MSI";

    private ExpectedSignature() {

    }
}
