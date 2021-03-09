package org.esa.s3tbx.dataio.s3.meris.reprocessing;

import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;

/**
 * Class providing methods for metadata extraction/conversion between 3RP and 4RP.
 *
 * @author olafd
 */
public class Meris3rd4thReprocessingMetadata {

    /**
     * Transfers selected metadata from 4RP into 3RP product.
     *
     * @param fourthReproProduct - MERIS L1b 4RP product
     * @param thirdReproProduct - MERIS L1b 3RP product
     */
    public static void fillMetadataInThirdRepro(Product fourthReproProduct, Product thirdReproProduct) {
        final MetadataElement inputProductMetadataRoot = fourthReproProduct.getMetadataRoot();
        final MetadataElement thirdReproProductMetadataRoot = thirdReproProduct.getMetadataRoot();
        if (thirdReproProductMetadataRoot != null && inputProductMetadataRoot != null) {
            final MetadataElement mphElement = new MetadataElement("MPH");
            thirdReproProductMetadataRoot.addElement(mphElement);
            final MetadataElement manifestElement = inputProductMetadataRoot.getElement("Manifest");
            if (manifestElement != null) {
                final MetadataElement metadataSectionMetadataElement = manifestElement.getElement("metadataSection");
                if (metadataSectionMetadataElement != null) {
                    // MPH --> PRODUCT
                    // Manifest --> metadataSection --> generalProductInformation --> productName
                    final MetadataElement generalProductInformationMetadataElement =
                            metadataSectionMetadataElement.getElement("generalProductInformation");
                    if (generalProductInformationMetadataElement != null) {
                        final MetadataAttribute productNameAttr =
                                generalProductInformationMetadataElement.getAttribute("productName");
                        if (productNameAttr != null) {
                            mphElement.addAttribute(new MetadataAttribute("PRODUCT", productNameAttr.getData(), true));
                        }
                    }

                    // MPH --> SENSING_START
                    // Manifest --> metadataSection --> acquisitionPeriod --> startTime
                    // MPH --> SENSING_STOP
                    // Manifest --> metadataSection --> acquisitionPeriod --> stopTime
                    final MetadataElement acquisitionPeriodMetadataElement =
                            metadataSectionMetadataElement.getElement("acquisitionPeriod");
                    if (acquisitionPeriodMetadataElement != null) {
                        final MetadataAttribute startTimeAttr =
                                acquisitionPeriodMetadataElement.getAttribute("startTime");
                        if (startTimeAttr != null) {
                            mphElement.addAttribute(new MetadataAttribute("SENSING_START", startTimeAttr.getData(), true));
                        }
                        final MetadataAttribute stopTimeAttr =
                                acquisitionPeriodMetadataElement.getAttribute("stopTime");
                        if (stopTimeAttr != null) {
                            mphElement.addAttribute(new MetadataAttribute("SENSING_STOP", stopTimeAttr.getData(), true));
                        }
                    }

                    // MPH --> CYCLE
                    // Manifest --> metadataSection --> orbitReference --> cycleNumber
                    // MPH --> REL_ORBIT
                    // Manifest --> metadataSection --> orbitReference --> relativeOrbitNumber --> relativeOrbitNumber
                    // MPH --> ABS_ORBIT
                    // Manifest --> metadataSection --> orbitReference --> orbitNumber --> orbitNumber
                    final MetadataElement orbitReferenceMetadataElement =
                            metadataSectionMetadataElement.getElement("orbitReference");
                    if (orbitReferenceMetadataElement != null) {
                        final MetadataAttribute cycleNumberAttr =
                                orbitReferenceMetadataElement.getAttribute("cycleNumber");
                        if (cycleNumberAttr != null) {
                            mphElement.addAttribute(new MetadataAttribute("CYCLE", cycleNumberAttr.getData(), true));
                        }

                        final MetadataElement relativeOrbitNumberMetadataElement =
                                orbitReferenceMetadataElement.getElement("relativeOrbitNumber");
                        if (relativeOrbitNumberMetadataElement != null) {
                            final MetadataAttribute relativeOrbitNumberAttr =
                                    relativeOrbitNumberMetadataElement.getAttribute("relativeOrbitNumber");
                            if (relativeOrbitNumberAttr != null) {
                                mphElement.addAttribute(new MetadataAttribute("REL_ORBIT", relativeOrbitNumberAttr.getData(), true));
                            }
                        }

                        final MetadataElement orbitNumberMetadataElement =
                                orbitReferenceMetadataElement.getElement("orbitNumber");
                        if (orbitNumberMetadataElement != null) {
                            final MetadataAttribute orbitNumberAttr =
                                    orbitNumberMetadataElement.getAttribute("orbitNumber");
                            if (orbitNumberAttr != null) {
                                mphElement.addAttribute(new MetadataAttribute("ABS_ORBIT", orbitNumberAttr.getData(), true));
                            }
                        }
                    }
                }
                // todo: discuss what else is needed?
            }
        }
    }
}
