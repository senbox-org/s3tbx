package org.esa.s3tbx.c2rcc.ancillary;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.SystemUtils;

import java.io.File;

/**
 * @author Marco Peters
 */
public class AtmosphericAuxdataBuilder {

    private RasterDataNode ozoneRaster;
    private RasterDataNode surfPressureRaster;
    private String atmosphericAuxDataPath;
    private Product tomsomiStartProduct;
    private Product tomsomiEndProduct;
    private Product ncepStartProduct;
    private Product ncepEndProduct;

    private double ozone = 330;
    private double surfacePressure = 1000;

    public void useAtmosphericAuxDataPath(String atmosphericAuxDataPath) {
        this.atmosphericAuxDataPath = atmosphericAuxDataPath;
    }

    public void useNcepProducts(Product ncepStartProduct, Product ncepEndProduct) {
        this.ncepStartProduct = ncepStartProduct;
        this.ncepEndProduct = ncepEndProduct;
    }

    public void setOzone(double ozone) {
        this.ozone = ozone;
    }

    public void setSurfacePressure(double press) {
        this.surfacePressure = press;
    }

    public void useTomsomiProducts(Product tomsomiStartProduct, Product tomsomiEndProduct) {
        this.tomsomiStartProduct = tomsomiStartProduct;
        this.tomsomiEndProduct = tomsomiEndProduct;

    }

    public void useAtmosphericRaster(RasterDataNode ozoneRaster, RasterDataNode surfPressureRaster) {
        this.ozoneRaster = ozoneRaster;
        this.surfPressureRaster = surfPressureRaster;
    }

    public AtmosphericAuxdata create() throws Exception {
        AtmosphericAuxdata auxdata;
        if (ozoneRaster != null && surfPressureRaster != null) {
            auxdata = new RasterAtmosphericAuxdata(ozoneRaster, surfPressureRaster);
        } else {
            if (StringUtils.isNullOrEmpty(atmosphericAuxDataPath)) {
                if (tomsomiStartProduct == null || tomsomiEndProduct == null || ncepStartProduct == null || ncepEndProduct == null) {
                    SystemUtils.LOG.info("Atmospheric auxdata product can't be used. At least one is not specified. " +
                                                 "Using constant values for ozone (" + ozone + ") and surface pressure (" + surfacePressure + ").");
                    auxdata = new ConstantAtmosphericAuxdata(ozone, surfacePressure);
                } else {
                    auxdata = new AtmosphericAuxdataStatic(tomsomiStartProduct, tomsomiEndProduct, "ozone", ozone,
                                                           ncepStartProduct, ncepEndProduct, "press", surfacePressure);
                }
            } else {
                final AncDownloader ancDownloader = new AncDownloader();
                final AncRepository ancRepository = new AncRepository(new File(atmosphericAuxDataPath), ancDownloader);
                AncDataFormat ozoneFormat = AncillaryCommons.createOzoneFormat(ozone);
                AncDataFormat pressureFormat = AncillaryCommons.createPressureFormat(surfacePressure);
                auxdata = new AtmosphericAuxdataDynamic(ancRepository, ozoneFormat, pressureFormat);
            }
        }
        return auxdata;
    }
}
