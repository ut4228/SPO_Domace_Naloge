package assembler.mnemonics;

import assembler.InstructionF1;
import assembler.Mnemonic;
import assembler.Node;
import assembler.ParseException;
import assembler.Parser;

public class MnemonicF1 extends Mnemonic {
    public MnemonicF1(String name) {
        super(name, null, "Format 1 instruction");
    }

    @Override
    public Node parse(Parser parser) throws ParseException {
        parser.ensureOperandCount(0, 0);
        return new InstructionF1(parser.getCurrentLabel(), name);
    }
}
