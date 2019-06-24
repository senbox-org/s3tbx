package org.esa.s3tbx.olci.harmonisation.kdinterpolation;

import org.junit.Test;
import smile.neighbor.KDTree;
import smile.neighbor.Neighbor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class KDTreeTest {

    @Test
    public void testKDTree() {

        // some weird test data, following what is finally going into Python breadboard
        // kd_interpolator.py, function lut2func_pre(DATA), line:
        //       TREE= KDTree(DATA['X'], leafsize=DATA['leafsize'])
        // which is called from 'test_mini_lut2func_pre()'
        // The coords below are equal to DATA['X'].
        //      dist, indx = TREE.query(wo, k=max(1,n_nearest),n_jobs=n_jobs)
        //
        double[][] coords = new double[72][3];
        coords[0] = new double[]{-1.70815931, -1.70815931, -1.70815931};
        coords[1] = new double[]{-1.66004214, -1.66004214, -1.66004214};
        coords[2] = new double[]{-1.61192498, -1.61192498, -1.61192498};
        coords[3] = new double[]{-1.56380782, -1.56380782, -1.56380782};
        coords[4] = new double[]{-1.51569065, -1.51569065, -1.51569065};
        coords[5] = new double[]{-1.46757349, -1.46757349, -1.46757349};
        coords[6] = new double[]{-1.41945633, -1.41945633, -1.41945633};
        coords[7] = new double[]{-1.37133916, -1.37133916, -1.37133916};
        coords[8] = new double[]{-1.323222, -1.323222, -1.323222};
        coords[9] = new double[]{-1.27510484, -1.27510484, -1.27510484};
        coords[10] = new double[]{-1.22698767, -1.22698767, -1.22698767};
        coords[11] = new double[]{-1.17887051, -1.17887051, -1.17887051};
        coords[12] = new double[]{-1.13075334, -1.13075334, -1.13075334};
        coords[13] = new double[]{-1.08263618, -1.08263618, -1.08263618};
        coords[14] = new double[]{-1.03451902, -1.03451902, -1.03451902};
        coords[15] = new double[]{-0.98640185, -0.98640185, -0.98640185};
        coords[16] = new double[]{-0.93828469, -0.93828469, -0.93828469};
        coords[17] = new double[]{-0.89016753, -0.89016753, -0.89016753};
        coords[18] = new double[]{-0.84205036, -0.84205036, -0.84205036};
        coords[19] = new double[]{-0.7939332, -0.7939332, -0.7939332};
        coords[20] = new double[]{-0.74581604, -0.74581604, -0.74581604};
        coords[21] = new double[]{-0.69769887, -0.69769887, -0.69769887};
        coords[22] = new double[]{-0.64958171, -0.64958171, -0.64958171};
        coords[23] = new double[]{-0.60146454, -0.60146454, -0.60146454};
        coords[24] = new double[]{-0.55334738, -0.55334738, -0.55334738};
        coords[25] = new double[]{-0.50523022, -0.50523022, -0.50523022};
        coords[26] = new double[]{-0.45711305, -0.45711305, -0.45711305};
        coords[27] = new double[]{-0.40899589, -0.40899589, -0.40899589};
        coords[28] = new double[]{-0.36087873, -0.36087873, -0.36087873};
        coords[29] = new double[]{-0.31276156, -0.31276156, -0.31276156};
        coords[30] = new double[]{-0.2646444, -0.2646444, -0.2646444};
        coords[31] = new double[]{-0.21652724, -0.21652724, -0.21652724};
        coords[32] = new double[]{-0.16841007, -0.16841007, -0.16841007};
        coords[33] = new double[]{-0.12029291, -0.12029291, -0.12029291};
        coords[34] = new double[]{-0.07217575, -0.07217575, -0.07217575};
        coords[35] = new double[]{-0.02405858, -0.02405858, -0.02405858};
        coords[36] = new double[]{0.02405858, 0.02405858, 0.02405858};
        coords[37] = new double[]{0.07217575, 0.07217575, 0.07217575};
        coords[38] = new double[]{0.12029291, 0.12029291, 0.12029291};
        coords[39] = new double[]{0.16841007, 0.16841007, 0.16841007};
        coords[40] = new double[]{0.21652724, 0.21652724, 0.21652724};
        coords[41] = new double[]{0.2646444, 0.2646444, 0.2646444};
        coords[42] = new double[]{0.31276156, 0.31276156, 0.31276156};
        coords[43] = new double[]{0.36087873, 0.36087873, 0.36087873};
        coords[44] = new double[]{0.40899589, 0.40899589, 0.40899589};
        coords[45] = new double[]{0.45711305, 0.45711305, 0.45711305};
        coords[46] = new double[]{0.50523022, 0.50523022, 0.50523022};
        coords[47] = new double[]{0.55334738, 0.55334738, 0.55334738};
        coords[48] = new double[]{0.60146454, 0.60146454, 0.60146454};
        coords[49] = new double[]{0.64958171, 0.64958171, 0.64958171};
        coords[50] = new double[]{0.69769887, 0.69769887, 0.69769887};
        coords[51] = new double[]{0.74581604, 0.74581604, 0.74581604};
        coords[52] = new double[]{0.7939332, 0.7939332, 0.7939332};
        coords[53] = new double[]{0.84205036, 0.84205036, 0.84205036};
        coords[54] = new double[]{0.89016753, 0.89016753, 0.89016753};
        coords[55] = new double[]{0.93828469, 0.93828469, 0.93828469};
        coords[56] = new double[]{0.98640185, 0.98640185, 0.98640185};
        coords[57] = new double[]{1.03451902, 1.03451902, 1.03451902};
        coords[58] = new double[]{1.08263618, 1.08263618, 1.08263618};
        coords[59] = new double[]{1.13075334, 1.13075334, 1.13075334};
        coords[60] = new double[]{1.17887051, 1.17887051, 1.17887051};
        coords[61] = new double[]{1.22698767, 1.22698767, 1.22698767};
        coords[62] = new double[]{1.27510484, 1.27510484, 1.27510484};
        coords[63] = new double[]{1.323222, 1.323222, 1.323222};
        coords[64] = new double[]{1.37133916, 1.37133916, 1.37133916};
        coords[65] = new double[]{1.41945633, 1.41945633, 1.41945633};
        coords[66] = new double[]{1.46757349, 1.46757349, 1.46757349};
        coords[67] = new double[]{1.51569065, 1.51569065, 1.51569065};
        coords[68] = new double[]{1.56380782, 1.56380782, 1.56380782};
        coords[69] = new double[]{1.61192498, 1.61192498, 1.61192498};
        coords[70] = new double[]{1.66004214, 1.66004214, 1.66004214};
        coords[71] = new double[]{1.70815931, 1.70815931, 1.70815931};

        KDTree<double[]> kdtree;
        kdtree = new KDTree<>(coords, coords);

        // coord_1 represents 'wo' passed into
        //      dist, indx = TREE.query(wo, k=max(1,n_nearest),n_jobs=n_jobs)
        // as called from 'test_mini_lut2func_pre()' with fnc(x[0]*1.005)
        final double[] coord_1 = {-1.70815930732, -1.70811119015, -1.70806307299};
        final Neighbor<double[], double[]>[] nearest_1 = kdtree.knn(coord_1, 8);
        assertNotNull(nearest_1);
        assertEquals(8, nearest_1.length);

        // NOTE: neighbour indices are provided in opposite order compared to Python TREE.query used by RP!
        assertEquals(7, nearest_1[0].index);
        assertEquals(0.583306266932, nearest_1[0].distance, 1.E-6);
        assertEquals(6, nearest_1[1].index);
        assertEquals(0.499964895546, nearest_1[1].distance, 1.E-6);
        assertEquals(5, nearest_1[2].index);
        assertEquals(0.416623524424, nearest_1[2].distance, 1.E-6);
        assertEquals(4, nearest_1[3].index);
        assertEquals(0.333282153766, nearest_1[3].distance, 1.E-6);
        assertEquals(3, nearest_1[4].index);
        assertEquals(0.249940784035, nearest_1[4].distance, 1.E-6);
        assertEquals(2, nearest_1[5].index);
        assertEquals(0.166599416621, nearest_1[5].distance, 1.E-6);
        assertEquals(1, nearest_1[6].index);
        assertEquals(0.083258058484, nearest_1[6].distance, 1.E-6);
        assertEquals(0, nearest_1[7].index);
        assertEquals(0.000107593248, nearest_1[7].distance, 1.E-6);
    }
}