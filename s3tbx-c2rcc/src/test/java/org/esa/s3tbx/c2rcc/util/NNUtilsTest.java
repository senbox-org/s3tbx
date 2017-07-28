package org.esa.s3tbx.c2rcc.util;

import com.google.common.jimfs.Jimfs;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class NNUtilsTest {

    private Path testDir;
    private String sep;

    @Before
    public void setUp() throws Exception {
        FileSystem fileSystem = Jimfs.newFileSystem();
        sep = fileSystem.getSeparator();
        testDir = fileSystem.getPath("test-dir");
        Files.createDirectories(testDir);
    }

    @After
    public void tearDown() throws Exception {
        testDir.getFileSystem().close();
    }

    @Test
    public void testGetNNFilePaths() throws Exception {
        for (int i = 0; i < 10; i++) {
            final Path nnDir = testDir.resolve(NNUtils.ALTERNATIVE_NET_DIR_NAMES[i]);
            Files.createDirectories(nnDir);
            final Path nnfile = nnDir.resolve("nnFile_" + i + ".net");
            Files.createFile(nnfile);
        }

        final String[] nnFilePaths = NNUtils.getNNFilePaths(testDir, NNUtils.ALTERNATIVE_NET_DIR_NAMES);
        assertNotNull(nnFilePaths);
        assertEquals(10, nnFilePaths.length);
        for (int i = 0; i < NNUtils.ALTERNATIVE_NET_DIR_NAMES.length; i++) {
            String alternativeNetDirName = NNUtils.ALTERNATIVE_NET_DIR_NAMES[i];
            assertTrue(nnFilePaths[i].contains(sep + alternativeNetDirName + sep));
        }
    }


    @Test
    public void testGetNNFilePaths_ignoringUpperCase() throws Exception {
        final String[] alternativeNetDirNamesWithUpperCase = new String[]{
                "rtosa_aaNN",
                "rtosa_rw",
                "rw_iop",
                "iop_rw",
                "rw_kd",
                "iop_unCIop",
                "iop_uncsumiop_unckd",
                "rw_rwNORM",
                "rtosa_trans",
                "rtosa_rpath"
        };

        for (int i = 0; i < 10; i++) {
            final Path nnDir = testDir.resolve(alternativeNetDirNamesWithUpperCase[i]);
            Files.createDirectories(nnDir);
            final Path nnfile = nnDir.resolve("nnFile_" + i + ".net");
            Files.createFile(nnfile);
        }

        final String[] nnFilePaths = NNUtils.getNNFilePaths(testDir, NNUtils.ALTERNATIVE_NET_DIR_NAMES);
        assertNotNull(nnFilePaths);
        assertEquals(10, nnFilePaths.length);
        for (int i = 0; i < NNUtils.ALTERNATIVE_NET_DIR_NAMES.length; i++) {
            String nameWithUpperCase = alternativeNetDirNamesWithUpperCase[i];
            assertTrue(nnFilePaths[i].contains(sep + nameWithUpperCase + sep));
        }
    }
}