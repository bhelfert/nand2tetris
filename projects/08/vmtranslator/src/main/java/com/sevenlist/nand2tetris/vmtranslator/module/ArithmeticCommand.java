package com.sevenlist.nand2tetris.vmtranslator.module;

import java.util.Map;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;

public enum ArithmeticCommand {

    ADD("+"),
    SUB("-"),
    NEG("-"),
    EQ,
    GT,
    LT,
    AND("&"),
    OR("|"),
    NOT("!");

    private static final Map<String, ArithmeticCommand> stringToEnum = Stream.of(values()).collect(toMap(Object::toString, e -> e));

    private final String operator;

    public static ArithmeticCommand fromString(String segment) {
        return stringToEnum.get(segment);
    }

    ArithmeticCommand() {
        this(null);
    }

    ArithmeticCommand(String operator) {
        this.operator = operator;
    }

    public String operator() {
        return operator;
    }

    @Override
    public String toString() {
        return name().toLowerCase();
    }

}
