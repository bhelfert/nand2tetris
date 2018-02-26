package com.sevenlist.nand2tetris.compiler.module;

public enum TokenType {

    KEYWORD,
    SYMBOL,
    IDENTIFIER,
    INT_CONST("integerConstant"),
    STRING_CONST("stringConstant"),
    COMMENT_OR_EMPTY_LINE;

    private final String alternativeXmlTag;

    TokenType() {
        this(null);
    }

    TokenType(String alternativeXmlTag) {
        this.alternativeXmlTag = alternativeXmlTag;
    }

    @Override
    public String toString() {
        return (alternativeXmlTag == null) ? name().toLowerCase() : alternativeXmlTag;
    }
}
