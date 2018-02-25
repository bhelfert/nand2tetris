package com.sevenlist.nand2tetris.compiler.module;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CommentParserTest {

    private CommentParser commentParser = new CommentParser();

    @Test
    public void someTextIsNoCommentAndNoWhitespace() {
        assertThat(commentParser.isCommentOrWhitespace("some text")).isFalse();
    }

    @Test
    public void textIsCommentToEndOfLine() {
        assertThat(commentParser.isCommentOrWhitespace("// a comment")).isTrue();
        assertThat(commentParser.isCommentOrWhitespace("   // a comment ")).isTrue();
        assertThat(commentParser.isCommentOrWhitespace("// a comment // more comments")).isTrue();
    }

    @Test
    public void noTextIsEmptyLine() {
        assertThat(commentParser.isCommentOrWhitespace("")).isTrue();
    }

    @Test
    public void textWithWhitespaceIsEmptyLine() {
        assertThat(commentParser.isCommentOrWhitespace(" \t")).isTrue();
    }

    @Test
    public void textIsCommentUntilClosing() {
        assertThat(commentParser.isCommentOrWhitespace("/* a comment */")).isTrue();
        assertThat(commentParser.isCommentOrWhitespace(" /*comment */")).isTrue();
        assertThat(commentParser.isCommentOrWhitespace("/* comment*/")).isTrue();
        assertThat(commentParser.isCommentOrWhitespace("/*comment*/ ")).isTrue();

        assertThat(commentParser.isCommentOrWhitespace("/*")).isTrue();
        assertThat(commentParser.isCommentOrWhitespace("comment")).isTrue();
        assertThat(commentParser.isCommentOrWhitespace("*/")).isTrue();
        assertThat(commentParser.isCommentOrWhitespace("some text")).isFalse();

        assertThat(commentParser.isCommentOrWhitespace("/* comment")).isTrue();
        assertThat(commentParser.isCommentOrWhitespace("*/")).isTrue();
        assertThat(commentParser.isCommentOrWhitespace("some text")).isFalse();

        assertThat(commentParser.isCommentOrWhitespace("/* comment")).isTrue();
        assertThat(commentParser.isCommentOrWhitespace("// more comments")).isTrue();
        assertThat(commentParser.isCommentOrWhitespace("  and more comments *")).isTrue();
        assertThat(commentParser.isCommentOrWhitespace("* and even more comment")).isTrue();
        assertThat(commentParser.isCommentOrWhitespace("*/")).isTrue();
        assertThat(commentParser.isCommentOrWhitespace("some text")).isFalse();

        assertThat(commentParser.isCommentOrWhitespace(" /* ")).isTrue();
        assertThat(commentParser.isCommentOrWhitespace(" comment */ ")).isTrue();
        assertThat(commentParser.isCommentOrWhitespace("some text")).isFalse();
    }

    @Test
    public void textIsApiDocumentationComment() {
        assertThat(commentParser.isCommentOrWhitespace("/** An API comment */")).isTrue();
    }

    @Test(expected = RuntimeException.class)
    public void textWithoutStartCommentIsInvalidComment() {
        commentParser.isCommentOrWhitespace("invalid comment */");
    }

    @Test
    public void someTextIsNotStripped() {
        assertThat(commentParser.stripComments("some text")).isEqualTo("some text");
    }

    @Test
    public void commentToEndOfLineIsStripped() {
        assertThat(commentParser.stripComments("some text // comment")).isEqualTo("some text");
    }

    @Test
    public void commentBeforeTextIsStripped() {
        assertThat(commentParser.stripComments("/* comment */ some text")).isEqualTo("some text");
    }

    @Test
    public void onlyCommentIsCompletelyStripped() {
        assertThat(commentParser.stripComments("// only comment")).isEqualTo("");
        assertThat(commentParser.stripComments("/* only comment */")).isEqualTo("");
    }

    @Test
    public void commentAfterTextIsStripped() {
        assertThat(commentParser.stripComments("some text /* comment */")).isEqualTo("some text");
    }

    @Test
    public void commentBetweenTextIsStripped() {
        assertThat(commentParser.stripComments("left text /* comment */ right text")).isEqualTo("left text right text");
    }

    @Test
    public void commentsSurroundingTextAreStripped() {
        assertThat(commentParser.stripComments("/* left comment */ some text /* right comment */ ")).isEqualTo("some text");
    }

    @Test
    public void apiCommentsAreStripped() {
        assertThat(commentParser.stripComments("/** Left API comment. */ some text /** Right API comment. */ ")).isEqualTo("some text");
    }

    @Test
    public void commentsOfDifferentTypesAreStripped() {
        assertThat(commentParser.stripComments("/* left comment */ text1 /** API comment. */ text2 // right comment")).isEqualTo("text1 text2");
    }

    @Test(expected = RuntimeException.class)
    public void textWithoutStartCommentIsInvalidComment2() {
        commentParser.stripComments("invalid comment */ some text");
    }
}