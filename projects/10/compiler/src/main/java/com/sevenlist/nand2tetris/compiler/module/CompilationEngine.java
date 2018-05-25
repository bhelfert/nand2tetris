package com.sevenlist.nand2tetris.compiler.module;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.sevenlist.nand2tetris.compiler.module.Keyword.*;
import static com.sevenlist.nand2tetris.compiler.module.Symbol.*;
import static com.sevenlist.nand2tetris.compiler.module.TokenType.*;

// Needs refactoring.
public class CompilationEngine {

    private static Set<Keyword> KEYWORD_CONSTANTS = Stream.of(TRUE, FALSE, NULL, THIS).collect(Collectors.toSet());
    private static Set<Symbol> OPERATORS = Stream.of(PLUS, MINUS, ASTERISK, SLASH, AMPERSAND, PIPE, LESS_THAN, GREATER_THAN, EQUAL_SIGN).collect(Collectors.toSet());
    private static Set<Symbol> UNARY_OPERATORS = Stream.of(MINUS, TILDE).collect(Collectors.toSet());

    private final File jackFile;
    private JackTokenizer tokenizer;
    private BufferedWriter structuredCodeWriter;
    private boolean tokenConsumed = true;
    private int indentLevel = 0;

    public CompilationEngine(File jackFile) {
        this.jackFile = jackFile;
        tokenizer = new JackTokenizer(jackFile);
    }

    public void compileClass() {
        openStructuredCodeFile();
        consumeKeyword(CLASS);
        consumeIdentifier(); // className
        consumeSymbol(LEFT_CURLY_BRACE);
        while (!isNextTokenTheSymbol(RIGHT_CURLY_BRACE)) {
            compileClassVarDec();
            compileSubroutine();
        }
        consumeSymbol(RIGHT_CURLY_BRACE);
        closeStructuredCodeFile();

        // Hack: Process the tokens up to the end to (automatically) close the
        // generated *T.xml file.
        while (tokenizer.hasMoreTokens()) {}
    }

    public void compileClassVarDec() {
        if (!isNextTokenOneOfKeywords(STATIC, FIELD)) {
            return;
        }
        encloseWithXmlTag("classVarDec", () -> {
            consumeKeyword(STATIC, FIELD);
            consumeJackType();
            consumeIdentifier(); // varName
            while (!isNextTokenTheSymbol(SEMICOLON)) {
                consumeSymbol(COMMA);
                consumeIdentifier(); // varName
            }
            consumeSymbol(SEMICOLON);
        });
    }

    public void compileSubroutine() {
        if (!isNextTokenOneOfKeywords(CONSTRUCTOR, FUNCTION, METHOD)) {
            return;
        }
        encloseWithXmlTag("subroutineDec", () -> {
            consumeKeyword(CONSTRUCTOR, FUNCTION, METHOD);
            if (isTokenOfType(KEYWORD) && tokenizer.keyword() == VOID) {
                consumeKeyword(VOID);
            }
            else {
                consumeJackType();
            }
            consumeIdentifier(); // subroutineName
            consumeSymbol(LEFT_PARENTHESIS);
            compileParameterList();
            consumeSymbol(RIGHT_PARENTHESIS);
            consumeSubroutineBody();
        });
    }

    public void compileParameterList() {
        writeXmlStartTag("parameterList");
        if (!isNextTokenOneOfKeywords(INT, CHAR, BOOLEAN) && !isTokenOfType(IDENTIFIER)) {
            writeXmlEndTag("parameterList");
            return;
        }
        consumeJackType();
        consumeIdentifier(); // varName
        while (isNextTokenTheSymbol(COMMA)) {
            consumeSymbol(COMMA);
            consumeJackType();
            consumeIdentifier(); // varName
        }
        writeXmlEndTag("parameterList");
    }

