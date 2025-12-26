package assembler.mnemonics;

import assembler.InstructionF2;
import assembler.Mnemonic;
import assembler.Node;
import assembler.ParseException;
import assembler.Parser;

public class MnemonicF2rr extends Mnemonic {
    public MnemonicF2rr(String name) {
        super(name, null, "Format 2 with two registers");
    }

    @Override
    public Node parse(Parser parser) throws ParseException {
        parser.ensureOperandCount(2, 2);
        String r1 = parser.parseRegister(parser.getOperands().get(0));
        String r2 = parser.parseRegister(parser.getOperands().get(1));
        return new InstructionF2(parser.getCurrentLabel(), name, r1, r2);
    }
}
