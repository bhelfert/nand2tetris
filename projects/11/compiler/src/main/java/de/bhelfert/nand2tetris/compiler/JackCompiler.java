package de.bhelfert.nand2tetris.compiler;

import de.bhelfert.nand2tetris.compiler.module.CompilationEngine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class JackCompiler {

    private static final String JACK_FILE_EXTENSION = ".jack";

    private final String jackFileNameOrDirectoryName;

    public static void main(String[] args) {
        new JackCompiler(args).compile();
    }

    public JackCompiler(String args[]) {
        jackFileNameOrDirectoryName = args[0];
    }

    public void compile() {
        for (File jackFile : getJackFiles()) {
            new CompilationEngine(jackFile).compileClass();
        }
    }

    private List<File> getJackFiles() {
        if (jackFileNameOrDirectoryName.endsWith(JACK_FILE_EXTENSION)) {
            return Arrays.asList(new File(jackFileNameOrDirectoryName));
        }

        try (Stream<File> jackFileStream = Files.list(Paths.get(jackFileNameOrDirectoryName))
                .filter(p -> p.toString().endsWith(JACK_FILE_EXTENSION))
                .map(Path::toFile)
                .sorted()) {
            return jackFileStream.collect(toList());
        }
        catch (IOException e) {
            throw new RuntimeException("Could not get " + JACK_FILE_EXTENSION + " files in directory [" + jackFileNameOrDirectoryName + "]", e);
        }
    }
}
