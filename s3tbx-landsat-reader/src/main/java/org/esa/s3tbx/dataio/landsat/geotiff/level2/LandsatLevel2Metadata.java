package org.esa.s3tbx.dataio.landsat.geotiff.level2;

import org.esa.s3tbx.dataio.landsat.metadata.XmlMetadata;
import org.esa.s3tbx.dataio.landsat.metadata.XmlMetadataParser;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.ProductData;

import java.awt.geom.Point2D;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by obarrile on 07/02/2019.
 */
public class LandsatLevel2Metadata extends XmlMetadata {

    private static final float[] ETM_PLUS_WAVELENGTHS = {
            485.0f,
            560.0f,
            660.0f,
            835.0f,
            1650.0f,
            11450.0f,
            2220.0f,
            710.0f
    };
    private static final float[] ETM_PLUS_BANDWIDTHS = {
            70.0f,
            80.0f,
            60.0f,
            130.0f,
            200.0f,
            2100.0f,
            260.0f,
            380.0f
    };

    private static final float[] TM_WAVELENGTHS = {
            490,
            560,
            660,
            830,
            1670,
            11500,
            2240,
            710
    };

    private static final float[] TM_BANDWIDTHS = {
            66,
            82,
            67,
            128,
            217,
            1000,
            252,
            380
    };

    private static final float[] L8_WAVELENGTHS = {
            440,
            480,
            560,
            655,
            865,
            1610,
            2200,
            590,
            1370,
            10895,
            12005
    };

    private static final float[] L8_BANDWIDTHS = {
            20,
            60,
            60,
            30,
            30,
            80,
            180,
            180,
            20,
            590,
            1010
    };

    public static class LandsatLevel2MetadataParser extends XmlMetadataParser<LandsatLevel2Metadata> {

        public LandsatLevel2MetadataParser(Class metadataFileClass) {
            super(metadataFileClass);
        }

        @Override
        protected boolean shouldValidateSchema() {
            return false;
        }
    }

    public LandsatLevel2Metadata(String name) {
        super(name);
    }

    @Override
    public String getFileName() {
        return name;
    }

    @Override
    public String getProductName() {
        String name = getAttributeValue(LandsatLevel2Constants.PRODUCT_ID, "NO_NAME") + " - Level 2";
        rootElement.setDescription(name);
        return name;
    }

    public String getProductDescription() {
        return getProductName();
    }

    @Override
    public String getFormatName() {
        //return getAttributeValue(MuscateConstants.PATH_METADATA_FORMAT, MuscateConstants.METADATA_MUSCATE);
        return null;
    }

    @Override
    public String getMetadataProfile() {
        //return getAttributeValue(MuscateConstants.PATH_METADATA_PROFILE, MuscateConstants.DISTRIBUTED);
        return null;
    }

    @Override
    public int getRasterWidth() {
        int width = 0;
        MetadataElement[] bandElements = getRootElement().getElement("bands").getElements();
        for (MetadataElement bandElement : bandElements) {
            int auxWidth = Integer.parseInt(bandElement.getAttribute("nsamps").getData().toString());
            if (auxWidth > width) width = auxWidth;
        }
        return width;
    }

    @Override
    public int getRasterHeight() {
        int height = 0;
        MetadataElement[] bandElements = getRootElement().getElement("bands").getElements();
        for (MetadataElement bandElement : bandElements) {
            int auxHeight = Integer.parseInt(bandElement.getAttribute("nlines").getData().toString());
            if (auxHeight > height) height = auxHeight;
        }
        return height;
    }


    @Override
    public String[] getRasterFileNames() {
        ArrayList<String> fileNamesArray = new ArrayList<>();
        MetadataElement[] bandElements = getRootElement().getElement("bands").getElements();
        for (MetadataElement bandElement : bandElements) {
            String name = bandElement.getAttribute("file_name").getData().toString();
            fileNamesArray.add(name);
        }
        return fileNamesArray.toArray(new String[fileNamesArray.size()]);
    }

    @Override
    public ProductData.UTC getProductStartTime() {
        //return the centerTime
        return getCenterTime();
    }

    @Override
    public ProductData.UTC getProductEndTime() {
        //return the centerTime
        return getCenterTime();
    }

    @Override
    public ProductData.UTC getCenterTime() {

        String timeString = getRootElement().getElement("global_metadata").getAttribute("scene_center_time").getData().toString();
        String dateString = getRootElement().getElement("global_metadata").getAttribute("acquisition_date").getData().toString();

        try {
            if (dateString != null && timeString != null) {
                timeString = timeString.substring(0, 12);
                final DateFormat dateFormat = ProductData.UTC.createDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                final Date date = dateFormat.parse(dateString + " " + timeString);
                String milliSeconds = timeString.substring(timeString.length() - 3);
                return ProductData.UTC.create(date, Long.parseLong(milliSeconds) * 1000);
            }
        } catch (ParseException ignored) {
            // ignore
        }
        return null;
    }

