package assembler;

public abstract class Mnemonic {
    protected final String name;
    protected final Integer opcode;
    protected final String description;

    protected Mnemonic(String name, Integer opcode, String description) {
        this.name = name;
        this.opcode = opcode;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public abstract Node parse(Parser parser) throws ParseException;
}
