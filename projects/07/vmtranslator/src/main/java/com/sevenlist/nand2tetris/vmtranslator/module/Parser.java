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
        currentCommand = currentCommand.trim();
        if (ArithmeticCommand.fromString(currentCommand) != null) {
            return ARITHMETIC;
        }
        if (currentCommand.startsWith("push")) {
            return PUSH;
        }
        if (currentCommand.startsWith("pop")) {
            return POP;
        }
        return NONE;
    }

    public Enum<?> arg1() {
        switch (commandType()) {
            case ARITHMETIC:
                return ArithmeticCommand.fromString(currentCommand);

            case PUSH:
            case POP:
                return Segment.fromString(currentCommand.split(" ")[1]);
        }
        throw new IllegalStateException("Method arg1() may only be called when context is right");
    }

    public int arg2() {
        if (commandType().equals(ARITHMETIC)) {
            throw new IllegalStateException("Method arg2() may only be called for push commands");
        }
        return Integer.valueOf(currentCommand.split(" ")[2]);
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
