package com.sevenlist.nand2tetris.vmtranslator.module;

import java.util.Map;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;

public enum Segment {

    CONSTANT("SP"),
    LOCAL("LCL"),
    ARGUMENT("ARG"),
    POINTER("R3"),
    THIS("THIS"),
    THAT("THAT"),
    TEMP("R5"),
    STATIC(null);

    private static final Map<String, Segment> stringToEnum = Stream.of(values()).collect(toMap(Object::toString, e -> e));

    private final String baseAddress;

    Segment(String baseAddress) {
        this.baseAddress = baseAddress;
    }

    public static Segment fromString(String segment) {
        return stringToEnum.get(segment);
    }

    public String baseAddress() {
        return baseAddress;
    }

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
