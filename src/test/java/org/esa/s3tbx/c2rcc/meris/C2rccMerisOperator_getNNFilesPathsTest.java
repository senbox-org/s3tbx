package org.esa.s3tbx.c2rcc.meris;

import static org.esa.s3tbx.c2rcc.meris.C2rccMerisOperator.alternativeNetDirNames;
import static org.junit.Assert.*;

import com.google.common.jimfs.Jimfs;
import org.junit.*;

import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created by Sabine on 29.10.2015.
 */
public class C2rccMerisOperator_getNNFilesPathsTest {

    private Path testDir;
    private FileSystem fileSystem;
    private String sep;

    @Before
    public void setUp() throws Exception {
        fileSystem = Jimfs.newFileSystem();
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
            final Path nnDir = testDir.resolve(alternativeNetDirNames[i]);
            Files.createDirectories(nnDir);
            final Path nnfile = nnDir.resolve("nnFile_" + i + ".net");
            Files.createFile(nnfile);
        }

        final String[] nnFilePaths = C2rccMerisOperator.getNNFilePaths(testDir);
        assertNotNull(nnFilePaths);
        assertEquals(10, nnFilePaths.length);
        for (int i = 0; i < alternativeNetDirNames.length; i++) {
            String alternativeNetDirName = alternativeNetDirNames[i];
            assertTrue(nnFilePaths[i].contains(sep + alternativeNetDirName + sep));
        }
    }
}