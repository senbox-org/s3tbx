package org.esa.s3tbx.dataio.s3.meris.reprocessing;

import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class Meris3rd4thReprocessingMetadataTest {

    private Product fourthReproTestProduct;

    @Before
    public void setUp() {
        createFourthReproTestProduct_metadataOnly();
    }

    @Test
    public void testFillMetadataInThirdRepro() {
        final Product thirdReproProduct = new Product("ME_1_RRG_3RP_test", "ME_1_RRG", 3, 2);
        Meris3rd4thReprocessingMetadata.fillMetadataInThirdRepro(fourthReproTestProduct, thirdReproProduct);

        final MetadataElement thirdReproProductMetadataRoot = thirdReproProduct.getMetadataRoot();
        assertNotNull(thirdReproProductMetadataRoot);

        final MetadataElement mphElement = thirdReproProductMetadataRoot.getElement("MPH");
        assertNotNull(mphElement);

        final MetadataAttribute productAttr = mphElement.getAttribute("PRODUCT");
        assertNotNull(productAttr);
        assertNotNull(productAttr.getData());
        assertEquals(fourthReproTestProduct.getName(), productAttr.getData().getElemString());

        final MetadataAttribute sensingStartAttr = mphElement.getAttribute("SENSING_START");
        assertNotNull(sensingStartAttr);
        assertNotNull(sensingStartAttr.getData());
        assertEquals("2011-07-02T14:08:01.955726Z", sensingStartAttr.getData().getElemString());

        final MetadataAttribute sensingStopAttr = mphElement.getAttribute("SENSING_STOP");
        assertNotNull(sensingStopAttr);
        assertNotNull(sensingStopAttr.getData());
        assertEquals("2011-07-02T14:51:57.552001Z", sensingStopAttr.getData().getElemString());

        final MetadataAttribute cycleAttr = mphElement.getAttribute("CYCLE");
        assertNotNull(cycleAttr);
        assertNotNull(cycleAttr.getData());
        assertEquals(104, cycleAttr.getData().getElemInt());

        final MetadataAttribute relOrbitAttr = mphElement.getAttribute("REL_ORBIT");
        assertNotNull(relOrbitAttr);
        assertNotNull(relOrbitAttr.getData());
        assertEquals(111, relOrbitAttr.getData().getElemInt());

        final MetadataAttribute absOrbitAttr = mphElement.getAttribute("ABS_ORBIT");
        assertNotNull(absOrbitAttr);
        assertNotNull(absOrbitAttr.getData());
        assertEquals(48832, absOrbitAttr.getData().getElemInt());
    }

    private void createFourthReproTestProduct_metadataOnly() {
        fourthReproTestProduct = new Product("ME_1_RRG_4RP_test", "ME_1_RRG", 3, 2);

        // add metadata
        final MetadataElement manifestElement = new MetadataElement("Manifest");
        fourthReproTestProduct.getMetadataRoot().addElement(manifestElement);
        final MetadataElement metadataSectionElement = new MetadataElement("metadataSection");
        manifestElement.addElement(metadataSectionElement);
        final MetadataElement generalProductInformationElement = new MetadataElement("generalProductInformation");
        metadataSectionElement.addElement(generalProductInformationElement);
        generalProductInformationElement.addAttribute(new MetadataAttribute("productName",
                ProductData.createInstance(fourthReproTestProduct.getName()), true));
        final MetadataElement acquisitionPeriodElement = new MetadataElement("acquisitionPeriod");
        metadataSectionElement.addElement(acquisitionPeriodElement);
        acquisitionPeriodElement.addAttribute(new MetadataAttribute("startTime",
                ProductData.createInstance("2011-07-02T14:08:01.955726Z"), true));
        acquisitionPeriodElement.addAttribute(new MetadataAttribute("stopTime",
                ProductData.createInstance("2011-07-02T14:51:57.552001Z"), true));
        final MetadataElement orbitReferenceElement = new MetadataElement("orbitReference");
        orbitReferenceElement.addAttribute(new MetadataAttribute("cycleNumber",
                ProductData.createInstance(new int[]{104}), true));
        final MetadataElement orbitNumberElement = new MetadataElement("orbitNumber");
        orbitNumberElement.addAttribute(new MetadataAttribute("orbitNumber",
                ProductData.createInstance(new int[]{48832}), true));
        orbitReferenceElement.addElement(orbitNumberElement);
        final MetadataElement relativeOrbitNumberElement = new MetadataElement("relativeOrbitNumber");
        relativeOrbitNumberElement.addAttribute(new MetadataAttribute("relativeOrbitNumber",
                ProductData.createInstance(new int[]{111}), true));
        orbitReferenceElement.addElement(relativeOrbitNumberElement);
        metadataSectionElement.addElement(orbitReferenceElement);
    }
}
