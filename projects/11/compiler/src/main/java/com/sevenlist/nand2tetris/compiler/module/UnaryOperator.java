package com.sevenlist.nand2tetris.compiler.module;

import java.util.Map;
import java.util.stream.Stream;

import static com.sevenlist.nand2tetris.compiler.module.Symbol.MINUS;
import static com.sevenlist.nand2tetris.compiler.module.Symbol.TILDE;
import static java.util.stream.Collectors.toMap;

public enum UnaryOperator implements Operator {

    NEG(MINUS),
    NOT(TILDE);

    private static final Map<Symbol, UnaryOperator> symbolToEnum = Stream.of(values()).collect(toMap(e -> e.symbol, e -> e));

    private final Symbol symbol;

    UnaryOperator(Symbol symbol) {
        this.symbol = symbol;
    }

    static UnaryOperator fromSymbol(Symbol symbol) {
        return UnaryOperator.symbolToEnum.get(symbol);
    }

    static boolean isOperator(Symbol symbol) {
        return UnaryOperator.symbolToEnum.containsKey(symbol);
    }

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}