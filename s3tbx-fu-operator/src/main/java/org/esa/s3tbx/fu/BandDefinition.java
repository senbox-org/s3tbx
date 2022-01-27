package org.esa.s3tbx.fu;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ColorPaletteDef;
import org.esa.snap.core.datamodel.ImageInfo;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.pointop.WritableSample;
import org.esa.snap.core.util.DefaultPropertyMap;
import org.esa.snap.core.util.PropertyMap;

import java.awt.Color;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Marco Peters
 */
abstract class BandDefinition {

    private static final String X3_BAND_NAME = "X3";
    private static final String Y3_BAND_NAME = "Y3";
    private static final String Z3_BAND_NAME = "Z3";
    private static final String CHRX_BAND_NAME = "chrX";
    private static final String CHRY_BAND_NAME = "chrY";
    private static final String HUE_BAND_NAME = "hue_band";
    private static final String POLY_CORR_BAND_NAME = "poly_corr";
    private static final String HUE_ANGLE_BAND_NAME = "hue_angle";
    private static final String DOMINANT_LAMBDA_BAND_NAME = "dominant_wvl";
    private static final String FU_VALUE_BAND_NAME = "FU";

    final String name;
    final String description;
    final int dataType;
    final double noDataValue;
    final boolean noDataValueUsed;
    private ColorPaletteDef colorPaletteDef;

    private BandDefinition(String description, String name, int dataType, double noDataValue, boolean noDataValueUsed) {
        this.description = description;
        this.name = name;
        this.dataType = dataType;
        this.noDataValue = noDataValue;
        this.noDataValueUsed = noDataValueUsed;
    }

    void setColorPalette(ColorPaletteDef cpd) {
        colorPaletteDef = cpd;
    }

    void addToProduct(Product targetProduct) {
        Band band = targetProduct.addBand(name, dataType);
        band.setDescription(description);
        band.setNoDataValue(noDataValue);
        band.setNoDataValueUsed(noDataValueUsed);
        if (colorPaletteDef != null) {
            band.setImageInfo(new ImageInfo(colorPaletteDef));
        }
    }

    abstract void setTargetSample(FuResult result, WritableSample targetSample);

    void setNoDataValue(WritableSample targetSample) {
        targetSample.set(noDataValue);
    }


