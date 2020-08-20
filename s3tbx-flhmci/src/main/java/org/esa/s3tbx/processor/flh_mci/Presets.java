package org.esa.s3tbx.processor.flh_mci;

@SuppressWarnings("unused")
public enum Presets {
    NONE("None", "", "", "", "", "", ""),
    MERIS_L1B_MCI("MERIS L1b MCI", "radiance_8", "radiance_10", "radiance_9", "MCI", "MCI_slope",
                  "NOT l1_flags.LAND_OCEAN AND NOT l1_flags.BRIGHT AND NOT l1_flags.INVALID"),
    MERIS_L2_FLH("MERIS L2 FLH", "reflec_7", "reflec_9", "reflec_8", "FLH", "FLH_slope",
                 "l2_flags.WATER"),
    MERIS_L2_MCI("MERIS L2 MCI", "reflec_8", "reflec_10", "reflec_9", "MCI", "MCI_slope",
                 "l2_flags.WATER"),
    OLCI_L1B_MCI("OLCI L1b MCI", "Oa10_radiance", "Oa12_radiance", "Oa11_radiance", "MCI", "MCI_slope",
                 "(!quality_flags.land || quality_flags.fresh_inland_water) && ! quality_flags.invalid"),
    OLCI_L2_FLH("OLCI L2 FLH", "Oa08_reflectance", "Oa11_reflectance", "Oa10_reflectance", "FLH", "FLH_slope",
                "WQSF_lsb.WATER || WQSF_lsb.INLAND_WATER"),
    OLCI_L2_MCI("OLCI L2 MCI", "Oa10_reflectance", "Oa12_reflectance", "Oa11_reflectance", "MCI", "MCI_slope",
                "WQSF_lsb.WATER || WQSF_lsb.INLAND_WATER");

    public final String label;
    public final String lowerBaselineBandName;
    public final String upperBaselineBandName;
    public final String signalBandName;
    public final String lineHeightBandName;
    public final String slopeBandName;
    public final String maskExpression;

    Presets(String label, String upperBaselineBandName, String lowerBaselineBandName,
            String signalBandName, String lineHeightBandName, String slopeBandName, String maskExpression) {
        this.label = label;
        this.upperBaselineBandName = upperBaselineBandName;
        this.lowerBaselineBandName = lowerBaselineBandName;
        this.signalBandName = signalBandName;
        this.lineHeightBandName = lineHeightBandName;
        this.slopeBandName = slopeBandName;
        this.maskExpression = maskExpression;
    }

    @Override
    public String toString() {
        return label;
    }

    public String getLowerBaselineBandName() {
        return lowerBaselineBandName;
    }

    public String getUpperBaselineBandName() {
        return upperBaselineBandName;
    }

    public String getSignalBandName() {
        return signalBandName;
    }

    public String getLineHeightBandName() {
        return lineHeightBandName;
    }

    public String getSlopeBandName() {
        return slopeBandName;
    }

    public String getMaskExpression() {
        return maskExpression;
    }
}
