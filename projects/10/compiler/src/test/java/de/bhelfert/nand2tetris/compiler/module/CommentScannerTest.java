package de.bhelfert.nand2tetris.compiler.module;

import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

public class CommentScannerTest {

    private CommentScanner commentScanner = new CommentScanner();

    @Test
    public void someTextIsNoCommentAndNoWhitespace() {
        assertThat(commentScanner.isCommentOrWhitespace("some text")).isFalse();
    }

    @Test
    public void textIsCommentToEndOfLine() {
        assertThat(commentScanner.isCommentOrWhitespace("// a comment")).isTrue();
        assertThat(commentScanner.isCommentOrWhitespace("   // a comment ")).isTrue();
        assertThat(commentScanner.isCommentOrWhitespace("// a comment // more comments")).isTrue();
    }

    @Test
    public void noTextIsEmptyLine() {
        assertThat(commentScanner.isCommentOrWhitespace("")).isTrue();
    }

    @Test
    public void textWithWhitespaceIsEmptyLine() {
        assertThat(commentScanner.isCommentOrWhitespace(" \t")).isTrue();
    }

    @Test
    public void textIsCommentUntilClosing() {
        assertThat(commentScanner.isCommentOrWhitespace("/* a comment */")).isTrue();
        assertThat(commentScanner.isCommentOrWhitespace(" /*comment */")).isTrue();
        assertThat(commentScanner.isCommentOrWhitespace("/* comment*/")).isTrue();
        assertThat(commentScanner.isCommentOrWhitespace("/*comment*/ ")).isTrue();

        assertThat(commentScanner.isCommentOrWhitespace("/*")).isTrue();
        assertThat(commentScanner.isCommentOrWhitespace("comment")).isTrue();
        assertThat(commentScanner.isCommentOrWhitespace("*/")).isTrue();
        assertThat(commentScanner.isCommentOrWhitespace("some text")).isFalse();

        assertThat(commentScanner.isCommentOrWhitespace("/* comment")).isTrue();
        assertThat(commentScanner.isCommentOrWhitespace("*/")).isTrue();
        assertThat(commentScanner.isCommentOrWhitespace("some text")).isFalse();

        assertThat(commentScanner.isCommentOrWhitespace("/* comment")).isTrue();
        assertThat(commentScanner.isCommentOrWhitespace("// more comments")).isTrue();
        assertThat(commentScanner.isCommentOrWhitespace("  and more comments *")).isTrue();
        assertThat(commentScanner.isCommentOrWhitespace("* and even more comment")).isTrue();
        assertThat(commentScanner.isCommentOrWhitespace("*/")).isTrue();
        assertThat(commentScanner.isCommentOrWhitespace("some text")).isFalse();

        assertThat(commentScanner.isCommentOrWhitespace(" /* ")).isTrue();
        assertThat(commentScanner.isCommentOrWhitespace(" comment */ ")).isTrue();
        assertThat(commentScanner.isCommentOrWhitespace("some text")).isFalse();
    }

    @Test
    public void textIsApiDocumentationComment() {
        assertThat(commentScanner.isCommentOrWhitespace("/** An API comment */")).isTrue();
    }

    @Test(expected = RuntimeException.class)
    public void textWithoutStartCommentIsInvalidComment() {
        commentScanner.isCommentOrWhitespace("invalid comment */");
    }

    @Test
    public void someTextIsNotStripped() {
        assertThat(commentScanner.stripComments("some text")).isEqualTo("some text");
    }

    @Test
    public void commentToEndOfLineIsStripped() {
        assertThat(commentScanner.stripComments("some text // comment")).isEqualTo("some text");
    }

    @Test
    public void commentBeforeTextIsStripped() {
        assertThat(commentScanner.stripComments("/* comment */ some text")).isEqualTo("some text");
    }

    @Test
    public void onlyCommentIsCompletelyStripped() {
        assertThat(commentScanner.stripComments("// only comment")).isEqualTo("");
        assertThat(commentScanner.stripComments("/* only comment */")).isEqualTo("");
    }

    @Test
    public void commentAfterTextIsStripped() {
        assertThat(commentScanner.stripComments("some text /* comment */")).isEqualTo("some text");
    }

    @Test
    public void commentBetweenTextIsStripped() {
        assertThat(commentScanner.stripComments("left text /* comment */ right text")).isEqualTo("left text right text");
    }

    @Test
    public void commentsSurroundingTextAreStripped() {
        assertThat(commentScanner.stripComments("/* left comment */ some text /* right comment */ ")).isEqualTo("some text");
    }

    @Test
    public void apiCommentsAreStripped() {
        assertThat(commentScanner.stripComments("/** Left API comment. */ some text /** Right API comment. */ ")).isEqualTo("some text");
    }

    @Test
    public void commentsOfDifferentTypesAreStripped() {
        assertThat(commentScanner.stripComments("/* left comment */ text1 /** API comment. */ text2 // right comment")).isEqualTo("text1 text2");
    }

    @Test(expected = RuntimeException.class)
    public void textWithoutStartCommentIsInvalidComment2() {
        commentScanner.stripComments("invalid comment */ some text");
    }
}
