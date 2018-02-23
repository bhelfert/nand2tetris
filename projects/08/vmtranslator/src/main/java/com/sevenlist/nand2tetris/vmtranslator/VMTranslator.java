package com.sevenlist.nand2tetris.vmtranslator;

import com.sevenlist.nand2tetris.vmtranslator.module.ArithmeticCommand;
import com.sevenlist.nand2tetris.vmtranslator.module.CodeWriter;
import com.sevenlist.nand2tetris.vmtranslator.module.Parser;
import com.sevenlist.nand2tetris.vmtranslator.module.Segment;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class VMTranslator {

    private static final String ASM_FILE_EXTENSION = ".asm";
    private static final String VM_FILE_EXTENSION = ".vm";

    private final String vmFileNameOrDirectoryName;

    public static void main(String[] args) {
        new VMTranslator(args).translate();
    }

    public VMTranslator(String[] args) {
        vmFileNameOrDirectoryName = args[0];
    }

    public void translate() {
        CodeWriter codeWriter = new CodeWriter(getAsmFile());
        List<File> vmFiles = getVmFiles();
        if (containsSysVmFile(vmFiles)) {
            codeWriter.writeInit();
        }
        for (File vmFile : vmFiles) {
            Parser parser = new Parser(vmFile);
            codeWriter.setVmFileName(vmFile.getName());
            while (parser.hasMoreCommands()) {
                parser.advance();
                switch (parser.commandType()) {
                    case C_ARITHMETIC:
                        codeWriter.writeArithmetic((ArithmeticCommand) parser.arg1());
                        break;

                    case C_CALL:
                        codeWriter.writeCall((String) parser.arg1(), parser.arg2());
                        break;

                    case C_FUNCTION:
                        codeWriter.writeFunction((String) parser.arg1(), parser.arg2());
                        break;

                    case C_GOTO:
                        codeWriter.writeGoto((String) parser.arg1());
                        break;

                    case C_IF:
                        codeWriter.writeIf((String) parser.arg1());
                        break;

                    case C_LABEL:
                        codeWriter.writeLabel((String) parser.arg1());
                        break;

                    case C_POP:
                    case C_PUSH:
                        codeWriter.writePushPop(parser.commandType(), (Segment) parser.arg1(), parser.arg2());
                        break;

                    case C_RETURN:
                        codeWriter.writeReturn();
                        break;
                }
            }
        }
        codeWriter.close();
    }

    private File getAsmFile() {
        if (vmFileNameOrDirectoryName.endsWith(VM_FILE_EXTENSION)) {
            return new File(vmFileNameOrDirectoryName.replace(VM_FILE_EXTENSION, ASM_FILE_EXTENSION));
        }

        String fileSeparator = System.getProperty("file.separator");
        int startIndexOfDeepestDirectory = vmFileNameOrDirectoryName.lastIndexOf(fileSeparator);
        String deepestDirectoryName = vmFileNameOrDirectoryName.substring(startIndexOfDeepestDirectory);
        String asmFileName = vmFileNameOrDirectoryName + fileSeparator + deepestDirectoryName + ASM_FILE_EXTENSION;
        return new File(asmFileName);
    }

    private List<File> getVmFiles() {
        if (vmFileNameOrDirectoryName.endsWith(VM_FILE_EXTENSION)) {
            return Arrays.asList(new File(vmFileNameOrDirectoryName));
        }

        try (Stream<File> vmFileStream = Files.list(Paths.get(vmFileNameOrDirectoryName))
                .filter(p -> p.toString().endsWith(VM_FILE_EXTENSION))
                .map(Path::toFile)
                .sorted()) {
            return vmFileStream.collect(toList());
        }
        catch (IOException e) {
            throw new RuntimeException("Could not get " + VM_FILE_EXTENSION + " files in directory [" + vmFileNameOrDirectoryName + "]", e);
        }
    }

    private boolean containsSysVmFile(List<File> vmFiles) {
        return vmFiles.stream().anyMatch(f -> f.getName().equals("Sys.vm"));
    }
}
