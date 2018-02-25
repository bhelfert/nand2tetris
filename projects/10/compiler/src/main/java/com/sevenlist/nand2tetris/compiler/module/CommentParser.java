package com.sevenlist.nand2tetris.compiler.module;

class CommentParser {

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
        int commentEndIndex;
        while ((commentEndIndex = s.indexOf("*/")) != -1) {
            int commentStartIndex = s.indexOf("/*");
            if (commentStartIndex == -1) {
                throw new RuntimeException("Invalid comment syntax as '/*' or '/**' is missing in: " + s);
            }
            if (commentStartIndex == 0) {
                s = s.substring(commentEndIndex + 2, s.length()).trim();
            }
            else {
                String textBeforeComment = s.substring(0, commentStartIndex).trim();
                String textAfterComment = s.substring(commentEndIndex + 2, s.length()).trim();
                s = textBeforeComment;
                if (!textAfterComment.isEmpty()) {
                    s += " " + textAfterComment;
                }
            }
        }
        int slashesCommentIndex;
        if ((slashesCommentIndex = s.indexOf("//")) != -1) {
            s = s.substring(0, slashesCommentIndex).trim();
        }
        return s;
    }

    private boolean isBlank(String s) {
        if (s.isEmpty()) {
            return true;
        }
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isWhitespace(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}