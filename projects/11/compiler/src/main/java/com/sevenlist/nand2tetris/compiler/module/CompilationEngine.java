package com.sevenlist.nand2tetris.compiler.module;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.sevenlist.nand2tetris.compiler.module.Keyword.*;
import static com.sevenlist.nand2tetris.compiler.module.Segment.CONSTANT;
import static com.sevenlist.nand2tetris.compiler.module.Segment.TEMP;
import static com.sevenlist.nand2tetris.compiler.module.Symbol.*;
import static com.sevenlist.nand2tetris.compiler.module.TokenType.*;

// Work in progress. Awful code, needs refactoring.
public class CompilationEngine {

    private static Set<Keyword> KEYWORD_CONSTANTS = Stream.of(TRUE, FALSE, NULL, THIS).collect(Collectors.toSet());

    private JackTokenizer tokenizer;
    private boolean tokenConsumed = true;
    private VMWriter vmWriter;

    private String className;
    private Deque<Operator> operatorStack = new ArrayDeque<>();
    private int numberOfArguments = 0;

    public CompilationEngine(File jackFile) {
        tokenizer = new JackTokenizer(jackFile);
        vmWriter = new VMWriter(new File(jackFile.getPath().replace(".jack", ".vm")));
    }

    public void compileClass() {
        consumeKeyword(CLASS);
        className = consumeIdentifier();
        consumeSymbol(LEFT_CURLY_BRACE);
        while (!isNextTokenTheSymbol(RIGHT_CURLY_BRACE)) {
            compileClassVarDec();
            compileSubroutine();
        }
        consumeSymbol(RIGHT_CURLY_BRACE);

        vmWriter.close();
        // Hack: Process the tokens up to the end to (automatically) close the
        // generated *T.xml file.
        while (tokenizer.hasMoreTokens()) {
        }
    }

    private void compileClassVarDec() {
        if (!isNextTokenOneOfKeywords(STATIC, FIELD)) {
            return;
        }
        consumeKeyword(STATIC, FIELD);
        consumeJackType();
        consumeIdentifier(); // varName
        while (!isNextTokenTheSymbol(SEMICOLON)) {
            consumeSymbol(COMMA);
            consumeIdentifier(); // varName
        }
        consumeSymbol(SEMICOLON);
    }

    private void compileSubroutine() {
        if (!isNextTokenOneOfKeywords(CONSTRUCTOR, FUNCTION, METHOD)) {
            return;
        }
        consumeKeyword(CONSTRUCTOR, FUNCTION, METHOD);
        if (isTokenOfType(KEYWORD) && tokenizer.keyword() == VOID) {
            consumeKeyword(VOID);
        }
        else {
            consumeJackType();
        }
        String subroutineName = consumeIdentifier();
        consumeSymbol(LEFT_PARENTHESIS);
        compileParameterList();
        consumeSymbol(RIGHT_PARENTHESIS);
        vmWriter.writeFunction(createFunctionName(subroutineName), 0);
        consumeSubroutineBody();
    }

    private void compileParameterList() {
        if (!isNextTokenOneOfKeywords(INT, CHAR, BOOLEAN) && !isTokenOfType(IDENTIFIER)) {
            return;
        }
        consumeJackType();
        consumeIdentifier(); // varName
        while (isNextTokenTheSymbol(COMMA)) {
            consumeSymbol(COMMA);
            consumeJackType();
            consumeIdentifier(); // varName
        }
    }

    private void compileVarDec() {
        if (!isNextTokenOneOfKeywords(VAR)) {
            return;
        }
        consumeKeyword(VAR);
        consumeJackType();
        consumeIdentifier(); // varName
        while (isNextTokenTheSymbol(COMMA)) {
            consumeSymbol(COMMA);
            consumeIdentifier(); // varName
        }
        consumeSymbol(SEMICOLON);
    }

    private void compileStatements() {
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
    }

    private void compileDo() {
        consumeKeyword(DO);
        compileSubroutineCall(Optional.empty());
        consumeSymbol(SEMICOLON);
    }

    private void compileLet() {
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
        compileOperatorsOnStack();
    }

    private void compileWhile() {
        consumeKeyword(WHILE);
        consumeSymbol(LEFT_PARENTHESIS);
        compileExpression();
        consumeSymbol(RIGHT_PARENTHESIS);
        consumeStatementBlock();
        compileOperatorsOnStack();
    }

    private void compileReturn() {
        consumeKeyword(RETURN);
        if (!isNextTokenTheSymbol(SEMICOLON)) {
            compileExpression();
            compileOperatorsOnStack();
        }
        else {
            vmWriter.writePush(CONSTANT, 0);
        }
        consumeSymbol(SEMICOLON);
        vmWriter.writeReturn();
    }

    private void compileIf() {
        consumeKeyword(IF);
        consumeSymbol(LEFT_PARENTHESIS);
        compileExpression();
        consumeSymbol(RIGHT_PARENTHESIS);
        consumeStatementBlock();
        if (isNextTokenOneOfKeywords(ELSE)) {
            consumeKeyword(ELSE);
            consumeStatementBlock();
        }
        compileOperatorsOnStack();
    }

    private void compileExpression() {
        compileTerm();
        while (isNextTokenABinaryOperator()) {
            consumeBinaryOperator();
            compileTerm();
        }
    }

