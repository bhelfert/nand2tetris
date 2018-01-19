package com.sevenlist.nand2tetris.assembler.module;

import java.util.HashMap;
import java.util.Map;

public class SymbolTable {

    private static final Map<String, Integer> SYMBOL_TABLE = new HashMap<>();
    static {
        SYMBOL_TABLE.put("SP", 0);
        SYMBOL_TABLE.put("LCL", 1);
        SYMBOL_TABLE.put("ARG", 2);
        SYMBOL_TABLE.put("THIS", 3);
        SYMBOL_TABLE.put("THAT", 4);
        SYMBOL_TABLE.put("R0", 0);
        SYMBOL_TABLE.put("R1", 1);
        SYMBOL_TABLE.put("R2", 2);
        SYMBOL_TABLE.put("R3", 3);
        SYMBOL_TABLE.put("R4", 4);
        SYMBOL_TABLE.put("R5", 5);
        SYMBOL_TABLE.put("R6", 6);
        SYMBOL_TABLE.put("R7", 7);
        SYMBOL_TABLE.put("R8", 8);
        SYMBOL_TABLE.put("R9", 9);
        SYMBOL_TABLE.put("R10", 10);
        SYMBOL_TABLE.put("R11", 11);
        SYMBOL_TABLE.put("R12", 12);
        SYMBOL_TABLE.put("R13", 13);
        SYMBOL_TABLE.put("R14", 14);
        SYMBOL_TABLE.put("R15", 15);
        SYMBOL_TABLE.put("SCREEN", 16384);
        SYMBOL_TABLE.put("KBD", 24576);
    }

    public void addEntry(String symbol, int address) {
        SYMBOL_TABLE.put(symbol, address);
    }

    public boolean contains(String symbol) {
        return SYMBOL_TABLE.containsKey(symbol);
    }

    public int getAddress(String symbol) {
        return SYMBOL_TABLE.get(symbol);
    }

    @Override
    public String toString() {
        return SYMBOL_TABLE.toString();
    }
}
