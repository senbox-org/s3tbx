/*
 *
 * Copyright (c) 2021.  Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 *
 */

package gov.nasa.gsfc.seadas.dataio;

import org.esa.snap.core.dataio.dimap.spi.DimapHistoricalDecoder;
import org.esa.snap.core.dataio.persistence.Container;
import org.esa.snap.core.dataio.persistence.HistoricalDecoder;
import org.esa.snap.core.dataio.persistence.Item;
import org.esa.snap.core.dataio.persistence.PersistenceConverter;
import org.esa.snap.core.dataio.persistence.Property;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.util.SystemUtils;

/**
 * @author Marco Zühlke
 * @author Sabine Embacher
 */
public class BowtieGeoCodingPersistenceConverter extends PersistenceConverter<AbstractBowtieGeoCoding> {

    private static final String NAME_BOWTIE_PIXEL_GEO_CODING = "BowtiePixelGeoCoding";
    private static final String NAME_BOWTIE_TIEPOINT_GEO_CODING = "BowtieTiePointGeoCoding";
    private static final String NAME_BOWTIE_SCANLINE_HEIGHT = "BowtieScanlineHeight";
    private static final String NAME_LATITUDE_BAND = "LATITUDE_BAND";
    private static final String NAME_LONGITUDE_BAND = "LONGITUDE_BAND";
    private static final String NAME_TIE_POINT_GRID_NAME_LAT = "TIE_POINT_GRID_NAME_LAT";
    private static final String NAME_TIE_POINT_GRID_NAME_LON = "TIE_POINT_GRID_NAME_LON";

    // Never change this constant! Instead, create a new one with the
    // name ID_VERSION_2, as ID_VERSION_1 is used in HistoricalDecoder0.
    // And so on ...
    public static final String ID_VERSION_1 = "BowtieGC:1";

    @Override
    public String getID() {
        return ID_VERSION_1;
    }

    @Override
    public AbstractBowtieGeoCoding decodeImpl(Item item, Product product) {
        if (item == null || !item.isContainer() ||
            !(NAME_BOWTIE_PIXEL_GEO_CODING.equals(item.getName()) || NAME_BOWTIE_TIEPOINT_GEO_CODING.equals(item.getName()))
        ) {
            SystemUtils.LOG.warning("For decoding a container item with name '" + NAME_BOWTIE_TIEPOINT_GEO_CODING
                                    + "' or '" + NAME_BOWTIE_PIXEL_GEO_CODING + "' is expected.");
            return null;
        }
        final Container root = item.asContainer();
        if (NAME_BOWTIE_PIXEL_GEO_CODING.equals(root.getName())) {
            final String nameLat = root.getProperty(NAME_LATITUDE_BAND).getValueString();
            final String nameLon = root.getProperty(NAME_LONGITUDE_BAND).getValueString();
            final int scanlineHeight = root.getProperty(NAME_BOWTIE_SCANLINE_HEIGHT).getValueInt();

            final Band latBand = product.getBand(nameLat);
            final Band lonBand = product.getBand(nameLon);
            if (latBand == null || lonBand == null) {
                if (latBand == null) {
                    SystemUtils.LOG.warning("Unable to find expected latitude band '" + nameLat + "' in product.");
                }
                if (lonBand == null) {
                    SystemUtils.LOG.warning("Unable to find expected longitude band '" + nameLon + "' in product.");
                }
                SystemUtils.LOG.warning("Unable to create " + NAME_BOWTIE_PIXEL_GEO_CODING + ".");
                return null;
            }
            return new BowtiePixelGeoCoding(latBand, lonBand, scanlineHeight);
        } else {
            final String nameLat = root.getProperty(NAME_TIE_POINT_GRID_NAME_LAT).getValueString();
            final String nameLon = root.getProperty(NAME_TIE_POINT_GRID_NAME_LON).getValueString();
            final int scanlineHeight = root.getProperty(NAME_BOWTIE_SCANLINE_HEIGHT).getValueInt();

            final TiePointGrid latGrid = product.getTiePointGrid(nameLat);
            final TiePointGrid lonGrid = product.getTiePointGrid(nameLon);
            if (latGrid == null || lonGrid == null) {
                if (latGrid == null) {
                    SystemUtils.LOG.warning("Unable to find expected latitude tie point grid '" + nameLat + "' in product.");
                }
                if (lonGrid == null) {
                    SystemUtils.LOG.warning("Unable to find expected longitude tie point grid '" + nameLon + "' in product.");
                }
                SystemUtils.LOG.warning("Unable to create " + NAME_BOWTIE_TIEPOINT_GEO_CODING + ".");
                return null;
            }

            return new BowtieTiePointGeoCoding(latGrid, lonGrid, scanlineHeight);
        }
    }

    @Override
    public Item encode(AbstractBowtieGeoCoding bowtieGeoCoding) {
        if (bowtieGeoCoding instanceof BowtiePixelGeoCoding) {
            BowtiePixelGeoCoding geoCoding = (BowtiePixelGeoCoding) bowtieGeoCoding;

            final Container root = createRootContainer(NAME_BOWTIE_PIXEL_GEO_CODING);
            root.add(new Property<>(NAME_LATITUDE_BAND, geoCoding.getLatBand().getName()));
            root.add(new Property<>(NAME_LONGITUDE_BAND, geoCoding.getLonBand().getName()));
            root.add(new Property<>(NAME_BOWTIE_SCANLINE_HEIGHT, geoCoding.getScanlineHeight()));

            return root;
        } else if (bowtieGeoCoding instanceof BowtieTiePointGeoCoding) {
            BowtieTiePointGeoCoding geoCoding = (BowtieTiePointGeoCoding) bowtieGeoCoding;

            final Container root = createRootContainer(NAME_BOWTIE_TIEPOINT_GEO_CODING);
            root.add(new Property<>(NAME_TIE_POINT_GRID_NAME_LAT, geoCoding.getLatGrid().getName()));
            root.add(new Property<>(NAME_TIE_POINT_GRID_NAME_LON, geoCoding.getLonGrid().getName()));
            root.add(new Property<>(NAME_BOWTIE_SCANLINE_HEIGHT, geoCoding.getScanlineHeight()));

            return root;
        } else {
            throw new IllegalArgumentException("Unsupported object " + bowtieGeoCoding);
        }
    }

    @Override
    public HistoricalDecoder[] getHistoricalDecoders() {
        return new HistoricalDecoder[]{
                new HistoricalDecoder0()
        };
    }

    private static class HistoricalDecoder0 extends DimapHistoricalDecoder {

        @Override
        public boolean canDecode(Item item) {
            return item != null && item.isContainer()
                   && ("BowtiePixelGeoCoding".equals(item.getName())
                       || "BowtieTiePointGeoCoding".equals(item.getName()));
        }

        @Override
        public Item decode(Item item, Product product) {
            final Container container = item.asContainer();
            container.add(new Property<>(KEY_PERSISTENCE_ID, ID_VERSION_1));
            return container;
        }
    }
}
