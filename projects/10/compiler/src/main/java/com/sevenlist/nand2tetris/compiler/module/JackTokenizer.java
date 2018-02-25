package com.sevenlist.nand2tetris.compiler.module;

import java.io.*;

import static com.sevenlist.nand2tetris.compiler.module.TokenType.*;

public class JackTokenizer {

    private final CommentParser commentParser = new CommentParser();
    private final BufferedReader jackTokenReader;
    private String currentLine;
    private String nextLine;

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
            nextLine = jackTokenReader.readLine();
        }
        catch (IOException e) {
            closeJackTokenReader();
            throw new RuntimeException("Could not read next token", e);
        }
        if (nextLine == null) {
            closeJackTokenReader();
        }
        return nextLine != null;
    }

    public void advance() {
        currentLine = nextLine;
    }

    public TokenType tokenType() {
        if (commentParser.isCommentOrWhitespace(currentLine)) {
            return COMMENT_OR_EMPTY_LINE;
        }
        currentLine = commentParser.stripComments(currentLine);
        return null;
    }

    public Keyword keyword() {
        if (!tokenType().equals(KEYWORD)) {
            throw new IllegalStateException("TokenType is " + tokenType() + " instead of " + KEYWORD);
        }
        return null;
    }

    public Symbol symbol() {
        if (!tokenType().equals(SYMBOL)) {
            throw new IllegalStateException("TokenType is " + tokenType() + " instead of " + SYMBOL);
        }
        return null;
    }

    public String identifier() {
        if (!tokenType().equals(IDENTIFIER)) {
            throw new IllegalStateException("TokenType is " + tokenType() + " instead of " + IDENTIFIER);
        }
        return "";
    }

    public int intVal() {
        if (!tokenType().equals(INT_CONST)) {
            throw new IllegalStateException("TokenType is " + tokenType() + " instead of " + INT_CONST);
        }
        return -1;
    }

    public String stringVal() {
        if (!tokenType().equals(STRING_CONST)) {
            throw new IllegalStateException("TokenType is " + tokenType() + " instead of " + STRING_CONST);
        }
        return "";
    }

    private boolean isIntegerConstant(String token) {
        // Integer.parseUnsignedInt() allows the first character of the passed string argument to be a plus sign; Jack doesn't
        if (token.startsWith("+")) {
            return false;
        }
        try {
            int unsignedInt = Integer.parseUnsignedInt(token);
            return unsignedInt <= 32767;
        }
        catch (NumberFormatException e) {
            return false;
        }
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
