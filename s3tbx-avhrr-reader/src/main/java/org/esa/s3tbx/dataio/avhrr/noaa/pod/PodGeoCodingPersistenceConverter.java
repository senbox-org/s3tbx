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

package org.esa.s3tbx.dataio.avhrr.noaa.pod;

import org.esa.snap.core.dataio.dimap.DimapProductConstants;
import org.esa.snap.core.dataio.dimap.spi.DimapHistoricalDecoder;
import org.esa.snap.core.dataio.persistence.Container;
import org.esa.snap.core.dataio.persistence.HistoricalDecoder;
import org.esa.snap.core.dataio.persistence.Item;
import org.esa.snap.core.dataio.persistence.PersistenceConverter;
import org.esa.snap.core.dataio.persistence.Property;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.util.SystemUtils;
import org.jdom.Element;

public class PodGeoCodingPersistenceConverter implements PersistenceConverter<PodGeoCoding> {

    public static final String NAME_POD_GEO_CODING = "PodGeoCoding";
    public static final String NAME_TIE_POINT_GRID_NAME_LAT = "TIE_POINT_GRID_NAME_LAT";
    public static final String NAME_TIE_POINT_GRID_NAME_LON = "TIE_POINT_GRID_NAME_LON";

    // Never change this constant! Instead, create a new one with the
    // name ID_VERSION_2, as ID_VERSION_1 is used in HistoricalDecoder0.
    // And so on ...
    private static final String ID_VERSION_1 = "PodGeoCoding:1";

    @Override
    public String getID() {
        return ID_VERSION_1;
    }

    @Override
    public PodGeoCoding decode(Item item, Product product) {
        if (item == null || !item.isContainer() || !NAME_POD_GEO_CODING.equals(item.getName())) {
            SystemUtils.LOG.warning("For decoding a container item with name '" + NAME_POD_GEO_CODING + "' is expected.");
            return null;
        }
        final Container root = item.asContainer();
        String nameLat = root.getProperty(NAME_TIE_POINT_GRID_NAME_LAT).getValueString();
        String nameLon = root.getProperty(NAME_TIE_POINT_GRID_NAME_LON).getValueString();

        TiePointGrid latGrid = product.getTiePointGrid(nameLat);
        TiePointGrid lonGrid = product.getTiePointGrid(nameLon);
        if (latGrid == null || lonGrid == null) {
            if (latGrid == null) {
                SystemUtils.LOG.warning("Unable to find expected latitude tie point grid '" + nameLat + "' in product.");
            }
            if (lonGrid == null) {
                SystemUtils.LOG.warning("Unable to find expected longitude tie point grid '" + nameLon + "' in product.");
            }
            SystemUtils.LOG.warning("Unable to create " + NAME_POD_GEO_CODING + ".");
            return null;
        }

        // TODO: 24.03.2021 SE -- show MARCO
        // The following "if" should not be needed because of the TiePointGrid's ability to load data lazy.
        // if (latGrid.hasRasterData() && lonGrid.hasRasterData()) {
        return new PodGeoCoding(latGrid, lonGrid);
        // }
    }

    @Override
    public Item encode(PodGeoCoding podGeoCoding) {
        final Container root = createRootContainer(NAME_POD_GEO_CODING);
        root.add(new Property<>(NAME_TIE_POINT_GRID_NAME_LAT, podGeoCoding.getLatGrid().getName()));
        root.add(new Property<>(NAME_TIE_POINT_GRID_NAME_LON, podGeoCoding.getLonGrid().getName()));
        return root;
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
            return item != null && item.isContainer() && "PodGeoCoding".equals(item.getName());
        }

        @Override
        public Item decode(Item item, Product product) {
            final Container container = item.asContainer();
            container.add(new Property<>(KEY_PERSISTENCE_ID, ID_VERSION_1));
            return container;
        }
    }
}
