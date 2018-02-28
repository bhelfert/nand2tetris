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
    private final BufferedWriter jackTokenWriter;

    private boolean newLine;
    private String currentLine;
    private int currentPositionInLine;
    private int endOfLinePosition = -1;

    private TokenType tokenType;
    private Keyword keyword;
    private Symbol symbol;
    private String identifier;
    private int intValue = -1;
    private String stringValue;

    public JackTokenizer(File jackFile) {
        jackTokenReader = createJackTokenReader(jackFile);
        jackTokenWriter = createJackTokenWriter(jackFile);
        writeLine("<tokens>");
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
            writeLine("</tokens>");
            close(jackTokenWriter);
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

            if (!STRING_CONST.equals(tokenType)) {
                token = token.trim();
            }

            if (token.isEmpty()) {
                continue;
            }

            if (token.length() == 1) {
                Symbol symbol = Symbol.fromString(token);
                if (symbol != null) {
                    setToken(SYMBOL, symbol);
                    return;
                }
            }

            int nextPositionInLine = currentPositionInLine + 1;
            if (nextPositionInLine == endOfLinePosition) {
                return;
            }

            String nextTokenChar = String.valueOf(currentLine.charAt(nextPositionInLine));

            if (isIdentifier(token)) {
                if (scanIdentifierOrKeyword(token, nextTokenChar)) {
                    continue;
                }
                return;
            }

            if (isIntegerConstant(token)) {
                if (scanIntegerConstant(token, nextTokenChar)) {
                    continue;
                }
                return;
            }

            if (isStringConstant(token)) {
                if (scanStringConstant(token, nextTokenChar)) {
                    continue;
                }
                return;
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
        checkThatTokenIsOfType(KEYWORD);
        return keyword;
    }

    public Symbol symbol() {
        checkThatTokenIsOfType(SYMBOL);
        return symbol;
    }

    public String identifier() {
        checkThatTokenIsOfType(IDENTIFIER);
        return identifier;
    }

    public int intVal() {
        checkThatTokenIsOfType(INT_CONST);
        return intValue;
    }

    public String stringVal() {
        checkThatTokenIsOfType(STRING_CONST);
        return stringValue;
    }

    private BufferedReader createJackTokenReader(File jackFile) {
        try {
            return new BufferedReader(new FileReader(jackFile));
        }
        catch (FileNotFoundException e) {
            throw new IllegalArgumentException("File [" + jackFile + "] not found", e);
        }
    }

    private BufferedWriter createJackTokenWriter(File jackFile) {
        File tokenFile = new File(jackFile.getPath().replace(".jack", "T.xml"));
        try {
            return new BufferedWriter(new FileWriter(tokenFile));
        }
        catch (IOException e) {
            throw new RuntimeException("File [" + tokenFile + "] cannot be created", e);
        }
    }

    private String readLine() {
        String line;
        try {
            line = jackTokenReader.readLine();
        }
        catch (IOException e) {
            close(jackTokenReader);
            throw new RuntimeException("Could not read next line", e);
        }
        if (line == null) {
            close(jackTokenReader);
        }
        return line;
    }

    private void writeLine(String line) {
        try {
            jackTokenWriter.write(line);
            jackTokenWriter.newLine();
        }
        catch (IOException e) {
            close(jackTokenWriter);
            throw new RuntimeException("Could not write line [" + line + "] in .xml file", e);
        }
    }

    private void setToken(TokenType tokenType, Object value) {
        this.tokenType = tokenType;
        keyword = tokenType.equals(KEYWORD) ? (Keyword) value : null;
        symbol = tokenType.equals(SYMBOL) ? (Symbol) value : null;
        identifier = tokenType.equals(IDENTIFIER) ? (String) value : null;
        intValue = tokenType.equals(INT_CONST) ? (int) value : -1;
        stringValue = tokenType.equals(STRING_CONST) ? (String) value : null;
        ++currentPositionInLine;
        writeTokenInXml(tokenType, value);
    }

    private void writeTokenInXml(TokenType tokenType, Object value) {
        String valueAsString = tokenType.equals(SYMBOL) ? ((Symbol) value).toEscapeString() : value.toString();
        writeLine("<" + tokenType + "> " + valueAsString + " </" + tokenType + ">");
    }

    private boolean isIdentifier(String token) {
        String firstTokenChar = getFirstChar(token);
        boolean startsWithLetterOrUnderscore = (LETTER_PATTERN.matcher(firstTokenChar).matches() || firstTokenChar.equals(UNDERSCORE));
        boolean containsLettersOrDigitsOrUnderscore = IDENTIFIER_PATTERN.matcher(token).matches();
        return startsWithLetterOrUnderscore && containsLettersOrDigitsOrUnderscore;
    }

    private boolean scanIdentifierOrKeyword(String token, String nextTokenChar) {
        identifier = token;
        if (!isSpaceOrSymbol(nextTokenChar)) {
            return true; // continue scanning of token
        }

        Keyword keyword = Keyword.fromString(token);
        if (keyword != null) {
            setToken(KEYWORD, keyword);
        }
        else {
            setToken(IDENTIFIER, identifier);
        }
        return false;
    }

    private boolean isIntegerConstant(String token) {
        String firstTokenChar = getFirstChar(token);
        return DIGIT_PATTERN.matcher(firstTokenChar).matches();
    }

    private boolean scanIntegerConstant(String token, String nextTokenChar) {
        intValue = Integer.valueOf(token);
        if (!isSpaceOrSymbol(nextTokenChar)) {
            return true; // continue scanning of token
        }
        checkIfIntValueIsValid();
        setToken(INT_CONST, intValue);
        return false;
    }

    private boolean isStringConstant(String token) {
        String firstTokenChar = getFirstChar(token);
        return firstTokenChar.equals(DOUBLE_QUOTE);
    }

    private boolean scanStringConstant(String token, String nextTokenChar) {
        stringValue = token;
        tokenType = STRING_CONST;
        if (!nextTokenChar.equals(DOUBLE_QUOTE)) {
            return true; // continue scanning of token
        }
        stringValue = stringValue.substring(1, stringValue.length());
        setToken(STRING_CONST, stringValue);
        ++currentPositionInLine;
        return false;
    }

    private String getFirstChar(String token) {
        return String.valueOf(token.charAt(0));
    }

    private boolean isSpaceOrSymbol(String tokenChar) {
        return tokenChar.equals(SPACE) || (Symbol.fromString(tokenChar) != null);
    }

    private void checkIfIntValueIsValid() {
        if (intValue > 32767) {
            throw new RuntimeException("Integer constant is too big (> 32767): " + intValue);
        }
    }

    private void checkThatTokenIsOfType(TokenType tokenType) {
        if (!this.tokenType.equals(tokenType)) {
            throw new IllegalStateException("TokenType is " + this.tokenType + " instead of " + tokenType);
        }
    }

    private void close(Closeable closeable) {
        try {
            closeable.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}