package org.esa.s3tbx.slstr.pdu.stitching;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.converters.DateFormatConverter;
import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;
import org.esa.s3tbx.slstr.pdu.stitching.manifest.ManifestMerger;
import org.esa.snap.runtime.EngineConfig;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * @author Tonio Fincke
 */
public class SlstrPduStitcher {

    private static final DateFormatConverter SLSTR_DATE_FORMAT_CONVERTER =
            new DateFormatConverter(new SimpleDateFormat("yyyyMMdd'T'HHmmss"));
    private static final ImageSize NULL_IMAGE_SIZE = new ImageSize("null", 0, 0, 0, 0);

    public static File createStitchedSlstrL1BFile(File targetDirectory, File[] slstrProductFiles, String productName,
                                                  ProgressMonitor pm)
            throws IllegalArgumentException, IOException, PDUStitchingException,
            ParserConfigurationException,  TransformerException {
        Assert.notNull(slstrProductFiles);
        final Logger logger = EngineConfig.instance().logger();
        Validator.validateSlstrProductFiles(slstrProductFiles);

        final Date now = Calendar.getInstance().getTime();
        if (slstrProductFiles.length == 1) {
            final File originalParentDirectory = slstrProductFiles[0].getParentFile();
            final String parentDirectoryName = originalParentDirectory.getName();
            File stitchedParentDirectory;
            if (productName == null) {
                stitchedParentDirectory = new File(targetDirectory, parentDirectoryName);
            } else {
                stitchedParentDirectory = new File(targetDirectory, productName);
            }
            if (stitchedParentDirectory.exists()) {
                throw new PDUStitchingException("Target file directory already exists");
            }
            Files.copy(originalParentDirectory.getParentFile().toPath(), stitchedParentDirectory.toPath());
            final File[] files = originalParentDirectory.listFiles();
            long productSize = 0;
            if (files != null) {
                for (File originalFile : files) {
                    final File newFile = new File(stitchedParentDirectory, originalFile.getName());
                    Files.copy(originalFile.toPath(), newFile.toPath());
                    productSize += newFile.length();
                }
            }
            return createManifestFile(slstrProductFiles, stitchedParentDirectory, now, productSize);
        }
        SlstrNameDecomposition[] slstrNameDecompositions = new SlstrNameDecomposition[slstrProductFiles.length];
        Document[] manifestDocuments = new Document[slstrProductFiles.length];
        List<String> ncFileNames = new ArrayList<>();
        Map<String, ImageSize[]> idToImageSizes = new HashMap<>();
        for (int i = 0; i < slstrProductFiles.length; i++) {
            slstrNameDecompositions[i] = decomposeSlstrName(slstrProductFiles[i].getParentFile().getName());
            manifestDocuments[i] = createXmlDocument(new FileInputStream(slstrProductFiles[i]));
            final ImageSize[] imageSizes = ImageSizeHandler.extractImageSizes(manifestDocuments[i]);
            for (ImageSize imageSize : imageSizes) {
                if (idToImageSizes.containsKey(imageSize.getIdentifier())) {
                    idToImageSizes.get(imageSize.getIdentifier())[i] = imageSize;
                } else {
                    final ImageSize[] mapImageSizes = new ImageSize[slstrProductFiles.length];
                    mapImageSizes[i] = imageSize;
                    idToImageSizes.put(imageSize.getIdentifier(), mapImageSizes);
                }
            }
            collectFiles(ncFileNames, manifestDocuments[i]);
        }
        Validator.validateOrbitReference(manifestDocuments);
        Validator.validateAdjacency(manifestDocuments);
        String stitchedProductFileName = productName;
        if (stitchedProductFileName == null) {
            stitchedProductFileName = createParentDirectoryNameOfStitchedFile(slstrNameDecompositions, now);
        }
        File stitchedProductFileParentDirectory = new File(targetDirectory, stitchedProductFileName);
        if (stitchedProductFileParentDirectory.exists()) {
            throw new PDUStitchingException("Target file directory already exists");
        }
        if (!stitchedProductFileParentDirectory.mkdirs()) {
            throw new PDUStitchingException("Could not create product directory");
        }
        Map<String, ImageSize> idToTargetImageSize = new HashMap<>();
        for (String id : idToImageSizes.keySet()) {
            idToTargetImageSize.put(id, ImageSizeHandler.createTargetImageSize(idToImageSizes.get(id)));
        }
        long productSize = 0;
        File manifestFile;
        pm.beginTask("Stitching SLSTR L1B Product Dissemination Units", ncFileNames.size() + 1);
        try {
            for (int i = 0; i < ncFileNames.size(); i++) {
                final String ncFileName = ncFileNames.get(i);
                String[] splitFileName = ncFileName.split("/");
                final String displayFileName = splitFileName[splitFileName.length - 1];
                pm.setSubTaskName(MessageFormat.format("Stitching ''{0}''", displayFileName));
                List<File> ncFiles = new ArrayList<>();
                List<ImageSize> imageSizeList = new ArrayList<>();
                String id = ncFileName.substring(ncFileName.length() - 5, ncFileName.length() - 3);
                if (id.equals("tx")) {
                    id = "tn";
                }
                ImageSize targetImageSize = idToTargetImageSize.get(id);
                if (targetImageSize == null) {
                    targetImageSize = NULL_IMAGE_SIZE;
                }
                ImageSize[] imageSizes = idToImageSizes.get(id);
                if (imageSizes == null) {
                    imageSizes = new ImageSize[ncFileNames.size()];
                    Arrays.fill(imageSizes, NULL_IMAGE_SIZE);
                }
                for (int j = 0; j < slstrProductFiles.length; j++) {
                    File slstrProductFile = slstrProductFiles[j];
                    File ncFile = new File(slstrProductFile.getParentFile(), ncFileName);
                    if (ncFile.exists()) {
                        ncFiles.add(ncFile);
                        imageSizeList.add(imageSizes[j]);
                    }
                }
                if (ncFiles.size() > 0) {
                    final File[] ncFilesArray = ncFiles.toArray(new File[ncFiles.size()]);
                    final ImageSize[] imageSizeArray = imageSizeList.toArray(new ImageSize[imageSizeList.size()]);
                    logger.log(Level.INFO, "Stitch " + displayFileName);
                    NcFileStitcher.stitchNcFiles(ncFileName, stitchedProductFileParentDirectory, now,
                            ncFilesArray, targetImageSize, imageSizeArray);
                    productSize += new File(stitchedProductFileParentDirectory, ncFileName).length();
                }
                if (pm.isCanceled()) {
                    return null;
                }
                pm.worked(1);
            }
            pm.setSubTaskName("Stitching manifest");
            logger.log(Level.INFO, "Stitch manifest");
            manifestFile = createManifestFile(slstrProductFiles, stitchedProductFileParentDirectory, now, productSize);
            pm.worked(1);
        } finally {
            pm.done();
        }
        return manifestFile;
    }

