package org.esa.s3tbx.c2rcc.util;

import org.esa.snap.core.gpf.OperatorException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * @author Marco Peters
 */
public class NNUtils {

    public static final String[] ALTERNATIVE_NET_DIR_NAMES = new String[]{
            "rtosa_aann",
            "rtosa_rw",
            "rw_iop",
            "iop_rw",
            "rw_kd",
            "iop_unciop",
            "iop_uncsumiop_unckd",
            "rw_rwnorm",
            "rtosa_trans",
            "rtosa_rpath"
    };

    public static String[] getNNFilePaths(Path nnRootPath, String[] alternativeNetDirNames) throws IOException {
        final ArrayList<String> pathsList = new ArrayList<>();
        final String prefix = "The path '" + nnRootPath.toString() + "' ";

        if (!Files.isDirectory(nnRootPath)) {
            throw new OperatorException(prefix + "for alternative neuronal nets is not a valid path");
        }

        final HashSet<String> dirNames = new HashSet<>();
        Files.newDirectoryStream(nnRootPath).forEach(path -> {
            if (Files.isDirectory(path)) {
                dirNames.add(path.getFileName().toString());
            }
        });
        for (String alternativeNetDirName : alternativeNetDirNames) {
            if (!containsIgnoreCase(dirNames, alternativeNetDirName)) {
                throw new OperatorException(prefix + "does not contain the expected sub directory '" + alternativeNetDirName + "'");
            }
            String dirName = getIgnoreCase(dirNames, alternativeNetDirName);
            final int[] dotNetFilesCount = {0};
            final Path nnDirPath = nnRootPath.resolve(dirName);
            Files.newDirectoryStream(nnDirPath).forEach(path -> {
                if (path.getFileName().toString().toLowerCase().endsWith(".net")
                        && Files.isRegularFile(path)) {
                    dotNetFilesCount[0]++;
                    pathsList.add(path.toString());
                }
            });
            int count = dotNetFilesCount[0];
            if (count != 1) {
                throw new OperatorException("The path '" + nnDirPath + " must contain exact 1 file with file ending '*.net', but contains " + count);
            }
        }

        return pathsList.toArray(new String[pathsList.size()]);
    }

    private static String getIgnoreCase(HashSet<String> dirNames, String alternativeNetDirName) {
        return dirNames.stream().filter(dirName -> dirName.equalsIgnoreCase(alternativeNetDirName)).findFirst().orElse(null);
    }

    private static boolean containsIgnoreCase(HashSet<String> dirNames, String alternativeNetDirName) {
        return dirNames.stream().anyMatch(dirName -> dirName.equalsIgnoreCase(alternativeNetDirName));
    }
}
