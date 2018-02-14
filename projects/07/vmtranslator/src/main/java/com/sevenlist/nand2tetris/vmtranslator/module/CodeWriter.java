package com.sevenlist.nand2tetris.vmtranslator.module;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class CodeWriter {

    private final BufferedWriter asmFileWriter;
    private int labelCounter = 0;

    public CodeWriter(File asmFile) {
        try {
            asmFileWriter = new BufferedWriter(new FileWriter(asmFile));
        }
        catch (IOException e) {
            throw new RuntimeException("Could not write .asm file [" + asmFile.getPath() + "]", e);
        }
    }

    public void writeArithmetic(ArithmeticCommand command) {
        invokeParameterlessMethod(command.toString());
    }

    public void writePushPop(CommandType commandType, Segment segment, int index) {
        invokeMethodWithIntParameter(commandType.toString() + segment.toStringCapitalized(), index);
    }

    public void close() {
        try {
            asmFileWriter.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void pushConstant(int value) {
        writeComment("push constant " + value);
        writeLine("@" + value);
        writeLine("D=A");
        writeLine("@SP");
        writeLine("A=M");
        writeLine("M=D");
        writeLine("@SP");
        writeLine("M=M+1");
        writeEmptyLine();
    }

    private void add() {
        addSubAndOr("add",  "+");
    }

    private void sub() {
        addSubAndOr("sub", "-");
    }

    private void and() {
        addSubAndOr("and", "&");
    }

    private void or() {
        addSubAndOr("or", "|");
    }

    private void addSubAndOr(String command, String operator) {
        writeComment(command);
        writeLine("@SP");
        writeLine("M=M-1");
        writeLine("A=M");
        writeLine("D=M");
        writeLine("A=A-1");
        writeLine("M=M" + operator + "D");
        writeEmptyLine();
    }

    private void eq() {
        eqLtGt("eq");
    }

    private void lt() {
        eqLtGt("lt");
    }

    private void gt() {
        eqLtGt("gt");
    }

    private void eqLtGt(String command) {
        writeComment(command);
        writeLine("@SP");
        writeLine("M=M-1");
        writeLine("A=M");
        writeLine("D=M");
        writeLine("A=A-1");
        writeLine("D=M-D");
        String ifTrueLabel = createLabelString("IF_TRUE");
        writeLine("@" + ifTrueLabel);
        writeLine("D;J" + command.toUpperCase());
        writeLine("@SP");
        writeLine("A=M-1");
        writeLine("M=0");
        String endIfLabel = createLabelString("END_IF");
        writeLine("@" + endIfLabel);
        writeLine("0;JMP");
        writeLine("(" + ifTrueLabel + ")");
        writeLine("@SP");
        writeLine("A=M-1");
        writeLine("M=-1");
        writeLine("(" + endIfLabel + ")");
        writeEmptyLine();
    }

    private void neg() {
        negNot("neg", "-");
    }

    private void not() {
        negNot("not", "!");
    }

    private void negNot(String command, String operator) {
        writeComment(command);
        writeLine("@SP");
        writeLine("A=M-1");
        writeLine("M=" + operator + "M");
        writeEmptyLine();
    }

    private String createLabelString(String labelName) {
        return labelName + "_" + labelCounter++;
    }

    private void writeComment(String comment) {
        writeLine("// " + comment);
    }

    private void writeLine(String line) {
        try {
            asmFileWriter.write(line);
            asmFileWriter.write(System.lineSeparator());
        }
        catch (IOException e) {
            throw new RuntimeException("Could not write line [" + line + "] in .asm file", e);
        }
    }

    private void writeEmptyLine() {
        writeLine("");
    }

    private void invokeParameterlessMethod(String methodName) {
        try {
            CodeWriter.class.getDeclaredMethod(methodName, null).invoke(this, null);
        }
        catch (Exception e) {
            throw new RuntimeException("Could not invoke method with name [" + methodName + "]", e);
        }
    }

    private void invokeMethodWithIntParameter(String methodName, Integer value) {
        try {
            CodeWriter.class.getDeclaredMethod(methodName, new Class[]{int.class}).invoke(this, value);
        }
        catch (Exception e) {
            throw new RuntimeException("Could not invoke method with name [" + methodName + "]", e);
        }
    }
}
