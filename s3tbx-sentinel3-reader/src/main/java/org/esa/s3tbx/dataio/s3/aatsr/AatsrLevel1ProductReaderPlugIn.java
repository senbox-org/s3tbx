package org.esa.s3tbx.dataio.s3.aatsr;

import org.esa.s3tbx.dataio.s3.Sentinel3ProductReaderPlugIn;
import org.esa.snap.core.dataio.ProductReader;

/**
 * @author Sabine Embacher
 */
public class AatsrLevel1ProductReaderPlugIn extends Sentinel3ProductReaderPlugIn {

    public final static String DIRECTORY_NAME_PATTERN = "(ENV|ER1|ER2)_AT_1_RBT____" +
                                                        "[12]\\d{3}[01]\\d[0123]\\dT[012]\\d[012345]\\d[012345]\\d_" +
                                                        "[12]\\d{3}[01]\\d[0123]\\dT[012]\\d[012345]\\d[012345]\\d_" +
                                                        "[12]\\d{3}[01]\\d[0123]\\dT[012]\\d[012345]\\d[012345]\\d_" +
                                                        "\\d{4}_\\d{3}_\\d{3}______(DSI|TLS|TPZ)_R_NT_...\\.SEN3";

    private static final String FORMAT_NAME = "ATS_L1_S3";

    public AatsrLevel1ProductReaderPlugIn() {
        super(FORMAT_NAME, "(A)ATSR Level 1 in Sentinel-3 product format",
              DIRECTORY_NAME_PATTERN, "xfdumanifest", null, ".xml");
    }

    @Override
    public ProductReader createReaderInstance() {
        return new AatsrLevel1ProductReader(this);
    }

}
