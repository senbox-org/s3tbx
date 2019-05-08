package org.esa.s3tbx.slstr.pdu.stitching.manifest;

import org.esa.s3tbx.slstr.pdu.stitching.PDUStitchingException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.swing.text.NumberFormatter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author Tonio Fincke
 */
class MissingElementsMerger extends AbstractElementMerger {

    private final static String SEPARATOR = "_";
    private final static String GLOBAL_INFO_NAME = "slstr:globalInfo";
    private final static String ELEMENT_MISSING_NAME = "slstr:elementMissing";
    private final static String BAND_SET_ELEMENT = "slstr:bandSet";
    private final static long NUMBER_OF_MILLIS_BETWEEN_ENTRIES = 450;
    private final static Logger logger = Logger.getLogger(MissingElementsMerger.class.getSimpleName());


    @Override
    public void mergeNodes(List<Node> fromParents, Element toParent, Document toDocument) throws PDUStitchingException {
        final String threshold = fromParents.get(0).getAttributes().getNamedItem("threshold").getNodeValue();
        toParent.setAttribute("threshold", threshold);
        final NumberFormatter numberFormatter = getNumberFormatter();
        final Map<String, List<Node>> globalInfoNodesListMap = collectNodes(fromParents, GLOBAL_INFO_NAME);
        Map<String, Double> overs = new HashMap<>();
        for (Map.Entry<String, List<Node>> globalInfoNodesEntry : globalInfoNodesListMap.entrySet()) {
            final String[] key = globalInfoNodesEntry.getKey().split(SEPARATOR);
            final String gridValue = key[0];
            final String viewValue = key[1];
            int value = 0;
            int over = 0;
            final List<Node> globalInfoNodes = globalInfoNodesEntry.getValue();
            for (Node globalInfoNode : globalInfoNodes) {
                final Node valueNode = globalInfoNode.getAttributes().getNamedItem("value");
                final Node overNode = globalInfoNode.getAttributes().getNamedItem("over");
                if (valueNode != null && overNode != null) {
                    value += Integer.parseInt(valueNode.getNodeValue());
                    final int over1 = Integer.parseInt(overNode.getNodeValue());
                    over += over1;
                }
            }
            String percentage;
            try {
                overs.put(globalInfoNodesEntry.getKey(), (double) over);
                final double percentageAsDouble = ((double) value / over) * 100;
                percentage = numberFormatter.valueToString(percentageAsDouble);
            } catch (ParseException e) {
                throw new PDUStitchingException("Could not format number: " + e.getMessage());
            }
            final Element globalInfoElement = toDocument.createElement(GLOBAL_INFO_NAME);
            globalInfoElement.setAttribute("grid", gridValue);
            globalInfoElement.setAttribute("view", viewValue);
            globalInfoElement.setAttribute("value", String.valueOf(value));
            globalInfoElement.setAttribute("over", String.valueOf(over));
            globalInfoElement.setAttribute("percentage", percentage);
            toParent.appendChild(globalInfoElement);
        }

        final List<NewNode> newMissingNodes = getNewMissingNodes(fromParents);
        final List<NewNode> sortedNodes = new ArrayList<>();
        for (NewNode newNode : newMissingNodes) {
            boolean inserted = false;
            for (int j = 0; j < sortedNodes.size(); j++) {
                final NewNode sortedNode = sortedNodes.get(j);
                if (newNode.startTime.before(sortedNode.startTime)) {
                    sortedNodes.add(j, newNode);
                    inserted = true;
                    break;
                }
            }
            if (!inserted) {
                sortedNodes.add(newNode);
            }
        }
        for (NewNode node : sortedNodes) {
            String key = node.gridValue.split(",")[0].trim() + SEPARATOR + node.viewValue;
            double over = overs.get(key);
            try {
                final String percentage = numberFormatter.valueToString((node.value / over) * 100);
                addNode(toDocument, toParent, node, percentage);
            } catch (ParseException e) {
                throw new PDUStitchingException("Could not format number: " + e.getMessage());
            }
        }
    }