    public void compileVarDec() {
        if (!isNextTokenOneOfKeywords(VAR)) {
            return;
        }
        encloseWithXmlTag("varDec", () -> {
            consumeKeyword(VAR);
            consumeJackType();
            consumeIdentifier(); // varName
            while (isNextTokenTheSymbol(COMMA)) {
                consumeSymbol(COMMA);
                consumeIdentifier(); // varName
            }
            consumeSymbol(SEMICOLON);
        });
    }

    public void compileStatements() {
        encloseWithXmlTag("statements", () -> {
            while (true) {
                if (isNextTokenOneOfKeywords(LET)) {
                    compileLet();
                }
                else if (isNextTokenOneOfKeywords(IF)) {
                    compileIf();
                }
                else if (isNextTokenOneOfKeywords(WHILE)) {
                    compileWhile();
                }
                else if (isNextTokenOneOfKeywords(DO)) {
                    compileDo();
                }
                else if (isNextTokenOneOfKeywords(RETURN)) {
                    compileReturn();
                }
                else {
                    break;
                }
            }
        });
    }

    public void compileDo() {
        encloseWithXmlTag("doStatement", () -> {
            consumeKeyword(DO);
            consumeSubroutineCall(Optional.empty());
            consumeSymbol(SEMICOLON);
        });
    }

    public void compileLet() {
        encloseWithXmlTag("letStatement", () -> {
            consumeKeyword(LET);
            consumeIdentifier(); // varName
            if (isNextTokenTheSymbol(LEFT_SQUARE_BRACKET)) {
                consumeSymbol(LEFT_SQUARE_BRACKET);
                compileExpression();
                consumeSymbol(RIGHT_SQUARE_BRACKET);
            }
            consumeSymbol(EQUAL_SIGN);
            compileExpression();
            consumeSymbol(SEMICOLON);
        });
    }

    public void compileWhile() {
        encloseWithXmlTag("whileStatement", () -> {
            consumeKeyword(WHILE);
            consumeSymbol(LEFT_PARENTHESIS);
            compileExpression();
            consumeSymbol(RIGHT_PARENTHESIS);
            consumeStatementBlock();
        });
    }

    public void compileReturn() {
        encloseWithXmlTag("returnStatement", () -> {
            consumeKeyword(RETURN);
            if (!isNextTokenTheSymbol(SEMICOLON)) {
                compileExpression();
            }
            consumeSymbol(SEMICOLON);
        });
    }

    public void compileIf() {
        encloseWithXmlTag("ifStatement", () -> {
            consumeKeyword(IF);
            consumeSymbol(LEFT_PARENTHESIS);
            compileExpression();
            consumeSymbol(RIGHT_PARENTHESIS);
            consumeStatementBlock();
            if (isNextTokenOneOfKeywords(ELSE)) {
                consumeKeyword(ELSE);
                consumeStatementBlock();
            }
        });
    }

    public void compileExpression() {
        encloseWithXmlTag("expression", () -> {
            compileTerm();
            while (isNextTokenAnOperator()) {
                consumeSymbol(tokenizer.symbol());
                compileTerm();
            }
        });
    }

    public void compileTerm() {
        encloseWithXmlTag("term", () -> {
            nextTokenIfPreviousHasBeenConsumed();
            if (tokenizer.tokenType() == INT_CONST) {
                writeXmlElement(INT_CONST, tokenizer.intVal());
                tokenConsumed();
            }
            else if (tokenizer.tokenType() == STRING_CONST) {
                writeXmlElement(STRING_CONST, tokenizer.stringVal());
                tokenConsumed();
            }
            else if (tokenizer.tokenType() == KEYWORD && KEYWORD_CONSTANTS.contains(tokenizer.keyword())) {
                writeXmlElement(KEYWORD, tokenizer.keyword());
                tokenConsumed();
            }
            else if (tokenizer.tokenType() == IDENTIFIER) {
                String identifier = consumeIdentifier(); // varName or subroutineCall's subroutineName|className|varName
                if (isNextTokenTheSymbol(LEFT_SQUARE_BRACKET)) { // varName[...]
                    consumeSymbol(LEFT_SQUARE_BRACKET);
                    compileExpression();
                    consumeSymbol(RIGHT_SQUARE_BRACKET);
                }
                else {
                    consumeSubroutineCall(Optional.of(identifier));
                }
            }
            else if (tokenizer.tokenType() == SYMBOL) {
                if (tokenizer.symbol() == LEFT_PARENTHESIS) {
                    consumeSymbol(LEFT_PARENTHESIS);
                    compileExpression();
                    consumeSymbol(RIGHT_PARENTHESIS);
                }
                else if (UNARY_OPERATORS.contains(tokenizer.symbol())) {
                    consumeSymbol(tokenizer.symbol());
                    compileTerm();
                }
            }
        });
    }

