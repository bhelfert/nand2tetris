package de.bhelfert.nand2tetris.compiler;

import org.junit.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.*;

public class JackAnalyzerAcceptanceTest {

    private static final String SRC_TEST_RESOURCES_PATH = "src/test/resources/";

    @Test
    public void analyzeArray() {
        compareXmlFilesFor("ArrayTest/Main.jack");
    }

    @Test
    public void analyzeSquare() {
        compareXmlFilesFor("Square/Main.jack");
        compareXmlFilesFor("Square/Square.jack");
        compareXmlFilesFor("Square/SquareGame.jack");
    }

    // Use AssertJ instead of tool TextCompare.sh
    private void compareXmlFilesFor(String relativePathToSourceFile) {
        analyze(relativePathToSourceFile);

        File referenceTokenizerFile = new File(SRC_TEST_RESOURCES_PATH + relativePathToSourceFile.replace(".jack", "T.reference.xml"));
        File generatedTokenizerFile = new File(SRC_TEST_RESOURCES_PATH + relativePathToSourceFile.replace(".jack", "T.xml"));
        assertThat(contentOf(generatedTokenizerFile)).isEqualTo(contentOf(referenceTokenizerFile));

        File referenceParserFile = new File(SRC_TEST_RESOURCES_PATH + relativePathToSourceFile.replace(".jack", ".reference.xml"));
        File generatedParserFile = new File(SRC_TEST_RESOURCES_PATH + relativePathToSourceFile.replace(".jack", ".xml"));
        assertThat(contentOf(generatedParserFile)).isEqualTo(contentOf(referenceParserFile));
    }

    private void analyze(String relativePathToSourceFile) {
        new JackAnalyzer(new String[] { SRC_TEST_RESOURCES_PATH + relativePathToSourceFile }).analyze();
    }
}
