package com.sevenlist.nand2tetris.vmtranslator.module;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static com.sevenlist.nand2tetris.vmtranslator.module.Segment.*;

public class CodeWriter {

    private final BufferedWriter asmFileWriter;
    private final String staticVariablePrefix;
    private int labelCounter = 0;

    public CodeWriter(File asmFile) {
        try {
            asmFileWriter = new BufferedWriter(new FileWriter(asmFile));
            staticVariablePrefix = asmFile.getName().split("\\.")[0];
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

    public void writePushPop(CommandType commandType, Segment segment, int valueOrIndex) {
        switch (commandType) {
            case PUSH:
                push(segment, valueOrIndex);
                break;

            case POP:
                pop(segment, valueOrIndex);
                break;
        }
    }

    public void close() {
        try {
            asmFileWriter.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void push(Segment segment, int valueOrIndex) {
        writeComment("push " + segment + " " + valueOrIndex);
        if (segment.equals(CONSTANT)) {
            writeLine("@" + valueOrIndex);
            writeLine("D=A");
        }
        else {
            if (segment.equals(POINTER)) {
                writeLine("@" + ((valueOrIndex == 1) ? "R4" : segment.baseAddress()));
            }
            else if (segment.equals(STATIC)) {
                writeLine("@" + createStaticAddress(valueOrIndex));
            }
            else {
                if (valueOrIndex > 0) {
                    writeAddressOfSegmentIndexToR13(segment, valueOrIndex);
                }
                writeLine("@" + ((valueOrIndex > 0) ? "R13" : segment.baseAddress()));
                writeLine("A=M");
            }
            writeLine("D=M");
        }
        writeLine("@SP");
        writeLine("A=M");
        writeLine("M=D");
        writeLine("@SP");
        writeLine("M=M+1");
    }

    private void pop(Segment segment, int index) {
        writeComment("pop " + segment.toString() + " " + index);
        if (!segment.equals(POINTER) && !segment.equals(STATIC) && index > 0) {
            writeAddressOfSegmentIndexToR13(segment, index);
        }
        writeLine("@SP");
        writeLine("AM=M-1");
        writeLine("D=M");
        if (segment.equals(POINTER)) {
            writeLine("@" + ((index == 1) ? "R4" : segment.baseAddress()));
        }
        else if (segment.equals(STATIC)) {
            writeLine("@" + createStaticAddress(index));
        }
        else {
            writeLine("@" + ((index > 0) ? "R13" : segment.baseAddress()));
            writeLine("A=M");
        }
        writeLine("M=D");
    }

    private void writeAddressOfSegmentIndexToR13(Segment segment, int index) {
        writeLine("@" + segment.baseAddress());
        writeLine("D=" + (segment.equals(TEMP) ? "A" : "M"));
        writeLine("@" + index);
        writeLine("D=A+D");
        writeLine("@R13");
        writeLine("M=D");
    }

    private String createStaticAddress(int index) {
        return staticVariablePrefix + "." + index;
    }

    private void addSubAndOr(ArithmeticCommand command) {
        writeComment(command.toString());
        writeLine("@SP");
        writeLine("AM=M-1");
        writeLine("D=M");
        writeLine("A=A-1");
        writeLine("M=M" + command.operator() + "D");
    }

    private void eqLtGt(ArithmeticCommand command) {
        writeComment(command.toString());
        writeLine("@SP");
        writeLine("AM=M-1");
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
    }

    private void negNot(ArithmeticCommand command) {
        writeComment(command.toString());
        writeLine("@SP");
        writeLine("A=M-1");
        writeLine("M=" + command.operator() + "M");
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
}
