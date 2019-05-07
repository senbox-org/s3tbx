package org.esa.s3tbx.slstr.pdu.stitching.manifest;

import org.esa.s3tbx.slstr.pdu.stitching.PDUStitchingException;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;

/**
 * @author Tonio Fincke
 */
public class MissingElementsMergerTest {

    @Test
    public void mergeNodes_multipleGrids() throws ParserConfigurationException, SAXException, IOException, PDUStitchingException {
        List<Node> fromParents = new ArrayList<>();
        fromParents.add(ManifestTestUtils.createNode(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<slstr:missingElements threshold=\"75.000000\">\n" +
                        "  <slstr:globalInfo grid=\"0.5 km stripe A\" view=\"Nadir\" value=\"4000\" over=\"4000\" percentage=\"100.000000\"/>\n" +
                        "  <slstr:globalInfo grid=\"0.5 km stripe B\" view=\"Nadir\" value=\"4000\" over=\"4000\" percentage=\"100.000000\"/>\n" +
                        "  <slstr:globalInfo grid=\"0.5 km TDI\" view=\"Nadir\" value=\"4000\" over=\"4000\" percentage=\"100.000000\"/>\n" +
                        "  <slstr:globalInfo grid=\"0.5 km stripe A\" view=\"Oblique\" value=\"4000\" over=\"4000\" percentage=\"100.000000\"/>\n" +
                        "  <slstr:globalInfo grid=\"0.5 km stripe B\" view=\"Oblique\" value=\"4000\" over=\"4000\" percentage=\"100.000000\"/>\n" +
                        "  <slstr:globalInfo grid=\"0.5 km TDI\" view=\"Oblique\" value=\"4000\" over=\"4000\" percentage=\"100.000000\"/>\n" +
                        "  <slstr:elementMissing grid=\"0.5 km stripe A, 0.5 km stripe B, 0.5 km TDI\" view=\"Nadir\" startTime=\"2018-04-11T03:22:56.935232Z\" stopTime=\"2018-04-11T03:27:56.921087Z\" percentage=\"100.000000\">\n" +
                        "    <slstr:bandSet>S1, S2, S3, S4</slstr:bandSet>\n" +
                        "  </slstr:elementMissing>\n" +
                        "  <slstr:elementMissing grid=\"0.5 km stripe A, 0.5 km stripe B, 0.5 km TDI\" view=\"Oblique\" startTime=\"2018-04-11T03:22:56.929785Z\" stopTime=\"2018-04-11T03:27:56.840589Z\" percentage=\"100.000000\">\n" +
                        "    <slstr:bandSet>S1, S2, S3, S4</slstr:bandSet>\n" +
                        "  </slstr:elementMissing>\n" +
                        "</slstr:missingElements>").getFirstChild());

        Document manifest = ManifestTestUtils.createDocument();
        final Element manifestElement = manifest.createElement("slstr:missingElements");
        manifest.appendChild(manifestElement);

        new MissingElementsMerger().mergeNodes(fromParents, manifestElement, manifest);

        assertEquals(1, manifestElement.getAttributes().getLength());
        assert(manifestElement.hasAttribute("threshold"));
        assertEquals("75.000000", manifestElement.getAttribute("threshold"));

        final NodeList childNodes = manifestElement.getChildNodes();
        assertEquals(8, childNodes.getLength());
        final Node globalInfoNode = childNodes.item(0);
        assertEquals("slstr:globalInfo", globalInfoNode.getNodeName());
        assert(globalInfoNode.hasAttributes());
        final NamedNodeMap globalInfoNodeAttributes = globalInfoNode.getAttributes();
        assertEquals(5, globalInfoNodeAttributes.getLength());
        assertNotNull(globalInfoNodeAttributes.getNamedItem("grid"));
        assertEquals("0.5 km stripe A", globalInfoNodeAttributes.getNamedItem("grid").getNodeValue());
        assertNotNull(globalInfoNodeAttributes.getNamedItem("view"));
        assertEquals("Nadir", globalInfoNodeAttributes.getNamedItem("view").getNodeValue());
        assertNotNull(globalInfoNodeAttributes.getNamedItem("value"));
        assertEquals("4000", globalInfoNodeAttributes.getNamedItem("value").getNodeValue());
        assertNotNull(globalInfoNodeAttributes.getNamedItem("over"));
        assertEquals("4000", globalInfoNodeAttributes.getNamedItem("over").getNodeValue());
        assertNotNull(globalInfoNodeAttributes.getNamedItem("percentage"));
        assertEquals("100.000000", globalInfoNodeAttributes.getNamedItem("percentage").getNodeValue());

        assertElement(childNodes.item(6), "0.5 km stripe A, 0.5 km stripe B, 0.5 km TDI", "Oblique",
                "2018-04-11T03:22:56.929785Z", "2018-04-11T03:27:56.840589Z", "100.000000", "S1, S2, S3, S4");
        assertElement(childNodes.item(7), "0.5 km stripe A, 0.5 km stripe B, 0.5 km TDI", "Nadir",
                "2018-04-11T03:22:56.935232Z", "2018-04-11T03:27:56.921087Z", "100.000000", "S1, S2, S3, S4");
    }

