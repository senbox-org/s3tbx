package org.esa.s3tbx.c2rcc.util;

import org.esa.s3tbx.c2rcc.msi.C2rccMsiAlgorithm;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.ProductUtils;

import java.util.HashMap;
import java.util.Map;


/**
 * @author Marco Peters
 */
public class TargetProductConfigurer {

    private static final String SOURCE_RHOT_NAME_PREFIX = "rhot_";
    private static final String SOURCE_RADIANCE_NAME_PREFIX = "radiance_";

    private static final int[] MODIS_TARGET_REFLEC_WAVELENGTHS = {412, 443, 488, 531, 547, 667, 678, 748, 869};
    private static final int[] SEAWIFS_TARGET_REFLEC_WAVELENGTHS = {412, 443, 490, 510, 555, 670, 765, 865};
    private static final int[] MERIS_TARGET_REFLEC_IDX = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 13};
    private static final int[] MERIS_SOURCE_RADIANCE_IDX = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
    private static final int[] OLCI_TARGET_REFLEC_IDX = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 16, 17, 18, 21};
    private static final int[] OLCI_SOURCE_RADIANCE_IDX = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21};
    private static String[] MSI_SOURCE_BAND_REFL_NAMES = new String[]{"B1", "B2", "B3", "B4", "B5", "B6", "B7", "B8", "B8A", "B9", "B10", "B11", "B12",};
    private static String[] MSI_TARGET_BAND_REFL_NAMES = new String[]{"B1", "B2", "B3", "B4", "B5", "B6", "B7", "B8A",};


    private static final String TARGET_RASTER_NAME_PREFIX_RHOW_USA = "reflec";
    private static final String TARGET_RASTER_NAME_PREFIX_RHOW_ESA = "rwa";

    private static float[] DEFAULT_MERIS_WAVELENGTH = new float[]{
       /*  1 */     412.691f,
       /*  2 */     442.55902f,
       /*  3 */     489.88202f,
       /*  4 */     509.81903f,
       /*  5 */     559.69403f,
       /*  6 */     619.601f,
       /*  7 */     664.57306f,
       /*  8 */     680.82104f,
       /*  9 */     708.32904f,
       /* 10 */     753.37103f,
       /* 11 */     761.50806f,
       /* 12 */     778.40906f,
       /* 13 */     864.87604f,
       /* 14 */     884.94403f,
       /* 15 */     900.00006f
    };
    private static float[] DEFAULT_OLCI_WAVELENGTH = new float[]{
      /*  1 */   400f,
      /*  2 */   412.5f,
      /*  3 */   442.5f,
      /*  4 */   490f,
      /*  5 */   510f,
      /*  6 */   560f,
      /*  7 */   620f,
      /*  8 */   665f,
      /*  9 */   673.75f,
      /* 10 */   681.25f,
      /* 11 */   708.75f,
      /* 12 */   753.75f,
      /* 13 */   761.25f,
      /* 14 */   764.375f,
      /* 15 */   767.5f,
      /* 16 */   778.75f,
      /* 17 */   865f,
      /* 18 */   885f,
      /* 19 */   900f,
      /* 20 */   940f,
      /* 21 */   1020f
    };
    private static float[] DEFAULT_MSI_WAVELENGTH = new float[]{
      /*  1 */   443,
      /*  2 */   490f,
      /*  3 */   560f,
      /*  4 */   665f,
      /*  5 */   705f,
      /*  6 */   740f,
      /*  7 */   783f,
      /*  8 */   842f,
      /*  9 */   865f,
      /* 10 */   945f,
      /* 11 */   1375f,
      /* 12 */   1610f,
      /* 13 */   2190f,
    };


    private static final Map<String, Float> SOURCE_WAVELENGTH_MAP = new HashMap<>();

    static {
        for (int modisTargetReflecWavelength : MODIS_TARGET_REFLEC_WAVELENGTHS) {
            SOURCE_WAVELENGTH_MAP.put(SOURCE_RHOT_NAME_PREFIX + modisTargetReflecWavelength, (float) modisTargetReflecWavelength);
        }
        for (int seawifsTargetReflecWavelength : SEAWIFS_TARGET_REFLEC_WAVELENGTHS) {
            SOURCE_WAVELENGTH_MAP.put(SOURCE_RHOT_NAME_PREFIX + seawifsTargetReflecWavelength, (float) seawifsTargetReflecWavelength);
        }
        for (int i = 0; i < MERIS_SOURCE_RADIANCE_IDX.length; i++) {
            int merisIdx = MERIS_SOURCE_RADIANCE_IDX[i];
            float wvl = DEFAULT_MERIS_WAVELENGTH[i];
            SOURCE_WAVELENGTH_MAP.put(SOURCE_RADIANCE_NAME_PREFIX + merisIdx, wvl);
        }
        for (int i = 0; i < OLCI_SOURCE_RADIANCE_IDX.length; i++) {
            int olciIdx = OLCI_SOURCE_RADIANCE_IDX[i];
            float wvl = DEFAULT_OLCI_WAVELENGTH[i];
            SOURCE_WAVELENGTH_MAP.put(String.format("Oa%02d_radiance", olciIdx), wvl);
        }
        for (int i = 0; i < MSI_SOURCE_BAND_REFL_NAMES.length; i++) {
            int olciIdx = OLCI_SOURCE_RADIANCE_IDX[i];
            float wvl = DEFAULT_MSI_WAVELENGTH[i];
            SOURCE_WAVELENGTH_MAP.put(MSI_SOURCE_BAND_REFL_NAMES[i], wvl);
        }

    }

    private final String sensor;
    private final Product sourceProduct;
    private String validPixelExpression;

    private boolean outputRwa;

    public TargetProductConfigurer(String sensor, Product sourceProduct) {
        this.sensor = sensor;
        this.sourceProduct = sourceProduct;
    }

    public void configure(Product targetProduct) {
        ProductUtils.copyMetadata(sourceProduct, targetProduct);
        final StringBuilder autoGrouping = new StringBuilder("iop");
        if (outputRwa) {
            addReflectances(targetProduct, autoGrouping);
        }
    }

    public void setValidPixelExpression(String validPixelExpression) {
        this.validPixelExpression = validPixelExpression;
    }

    public void setOutputRwa(boolean outputRwa) {
        this.outputRwa = outputRwa;
    }

    private void addReflectances(Product targetProduct, StringBuilder autoGrouping) {
        switch (sensor) {
            case "modis":
                for (int i = 0; i < MODIS_TARGET_REFLEC_WAVELENGTHS.length; i++) {
                    int idx_wl = MODIS_TARGET_REFLEC_WAVELENGTHS[i];
                    String targetBandName = TARGET_RASTER_NAME_PREFIX_RHOW_USA + "_" + idx_wl;
                    String sourceBandName = SOURCE_RHOT_NAME_PREFIX + idx_wl;
                    Float fallBackSpectralWavelength = SOURCE_WAVELENGTH_MAP.get(sourceBandName);

                    Band reflecBand = addBand(targetProduct, targetBandName, "1", "Angular dependent water leaving reflectances");
                    ProductUtils.copySpectralBandProperties(sourceProduct.getBand(sourceBandName), reflecBand);
                    if (reflecBand.getSpectralWavelength() == 0) {
                        reflecBand.setSpectralWavelength(SOURCE_WAVELENGTH_MAP.get(sourceBandName));
                        reflecBand.setSpectralBandIndex(i);
                    }
                    reflecBand.setValidPixelExpression(validPixelExpression);
                }
                autoGrouping.append(":" + TARGET_RASTER_NAME_PREFIX_RHOW_USA);
                break;
            case "seawifs":
                for (int i = 0; i < SEAWIFS_TARGET_REFLEC_WAVELENGTHS.length; i++) {
                    int idx_wl = SEAWIFS_TARGET_REFLEC_WAVELENGTHS[i];
                    String targetBandName = TARGET_RASTER_NAME_PREFIX_RHOW_USA + "_" + idx_wl;
                    String sourceBandName = SOURCE_RHOT_NAME_PREFIX + idx_wl;
                    Float fallBackSpectralWavelength = SOURCE_WAVELENGTH_MAP.get(sourceBandName);

                    Band reflecBand = addBand(targetProduct, targetBandName, "1", "Angular dependent water leaving reflectances");
                    ProductUtils.copySpectralBandProperties(sourceProduct.getBand(sourceBandName), reflecBand);
                    if (reflecBand.getSpectralWavelength() == 0) {
                        reflecBand.setSpectralWavelength(fallBackSpectralWavelength);
                        reflecBand.setSpectralBandIndex(i);
                    }
                    reflecBand.setValidPixelExpression(validPixelExpression);
                }
                autoGrouping.append(":" + TARGET_RASTER_NAME_PREFIX_RHOW_USA);
                break;
            case "meris":
                for (int i = 0; i < MERIS_TARGET_REFLEC_IDX.length; i++) {
                    int index = MERIS_TARGET_REFLEC_IDX[i];
                    String targetBandName = TARGET_RASTER_NAME_PREFIX_RHOW_ESA + "_" + index;
                    String sourceBandName = SOURCE_RADIANCE_NAME_PREFIX + index;
                    Float fallBackSpectralWavelength = SOURCE_WAVELENGTH_MAP.get(sourceBandName);

                    final Band reflecBand = addBand(targetProduct, targetBandName, "1", "Angular dependent water leaving reflectances");
                    ProductUtils.copySpectralBandProperties(sourceProduct.getBand(sourceBandName), reflecBand);
                    if (reflecBand.getSpectralWavelength() == 0) {
                        reflecBand.setSpectralWavelength(fallBackSpectralWavelength);
                        reflecBand.setSpectralBandIndex(i);
                    }
                    reflecBand.setValidPixelExpression(validPixelExpression);
                }
                autoGrouping.append(":" + TARGET_RASTER_NAME_PREFIX_RHOW_ESA);
                break;
            case "olci":
                for (int i = 0; i < OLCI_TARGET_REFLEC_IDX.length; i++) {
                    int index = OLCI_TARGET_REFLEC_IDX[i];
                    String targetBandName = TARGET_RASTER_NAME_PREFIX_RHOW_ESA + "_" + index;
                    String sourceBandName = SOURCE_RADIANCE_NAME_PREFIX + index;
                    Float fallBackSpectralWavelength = SOURCE_WAVELENGTH_MAP.get(sourceBandName);

                    final Band reflecBand = addBand(targetProduct, targetBandName, "1", "Angular dependent water leaving reflectances");
                    ProductUtils.copySpectralBandProperties(sourceProduct.getBand(sourceBandName), reflecBand);
                    if (reflecBand.getSpectralWavelength() == 0) {
                        reflecBand.setSpectralWavelength(fallBackSpectralWavelength);
                        reflecBand.setSpectralBandIndex(i);
                    }
                    reflecBand.setValidPixelExpression(validPixelExpression);
                }
                autoGrouping.append(":" + TARGET_RASTER_NAME_PREFIX_RHOW_ESA);
            case "msi":
                for (int i = 0; i < MSI_TARGET_BAND_REFL_NAMES.length; i++) {
                    String sourceBandName = MSI_TARGET_BAND_REFL_NAMES[i];
                    String targetBandName = TARGET_RASTER_NAME_PREFIX_RHOW_ESA + "_" + sourceBandName;
                    Float fallBackSpectralWavelength = SOURCE_WAVELENGTH_MAP.get(sourceBandName);

                    final Band reflecBand = addBand(targetProduct, targetBandName, "1", "Angular dependent water leaving reflectances");
                    ProductUtils.copySpectralBandProperties(sourceProduct.getBand(sourceBandName), reflecBand);
                    if (reflecBand.getSpectralWavelength() == 0) {
                        reflecBand.setSpectralWavelength(fallBackSpectralWavelength);
                        reflecBand.setSpectralBandIndex(i);
                    }
                    reflecBand.setValidPixelExpression(validPixelExpression);
                }
                autoGrouping.append(":" + TARGET_RASTER_NAME_PREFIX_RHOW_ESA);
            default:
                throw new IllegalStateException(String.format("Unknown sensor '%s'", sensor));
        }
    }

    public Band addBand(Product targetProduct, String name, String unit, String description) {
        Band targetBand = targetProduct.addBand(name, ProductData.TYPE_FLOAT32);
        targetBand.setUnit(unit);
        targetBand.setDescription(description);
        targetBand.setGeophysicalNoDataValue(Double.NaN);
        targetBand.setNoDataValueUsed(true);
        targetBand.setValidPixelExpression(validPixelExpression);
        return targetBand;
    }

}
