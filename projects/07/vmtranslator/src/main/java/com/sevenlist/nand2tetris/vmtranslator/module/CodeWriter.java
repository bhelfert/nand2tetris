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
        initializeStackPointer();
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

    private void initializeStackPointer() {
        writeComment("SP = 256");
        writeLine("@256");
        writeLine("D=A");
        writeLine("@SP");
        writeLine("M=D");
        writeEmptyLine();
    }

    private void incrementStackPointer() {
        writeComment("SP += 1");
        writeLine("@SP");
        writeLine("M=M+1");
    }

    private void decrementStackPointer() {
        writeComment("SP -= 1");
        writeLine("@SP");
        writeLine("M=M-1");
    }

    private void pushConstant(int value) {
        writeComment("push constant " + value);
        writeLine("@" + value);
        writeLine("D=A");
        writeLine("@SP");
        writeLine("A=M");
        writeLine("M=D");
        incrementStackPointer();
        writeEmptyLine();
    }

    private void add() {
        operateArithmetically("add", () -> {
            decrementStackPointer();
            dereferencePointer();
            decrementStackPointer();
            writeLine("A=M");
            writeLine("M=D+M");
        });
    }

    private void sub() {
        sub(true);
    }

    private void sub(boolean incrementStackPointer) {
        writeComment("sub");
        decrementStackPointer();
        dereferencePointer();
        decrementStackPointer();
        writeLine("A=M");
        writeLine("M=M-D");
        if (incrementStackPointer) {
            incrementStackPointer();
            writeEmptyLine();
        }
    }

    private void eq() {
        compareWith("eq", "D;JEQ");
    }

    private void lt() {
        compareWith("lt", "D;JLT");
    }

    private void gt() {
        compareWith("gt", "D;JGT");
    }

    private void neg() {
        negOrNor("neg", "-D");
    }

    private void and() {
        andOrOr("and", "D&M");
    }

    private void or() {
        andOrOr("or", "D|M");
    }

    private void not() {
        negOrNor("not", "!D");
    }

    private void operateArithmetically(String comment, Runnable instructions) {
        writeComment(comment);
        instructions.run();
        incrementStackPointer();
        writeEmptyLine();
    }

    private void compareWith(String comparison, String jumpInstruction) {
        writeComment(comparison);
        sub(false);
        writeLine("@SP");
        dereferencePointer();
        String ifTrueLabel = createLabelString("IF_TRUE");
        writeLine("@" + ifTrueLabel);
        writeLine(jumpInstruction);
        writeLine("@SP");
        writeLine("A=M");
        writeLine("M=0");
        String endIfLabel = createLabelString("END_IF");
        writeLine("@" + endIfLabel);
        writeLine("0;JMP");
        writeLine("(" + ifTrueLabel + ")");
        writeLine("@SP");
        writeLine("A=M");
        writeLine("M=-1");
        writeLine("(" + endIfLabel + ")");
        incrementStackPointer();
        writeEmptyLine();
    }

    private void dereferencePointer() {
        writeLine("A=M");
        writeLine("D=M");
    }

    private String createLabelString(String labelName) {
        return labelName + "_" + labelCounter++;
    }

    private void negOrNor(String operation, String instruction) {
        operateArithmetically(operation, () -> {
            decrementStackPointer();
            dereferencePointer();
            writeLine("M=" + instruction);
        });
    }

    private void andOrOr(String operation, String instruction) {
        operateArithmetically(operation, () -> {
            decrementStackPointer();
            dereferencePointer();
            decrementStackPointer();
            writeLine("A=M");
            writeLine("M=" + instruction);
        });
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

    private void writeComment(String comment) {
        writeLine("// " + comment);
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
