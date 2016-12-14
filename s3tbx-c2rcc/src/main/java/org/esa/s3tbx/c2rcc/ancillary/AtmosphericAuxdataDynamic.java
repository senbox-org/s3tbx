package org.esa.s3tbx.c2rcc.ancillary;

import java.io.IOException;

class AtmosphericAuxdataDynamic implements AtmosphericAuxdata {

    private final DataInterpolatorDynamic ozoneInterpolator;
    private final DataInterpolatorDynamic pressInterpolator;

    public AtmosphericAuxdataDynamic(AncRepository ancRepository, final AncDataFormat ozoneFormat, final AncDataFormat pressFormat) {
        ozoneInterpolator = new DataInterpolatorDynamic(ozoneFormat, ancRepository);
        pressInterpolator = new DataInterpolatorDynamic(pressFormat, ancRepository);
    }

    @Override
    public double getOzone(double mjd, int x, int y, double lat, double lon) throws IOException {
        return ozoneInterpolator.getValue(mjd, lat, lon);
    }

    @Override
    public double getSurfacePressure(double mjd, int x, int y, double lat, double lon) throws IOException {
        return pressInterpolator.getValue(mjd, lat, lon);
    }

    @Override
    public void dispose() {
        ozoneInterpolator.dispose();
        pressInterpolator.dispose();
    }
}
