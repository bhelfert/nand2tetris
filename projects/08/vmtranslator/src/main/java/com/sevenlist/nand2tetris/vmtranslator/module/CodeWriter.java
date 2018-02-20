package com.sevenlist.nand2tetris.vmtranslator.module;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static com.sevenlist.nand2tetris.vmtranslator.module.Segment.*;

public class CodeWriter {

    private final BufferedWriter asmFileWriter;
    private String staticVariablePrefix;
    private int labelCounter = 0;
    private String functionName;

    public CodeWriter(File asmFile) {
        try {
            asmFileWriter = new BufferedWriter(new FileWriter(asmFile));
        }
        catch (IOException e) {
            throw new RuntimeException("Could not write .asm file [" + asmFile.getPath() + "]", e);
        }
    }

    public void setVmFileName(String vmFileName) {
        staticVariablePrefix = vmFileName.substring(0, vmFileName.indexOf(".vm"));
    }

    public void writeInit() {
        initializeStackPointer();
        writeCall("Sys.init", 0);
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

    public void writeCall(String functionName, int numArgs) {
        writeComment("call " + functionName + " " + numArgs);
        String returnLabel = saveCallerReturnAddress();
        saveCallerSegment(LOCAL);
        saveCallerSegment(ARGUMENT);
        saveCallerSegment(THIS);
        saveCallerSegment(THAT);
        repositionArgumentPointer(numArgs);
        repositionLocalPointer();
        gotoFunction(functionName);
        writeLine("(" + returnLabel + ")");
    }

    public void writeFunction(String functionName, int numLocals) {
        this.functionName = functionName;
        writeComment("function " + functionName + " " + numLocals);
        writeLine("(" + functionName + ")");
        for (int i = 0; i < numLocals; i++) {
            push(CONSTANT, 0);
        }
    }

    public void writeGoto(String labelName) {
        writeComment("goto " + labelName);
        writeLine("@" + createLabelStringWithFunctionName(labelName));
        writeLine("0;JMP");
    }

    public void writeIf(String labelName) {
        writeComment("if-goto " + labelName);
        writeLine("@SP");
        writeLine("AM=M-1");
        writeLine("D=M");
        writeLine("@" + createLabelStringWithFunctionName(labelName));
        writeLine("D;JNE");
    }

    public void writeLabel(String labelName) {
        writeComment("label " + labelName);
        writeLine("(" + createLabelStringWithFunctionName(labelName) + ")");
    }

    public void writePushPop(CommandType commandType, Segment segment, int valueOrIndex) {
        switch (commandType) {
            case C_POP:
                pop(segment, valueOrIndex);
                break;

            case C_PUSH:
                push(segment, valueOrIndex);
                break;
        }
    }

    public void writeReturn() {
        writeComment("return (from " + functionName + ")");
        restoreCallerStackPointer();
        storeTemporaryFrameVariable();
        restoreCallerSegment(THAT, 1);
        restoreCallerSegment(THIS, 2);
        restoreCallerSegment(ARGUMENT, 3);
        restoreCallerSegment(LOCAL, 4);
        gotoReturnAddress();
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
    }

    private String createLabelStringWithFunctionName(String labelName) {
        return functionName + "$" + labelName;
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
        String ifTrueLabel = createLabelStringWithNumber("IF_TRUE");
        writeLine("@" + ifTrueLabel);
        writeLine("D;J" + command.name());
        writeLine("@SP");
        writeLine("A=M-1");
        writeLine("M=0");
        String endIfLabel = createLabelStringWithNumber("END_IF");
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

    private String createLabelStringWithNumber(String labelName) {
        return labelName + "_" + labelCounter++;
    }

    private String saveCallerReturnAddress() {
        String returnLabel = createLabelStringWithNumber("RET");
        writeComment("push " + returnLabel);
        writeLine("@" + returnLabel);
        writeLine("D=A");
        writeLine("@SP");
        writeLine("A=M");
        writeLine("M=D");
        writeLine("@SP");
        writeLine("M=M+1");
        return returnLabel;
    }

    private void saveCallerSegment(Segment segment) {
        writeComment("push " + segment.baseAddress());
        writeLine("@" + segment.baseAddress());
        writeLine("D=M");
        writeLine("@SP");
        writeLine("A=M");
        writeLine("M=D");
        writeLine("@SP");
        writeLine("M=M+1");
    }

    private void repositionArgumentPointer(int numArgs) {
        writeComment("ARG = SP-" + numArgs + "-5");
        writeLine("@" + (numArgs + 5));
        writeLine("D=A");
        writeLine("@SP");
        writeLine("A=M");
        writeLine("D=A-D");
        writeLine("@ARG");
        writeLine("M=D");
    }

    private void repositionLocalPointer() {
        writeComment("LCL = SP");
        writeLine("@SP");
        writeLine("D=M");
        writeLine("@LCL");
        writeLine("M=D");
    }

    private void gotoFunction(String functionName) {
        writeComment("goto " + functionName);
        writeLine("@" + functionName);
        writeLine("0;JMP");
    }

    private void restoreCallerStackPointer() {
        writeComment("*ARG = pop()");
        writeLine("@SP");
        writeLine("AM=M-1");
        writeLine("D=M");
        writeLine("@ARG");
        writeLine("A=M");
        writeLine("M=D");

        writeComment("SP = ARG+1");
        writeLine("@ARG");
        writeLine("D=M");
        writeLine("@SP");
        writeLine("M=D+1");
    }

    private void storeTemporaryFrameVariable() {
        writeComment("FRAME = LCL");
        writeLine("@LCL");
        writeLine("D=M");
        writeLine("@R13"); // FRAME
        writeLine("M=D");
    }

    private void restoreCallerSegment(Segment segment, int reverseOffset) {
        writeComment(segment.baseAddress() + " = *(FRAME-" + reverseOffset + ")");
        writeLine("@R13");
        writeLine("AM=M-1");
        writeLine("D=M");
        writeLine("@" + segment.baseAddress());
        writeLine("M=D");
    }

    private void gotoReturnAddress() {
        writeComment("goto RET = *(FRAME-5)");
        writeLine("@R13");
        writeLine("AM=M-1");
        writeLine("A=M");
        writeLine("0;JMP");
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
