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
        switch (command) {
            case ADD:
            case SUB:
            case AND:
            case OR:
                addSubAndOr(command);
                break;

            case EQ:
            case LT:
            case GT:
                eqLtGt(command);
                break;

            case NEG:
            case NOT:
                negNot(command);
                break;
        }
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

    private void addSubAndOr(ArithmeticCommand command) {
        writeComment(command.toString());
        writeLine("@SP");
        writeLine("M=M-1");
        writeLine("A=M");
        writeLine("D=M");
        writeLine("A=A-1");
        writeLine("M=M" + command.operator() + "D");
        writeEmptyLine();
    }

    private void eqLtGt(ArithmeticCommand command) {
        writeComment(command.toString());
        writeLine("@SP");
        writeLine("M=M-1");
        writeLine("A=M");
        writeLine("D=M");
        writeLine("A=A-1");
        writeLine("D=M-D");
        String ifTrueLabel = createLabelString("IF_TRUE");
        writeLine("@" + ifTrueLabel);
        writeLine("D;J" + command.name());
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

    private void negNot(ArithmeticCommand command) {
        writeComment(command.toString());
        writeLine("@SP");
        writeLine("A=M-1");
        writeLine("M=" + command.operator() + "M");
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

    private void invokeMethodWithIntParameter(String methodName, Integer value) {
        try {
            CodeWriter.class.getDeclaredMethod(methodName, new Class[]{int.class}).invoke(this, value);
        }
        catch (Exception e) {
            throw new RuntimeException("Could not invoke method with name [" + methodName + "]", e);
        }
    }
}
