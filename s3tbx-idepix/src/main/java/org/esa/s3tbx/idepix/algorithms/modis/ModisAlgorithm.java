package org.esa.s3tbx.idepix.algorithms.modis;

/**
 * IDEPIX pixel identification algorithm for OC-CCI/MODIS
 *
 * @author olafd
 */
public class ModisAlgorithm {

    private static final double THRESH_BRIGHT_SNOW_ICE = 0.25;
    private static final double THRESH_NDSI_SNOW_ICE = 0.8;

    private boolean modisApplyBrightnessTest;
    private double modisBrightnessThreshCloudSure;
    private double modisBrightnessThreshCloudAmbiguous;
    private double modisBNirThresh859;
    private double modisGlintThresh859forCloudSure;
    private double modisGlintThresh859forCloudAmbiguous;
    private boolean modisApplyOrLogicInCloudTest;

    private double nnCloudAmbiguousLowerBoundaryValue;
    private double nnCloudAmbiguousSureSeparationValue;
    private double nnCloudSureSnowSeparationValue;

    float waterFraction;
    double[] refl;
    double[] nnOutput;

    /**
     * Provides snow/ice identification (still experimental)
     *
     * @return boolean
     */
    public boolean isSnowIce() {

        // for MODIS ALL NN, nnOutput has one element:
        // nnOutput[0] =
        // 0 < x < nnCloudAmbiguousLowerBoundaryValue : clear
        // 2.0 < x < 3.35 : noncl / semitransparent cloud --> cloud ambiguous
        // 3.35 < x < 4.2 : cloudy --> cloud sure
        // 4.2 < x : clear snow/ice
        boolean isSnowIceFromNN;
        if (nnOutput != null) {
            isSnowIceFromNN = nnOutput[0] > nnCloudSureSnowSeparationValue && nnOutput[0] <= 5.0;    // separation numbers from HS, 20140923
        } else {
            // fallback
            // needs ndsi and brightness
            // MODIS: for slope use bands 16 (869nm) and 7 (2130nm, 500m spatial), threshold to be adjusted
            // for brightness use band 16 (Rayleigh corrected?)
            isSnowIceFromNN = (!isInvalid() && brightValue() > THRESH_BRIGHT_SNOW_ICE && ndsiValue() > THRESH_NDSI_SNOW_ICE);
            // todo: use MP stuff as fallback or in combination?
        }

        // MP additional criteria:

        // 0.95 < (EV_500_Aggr1km_RefSB_4 / EV_500_Aggr1km_RefSB_3 ) < 1 -> ice confidence 1 (not in sun glint area)
        // todo: does not work, see Madagaskar example A2003062103500.L1B_LAC
//        final double reflRatio4By3 = refl[3]/refl[2];
//        final boolean isSnowIceFromReflRatio = !isGlintRisk() && reflRatio4By3 > 0.95 && reflRatio4By3 < 1.0;
        final boolean isSnowIceFromReflRatio = false;

        // f1: EV_500_Aggr1km_RefSB_3 (469.0nm) [R]
        // f2: EV_500_Aggr1km_RefSB_5 (1240.0nm) [G]
        // f3: EV_500_Aggr1km_RefSB_7 (2130.0nm) [B]
        // f1 > 0.3 && f1/f2 > 2 && f1/f3 > 3 => SNOW/ICE
        // todo: does not work, no ice over Antarctica, example A2003062103500.L1B_LAC
//        final double reflR = refl[2];
//        final double reflG = refl[4];
//        final double reflB = refl[6];
//        final boolean isSnowIceFromRGB = reflR > 0.3 && reflR/reflG > 2.0 && reflR/reflB > 3.0;
        final boolean isSnowIceFromRGB = false;

//        return isSnowIceFromNN || isSnowIceFromReflRatio || isSnowIceFromRGB;
        return isSnowIceFromNN;
    }

    /**
     * Provides cloud identification
     *
     * @return boolean
     */
    public boolean isCloud() {
        return isCloudAmbiguous() || isCloudSure();
    }