    private void addNode(Document toDocument, Element toParent, NewNode node, String percentage) throws PDUStitchingException {
        final Element elementMissingElement = toDocument.createElement(ELEMENT_MISSING_NAME);
        elementMissingElement.setAttribute("grid", node.gridValue);
        elementMissingElement.setAttribute("view", node.viewValue);
        elementMissingElement.setAttribute("startTime", node.startTimeAsNodeValue);
        elementMissingElement.setAttribute("stopTime", node.stopTimeAsNodeValue);
        elementMissingElement.setAttribute("percentage", percentage);
        final Element bandSetElement = toDocument.createElement(BAND_SET_ELEMENT);
        addTextToNode(bandSetElement, node.bandSet, toDocument);
        elementMissingElement.appendChild(bandSetElement);
        toParent.appendChild(elementMissingElement);
    }

    private String getBandSetFromNode(Node node) {
        final NodeList elementMissingNodeChildNodes = node.getChildNodes();
        for (int i = 0; i < elementMissingNodeChildNodes.getLength(); i++) {
            final Node childNode = elementMissingNodeChildNodes.item(i);
            if (childNode.getNodeName().equals(BAND_SET_ELEMENT)) {
                return childNode.getTextContent();
            }
        }
        return "";
    }

    private String getStartTimeFromNode(Node node) throws PDUStitchingException {
        final Node startTimeNode = node.getAttributes().getNamedItem("startTime");
        return startTimeNode.getNodeValue();
    }

    private String getStopTimeFromNode(Node node) throws PDUStitchingException {
        final Node stopTimeNode = node.getAttributes().getNamedItem("stopTime");
        return stopTimeNode.getNodeValue();
    }

    private double getPercentageFromNode(Node node) throws PDUStitchingException {
        final Node percentageNode = node.getAttributes().getNamedItem("percentage");
        final String percentageNodeValue = percentageNode.getNodeValue();
        return Double.parseDouble(percentageNodeValue);
    }

    private String getGridFromNode(Node node) throws PDUStitchingException {
        final Node gridNode = node.getAttributes().getNamedItem("grid");
        return gridNode.getNodeValue();
    }

    private String getViewFromNode(Node node) throws PDUStitchingException {
        final Node viewNode = node.getAttributes().getNamedItem("view");
        return viewNode.getNodeValue();
    }

    private int getOverFromNode(Node node) throws PDUStitchingException {
        final Node overNode = node.getAttributes().getNamedItem("over");
        return Integer.parseInt(overNode.getNodeValue());
    }