    @Test
    public void mergeNodes_interrupted() throws ParserConfigurationException, SAXException, IOException, PDUStitchingException {
        List<Node> fromParents = new ArrayList<>();
        fromParents.add(ManifestTestUtils.createNode(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<slstr:missingElements threshold=\"75\">\n" +
                        "  <slstr:globalInfo grid=\"1 km\" view=\"Oblique\" value=\"77\" over=\"601\" percentage=\"12.81198\"/>\n" +
                        "  <slstr:elementMissing grid=\"1 km\" view=\"Oblique\" startTime=\"2017-10-18T01:25:22.192255Z\" stopTime=\"2017-10-18T01:25:24.592142Z\" percentage=\"10.482529\">\n" +
                        "     <slstr:bandSet>S8, F2</slstr:bandSet>\n" +
                        "  </slstr:elementMissing>\n" +
                        "  <slstr:elementMissing grid=\"1 km\" view=\"Oblique\" startTime=\"2017-10-18T01:25:34.592568Z\" stopTime=\"2017-10-18T01:25:35.292568Z\" percentage=\"2.329451\">\n" +
                        "     <slstr:bandSet>S8</slstr:bandSet>\n" +
                        "  </slstr:elementMissing>\n" +
                        "</slstr:missingElements>").getFirstChild());
        fromParents.add(ManifestTestUtils.createNode(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<slstr:missingElements threshold=\"75\">\n" +
                        "  <slstr:globalInfo grid=\"1 km\" view=\"Oblique\" value=\"15\" over=\"575\" percentage=\"2.608696\"/>\n" +
                        "  <slstr:elementMissing grid=\"1 km\" view=\"Oblique\" startTime=\"2017-10-18T01:25:35.592568Z\" stopTime=\"2017-10-18T01:25:36.442568Z\" percentage=\"0.173913\">\n" +
                        "     <slstr:bandSet>S8</slstr:bandSet>\n" +
                        "  </slstr:elementMissing>\n" +
                        "  <slstr:elementMissing grid=\"1 km\" view=\"Oblique\" startTime=\"2017-10-18T01:25:36.942504Z\" stopTime=\"2017-10-18T01:25:38.142447Z\" percentage=\"0.869565\">\n" +
                        "     <slstr:bandSet>S8</slstr:bandSet>\n" +
                        "  </slstr:elementMissing>\n" +
                        "  <slstr:elementMissing grid=\"1 km\" view=\"Oblique\" startTime=\"2017-10-18T01:25:42.192255Z\" stopTime=\"2017-10-18T01:25:44.562142Z\" percentage=\"1.565217\">\n" +
                        "     <slstr:bandSet>S8, F2</slstr:bandSet>\n" +
                        "  </slstr:elementMissing>\n" +
                        "</slstr:missingElements>").getFirstChild());
        fromParents.add(ManifestTestUtils.createNode(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<slstr:missingElements threshold=\"75\">\n" +
                        "  <slstr:globalInfo grid=\"1 km\" view=\"Oblique\" value=\"10\" over=\"575\" percentage=\"2.608696\"/>\n" +
                        "  <slstr:elementMissing grid=\"1 km\" view=\"Oblique\" startTime=\"2017-10-18T01:25:44.592142Z\" stopTime=\"2017-10-18T01:25:46.592142Z\" percentage=\"1.391304\">\n" +
                        "     <slstr:bandSet>S8, F2</slstr:bandSet>\n" +
                        "  </slstr:elementMissing>\n" +
                        "  <slstr:elementMissing grid=\"1 km\" view=\"Oblique\" startTime=\"2017-10-18T01:25:55.592568Z\" stopTime=\"2017-10-18T01:25:56.592568Z\" percentage=\"0.347826\">\n" +
                        "     <slstr:bandSet>F2</slstr:bandSet>\n" +
                        "  </slstr:elementMissing>\n" +
                        "</slstr:missingElements>").getFirstChild());

        Document manifest = ManifestTestUtils.createDocument();
        final Element manifestElement = manifest.createElement("slstr:missingElements");
        manifest.appendChild(manifestElement);

        new MissingElementsMerger().mergeNodes(fromParents, manifestElement, manifest);

        assertEquals(1, manifestElement.getAttributes().getLength());
        assert(manifestElement.hasAttribute("threshold"));
        assertEquals("75", manifestElement.getAttribute("threshold"));

        final NodeList childNodes = manifestElement.getChildNodes();
        assertEquals(6, childNodes.getLength());
        final Node globalInfoNode = childNodes.item(0);
        assertEquals("slstr:globalInfo", globalInfoNode.getNodeName());
        assert(globalInfoNode.hasAttributes());
        final NamedNodeMap globalInfoNodeAttributes = globalInfoNode.getAttributes();
        assertEquals(5, globalInfoNodeAttributes.getLength());
        assertNotNull(globalInfoNodeAttributes.getNamedItem("grid"));
        assertEquals("1 km", globalInfoNodeAttributes.getNamedItem("grid").getNodeValue());
        assertNotNull(globalInfoNodeAttributes.getNamedItem("view"));
        assertEquals("Oblique", globalInfoNodeAttributes.getNamedItem("view").getNodeValue());
        assertNotNull(globalInfoNodeAttributes.getNamedItem("value"));
        assertEquals("102", globalInfoNodeAttributes.getNamedItem("value").getNodeValue());
        assertNotNull(globalInfoNodeAttributes.getNamedItem("over"));
        assertEquals("1751", globalInfoNodeAttributes.getNamedItem("over").getNodeValue());
        assertNotNull(globalInfoNodeAttributes.getNamedItem("percentage"));
        assertEquals("5.825243", globalInfoNodeAttributes.getNamedItem("percentage").getNodeValue());

        assertElement(childNodes.item(1), "1 km", "Oblique", "2017-10-18T01:25:22.192255Z",
                      "2017-10-18T01:25:24.592142Z", "3.597944", "S8, F2");
        assertElement(childNodes.item(2), "1 km", "Oblique", "2017-10-18T01:25:34.592568Z",
                      "2017-10-18T01:25:36.442568Z", "0.856653", "S8");
        assertElement(childNodes.item(3), "1 km", "Oblique", "2017-10-18T01:25:36.942504Z",
                      "2017-10-18T01:25:38.142447Z", "0.285551", "S8");
        assertElement(childNodes.item(4), "1 km", "Oblique", "2017-10-18T01:25:42.192255Z",
                      "2017-10-18T01:25:46.592142Z", "0.970874", "S8, F2");
        assertElement(childNodes.item(5), "1 km", "Oblique", "2017-10-18T01:25:55.592568Z",
                      "2017-10-18T01:25:56.592568Z", "0.114220", "F2");
    }

