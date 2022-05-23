package org.esa.s3tbx.olci.radiometry.rayleigh;

import com.google.common.primitives.Doubles;
import com.google.common.primitives.Floats;
import org.esa.s3tbx.olci.radiometry.Sensor;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.math.RsMathUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * @author muhammad.bc.
 */
public class RayleighCorrAlgorithm {

    private Sensor sensor;
    private int numBands;
    private String bandNamePattern;

    public RayleighCorrAlgorithm(String bandPattern, int numBands) {
        this.numBands = numBands;
        this.bandNamePattern = bandPattern;
    }

    public RayleighCorrAlgorithm(Sensor sensor) {
        this(sensor.getNameFormat(), sensor.getNumBands());
        this.sensor = sensor;
    }

    //todo mba/* write test
    double[] waterVaporCorrection709(double[] reflectances, double[] bWVRefTile, double[] bWVTile) {
        double[] H2O_COR_POLY = new double[]{0.3832989, 1.6527957, -1.5635101, 0.5311913};  // Polynomial coefficients for WV transmission @ 709nm
        // in order to optimise performance we do:
        // trans709 = H2O_COR_POLY[0] + (H2O_COR_POLY[1] + (H2O_COR_POLY[2] + H2O_COR_POLY[3] * X2) * X2) * X2
        // when X2 = 1
        // trans709 = 0.3832989 + ( 1.6527957+ (-1.5635101+ 0.5311913*1)*1)*1
        for (int i = 0; i < bWVTile.length; i++) {
            double trans709 = 1.0037757999999999;
            if (bWVTile[i] > 0) {
                double X2 = bWVTile[i] / bWVRefTile[i];
                trans709 = H2O_COR_POLY[0] + (H2O_COR_POLY[1] + (H2O_COR_POLY[2] + H2O_COR_POLY[3] * X2) * X2) * X2;
            }
            reflectances[i] = reflectances[i] / trans709;
        }
        return reflectances;
    }

    double[] getCrossSectionSigma(Product sourceProduct, int numBands, String getBandNamePattern) {
        if (sensor != null && sensor == Sensor.S2_MSI) {
            // use 'true' instead of central wavelengths instead
            // CB/GK 20170721, following https://earth.esa.int/documents/247904/685211/Sentinel-2+MSI+Spectral+Responses/
            return getCrossSection(S2Utils.getS2TrueWavelengths());
        }
        if (sensor != null && sensor == Sensor.LANDSAT_8) {
            // PAN is omitted, because of large bandwidth
            //Bands are: coastal, blue, green, red, NIR, SWIR1, SWIR2, cirrus
//            double[] wavelength = new double[]{442.98221107806575, 482.58885989752395, 561.3321416459181,654.6055091346834,
//                    864.570827584894, 1609.0905268131812, 2201.248335978548, 1373.4761739935636};
            //wavelengths are adjusted, so that the crosssection is the same for this single wavelength and the SRF convolution
            double[] wavelength = new double[]{442.84270278503936, 480.9275707481116, 560.0407717798489, 654.1336756414217,
                    864.3504561762736, 1607.9946386141337, 2197.7003396698724, 1373.394946188435};
            return getCrossSection(wavelength);
        } else if (sensor != null && sensor == Sensor.OLCI) {
            double[] wavelength;
            if (isSentinelB(sourceProduct)) {
                //these are the wavelengths for S3B
                wavelength = new double[]{400.5946791630635, 411.9509453369866, 442.9881235940998, 490.3991247296052, 510.4022075929168,
                        560.3663252369439, 620.2839618019312, 665.1312164230563, 673.8681527151621, 681.3856157084324,
                        708.9820148630681, 754.0283289464179, 761.5594400832483, 764.6921706636257, 767.8224408129396,
                        779.0792072094514, 865.2710905079641, 884.1272744748017, 899.121616355846, 938.7977736873601,
                        1015.7390081732657};
            } else {
                //these wavelengths belong to S3A
                wavelength = new double[]{400.3031914558257, 411.8452957525705, 442.9625672120682, 490.4930356580268, 510.46748124416945,
                        560.4502797598624, 620.4092905501666, 665.2744162328253, 674.0251490485472, 681.5706005756095,
                        709.1148593849875, 754.1813240203888, 761.7260948029898, 764.8247093465473, 767.9174355161354,
                        779.2567595815481, 865.4296340763456, 884.3082558969855, 899.3107685704568, 938.9730748611009,
                        1015.7990909091901};
            }
            return getCrossSection(wavelength);
        } else {
            double[] wavelength = new double[numBands];
            for (int i = 0; i < numBands; i++) {
                wavelength[i] = sourceProduct.getBand(String.format(getBandNamePattern, i + 1)).getSpectralWavelength();
            }
            return getCrossSection(wavelength);
        }
    }

