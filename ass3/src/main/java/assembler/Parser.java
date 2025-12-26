package assembler;

import assembler.mnemonics.*;

import java.util.*;
import java.util.regex.Pattern;

public class Parser {
    private final Map<String, Mnemonic> mnemonicTable;
    private final Code code;
    private List<String> operands = List.of();
    private String currentLabel;
    private int currentLineNo;

    private static final Set<String> REGISTERS = Set.of("A", "X", "L", "B", "S", "T", "F", "PC", "SW");

    public Parser(String source) {
        this.mnemonicTable = buildMnemonicTable();
        this.lexer = new Lexer(source);
        this.code = new Code();
    }

    private final Lexer lexer;

    public Code parse() throws ParseException {
    Code code = this.code;
        List<Token> lineTokens;
        while ((lineTokens = lexer.nextLineTokens()) != null) {
            if (lineTokens.isEmpty()) continue;
            currentLineNo = lineTokens.get(0).getLine();

            // whole-line comment
            if (lineTokens.size() == 1 && lineTokens.get(0).getType() == TokenType.COMMENT) {
                code.getNodes().add(new Comment(lineTokens.get(0).getLexeme()));
                continue;
            }

            LineParts parts = parseLine(lineTokens);
            if (parts == null) continue; // empty line after trimming comment

            // Label-only line (no mnemonic): define symbol at current locctr.
            if (parts.mnemonic == null) {
                code.getNodes().add(new LabelOnly(parts.label));
                continue;
            }

            boolean fmt4 = parts.fmt4;
            String mnemonicName = parts.mnemonic;
            String key = (fmt4 ? "+" : "") + mnemonicName.toUpperCase();
            Mnemonic mnemonic = mnemonicTable.getOrDefault(key, mnemonicTable.get(mnemonicName.toUpperCase()));
            if (mnemonic == null) {
                throw new ParseException("Unknown mnemonic '" + mnemonicName + "' on line " + currentLineNo);
            }

            this.currentLabel = parts.label;
            this.operands = parts.operands;

            Node node;
            try {
                node = mnemonic.parse(this);
            } catch (ParseException e) {
                throw e;
            } catch (Exception e) {
                throw new ParseException("Failed to parse line " + currentLineNo + ": " + e.getMessage(), e);
            }

            if (node instanceof Directive directive && directivePrettyNameEquals(directive, "START")) {
                if (directive.getLabel() != null) {
                    code.setProgramName(directive.getLabel());
                }
                if (directive.getOperand() instanceof Integer val) {
                    code.setStartAddress(val);
                }
            }

            code.getNodes().add(node);
        }
        return code;
    }

    private boolean directivePrettyNameEquals(Directive directive, String name) {
        return directive != null && directive.getMnemonic().equalsIgnoreCase(name);
    }

    public String getCurrentLabel() {
        return currentLabel;
    }

    public List<String> getOperands() {
        return operands;
    }

    private LineParts parseLine(List<Token> tokens) throws ParseException {
        if (tokens.isEmpty()) return null;

        // label if first token is IDENT and at column 0
        String label = null;
        int idx = 0;
        Token first = tokens.get(idx);
        if (first.getType() == TokenType.IDENT && first.getColumn() == 0) {
            label = first.getLexeme();
            idx++;
        }

    if (idx >= tokens.size()) return new LineParts(label, null, false, List.of());

        boolean fmt4 = false;
        if (tokens.get(idx).getType() == TokenType.PLUS) {
            fmt4 = true;
            idx++;
        }
        if (idx >= tokens.size() || tokens.get(idx).getType() != TokenType.IDENT) {
            throw new ParseException("Missing mnemonic on line " + currentLineNo);
        }
        String mnemonic = tokens.get(idx).getLexeme();
        idx++;

        // Remaining tokens are operands.
        //
        // SIC/XE has an indexed addressing suffix ",X" for format 3/4.
        // Many sample programs write it as a separate token sequence: IDENT "," IDENT.
        // We must keep it inside the *same* operand (e.g. "BUFFER,X") instead of
        // splitting into two operands.
        List<String> ops = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (; idx < tokens.size(); idx++) {
            Token t = tokens.get(idx);

            if (t.getType() == TokenType.COMMA) {
                // If this comma is followed by an X register, treat as indexed suffix.
                if (idx + 1 < tokens.size()) {
                    Token next = tokens.get(idx + 1);
                    if (next.getType() == TokenType.IDENT && next.getLexeme().equalsIgnoreCase("X")) {
                        current.append(",X");
                        idx++; // consume the 'X'
                        continue;
                    }
                }

                // Otherwise, comma separates operands.
                if (current.length() > 0) {
                    ops.add(current.toString());
                    current.setLength(0);
                } else {
                    ops.add("");
                }
                continue;
            }

            // reconstruct operand text without spaces
            current.append(t.getLexeme());
        }
        if (current.length() > 0) ops.add(current.toString());

        return new LineParts(label, mnemonic, fmt4, ops);
    }

