package de.bhelfert.nand2tetris.compiler.module;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.bhelfert.nand2tetris.compiler.module.BinaryOperator.ADD;

// Messy code, needs clean up.

/**
 * Uses Dijkstra's <a href="https://en.wikipedia.org/wiki/Shunting-yard_algorithm">Shunting Yard algorithm</a> to compile expressions.
 */
public class CompilationEngine {

    private static Set<Keyword> KEYWORD_CONSTANTS = Stream.of(TRUE, FALSE, NULL).collect(Collectors.toSet());
    private static final int AFTER_PREFIX_LEFT_INDEX = 4;

    private JackTokenizer tokenizer;
    private boolean tokenConsumed = true;
    private VMWriter vmWriter;

    private String className;
    private SymbolTable symbolTable = new SymbolTable();
    private Deque<Operator> operatorStack = new ArrayDeque<>();

    private boolean methodBody;
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
            closeTokenizerAndVmWriter();
        }
    }

    private Keyword consumeKeyword(Keyword expectedKeyword, Keyword... otherPossibleKeywords) {
        nextTokenIfPreviousHasBeenConsumed();
        Keyword keyword = tokenizer.keyword();
        if (keyword != expectedKeyword) {
            boolean expectedKeywordFound = Arrays.asList(otherPossibleKeywords).contains(keyword);
            if (!expectedKeywordFound) {
                throw new RuntimeException("Got unexpected keyword [" + keyword + "] while parsing");
            }
        }
        tokenConsumed();
        return keyword;
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

    private boolean isNextTokenTheSymbol(Symbol symbol) {
        return isTokenOfType(SYMBOL) && (tokenizer.symbol() == symbol);
    }

    private boolean isTokenOfType(TokenType tokenType) {
        nextTokenIfPreviousHasBeenConsumed();
        return tokenizer.tokenType() == tokenType;
    }

    private void compileClassVarDec() {
        if (!isNextTokenOneOfKeywords(Keyword.STATIC, FIELD)) {
            return;
        }
        Keyword keyword = consumeKeyword(Keyword.STATIC, FIELD);
        Kind kind = Kind.valueOf(keyword.name());
        String classVarType = consumeJackType();
        String classVarName = consumeIdentifier();
        addIdentifierInformation(classVarName, classVarType, kind);
        while (!isNextTokenTheSymbol(SEMICOLON)) {
            consumeSymbol(COMMA);
            classVarName = consumeIdentifier();
            addIdentifierInformation(classVarName, classVarType, kind);
        }
        consumeSymbol(SEMICOLON);
    }

    private boolean isNextTokenOneOfKeywords(Keyword... keywords) {
        if (!isTokenOfType(KEYWORD)) {
            return false;
        }
        return Arrays.asList(keywords).contains(tokenizer.keyword());
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
        throw new RuntimeException("Expected token of type keyword or identifier, but got token of type [" + tokenizer.tokenType() + "]");
    }

    private void addIdentifierInformation(String name, String type, Kind kind) {
        symbolTable.define(name, type, kind);
    }

    private void compileSubroutine() {
        if (!isNextTokenOneOfKeywords(CONSTRUCTOR, FUNCTION, METHOD)) {
            return;
        }
        symbolTable.startSubroutine();
        Keyword subroutineKeyword = consumeKeyword(CONSTRUCTOR, FUNCTION, METHOD);
        consumeReturnType();
        String subroutineName = consumeIdentifier();
        consumeSymbol(LEFT_PARENTHESIS);
        compileParameterList();
        consumeSymbol(RIGHT_PARENTHESIS);
        compileSubroutineBody(subroutineKeyword, subroutineName);
    }

    private void consumeReturnType() {
        if (isNextTokenOneOfKeywords(VOID)) {
            consumeKeyword(VOID);
        }
        else {
            consumeJackType();
        }
    }

    private void compileParameterList() {
        if (!isNextTokenAParameterType()) {
            return;
        }
        String paramType = consumeJackType();
        String paramName = consumeIdentifier();
        addIdentifierInformation(paramName, paramType, Kind.ARG);
        while (isNextTokenTheSymbol(COMMA)) {
            consumeSymbol(COMMA);
            paramType = consumeJackType();
            paramName = consumeIdentifier();
            addIdentifierInformation(paramName, paramType, Kind.ARG);
        }
    }

    private boolean isNextTokenAParameterType() {
        return isNextTokenOneOfKeywords(INT, CHAR, BOOLEAN) || isTokenOfType(IDENTIFIER);
    }

    private void compileSubroutineBody(Keyword subroutineKeyword, String subroutineName) {
        consumeSymbol(LEFT_CURLY_BRACE);
        while (isNextTokenOneOfKeywords(Keyword.VAR)) {
            compileVarDec();
        }
        writeVmFunction(subroutineKeyword, subroutineName);
        switch (subroutineKeyword) {
            case CONSTRUCTOR:
                createObject();
                break;

            case METHOD:
                methodBody = true;
                setThisPointerInMethod();
                break;
        }
        ifStatementInSubroutineCounter = 0;
        whileStatementInSubroutineCounter = 0;
        compileStatements();
        consumeSymbol(RIGHT_CURLY_BRACE);
        methodBody = false;
    }

    private void compileVarDec() {
        if (!isNextTokenOneOfKeywords(Keyword.VAR)) {
            return;
        }
        consumeKeyword(Keyword.VAR);
        String varType = consumeJackType();
        String varName = consumeIdentifier();
        addIdentifierInformation(varName, varType, Kind.VAR);
        while (isNextTokenTheSymbol(COMMA)) {
            consumeSymbol(COMMA);
            varName = consumeIdentifier();
            addIdentifierInformation(varName, varType, Kind.VAR);
        }
        consumeSymbol(SEMICOLON);
    }

    private void writeVmFunction(Keyword subroutineKeyword, String subroutineName) {
        int numberOfLocalVariables = (subroutineKeyword == CONSTRUCTOR) ? 0 : symbolTable.varCount(Kind.VAR);
        vmWriter.writeFunction(createFunctionName(subroutineName), numberOfLocalVariables);
    }

    private String createFunctionName(String subroutineName) {
        return className + "." + subroutineName;
    }

    private void createObject() {
        allocateMemoryForFields();
        setThisPointer();
    }

    private void allocateMemoryForFields() {
        vmWriter.writePush(CONSTANT, symbolTable.varCount(Kind.FIELD));
        vmWriter.writeCall("Memory.alloc", 1);
    }

    private void setThisPointer() {
        vmWriter.writePop(POINTER, 0);
    }

    private void setThisPointerInMethod() {
        vmWriter.writePush(ARGUMENT, 0);
        setThisPointer();
    }

    private void compileStatements() {
        while (isTokenOfType(KEYWORD)) {
            switch (tokenizer.keyword()) {
                case LET:
                    compileLet();
                    break;

                case IF:
                    compileIf();
                    break;

                case WHILE:
                    compileWhile();
                    break;

                case DO:
                    compileDo();
                    break;

                case RETURN:
                    compileReturn();
                    break;

                default:
                    throw new RuntimeException("Unexpected keyword [" + tokenizer.keyword() + "]");
            }
        }
    }

    private void compileLet() {
        consumeKeyword(LET);
        String varName = consumeIdentifier();
        boolean array = isNextTokenTheSymbol(LEFT_SQUARE_BRACKET);
        if (array) {
            compileArrayExpression();
            vmWriter.writePush(symbolTable.kindOf(varName).segment(), symbolTable.indexOf(varName));
            vmWriter.writeArithmetic(ADD);
        }
        consumeSymbol(EQUAL_SIGN);
        compileExpression();
        consumeSymbol(SEMICOLON);
        if (array) {
            vmWriter.writePop(TEMP, 0); // pop the expression's value and store it in TEMP 0
            vmWriter.writePop(POINTER, 1); // pop the array index calculated before and store it in POINTER 1, i.e. aligning THAT to the heap area beginning at that address
            vmWriter.writePush(TEMP, 0); // push the expression's value onto the stack again
            vmWriter.writePop(THAT, 0); // pop it and store it in THAT
        }
        else {
            vmWriter.writePop(symbolTable.kindOf(varName).segment(), symbolTable.indexOf(varName));
        }
    }

    private void compileArrayExpression() {
        compileExpressionWithGroupingOperator(GroupingOperator.LEFT_SQUARE_BRACKET);
    }

    private void compileExpressionWithGroupingOperator(GroupingOperator groupingOperator) {
        operatorStack.push(groupingOperator);
        consumeSymbol(Symbol.valueOf(groupingOperator.name()));
        compileGroupedExpression();
        compileOperatorsOnStackUntilGroupingOperator(groupingOperator);
        consumeSymbol(Symbol.valueOf("RIGHT" + groupingOperator.name().substring(AFTER_PREFIX_LEFT_INDEX)));
    }

    private void compileGroupedExpression() {
        compileExpressionPoppingOperatorsFromStack(false);
    }

    private void compileExpressionPoppingOperatorsFromStack(boolean popOperatorsFromStack) {
        compileTerm();
        while (isNextTokenABinaryOperator()) {
            consumeBinaryOperator();
            compileTerm();
            if (popOperatorsFromStack) {
                compileOperatorsOnStack();
            }
        }
        if (popOperatorsFromStack) {
            compileOperatorsOnStack();
        }
    }

    private void compileTerm() {
        nextTokenIfPreviousHasBeenConsumed();
        switch (tokenizer.tokenType()) {
            case INT_CONST:
                compileIntegerConstant();
                break;

            case STRING_CONST:
                compileStringConstant();
                break;

            case KEYWORD:
                if (isTokenTheThisPointerOrAKeywordConstant()) {
                    compileThisPointerOrKeywordConstant();
                }
                break;

            case IDENTIFIER:
                compileIdentifier();
                break;

            case SYMBOL:
                compileGroupedExpressionOrUnaryOperator();
                break;

            default:
                throw new RuntimeException("Unexpected token type [" + tokenizer.tokenType() + "]");
        }
    }

    private void compileIntegerConstant() {
        vmWriter.writePush(CONSTANT, Math.abs(tokenizer.intVal()));
        if (tokenizer.intVal() < 0) {
            vmWriter.writeArithmetic(UnaryOperator.NEG);
        }
        tokenConsumed();
    }

    private void compileStringConstant() {
        String s = tokenizer.stringVal();
        vmWriter.writePush(CONSTANT, s.length());
        vmWriter.writeCall("String.new", 1);
        char[] chars = s.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            vmWriter.writePush(CONSTANT, (int) chars[i]);
            vmWriter.writeCall("String.appendChar", 2);
        }
        tokenConsumed();
    }

    private boolean isTokenTheThisPointerOrAKeywordConstant() {
        return isNextTokenOneOfKeywords(Keyword.THIS) || KEYWORD_CONSTANTS.contains(tokenizer.keyword());
    }

    private void compileThisPointerOrKeywordConstant() {
        if (isNextTokenOneOfKeywords(Keyword.THIS)) {
            getThisPointer();
            tokenConsumed();
        }
        else {
            compileKeywordConstant();
        }
    }

    private void getThisPointer() {
        vmWriter.writePush(POINTER, 0);
    }

    private void compileKeywordConstant() {
        vmWriter.writePush(CONSTANT, 0);
        if (isNextTokenOneOfKeywords(TRUE)) {
            // true == -1, i.e. ~(0000 0000) = 1111 1111
            vmWriter.writeArithmetic(UnaryOperator.NOT);
        }
        tokenConsumed();
    }

    private void compileIdentifier() {
        String identifier = consumeIdentifier(); // varName or subroutineCall's subroutineName|className|varName
        int identifierIndex = symbolTable.indexOf(identifier);
        if (identifierIndex > -1) {
            boolean arrayExpression = isNextTokenTheSymbol(LEFT_SQUARE_BRACKET);
            if (arrayExpression) { // varName[...]
                compileArrayExpression();
            }
            if (methodBody && (symbolTable.kindOf(identifier) == Kind.ARG)) {
                ++identifierIndex;
            }
            if (!isNextTokenTheSymbol(DOT)) {
                vmWriter.writePush(symbolTable.kindOf(identifier).segment(), identifierIndex);
            }
            if (arrayExpression) {
                vmWriter.writeArithmetic(ADD);
                vmWriter.writePop(POINTER, 1);
                vmWriter.writePush(THAT, 0);
            }
            else if (isNextTokenTheSymbol(DOT)) {
                compileSubroutineCall(Optional.of(identifier));
            }
        }
        else {
            compileSubroutineCall(Optional.of(identifier));
        }
    }

    private void compileSubroutineCall(Optional<String> subroutineNameOrClassNameOrVarName) {
        String subroutineName = subroutineNameOrClassNameOrVarName.orElseGet(() -> consumeIdentifier());
        if (isNextTokenTheSymbol(LEFT_PARENTHESIS)) { // with subroutineName left of parenthesis
            getThisPointer();
            subroutineName = className + DOT + subroutineName;
            ++numberOfArguments;
        }
        else if (isNextTokenTheSymbol(DOT)) { // with className or varName left of dot
            String objectType = symbolTable.typeOf(subroutineName);
            if (objectType != null) {
                vmWriter.writePush(symbolTable.kindOf(subroutineName).segment(), symbolTable.indexOf(subroutineName));
                subroutineName = objectType;
                ++numberOfArguments;
            }
            consumeSymbol(DOT);
            subroutineName += DOT + consumeIdentifier();
        }
        consumeExpressionListWithParenthesis();
        vmWriter.writeCall(subroutineName, numberOfArguments);
        numberOfArguments = 0;
    }

    private void consumeExpressionListWithParenthesis() {
        consumeSymbol(LEFT_PARENTHESIS);
        compileExpressionList();
        consumeSymbol(RIGHT_PARENTHESIS);
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

    private void compileExpression() {
        compileExpressionPoppingOperatorsFromStack(true);
    }

    private void compileGroupedExpressionOrUnaryOperator() {
        if (tokenizer.symbol() == LEFT_PARENTHESIS) {
            compileExpressionWithGroupingOperator(GroupingOperator.LEFT_PARENTHESIS);
        }
        else if (UnaryOperator.isOperator(tokenizer.symbol())) {
            compileUnaryOperator();
            compileTerm();
        }
    }

    private void compileOperatorsOnStackUntilGroupingOperator(GroupingOperator groupingOperator) {
        while (operatorStack.contains(groupingOperator)) {
            Operator operator = operatorStack.pop();
            if (operator == groupingOperator) {
                break;
            }
            vmWriter.writeArithmetic(operator);
        }
    }

    private void compileUnaryOperator() {
        Symbol symbol = tokenizer.symbol();
        operatorStack.push(UnaryOperator.fromSymbol(symbol));
        consumeSymbol(symbol);
    }

    private boolean isNextTokenABinaryOperator() {
        return isTokenOfType(SYMBOL) && BinaryOperator.isOperator(tokenizer.symbol());
    }

    private void consumeBinaryOperator() {
        Symbol symbol = tokenizer.symbol();
        operatorStack.push(BinaryOperator.fromSymbol(symbol));
        consumeSymbol(symbol);
    }

    private void compileOperatorsOnStack() {
        if (operatorStack.contains(GroupingOperator.LEFT_PARENTHESIS) || operatorStack.contains(GroupingOperator.LEFT_SQUARE_BRACKET)) {
            return;
        }
        while (!operatorStack.isEmpty()) {
            Operator operator = operatorStack.pop();
            vmWriter.writeArithmetic(operator);
        }
    }

    private void compileIf() {
        int labelNumber = ++ifStatementInSubroutineCounter - 1;
        consumeKeyword(IF);
        consumeSymbol(LEFT_PARENTHESIS);
        compileExpression();
        consumeSymbol(RIGHT_PARENTHESIS);
        vmWriter.writeIf("IF_TRUE" + labelNumber);
        vmWriter.writeGoto("IF_FALSE" + labelNumber);
        vmWriter.writeLabel("IF_TRUE" + labelNumber);
        consumeStatementBlock();
        if (isNextTokenOneOfKeywords(ELSE)) {
            vmWriter.writeGoto("IF_END" + labelNumber);
        }
        vmWriter.writeLabel("IF_FALSE" + labelNumber);
        if (isNextTokenOneOfKeywords(ELSE)) {
            consumeKeyword(ELSE);
            consumeStatementBlock();
            vmWriter.writeLabel("IF_END" + labelNumber);
        }
    }

    private void consumeStatementBlock() {
        consumeSymbol(LEFT_CURLY_BRACE);
        compileStatements();
        consumeSymbol(RIGHT_CURLY_BRACE);
    }

    private void compileWhile() {
        int labelNumber = ++whileStatementInSubroutineCounter - 1;
        consumeKeyword(WHILE);
        consumeSymbol(LEFT_PARENTHESIS);
        vmWriter.writeLabel("WHILE_EXP" + labelNumber);
        compileExpression();
        consumeSymbol(RIGHT_PARENTHESIS);
        vmWriter.writeArithmetic(UnaryOperator.NOT);
        vmWriter.writeIf("WHILE_END" + labelNumber);
        consumeStatementBlock();
        vmWriter.writeGoto("WHILE_EXP" + labelNumber);
        vmWriter.writeLabel("WHILE_END" + labelNumber);
    }

    private void compileDo() {
        consumeKeyword(DO);
        compileSubroutineCall(Optional.empty());
        vmWriter.writePop(TEMP, 0);
        consumeSymbol(SEMICOLON);
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

    private void closeTokenizerAndVmWriter() {
        // hack
        while (tokenizer.hasMoreTokens()) {
        }
        vmWriter.close();
    }
}
