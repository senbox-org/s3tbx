package org.esa.s3tbx.c2rcc.ancillary;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.SystemUtils;

import java.io.File;
import java.util.Arrays;

/**
 * @author Marco Peters
 */
public class AtmosphericAuxdataBuilder {

    private RasterDataNode ozoneRaster;
    private RasterDataNode surfPressureRaster;
    private String atmosphericAuxDataPath;
    private Product ozoneStartProduct;
    private Product ozoneEndProduct;
    private Product pressureStartProduct;
    private Product pressureEndProduct;

    private double ozone = 330;
    private double surfacePressure = 1000;
    private ProductData.UTC sourceTime = null;

    public void useAtmosphericAuxDataPath(String atmosphericAuxDataPath) {
        this.atmosphericAuxDataPath = atmosphericAuxDataPath;
    }

    public void useNcepCamsProducts(Product pressureStartProduct, Product pressureEndProduct) {
        this.pressureStartProduct = pressureStartProduct;
        this.pressureEndProduct = pressureEndProduct;
    }

    public void setOzone(double ozone) {
        this.ozone = ozone;
    }

    public void setSourceTime(ProductData.UTC sourceTime) {
        this.sourceTime = sourceTime;
    }

    public void setSurfacePressure(double press) {
        this.surfacePressure = press;
    }

    public void useTomsomiCamsProducts(Product ozoneStartProduct, Product ozoneEndProduct) {
        this.ozoneStartProduct = ozoneStartProduct;
        this.ozoneEndProduct = ozoneEndProduct;

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
                if (ozoneStartProduct == null || ozoneEndProduct == null || pressureStartProduct == null || pressureEndProduct == null) {
                    SystemUtils.LOG.info("Atmospheric auxdata product can't be used. At least one is not specified. " +
                            "Using constant values for ozone (" + ozone + ") and surface pressure (" + surfacePressure + ").");
                    auxdata = new ConstantAtmosphericAuxdata(ozone, surfacePressure);
                } else {
                    boolean contains_cams_band_gtco3 = Arrays.stream(ozoneStartProduct.getBandNames()).anyMatch(name -> name.contains("gtco3"));
                    boolean contains_cams_band_msl = Arrays.stream(pressureStartProduct.getBandNames()).anyMatch(name -> name.contains("msl"));
                    String ozoneBandName = contains_cams_band_gtco3 ? "gtco3" : "ozone";
                    String pressureBandName = contains_cams_band_msl ? "msl" : "press";
                    auxdata = new AtmosphericAuxdataStatic(ozoneStartProduct, ozoneEndProduct, ozoneBandName, ozone,
                            pressureStartProduct, pressureEndProduct, pressureBandName, surfacePressure, sourceTime);
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
