package assembler;

import java.io.ByteArrayOutputStream;

public class InstructionF4 extends Node {
    private final String mnemonic;
    private final Object operand;
    private final String addressing;
    private final boolean indexed;

    private int address = 0;
    private boolean resolved = false;
    private String targetSymbol = null;

    public InstructionF4(String label, String mnemonic, Object operand, String addressing, boolean indexed) {
        super(label, null);
        this.mnemonic = mnemonic;
        this.operand = operand;
        this.addressing = addressing;
        this.indexed = indexed;
    }

    @Override
    public String pretty(int indent) {
        String idx = indexed ? ",X" : "";
        String prefix = switch (addressing) {
            case "immediate" -> "#";
            case "indirect" -> "@";
            case "literal" -> "=";
            default -> "";
        };
        String op = operand != null ? " " + prefix + operand + idx : "";
        return pad(indent) + "F4 " + labelPrefix() + "+" + mnemonic + op;
    }

    @Override
    public int length() {
        return 4; // format 4 size
    }

    @Override
    public void resolve(Code code) throws ParseException {
        if (operand instanceof String sym) {
            Integer addr = code.lookupSymbol(sym);
            if (addr == null) throw new ParseException("Undefined symbol: " + sym);
            this.address = addr & 0xFFFFF; // 20 bits
            this.targetSymbol = sym;
            this.resolved = true;
        } else if (operand instanceof Integer val) {
            int v = val;
            if (v < -524288 || v > 0xFFFFF) {
                throw new ParseException("Immediate out of range for format 4: " + v);
            }
            this.address = v & 0xFFFFF;
            this.resolved = true;
        } else if (operand == null) {
            this.resolved = true;
        }
    }

    public String getTargetSymbol() {
        return targetSymbol;
    }

    @Override
    public void emitCode(ByteArrayOutputStream out, Code code) throws ParseException {
        if (!resolved) resolve(code);
        int op = MnemonicOpcodes.opcode(mnemonic);
        int ni = switch (addressing) {
            case "immediate" -> 0b01;
            case "indirect" -> 0b10;
            default -> 0b11;
        };
        int xbpe = 0b0001; // e = 1 for format 4
        if (indexed) xbpe |= 0b1000;
        int byte1 = (op & 0xFC) | ni;
        out.write(byte1 & 0xFF);
        out.write((xbpe << 4) | ((address >> 16) & 0x0F));
        out.write((address >> 8) & 0xFF);
        out.write(address & 0xFF);
    }
}
