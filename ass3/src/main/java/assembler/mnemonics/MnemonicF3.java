package assembler.mnemonics;

import assembler.InstructionF3;
import assembler.Mnemonic;
import assembler.Node;
import assembler.ParseException;
import assembler.Parser;

public class MnemonicF3 extends Mnemonic {
    public MnemonicF3(String name) {
        super(name, null, "Format 3 without operands");
    }

    @Override
    public Node parse(Parser parser) throws ParseException {
        parser.ensureOperandCount(0, 0);
        return new InstructionF3(parser.getCurrentLabel(), name, null, "simple", false);
    }
}
