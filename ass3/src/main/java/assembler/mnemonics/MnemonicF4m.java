package assembler.mnemonics;

import assembler.InstructionF4;
import assembler.Mnemonic;
import assembler.Node;
import assembler.ParseException;
import assembler.Parser;
import assembler.Parser.AddressedOperand;

public class MnemonicF4m extends Mnemonic {
    public MnemonicF4m(String name) {
        super(name, null, "Format 4 with operand");
    }

    @Override
    public Node parse(Parser parser) throws ParseException {
        parser.ensureOperandCount(1, 1);
        AddressedOperand info = parser.parseAddressedOperand(parser.getOperands().get(0));
        parser.registerLiteral(info.literalText());
        return new InstructionF4(parser.getCurrentLabel(), name, info.operand(), info.addressing(), info.indexed());
    }
}
