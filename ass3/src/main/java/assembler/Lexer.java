package assembler;

import java.util.ArrayList;
import java.util.List;

/** Simple line-oriented lexer sufficient for SIC/XE assembler parsing. */
public class Lexer {
    private final String[] lines;
    private int lineIndex = 0;

    public Lexer(String source) {
        this.lines = source.split("\\r?\\n", -1);
    }

    /** Returns tokens for the next line (without trailing NEWLINE), or null at EOF. */
    public List<Token> nextLineTokens() {
        if (lineIndex >= lines.length) return null;
        String line = lines[lineIndex];
        int lineNo = lineIndex + 1;
        lineIndex++;

        List<Token> tokens = new ArrayList<>();
        int i = 0;

        // detect whole-line comment starting with '.' as first non-space
        int firstNonSpace = firstNonSpace(line);
        if (firstNonSpace >= 0 && line.charAt(firstNonSpace) == '.') {
            String content = line.substring(firstNonSpace + 1).trim();
            tokens.add(new Token(TokenType.COMMENT, content, lineNo, firstNonSpace));
            return tokens;
        }

        while (i < line.length()) {
            char c = line.charAt(i);
            int col = i;

            // inline comments ; or //
            if (c == ';' || (c == '/' && i + 1 < line.length() && line.charAt(i + 1) == '/')) {
                break; // ignore rest of line
            }

            // inline comment starting with '.' (if not already handled as whole-line comment)
            if (c == '.') {
                break;
            }

            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }

            // Character / hex literals: C'....' or X'....'
            if ((c == 'C' || c == 'X') && (i + 1) < line.length() && line.charAt(i + 1) == '\'') {
                int start = i;
                i += 2; // skip C'
                while (i < line.length() && line.charAt(i) != '\'') i++;
                if (i < line.length()) i++; // include closing '
                String lex = line.substring(start, i);
                tokens.add(new Token(TokenType.IDENT, lex, lineNo, start));
                continue;
            }

            if (c == ',') { tokens.add(new Token(TokenType.COMMA, ",", lineNo, col)); i++; continue; }
            if (c == '#') { tokens.add(new Token(TokenType.HASH, "#", lineNo, col)); i++; continue; }
            if (c == '@') { tokens.add(new Token(TokenType.AT, "@", lineNo, col)); i++; continue; }
            if (c == '+') { tokens.add(new Token(TokenType.PLUS, "+", lineNo, col)); i++; continue; }
            if (c == '=') { tokens.add(new Token(TokenType.EQUAL, "=", lineNo, col)); i++; continue; }

            if (Character.isDigit(c)) {
                int start = i;
                i++;
                while (i < line.length() && (Character.isDigit(line.charAt(i)) || isIdentChar(line.charAt(i)))) i++;
                String lex = line.substring(start, i);
                tokens.add(new Token(TokenType.NUMBER, lex, lineNo, start));
                continue;
            }

            if (isIdentStart(c)) {
                int start = i;
                i++;
                while (i < line.length() && isIdentChar(line.charAt(i))) i++;
                String lex = line.substring(start, i);
                tokens.add(new Token(TokenType.IDENT, lex, lineNo, start));
                continue;
            }

            // fallback single char
            tokens.add(new Token(TokenType.OTHER, String.valueOf(c), lineNo, col));
            i++;
        }

        return tokens;
    }

    private static boolean isIdentStart(char c) {
        return Character.isLetter(c) || c == '_';
    }

    private static boolean isIdentChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    private static int firstNonSpace(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isWhitespace(s.charAt(i))) return i;
        }
        return -1;
    }
}