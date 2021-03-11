package org.esa.s3tbx.mphchl;

import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.pointop.Sample;
import org.esa.snap.core.gpf.pointop.SourceSampleConfigurer;
import org.esa.snap.core.gpf.pointop.WritableSample;

@OperatorMetadata(alias = "MphChlOlci-beta",
        version = "1.0",
        internal = true,
        authors = "Mark William Matthews, Daniel Odermatt, Tom Block, Olaf Danne",
        copyright = "(c) 2013, 2014, 2017 by Brockmann Consult",
        description = "Computes maximum peak height of chlorophyll for OLCI. Implements OLCI-specific parts.")
public class MphChlOlciBetaOp extends MphChlBasisBetaOp {

    private static final int BRR_7_IDX = 0;
    private static final int BRR_8_IDX = 1;
    private static final int BRR_10_IDX = 2;
    private static final int BRR_11_IDX = 3;
    private static final int BRR_12_IDX = 4;
    private static final int BRR_18_IDX = 5;
    private static final int BRR_06_IDX = 6;

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {
        if (!isSampleValid(x, y)) {
            MphChlUtils.setToInvalid(targetSamples, exportMph, exportAddBands);
            return;
        }

        final double r_7 = sourceSamples[BRR_7_IDX].getDouble();
        final double r_8 = sourceSamples[BRR_8_IDX].getDouble();
        final double r_10 = sourceSamples[BRR_10_IDX].getDouble();
        final double r_11 = sourceSamples[BRR_11_IDX].getDouble();
        final double r_12 = sourceSamples[BRR_12_IDX].getDouble();
        final double r_18 = sourceSamples[BRR_18_IDX].getDouble();
        final double r_6 = sourceSamples[BRR_06_IDX].getDouble();

        double maxBrr_0 = r_10;
        double maxLambda_0 = sensorWvls[9];     // 681
        if (r_11 > maxBrr_0) {
            maxBrr_0 = r_11;
            maxLambda_0 = sensorWvls[10];        // 709
        }

        double maxBrr_1 = maxBrr_0;
        double maxLambda_1 = maxLambda_0;
        if (r_12 > maxBrr_1) {
            maxBrr_1 = r_12;
            maxLambda_1 = sensorWvls[11];      // 753
        }

        final double ndvi = (r_18 - r_8) / (r_18 + r_8);
        final double SIPF_peak = r_8 - r_7 - ((r_10 - r_7) * ratioP);
        final double SICF_peak = r_10 - r_8 - ((r_11 - r_8) * ratioC);
        final double BAIR_peak = r_11 - r_8 - ((r_18 - r_8) * ratioB);

        double mph_0 = MphChlUtils.computeMph(maxBrr_0, r_8, r_18, maxLambda_0,
                sensorWvls[7],         // 664
                sensorWvls[17]);     // 885
        double mph_1 = MphChlUtils.computeMph(maxBrr_1, r_8, r_18, maxLambda_1,
                sensorWvls[7],         // 664
                sensorWvls[17]);     // 885

        boolean floating_flag = false;
        boolean adj_flag = false;
        boolean cyano_flag = false;

        int immersed_cyano = 0;
        int floating_cyano = 0;
        int floating_vegetation = 0;

        boolean calculatePolynomial = false;
        boolean calculateExponential = false;

        if (maxLambda_1 != sensorWvls[11]) {       // 753
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
        pci = MphChlUtils.computePci(r_6, r_7, r_8,
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
        sampleConfigurer.defineSample(BRR_7_IDX , "rBRR_07");   //  619
        sampleConfigurer.defineSample(BRR_8_IDX , "rBRR_08");    // 664
        sampleConfigurer.defineSample(BRR_10_IDX, "rBRR_10");    // 681
        sampleConfigurer.defineSample(BRR_11_IDX, "rBRR_11");    // 709
        sampleConfigurer.defineSample(BRR_12_IDX, "rBRR_12");   // 753
        sampleConfigurer.defineSample(BRR_18_IDX, "rBRR_18");   // 885
        sampleConfigurer.defineSample(BRR_06_IDX, "rBRR_06");   // 560
    }

    @Override
    protected void prepareInputs() throws OperatorException {
        super.prepareInputs();

        sensorWvls = MphChlConstants.OLCI_WAVELENGHTS;

        ratioP = (sensorWvls[7] - sensorWvls[6]) / (sensorWvls[9] - sensorWvls[6]);    // (664 - 619)/(681 - 619)
        ratioC = (sensorWvls[9] - sensorWvls[7]) / (sensorWvls[10] - sensorWvls[7]);   // (681 - 664)/(709 - 664)
        ratioB = (sensorWvls[10] - sensorWvls[7]) / (sensorWvls[17] - sensorWvls[7]);  // (709 - 664)/(885 - 664)
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(MphChlOlciBetaOp.class);
        }
    }
}
