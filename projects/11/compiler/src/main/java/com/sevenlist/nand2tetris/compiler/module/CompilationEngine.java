package com.sevenlist.nand2tetris.compiler.module;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.sevenlist.nand2tetris.compiler.module.Keyword.*;
import static com.sevenlist.nand2tetris.compiler.module.Kind.ARG;
import static com.sevenlist.nand2tetris.compiler.module.Kind.VAR;
import static com.sevenlist.nand2tetris.compiler.module.Segment.CONSTANT;
import static com.sevenlist.nand2tetris.compiler.module.Segment.TEMP;
import static com.sevenlist.nand2tetris.compiler.module.Symbol.*;
import static com.sevenlist.nand2tetris.compiler.module.TokenType.*;
import static com.sevenlist.nand2tetris.compiler.module.UnaryOperator.NEG;
import static com.sevenlist.nand2tetris.compiler.module.UnaryOperator.NOT;

// Work in progress. Terribly messy code, needs to be cleaned up.

/**
 * Uses Dijkstra's <a href="https://en.wikipedia.org/wiki/Shunting-yard_algorithm">Shunting Yard algorithm</a> to compile expressions.
 */
public class CompilationEngine {

    private static Set<Keyword> KEYWORD_CONSTANTS = Stream.of(TRUE, FALSE, NULL).collect(Collectors.toSet());

    private JackTokenizer tokenizer;
    private boolean tokenConsumed = true;
    private VMWriter vmWriter;

    private String className;
    private SymbolTable symbolTable = new SymbolTable();
    private Deque<Operator> operatorStack = new ArrayDeque<>();

    private int numberOfArguments;
    private int ifStatementInSubroutineCounter;
    private int whileStatementInSubroutineCounter;

    public CompilationEngine(File jackFile) {
        tokenizer = new JackTokenizer(jackFile);
        vmWriter = new VMWriter(new File(jackFile.getPath().replace(".jack", ".vm")));
    }

