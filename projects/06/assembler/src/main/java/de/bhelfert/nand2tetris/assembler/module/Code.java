package de.bhelfert.nand2tetris.assembler.module;

import java.util.HashMap;
import java.util.Map;

public class Code {

    private static final Map<String, String> DEST_MNEMONIC_TO_BINARY_STRING = new HashMap<>();
    static {
        DEST_MNEMONIC_TO_BINARY_STRING.put("M", "001");
        DEST_MNEMONIC_TO_BINARY_STRING.put("D", "010");
        DEST_MNEMONIC_TO_BINARY_STRING.put("MD", "011");
        DEST_MNEMONIC_TO_BINARY_STRING.put("DM", "011");
        DEST_MNEMONIC_TO_BINARY_STRING.put("A", "100");
        DEST_MNEMONIC_TO_BINARY_STRING.put("AM", "101");
        DEST_MNEMONIC_TO_BINARY_STRING.put("MA", "101");
        DEST_MNEMONIC_TO_BINARY_STRING.put("AD", "110");
        DEST_MNEMONIC_TO_BINARY_STRING.put("DA", "110");
        DEST_MNEMONIC_TO_BINARY_STRING.put("AMD", "111");
        DEST_MNEMONIC_TO_BINARY_STRING.put("ADM", "111");
        DEST_MNEMONIC_TO_BINARY_STRING.put("DAM", "111");
        DEST_MNEMONIC_TO_BINARY_STRING.put("DMA", "111");
        DEST_MNEMONIC_TO_BINARY_STRING.put("MAD", "111");
        DEST_MNEMONIC_TO_BINARY_STRING.put("MDA", "111");
    }

    private static final Map<String, String> COMP_MNEMONIC_TO_BINARY_STRING = new HashMap<>();
    static {
        COMP_MNEMONIC_TO_BINARY_STRING.put("0", "101010");
        COMP_MNEMONIC_TO_BINARY_STRING.put("1", "111111");
        COMP_MNEMONIC_TO_BINARY_STRING.put("-1", "111010");
        COMP_MNEMONIC_TO_BINARY_STRING.put("D", "001100");
        COMP_MNEMONIC_TO_BINARY_STRING.put("A", "110000");
        COMP_MNEMONIC_TO_BINARY_STRING.put("M", "110000");
        COMP_MNEMONIC_TO_BINARY_STRING.put("!D", "001101");
        COMP_MNEMONIC_TO_BINARY_STRING.put("!A", "110001");
        COMP_MNEMONIC_TO_BINARY_STRING.put("!M", "110001");
        COMP_MNEMONIC_TO_BINARY_STRING.put("-D", "001111");
        COMP_MNEMONIC_TO_BINARY_STRING.put("-A", "110011");
        COMP_MNEMONIC_TO_BINARY_STRING.put("-M", "110011");
        COMP_MNEMONIC_TO_BINARY_STRING.put("D+1", "011111");
        COMP_MNEMONIC_TO_BINARY_STRING.put("1+D", "011111");
        COMP_MNEMONIC_TO_BINARY_STRING.put("A+1", "110111");
        COMP_MNEMONIC_TO_BINARY_STRING.put("1+A", "110111");
        COMP_MNEMONIC_TO_BINARY_STRING.put("M+1", "110111");
        COMP_MNEMONIC_TO_BINARY_STRING.put("1+M", "110111");
        COMP_MNEMONIC_TO_BINARY_STRING.put("D-1", "001110");
        COMP_MNEMONIC_TO_BINARY_STRING.put("A-1", "110010");
        COMP_MNEMONIC_TO_BINARY_STRING.put("M-1", "110010");
        COMP_MNEMONIC_TO_BINARY_STRING.put("D+A", "000010");
        COMP_MNEMONIC_TO_BINARY_STRING.put("A+D", "000010");
        COMP_MNEMONIC_TO_BINARY_STRING.put("D+M", "000010");
        COMP_MNEMONIC_TO_BINARY_STRING.put("M+D", "000010");
        COMP_MNEMONIC_TO_BINARY_STRING.put("D-A", "010011");
        COMP_MNEMONIC_TO_BINARY_STRING.put("D-M", "010011");
        COMP_MNEMONIC_TO_BINARY_STRING.put("A-D", "000111");
        COMP_MNEMONIC_TO_BINARY_STRING.put("M-D", "000111");
        COMP_MNEMONIC_TO_BINARY_STRING.put("D&A", "000000");
        COMP_MNEMONIC_TO_BINARY_STRING.put("A&D", "000000");
        COMP_MNEMONIC_TO_BINARY_STRING.put("D&M", "000000");
        COMP_MNEMONIC_TO_BINARY_STRING.put("M&D", "000000");
        COMP_MNEMONIC_TO_BINARY_STRING.put("D|A", "010101");
        COMP_MNEMONIC_TO_BINARY_STRING.put("A|D", "010101");
        COMP_MNEMONIC_TO_BINARY_STRING.put("D|M", "010101");
        COMP_MNEMONIC_TO_BINARY_STRING.put("M|D", "010101");
    }

    private static final Map<String, String> JUMP_MNEMONIC_TO_BINARY_STRING = new HashMap<>();
    static {
        JUMP_MNEMONIC_TO_BINARY_STRING.put("JGT", "001");
        JUMP_MNEMONIC_TO_BINARY_STRING.put("JEQ", "010");
        JUMP_MNEMONIC_TO_BINARY_STRING.put("JGE", "011");
        JUMP_MNEMONIC_TO_BINARY_STRING.put("JLT", "100");
        JUMP_MNEMONIC_TO_BINARY_STRING.put("JNE", "101");
        JUMP_MNEMONIC_TO_BINARY_STRING.put("JLE", "110");
        JUMP_MNEMONIC_TO_BINARY_STRING.put("JMP", "111");
    }

    public String dest(String mnemonic) {
        return DEST_MNEMONIC_TO_BINARY_STRING.get(mnemonic);
    }

    public String comp(String mnemonic) {
        return COMP_MNEMONIC_TO_BINARY_STRING.get(mnemonic);
    }

    public String jump(String mnemonic) {
        return JUMP_MNEMONIC_TO_BINARY_STRING.get(mnemonic);
    }
}