    @Override
    public int getNumBands() {
        MetadataElement[] bandElements = getRootElement().getElement("bands").getElements();
        return bandElements.length;
    }


    public Point2D.Double getUpperLeft() {
        MetadataElement[] metadataElements = getRootElement().getElement("global_metadata").getElement("projection_information").getElements();
        for (MetadataElement corner_point : metadataElements) {
            if(!corner_point.getName().equals("corner_point")) {
                continue;
            }
            if(corner_point.getAttribute("location").getData().toString().equals("UL")) {
                double x = Double.parseDouble(corner_point.getAttribute("x").getData().toString());
                double y = Double.parseDouble(corner_point.getAttribute("y").getData().toString());
                return new Point2D.Double(x, y);
            }
        }

        return null;
    }


    public String getBandName(String filename) {
        MetadataElement[] bandElements = getRootElement().getElement("bands").getElements();
        for (MetadataElement bandElement : bandElements) {
            String name = bandElement.getAttribute("file_name").getData().toString();
            if(filename.equals(name)) {
                return bandElement.getAttribute("name").getData().toString();
            }
        }
        return null;
    }

    public double getScalingFactor(String filename) {
        MetadataElement[] bandElements = getRootElement().getElement("bands").getElements();
        for (MetadataElement bandElement : bandElements) {
            String name = bandElement.getAttribute("file_name").getData().toString();
            if (filename.equals(name)) {
                MetadataAttribute attribute = bandElement.getAttribute("scale_factor");
                if (attribute != null) {
                    return Double.parseDouble(attribute.getData().toString());
                }
            }
        }
        return 1.0;
    }

    public double getFillValue(String filename) {
        MetadataElement[] bandElements = getRootElement().getElement("bands").getElements();
        for (MetadataElement bandElement : bandElements) {
            String name = bandElement.getAttribute("file_name").getData().toString();
            if (filename.equals(name)) {
                MetadataAttribute attribute = bandElement.getAttribute("fill_value");
                if (attribute != null) {
                    return Double.parseDouble(attribute.getData().toString());
                }
            }
        }
        return Double.NaN;
    }

    public double getScalingOffset(String filename) {
        return 0.0;
    }

    public float getWavelength(String filename) {
        String instrument = getInstrument();
        int index1 = filename.indexOf("sr_band");
        int index2 = filename.indexOf(".tif");
        if (index1 < 0 || index2 < index1) {
            return 0;
        }
        Integer.parseInt(filename.substring(index1+"sr_band".length(),index2));
        int band=Integer.parseInt(filename.substring(index1+"sr_band".length(),index2)) - 1; //TODO
        if(instrument.startsWith("OLI")) {
            return L8_WAVELENGTHS[band];
        } else if(instrument.startsWith("TM")) {
            return TM_WAVELENGTHS[band];
        } else if (instrument.startsWith("ETM")) {
            return ETM_PLUS_WAVELENGTHS[band];
        }
        return 0;
    }

    public float getBandwidth(String filename) {
        String instrument = getInstrument();
        int index1 = filename.indexOf("sr_band");
        int index2 = filename.indexOf(".tif");
        if(index1<0 || index2<index1) {
            return 0;
        }
        Integer.parseInt(filename.substring(index1+"sr_band".length(),index2));
        int band=Integer.parseInt(filename.substring(index1+"sr_band".length(),index2)) - 1; //TODO
        if(instrument.startsWith("OLI")) {
            return L8_BANDWIDTHS[band];
        } else if(instrument.startsWith("TM")) {
            return TM_BANDWIDTHS[band];
        } else if (instrument.startsWith("ETM")) {
            return ETM_PLUS_BANDWIDTHS[band];
        }
        return 0;
    }

    public String getBandDescription(String filename) {
        MetadataElement[] bandElements = getRootElement().getElement("bands").getElements();
        for (MetadataElement bandElement : bandElements) {
            String name = bandElement.getAttribute("file_name").getData().toString();
            if(filename.equals(name)) {
                return bandElement.getAttribute("long_name").getData().toString();
            }
        }
        return null;
    }

    public boolean isReflectanceBand(String filename) {
        MetadataElement[] bandElements = getRootElement().getElement("bands").getElements();
        for (MetadataElement bandElement : bandElements) {
            String name = bandElement.getAttribute("file_name").getData().toString();
            if(filename.equals(name)) {
                return (bandElement.getAttribute("product").getData().toString().equals("sr_refl") &&
                        bandElement.getAttribute("name").getData().toString().startsWith("sr_band") &&
                        bandElement.getAttribute("category").getData().toString().equals("image"));
            }
        }
        return false;
    }