    /**
     * Provides cloud ambiguous identification
     *
     * @return boolean
     */
    public boolean isCloudAmbiguous() {
        if (isLand()) {
            // over land, cloud_sure works very well, and the NN thresholds for water do not really work over land
            return isCloudSure();
        }

        if (isCloudSure() || isSnowIce()) {   // this check has priority
            return false;
        }

        // for MODIS ALL NN, nnOutput has one element:
        // nnOutput[0] =
        // 0 < x < 2.0 : clear
        // 2.0 < x < 3.35 : noncl / semitransparent cloud --> cloud ambiguous
        // 3.35 < x < 4.2 : cloudy --> cloud sure
        // 4.2 < x : clear snow/ice
        boolean isCloudAmbiguousFromNN;
        final boolean isCloudAmbiguousFromBrightness = modisApplyBrightnessTest &&
                brightValue() > modisBrightnessThreshCloudAmbiguous;
        if (nnOutput != null) {
            isCloudAmbiguousFromNN = nnOutput[0] > nnCloudAmbiguousLowerBoundaryValue && nnOutput[0] <= nnCloudAmbiguousSureSeparationValue;    // separation numbers from HS, 20140923
            isCloudAmbiguousFromNN = isCloudAmbiguousFromNN && refl[1] > modisGlintThresh859forCloudAmbiguous;
        } else {
            // fallback
            isCloudAmbiguousFromNN = isCloudAmbiguousFromBrightness;
        }

        // MP additional criteria:

        // Whiteness Criteria
        // (A bright and spectrally flat signal)
        // c1: EV_250_Aggr1km_RefSB_1 / EV_500_Aggr1km_RefSB_3
        // c2: EV_500_Aggr1km_RefSB_4/ EV_500_Aggr1km_RefSB_3
        // c3: EV_250_Aggr1km_RefSB_1 / EV_500_Aggr1km_RefSB_4

        // c1 > 0.87 && c2 > 0.9 && c3 > 0.97 --> cloud sure
        final float c1 = whiteValue(0, 2);
        final float c2 = whiteValue(3, 2);
        final float c3 = whiteValue(0, 3);

        boolean isCloudAmbiguousFromWhitenesses;
        final double m = Math.min(Math.min(refl[0], refl[2]), refl[3]);
        if (isLand()) {
            isCloudAmbiguousFromWhitenesses = m > 0.3 && c1 > 0.85 && c2 > 0.86 && c3 > 0.86 && c1 <= 0.96 && c2 <= 0.93 && c3 <= 0.05;
        } else {
            isCloudAmbiguousFromWhitenesses = m > 0.3 && c1 > 0.6 && c2 > 0.74 && c3 > 0.9;
        }
        isCloudAmbiguousFromWhitenesses = isCloudAmbiguousFromWhitenesses &&
                refl[1] > modisGlintThresh859forCloudAmbiguous;

        if (modisApplyOrLogicInCloudTest) {
            return isCloudAmbiguousFromNN || isCloudAmbiguousFromBrightness || isCloudAmbiguousFromWhitenesses;
        } else {
            return isCloudAmbiguousFromNN || (isCloudAmbiguousFromBrightness && isCloudAmbiguousFromWhitenesses);
        }
    }

    /**
     * Provides cloud sure identification
     *
     * @return boolean
     */
    public boolean isCloudSure() {
        if (isSnowIce()) {   // this has priority
            return false;
        }

        // for MODIS ALL NN, nnOutput has one element:
        // nnOutput[0] =
        // 0 < x < 2.0 : clear
        // 2.0 < x < 3.35 : noncl / semitransparent cloud --> cloud ambiguous
        // 3.35 < x < 4.2 : cloudy --> cloud sure
        // 4.2 < x : clear snow/ice
        boolean isCloudSureFromNN;
        final boolean isCloudSureFromBrightness = modisApplyBrightnessTest &&
                brightValue() > Math.max(modisBrightnessThreshCloudSure, modisBrightnessThreshCloudAmbiguous);
        if (nnOutput != null) {
            isCloudSureFromNN = nnOutput[0] > nnCloudAmbiguousSureSeparationValue && nnOutput[0] <= nnCloudSureSnowSeparationValue;   // ALL NN separation numbers from HS, 20140923
            isCloudSureFromNN = isCloudSureFromNN  && refl[1] > modisGlintThresh859forCloudSure;
        } else {
            // fallback
            isCloudSureFromNN = isCloudSureFromBrightness;
        }

        // MP additional criteria:

        // Whiteness Criteria
        // (A bright and spectrally flat signal)
        // c1: EV_250_Aggr1km_RefSB_1 / EV_500_Aggr1km_RefSB_3       (645/469)
        // c2: EV_500_Aggr1km_RefSB_4/ EV_500_Aggr1km_RefSB_3        (555/469)
        // c3: EV_250_Aggr1km_RefSB_1 / EV_500_Aggr1km_RefSB_4       (645/555)

        // c1 > 0.87 && c2 > 0.9 && c3 > 0.97 --> cloud sure
        final float c1 = whiteValue(0, 2);
        final float c2 = whiteValue(3, 2);
        final float c3 = whiteValue(0, 3);

        boolean isCloudSureFromWhitenesses;
//        m = Min(EV_250_Aggr1km_RefSB_1, EV_500_Aggr1km_RefSB_3, EV_500_Aggr1km_RefSB_4) > 0.3:
        final double m = Math.min(Math.min(refl[0], refl[2]), refl[3]);
        if (isLand()) {
            isCloudSureFromWhitenesses = m > 0.7 && c1 > 0.96 && c2 > 0.93 && c3 > 0.95 && c1 < 1.04 && c2 < 1.05 && c3 < 1.05;
        } else {
            isCloudSureFromWhitenesses = c1 > 0.87 && c2 > 0.9 && c3 > 0.97;
        }
        isCloudSureFromWhitenesses = isCloudSureFromWhitenesses && refl[1] > modisGlintThresh859forCloudSure;

        if (modisApplyOrLogicInCloudTest) {
            return isCloudSureFromNN || isCloudSureFromBrightness || isCloudSureFromWhitenesses;
        } else {
            return isCloudSureFromNN || (isCloudSureFromBrightness && isCloudSureFromWhitenesses);
        }
    }

