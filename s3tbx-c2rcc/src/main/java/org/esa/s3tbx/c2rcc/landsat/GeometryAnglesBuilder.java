package org.esa.s3tbx.c2rcc.landsat;

class GeometryAnglesBuilder {

    private final int subsampling_x;
    private final int x_off;
    private final int x_c;
    private final double zenith_wink_fak;

//    private final double sun_azimuth;
//    private double cos_sun_zenith;
//    private double sin_sun_zenith;

    GeometryAnglesBuilder(int subsampling_x, int x_off, int x_c, double sun_azimuth, double sun_zenith) {
        this.subsampling_x = subsampling_x;
        this.x_off = x_off;
        this.x_c = x_c;
        this.zenith_wink_fak = 7.0 / this.x_c;
//        this.sun_azimuth = sun_azimuth;
        double sun_zenith_rad = Math.toRadians(sun_zenith);
//        this.cos_sun_zenith = Math.cos(sun_zenith_rad);
//        this.sin_sun_zenith = Math.sin(sun_zenith_rad);
    }

    GeometryAngles getGeometryAngles(double xb, double latitude) {
        return getGeometryAngles(xb, latitude, -1);
    }

    private GeometryAngles getGeometryAngles(double xb, double latitude, double yb) {
        GeometryAngles geomAngels = new GeometryAngles();
        double a = Math.toRadians(latitude);
        double alpha = 98.2; // Landsat 8 inclination
        double alpha_rad = Math.toRadians(alpha);
        double cos_alpha = Math.cos(alpha_rad);
        double sin_beta = cos_alpha / (Math.sin(Math.PI / 2 - a));
        double beta_rad = Math.asin(sin_beta);
        double beta_deg = Math.toDegrees(beta_rad);

        // double bb = y_c - (yb * subsampling_y + y_off);
        geomAngels.ab = xb * subsampling_x + x_off - x_c;
        // double cb = Math.sqrt(Math.pow(ab, 2) + Math.pow(bb, 2));
        // alpha_rad = Math.acos(bb / cb);
        // double db = Math.asin(beta_rad + alpha_rad) * cb;
        geomAngels.view_zenith = geomAngels.ab * zenith_wink_fak;
        // viewing left of ascending path, definition when standing on pixel looking to sensor
        if (geomAngels.ab < 0.0) {
            geomAngels.view_azimuth = beta_deg + 90.0;
        } else {
            // right of image
            geomAngels.view_azimuth = beta_deg + 270;
        }

        // duplicated - it is also computed in the algorithm part.
//        geomAngels.cos_sun = cos_sun_zenith;
//        geomAngels.sin_sun = sin_sun_zenith;
//        geomAngels.cos_view = Math.cos(Math.toRadians(geomAngels.view_zenith));
//        geomAngels.sin_view = Math.sin(Math.toRadians(geomAngels.view_zenith));

//        geomAngels.cos_azi_diff = Math.cos(Math.toRadians(geomAngels.view_azimuth - sun_azimuth));
//        double azi_diff_rad = Math.acos(Math.cos(Math.toRadians(geomAngels.view_azimuth - sun_azimuth)));
//        geomAngels.sin_azi_diff = Math.sin(azi_diff_rad);
//        geomAngels.azi_diff_deg = Math.toDegrees(azi_diff_rad);
//
//        geomAngels.x = geomAngels.sin_view * geomAngels.cos_azi_diff;
//        geomAngels.y = geomAngels.sin_view * geomAngels.sin_azi_diff;
//        geomAngels.z = geomAngels.cos_view;

        return geomAngels;
    }

}
