package assembler;

import java.io.ByteArrayOutputStream;

public class InstructionF1 extends Node {
    private final String mnemonic;

    public InstructionF1(String label, String mnemonic) {
        super(label, null);
        this.mnemonic = mnemonic;
    }

    @Override
    public String pretty(int indent) {
        return pad(indent) + "F1 " + labelPrefix() + mnemonic;
    }

    @Override
    public int length() {
        return 1; // format 1 size in bytes
    }

    @Override
    public void emitCode(ByteArrayOutputStream out, Code code) {
        int opcode = MnemonicOpcodes.opcode(mnemonic);
        out.write(opcode & 0xFF);
    }
}
