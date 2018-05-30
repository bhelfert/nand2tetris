package com.sevenlist.nand2tetris.compiler;

import org.junit.Ignore;
import org.junit.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.*;

public class JackCompilerAcceptanceTest {

    private static final String SRC_TEST_RESOURCES_PATH = "src/test/resources/";

    @Test
    public void compileSeven() {
        compareVmFileFor("Seven/Main.jack");
    }

    @Ignore
    @Test
    public void compileConvertToBin() {
        compareVmFileFor("ConvertToBin/Main.jack");
    }

    @Ignore
    @Test
    public void compileSquare() {
        compareVmFileFor("Square/Main.jack");
        compareVmFileFor("Square/Square.jack");
        compareVmFileFor("Square/SquareGame.jack");
    }

    @Ignore
    @Test
    public void compileAverage() {
        compareVmFileFor("Average/Main.jack");
    }

    @Ignore
    @Test
    public void compilePong() {
        compareVmFileFor("Pong/Ball.jack");
        compareVmFileFor("Pong/Bat.jack");
        compareVmFileFor("Pong/Main.jack");
        compareVmFileFor("Pong/PongGame.jack");
    }

    @Ignore
    @Test
    public void compileComplexArrays() {
        compareVmFileFor("ComplexArrays/Main.jack");
    }

    private void compareVmFileFor(String relativePathToSourceFile) {
        compile(relativePathToSourceFile);

        File referenceVmFile = new File(SRC_TEST_RESOURCES_PATH + relativePathToSourceFile.replace(".jack", ".reference.vm"));
        File generatedVmFile = new File(SRC_TEST_RESOURCES_PATH + relativePathToSourceFile.replace(".jack", ".vm"));
        assertThat(contentOf(generatedVmFile)).isEqualTo(contentOf(referenceVmFile));
    }

    private void compile(String relativePathToSourceFile) {
        new JackCompiler(new String[] { SRC_TEST_RESOURCES_PATH + relativePathToSourceFile }).compile();
    }
}