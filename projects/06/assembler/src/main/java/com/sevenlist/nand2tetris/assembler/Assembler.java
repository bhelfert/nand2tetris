package com.sevenlist.nand2tetris.assembler;

import com.sevenlist.nand2tetris.assembler.module.Code;
import com.sevenlist.nand2tetris.assembler.module.Parser;
import com.sevenlist.nand2tetris.assembler.module.SymbolTable;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static com.sevenlist.nand2tetris.assembler.module.CommandType.L_COMMAND;
import static com.sevenlist.nand2tetris.assembler.module.CommandType.NO_COMMAND;

public class Assembler {

    private static final String NOTHING = "";
    private static final String ADDRESS_INSTRUCTION_PREFIX = "0";
    private static final String COMPUTE_INSTRUCTION_PREFIX = "111";
    private static final int VARIABLES_START_ADDRESS = 16;

    private final File asmFile;
    private int variables;

    public static void main(String[] args) {
        new Assembler(args).assemble();
    }

    public Assembler(String[] args) {
        asmFile = new File(args[0]);
    }

    public void assemble() {
        SymbolTable symbolTable = parseLabels();
        String machineCodes = parseInstructionsAndTranslateToMachineCodes(symbolTable);
        writeHackFile(machineCodes);
    }

    private SymbolTable parseLabels() {
        SymbolTable symbolTable = new SymbolTable();
        Parser parser = new Parser(asmFile);
        int commandCounter = -1;
        while (parser.hasMoreCommands()) {
            parser.advance();
            if (parser.commandType().equals(L_COMMAND)) {
                symbolTable.addEntry(parser.symbol(), commandCounter + 1);
            }
            else if (!parser.commandType().equals(NO_COMMAND)) {
                ++commandCounter;
            }
        }
        return symbolTable;
    }

    private String parseInstructionsAndTranslateToMachineCodes(SymbolTable symbolTable) {
        StringBuilder machineCodes = new StringBuilder();
        Parser parser = new Parser(asmFile);
        variables = 0;
        while (parser.hasMoreCommands()) {
            parser.advance();
            switch (parser.commandType()) {
                case A_COMMAND:
                    machineCodes.append(translateAddressInstruction(parser, symbolTable));
                    break;

                case C_COMMAND:
                    machineCodes.append(translateComputeInstruction(parser));
                    break;
            }
        }
        return machineCodes.toString();
    }

    private String translateAddressInstruction(Parser parser, SymbolTable symbolTable) {
        String symbol = parser.symbol();
        StringBuilder machineCode = new StringBuilder();
        machineCode.append(isAddress(symbol) ? machineCodeForAddress(symbol) : NOTHING);
        machineCode.append(isLabelOrKnownVariable(symbol, symbolTable) ? machineCodeForLabelOrKnownVariable(symbol, symbolTable) : NOTHING);
        machineCode.append(isNewVariable(symbol, symbolTable) ? machineCodeForNewVariable(symbol, symbolTable) : NOTHING);
        machineCode.append(System.lineSeparator());
        return machineCode.toString();
    }

    private boolean isAddress(String symbol) {
        return isNumeric(symbol);
    }

    private boolean isNumeric(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private String machineCodeForAddress(String symbol) {
        return addressMachineCode(Integer.valueOf(symbol));
    }

    private boolean isLabelOrKnownVariable(String symbol, SymbolTable symbolTable) {
        return symbolTable.contains(symbol);
    }

    private String machineCodeForLabelOrKnownVariable(String symbol, SymbolTable symbolTable) {
        return addressMachineCode(symbolTable.getAddress(symbol));
    }

    private boolean isNewVariable(String symbol, SymbolTable symbolTable) {
        return !isAddress(symbol) && !symbolTable.contains(symbol);
    }

    private String machineCodeForNewVariable(String symbol, SymbolTable symbolTable) {
        int decimalVariableAddress = VARIABLES_START_ADDRESS + variables++;
        symbolTable.addEntry(symbol, decimalVariableAddress);
        return addressMachineCode(decimalVariableAddress);
    }

    private String addressMachineCode(int decimalValue) {
        String binaryValue = Integer.toBinaryString(decimalValue);
        return ADDRESS_INSTRUCTION_PREFIX + String.format("%15s", binaryValue).replace(' ', '0');
    }

    private String translateComputeInstruction(Parser parser) {
        Code code = new Code();
        StringBuilder machineCode = new StringBuilder(COMPUTE_INSTRUCTION_PREFIX);
        machineCode.append(addressOrMemoryMachineCode(parser));
        machineCode.append(compMachineCode(parser, code));
        machineCode.append(destMachineCode(parser, code));
        machineCode.append(jumpMachineCode(parser, code));
        machineCode.append(System.lineSeparator());
        return machineCode.toString();
    }

    private String addressOrMemoryMachineCode(Parser parser) {
        String comp = parser.comp();
        return ((comp != null) && comp.contains("M")) ? "1" : "0";
    }

    private String compMachineCode(Parser parser, Code code) {
        return code.comp(parser.comp());
    }

    private String destMachineCode(Parser parser, Code code) {
        return (parser.dest() != null) ? code.dest(parser.dest()) : "000";
    }

    private String jumpMachineCode(Parser parser, Code code) {
        return (parser.jump() != null) ? code.jump(parser.jump()) : "000";
    }

    private void writeHackFile(String machineCodes) {
        String hackFilename = asmFile.getPath().replace(".asm", ".hack");
        try (BufferedWriter hackFileWriter = new BufferedWriter(new FileWriter(new File(hackFilename)))) {
            hackFileWriter.write(machineCodes);
        }
        catch (IOException e) {
            throw new RuntimeException("Could not write .hack file [" + hackFilename + "]", e);
        }
        System.out.println("Assembled " + hackFilename);
    }
}