    public void compileExpressionList() {
        encloseWithXmlTag("expressionList", () -> {
            if (isNextTokenTheSymbol(RIGHT_PARENTHESIS)) {
                return;
            }
            compileExpression();
            while (isNextTokenTheSymbol(COMMA)) {
                consumeSymbol(COMMA);
                compileExpression();
            }
        });
    }

    private void openStructuredCodeFile() {
        structuredCodeWriter = createStructuredCodeWriter(jackFile);
        writeXmlStartTag("class");
    }

    private void closeStructuredCodeFile() {
        writeXmlEndTag("class");
        closeStructuredCodeWriter();
    }

    private BufferedWriter createStructuredCodeWriter(File jackFile) {
        File structuredCodeFile = new File(jackFile.getPath().replace(".jack", ".xml"));
        try {
            return new BufferedWriter(new FileWriter(structuredCodeFile));
        }
        catch (IOException e) {
            throw new RuntimeException("File [" + structuredCodeFile + "] cannot be created", e);
        }
    }

    private void writeLine(String line) {
        try {
            structuredCodeWriter.write(line);
            structuredCodeWriter.newLine();
        }
        catch (IOException e) {
            closeStructuredCodeWriter();
            throw new RuntimeException("Could not write line [" + line + "] in .xml file", e);
        }
    }

