package de.bhelfert.nand2tetris.compiler.module;

import java.util.Map;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;

public enum UnaryOperator implements Operator {

    NEG(Symbol.MINUS),
    NOT(Symbol.TILDE);

    private static final Map<Symbol, UnaryOperator> symbolToEnum = Stream.of(values()).collect(toMap(e -> e.symbol, e -> e));

    private final Symbol symbol;

    UnaryOperator(Symbol symbol) {
        this.symbol = symbol;
    }

    static UnaryOperator fromSymbol(Symbol symbol) {
        return symbolToEnum.get(symbol);
    }

    static boolean isOperator(Symbol symbol) {
        return symbolToEnum.containsKey(symbol);
    }

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
