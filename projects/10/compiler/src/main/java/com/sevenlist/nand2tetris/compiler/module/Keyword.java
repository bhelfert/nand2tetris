package com.sevenlist.nand2tetris.compiler.module;

import java.util.Map;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;

public enum Keyword {

    CLASS,
    METHOD,
    FUNCTION,
    CONSTRUCTOR,
    INT,
    BOOLEAN,
    CHAR,
    VOID,
    VAR,
    STATIC,
    FIELD,
    LET,
    DO,
    IF,
    ELSE,
    WHILE,
    RETURN,
    TRUE,
    FALSE,
    NULL,
    THIS;

    private static final Map<String, Keyword> stringToEnum = Stream.of(values()).collect(toMap(Object::toString, e -> e));

    public static Keyword fromString(String keyword) {
        return stringToEnum.get(keyword);
    }

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
