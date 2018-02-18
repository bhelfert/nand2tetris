package com.sevenlist.nand2tetris.vmtranslator.module;

public enum CommandType {

    ARITHMETIC,
    PUSH,
    POP,
    NONE;

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
