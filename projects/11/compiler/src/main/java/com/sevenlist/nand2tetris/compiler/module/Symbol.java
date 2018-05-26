package com.sevenlist.nand2tetris.compiler.module;

import java.util.Map;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;

public enum Symbol {

    LEFT_CURLY_BRACE("{"),
    RIGHT_CURLY_BRACE("}"),
    LEFT_PARENTHESIS("("),
    RIGHT_PARENTHESIS(")"),
    LEFT_SQUARE_BRACKET("["),
    RIGHT_SQUARE_BRACKET("]"),
    DOT("."),
    COMMA(","),
    SEMICOLON(";"),
    PLUS("+"),
    MINUS("-"),
    ASTERISK("*"),
    SLASH("/"),
    AMPERSAND("&", "&amp;"),
    PIPE("|"),
    LESS_THAN("<", "&lt;"),
    GREATER_THAN(">", "&gt;"),
    EQUAL_SIGN("="),
    TILDE("~");

    private static final Map<String, Symbol> stringToEnum = Stream.of(values()).collect(toMap(Object::toString, e -> e));

    private final String character;
    private final String xmlEscapeString;

    public static Symbol fromString(String character) {
        return stringToEnum.get(character);
    }

    Symbol(String character) {
        this(character, null);
    }

    Symbol(String character, String xmlEscapeString) {
        this.character = character;
        this.xmlEscapeString = xmlEscapeString;
    }

    @Override
    public String toString() {
        return character;
    }

    public String toEscapeString() {
        return (xmlEscapeString == null) ? character : xmlEscapeString;
    }
}