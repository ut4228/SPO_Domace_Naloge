package assembler.mnemonics;

import assembler.InstructionF2;
import assembler.Mnemonic;
import assembler.Node;
import assembler.ParseException;
import assembler.Parser;

public class MnemonicF2r extends Mnemonic {
    public MnemonicF2r(String name) {
        super(name, null, "Format 2 with one register");
    }

    @Override
    public Node parse(Parser parser) throws ParseException {
        parser.ensureOperandCount(1, 1);
        String r1 = parser.parseRegister(parser.getOperands().get(0));
        return new InstructionF2(parser.getCurrentLabel(), name, r1, null);
    }
}
