package com.sevenlist.nand2tetris.compiler.module;

import java.util.HashMap;
import java.util.Map;

public class SymbolTable {

    private Map<String, IdentifierInfo> classScope = new HashMap<>();
    private Map<String, IdentifierInfo> subroutineScope = new HashMap<>();

    public void startSubroutine() {
        subroutineScope.clear();
    }

    public void define(String name, String type, Kind kind) {
        switch (kind) {
            case STATIC:
            case FIELD:
                classScope.put(name, new IdentifierInfo(type, kind, varCount(kind)));
                return;

            case ARG:
            case VAR:
                subroutineScope.put(name, new IdentifierInfo(type, kind, varCount(kind)));
                return;
        }
        throw new IllegalArgumentException("Kind [" + kind + "] is not handled");
    }

    public int varCount(Kind kind) {
        Map<String, IdentifierInfo> scopeMapToSearchIn;
        switch (kind) {
            case STATIC:
            case FIELD:
                scopeMapToSearchIn = classScope;
                break;

            case ARG:
            case VAR:
                scopeMapToSearchIn = subroutineScope;
                break;

            default:
                throw new IllegalArgumentException("Kind [" + kind + "] is not handled");
        }
        long numberOfVariablesHavingKind = scopeMapToSearchIn.values().stream()
                .filter(identifierInfo -> identifierInfo.kind == kind)
                .count();
        return Math.toIntExact(numberOfVariablesHavingKind);
    }

    public Kind kindOf(String name) {
        if (subroutineScope.containsKey(name)) {
            return subroutineScope.get(name).kind;
        }
        if (classScope.containsKey(name)) {
            return classScope.get(name).kind;
        }
        return null;
    }

    public String typeOf(String name) {
        if (subroutineScope.containsKey(name)) {
            return subroutineScope.get(name).type;
        }
        if (classScope.containsKey(name)) {
            return classScope.get(name).type;
        }
        return null;
    }

    public int indexOf(String name) {
        if (subroutineScope.containsKey(name)) {
            return subroutineScope.get(name).index;
        }
        if (classScope.containsKey(name)) {
            return classScope.get(name).index;
        }
        return -1;
    }

    private static final class IdentifierInfo {

        public final String type;
        public final Kind kind;
        public final int index;

        public IdentifierInfo(String type, Kind kind, int index) {
            this.type = type;
            this.kind = kind;
            this.index = index;
        }
    }
}