    private boolean isSentinelB(Product sourceProduct) {
        MetadataElement metadataRoot = sourceProduct.getMetadataRoot();
        if (metadataRoot != null) {
            MetadataElement manifest = metadataRoot.getElement("Manifest");
            if (manifest != null) {
                MetadataElement metadataSection = manifest.getElement("metadataSection");
                if (metadataSection != null) {
                    MetadataElement platform = metadataSection.getElement("platform");
                    if (platform != null) {
                        String sentinelNumber = platform.getAttributeString("number");
                        return "B".equals(sentinelNumber);
                    }
                }
            }
        }
        return false;
    }

    /**
     * This is from [1]: Bodhaine et al (1999): On Rayleigh Optical Depth Calculations
     * http://journals.ametsoc.org/doi/pdf/10.1175/1520-0426%281999%29016%3C1854%3AORODC%3E2.0.CO%3B2
     *
     * @param lambdas - spectral wavelengths (should be the real ones which consider the response function,
     *                not necessarily the central wavelengths)
     * @return the scatttering cross sections for each wavelength
     */
    double[] getCrossSection(double[] lambdas) {
        double n_ratio = 1 + 0.54 * (RayleighConstants.CO2 - 0.0003);
        double molecularDen = RayleighConstants.Molecular_cm3;
        double[] sigma = new double[lambdas.length];

        for (int i = 0; i < lambdas.length; i++) {
            double lambdamm = lambdas[i] / 1000.0;
            double lambdaWLcm = lambdas[i] / 1.E7;

            // [1], eq. (5):
            double F_N2 = 1.034 + 0.000317 / Math.pow(lambdamm, 2);
            // [1], eq. (6):
            double F_O2 = 1.096 + 0.001385 / Math.pow(lambdamm, 2) + 0.0001448 / Math.pow(lambdamm, 4);

            // [1], eqs. (4), (18):
            double n_1_300 = (8060.51 + (2480990.0 / (132.274 - Math.pow(lambdamm, (-2)))) + (17455.7 /
                    (39.32957 - Math.pow(lambdamm, (-2))))) / 100000000;
            // [1], eq. (23):
            double F_air = (78.084 * F_N2 + 20.946 * F_O2 + 0.934 * 1 + RayleighConstants.C_CO2 * 1.15) /
                    (78.084 + 20.946 + 0.934 + RayleighConstants.C_CO2);

            double nCO2 = n_ratio * n_1_300 + 1.;
            // [1], eq. (22):
            sigma[i] = 24 * Math.pow(Math.PI, 3) * Math.pow((Math.pow(nCO2, 2) - 1), 2) / (Math.pow(lambdaWLcm, 4) *
                    Math.pow(molecularDen, 2) * Math.pow((Math.pow(nCO2, 2) + 2), 2)) * F_air;
        }
        return sigma;
    }

    double[] getCorrOzone(double[] rho_ng_ref, double absorpO, double[] ozones, double[] cosOZARads,
                          double[] cosSZARads) {
        double[] ozoneCorrRefl = new double[rho_ng_ref.length];
        for (int i = 0; i < rho_ng_ref.length; i++) {
            double cts = cosSZARads[i]; //#cosine of sun zenith angle
            double ctv = cosOZARads[i];//#cosine of view zenith angle
            double ozone = ozones[i];
            double rho_ng = rho_ng_ref[i];
            ozoneCorrRefl[i] = Double.isNaN(rho_ng) ? Double.NaN : getCorrOzone(rho_ng, absorpO, ozone, cts, ctv);
        }
        return ozoneCorrRefl;
    }

