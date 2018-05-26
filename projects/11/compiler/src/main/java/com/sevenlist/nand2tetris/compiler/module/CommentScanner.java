package com.sevenlist.nand2tetris.compiler.module;

class CommentScanner {

    private boolean inComment;

    boolean isCommentOrWhitespace(String s) {
        s = s.trim();
        if (s.startsWith("//") || isBlank(s)) {
            return true;
        }
        if (!inComment && s.contains("*/") && !s.contains("/*")) {
            throw new RuntimeException("Invalid comment syntax as '/*' or '/**' is missing in: " + s);
        }
        if (s.startsWith("/*") && !s.contains("*/")) {
            inComment = true;
            return true;
        }
        if (inComment && !s.endsWith("*/")) {
            return true;
        }
        if (s.endsWith("*/")) {
            inComment = false;
            return true;
        }
        return false;
    }

    String stripComments(String s) {
        s = stripCommentsUntilClosing(s);
        s = stripCommentToEndOfLine(s);
        return s;
    }

    private String stripCommentsUntilClosing(String s) {
        int commentEndIndex;
        while ((commentEndIndex = s.indexOf("*/")) != -1) {
            int commentStartIndex = s.indexOf("/*");
            if (commentStartIndex == -1) {
                throw new RuntimeException("Invalid comment syntax as '/*' or '/**' is missing in: " + s);
            }
            if (commentStartIndex == 0) {
                s = getTextAfterComment(s, commentEndIndex);
            }
            else {
                s = getTextSurroundingComment(s, commentStartIndex, commentEndIndex);
            }
        }
        return s;
    }

    private String getTextSurroundingComment(String s, int commentStartIndex, int commentEndIndex) {
        String textBeforeComment = getTextBeforeComment(s, commentStartIndex);
        String textAfterComment = getTextAfterComment(s, commentEndIndex);
        s = textBeforeComment;
        if (!textAfterComment.isEmpty()) {
            s += " " + textAfterComment;
        }
        return s;
    }

    private String getTextBeforeComment(String s, int commentStartIndex) {
        return s.substring(0, commentStartIndex).trim();
    }

    private String getTextAfterComment(String s, int commentEndIndex) {
        return s.substring(commentEndIndex + 2, s.length()).trim();
    }

    private String stripCommentToEndOfLine(String s) {
        int slashesCommentIndex = s.indexOf("//");
        if (slashesCommentIndex == -1) {
            return s;
        }
        return s.substring(0, slashesCommentIndex).trim();
    }

    private boolean isBlank(String s) {
        return s.isEmpty() ? true : containsWhitespace(s);
    }

    private boolean containsWhitespace(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isWhitespace(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}