    private void closeStructuredCodeWriter() {
        try {
            structuredCodeWriter.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeXmlStartTag(String tagName) {
        writeXmlTag(tagName, false);
        ++indentLevel;
    }

    private void writeXmlEndTag(String tagName) {
        --indentLevel;
        writeXmlTag(tagName, true);
    }

    private void writeXmlTag(String tagName, boolean closingTag) {
        String spaces = (indentLevel > 0) ? String.format("%" + (indentLevel * 2) + "s", "") : "";
        writeLine(spaces + "<" + (closingTag ? "/" : "") + tagName + ">");
    }

    private void writeXmlElement(TokenType elementTag, Object elementText) {
        String elementTextAsString = elementTag.equals(SYMBOL) ? ((Symbol) elementText).toEscapeString() : elementText.toString();
        String spaces = String.format("%" + (indentLevel * 2) + "s", "");
        writeLine(spaces + "<" + elementTag + "> " + elementTextAsString + " </" + elementTag + ">");
    }

    private boolean isNextTokenOneOfKeywords(Keyword... keywords) {
        nextTokenIfPreviousHasBeenConsumed();
        if (tokenizer.tokenType() != KEYWORD) {
            return false;
        }
        for (Keyword keyword : keywords) {
            if (tokenizer.keyword() == keyword) {
                return true;
            }
        }
        return false;
    }

    private void consumeKeyword(Keyword expectedKeyword, Keyword... otherPossibleKeywords) {
        nextTokenIfPreviousHasBeenConsumed();
        Keyword keyword = tokenizer.keyword();
        if (keyword != expectedKeyword) {
            boolean found = false;
            for (Keyword otherPossibleKeyword : otherPossibleKeywords) {
                if (keyword == otherPossibleKeyword) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                List<Keyword> keywords = new ArrayList<>();
                keywords.add(expectedKeyword);
                keywords.addAll(Arrays.asList(otherPossibleKeywords));
                throw new RuntimeException("Expected one of the keywords " + keywords + " while parsing, but got keyword [" + keyword + "]");
            }
        }
        writeKeywordXmlElement();
        tokenConsumed();
    }

    private void writeKeywordXmlElement() {
        writeXmlElement(KEYWORD, tokenizer.keyword());
    }

    private String consumeIdentifier() {
        nextTokenIfPreviousHasBeenConsumed();
        writeIdentifierXmlElement();
        tokenConsumed();
        return tokenizer.identifier();
    }

    private void writeIdentifierXmlElement() {
        writeXmlElement(IDENTIFIER, tokenizer.identifier());
    }

    private void consumeSymbol(Symbol expectedSymbol) {
        nextTokenIfPreviousHasBeenConsumed();
        Symbol symbol = tokenizer.symbol();
        if (symbol != expectedSymbol) {
            throw new RuntimeException("Expected symbol [" + expectedSymbol + "] while parsing, but got symbol [" + symbol + "]");
        }
        writeXmlElement(SYMBOL, symbol);
        tokenConsumed();
    }

    private void nextTokenIfPreviousHasBeenConsumed() {
        if (!tokenConsumed) {
            return;
        }
        while (tokenizer.hasMoreTokens()) {
            tokenizer.advance();
            if (tokenizer.tokenType() != COMMENT_LINE_OR_EMPTY_LINE) {
                break;
            }
        }
        tokenConsumed = false;
    }

    private void tokenConsumed() {
        tokenConsumed = true;
    }

    private void consumeJackType() {
        if (isTokenOfType(KEYWORD)) {
            consumeKeyword(INT, CHAR, BOOLEAN);
        }
        else if (isTokenOfType(IDENTIFIER)) {
            consumeIdentifier(); // className
        }
        else {
            throw new RuntimeException("Expected token to be of type keyword or identifier, but got token of type [" + tokenizer.tokenType() + "]");
        }
    }

    private boolean isTokenOfType(TokenType tokenType) {
        nextTokenIfPreviousHasBeenConsumed();
        return tokenizer.tokenType() == tokenType;
    }

    private boolean isNextTokenTheSymbol(Symbol symbol) {
        nextTokenIfPreviousHasBeenConsumed();
        return (tokenizer.tokenType() == SYMBOL) && (tokenizer.symbol() == symbol);
    }

    private void consumeSubroutineBody() {
        encloseWithXmlTag("subroutineBody", () -> {
            consumeSymbol(LEFT_CURLY_BRACE);
            while (isNextTokenOneOfKeywords(VAR)) {
                compileVarDec();
            }
            compileStatements();
            consumeSymbol(RIGHT_CURLY_BRACE);
        });
    }

    private void consumeStatementBlock() {
        consumeSymbol(LEFT_CURLY_BRACE);
        compileStatements();
        consumeSymbol(RIGHT_CURLY_BRACE);
    }

    private boolean isNextTokenAnOperator() {
        nextTokenIfPreviousHasBeenConsumed();
        return (tokenizer.tokenType() == SYMBOL) && OPERATORS.contains(tokenizer.symbol());
    }

    private void encloseWithXmlTag(String tagName, Runnable code) {
        writeXmlStartTag(tagName);
        code.run();
        writeXmlEndTag(tagName);
    }

    private void consumeSubroutineCall(Optional<String> subroutineNameOrClassNameOrVarName) {
        if (!subroutineNameOrClassNameOrVarName.isPresent()) {
            consumeIdentifier();
        }
        if (isNextTokenTheSymbol(LEFT_PARENTHESIS)) { // with subroutineName left of parenthesis
            consumeExpressionListWithParenthesis();
        }
        else if (isNextTokenTheSymbol(DOT)) { // with className or varName left of dot
            consumeSymbol(DOT);
            consumeIdentifier(); // subroutineName
            consumeExpressionListWithParenthesis();
        }
    }

    private void consumeExpressionListWithParenthesis() {
        consumeSymbol(LEFT_PARENTHESIS);
        compileExpressionList();
        consumeSymbol(RIGHT_PARENTHESIS);
    }
}
