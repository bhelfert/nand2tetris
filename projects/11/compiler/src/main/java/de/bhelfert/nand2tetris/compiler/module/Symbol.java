package de.bhelfert.nand2tetris.compiler.module;

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
    AMPERSAND("&"),
    PIPE("|"),
    LESS_THAN("<"),
    GREATER_THAN(">"),
    EQUAL_SIGN("="),
    TILDE("~");

    private static final Map<String, Symbol> stringToEnum = Stream.of(values()).collect(toMap(Object::toString, e -> e));

    private final String character;

    public static Symbol fromString(String character) {
        return stringToEnum.get(character);
    }

    Symbol(String character) {
        this.character = character;
    }

    @Override
    public String toString() {
        return character;
    }
}
