package org.esa.s3tbx.dataio.s3.meris;

import org.esa.s3tbx.dataio.s3.Sentinel3ProductReader;
import org.esa.snap.core.datamodel.Product;

import java.io.IOException;

/**
 * @author Tonio Fincke
 */
public class MerisLevel1ProductReader extends Sentinel3ProductReader {

    public MerisLevel1ProductReader(MerisLevel1ProductPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        final String dirName = getInputFileParentDirectory().getName();
        if (dirName.matches("EN(1|V).*_ME(R|_)1?_(F|R)R(G|P|S|_)_(1P)?_.*")) {
            setFactory(new MerisLevel1ProductFactory(this));
        }
        return createProduct();
    }

}
