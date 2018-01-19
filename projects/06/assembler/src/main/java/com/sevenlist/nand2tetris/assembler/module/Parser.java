package com.sevenlist.nand2tetris.assembler.module;

import java.io.*;

import static com.sevenlist.nand2tetris.assembler.module.CommandType.*;

public class Parser {

    private static final String EQUAL_SIGN = "=";
    private static final String SEMICOLON = ";";

    private final BufferedReader asmCommandReader;
    private String currentCommand;
    private String nextCommand;

    public Parser(File asmFile) {
        try {
            asmCommandReader = new BufferedReader(new FileReader(asmFile));
        }
        catch (FileNotFoundException e) {
            throw new IllegalArgumentException("File [" + asmFile + "] not found", e);
        }
    }

    public boolean hasMoreCommands() {
        try {
            nextCommand = asmCommandReader.readLine();
        }
        catch (IOException e) {
            closeAsmCommandReader();
            throw new RuntimeException("Could not read next command", e);
        }
        if (nextCommand == null) {
            closeAsmCommandReader();
        }
        return nextCommand != null;
    }

    public void advance() {
        currentCommand = nextCommand;
    }

    public CommandType commandType() {
        currentCommand = currentCommand.trim();
        if (currentCommand.startsWith("/")) {
            return NO_COMMAND;
        }
        if (currentCommand.startsWith("@")) {
            return A_COMMAND;
        }
        if (currentCommand.contains(EQUAL_SIGN) || currentCommand.contains(SEMICOLON)) {
            return C_COMMAND;
        }
        if (currentCommand.startsWith("(")) {
            return L_COMMAND;
        }
        return NO_COMMAND;
    }

    public String symbol() {
        if (commandType().equals(NO_COMMAND)) {
            return "";
        }
        if (commandType().equals(L_COMMAND)) {
            return currentCommand.substring(1, currentCommand.indexOf(')'));
        }
        return currentCommand.substring(1).split("/")[0].trim();
    }

    public String dest() {
        if (!currentCommand.contains(EQUAL_SIGN)) {
            return null;
        }
        return currentCommand.split(EQUAL_SIGN)[0].trim();
    }

    public String comp() {
        if (currentCommand.contains(SEMICOLON)) {
            return currentCommand.split(SEMICOLON)[0].trim();
        }
        return currentCommand.split(EQUAL_SIGN)[1].split("/")[0].trim();
    }

    public String jump() {
        if (!currentCommand.contains(SEMICOLON)) {
            return null;
        }
        return currentCommand.split(SEMICOLON)[1].split("/")[0].trim();
    }

    private void closeAsmCommandReader() {
        try {
            asmCommandReader.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
