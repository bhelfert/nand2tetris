package com.sevenlist.nand2tetris.vmtranslator.module;

import java.util.Map;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;

public enum Segment {

    ARGUMENT,
    LOCAL,
    STATIC,
    CONSTANT,
    THIS,
    THAT,
    POINTER,
    TEMP;

    private static final Map<String, Segment> stringToEnum = Stream.of(values()).collect(toMap(Object::toString, e -> e));

    public static Segment fromString(String segment) {
        return stringToEnum.get(segment);
    }

    @Override
    public String toString() {
        return name().toLowerCase();
    }

    public String toStringCapitalized() {
        return Character.toUpperCase(toString().charAt(0)) + toString().substring(1);
    }
}