    private NumberFormatter getNumberFormatter() {
        final DecimalFormat format = new DecimalFormat("0.000000");
        format.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ENGLISH));
        return new NumberFormatter(format);
    }

    private List<NewNode> getNewMissingNodes(List<Node> fromParents) throws PDUStitchingException {
        final List<NewNode> newMissingElements = new ArrayList<>();
        for (final Node parent : fromParents) {
            final Map<String, Integer> overs = new HashMap<>();
            final NodeList childNodes = parent.getChildNodes();
            int i;
            for (i = 0; i < childNodes.getLength(); i++) {
                final Node childNode = childNodes.item(i);
                final String nodeName = childNode.getNodeName();
                if (nodeName.equals(GLOBAL_INFO_NAME)) {
                    final String childGrid = getGridFromNode(childNode);
                    final String childView = getViewFromNode(childNode);
                    final int childOver = getOverFromNode(childNode);
                    String key = childGrid + SEPARATOR + childView;
                    overs.put(key, childOver);
                }
            }
            for (int j = 0; j < childNodes.getLength(); j++) {
                final Node childNode = childNodes.item(j);
                final String nodeName = childNode.getNodeName();
                if (nodeName.equals(ELEMENT_MISSING_NAME)) {
                    final String childGrids = getGridFromNode(childNode);
                    final String childView = getViewFromNode(childNode);
                    final String childStartTime = getStartTimeFromNode(childNode);
                    final String childStopTime = getStopTimeFromNode(childNode);
                    final String childBandSet = getBandSetFromNode(childNode);
                    final double childPercentage = getPercentageFromNode(childNode);
                    Integer over = 0;

                    for (String childGrid : childGrids.split(",")) {
                        String key = childGrid.trim() + SEPARATOR + childView;
                        if (!overs.containsKey(key)) {
                            logger.warning("Could not determine grid of missing element: " + key);
                            continue;
                        }
                        over = overs.get(key);
                    }
                    final int childValue = (int) Math.round((over / 100.0) * childPercentage);
                    boolean contained = false;
                    for (final NewNode missingNode : newMissingElements) {
                        if (missingNode.belongsToNode(childGrids, childView, childStartTime, childStopTime, childBandSet)) {
                            missingNode.incorporate(childStartTime, childStopTime, childValue);
                            contained = true;
                        }
                    }
                    if (!contained) {
                        final NewNode newNode = new NewNode(childGrids, childView, childStartTime, childStopTime,
                                                            childBandSet, childValue);
                        newMissingElements.add(newNode);
                    }
                }
            }
        }
        for (NewNode newNode : newMissingElements) {
            for (NewNode otherNode : newMissingElements) {
                if (newNode != otherNode && newNode.belongsToNode(otherNode)) {
                    newNode.incorporate(otherNode);
                    newMissingElements.remove(otherNode);
                }
            }
        }
        return newMissingElements;
    }

    private Map<String, List<Node>> collectNodes(List<Node> fromParents, String wantedNodeName) throws PDUStitchingException {
        Map<String, List<Node>> nodesMap = new LinkedHashMap<>();
        for (final Node parent : fromParents) {
            final NodeList childNodes = parent.getChildNodes();
            for (int j = 0; j < childNodes.getLength(); j++) {
                final Node childNode = childNodes.item(j);
                final String nodeName = childNode.getNodeName();
                if (nodeName.equals(wantedNodeName)) {
                    final String gridFromNode = getGridFromNode(childNode);
                    final String viewFromNode = getViewFromNode(childNode);
                    String identifier = gridFromNode + SEPARATOR + viewFromNode;
                    if (nodesMap.containsKey(identifier)) {
                        nodesMap.get(identifier).add(childNode);
                    } else {
                        final ArrayList<Node> nodeList = new ArrayList<>();
                        nodeList.add(childNode);
                        nodesMap.put(identifier, nodeList);
                    }
                }
            }
        }
        return nodesMap;
    }

    private class NewNode {

        private final String gridValue;
        private final String viewValue;
        private String startTimeAsNodeValue;
        private Date startTime;
        private String stopTimeAsNodeValue;
        private Date stopTime;
        private final String bandSet;
        private int value;

        NewNode(String gridValue, String viewValue, String startTime, String stopTime, String bandSet, int value) throws PDUStitchingException {
            this.gridValue = gridValue;
            this.viewValue = viewValue;
            this.startTimeAsNodeValue = startTime;
            this.startTime = parseDate(startTime);
            this.stopTimeAsNodeValue = stopTime;
            this.stopTime = parseDate(stopTime);
            this.bandSet = bandSet;
            this.value = value;
        }

        boolean belongsToNode(NewNode otherNode) throws PDUStitchingException {
            return belongsToNode(otherNode.gridValue, otherNode.viewValue, otherNode.startTimeAsNodeValue,
                                 otherNode.stopTimeAsNodeValue, otherNode.bandSet);
        }

        boolean belongsToNode(String otherGrid, String otherView, String otherStartTime, String otherStopTime,
                              String otherBandSet) throws PDUStitchingException {
            if (!bandSet.equals(otherBandSet) || !gridValue.equals(otherGrid) || !viewValue.equals(otherView)) {
                return false;
            }
            final Date otherStartTimeAsDate = parseDate(otherStartTime);
            if (otherStartTimeAsDate.getTime() - stopTime.getTime() > NUMBER_OF_MILLIS_BETWEEN_ENTRIES) {
                return false;
            }
            final Date otherStopTimeAsDate = parseDate(otherStopTime);
            return startTime.getTime() - otherStopTimeAsDate.getTime() < NUMBER_OF_MILLIS_BETWEEN_ENTRIES;
        }

        void incorporate(NewNode otherNode) throws PDUStitchingException {
            incorporate(otherNode.startTimeAsNodeValue, otherNode.stopTimeAsNodeValue, otherNode.value);
        }

        void incorporate(String otherStartTime, String otherStopTime, int otherValue) throws PDUStitchingException {
            final Date otherStopTimeAsStopDate = parseDate(otherStopTime);
            final Date otherStartTimeAsStartDate = parseDate(otherStartTime);
            if (startTime.before(otherStopTimeAsStopDate)) {
                stopTime = otherStopTimeAsStopDate;
                stopTimeAsNodeValue = otherStopTime;
            } else {
                startTime = otherStartTimeAsStartDate;
                startTimeAsNodeValue = otherStartTime;
            }
            value += otherValue;
        }

    }

}
