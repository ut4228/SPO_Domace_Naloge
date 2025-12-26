package assembler;

import java.io.ByteArrayOutputStream;

public class InstructionF3 extends Node {
    private final String mnemonic;
    private final Object operand;
    private final String addressing; // immediate, indirect, literal, simple
    private final boolean indexed;

    // resolved
    private int disp = 0;
    private boolean usePc = false;
    private boolean useBase = false;
    private boolean resolved = false;

    public InstructionF3(String label, String mnemonic, Object operand, String addressing, boolean indexed) {
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
        return pad(indent) + "F3 " + labelPrefix() + mnemonic + op;
    }

    @Override
    public int length() {
        return 3; // format 3 size
    }

    @Override
    public void resolve(Code code) throws ParseException {
        if (operand instanceof String sym) {
            Integer addr = code.lookupSymbol(sym);
            if (addr == null) throw new ParseException("Undefined symbol: " + sym);
            // choose addressing: prefer PC-relative if fits, else BASE, else direct
            int pc = code.getNext();
            int disp = addr - pc;
            if (disp >= -2048 && disp <= 2047) {
                // PC-relative allowed
                this.disp = disp & 0xFFF;
                this.usePc = true;
                this.resolved = true;
            } else if (code.getBase() != null) {
                int bdisp = addr - code.getBase();
                if (bdisp < 0 || bdisp > 4095) {
                    throw new ParseException("Displacement out of range for BASE: " + sym);
                }
                this.disp = bdisp & 0xFFF;
                this.useBase = true;
                this.resolved = true;
            } else {
                throw new ParseException("Cannot resolve address (no PC/BASE fit): " + sym);
            }
        } else if (operand instanceof Integer val) {
            int v = val;
            if (v < -2048 || v > 4095) {
                throw new ParseException("Immediate out of range for format 3: " + v);
            }
            this.disp = v & 0xFFF;
            this.usePc = false;
            this.useBase = false;
            this.resolved = true;
        } else if (operand == null) {
            this.resolved = true;
        }
    }

    @Override
    public void emitCode(ByteArrayOutputStream out, Code code) throws ParseException {
        if (!resolved) resolve(code);
        int op = MnemonicOpcodes.opcode(mnemonic);
        int ni = switch (addressing) {
            case "immediate" -> 0b01;
            case "indirect" -> 0b10;
            default -> 0b11; // simple
        };
        int xbpe = 0;
        if (indexed) xbpe |= 0b1000;
        if (useBase) xbpe |= 0b0100;
        if (usePc) xbpe |= 0b0010;
        // e=0 for format3
        int byte1 = (op & 0xFC) | ((ni >> 1) & 0x01) << 1 | (ni & 0x01);
        out.write(byte1 & 0xFF);
        out.write((xbpe << 4) | ((disp >> 8) & 0x0F));
        out.write(disp & 0xFF);
    }
}