    double getCorrOzone(double rho_ng, double absorpO, double ozone, double cts, double ctv) {
        if (cts == 0 || ctv == 0) {
            throw new ArithmeticException("The sun angel and the view angle must not be zero.");
        }
        double model_ozone = 0;
        double trans_ozoned12 = Math.exp(-(absorpO * ozone / 1000.0 - model_ozone) / cts);
        double trans_ozoneu12 = Math.exp(-(absorpO * ozone / 1000.0 - model_ozone) / ctv);
        double trans_ozone12 = trans_ozoned12 * trans_ozoneu12;
        return rho_ng / trans_ozone12;
    }

    //todo: water vapour absorption for Landsat8 SWIR2
    // use fixed value for scene? or: derive water vapour from cirrus band?

    //todo: NO2 absortion for Landsat8 coastal, blue, green


    double[] getRhoBrr(RayleighAux rayleighAux, double[] rayleighOpticalThickness, double[] corrOzoneRefl) {
        final double[] airMasses = rayleighAux.getAirMass();
        final double[] aziDiffs = rayleighAux.getAziDifferent();
        final double[] cosSZARads = rayleighAux.getCosSZARads();
        final double[] cosOZARads = rayleighAux.getCosOZARads();
        final double[] sARay = rayleighAux.getInterpolateRayleighThickness(rayleighOpticalThickness);
        final double[] tau_ray = rayleighAux.getTaur();

        final Map<Integer, double[]> fourier = rayleighAux.getFourier();
        final Map<Integer, List<double[]>> interpolation = rayleighAux.getInterpolation();
        final int length = cosOZARads.length;

        final double[] rho_BRR = new double[length];

        for (int index = 0; index < length; index++) {

            double corrOzone = corrOzoneRefl[index];
            if (Double.isNaN(corrOzone)) {
                rho_BRR[index] = RayleighConstants.INVALID_VALUE;
                continue;
            }
            if (corrOzone <= 0) {
                rho_BRR[index] = 0.0;
                continue;
            }
            double taurVal = rayleighOpticalThickness[index];
            if (Double.isNaN(taurVal)) {
                rho_BRR[index] = taurVal;
                continue;
            }
            double aziDiff = aziDiffs[index];
            double massAir = airMasses[index];

            double cosOZARad = cosOZARads[index];
            double cosSZARad = cosSZARads[index];

            List<double[]> interpolateValues = interpolation.get(index);
            if (interpolateValues == null || interpolateValues.size() == 0) {
                // this might happen if we are out of range of RayleighAux data (e.g. SZA > 80deg)
                // --> set to NaN (CB, 20171026)
                rho_BRR[index] = RayleighConstants.INVALID_VALUE;
                continue;
            }
            double[] fourierSeries = fourier.get(index);

            double[] rho_Rm = getFourierSeries(taurVal, massAir, cosOZARad, cosSZARad, interpolateValues, fourierSeries);

            double rho_R = rho_Rm[0] + 2.0 * rho_Rm[1] * Math.cos(aziDiff) + 2.0 * rho_Rm[2] * Math.cos(2.0 * aziDiff);

            // polynomial coefficients tpoly0, tpoly1 and tpoly2 from MERIS LUT

            double tRs = ((2.0 / 3.0 + cosSZARad) + (2.0 / 3.0 - cosSZARad) * Math.exp(-taurVal / cosSZARad)) / (4.0 / 3.0 + taurVal);

            double tR_thetaS = tau_ray[0] + tau_ray[1] * tRs + tau_ray[2] * Math.pow(tRs, 2);
            //#Rayleigh Transmittance sun - surface
            double tRv = ((2.0 / 3.0 + cosOZARad) + (2.0 / 3.0 - cosOZARad) * Math.exp(-taurVal / cosOZARad)) / (4.0 / 3.0 + taurVal);
            //#Rayleigh Transmittance surface - sensor
            double tR_thetaV = tau_ray[0] + tau_ray[1] * tRv + tau_ray[2] * Math.pow(tRv, 2);

            double saRay = sARay[index];


            double rho_toaR = (corrOzone - rho_R) / (tR_thetaS * tR_thetaV); //toa corrOzoneRefl corrected for Rayleigh scattering
            double sphericalFactor = 1.0 / (1.0 + saRay * rho_toaR); //#factor used in the next equation to account for the spherical albedo
            //#top of aerosol reflectance, which is equal to bottom of Rayleigh reflectance
            rho_BRR[index] = (rho_toaR * sphericalFactor);
        }
        return rho_BRR;
    }

