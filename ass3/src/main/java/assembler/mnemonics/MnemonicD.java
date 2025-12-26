package assembler.mnemonics;

import assembler.Directive;
import assembler.Mnemonic;
import assembler.Node;
import assembler.ParseException;
import assembler.Parser;

public class MnemonicD extends Mnemonic {
    public MnemonicD(String name) {
        super(name, null, "Directive without operands");
    }

    @Override
    public Node parse(Parser parser) throws ParseException {
        parser.ensureOperandCount(0, 0);
        return new Directive(parser.getCurrentLabel(), name, null);
    }
}