    public boolean isCloudBNir() {
        // a new MODIS test for water  (CB/CL, 20161128)
        if (isLand()) {
            // over land, cloud_sure works very well, and the BNir test does not really work here
            return isCloudSure();
        } else {
            return refl[1] > modisBNirThresh859;
        }
    }


    public boolean isCloudBuffer() {
        // is applied in post processing!
        return false;
    }

    public boolean isCloudShadow() {
        // will be applied in post processing once we have an appropriate algorithm
        return false;
    }

    public boolean isMixedPixel() {
        // todo
        // unmixing using MERIS bands 7, 9, 10, 12
        return false;
    }

    public boolean isGlintRisk() {
        // todo
        // depends on geometry, windspeed and rho_toa_865
        // MODIS: we have rho_toa_865, wind components are required!
        // MODIS: use L2 product if available
        return false;
    }

    public boolean isInvalid() {
        // todo: define if needed
        return false;
    }

    public boolean isCoastline() {
        // NOTE that this does not work if we have a PixelGeocoding. In that case, waterFraction
        // is always 0 or 100!! (TS, OD, 20140502). If so, get a coastline in post processing approach.
        return waterFraction < 100 && waterFraction > 0;
    }

    public boolean isLand() {
        return waterFraction == 0;
    }

    public boolean isBright() {
        return brightValue() > modisBrightnessThreshCloudSure;
    }

    ///////////////// feature values ////////////////////////////////////////

    public float brightValue() {
        return (float) refl[0];   //  EV_250_Aggr1km_RefSB_1 (645nm)
//        return (float) refl[1];   //  EV_250_Aggr1km_RefSB_2 (859nm)
//        return (float) refl[4];   //  EV_250_Aggr1km_RefSB_5 (1240nm)
    }

    public float whiteValue(int numeratorIndex, int denominatorIndex) {
        return (float) (refl[numeratorIndex] / refl[denominatorIndex]);
    }

    public float ndsiValue() {
        // use EV_250_Aggr1km_RefSB_1, EV_500_Aggr1km_RefSB_7
        return (float) ((refl[0] - refl[6]) / (refl[0] + refl[6]));
    }

    
    ///////////////// further setter methods ////////////////////////////////////////
    public void setWaterFraction(float waterFraction) {
        this.waterFraction = waterFraction;
    }

    public void setRefl(double[] reflectance) {
        refl = reflectance;
    }

    public void setNnOutput(double[] nnOutput) {
        this.nnOutput = nnOutput;
    }
    
    public void setModisApplyBrightnessTest(boolean modisApplyBrightnessTest) {
        this.modisApplyBrightnessTest = modisApplyBrightnessTest;
    }

    public void setModisBrightnessThreshCloudSure(double modisBrightnessThresh) {
        this.modisBrightnessThreshCloudSure = modisBrightnessThresh;
    }

    public void setModisBNirThresh859(double modisBNirThresh859) {
        this.modisBNirThresh859 = modisBNirThresh859;
    }

    public void setModisBrightnessThreshCloudAmbiguous(double modisBrightnessThreshCloudAmbiguous) {
        this.modisBrightnessThreshCloudAmbiguous = modisBrightnessThreshCloudAmbiguous;
    }

    public void setModisGlintThresh859forCloudSure(double modisGlintThresh859forCloudSure) {
        this.modisGlintThresh859forCloudSure = modisGlintThresh859forCloudSure;
    }

    public void setModisGlintThresh859forCloudAmbiguous(double modisGlintThresh859forCloudAmbiguous) {
        this.modisGlintThresh859forCloudAmbiguous = modisGlintThresh859forCloudAmbiguous;
    }


    public void setModisApplyOrLogicInCloudTest(boolean modisApplyOrLogicInCloudTest) {
        this.modisApplyOrLogicInCloudTest = modisApplyOrLogicInCloudTest;
    }

    public void setNnCloudAmbiguousLowerBoundaryValue(double nnCloudAmbiguousLowerBoundaryValue) {
        this.nnCloudAmbiguousLowerBoundaryValue = nnCloudAmbiguousLowerBoundaryValue;
    }

    public void setNnCloudAmbiguousSureSeparationValue(double nnCloudAmbiguousSureSeparationValue) {
        this.nnCloudAmbiguousSureSeparationValue = nnCloudAmbiguousSureSeparationValue;
    }

    public void setNnCloudSureSnowSeparationValue(double nnCloudSureSnowSeparationValue) {
        this.nnCloudSureSnowSeparationValue = nnCloudSureSnowSeparationValue;
    }
}
