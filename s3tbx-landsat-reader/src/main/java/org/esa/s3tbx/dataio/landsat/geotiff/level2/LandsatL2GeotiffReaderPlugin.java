package org.esa.s3tbx.dataio.landsat.geotiff.level2;

import com.bc.ceres.core.VirtualDir;
import org.esa.s3tbx.dataio.landsat.geotiff.LandsatTypeInfo;
import org.esa.s3tbx.dataio.landsat.tgz.VirtualDirTgz;
import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.core.util.io.SnapFileFilter;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

/**
 * Created by obarrile on 06/02/2019.
 */
public class LandsatL2GeotiffReaderPlugin implements ProductReaderPlugIn {

    private static final Class[] READER_INPUT_TYPES = new Class[]{String.class, File.class};
    private static final String[] FORMAT_NAMES = new String[]{"LandsatL2GeoTIFF"};
    private static final String[] DEFAULT_FILE_EXTENSIONS = new String[]{".xml", ".XML", ".tar.gz", ".tar"};
    private static final String READER_DESCRIPTION = "Landsat L2 Data Products (GeoTIFF)";

    @Override
    public DecodeQualification getDecodeQualification(Object input) {
        String filename = new File(input.toString()).getName();
        if (!LandsatTypeInfo.isLandsatLevel2(filename)) {
            return DecodeQualification.UNABLE;
        }

        VirtualDir virtualDir;
        try {
            virtualDir = getInput(input);
        } catch (IOException e) {
            return DecodeQualification.UNABLE;
        }

        return getDecodeQualification(virtualDir);

    }

    static DecodeQualification getDecodeQualification(VirtualDir virtualDir) {
        if (virtualDir == null) {
            return DecodeQualification.UNABLE;
        }

        String[] allFiles;
        try {
            allFiles = virtualDir.listAllFiles();
            if (allFiles == null || allFiles.length == 0) {
                return DecodeQualification.UNABLE;
            }
        } catch (IOException e) {
            return DecodeQualification.UNABLE;
        }

        for (String filePath : allFiles) {
                if (isMetadataFilename(new File(filePath).getName())) {
                    //other check inside the file? TODO
                    return DecodeQualification.INTENDED;
                }
        }
        // didn't find the expected metadata file
        return DecodeQualification.UNABLE;
    }

    static boolean isMetadataFilename(String filename) {
        return (LandsatTypeInfo.isLandsatLevel2(filename) && filename.toLowerCase().endsWith(".xml"));
    }


    @Override
    public Class[] getInputTypes() {
        return READER_INPUT_TYPES;
    }

    @Override
    public ProductReader createReaderInstance() {
        return new LandsatL2GeotiffReader(this);
    }

    @Override
    public String[] getFormatNames() {
        return FORMAT_NAMES;
    }

    @Override
    public String[] getDefaultFileExtensions() {
        return DEFAULT_FILE_EXTENSIONS;
    }

    @Override
    public String getDescription(Locale locale) {
        return READER_DESCRIPTION;
    }

    @Override
    public SnapFileFilter getProductFileFilter() {
        return new SnapFileFilter(FORMAT_NAMES[0], DEFAULT_FILE_EXTENSIONS, READER_DESCRIPTION);
    }

    static VirtualDir getInput(Object input) throws IOException {
        File inputFile = getFileInput(input);

        if (inputFile == null) {
            throw new IOException("Unknown input type.");
        }
        if (inputFile.isFile() && !isCompressedFile(inputFile)) {
            final File absoluteFile = inputFile.getAbsoluteFile();
            inputFile = absoluteFile.getParentFile();
            if (inputFile == null) {
                throw new IOException("Unable to retrieve parent to file: " + absoluteFile.getAbsolutePath());
            }
        }

        VirtualDir virtualDir = VirtualDir.create(inputFile);
        if (virtualDir == null) {
            virtualDir = new VirtualDirTgz(inputFile);
        }
        return virtualDir;
    }

    static File getFileInput(Object input) {
        if (input instanceof String) {
            return new File((String) input);
        } else if (input instanceof File) {
            return (File) input;
        }
        return null;
    }

    static boolean isCompressedFile(File file) {
        String extension = FileUtils.getExtension(file);
        if (StringUtils.isNullOrEmpty(extension)) {
            return false;
        }

        extension = extension.toLowerCase();

        return extension.contains("zip")
                || extension.contains("tar")
                || extension.contains("tgz")
                || extension.contains("gz")
                || extension.contains("tbz")
                || extension.contains("bz")
                || extension.contains("tbz2")
                || extension.contains("bz2");
    }
}
