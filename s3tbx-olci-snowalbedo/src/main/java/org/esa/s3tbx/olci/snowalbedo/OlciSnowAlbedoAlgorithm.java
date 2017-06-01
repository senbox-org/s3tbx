package org.esa.s3tbx.olci.snowalbedo;

import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
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

    /**
     * Computes snow spectral albedo for considered wavelengths from input reflectances (after AC) and given geometry.
     * Follows 'snow_albedo_algorithm_1.docx' (AK, 20170519)
     *
     * @param brr - Rayleigh corrected reflectance at considered wavelengths (see {@link OlciSnowAlbedoConstants })
     * @param sza - sun zenith angle
     * @param vza - view zenith angle
     * @param saa - sun azimuth angle
     * @param vaa - view azimuth angle
     *
     * @return  array of snow spectral albedos
     */
    static double[] computeSpectralSphericalAlbedos(double[] brr, double sza, double vza, double saa, double vaa) {
        double[] spectralSphericalAlbedos = new double[brr.length];
        for (int i = 0; i < spectralSphericalAlbedos.length; i++) {
            spectralSphericalAlbedos[i] = computeSpectralAlbedo(brr[i], sza, vza, saa, vaa);
        }
        return spectralSphericalAlbedos;
    }

    /**
     * Computes snow spectral albedo value from input reflectance value (after AC) and given geometry.
     * Follows 'snow_albedo_algorithm_1.docx' (AK, 20170519)
     *
     * @param brr - Rayleigh corrected reflectance at considered wavelengths (see {@link OlciSnowAlbedoConstants })
     * @param sza - sun zenith angle
     * @param vza - view zenith angle
     * @param saa - sun azimuth angle
     * @param vaa - view azimuth angle
     *
     * @return  the snow spectral albedo value
     */
    static double computeSpectralAlbedo(double brr, double sza, double vza, double saa, double vaa) {
        // eq. (8):
        final double mu_0 = Math.cos(sza * MathUtils.DTOR);
        final double mu = Math.cos(vza * MathUtils.DTOR);
        final double s_0 = Math.sin(sza * MathUtils.DTOR);
        final double s = Math.sin(vza * MathUtils.DTOR);
        final double theta = MathUtils.RTOD *
                Math.acos(-mu * mu_0 + s * s_0 * Math.cos(Math.abs((vaa - saa)) * MathUtils.DTOR));
        // eq. (7):
        final double p_theta = 11.1 * Math.exp(-0.087 * theta) + 1.1 * Math.exp(-0.014 * theta);
        // eq. (6):
        final double A = 1.247;
        final double B = 1.186;
        final double C = 5.157;
        final double R_0 = (A + B * (mu_0 + mu) + C * mu_0 * mu + p_theta) / (4.0 * (mu_0 + mu));
        // eq. (3):
        final double p = R_0 / (computeU(mu) * computeU(mu_0));

        // eq. (1):
        return Math.pow(brr / R_0, p);
    }

    /**
     * Computes planar from spherical albedos at considered wavelengths.
     * Follows 'snow_albedo_algorithm_1.docx' (AK, 20170519).
     *
     * @param sphericalAlbedos - the spherical albedos at considered wavelengths
     * @param sza - sun zenith angle
     *
     * @return  array of planar albedos
     */
    static double[] computePlanarFromSphericalAlbedos(double[] sphericalAlbedos, double sza) {
        double[] planarAlbedos = new double[sphericalAlbedos.length];
        for (int i = 0; i < planarAlbedos.length; i++) {
            planarAlbedos[i] = computePlanarFromSphericalAlbedo(sphericalAlbedos[i], sza);
        }
        return planarAlbedos;
    }

    /**
     * Computes planar from spherical albedos at considered wavelengths.
     * Follows 'snow_albedo_algorithm_1.docx' (AK, 20170519)
     *
     * @param sphericaAlbedo - the spherical albedo value
     * @param sza - sun zenith angle
     *
     * @return  theplanar albedo value
     */
    static double computePlanarFromSphericalAlbedo(double sphericaAlbedo,
                                                   double sza) {
        // eq. (5):
        final double mu_0 = Math.cos(sza * MathUtils.DTOR);
        return Math.pow(sphericaAlbedo, computeU(mu_0));
    }

    /**
     * Computes spherical broadband albedo terms (b_1, b_2 integrals, grain diameter).
     * Follows 'algorithm__BROADBAND_SPHERICAL_ALBEDO.docx' (AK, 20170530)
     *
     * @param sphericalSpectralAlbedos - the spherical spectral albedos at considered wavelengths.
     * @param brr21 - Rayleigh corrected reflectance at band 21 (1020nm)
     *
     * @return  SphericalBroadbandAlbedo object holding b_1, b_2 integrals and grain diameter.
     */
    static SphericalBroadbandAlbedo computeSphericalBroadbandAlbedoTerms(double[] sphericalSpectralAlbedos, double brr21) {
        SphericalBroadbandAlbedo sbba = new SphericalBroadbandAlbedo();
        sbba.setR_b1(integrateR_b1(sphericalSpectralAlbedos));
        final double grainDiameter = computeGrainDiameter(brr21);
        sbba.setGrainDiameter(grainDiameter);
        sbba.setR_b2(integrateR_b2(grainDiameter));

        return sbba;
    }

    /**
     * Computes the snow grain diameter for given Rayleigh corrected reflectance at band 21 (1020nm).
     * Follows 'algorithm__BROADBAND_SPHERICAL_ALBEDO.docx' (AK, 20170530)
     *
     * @param brr21 - Rayleigh corrected reflectance at band 21 (1020nm)
     *
     * @return the snow grain diameter in microns
     */
    static double computeGrainDiameter(double brr21) {
        // eq. (5):
        final double b = 3.62;
        final double chi = 2.25E-6;
        final double lambda_21 = 1.02;
        final double a_21 = 4.0 * Math.PI * chi / lambda_21;

        return Math.log(brr21) * Math.log(brr21) / (b * b * a_21);    // this is the grain diameter in microns!!
    }

    /**
     * Computes the 'r_b1' term for broadband snow albedo
     * Follows 'algorithm__BROADBAND_SPHERICAL_ALBEDO.docx' (AK, 20170530)
     *
     * @param spectralAlbedos - the spectral albedos at considered wavelengths.
     *
     * @return r_b1
     */
    static double integrateR_b1(double[] spectralAlbedos) {
        double r_b1 = 0.0;
        // interpolate input spectralAlbedos (14 OLCI wavelengths) to full grid 300-1020nm (53 wavelengths)
        final double[] wvlFull = OlciSnowAlbedoConstants.WAVELENGTH_GRID_FULL;
        final double[] wvlOlci = OlciSnowAlbedoConstants.WAVELENGTH_GRID_OLCI;
        // we need to extrapolate array to 300nm on the lower side:
        double[] spectralAlbedos15 = new double[spectralAlbedos.length+1];
        spectralAlbedos15[0] = spectralAlbedos[0];
        System.arraycopy(spectralAlbedos, 0, spectralAlbedos15, 1, spectralAlbedos.length);
        final double[] spectralAlbedosInterpolated = interpolateSpectralAlbedos(wvlOlci, spectralAlbedos15, wvlFull);
        // integration: eq. (A.1) with f_lambda from Table (A.2)
        for (int i = 0; i < OlciSnowAlbedoConstants.WAVELENGTH_GRID_FULL.length; i++) {
            r_b1 += spectralAlbedosInterpolated[i] * OlciSnowAlbedoConstants.F_LAMBDA_FULL[i];
        }
        return r_b1;
    }

    /**
     * Interpolates array of spectral albedos (here: 14 OLCI) to coarser grid (here 53 wavelengths 300-1020nm).
     * Follows 'algorithm__BROADBAND_SPHERICAL_ALBEDO.docx' (AK, 20170530)
     *
     * @param x - array of x values (OLCI wavelengths)
     * @param y - array of spectral albedos at OLCI wavelengths
     * @param xi - array of 53 wavelengths 300-1200nm
     *
     * @return array of spectral albedos at 53 wavelengths 300-1200nm
     */
    static double[] interpolateSpectralAlbedos(double[] x, double[] y, double[] xi) {
        final LinearInterpolator linearInterpolator = new LinearInterpolator();
        PolynomialSplineFunction psf = linearInterpolator.interpolate(x, y);

        double[] yi = new double[xi.length];
        for (int i = 0; i < xi.length; i++) {
            yi[i] = psf.value(xi[i]);
        }
        return yi;
    }

    /**
     * Computes the 'r_b2' term for broadband snow albedo
     * Follows 'algorithm__BROADBAND_SPHERICAL_ALBEDO.docx' (AK, 20170530)
     *
     * @param d - the snow grain diameter.
     *
     * @return r_b2
     */
    static double integrateR_b2(double d) {
        // eq. (4):
        final double q0 = 0.0947;
        final double q1 = 0.0569;
        final double d0 = 200.0;
        return q0 - q1 * Math.log10(d / d0);
    }

    private static double computeU(double mu) {
        return 3.0 * (1.0 + 2.0 * mu) / 7.0;
    }

    /**
     * Container object for spherical broadband albedo terms (b_1, b_2 integrals, grain diameter).
     */
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