    static BandDefinition[] create(Instrument instrument, boolean includeIntermediateResults, boolean includeDominantLambda) {
        List<BandDefinition> list = new ArrayList<>();
        if (includeIntermediateResults) {
            String bandNameSuffix = "_" + instrument.name();
            list.add(new BandDefinition("", X3_BAND_NAME + bandNameSuffix, ProductData.TYPE_FLOAT32, Float.NaN, true) {
                @Override
                public void setTargetSample(FuResult result, WritableSample targetSample) {
                    targetSample.set(result.getX3());
                }
            });
            list.add(new BandDefinition("", Y3_BAND_NAME + bandNameSuffix, ProductData.TYPE_FLOAT32, Float.NaN, true) {
                @Override
                public void setTargetSample(FuResult result, WritableSample targetSample) {
                    targetSample.set(result.getY3());
                }
            });
            list.add(new BandDefinition("", Z3_BAND_NAME + bandNameSuffix, ProductData.TYPE_FLOAT32, Float.NaN, true) {
                @Override
                public void setTargetSample(FuResult result, WritableSample targetSample) {
                    targetSample.set(result.getZ3());
                }
            });
            list.add(new BandDefinition("", CHRX_BAND_NAME + bandNameSuffix, ProductData.TYPE_FLOAT32, Float.NaN, true) {
                @Override
                public void setTargetSample(FuResult result, WritableSample targetSample) {
                    targetSample.set(result.getChrX());
                }
            });
            list.add(new BandDefinition("", CHRY_BAND_NAME + bandNameSuffix, ProductData.TYPE_FLOAT32, Float.NaN, true) {
                @Override
                public void setTargetSample(FuResult result, WritableSample targetSample) {
                    targetSample.set(result.getChrY());
                }
            });
            list.add(new BandDefinition("", POLY_CORR_BAND_NAME + bandNameSuffix, ProductData.TYPE_FLOAT32, Float.NaN, true) {
                @Override
                public void setTargetSample(FuResult result, WritableSample targetSample) {
                    targetSample.set(result.getPolyCorr());
                }
            });
            list.add(new BandDefinition("", HUE_BAND_NAME + bandNameSuffix, ProductData.TYPE_FLOAT32, Float.NaN, true) {
                @Override
                public void setTargetSample(FuResult result, WritableSample targetSample) {
                    targetSample.set(result.getHue());
                }
            });
        }
        final BandDefinition hueAngleDef = new BandDefinition("", HUE_ANGLE_BAND_NAME, ProductData.TYPE_FLOAT32, Float.NaN, true) {
            @Override
            public void setTargetSample(FuResult result, WritableSample targetSample) {
                targetSample.set(result.getHueAngle());
            }
        };
        hueAngleDef.setColorPalette(loadHueAngleCpd());
        list.add(hueAngleDef);
        if (includeDominantLambda) {
            list.add(new BandDefinition("The dominant wavelength derived from hue_angle", DOMINANT_LAMBDA_BAND_NAME, ProductData.TYPE_FLOAT32, Float.NaN, true) {
                @Override
                public void setTargetSample(FuResult result, WritableSample targetSample) {
                    targetSample.set(result.getDominantLambda());
                }
            });

        }
        list.add(new BandDefinition("", FU_VALUE_BAND_NAME, ProductData.TYPE_INT8, Byte.MAX_VALUE, true) {
            @Override
            public void setTargetSample(FuResult result, WritableSample targetSample) {
                targetSample.set(result.getFuValue());
            }
        });

        return list.toArray(new BandDefinition[0]);
    }

    private static ColorPaletteDef loadHueAngleCpd() {
        try {
            URL resource = BandDefinition.class.getResource("/auxdata/color_palettes/hue_angle.cpd");
            if (resource != null) {
                final Path path = Paths.get(resource.toURI());
                return loadColorPaletteDef(path);
                // Method not yet available in 8.x, so it is copied here.
                // In master it is correctly used.
//                return ColorPaletteDef.loadColorPaletteDef(path);
            } else {
                throw new IllegalStateException("Not able to load hue_angle CPD. Resource not found.");
            }
        } catch (URISyntaxException | IOException e) {
            throw new IllegalStateException("Not able to load hue_angle CPD.", e);
        }
    }

    // Method not yet available in 8.x, so it is copied here:
    public static ColorPaletteDef loadColorPaletteDef(Path path) throws IOException {
        final PropertyMap propertyMap = new DefaultPropertyMap();
        propertyMap.load(path); // Overwrite existing values
        final int numPoints = propertyMap.getPropertyInt("numPoints");
        if (numPoints < 2) {
            throw new IOException("The selected file contains less than\n" +
                                          "two colour points.");
        }
        final ColorPaletteDef.Point[] points = new ColorPaletteDef.Point[numPoints];
        double lastSample = 0;
        for (int i = 0; i < points.length; i++) {
            final ColorPaletteDef.Point point = new ColorPaletteDef.Point();
            final Color color = propertyMap.getPropertyColor("color" + i);
            double sample = propertyMap.getPropertyDouble("sample" + i);
            if (i > 0 && sample < lastSample) {
                sample = lastSample + 1.0;
            }
            point.setColor(color);
            point.setSample(sample);
            point.setLabel(path.getFileName().toString());
            points[i] = point;
            lastSample = sample;
        }
        ColorPaletteDef paletteDef = new ColorPaletteDef(points, 256);
        paletteDef.setAutoDistribute(propertyMap.getPropertyBool("autoDistribute", false));
        return paletteDef;
    }

}
