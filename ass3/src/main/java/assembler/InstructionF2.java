package assembler;

import java.io.ByteArrayOutputStream;

public class InstructionF2 extends Node {
    private final String mnemonic;
    private final String r1;
    private final String r2;

    public InstructionF2(String label, String mnemonic, String r1, String r2) {
        super(label, null);
        this.mnemonic = mnemonic;
        this.r1 = r1;
        this.r2 = r2;
    }

    @Override
    public String pretty(int indent) {
        String ops;
        if (r1 == null && r2 == null) {
            ops = "";
        } else if (r2 == null) {
            ops = " " + r1;
        } else {
            ops = " " + r1 + ", " + r2;
        }
        return pad(indent) + "F2 " + labelPrefix() + mnemonic + ops;
    }

    @Override
    public int length() {
        return 2; // format 2 size in bytes
    }

    @Override
    public void resolve(Code code) throws ParseException {
        // registers already validated; no symbol resolution needed here
    }

    @Override
    public void emitCode(ByteArrayOutputStream out, Code code) throws ParseException {
        int op = MnemonicOpcodes.opcode(mnemonic);
        int r1v = r1 != null ? Registers.reg(r1) : 0;
        int r2v = 0;
        if (r2 != null) {
            if (r2.matches("-?\\d+")) {
                r2v = Integer.parseInt(r2) & 0x0F;
            } else {
                r2v = Registers.reg(r2);
            }
        }
        out.write(op & 0xFF);
        out.write(((r1v & 0x0F) << 4) | (r2v & 0x0F));
    }
}
