package org.esa.s3tbx.c2rcc.ancillary;

import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.RasterDataNode;

/**
 * @author Marco Peters
 */
public class RasterAtmosphericAuxdata implements AtmosphericAuxdata {

    private RasterDataNode ozoneRaster;
    private RasterDataNode surfPressureRaster;

    public RasterAtmosphericAuxdata(RasterDataNode ozoneRaster, RasterDataNode surfPressureRaster) {
        if(ozoneRaster.getGeoCoding() == null) {
            throw new IllegalStateException("Raster for ozone must be geo-referenced");
        }
        if(surfPressureRaster.getGeoCoding() == null) {
            throw new IllegalStateException("Raster for surfPressure must be geo-referenced");
        }
        this.ozoneRaster = ozoneRaster;
        this.surfPressureRaster = surfPressureRaster;
    }

    @Override
    public double getOzone(double mjd, int x, int y, double lat, double lon) throws Exception {
        PixelPos pixelPos = ozoneRaster.getGeoCoding().getPixelPos(new GeoPos(lat, lon), null);
        return ozoneRaster.getSampleFloat((int)pixelPos.x, (int)pixelPos.y);
    }

    @Override
    public double getSurfacePressure(double mjd, int x, int y, double lat, double lon) throws Exception {
        return surfPressureRaster.getSampleFloat(x, y);
    }

    @Override
    public void dispose() {

    }
}