    private static File createManifestFile(File[] manifestFiles, File stitchedParentDirectory, Date now, long productSize)
            throws ParserConfigurationException, PDUStitchingException, IOException, TransformerException {
        return new ManifestMerger().createMergedManifest(manifestFiles, now, stitchedParentDirectory, productSize);
    }

    static void collectFiles(List<String> ncFileNames, Document manifestDocument) {
        final NodeList fileLocationNodes = manifestDocument.getElementsByTagName("fileLocation");
        for (int i = 0; i < fileLocationNodes.getLength(); i++) {
            final String ncFileName = fileLocationNodes.item(i).getAttributes().getNamedItem("href").getNodeValue();
            if (!ncFileNames.contains(ncFileName)) {
                ncFileNames.add(ncFileName);
            }
        }
    }

    static Document createXmlDocument(InputStream inputStream) throws IOException {
        final String msg = "Cannot create document from manifest XML file.";
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream);
        } catch (SAXException | ParserConfigurationException e) {
            throw new IOException(msg, e);
        }
    }

    static String createParentDirectoryNameOfStitchedFile(SlstrNameDecomposition[] slstrNameDecompositions, Date now) {
        Date startTime = extractStartTime(slstrNameDecompositions);
        Date stopTime = extractStopTime(slstrNameDecompositions);
        final StringBuilder slstrNameStringBuilder = new StringBuilder("S3A_SL_1_RBT___");
        String[] slstrNameParts = new String[]{SLSTR_DATE_FORMAT_CONVERTER.format(startTime),
                SLSTR_DATE_FORMAT_CONVERTER.format(stopTime), SLSTR_DATE_FORMAT_CONVERTER.format(now),
                slstrNameDecompositions[0].duration, slstrNameDecompositions[0].cycleNumber, slstrNameDecompositions[0].relativeOrbitNumber,
                slstrNameDecompositions[0].frameAlongTrackCoordinate, slstrNameDecompositions[0].fileGeneratingCentre, slstrNameDecompositions[0].platform,
                slstrNameDecompositions[0].timelinessOfProcessingWorkflow, slstrNameDecompositions[0].baselineCollectionOrDataUsage};
        for (String namePart : slstrNameParts) {
            slstrNameStringBuilder.append("_").append(namePart);
        }
        slstrNameStringBuilder.append(".SEN3");
        return slstrNameStringBuilder.toString();
    }

    private static Date extractStartTime(SlstrNameDecomposition[] slstrNameDecompositions) {
        Date earliestDate = new GregorianCalendar(3000, 1, 1).getTime();
        for (SlstrNameDecomposition slstrNameDecomposition : slstrNameDecompositions) {
            final Date startTime = slstrNameDecomposition.startTime;
            if (startTime.before(earliestDate)) {
                earliestDate = startTime;
            }
        }
        return earliestDate;
    }

    private static Date extractStopTime(SlstrNameDecomposition[] slstrNameDecompositions) {
        Date latestDate = new GregorianCalendar(1800, 1, 1).getTime();
        for (SlstrNameDecomposition slstrNameDecomposition : slstrNameDecompositions) {
            final Date stopTime = slstrNameDecomposition.stopTime;
            if (stopTime.after(latestDate)) {
                latestDate = stopTime;
            }
        }
        return latestDate;
    }

    static SlstrNameDecomposition decomposeSlstrName(String slstrName) throws PDUStitchingException {
        final SlstrNameDecomposition slstrNameDecomposition = new SlstrNameDecomposition();
        try {
            slstrNameDecomposition.startTime = SLSTR_DATE_FORMAT_CONVERTER.parse(slstrName.substring(16, 31));
            slstrNameDecomposition.stopTime = SLSTR_DATE_FORMAT_CONVERTER.parse(slstrName.substring(32, 47));
        } catch (ConversionException e) {
            throw new PDUStitchingException(e.getMessage());
        }
        slstrNameDecomposition.duration = slstrName.substring(64, 68);
        slstrNameDecomposition.cycleNumber = slstrName.substring(69, 72);
        slstrNameDecomposition.relativeOrbitNumber = slstrName.substring(73, 76);
        slstrNameDecomposition.frameAlongTrackCoordinate = slstrName.substring(77, 81);
        slstrNameDecomposition.fileGeneratingCentre = slstrName.substring(82, 85);
        slstrNameDecomposition.platform = slstrName.substring(86, 87);
        slstrNameDecomposition.timelinessOfProcessingWorkflow = slstrName.substring(88, 90);
        slstrNameDecomposition.baselineCollectionOrDataUsage = slstrName.substring(91, 94);
        return slstrNameDecomposition;
    }

    static class SlstrNameDecomposition {
        Date startTime;
        Date stopTime;
        String duration;
        String cycleNumber;
        String relativeOrbitNumber;
        String frameAlongTrackCoordinate;
        String fileGeneratingCentre;
        String platform;
        String timelinessOfProcessingWorkflow;
        String baselineCollectionOrDataUsage;
    }

}
