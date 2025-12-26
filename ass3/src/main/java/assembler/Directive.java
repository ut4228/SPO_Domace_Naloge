package assembler;

public class Directive extends Node {
    private final String mnemonic;
    private final Object operand;

    public Directive(String label, String mnemonic, Object operand) {
        super(label, null);
        this.mnemonic = mnemonic;
        this.operand = operand;
    }

    public String getMnemonic() {
        return mnemonic;
    }

    public Object getOperand() {
        return operand;
    }

    @Override
    public String pretty(int indent) {
        String op = operand != null ? " " + operand : "";
        return pad(indent) + "Directive " + labelPrefix() + mnemonic + op;
    }

    @Override
    public int length() {
        return 0;
    }

    @Override
    public void enter(Code code) {
        if (mnemonic.equalsIgnoreCase("ORG") && operand != null) {
            Integer addr = operandAsIntOrNull();
            if (addr != null) {
                code.setLocctr(addr);
                code.setNext(addr);
            }
        } else if (mnemonic.equalsIgnoreCase("START") && operand != null) {
            Integer addr = operandAsIntOrNull();
            if (addr != null) {
                code.setStartAddress(addr);
                code.setLocctr(addr);
                code.setNext(addr);
            }
        }
    }

    @Override
    public void resolve(Code code) throws ParseException {
        if (mnemonic.equalsIgnoreCase("BASE")) {
            if (operand == null) return;
            Integer addr = code.lookupSymbol(String.valueOf(operand));
            if (addr == null) {
                throw new ParseException("Undefined symbol for BASE: " + operand);
            }
            code.setBase(addr);
        } else if (mnemonic.equalsIgnoreCase("NOBASE")) {
            code.setBase(null);
        } else if (mnemonic.equalsIgnoreCase("EQU")) {
            // Already defined during activation; nothing more here
        }
    }

    @Override
    public void activate(Code code) throws ParseException {
        if (mnemonic.equalsIgnoreCase("EQU")) {
            if (label == null) throw new ParseException("EQU without label");

            // EQU * means "current location counter".
            if (operand instanceof String sym && sym.equals("*")) {
                code.defineSymbol(label, code.getLocctr());
                return;
            }

            Integer val = operandAsIntOrNull();
            if (val != null) {
                code.defineSymbol(label, val);
                return;
            }
            if (operand instanceof String sym) {
                // Support simple expression: A-B
                Integer expr = tryEvalMinusExpression(sym, code);
                if (expr != null) {
                    code.defineSymbol(label, expr);
                    return;
                }

                Integer other = code.lookupSymbol(sym);
                if (other == null) throw new ParseException("EQU undefined symbol: " + sym);
                code.defineSymbol(label, other);
                return;
            }
            throw new ParseException("Unsupported EQU operand: " + operand);
        }
        super.activate(code);
    }

    /**
     * Tries to evaluate "A-B" where A and B are symbols or "*".
     * Returns null if the expression doesn't match this simple form.
     */
    private Integer tryEvalMinusExpression(String expr, Code code) throws ParseException {
        String s = expr.replaceAll("\\s+", "");
        int dash = s.indexOf('-');
        if (dash <= 0 || dash != s.lastIndexOf('-') || dash >= s.length() - 1) return null;

        String left = s.substring(0, dash);
        String right = s.substring(dash + 1);

        Integer l;
        if (left.equals("*")) {
            l = code.getLocctr();
        } else {
            l = code.lookupSymbol(left);
            if (l == null) {
                throw new ParseException("EQU undefined symbol: " + left);
            }
        }

        Integer r;
        if (right.equals("*")) {
            r = code.getLocctr();
        } else {
            r = code.lookupSymbol(right);
            if (r == null) {
                throw new ParseException("EQU undefined symbol: " + right);
            }
        }
        return l - r;
    }

    private Integer operandAsIntOrNull() {
        if (operand instanceof Integer i) return i;
        try {
            return Integer.parseInt(String.valueOf(operand));
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
