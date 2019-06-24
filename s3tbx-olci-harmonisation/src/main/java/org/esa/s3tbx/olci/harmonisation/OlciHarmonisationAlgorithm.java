package org.esa.s3tbx.olci.harmonisation;

import smile.neighbor.KDTree;
import smile.neighbor.Neighbor;

/**
 * Class providing the algorithm for OLCI Harmonisation
 *
 * @author olafd
 */
public class OlciHarmonisationAlgorithm {

    /**
     * This calculates the 1to1 transmission  ('rectified') (Zenith Sun --> Nadir observation: amf=2) at
     * given band, which would be measured without scattering. Useful only for comparison.
     *
     * @param press      - input pressure
     * @param bandIndex  - index of given band
     * @return the rectified transmission
     */
    public static double press2Trans(double press, int bandIndex) {
        final double[] p = OlciHarmonisationConstants.pCoeffsPress2Tra[bandIndex - 13];
        double pressPolynom = p[0] + p[1] * press + p[2] * press * press;
        return Math.exp(-pressPolynom);
    }

    /**
     * This calculates the pressure given a 1to1 transmission ('rectified') (Zenith Sun --> Nadir
     * observation: amf=2), which would be measured in given band without scattering. Useful for a
     * first object height estimation (but *not* for dark targets (ocean!!!!))
     *
     * @param trans_rectified - input rectified transmission
     * @param bandIndex      - index of given band
     * @return the pressure in hPa
     */
    public static double trans2Press(double trans_rectified, int bandIndex) {
        final double[] p = OlciHarmonisationConstants.pCoeffsTra2Press[bandIndex - 13];
        return p[0] + p[1] * trans_rectified + p[2] * trans_rectified * trans_rectified;
    }

    /**
     * Provides a simple estimate for pressure from given height.
     *
     * @param height - height in m
     * @param slp - sea level pressure
     * @return pressure in hPa
     */
    public static double height2press(double height, double slp) {
        return slp * Math.pow((1.0 - (height * 0.0065 / 288.15)), 5.2555);
    }

    public static float overcorrectLambda(float cam, double[] dwvl) {
        double delta = 0.0;
        for (int i = 0; i < 5; i++) {
            delta = (cam == i + 1) ? dwvl[i] : 0.0;
        }

        return (float) delta;
    }

    /**
     * Desmile input transmission using interpolation of Desmile LUT, using KD search.
     * Java version (simplified) of 'lut2func_internal' in kd_interpolator.py of RP Python breadboard.
     *
     * @param dwl   - central wavelength
     * @param fwhm  - band width (full width at half maximum)
     * @param amf   - air mass factor
     * @param trans - original transmission
     * @param tree  - the KD Tree. Should have been once initialized at earlier stage.
     * @param lut   - the desmile LUT held in DesmileLut object. Should have been once initialized at earlier stage.
     * @return trans_desmiled
     */
    public static double desmileTransmission(double dwl, double fwhm, double amf, double trans,
                                             KDTree<double[]> tree, DesmileLut lut) {

        double[] x = new double[]{dwl, fwhm, trans, amf};
        final double[] wo = new double[lut.getVARI().length];
        for (int i = 0; i < lut.getVARI().length; i++) {
            wo[i] = (x[i] - lut.getMEAN()[i]) / lut.getVARI()[i];
        }

        final int nNearest = 1;   // see Python: func(x, 1) !!!
        final Neighbor<double[], double[]>[] neighbors = tree.knn(wo, nNearest);
        final double[] distances = new double[neighbors.length];
        final int[] indices = new int[neighbors.length];
        for (int i = 0; i < distances.length; i++) {
            distances[i] = neighbors[i].distance;
            indices[distances.length - i - 1] = neighbors[i].index;
        }

        double[] weight = new double[distances.length];
        double norm = 0.0;
        for (int i = 0; i < weight.length; i++) {
            weight[i] = Double.isInfinite(distances[i]) ? 0.0 : 1.0;
            norm += weight[i];
        }

        double temp = 0.0;
        for (int j = 0; j < nNearest; j++) {
            final boolean valid = !Double.isInfinite(distances[j]);
            if (valid) {
                double dxCrossJaco = 0.0;
                for (int k = 0; k < wo.length; k++) {
                    final double dx = (wo[k] - lut.getX()[indices[j]][k]) * lut.getVARI()[k];
                    final double jaco = lut.getJACO()[indices[j]][0][k];
                    dxCrossJaco += (dx * jaco);
                }
                temp += (lut.getY()[indices[j]][0] + dxCrossJaco * weight[j]);
            }
        }

        double kdInterpolResult = temp / norm;

        return trans / kdInterpolResult;
    }

    /**
     * Rectifies input desmiled transmission.
     * Java version of 'generate_tra2recti' in o2corr__io_v3.py of RP Python breadboard.
     *
     * @param trans_desmiled - desmiled transmission
     * @param amf            - air mass factor
     * @param bandIndex      - band index
     * @return trans_rectified
     */
    public static double rectifyDesmiledTransmission(double trans_desmiled, double amf, int bandIndex) {
        final double tau = Math.log(trans_desmiled);
        final double amfM = amf - 2.0;
        final double[] p = OlciHarmonisationConstants.pCoeffsRectification[bandIndex - 13];

        final double rectifyFactor = p[0] + p[1] * tau + p[2] * tau * tau + p[3] * amfM + p[4] * amfM * amfM +
                p[5] * tau * Math.sqrt(amfM) + p[7] * trans_desmiled;

        return trans_desmiled / rectifyFactor;
    }
}