    double[] getRhoRayleigh(RayleighAux rayleighAux, double[] rayleighOpticalThickness,
                            double[] corrOzoneRefl) {
        //for debugging.
        final double[] airMasses = rayleighAux.getAirMass();
        final double[] aziDiffs = rayleighAux.getAziDifferent();
        final double[] cosSZARads = rayleighAux.getCosSZARads();
        final double[] cosOZARads = rayleighAux.getCosOZARads();

        final Map<Integer, double[]> fourier = rayleighAux.getFourier();
        final Map<Integer, List<double[]>> interpolation = rayleighAux.getInterpolation();
        final int length = cosOZARads.length;

        final double[] rho_R = new double[length];

        for (int index = 0; index < length; index++) {

            double corrOzone = corrOzoneRefl[index];
            if (Double.isNaN(corrOzone)) {
                rho_R[index] = RayleighConstants.INVALID_VALUE;
                continue;
            }
            if (corrOzone <= 0) {
                rho_R[index] = 0.0;
                continue;
            }
            double taurVal = rayleighOpticalThickness[index];
            if (Double.isNaN(taurVal)) {
                rho_R[index] = taurVal;
                continue;
            }
            double aziDiff = aziDiffs[index];
            double massAir = airMasses[index];

            double cosOZARad = cosOZARads[index];
            double cosSZARad = cosSZARads[index];

            List<double[]> interpolateValues = interpolation.get(index);
            if (interpolateValues == null || interpolateValues.size() == 0) {
                // this might happen if we are out of range of RayleighAux data (e.g. SZA > 80deg)
                // --> set to NaN (CB, 20171026)
                rho_R[index] = RayleighConstants.INVALID_VALUE;
                continue;
            }
            double[] fourierSeries = fourier.get(index);
            double[] rho_Rm = getFourierSeries(taurVal, massAir, cosOZARad, cosSZARad, interpolateValues, fourierSeries);
            rho_R[index] = rho_Rm[0] + 2.0 * rho_Rm[1] * Math.cos(aziDiff) + 2.0 * rho_Rm[2] * Math.cos(2.0 * aziDiff);
        }
        return rho_R;
    }

    double[] getFourierSeries(double rayleighOpticalThickness, double massAir, double cosOZARad,
                              double cosSZARad, List<double[]> interpolateValues, double[] fourierSeriesCof) {
        double[] rhoRm = new double[fourierSeriesCof.length];
        for (int i = 0; i < fourierSeriesCof.length; i++) {
            double[] interpolatedValueABCD = interpolateValues.get(i);
            double a = interpolatedValueABCD[0];
            double b = interpolatedValueABCD[1];
            double c = interpolatedValueABCD[2];
            double d = interpolatedValueABCD[3];

            double rayPrimaryScatters = (fourierSeriesCof[i] / (4.0 * (cosSZARad + cosOZARad))) * (1.0 - Math.exp(-massAir * rayleighOpticalThickness));
            double rayMultiCorr = a + b * rayleighOpticalThickness + c * Math.pow(rayleighOpticalThickness, 2) + d * Math.pow(rayleighOpticalThickness, 3);
            rhoRm[i] = rayMultiCorr * rayPrimaryScatters;
        }
        return rhoRm;
    }


