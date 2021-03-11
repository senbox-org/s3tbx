package org.esa.s3tbx.mphchl;

import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.pointop.Sample;
import org.esa.snap.core.gpf.pointop.SourceSampleConfigurer;
import org.esa.snap.core.gpf.pointop.WritableSample;

@OperatorMetadata(alias = "MphChlMeris-beta",
        version = "1.0",
        internal = true,
        authors = "Mark William Matthews, Daniel Odermatt, Tom Block, Olaf Danne",
        copyright = "(c) 2013, 2014, 2017 by Brockmann Consult",
        description = "Computes maximum peak height of chlorophyll for MERIS. Implements MERIS-specific parts.")
public class MphChlMerisBetaOp extends MphChlBasisBetaOp {

    private static final int REFL_6_IDX = 0;
    private static final int REFL_7_IDX = 1;
    private static final int REFL_8_IDX = 2;
    private static final int REFL_9_IDX = 3;
    private static final int REFL_10_IDX = 4;
    private static final int REFL_14_IDX = 5;
    private static final int REFL_5_IDX = 6;

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {

        if (!isSampleValid(x, y)) {
            MphChlUtils.setToInvalid(targetSamples, exportMph, exportAddBands);
            return;
        }

        final double r_6 = sourceSamples[REFL_6_IDX].getDouble();
        final double r_7 = sourceSamples[REFL_7_IDX].getDouble();
        final double r_8 = sourceSamples[REFL_8_IDX].getDouble();
        final double r_9 = sourceSamples[REFL_9_IDX].getDouble();
        final double r_10 = sourceSamples[REFL_10_IDX].getDouble();
        final double r_14 = sourceSamples[REFL_14_IDX].getDouble();
        final double r_5 = sourceSamples[REFL_5_IDX].getDouble();

        double maxBrr_0 = r_8;
        double maxLambda_0 = sensorWvls[8];     // 681
        if (r_9 > maxBrr_0) {
            maxBrr_0 = r_9;
            maxLambda_0 = sensorWvls[9];        // 709
        }

        double maxBrr_1 = maxBrr_0;
        double maxLambda_1 = maxLambda_0;
        if (r_10 > maxBrr_1) {
            maxBrr_1 = r_10;
            maxLambda_1 = sensorWvls[10];      // 753
        }

        final double ndvi = (r_14 - r_7) / (r_14 + r_7);
        final double SIPF_peak = r_7 - r_6 - ((r_8 - r_6) * ratioP);
        final double SICF_peak = r_8 - r_7 - ((r_9 - r_7) * ratioC);
        final double BAIR_peak = r_9 - r_7 - ((r_14 - r_7) * ratioB);

        double mph_0 = MphChlUtils.computeMph(maxBrr_0, r_7, r_14, maxLambda_0,
                sensorWvls[7],         // 664
                sensorWvls[14]);     // 885
        double mph_1 = MphChlUtils.computeMph(maxBrr_1, r_7, r_14, maxLambda_1,
                sensorWvls[7],         // 664
                sensorWvls[14]);     // 885

        boolean floating_flag = false;
        boolean adj_flag = false;
        boolean cyano_flag = false;

        int immersed_cyano = 0;
        int floating_cyano = 0;
        int floating_vegetation = 0;

        boolean calculatePolynomial = false;
        boolean calculateExponential = false;

        if (maxLambda_1 != sensorWvls[10]) {       // 753
            if (MphChlUtils.isCyano(SICF_peak, SIPF_peak, BAIR_peak)) {
                cyano_flag = true;
                calculateExponential = true;
            } else {
                calculatePolynomial = true;
            }
        } else {
            if (mph_1 >= 0.02 || ndvi >= 0.2) {
                floating_flag = true;
                adj_flag = false;
                if (MphChlUtils.isCyano(SICF_peak, SIPF_peak)) {
                    cyano_flag = true;
                    calculateExponential = true;
                } else {
                    cyano_flag = false;
                    floating_vegetation = 1;
                }
            }
            if (mph_1 < 0.02 && ndvi < 0.2) {
                floating_flag = false;
                adj_flag = true;
                cyano_flag = false;
                calculatePolynomial = true;
            }
        }

        double mph_chl = Double.NaN;
        double mph_matthews = Double.NaN;
        double chl_pitarch = Double.NaN;
        double chl_pci_pitarch = Double.NaN;
        double pci = Double.NaN;
        if (calculatePolynomial) {
            mph_chl = MphChlUtils.computeChlPolynomial(mph_0);
            mph_matthews = MphChlUtils.computeChlMatthewsPolynomial(mph_0);
        }

        if (calculateExponential) {
            mph_chl = MphChlUtils.computeChlExponential(mph_1);
            mph_matthews = MphChlUtils.computeChlExponential(mph_1);
            if (mph_chl < chlThreshForFloatFlag) {
                immersed_cyano = 1;
            } else {
                floating_flag = true;
                floating_cyano = 1;
            }
        }

        chl_pitarch = MphChlUtils.computeChlPitarch(mph_0);
        pci = MphChlUtils.computePci(r_5, r_6, r_7,
                sensorWvls[5], //560
                sensorWvls[6], //620
                sensorWvls[7]//664
        );
        chl_pci_pitarch = MphChlUtils.computeChlPciPitarch(mph_0, pci);

        if (mph_chl > cyanoMaxValue) {
            mph_chl = cyanoMaxValue;
        }

        targetSamples[0].set(mph_chl);
        targetSamples[1].set(MphChlUtils.encodeFlags(cyano_flag, floating_flag, adj_flag));
        targetSamples[2].set(immersed_cyano);
        targetSamples[3].set(floating_cyano);
        targetSamples[4].set(floating_vegetation);
        if (exportMph) {
            targetSamples[5].set(mph_0);
        }
        if (exportAddBands) {
            targetSamples[6].set(mph_matthews);
            targetSamples[7].set(chl_pitarch);
            targetSamples[8].set(chl_pci_pitarch);
            targetSamples[9].set(pci);
        }
    }

    @Override
    protected void configureSourceSamples(SourceSampleConfigurer sampleConfigurer) throws OperatorException {
        sampleConfigurer.defineSample(0, "rBRR_06");
        sampleConfigurer.defineSample(1, "rBRR_07");
        sampleConfigurer.defineSample(2, "rBRR_08");
        sampleConfigurer.defineSample(3, "rBRR_09");
        sampleConfigurer.defineSample(4, "rBRR_10");
        sampleConfigurer.defineSample(5, "rBRR_14");
        sampleConfigurer.defineSample(6, "rBRR_05");
    }

    @Override
    protected void prepareInputs() throws OperatorException {
        super.prepareInputs();

        sensorWvls = MphChlConstants.MERIS_WAVELENGTHS;

        ratioP = (sensorWvls[7] - sensorWvls[6]) / (sensorWvls[8] - sensorWvls[6]);    // (664 - 619)/(681 - 619)
        ratioC = (sensorWvls[8] - sensorWvls[7]) / (sensorWvls[9] - sensorWvls[7]);   // (681 - 664)/(709 - 664)
        ratioB = (sensorWvls[9] - sensorWvls[7]) / (sensorWvls[14] - sensorWvls[7]);  // (709 - 664)/(885 - 664)
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(MphChlMerisBetaOp.class);
        }
    }
}
