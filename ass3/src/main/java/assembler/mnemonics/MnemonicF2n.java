package assembler.mnemonics;

import assembler.InstructionF2;
import assembler.Mnemonic;
import assembler.Node;
import assembler.ParseException;
import assembler.Parser;

public class MnemonicF2n extends Mnemonic {
    public MnemonicF2n(String name) {
        super(name, null, "Format 2 with numeric operand");
    }

    @Override
    public Node parse(Parser parser) throws ParseException {
        parser.ensureOperandCount(1, 1);
        Object num = parser.parseNumericOrSymbol(parser.getOperands().get(0));
        return new InstructionF2(parser.getCurrentLabel(), name, String.valueOf(num), null);
    }
}
