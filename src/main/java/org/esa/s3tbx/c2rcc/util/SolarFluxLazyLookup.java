package org.esa.s3tbx.c2rcc.util;

public class SolarFluxLazyLookup {

    final double[] solFlux;
    private double[][][] correctedSolFluxForADay;

    public SolarFluxLazyLookup(double[] solFlux) {
        this.solFlux = solFlux;
        correctedSolFluxForADay = new double[2][366][];
    }

    public double[] getCorrectedFluxFor(int doy, int year) {
        final int yearDays = SolarFluxCorrectionFactorCalculator.getNumDaysInTheYear(year);
        int yearIndex = yearDays - 365; // can be 0 if it is a 365 days year or 1 if it is a leap year with 366 days
        double[] corrected = correctedSolFluxForADay[yearIndex][doy];
        if (corrected != null) {
            return corrected;
        }
        final double correctionFactor = SolarFluxCorrectionFactorCalculator.getDayCorrectionFactorFor(doy, year);
        corrected = new double[solFlux.length];
        for (int i = 0; i < solFlux.length; i++) {
            corrected[i] = solFlux[i] * correctionFactor;
        }
        correctedSolFluxForADay[yearIndex][doy] = corrected;
        return corrected;
    }
}
