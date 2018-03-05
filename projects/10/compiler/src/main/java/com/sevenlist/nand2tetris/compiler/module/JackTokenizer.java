package com.sevenlist.nand2tetris.compiler.module;

import java.io.*;
import java.util.regex.Pattern;

import static com.sevenlist.nand2tetris.compiler.module.JackTokenizer.ScanMode.*;
import static com.sevenlist.nand2tetris.compiler.module.TokenType.*;

public class JackTokenizer {

    private static final String DOUBLE_QUOTE = "\"";
    private static final String SPACE = " ";
    private static final String UNDERSCORE = "_";

    private static final Pattern LETTER_PATTERN = Pattern.compile("[a-zA-Z]+");
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("\\w+");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("\\d+");

    private final CommentScanner commentScanner = new CommentScanner();
    private final BufferedReader jackTokenReader;
    private BufferedWriter jackTokenWriter;

    private ScanMode scanMode;
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
        openTokenFile(jackFile);
    }

    // ugly code: method configures scanner as side effect
    public boolean hasMoreTokens() {
        if (hasCurrentLineMoreTokens()) {
            configureScannerFor(CONTINUE_SCANNING_CURRENT_LINE);
            return true;
        }

        readNextLine();

        if (hasEndOfFileBeReached()) {
            configureScannerFor(SCAN_NEXT_JACK_FILE);
            closeTokenFile();
            return false;
        }

        configureScannerFor(SCAN_NEW_LINE);
        return true;
    }

    public void advance() {
        if (scanComments() == SCAN_NEXT_TOKEN) {
            return;
        }
        scanTokens();
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

    private void openTokenFile(File jackFile) {
        jackTokenWriter = createJackTokenWriter(jackFile);
        writeLine("<tokens>");
    }

    private void closeTokenFile() {
        writeLine("</tokens>");
        close(jackTokenWriter);
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

    private boolean hasCurrentLineMoreTokens() {
        return hasFirstLineOfNewFileBeRead() && !hasLineFullyBeScanned() && !isCommentLineOrEmptyLine();
    }

    private boolean hasFirstLineOfNewFileBeRead() {
        return endOfLinePosition > -1;
    }

    private boolean hasLineFullyBeScanned() {
        return (currentPositionInLine == endOfLinePosition);
    }

    private boolean isCommentLineOrEmptyLine() {
        return COMMENT_LINE_OR_EMPTY_LINE.equals(tokenType);
    }

    private void readNextLine() {
        currentLine = readLine();
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

    private boolean hasEndOfFileBeReached() {
        return currentLine == null;
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

    private void configureScannerFor(ScanMode scanMode) {
        this.scanMode = scanMode;
        switch (scanMode) {
            case SCAN_NEXT_JACK_FILE:
                endOfLinePosition = -1;
                break;

            case SCAN_NEW_LINE:
                currentPositionInLine = 0;
                // make sure calling hasMoreTokens() works as expected when advance() is not called in between
                endOfLinePosition = currentLine.length();
                break;
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

    private ScanMode scanComments() {
        if (scanMode.equals(SCAN_NEW_LINE)) {
            if (commentScanner.isCommentOrWhitespace(currentLine)) {
                tokenType = COMMENT_LINE_OR_EMPTY_LINE;
                return SCAN_NEXT_TOKEN;
            }
            tokenType = null;
            currentLine = commentScanner.stripComments(currentLine);
            endOfLinePosition = currentLine.length();
        }
        return CONTINUE_SCANNING_TOKEN;
    }

    private void scanTokens() {
        String tokenChars = "";
        for (; currentPositionInLine < endOfLinePosition; currentPositionInLine++) {
            tokenChars += currentLine.charAt(currentPositionInLine);

            if (isNotStringConstant()) {
                tokenChars = tokenChars.trim();
            }

            if (tokenChars.isEmpty()) {
                continue;
            }

            if (isSymbol(tokenChars)) {
                if (scanSymbol(tokenChars) == SCAN_NEXT_TOKEN) {
                    return;
                }
            }

            if (isEndOfLineReached()) {
                return;
            }

            String firstTokenChar = String.valueOf(tokenChars.charAt(0));
            String nextTokenChar = String.valueOf(currentLine.charAt(currentPositionInLine + 1));

            if (isIdentifierOrKeyword(firstTokenChar, tokenChars)) {
                if (scanIdentifierOrKeyword(tokenChars, nextTokenChar) == SCAN_NEXT_TOKEN) {
                    return;
                }
                continue;
            }

            if (isIntegerConstant(firstTokenChar)) {
                if (scanIntegerConstant(tokenChars, nextTokenChar) == SCAN_NEXT_TOKEN) {
                    return;
                }
                continue;
            }

            if (isStringConstant(firstTokenChar)) {
                if (scanStringConstant(tokenChars, nextTokenChar) == SCAN_NEXT_TOKEN) {
                    return;
                }
                continue;
            }
        }
    }

    private boolean isNotStringConstant() {
        return !STRING_CONST.equals(tokenType);
    }

    private boolean isEndOfLineReached() {
        int nextPositionInLine = currentPositionInLine + 1;
        return (nextPositionInLine == endOfLinePosition);
    }

    private boolean isSymbol(String tokenChars) {
        return tokenChars.length() == 1;
    }

    private ScanMode scanSymbol(String tokenChars) {
        Symbol symbol = Symbol.fromString(tokenChars);
        if (symbol == null) {
            return CONTINUE_SCANNING_TOKEN;
        }
        setToken(SYMBOL, symbol);
        return SCAN_NEXT_TOKEN;
    }

    private boolean isIdentifierOrKeyword(String firstTokenChar, String tokenChars) {
        boolean startsWithLetterOrUnderscore = (LETTER_PATTERN.matcher(firstTokenChar).matches() || firstTokenChar.equals(UNDERSCORE));
        boolean containsLettersOrDigitsOrUnderscore = IDENTIFIER_PATTERN.matcher(tokenChars).matches();
        return startsWithLetterOrUnderscore && containsLettersOrDigitsOrUnderscore;
    }

    private ScanMode scanIdentifierOrKeyword(String tokenChars, String nextTokenChar) {
        identifier = tokenChars;
        if (!isSpaceOrSymbol(nextTokenChar)) {
            return CONTINUE_SCANNING_TOKEN;
        }

        Keyword keyword = Keyword.fromString(tokenChars);
        if (keyword != null) {
            setToken(KEYWORD, keyword);
        }
        else {
            setToken(IDENTIFIER, identifier);
        }
        return SCAN_NEXT_TOKEN;
    }

    private boolean isIntegerConstant(String firstTokenChar) {
        return DIGIT_PATTERN.matcher(firstTokenChar).matches();
    }

    private ScanMode scanIntegerConstant(String tokenChars, String nextTokenChar) {
        intValue = Integer.valueOf(tokenChars);
        if (!isSpaceOrSymbol(nextTokenChar)) {
            return CONTINUE_SCANNING_TOKEN;
        }
        checkIfIntValueIsValid();
        setToken(INT_CONST, intValue);
        return SCAN_NEXT_TOKEN;
    }

    private boolean isStringConstant(String firstTokenChar) {
        return firstTokenChar.equals(DOUBLE_QUOTE);
    }

    private ScanMode scanStringConstant(String tokenChars, String nextTokenChar) {
        stringValue = tokenChars;
        tokenType = STRING_CONST;
        if (!nextTokenChar.equals(DOUBLE_QUOTE)) {
            return CONTINUE_SCANNING_TOKEN;
        }
        stringValue = stringValue.substring(1, stringValue.length());
        setToken(STRING_CONST, stringValue);
        ++currentPositionInLine;
        return SCAN_NEXT_TOKEN;
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

    enum ScanMode {
        SCAN_NEXT_JACK_FILE,
        SCAN_NEW_LINE,
        CONTINUE_SCANNING_CURRENT_LINE,
        SCAN_NEXT_TOKEN,
        CONTINUE_SCANNING_TOKEN
    }
}