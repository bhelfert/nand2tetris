package de.bhelfert.nand2tetris.compiler.module;

import org.junit.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.*;

public class JackTokenizerTest {

    private static final String SRC_TEST_RESOURCES_PATH = "src/test/resources/";

    private JackTokenizer tokenizer;

    @Test
    public void emptyFileHasNoTokens() {
        tokenizer = createJackTokenizer("EmptyFile.jack");
        assertThat(tokenizer.hasMoreTokens()).isFalse();
    }

    @Test
    public void fileWithEmptyLineHasNoTokens() {
        tokenizer = createJackTokenizer("FileWithEmptyLine.jack");
        assertThat(tokenizer.hasMoreTokens()).isFalse();
    }

    @Test
    public void fileWithTokenHasTokens() {
        tokenizer = createJackTokenizer("FileWithToken.jack");
        assertThat(tokenizer.hasMoreTokens()).isTrue();
        // token has not be consumed = advance() has not been called
        assertThat(tokenizer.hasMoreTokens()).isTrue();
    }

    private JackTokenizer createJackTokenizer(String testFileName) {
        return new JackTokenizer(new File(SRC_TEST_RESOURCES_PATH + testFileName));
    }
}
