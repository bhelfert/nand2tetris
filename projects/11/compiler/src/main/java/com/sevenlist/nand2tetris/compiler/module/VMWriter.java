package com.sevenlist.nand2tetris.compiler.module;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class VMWriter {

    private final BufferedWriter vmFileWriter;

    public VMWriter(File vmFile) {
        try {
            vmFileWriter = new BufferedWriter(new FileWriter(vmFile));
        }
        catch (IOException e) {
            close();
            throw new RuntimeException("Could not write .vm file [" + vmFile.getPath() + "]", e);
        }
    }

    public void writePush(Segment segment, int index) {
        writeLine("push " + segment + " " + index);
    }

    public void writePop(Segment segment, int index) {
        writeLine("pop " + segment + " " + index);
    }

    public void writeArithmetic(Operator operator) {
        writeLine(operator.toString());
    }

    public void writeLabel(String label) {
        writeLine("label " + label);
    }

    public void writeGoto(String label) {
        writeLine("goto " + label);
    }

    public void writeIf(String label) {
        writeLine("if-goto " + label);
    }

    public void writeCall(String name, int nArgs) {
        writeLine("call " + name + " " + nArgs);
    }

    public void writeFunction(String name, int nLocals) {
        writeLine("function " + name + " " + nLocals);
    }

    public void writeReturn() {
        writeLine("return");
    }

    public void close() {
        try {
            if (vmFileWriter != null) {
                vmFileWriter.close();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeLine(String line) {
        try {
            vmFileWriter.write(line);
            vmFileWriter.newLine();
        }
        catch (IOException e) {
            close();
            throw new RuntimeException("Could not write line [" + line + "] in .vm file", e);
        }
    }
}