    public RayleighOutput getRayleighReflectance(RayleighInput rayleighInput, RayleighAux rayleighAux,
                                                 double[] absorptionOfBand, Product product) {
        int sourceIndex = rayleighInput.getSourceIndex();
        int lowerWaterIndex = rayleighInput.getLowerWaterIndex();
        int upperWaterIndex = rayleighInput.getUpperWaterIndex();

        OpticalThickness opticalThickness = new OpticalThickness(rayleighAux, product);
        double[] bandThicknessSource = opticalThickness.getThicknessBand(sourceIndex);
        double[] bandThicknessLower = opticalThickness.getThicknessBand(lowerWaterIndex);
        double[] bandThicknessUpper = opticalThickness.getThicknessBand(upperWaterIndex);

        double absorpSourceBand = absorptionOfBand[sourceIndex];
        double absorpLowerBand = absorptionOfBand[lowerWaterIndex];
        double absorpUppereBand = absorptionOfBand[upperWaterIndex];

        float[] sourceRayRefl = getRayleigh(rayleighAux, absorpSourceBand, bandThicknessSource, rayleighInput.getSourceReflectences());
        float[] lowerRayRefl = getRayleigh(rayleighAux, absorpLowerBand, bandThicknessLower, rayleighInput.getLowerReflectences());
        float[] upperRayRefl = getRayleigh(rayleighAux, absorpUppereBand, bandThicknessUpper, rayleighInput.getUpperReflectences());

        return new RayleighOutput(sourceRayRefl, lowerRayRefl, upperRayRefl);
    }

    private float[] getRayleigh(RayleighAux rayleighAux, double absorptionOfBand, double[] thicknessAllBand,
                                float[] ref) {

        double[] ozones = rayleighAux.getTotalOzones();
        double[] cosOZARads = rayleighAux.getCosOZARads();
        double[] cosSZARads = rayleighAux.getCosSZARads();

        double[] refDoube = convertFloatToDouble(ref);
        double[] corrOzone = getCorrOzone(refDoube, absorptionOfBand, ozones, cosSZARads, cosOZARads);

        double[] rhoBrr = getRhoBrr(rayleighAux, thicknessAllBand, corrOzone);
        return convertDoubleToFloat(rhoBrr);
    }

    private double[] convertFloatToDouble(float[] ref) {
        return IntStream.range(0, ref.length).mapToDouble(p -> ref[p]).toArray();
    }

    private float[] convertDoubleToFloat(double[] ref) {
        return Floats.toArray(Doubles.asList(ref));
    }


    double[] getRayleighThickness(RayleighAux rayleighAux,
                                  double[] crossSectionSigma,
                                  int sourceBandIndex,
                                  String targetBandName) {
        double[] seaLevels = rayleighAux.getSeaLevels();
        double[] altitudes = rayleighAux.getAltitudes();
        double[] latitudes = rayleighAux.getLatitudes();
        final int crossSectionSigmaIndex = getCrossSectionSigmaIndex(sourceBandIndex, targetBandName);
        double sigma = crossSectionSigma[crossSectionSigmaIndex];

        double[] rayleighOpticalThickness = new double[altitudes.length];
        for (int i = 0; i < altitudes.length; i++) {
            rayleighOpticalThickness[i] = getRayleighOpticalThickness(sigma, seaLevels[i], altitudes[i], latitudes[i]);
        }

        return rayleighOpticalThickness;
    }


    public void getRayleighThicknessBodhaineTest() {
        double[] seaLevels = new double[]{1013.5, 680.};
        double[] altitudes = new double[]{0., 3400.};
        double[] latitudes = new double[]{45., 19.5338};
        double[] wavelength = new double[]{400., 405., 410.};

        double[] sigma = getCrossSection(wavelength);
        double[] bodhaine_sigma = new double[]{1.67380E-26, 1.58950E-26, 1.51050E-26};

        for (int i = 0; i < bodhaine_sigma.length; i++) {
            System.out.println("sigma");
            System.out.println(wavelength[i]);
            System.out.println((sigma[i] - bodhaine_sigma[i]) / bodhaine_sigma[i] * 100.);
        } // sigma error is fine! implementation is fine.

        //example 1 at sealevel:
        double[] bodhaine_taur1 = new double[]{3.6022E-01, 3.4207E-01, 3.2506E-01};
        //example 2 at 3400m:
        double[] bodhaine_taur2 = new double[]{2.4243E-01, 2.3022E-01, 2.1877E-01};

        for (int i = 0; i < altitudes.length; i++) {
            double[] rayleighOpticalThickness = new double[sigma.length];
            for (int j = 0; j < sigma.length; j++) {
                //for the test the pressure does not need to be corrected for the altitude, hence we do:
                //pressure = seaLevelPressure * 1000.;
                rayleighOpticalThickness[j] = getRayleightOptThickness(sigma[j], altitudes[i], latitudes[i], seaLevels[i] * 1000.0);
                if (altitudes[i] == 0.) {
                    System.out.println((rayleighOpticalThickness[j] - bodhaine_taur1[j]) / bodhaine_taur1[j] * 100.);
                } else if (altitudes[i] > 0.) {
                    System.out.println((rayleighOpticalThickness[j] - bodhaine_taur2[j]) / bodhaine_taur2[j] * 100.);
                }
            }
        }

    }


