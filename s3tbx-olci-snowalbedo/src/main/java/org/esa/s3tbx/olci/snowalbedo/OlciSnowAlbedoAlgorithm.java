package org.esa.s3tbx.olci.snowalbedo;

import org.esa.s3tbx.processor.rad2refl.Rad2ReflConstants;
import org.esa.snap.core.util.math.MathUtils;

/**
 * Snow Albedo Algorithm for OLCI following A. Kokhanovsky (EUMETSAT).
 *
 * References:
 * - [TN1] Snow planar broadband albedo determination from spectral OLCI snow spherical albedo measurements. (2017)
 * - [TN2] Snow spectral albedo determination using OLCI measurement. (2017)
 *
 * @author olafd
 */
public class OlciSnowAlbedoAlgorithm {

    public static double[] computeSpectralAlbedos(double[] rhoToa, double sza, double vza, double saa, double vaa) {
        double[] spectralAlbedos = new double[rhoToa.length];
        for (int i = 0; i < spectralAlbedos.length; i++) {
            spectralAlbedos[i] = computeSpectralAlbedo(rhoToa[i], sza, vza, saa, vaa);
        }
        return spectralAlbedos;
    }

    public static double computePlanarBroadbandAlbedo(double[] spectralAlbedos,
                                                      float[] solarFluxes,
                                                      double r21,
                                                      double sza) {
        // [TN1] eq. (2): integrate over all OLCI wavelengths
        double sumNumerator = 0.0;
        double sumDenominator = 0.0;
        double mu_0 = Math.cos(sza * MathUtils.DTOR);
        double theta_f = 50.0;
        double mu_ef = Math.cos(theta_f * MathUtils.DTOR);
        double z = mu_ef/mu_0;
        for (int i = 0; i < Rad2ReflConstants.OLCI_REFL_BAND_NAMES.length; i++) {
            double F_D = 1.0; // todo: get 'F_D = measured transmitted solar flux at SZA=50' from
            // Dang et al (2015) --> ask AK
            double F = solarFluxes[i] * Math.pow(F_D/solarFluxes[i], z);
            sumNumerator += spectralAlbedos[i] * F;
            sumDenominator += F;
        }
        final double r_b1 = sumNumerator / sumDenominator;

        final double b = 3.62;
        final double chi = 2.25E-6;
        final double lambda_21 = 1020.0;
        final double a_21 = 4.0*Math.PI*chi / lambda_21;
        final double d = Math.log(r21)*Math.log(r21) / b*b*a_21;

        final double q0 = 0.0947;
        final double q1 = 0.0569;
        final double d0 = 200.0;
        final double r_b2 = q0 - q1 * Math.log10(d/d0);

        return r_b1 + r_b2;
    }

    public static double computeSpectralAlbedo(double rhoToa, double sza, double vza, double saa, double vaa) {
        final double mu_0 = Math.cos(sza* MathUtils.DTOR);
        final double mu = Math.cos(vza*MathUtils.DTOR);
        final double s_0 = Math.sin(sza*MathUtils.DTOR);
        final double s = Math.sin(saa*MathUtils.DTOR);

        final double A = 1.247;
        final double B = 1.186;
        final double C = 5.157;

        final double theta = Math.acos(-mu*mu_0 + s*s_0*Math.cos((vaa - saa) * MathUtils.DTOR));
        final double p_theta = 11.1*Math.exp(-0.087*theta) + 1.1*Math.exp(-0.014*theta);
        final double R_0 = (A + B*(mu_0 + mu) + C*mu_0*mu + p_theta) / (4.0*(mu_0 + mu));

        final double p = R_0 / (computeU(mu)*computeU(mu_0));

        return Math.pow(rhoToa/R_0, p);
    }

    private static double computeU(double mu) {
        return 3.0 * (1.0 + 2.0*mu) / 7.0;
    }

}
