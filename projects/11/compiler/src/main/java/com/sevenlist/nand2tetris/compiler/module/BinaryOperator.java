package com.sevenlist.nand2tetris.compiler.module;

import java.util.Map;
import java.util.stream.Stream;

import static com.sevenlist.nand2tetris.compiler.module.Symbol.*;
import static java.util.stream.Collectors.toMap;

public enum BinaryOperator implements Operator {

    ADD(PLUS),
    SUB(MINUS),
    MULTIPLY(ASTERISK, "call Math.multiply 2"),
    DIVIDE(SLASH, "call Math.divide 2"),
    EQ(EQUAL_SIGN),
    LT(LESS_THAN),
    GT(GREATER_THAN),
    AND(AMPERSAND),
    OR(PIPE);

    private static final Map<Symbol, BinaryOperator> symbolToEnum = Stream.of(values()).collect(toMap(e -> e.symbol, e -> e));

    private final Symbol symbol;
    private final String vmCommand;

    BinaryOperator(Symbol symbol) {
        this(symbol, null);
    }

    BinaryOperator(Symbol symbol, String vmCommand) {
        this.symbol = symbol;
        this.vmCommand = vmCommand;
    }

    static BinaryOperator fromSymbol(Symbol symbol) {
        return BinaryOperator.symbolToEnum.get(symbol);
    }

    static boolean isOperator(Symbol symbol) {
        return BinaryOperator.symbolToEnum.containsKey(symbol);
    }

    @Override
    public String toString() {
        return (vmCommand != null) ? vmCommand : name().toLowerCase();
    }
}