    public void compileClass() {
        try {
            consumeKeyword(CLASS);
            className = consumeIdentifier();
            consumeSymbol(LEFT_CURLY_BRACE);
            while (!isNextTokenTheSymbol(RIGHT_CURLY_BRACE)) {
                compileClassVarDec();
                compileSubroutine();
            }
            consumeSymbol(RIGHT_CURLY_BRACE);
        }
        finally {
            vmWriter.close();
            // Hack: Process the tokens up to the end to (automatically) close the
            // generated *T.xml file.
            while (tokenizer.hasMoreTokens()) {
            }
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
        symbolTable.startSubroutine();
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
        compileSubroutineBody(subroutineName);
    }

    private void compileParameterList() {
        if (!isNextTokenOneOfKeywords(INT, CHAR, BOOLEAN) && !isTokenOfType(IDENTIFIER)) {
            return;
        }
        String paramType = consumeJackType();
        String paramName = consumeIdentifier();
        symbolTable.define(paramName, paramType, ARG);
        while (isNextTokenTheSymbol(COMMA)) {
            consumeSymbol(COMMA);
            paramType = consumeJackType();
            paramName = consumeIdentifier();
            symbolTable.define(paramName, paramType, ARG);
        }
    }

    private void compileVarDec() {
        if (!isNextTokenOneOfKeywords(Keyword.VAR)) {
            return;
        }
        consumeKeyword(Keyword.VAR);
        String varType = consumeJackType();
        String varName = consumeIdentifier();
        symbolTable.define(varName, varType, VAR);
        while (isNextTokenTheSymbol(COMMA)) {
            consumeSymbol(COMMA);
            varName = consumeIdentifier();
            symbolTable.define(varName, varType, VAR);
        }
        consumeSymbol(SEMICOLON);
    }

    private void compileStatements(boolean nested) {
        while (true) {
            if (isNextTokenOneOfKeywords(LET)) {
                compileLet();
            }
            else if (isNextTokenOneOfKeywords(IF)) {
                compileIf(nested);
            }
            else if (isNextTokenOneOfKeywords(WHILE)) {
                compileWhile(nested);
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
        vmWriter.writePop(TEMP, 0);
        consumeSymbol(SEMICOLON);
    }

    private void compileLet() {
        consumeKeyword(LET);
        String varName = consumeIdentifier();
        if (isNextTokenTheSymbol(LEFT_SQUARE_BRACKET)) {
            consumeSymbol(LEFT_SQUARE_BRACKET);
            compileExpression();
            consumeSymbol(RIGHT_SQUARE_BRACKET);
        }
        consumeSymbol(EQUAL_SIGN);
        compileExpression();
        consumeSymbol(SEMICOLON);
        vmWriter.writePop(symbolTable.kindOf(varName).segment(), symbolTable.indexOf(varName));
    }

    private void compileWhile(boolean asNestedWhileStatement) {
        int labelNumber = ++whileStatementInSubroutineCounter - 1;
        consumeKeyword(WHILE);
        consumeSymbol(LEFT_PARENTHESIS);
        vmWriter.writeLabel("WHILE_EXP" + labelNumber);
        compileExpression();
        consumeSymbol(RIGHT_PARENTHESIS);
        vmWriter.writeArithmetic(NOT);
        vmWriter.writeIf("WHILE_END" + labelNumber);
        consumeStatementBlock(true);
        vmWriter.writeGoto("WHILE_EXP" + labelNumber);
        vmWriter.writeLabel("WHILE_END" + labelNumber);
        if (asNestedWhileStatement) {
            --whileStatementInSubroutineCounter;
        }
    }

    private void compileReturn() {
        consumeKeyword(RETURN);
        if (!isNextTokenTheSymbol(SEMICOLON)) {
            compileExpression();
        }
        else {
            vmWriter.writePush(CONSTANT, 0);
        }
        consumeSymbol(SEMICOLON);
        vmWriter.writeReturn();
    }

    private void compileIf(boolean asNestedIfStatement) {
        int labelNumber = ++ifStatementInSubroutineCounter - 1;
        consumeKeyword(IF);
        consumeSymbol(LEFT_PARENTHESIS);
        compileExpression();
        consumeSymbol(RIGHT_PARENTHESIS);
        vmWriter.writeIf("IF_TRUE" + labelNumber);
        vmWriter.writeGoto("IF_FALSE" + labelNumber);
        vmWriter.writeLabel("IF_TRUE" + labelNumber);
        consumeStatementBlock(true);
        vmWriter.writeGoto("IF_END" + labelNumber);
        vmWriter.writeLabel("IF_FALSE" + labelNumber);
        if (isNextTokenOneOfKeywords(ELSE)) {
            consumeKeyword(ELSE);
            consumeStatementBlock(true);
        }
        vmWriter.writeLabel("IF_END" + labelNumber);
        if (asNestedIfStatement) {
            --ifStatementInSubroutineCounter;
        }
    }

    private void compileExpression() {
        compileExpressionPoppingOperatorsOnStack(true);
    }

    private void compileGroupedExpression() {
        compileExpressionPoppingOperatorsOnStack(false);
    }

    private void compileExpressionPoppingOperatorsOnStack(boolean popOperatorsOnStack) {
        compileTerm();
        while (isNextTokenABinaryOperator()) {
            consumeBinaryOperator();
            compileTerm();
            if (popOperatorsOnStack) {
                compileOperatorsOnStack();
            }
        }
        if (popOperatorsOnStack) {
            compileOperatorsOnStack();
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
        else if (isTokenAnObjectReferenceOrAConstant()) {
            if (isNextTokenOneOfKeywords(THIS)) {
                // TODO
                tokenConsumed();
            }
            else {
                compileKeywordConstant();
            }
        }
        else if (isTokenOfType(IDENTIFIER)) {
            String identifier = consumeIdentifier(); // varName or subroutineCall's subroutineName|className|varName
            int identifierIndex = symbolTable.indexOf(identifier);
            if (identifierIndex > -1) {
                vmWriter.writePush(symbolTable.kindOf(identifier).segment(), identifierIndex);
            }
            else if (isNextTokenTheSymbol(LEFT_SQUARE_BRACKET)) { // varName[...]
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
                compileGroupedExpression();
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

    private Keyword consumeKeyword(Keyword expectedKeyword, Keyword... otherPossibleKeywords) {
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
        return keyword;
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

    private String consumeJackType() {
        if (isTokenOfType(KEYWORD)) {
            Keyword primitiveType = consumeKeyword(INT, CHAR, BOOLEAN);
            return primitiveType.toString();
        }
        else if (isTokenOfType(IDENTIFIER)) {
            String className = consumeIdentifier();
            return className;
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

    private void compileSubroutineBody(String subroutineName) {
        consumeSymbol(LEFT_CURLY_BRACE);
        while (isNextTokenOneOfKeywords(Keyword.VAR)) {
            compileVarDec();
        }
        vmWriter.writeFunction(createFunctionName(subroutineName), symbolTable.varCount(VAR));
        ifStatementInSubroutineCounter = 0;
        whileStatementInSubroutineCounter = 0;
        compileStatements(false);
        consumeSymbol(RIGHT_CURLY_BRACE);
    }

    private void consumeStatementBlock(boolean nested) {
        consumeSymbol(LEFT_CURLY_BRACE);
        compileStatements(nested);
        consumeSymbol(RIGHT_CURLY_BRACE);
    }

    private boolean isNextTokenABinaryOperator() {
        nextTokenIfPreviousHasBeenConsumed();
        return isTokenOfType(SYMBOL) && BinaryOperator.isOperator(tokenizer.symbol());
    }

    private void compileSubroutineCall(Optional<String> subroutineNameOrClassNameOrVarName) {
        String subroutineName = subroutineNameOrClassNameOrVarName.orElseGet(() -> consumeIdentifier());
        if (isNextTokenTheSymbol(LEFT_PARENTHESIS)) { // with subroutineName left of parenthesis
            consumeExpressionListWithParenthesis();
        }
        else if (isNextTokenTheSymbol(DOT)) { // with className or varName left of dot
            consumeSymbol(DOT);
            subroutineName += DOT + consumeIdentifier();
            consumeExpressionListWithParenthesis();
        }
        vmWriter.writeCall(subroutineName, numberOfArguments);
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
        vmWriter.writePush(CONSTANT, Math.abs(tokenizer.intVal()));
        if (tokenizer.intVal() < 0) {
            vmWriter.writeArithmetic(NEG);
        }
        tokenConsumed();
    }

    private boolean isTokenAnObjectReferenceOrAConstant() {
        return isNextTokenOneOfKeywords(THIS)
                || (isTokenOfType(KEYWORD) && KEYWORD_CONSTANTS.contains(tokenizer.keyword()));
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

    private void compileKeywordConstant() {
        vmWriter.writePush(CONSTANT, 0);
        if (isNextTokenOneOfKeywords(TRUE)) {
            // true == -1, i.e. ~(0000 0000) = 1111 1111
            vmWriter.writeArithmetic(NOT);
        }
        tokenConsumed();
    }
}