    private void compileTerm() {
        nextTokenIfPreviousHasBeenConsumed();
        if (isTokenOfType(INT_CONST)) {
            compileIntegerConstant();
        }
        else if (isTokenOfType(STRING_CONST)) {
            tokenConsumed();
        }
        else if (isTokenOneOfKeywordConstants()) {
            tokenConsumed();
        }
        else if (isTokenOfType(IDENTIFIER)) {
            String identifier = consumeIdentifier(); // varName or subroutineCall's subroutineName|className|varName
            if (isNextTokenTheSymbol(LEFT_SQUARE_BRACKET)) { // varName[...]
                consumeSymbol(LEFT_SQUARE_BRACKET);
                compileExpression();
                consumeSymbol(RIGHT_SQUARE_BRACKET);
            }
            else {
                compileSubroutineCall(Optional.of(identifier));
            }
        }
        else if (isTokenOfType(SYMBOL)) {
            if (tokenizer.symbol() == LEFT_PARENTHESIS) {
                operatorStack.push(GroupingOperator.LEFT_PARENTHESIS);
                consumeSymbol(LEFT_PARENTHESIS);
                compileExpression();
                while (operatorStack.contains(GroupingOperator.LEFT_PARENTHESIS)) {
                    Operator operator = operatorStack.pop();
                    if (operator == GroupingOperator.LEFT_PARENTHESIS) {
                        break;
                    }
                    vmWriter.writeArithmetic(operator);
                }
                consumeSymbol(RIGHT_PARENTHESIS);
            }
            else if (UnaryOperator.isOperator(tokenizer.symbol())) {
                compileUnaryOperator();
                compileTerm();
            }
        }
    }

    private void compileExpressionList() {
        if (isNextTokenTheSymbol(RIGHT_PARENTHESIS)) {
            return;
        }
        compileExpression();
        ++numberOfArguments;
        while (isNextTokenTheSymbol(COMMA)) {
            consumeSymbol(COMMA);
            compileExpression();
            ++numberOfArguments;
        }
        compileOperatorsOnStack();
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
        tokenConsumed();
    }

    private String consumeIdentifier() {
        nextTokenIfPreviousHasBeenConsumed();
        tokenConsumed();
        return tokenizer.identifier();
    }

    private void consumeSymbol(Symbol expectedSymbol) {
        nextTokenIfPreviousHasBeenConsumed();
        Symbol symbol = tokenizer.symbol();
        if (symbol != expectedSymbol) {
            throw new RuntimeException("Expected symbol [" + expectedSymbol + "] while parsing, but got symbol [" + symbol + "]");
        }
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
        consumeSymbol(LEFT_CURLY_BRACE);
        while (isNextTokenOneOfKeywords(VAR)) {
            compileVarDec();
        }
        compileStatements();
        consumeSymbol(RIGHT_CURLY_BRACE);
    }

    private void consumeStatementBlock() {
        consumeSymbol(LEFT_CURLY_BRACE);
        compileStatements();
        consumeSymbol(RIGHT_CURLY_BRACE);
    }

    private boolean isNextTokenABinaryOperator() {
        nextTokenIfPreviousHasBeenConsumed();
        return (tokenizer.tokenType() == SYMBOL) && BinaryOperator.isOperator(tokenizer.symbol());
    }

    private void compileSubroutineCall(Optional<String> subroutineNameOrClassNameOrVarName) {
        String subroutineName = subroutineNameOrClassNameOrVarName.orElse(consumeIdentifier());
        if (isNextTokenTheSymbol(LEFT_PARENTHESIS)) { // with subroutineName left of parenthesis
            consumeExpressionListWithParenthesis();
        }
        else if (isNextTokenTheSymbol(DOT)) { // with className or varName left of dot
            consumeSymbol(DOT);
            subroutineName += DOT + consumeIdentifier();
            consumeExpressionListWithParenthesis();
        }
        vmWriter.writeCall(subroutineName, numberOfArguments);
        vmWriter.writePop(TEMP, 0);
        numberOfArguments = 0;
    }

    private void consumeExpressionListWithParenthesis() {
        consumeSymbol(LEFT_PARENTHESIS);
        compileExpressionList();
        consumeSymbol(RIGHT_PARENTHESIS);
    }

    private String createFunctionName(String subroutineName) {
        return className + "." + subroutineName;
    }

    private void consumeBinaryOperator() {
        Symbol symbol = tokenizer.symbol();
        operatorStack.push(BinaryOperator.fromSymbol(symbol));
        consumeSymbol(symbol);
    }

    private void compileIntegerConstant() {
        vmWriter.writePush(CONSTANT, tokenizer.intVal());
        tokenConsumed();
    }

    private boolean isTokenOneOfKeywordConstants() {
        return isTokenOfType(KEYWORD) && KEYWORD_CONSTANTS.contains(tokenizer.keyword());
    }

    private void compileUnaryOperator() {
        Symbol symbol = tokenizer.symbol();
        operatorStack.push(UnaryOperator.fromSymbol(symbol));
        consumeSymbol(symbol);
    }

    private void compileOperatorsOnStack() {
        while (!operatorStack.isEmpty()) {
            Operator operator = operatorStack.pop();
            vmWriter.writeArithmetic(operator);
        }
    }
}
