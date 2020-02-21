package org.esa.s3tbx.dataio.s3.slstr;

import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.Scene;
import org.esa.snap.core.datamodel.TiePointGeoCoding;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.geotools.referencing.operation.transform.AffineTransform2D;

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;

/**
 * @author Tonio Fincke
 */
class SlstrTiePointGeoCoding extends TiePointGeoCoding {

    private final AffineTransform2D transform;
    private final AffineTransform inverse;

    public SlstrTiePointGeoCoding(TiePointGrid latGrid, TiePointGrid lonGrid, AffineTransform2D transform)
            throws NoninvertibleTransformException {
        super(latGrid, lonGrid);
        this.transform = transform;
        inverse = transform.createInverse();
    }

    @Override
    public GeoPos getGeoPos(PixelPos pixelPos, GeoPos geoPos) {
        PixelPos transformedPixelPos = new PixelPos();
        transform.transform(pixelPos, transformedPixelPos);
        return super.getGeoPos(transformedPixelPos, geoPos);
    }

    @Override
    public PixelPos getPixelPos(GeoPos geoPos, PixelPos pixelPos) {
        pixelPos = super.getPixelPos(geoPos, pixelPos);
        PixelPos transformedPixelPos = new PixelPos();
        inverse.transform(pixelPos, transformedPixelPos);
        pixelPos.setLocation(transformedPixelPos);
        return transformedPixelPos;
    }

    @Override
    public boolean transferGeoCoding(Scene srcScene, Scene destScene, ProductSubsetDef subsetDef) {
        final String latGridName = getLatGrid().getName();
        final String lonGridName = getLonGrid().getName();
        final Product destProduct = destScene.getProduct();
        TiePointGrid latGrid = destProduct.getTiePointGrid(latGridName);
        if (latGrid == null) {
            if (subsetDef != null) {
                latGrid = TiePointGrid.createSubset(getLatGrid(), subsetDef);
            } else {
                latGrid = getLatGrid().cloneTiePointGrid();
            }
            destProduct.addTiePointGrid(latGrid);
        }
        TiePointGrid lonGrid = destProduct.getTiePointGrid(lonGridName);
        if (lonGrid == null) {
            if (subsetDef != null) {
                lonGrid = TiePointGrid.createSubset(getLonGrid(), subsetDef);
            } else {
                lonGrid = getLonGrid().cloneTiePointGrid();
            }
            destProduct.addTiePointGrid(lonGrid);
        }
        if (latGrid != null && lonGrid != null) {
            try {
                SlstrTiePointGeoCoding slstrTiePointGeoCoding =
                        new SlstrTiePointGeoCoding(latGrid, lonGrid, this.transform);
                destScene.setGeoCoding(slstrTiePointGeoCoding);
                return true;
            } catch (NoninvertibleTransformException e) {
                return false;
            }
        } else {
            return false;
        }
    }
}
