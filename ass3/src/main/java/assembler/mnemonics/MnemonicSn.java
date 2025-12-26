package assembler.mnemonics;

import assembler.Mnemonic;
import assembler.Node;
import assembler.ParseException;
import assembler.Parser;
import assembler.Storage;

public class MnemonicSn extends Mnemonic {
    public MnemonicSn(String name) {
        super(name, null, "Storage reservation directive");
    }

    @Override
    public Node parse(Parser parser) throws ParseException {
        parser.ensureOperandCount(1, 1);
        Object count = parser.parseNumericOrSymbol(parser.getOperands().get(0));
        return new Storage(parser.getCurrentLabel(), name, count);
    }
}
