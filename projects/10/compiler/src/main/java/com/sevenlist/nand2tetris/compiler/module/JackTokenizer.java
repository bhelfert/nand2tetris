package com.sevenlist.nand2tetris.compiler.module;

import java.io.*;
import java.util.regex.Pattern;

import static com.sevenlist.nand2tetris.compiler.module.TokenType.*;

public class JackTokenizer {

    private static final String DOUBLE_QUOTE = "\"";
    private static final String SPACE = " ";
    private static final String UNDERSCORE = "_";

    private static final Pattern LETTER_PATTERN = Pattern.compile("[a-zA-Z]+");
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("\\w+");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("\\d+");

    private final CommentParser commentParser = new CommentParser();
    private final BufferedReader jackTokenReader;

    private boolean newLine;
    private String currentLine;
    private int endOfLinePosition = -1;
    private int currentPositionInLine;

    private TokenType tokenType;
    private Keyword keyword;
    private Symbol symbol;
    private String identifier;
    private int intValue = -1;
    private String stringValue;

    public JackTokenizer(File jackFile) {
        try {
            jackTokenReader = new BufferedReader(new FileReader(jackFile));
        }
        catch (FileNotFoundException e) {
            throw new IllegalArgumentException("File [" + jackFile + "] not found", e);
        }
    }

    public boolean hasMoreTokens() {
        if (!COMMENT_OR_EMPTY_LINE.equals(tokenType) && (endOfLinePosition > -1)) {
            if (currentPositionInLine < endOfLinePosition) {
                newLine = false;
                return true;
            }
        }
        currentLine = readLine();
        newLine = true;
        if (currentLine == null) {
            endOfLinePosition = -1;
            return false;
        }
        currentPositionInLine = 0;
        // make sure calling hasMoreTokens() works as expected when advance() is not called in between
        endOfLinePosition = currentLine.length();
        return true;
    }

    public void advance() {
        if (newLine) {
            if (commentParser.isCommentOrWhitespace(currentLine)) {
                tokenType = COMMENT_OR_EMPTY_LINE;
                return;
            }
            tokenType = null;
            currentLine = commentParser.stripComments(currentLine);
            endOfLinePosition = currentLine.length();
        }

        String token = "";
        for (; currentPositionInLine < endOfLinePosition; currentPositionInLine++) {
            token += currentLine.charAt(currentPositionInLine);
            token = token.trim();

            if (token.isEmpty()) {
                continue;
            }

            Keyword keyword = Keyword.fromString(token);
            if (keyword != null) {
                tokenType = KEYWORD;
                this.keyword = keyword;
                symbol = null;
                identifier = null;
                intValue = -1;
                stringValue = null;
                ++currentPositionInLine;
                System.out.println("Keyword: " + this.keyword);
                return;
            }

            String firstTokenChar = String.valueOf(token.charAt(0));

            if (isIdentifier(firstTokenChar)) {
                if (IDENTIFIER_PATTERN.matcher(token).matches()) {
                    identifier = token;
                    int nextPositionInLine = currentPositionInLine + 1;
                    if (nextPositionInLine < endOfLinePosition) {
                        String nextTokenChar = String.valueOf(currentLine.charAt(nextPositionInLine));
                        if (nextTokenChar.equals(SPACE) || (Symbol.fromString(nextTokenChar) != null)) {
                            tokenType = IDENTIFIER;
                            this.keyword = null;
                            this.symbol = null;
                            intValue = -1;
                            stringValue = null;
                            ++currentPositionInLine;
                            System.out.println("Identifier: " + identifier);
                            return;
                        }
                    }
                }
                continue;
            }

            Symbol symbol = Symbol.fromString(token);
            if (symbol != null) {
                tokenType = SYMBOL;
                this.keyword = null;
                this.symbol = symbol;
                identifier = null;
                intValue = -1;
                stringValue = null;
                ++currentPositionInLine;
                System.out.println("Symbol: " + this.symbol);
                return;
            }

            if (isIntegerConstant(firstTokenChar)) {
                intValue = Integer.valueOf(token);
                if (intValue > 32767) {
                    throw new RuntimeException("Integer constant is too big (> 32767): " + token);
                }
                int nextPositionInLine = currentPositionInLine + 1;
                if (nextPositionInLine < endOfLinePosition) {
                    String nextTokenChar = String.valueOf(currentLine.charAt(nextPositionInLine));
                    if (nextTokenChar.equals(SPACE) || (Symbol.fromString(nextTokenChar) != null)) {
                        tokenType = INT_CONST;
                        this.keyword = null;
                        this.symbol = null;
                        identifier = null;
                        stringValue = null;
                        ++currentPositionInLine;
                        System.out.println("Integer constant: " + intValue);
                        return;
                    }
                }
                continue;
            }

            if (isStringConstant(firstTokenChar)) {
                stringValue = token;
                int nextPositionInLine = currentPositionInLine + 1;
                if (nextPositionInLine < endOfLinePosition) {
                    String nextTokenChar = String.valueOf(currentLine.charAt(nextPositionInLine));
                    if (nextTokenChar.equals(DOUBLE_QUOTE)) {
                        tokenType = STRING_CONST;
                        this.keyword = null;
                        this.symbol = null;
                        identifier = null;
                        intValue = -1;
                        stringValue = stringValue.substring(1, currentPositionInLine);
                        ++currentPositionInLine;
                        System.out.println("String constant: " + stringValue);
                        return;
                    }
                }
                continue;
            }
        }
    }

    public TokenType tokenType() {
        if (tokenType == null) {
            throw new IllegalStateException("TokenType is null; call advance() if hasMoreTokens()");
        }
        return tokenType;
    }

    public Keyword keyword() {
        if (!tokenType.equals(KEYWORD)) {
            throw new IllegalStateException("TokenType is " + tokenType + " instead of " + KEYWORD);
        }
        return keyword;
    }

    public Symbol symbol() {
        if (!tokenType.equals(SYMBOL)) {
            throw new IllegalStateException("TokenType is " + tokenType + " instead of " + SYMBOL);
        }
        return symbol;
    }

    public String identifier() {
        if (!tokenType.equals(IDENTIFIER)) {
            throw new IllegalStateException("TokenType is " + tokenType + " instead of " + IDENTIFIER);
        }
        return "";
    }

    public int intVal() {
        if (!tokenType.equals(INT_CONST)) {
            throw new IllegalStateException("TokenType is " + tokenType + " instead of " + INT_CONST);
        }
        return -1;
    }

    public String stringVal() {
        if (!tokenType.equals(STRING_CONST)) {
            throw new IllegalStateException("TokenType is " + tokenType + " instead of " + STRING_CONST);
        }
        return "";
    }

    private String readLine() {
        String line;
        try {
            line = jackTokenReader.readLine();
        }
        catch (IOException e) {
            closeJackTokenReader();
            throw new RuntimeException("Could not read next line", e);
        }
        if (line == null) {
            closeJackTokenReader();
        }
        return line;
    }

    private boolean isIdentifier(String firstTokenChar) {
        return firstTokenChar.equals(UNDERSCORE) || LETTER_PATTERN.matcher(firstTokenChar).matches();
    }

    private boolean isIntegerConstant(String firstTokenChar) {
        return DIGIT_PATTERN.matcher(firstTokenChar).matches();
    }

    private boolean isStringConstant(String firstTokenChar) {
        return firstTokenChar.equals(DOUBLE_QUOTE);
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