    public void ensureOperandCount(int min, int max) throws ParseException {
        int n = operands.size();
        if (n < min || n > max) {
            throw new ParseException("Expected " + min + "-" + max + " operands, got " + n + " on line " + currentLineNo);
        }
    }

    public String parseRegister(String operand) throws ParseException {
        String reg = operand.toUpperCase();
        if (!REGISTERS.contains(reg)) {
            throw new ParseException("Unknown register '" + operand + "' on line " + currentLineNo);
        }
        return reg;
    }

    public Object parseNumericOrSymbol(String operand) {
        try {
            if (Pattern.matches("[-+]?0x[0-9a-fA-F]+", operand)) {
                return Integer.parseInt(operand.substring(2), 16);
            }
            if (Pattern.matches("[-+]?0[0-7]+", operand)) {
                return Integer.parseInt(operand, 8);
            }
            return Integer.parseInt(operand);
        } catch (NumberFormatException ex) {
            return operand;
        }
    }

    public void registerLiteral(String literalText) {
        if (literalText != null) {
            code.registerLiteral(literalText);
        }
    }

    public AddressedOperand parseAddressedOperand(String operand) {
        String addressing = "simple";
        if (operand.startsWith("#")) {
            addressing = "immediate";
            operand = operand.substring(1);
        } else if (operand.startsWith("@")) {
            addressing = "indirect";
            operand = operand.substring(1);
        } else if (operand.startsWith("=")) {
            addressing = "literal";
            operand = operand.substring(1);
        }

        boolean indexed = false;
        if (operand.toUpperCase().endsWith(",X")) {
            indexed = true;
            operand = operand.substring(0, operand.length() - 2);
        }

        Object value = parseNumericOrSymbol(operand);
        String literalText = "literal".equals(addressing) ? operand : null;
        return new AddressedOperand(addressing, value, indexed, literalText);
    }

    private Map<String, Mnemonic> buildMnemonicTable() {
        Map<String, Mnemonic> table = new HashMap<>();

    register(table, List.of("NOBASE", "LTORG"), MnemonicD::new);
    register(table, List.of("START", "END", "BASE", "ORG", "EQU"), MnemonicDn::new);

        register(table, List.of("BYTE", "WORD"), MnemonicSd::new);
        register(table, List.of("RESB", "RESW"), MnemonicSn::new);

        register(table, List.of("FIX", "FLOAT", "HIO", "NORM", "SIO", "TIO"), MnemonicF1::new);

    register(table, List.of("CLEAR", "TIXR"), MnemonicF2r::new);
    register(table, List.of("ADDR", "SUBR", "MULR", "DIVR", "COMPR", "RMO"), MnemonicF2rr::new);
        register(table, List.of("SHIFTL", "SHIFTR"), MnemonicF2rn::new);
        register(table, List.of("SVC"), MnemonicF2n::new);

        register(table, List.of("RSUB"), MnemonicF3::new);
        List<String> f3ops = List.of(
                "LDA", "LDX", "LDL", "STA", "STX", "STL", "ADD", "SUB", "MUL", "DIV", "COMP",
        "J", "JEQ", "JLT", "JGT", "JSUB", "TD", "RD", "WD", "TIX", "LDCH", "STCH",
        "ADDF", "SUBF", "MULF", "DIVF", "LDB", "LDS", "LDF", "LDT", "STB", "STS", "STF", "STT", "COMPF",
        "AND", "OR", "LPS", "STI", "SSK", "STSW"
        );
        for (String op : f3ops) {
            table.put(op.toUpperCase(), new MnemonicF3m(op));
            table.put(("+" + op).toUpperCase(), new MnemonicF4m(op));
        }

        return table;
    }

    private void register(Map<String, Mnemonic> table, List<String> names, java.util.function.Function<String, Mnemonic> factory) {
        for (String name : names) {
            table.put(name.toUpperCase(), factory.apply(name));
        }
    }

    private record LineParts(String label, String mnemonic, boolean fmt4, List<String> operands) {}

    public record AddressedOperand(String addressing, Object operand, boolean indexed, String literalText) {}
}
