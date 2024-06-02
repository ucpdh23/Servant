package es.xan.servantv3.knowledge.utils.parser;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;
import java.util.*;

public class Tokenizer {

    private static Map<String, String> delimiters = createDelimiters();
    private boolean parseable = false;
    private Deque<String> tokens = new LinkedList<>();
    private ArrayDeque<String> brackets = new ArrayDeque<>();

    private static Map<String, String> createDelimiters() {
        Map<String, String> delimiters = new HashMap<>();

        delimiters.put("'", "'");
        delimiters.put("`", "`");
        delimiters.put(",", ",");
        delimiters.put(",@", ",@");

        return delimiters;
    }

    public boolean isParseable() {
        return parseable;
    }

    public Deque<String> tokenize(String expr) {
        boolean readAsString = false;
        StringBuilder strAcc = new StringBuilder();
        StringBuilder tokenAcc = new StringBuilder();
        Deque<String> newTokens = new LinkedList<>();

        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);

            if (c == '"') {
                readAsString = !readAsString;

                strAcc.append(c);

                if (!readAsString) {
                    newTokens.add(strAcc.toString());
                    strAcc = new StringBuilder();
                }
            } else if (readAsString) {
                strAcc.append(c);
            } else if (c == ' ' || c == '\t' || c == '\r' || c == '\n') {
                if (tokenAcc.length() > 0) {
                    newTokens.add(tokenAcc.toString());
                    tokenAcc = new StringBuilder();
                }
            } else if (c == ';') {
                break;
            } else if (c == '(' || c == '[') {
                brackets.add(String.valueOf(c));
                if (tokenAcc.length() > 0) {
                    newTokens.add(tokenAcc.toString());
                    tokenAcc = new StringBuilder();
                }
                newTokens.add(String.valueOf(c));
            } else if (c == ')') {
                if (brackets.isEmpty() || !brackets.removeLast().equals("(")) {
                    throw new SyntaxError("Unmatched delimiter (");
                }

                if (tokenAcc.length() > 0) {
                    newTokens.add(tokenAcc.toString());
                    tokenAcc = new StringBuilder();
                }

                newTokens.add(String.valueOf(c));
            } else if (c == ']') {
                if (brackets.isEmpty() || !brackets.removeLast().equals("[")) {
                    throw new SyntaxError("Unmatched delimiter [");
                }

                if (tokenAcc.length() > 0) {
                    newTokens.add(tokenAcc.toString());
                    tokenAcc = new StringBuilder();
                }

                newTokens.add(String.valueOf(c));
            } else if (c == ',' && expr.charAt(i + 1) == '@') {
                newTokens.add(",@");
                i++;
            } else if (delimiters.containsKey(String.valueOf(c))) {
                if (tokenAcc.length() > 0) {
                    newTokens.add(tokenAcc.toString());
                    tokenAcc = new StringBuilder();
                }

                newTokens.add(String.valueOf(c));
            } else {
                tokenAcc.append(c);
            }
        }

        if (tokenAcc.length() > 0) {
            newTokens.add(tokenAcc.toString());
        }

        if (tokenAcc.length() > 0) {
            throw new SyntaxError("Unclosed quotes error");
        }

        parseable = brackets.isEmpty() && !newTokens.isEmpty();

        this.tokens.addAll(newTokens);

        return new LinkedList<>(this.tokens);
    }

    public Deque<String> getTokens() {
        return tokens;
    }

    public void clean() {
        tokens.clear();
        brackets.clear();
    }
}