    double getRayleighOpticalThickness(double sigma, double seaLevelPressure, double altitude,
                                       double latitude) {
        double pressure = seaLevelPressure * Math.pow((1.0 - 0.0065 * altitude / 288.15), 5.255) * 1000.;
        return getRayleightOptThickness(sigma, altitude, latitude, pressure);
    }

    private double getRayleightOptThickness(double sigma, double altitude, double latitude, double pressure) {
        double latRad = Math.toRadians(latitude);
        double cos2LatRad = Math.cos(2 * latRad);
        double g0 = 980.616 * (1 - 0.0026372 * cos2LatRad + 0.0000059 * Math.pow(cos2LatRad, 2));
        double effectiveMassWeightAltitude = 0.73737 * altitude + 5517.56;

        double g = g0 - (3.085462E-4 + 2.27E-7 * cos2LatRad) * effectiveMassWeightAltitude +
                (7.254E-11 + 1.E-13 * cos2LatRad) * Math.pow(effectiveMassWeightAltitude, 2.) -
                (1.517E-17 + 6.E-20 * cos2LatRad) * Math.pow(effectiveMassWeightAltitude, 3.);

        double p1 = (3.085462E-4 + 2.27E-7 * cos2LatRad) * effectiveMassWeightAltitude;
        double p2 = (7.254E-11 + 1.E-13 * cos2LatRad) * Math.pow(effectiveMassWeightAltitude, 2.);
        double p3 = (1.517E-17 + 6.E-20 * cos2LatRad) * Math.pow(effectiveMassWeightAltitude, 3.);

        double factor = (pressure * RayleighConstants.AVOGADRO_NUMBER) / (RayleighConstants.MEAN_MOLECULAR_WEIGHT_C02 * g);
//        System.out.println(latitude + " " + p1 + " " + p2 + " " +p3 +" " + sigma + " "+ factor + " " + g + " " + g0 + " " + cos2LatRad + " "+ altitude);
        return factor * sigma;
    }

    double[] convertRadsToRefls(double[] radiance, double[] solarIrradiance, double[] sza) {
        double[] ref = new double[radiance.length];
        for (int i = 0; i < ref.length; i++) {
            ref[i] = RsMathUtils.radianceToReflectance((float) radiance[i], (float) sza[i], (float) solarIrradiance[i]);
        }
        return ref;
    }

    private int getCrossSectionSigmaIndex(int sourceBandIndex, String targetBandName) {
        if (sensor != null && sensor == Sensor.S2_MSI) {
            return S2Utils.getS2SpectralBandIndex(targetBandName);
        } else {
            return sourceBandIndex - 1;
        }
    }

    private class OpticalThickness {
        RayleighAux rayleighAux;
        private Product product;
        private Map<Integer, double[]> thicknessAllBands;

        OpticalThickness(RayleighAux rayleighAux, Product product) {
            this.rayleighAux = rayleighAux;
            this.product = product;
        }

        private Map<Integer, double[]> getThicknessAllBands() {
            double[] crossSectionSigma = getCrossSectionSigma(product, numBands, bandNamePattern);
            Map<Integer, double[]> thicknessPerBand = new HashMap<>();
            for (int bandIndex = 1; bandIndex <= numBands; bandIndex++) {
                double[] rayleighThickness = getRayleighThickness(rayleighAux, crossSectionSigma, bandIndex, null);
                thicknessPerBand.put(bandIndex, rayleighThickness);
            }
            return thicknessPerBand;
        }

        double[] getThicknessBand(int bandIndex) {
            if (thicknessAllBands == null) {
                thicknessAllBands = getThicknessAllBands();
            }
            return thicknessAllBands.get(bandIndex + 1);
        }
    }
}
