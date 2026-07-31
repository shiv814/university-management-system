package ca.shivam.university;

import java.util.ArrayList;
import java.util.List;

final class CommandTokenizer {
    private CommandTokenizer() {}

    static List<String> tokenize(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < line.length(); index++) {
            char ch = line.charAt(index);
            if (ch == '"') {
                quoted = !quoted;
            } else if (Character.isWhitespace(ch) && !quoted) {
                if (!current.isEmpty()) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(ch);
            }
        }
        if (quoted) throw new IllegalArgumentException("unterminated quote");
        if (!current.isEmpty()) tokens.add(current.toString());
        return tokens;
    }
}
