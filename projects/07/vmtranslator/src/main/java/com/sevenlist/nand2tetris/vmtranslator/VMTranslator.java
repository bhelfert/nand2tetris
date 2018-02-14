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

    private final String vmFileNameOrDirectoryName;

    public static void main(String[] args) {
        new VMTranslator(args).translate();
    }

    public VMTranslator(String[] args) {
        vmFileNameOrDirectoryName = args[0];
    }

    public void translate() {
        for (File vmFile : getVmFiles()) {
            Parser parser = new Parser(vmFile);
            CodeWriter codeWriter = new CodeWriter(getAsmFile(vmFile));
            while (parser.hasMoreCommands()) {
                parser.advance();
                switch (parser.commandType()) {
                    case ARITHMETIC:
                        codeWriter.writeArithmetic((ArithmeticCommand) parser.arg1());
                        break;

                    case POP:
                    case PUSH:
                        codeWriter.writePushPop(parser.commandType(), (Segment) parser.arg1(), parser.arg2());
                        break;
                }
            }
            codeWriter.close();
        }
    }

    private List<File> getVmFiles() {
        if (vmFileNameOrDirectoryName.endsWith(".vm")) {
            return Arrays.asList(new File(vmFileNameOrDirectoryName));
        }

        try (Stream<File> vmFileStream = Files.list(Paths.get(vmFileNameOrDirectoryName))
                     .filter(p -> p.toString().endsWith(".vm"))
                     .map(Path::toFile)) {
            return vmFileStream.collect(toList());
        }
        catch (IOException e) {
            throw new RuntimeException("Could not get VM files in directory [" + vmFileNameOrDirectoryName + "]", e);
        }
    }

    private File getAsmFile(File vmFile) {
        return new File(vmFile.getPath().replace(".vm", ".asm"));
    }
}
