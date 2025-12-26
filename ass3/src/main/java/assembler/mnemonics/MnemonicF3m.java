package assembler.mnemonics;

import assembler.InstructionF3;
import assembler.Mnemonic;
import assembler.Node;
import assembler.ParseException;
import assembler.Parser;
import assembler.Parser.AddressedOperand;

public class MnemonicF3m extends Mnemonic {
    public MnemonicF3m(String name) {
        super(name, null, "Format 3 with operand");
    }

    @Override
    public Node parse(Parser parser) throws ParseException {
        parser.ensureOperandCount(1, 1);
        AddressedOperand info = parser.parseAddressedOperand(parser.getOperands().get(0));
        parser.registerLiteral(info.literalText());
        return new InstructionF3(parser.getCurrentLabel(), name, info.operand(), info.addressing(), info.indexed());
    }
}