    @Test
    public void mergeNodes() throws Exception {
        List<Node> fromParents = new ArrayList<>();
        fromParents.add(ManifestTestUtils.createNode(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "                      <slstr:missingElements threshold=\"75\">\n" +
                        "                        <slstr:globalInfo grid=\"0.5 km stripe A\" view=\"Nadir\" value=\"2396\" over=\"2396\" percentage=\"100.000000\"/>\n" +
                        "                        <slstr:elementMissing grid=\"0.5 km stripe A\" view=\"Nadir\" startTime=\"2016-04-19T12:09:27.150299Z\" stopTime=\"2016-04-19T12:12:26.841151Z\" percentage=\"100.000000\">\n" +
                        "                           <slstr:bandSet>S4</slstr:bandSet>\n" +
                        "                        </slstr:elementMissing>\n" +
                        "                     </slstr:missingElements>").getFirstChild());
        fromParents.add(ManifestTestUtils.createNode(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "                      <slstr:missingElements threshold=\"75\">\n" +
                        "                        <slstr:globalInfo grid=\"0.5 km stripe A\" view=\"Nadir\" value=\"2396\" over=\"2396\" percentage=\"100.000000\"/>\n" +
                        "                        <slstr:elementMissing grid=\"0.5 km stripe A\" view=\"Nadir\" startTime=\"2016-04-19T12:12:27.141133Z\" stopTime=\"2016-04-19T12:15:26.831968Z\" percentage=\"100.000000\">\n" +
                        "                           <slstr:bandSet>S4</slstr:bandSet>\n" +
                        "                        </slstr:elementMissing>\n" +
                        "                     </slstr:missingElements>").getFirstChild());
        fromParents.add(ManifestTestUtils.createNode(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "                      <slstr:missingElements threshold=\"75\">\n" +
                        "                        <slstr:globalInfo grid=\"0.5 km stripe A\" view=\"Nadir\" value=\"1948\" over=\"2400\" percentage=\"81.166664\"/>\n" +
                        "                        <slstr:elementMissing grid=\"0.5 km stripe A\" view=\"Nadir\" startTime=\"2016-04-19T12:15:27.131950Z\" stopTime=\"2016-04-19T12:17:53.224483Z\" percentage=\"81.166664\">\n" +
                        "                           <slstr:bandSet>S4</slstr:bandSet>\n" +
                        "                        </slstr:elementMissing>\n" +
                        "                     </slstr:missingElements>").getFirstChild());

        Document manifest = ManifestTestUtils.createDocument();
        final Element manifestElement = manifest.createElement("slstr:missingElements");
        manifest.appendChild(manifestElement);

        new MissingElementsMerger().mergeNodes(fromParents, manifestElement, manifest);

        assertEquals(1, manifestElement.getAttributes().getLength());
        assert(manifestElement.hasAttribute("threshold"));
        assertEquals("75", manifestElement.getAttribute("threshold"));

        final NodeList childNodes = manifestElement.getChildNodes();
        assertEquals(2, childNodes.getLength());
        final Node globalInfoNode = childNodes.item(0);
        assertEquals("slstr:globalInfo", globalInfoNode.getNodeName());
        assert(globalInfoNode.hasAttributes());
        final NamedNodeMap globalInfoNodeAttributes = globalInfoNode.getAttributes();
        assertEquals(5, globalInfoNodeAttributes.getLength());
        assertNotNull(globalInfoNodeAttributes.getNamedItem("grid"));
        assertEquals("0.5 km stripe A", globalInfoNodeAttributes.getNamedItem("grid").getNodeValue());
        assertNotNull(globalInfoNodeAttributes.getNamedItem("view"));
        assertEquals("Nadir", globalInfoNodeAttributes.getNamedItem("view").getNodeValue());
        assertNotNull(globalInfoNodeAttributes.getNamedItem("value"));
        assertEquals("6740", globalInfoNodeAttributes.getNamedItem("value").getNodeValue());
        assertNotNull(globalInfoNodeAttributes.getNamedItem("over"));
        assertEquals("7192", globalInfoNodeAttributes.getNamedItem("over").getNodeValue());
        assertNotNull(globalInfoNodeAttributes.getNamedItem("percentage"));
        assertEquals("93.715239", globalInfoNodeAttributes.getNamedItem("percentage").getNodeValue());

        final Node elementMissingNode = childNodes.item(1);
        assertElement(elementMissingNode, "0.5 km stripe A", "Nadir", "2016-04-19T12:09:27.150299Z",
                      "2016-04-19T12:17:53.224483Z", "93.715239", "S4");
    }

