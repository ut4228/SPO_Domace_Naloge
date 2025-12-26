package assembler.mnemonics;

import assembler.Directive;
import assembler.Mnemonic;
import assembler.Node;
import assembler.ParseException;
import assembler.Parser;

public class MnemonicDn extends Mnemonic {
    public MnemonicDn(String name) {
        super(name, null, "Directive with numeric/symbol operand");
    }

    @Override
    public Node parse(Parser parser) throws ParseException {
        parser.ensureOperandCount(1, 1);
        Object operand = parser.parseNumericOrSymbol(parser.getOperands().get(0));
        return new Directive(parser.getCurrentLabel(), name, operand);
    }
}
