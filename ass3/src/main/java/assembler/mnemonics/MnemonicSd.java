package assembler.mnemonics;

import assembler.Mnemonic;
import assembler.Node;
import assembler.ParseException;
import assembler.Parser;
import assembler.Storage;

public class MnemonicSd extends Mnemonic {
    public MnemonicSd(String name) {
        super(name, null, "Storage directive with data");
    }

    @Override
    public Node parse(Parser parser) throws ParseException {
        parser.ensureOperandCount(1, 1);
        Object value = parser.getOperands().get(0);
        return new Storage(parser.getCurrentLabel(), name, value);
    }
}