    private void assertElement(Node elementMissingNode, String expectedGrid, String expectedView,
                               String expectedStartTime, String expectedStopTime, String expectedPercentage,
                               String expectedBandSet) {
        assertEquals("slstr:elementMissing", elementMissingNode.getNodeName());
        assert(elementMissingNode.hasAttributes());
        final NamedNodeMap elementMissingNodeAttributes = elementMissingNode.getAttributes();
        assertEquals(5, elementMissingNodeAttributes.getLength());
        assertNotNull(elementMissingNodeAttributes.getNamedItem("grid"));
        assertEquals(expectedGrid, elementMissingNodeAttributes.getNamedItem("grid").getNodeValue());
        assertNotNull(elementMissingNodeAttributes.getNamedItem("view"));
        assertEquals(expectedView, elementMissingNodeAttributes.getNamedItem("view").getNodeValue());
        assertNotNull(elementMissingNodeAttributes.getNamedItem("startTime"));
        assertEquals(expectedStartTime, elementMissingNodeAttributes.getNamedItem("startTime").getNodeValue());
        assertNotNull(elementMissingNodeAttributes.getNamedItem("stopTime"));
        assertEquals(expectedStopTime, elementMissingNodeAttributes.getNamedItem("stopTime").getNodeValue());
        assertNotNull(elementMissingNodeAttributes.getNamedItem("percentage"));
        assertEquals(expectedPercentage, elementMissingNodeAttributes.getNamedItem("percentage").getNodeValue());

        final NodeList elementMissingNodeChildNodes = elementMissingNode.getChildNodes();
        assertEquals(1, elementMissingNodeChildNodes.getLength());
        final Node bandSetNode = elementMissingNodeChildNodes.item(0);
        assertEquals("slstr:bandSet", bandSetNode.getNodeName());
        assertEquals(0, bandSetNode.getAttributes().getLength());
        assertEquals(1, bandSetNode.getChildNodes().getLength());
        assertEquals(expectedBandSet, bandSetNode.getFirstChild().getNodeValue());
        assertEquals(expectedBandSet, bandSetNode.getTextContent());
    }

}