    public boolean isSaturationBand(String filename) {
        MetadataElement[] bandElements = getRootElement().getElement("bands").getElements();
        for (MetadataElement bandElement : bandElements) {
            String name = bandElement.getAttribute("file_name").getData().toString();
            if(filename.equals(name)) {
                return (bandElement.getAttribute("product").getData().toString().equals("toa_refl") &&
                        bandElement.getAttribute("name").getData().toString().equals("radsat_qa") &&
                        bandElement.getAttribute("category").getData().toString().equals("qa"));
            }
        }
        return false;
    }

    public boolean isAerosolBand(String filename) {
        MetadataElement[] bandElements = getRootElement().getElement("bands").getElements();
        for (MetadataElement bandElement : bandElements) {
            String name = bandElement.getAttribute("file_name").getData().toString();
            if(filename.equals(name)) {
                return (bandElement.getAttribute("product").getData().toString().equals("sr_refl") &&
                        bandElement.getAttribute("name").getData().toString().equals("sr_aerosol") &&
                        bandElement.getAttribute("category").getData().toString().equals("qa"));
            }
        }
        return false;
    }

    public boolean isQualityBand(String filename) {
        MetadataElement[] bandElements = getRootElement().getElement("bands").getElements();
        for (MetadataElement bandElement : bandElements) {
            String name = bandElement.getAttribute("file_name").getData().toString();
            if(filename.equals(name)) {
                return (bandElement.getAttribute("product").getData().toString().equals("level2_qa") &&
                        bandElement.getAttribute("name").getData().toString().equals("pixel_qa") &&
                        bandElement.getAttribute("category").getData().toString().equals("qa"));
            }
        }
        return false;
    }

    public boolean isAtmosBand(String filename) {
        MetadataElement[] bandElements = getRootElement().getElement("bands").getElements();
        for (MetadataElement bandElement : bandElements) {
            String name = bandElement.getAttribute("file_name").getData().toString();
            if(filename.equals(name)) {
                return (bandElement.getAttribute("product").getData().toString().equals("sr_refl") &&
                        bandElement.getAttribute("name").getData().toString().equals("sr_atmos_opacity") &&
                        bandElement.getAttribute("category").getData().toString().equals("image"));
            }
        }
        return false;
    }

    public boolean isCloudBand(String filename) {
        MetadataElement[] bandElements = getRootElement().getElement("bands").getElements();
        for (MetadataElement bandElement : bandElements) {
            String name = bandElement.getAttribute("file_name").getData().toString();
            if(filename.equals(name)) {
                return (bandElement.getAttribute("product").getData().toString().equals("sr_refl") &&
                        bandElement.getAttribute("name").getData().toString().equals("sr_cloud_qa") &&
                        bandElement.getAttribute("category").getData().toString().equals("qa"));
            }
        }
        return false;
    }

    public String getSatellite() {
        String name = getAttributeValue(LandsatLevel2Constants.SATELLITE, "LANDSAT_8");
        return name;
    }

    public String getInstrument() {
        String name = getAttributeValue(LandsatLevel2Constants.INSTRUMENT, "OLI_TIRS");
        return name;
    }

    public FlagCoding createSaturationFlagCoding(String bandName) {
        //TODO from metadata
        FlagCoding flagCoding = new FlagCoding("saturation");
        flagCoding.addFlag("data_fill_flag", 1, "Designated Fill");
        flagCoding.addFlag("Band1_saturated", 2, "Band 1 Data Saturation Flag");
        flagCoding.addFlag("Band2_saturated", 4, "Band 2 Data Saturation Flag");
        flagCoding.addFlag("Band3_saturated", 8, "Band 3 Data Saturation Fla ");
        flagCoding.addFlag("Band4_saturated", 16, "Band 4 Data Saturation Flag");
        flagCoding.addFlag("Band5_saturated", 32, "Band 5 Data Saturation Flag");
        flagCoding.addFlag("Band6_saturated", 64, "Band 6 Data Saturation Flag");
        flagCoding.addFlag("Band7_saturated", 128, "Band 7 Data Saturation Flag");
        if(getSatellite().equals("LANDSAT_8")) {
            flagCoding.addFlag("Band8_saturated", 256, "N/A");
            flagCoding.addFlag("Band9_saturated", 512, "Band 9 Data Saturation Flag");
            flagCoding.addFlag("Band10_saturated", 1024, "Band 10 Data Saturation Flag");
            flagCoding.addFlag("Band11_saturated", 2048, "Band 11 Data Saturation Flag");
        }
        return flagCoding;
    }

}
