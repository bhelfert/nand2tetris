package com.sevenlist.nand2tetris.vmtranslator.module;

import java.io.*;

import static com.sevenlist.nand2tetris.vmtranslator.module.CommandType.*;

public class Parser {

    private final BufferedReader vmCommandReader;
    private String currentCommand;
    private String nextCommand;

    public Parser(File vmFile) {
        try {
            vmCommandReader = new BufferedReader(new FileReader(vmFile));
        }
        catch (FileNotFoundException e) {
            throw new IllegalArgumentException("File [" + vmFile + "] not found", e);
        }
    }

    public boolean hasMoreCommands() {
        try {
            nextCommand = vmCommandReader.readLine();
        }
        catch (IOException e) {
            closeVmCommandReader();
            throw new RuntimeException("Could not read next command", e);
        }
        if (nextCommand == null) {
            closeVmCommandReader();
        }
        return nextCommand != null;
    }

    public void advance() {
        currentCommand = nextCommand;
    }

    public CommandType commandType() {
        if (currentCommand.trim().startsWith("/")) {
            return C_NONE;
        }
        if (currentCommand.contains("/")) {
            currentCommand = currentCommand.split("/")[0];
        }
        currentCommand = currentCommand.trim();
        if (ArithmeticCommand.fromString(currentCommand) != null) {
            return C_ARITHMETIC;
        }
        if (currentCommand.startsWith("call")) {
            return C_CALL;
        }
        if (currentCommand.startsWith("function")) {
            return C_FUNCTION;
        }
        if (currentCommand.startsWith("goto")) {
            return C_GOTO;
        }
        if (currentCommand.startsWith("if-goto")) {
            return C_IF;
        }
        if (currentCommand.startsWith("label")) {
            return C_LABEL;
        }
        if (currentCommand.startsWith("pop")) {
            return C_POP;
        }
        if (currentCommand.startsWith("push")) {
            return C_PUSH;
        }
        if (currentCommand.equals("return")) {
            return C_RETURN;
        }
        return C_NONE;
    }

    public Object arg1() {
        switch (commandType()) {
            case C_ARITHMETIC:
                return ArithmeticCommand.fromString(currentCommand);

            case C_CALL:
            case C_FUNCTION:
            case C_GOTO:
            case C_IF:
            case C_LABEL:
                return firstArg();

            case C_POP:
            case C_PUSH:
                return Segment.fromString(firstArg());
        }
        throw new RuntimeException("Unhandled command type: [" + commandType() + "]");
    }

    public int arg2() {
        return Integer.valueOf(currentCommand.split(" ")[2]);
    }

    private String firstArg() {
        return currentCommand.split(" ")[1];
    }

    private void closeVmCommandReader() {
        try {
            vmCommandReader.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
