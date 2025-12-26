package assembler;

/** Zero-length node used to declare a label on a line without a mnemonic. */
public class LabelOnly extends Node {
    public LabelOnly(String label) {
        super(label, null);
    }

    @Override
    public String pretty(int indent) {
        return pad(indent) + labelPrefix() + "(label)";
    }
}
