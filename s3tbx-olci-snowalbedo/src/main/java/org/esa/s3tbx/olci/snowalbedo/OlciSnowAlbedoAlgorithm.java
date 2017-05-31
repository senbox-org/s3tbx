package org.esa.s3tbx.olci.snowalbedo;

import org.esa.snap.core.util.math.MathUtils;

/**
 * Snow Albedo Algorithm for OLCI following A. Kokhanovsky (EUMETSAT).
 * <p>
 * References:
 * - [TN1] Snow planar broadband albedo determination from spectral OLCI snow spherical albedo measurements. (2017)
 * - [TN2] Snow spectral albedo determination using OLCI measurement. (2017)
 * <p>
 * todo: extract magic numbers as constants
 *
 * @author olafd
 */
public class OlciSnowAlbedoAlgorithm {

    public static double[] computeSpectralSphericalAlbedos(double[] rhoToa, double sza, double vza, double saa, double vaa) {
        double[] spectralAlbedos = new double[rhoToa.length];
        for (int i = 0; i < spectralAlbedos.length; i++) {
            spectralAlbedos[i] = computeSpectralAlbedo(rhoToa[i], sza, vza, saa, vaa);
        }
        return spectralAlbedos;
    }

    public static double[] computeSpectralPlanarAlbedos(double[] spectralSphericalAlbedos, double sza) {
        double[] spectralPlanarAlbedos = new double[spectralSphericalAlbedos.length];
        for (int i = 0; i < spectralPlanarAlbedos.length; i++) {
            spectralPlanarAlbedos[i] = computePlanarBroadbandAlbedo(spectralSphericalAlbedos[i], sza);
        }
        return spectralPlanarAlbedos;
    }


    public static SphericalBroadbandAlbedo computeSphericalBroadbandAlbedoTerms(double[] spectralAlbedos, double r21) {
        SphericalBroadbandAlbedo sbba = new SphericalBroadbandAlbedo();
        sbba.setR_b1(integrateR_b1(spectralAlbedos));
        final double grainDiameter = computeGrainDiameter(r21);
        sbba.setGrainDiameter(grainDiameter);
        sbba.setR_b2(integrateR_b2(grainDiameter));

        return sbba;
    }

    public static double computePlanarBroadbandAlbedo(double sphericalBroadbandAlbedo,
                                                      double sza) {
        final double mu_0 = Math.cos(sza * MathUtils.DTOR);
        return Math.pow(sphericalBroadbandAlbedo, computeU(mu_0));
    }

    public static double computeSpectralAlbedo(double brr, double sza, double vza, double saa, double vaa) {
        final double mu_0 = Math.cos(sza * MathUtils.DTOR);
        final double mu = Math.cos(vza * MathUtils.DTOR);
        final double s_0 = Math.sin(sza * MathUtils.DTOR);
        final double s = Math.sin(vza * MathUtils.DTOR);

        final double A = 1.247;
        final double B = 1.186;
        final double C = 5.157;

        final double theta = MathUtils.RTOD * Math.acos(-mu * mu_0 + s * s_0 * Math.cos(Math.abs((vaa - saa)) * MathUtils.DTOR));
        final double p_theta = 11.1 * Math.exp(-0.087 * theta) + 1.1 * Math.exp(-0.014 * theta);
        final double R_0 = (A + B * (mu_0 + mu) + C * mu_0 * mu + p_theta) / (4.0 * (mu_0 + mu));

        final double p = R_0 / (computeU(mu) * computeU(mu_0));

        return Math.pow(brr / R_0, p);
    }

    static double computeGrainDiameter(double r21) {
        final double b = 3.62;
        final double chi = 2.25E-6;
        final double lambda_21 = 1.02;
        final double a_21 = 4.0 * Math.PI * chi / lambda_21;
        return Math.log(r21) * Math.log(r21) / (b * b * a_21);    // this is the grain diameter in microns!!
    }

    static double integrateR_b1(double[] spectralAlbedos) {
        double r_b1 = 0.0;
//        for (int i = 0; i < OlciSnowAlbedoConstants.WAVELENGTH_GRID_OLCI.length; i++) {
//            r_b1 += spectralAlbedos[i] * OlciSnowAlbedoConstants.F_LAMBDA_OLCI[i];
//        }
        // todo: interpolate input spectralAlbedos (14 OLCI wavelengths) to full grid 300-1020nm (53 wavelengths)
        double[] spectralAlbedosInterpolated = new double[OlciSnowAlbedoConstants.WAVELENGTH_GRID_FULL.length];
        int olciIndex = 0;
        for (int i = 0; i < OlciSnowAlbedoConstants.WAVELENGTH_GRID_FULL.length; i++) {
            final double[] wvlFull = OlciSnowAlbedoConstants.WAVELENGTH_GRID_FULL;
            final double[] wvlOlci = OlciSnowAlbedoConstants.WAVELENGTH_GRID_OLCI;
            if (wvlFull[i] < wvlOlci[0]) {
                // 300-400nm
                spectralAlbedosInterpolated[i] = spectralAlbedos[0];
            } else {
                // 400-1028nm
                if (wvlFull[i] > wvlOlci[olciIndex + 1]) {
                    olciIndex++;
                }
                double weight = (wvlFull[i] - wvlOlci[olciIndex]) / (wvlOlci[olciIndex + 1] - wvlOlci[olciIndex]);
                spectralAlbedosInterpolated[i] = spectralAlbedos[olciIndex] +
                        weight * (spectralAlbedos[olciIndex + 1] - spectralAlbedos[olciIndex]);
            }
        }

        for (int i = 0; i < OlciSnowAlbedoConstants.WAVELENGTH_GRID_FULL.length; i++) {
            r_b1 += spectralAlbedosInterpolated[i] * OlciSnowAlbedoConstants.F_LAMBDA_FULL[i];
        }
        return r_b1;
    }

    static double integrateR_b2(double d) {
        final double q0 = 0.0947;
        final double q1 = 0.0569;
        final double d0 = 200.0;
        return q0 - q1 * Math.log10(d / d0);
    }

    private static double computeU(double mu) {
        return 3.0 * (1.0 + 2.0 * mu) / 7.0;
    }

    static class SphericalBroadbandAlbedo {
        double r_b1;
        double r_b2;
        double grainDiameter;

        public double getR_b1() {
            return r_b1;
        }

        public void setR_b1(double r_b1) {
            this.r_b1 = r_b1;
        }

        public double getR_b2() {
            return r_b2;
        }

        public void setR_b2(double r_b2) {
            this.r_b2 = r_b2;
        }

        public double getGrainDiameter() {
            return grainDiameter;
        }

        public void setGrainDiameter(double grainDiameter) {
            this.grainDiameter = grainDiameter;
        }
    }
}
