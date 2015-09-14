package org.esa.s3tbx.c2rcc.anc;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.junit.*;

import java.io.File;
import java.io.IOException;

/**
 * Created by Sabine on 07.09.2015.
 */
public class AncRepositoryTest_ValidFileNamePattern {

    private AncRepository ancRepository;
    private String validFilename;

    @Before
    public void setUp() throws Exception {
        ancRepository = new AncRepository(new File("asd"));
        validFilename = "N200136524_lsmf";
    }

    @Test
    public void testValidFilename() throws IOException {
        try {
            ancRepository.getProduct(new String[]{validFilename});
        } catch (IllegalArgumentException notExpected) {
            fail("IllegalArgumentException not expected.");
        }
    }

    @Test
    public void testCharacter_0_invalid_S() throws Exception {
        final String invalidFilename = replaceCharacter(0, 'S');
        try {
            ancRepository.getProduct(new String[] {invalidFilename});
            fail("IllegalArgumentException expected.");
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(), containsString("is not a valid ancillary filename"));
        }
    }

    @Test
    public void testCharacter_1_invalid_0() throws Exception {
        final String invalidFilename = replaceCharacter(1, '0');
        try {
            ancRepository.getProduct(new String[] {invalidFilename});
            fail("IllegalArgumentException expected.");
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(), containsString("is not a valid ancillary filename"));
        }
    }

    @Test
    public void testCharacter_1_invalid_3() throws Exception {
        final String invalidFilename = replaceCharacter(1, '3');
        try {
            ancRepository.getProduct(new String[] {invalidFilename});
            fail("IllegalArgumentException expected.");
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(), containsString("is not a valid ancillary filename"));
        }
    }

    @Test
    public void testCharacter_2_invalid_1() throws Exception {
        final String invalidFilename = replaceCharacter(2, '1');
        try {
            ancRepository.getProduct(new String[] {invalidFilename});
            fail("IllegalArgumentException expected.");
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(), containsString("is not a valid ancillary filename"));
        }
    }

    @Test
    public void testCharacter_2_invalid_8() throws Exception {
        final String invalidFilename = replaceCharacter(2, '8');
        try {
            ancRepository.getProduct(new String[] {invalidFilename});
            fail("IllegalArgumentException expected.");
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(), containsString("is not a valid ancillary filename"));
        }
    }

    @Test
    public void testCharacter_3_invalid_x() throws Exception {
        final String invalidFilename = replaceCharacter(3, 'x');
        try {
            ancRepository.getProduct(new String[] {invalidFilename});
            fail("IllegalArgumentException expected.");
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(), containsString("is not a valid ancillary filename"));
        }
    }

    @Test
    public void testCharacter_4_invalid_x() throws Exception {
        final String invalidFilename = replaceCharacter(4, 'x');
        try {
            ancRepository.getProduct(new String[] {invalidFilename});
            fail("IllegalArgumentException expected.");
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(), containsString("is not a valid ancillary filename"));
        }
    }

    @Test
    public void testCharacter_5_invalid_4() throws Exception {
        final String invalidFilename = replaceCharacter(5, '4');
        try {
            ancRepository.getProduct(new String[] {invalidFilename});
            fail("IllegalArgumentException expected.");
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(), containsString("is not a valid ancillary filename"));
        }
    }

    @Test
    public void testCharacter_5_invalid_9() throws Exception {
        final String invalidFilename = replaceCharacter(5, '9');
        try {
            ancRepository.getProduct(new String[] {invalidFilename});
            fail("IllegalArgumentException expected.");
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(), containsString("is not a valid ancillary filename"));
        }
    }

    @Test
    public void testCharacter_6_invalid_x() throws Exception {
        final String invalidFilename = replaceCharacter(6, 'x');
        try {
            ancRepository.getProduct(new String[] {invalidFilename});
            fail("IllegalArgumentException expected.");
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(), containsString("is not a valid ancillary filename"));
        }
    }

    @Test
    public void testCharacter_7_invalid_x() throws Exception {
        final String invalidFilename = replaceCharacter(7, 'x');
        try {
            ancRepository.getProduct(new String[] {invalidFilename});
            fail("IllegalArgumentException expected.");
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(), containsString("is not a valid ancillary filename"));
        }
    }

    @Test
    public void testCharacter_8_invalid_3() throws Exception {
        final String invalidFilename = replaceCharacter(8, '3');
        try {
            ancRepository.getProduct(new String[] {invalidFilename});
            fail("IllegalArgumentException expected.");
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(), containsString("is not a valid ancillary filename"));
        }
    }

    @Test
    public void testCharacter_9_invalid_x() throws Exception {
        final String invalidFilename = replaceCharacter(9, 'x');
        try {
            ancRepository.getProduct(new String[] {invalidFilename});
            fail("IllegalArgumentException expected.");
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(), containsString("is not a valid ancillary filename"));
        }
    }

    @Test
    public void testCharacter_10_invalid_x() throws Exception {
        final String invalidFilename = replaceCharacter(10, 'x');
        try {
            ancRepository.getProduct(new String[]{invalidFilename});
            fail("IllegalArgumentException expected.");
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(), containsString("is not a valid ancillary filename"));
        }
    }

    private String replaceCharacter(int pos, char character) {
        final char[] chars = validFilename.toCharArray();
        chars[pos] = character;
        return new String(chars);
    }
}