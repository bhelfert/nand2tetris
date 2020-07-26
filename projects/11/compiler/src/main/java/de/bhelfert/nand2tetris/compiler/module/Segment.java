package de.bhelfert.nand2tetris.compiler.module;

import java.util.Map;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;

public enum Segment {

    CONSTANT, // R0
    LOCAL,    // R1
    ARGUMENT, // R2
    POINTER,  // R3
    THIS,     // R3
    THAT,     // R4
    TEMP,     // R5
    STATIC;

    private static final Map<String, Segment> stringToEnum = Stream.of(values()).collect(toMap(Object::toString, e -> e));

    public static Segment fromString(String segment) {
        return stringToEnum.get(segment);
    }

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
