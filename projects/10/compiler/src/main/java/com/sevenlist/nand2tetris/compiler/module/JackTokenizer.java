package com.sevenlist.nand2tetris.compiler.module;

import java.io.*;

public class JackTokenizer {

    private final BufferedReader jackTokenReader;
    private String currentToken;
    private String nextToken;

    public JackTokenizer(File jackFile) {
        try {
            jackTokenReader = new BufferedReader(new FileReader(jackFile));
        }
        catch (FileNotFoundException e) {
            throw new IllegalArgumentException("File [" + jackFile + "] not found", e);
        }
    }

    public boolean hasMoreTokens() {
        try {
            nextToken = jackTokenReader.readLine();
        }
        catch (IOException e) {
            closeJackTokenReader();
            throw new RuntimeException("Could not read next token", e);
        }
        if (nextToken == null) {
            closeJackTokenReader();
        }
        return nextToken != null;
    }

    public void advance() {
        currentToken = nextToken;
    }

    public TokenType tokenType() {
        return null;
    }

    public Keyword keyword() {
        return null;
    }

    public String symbol() {
        return "";
    }

    public String identifier() {
        return "";
    }

    public int intVal() {
        return -1;
    }

    public String stringVal() {
        return "";
    }

    private void closeJackTokenReader() {
        try {
            jackTokenReader.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
