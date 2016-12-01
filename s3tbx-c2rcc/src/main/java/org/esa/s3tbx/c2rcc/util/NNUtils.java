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
    public static String[] getNNFilePaths(Path nnRootPath, String[] alternativeNetDirNames) throws IOException {
        final ArrayList<String> pathsList = new ArrayList<>();
        final String Präfix = "The path '" + nnRootPath.toString() + "' ";

        if (!Files.isDirectory(nnRootPath)) {
            throw new OperatorException(Präfix + "for alternative neuronal nets is not a valid path");
        }

        final HashSet<String> dirNames = new HashSet<>();
        Files.newDirectoryStream(nnRootPath).forEach(path -> {
            if (Files.isDirectory(path)) {
                dirNames.add(path.getFileName().toString());
            }
        });
        for (String alternativeNetDirName : alternativeNetDirNames) {
            if (!dirNames.contains(alternativeNetDirName)) {
                throw new OperatorException(Präfix + "does not contain the expected sub directory '" + alternativeNetDirName + "'");
            }
            final int[] dotNetFilesCount = {0};
            final Path nnDirPath = nnRootPath.resolve(alternativeNetDirName);
            Files.newDirectoryStream(nnDirPath).forEach(path -> {
                if (path.getFileName().toString().toLowerCase().endsWith(".net")
                        && Files.isRegularFile(path)) {
                    dotNetFilesCount[0]++;
                    pathsList.add(path.toString());
                }
            });
            int count = dotNetFilesCount[0];
            if (count != 1) {
                throw new OperatorException("The path '" + nnDirPath + " must contain exact 1 file whith file ending '*.net' but contains " + count);
            }
        }

        return pathsList.toArray(new String[pathsList.size()]);
    }
}
