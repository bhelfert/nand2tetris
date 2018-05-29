package com.sevenlist.nand2tetris.assembler;

import org.junit.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.contentOf;

public class AssemblerAcceptanceTest {

    private static final String SRC_TEST_RESOURCES_PATH = "src/test/resources/";

    @Test
    public void assembleAdd() {
        compareHackFilesOf("add/Add.asm");
    }

    @Test
    public void assembleMax() {
        compareHackFilesOf("max/Max.asm");
    }

    @Test
    public void assembleMaxL() {
        compareHackFilesOf("max/MaxL.asm");
    }

    @Test
    public void assemblePong() {
        compareHackFilesOf("pong/Pong.asm");
    }

    @Test
    public void assemblePongL() {
        compareHackFilesOf("pong/PongL.asm");
    }

    @Test
    public void assembleRect() {
        compareHackFilesOf("rect/Rect.asm");
    }

    @Test
    public void assembleRectL() {
        compareHackFilesOf("rect/RectL.asm");
    }

    private void compareHackFilesOf(String relativePathToSourceFile) {
        assemble(relativePathToSourceFile);
        File referenceHackFile = new File(SRC_TEST_RESOURCES_PATH + relativePathToSourceFile.replace(".asm", ".reference.hack"));
        File assembledHackFile = new File(SRC_TEST_RESOURCES_PATH + relativePathToSourceFile.replace(".asm", ".hack"));
        assertThat(contentOf(assembledHackFile)).isEqualTo(contentOf(referenceHackFile));
    }

    private void assemble(String relativePathToSourceFile) {
        new Assembler(new String[] { SRC_TEST_RESOURCES_PATH + relativePathToSourceFile }).assemble();
    }
}