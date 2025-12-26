package assembler;

import java.io.ByteArrayOutputStream;

public abstract class Node {
    protected String label;
    protected String comment;

    public Node() {}

    public Node(String label, String comment) {
        this.label = label;
        this.comment = comment;
    }

    public String getLabel() {
        return label;
    }

    public String getComment() {
        return comment;
    }

    public abstract String pretty(int indent);

    /** Length of generated code in bytes; default 0 for non-encoding nodes (e.g., comments). */
    public int length() {
        return 0;
    }

    /** Called before processing this node in a pass (updates locctr, etc.). */
    public void enter(Code code) {}

    /** Called after processing this node in a pass. */
    public void leave(Code code) {}

    /** Register label to symbol table (first pass). */
    public void activate(Code code) throws ParseException {
        if (label != null) {
            code.defineSymbol(label, code.getLocctr());
        }
    }

    /** Resolve right-hand symbols / addressing (second pass). */
    public void resolve(Code code) throws ParseException {}

    /** Emit raw bytes for this node into buffer at given absolute address. Default no-op. */
    public void emitCode(ByteArrayOutputStream out, Code code) throws ParseException {}

    protected String pad(int indent) {
        return " ".repeat(Math.max(0, indent));
    }

    protected String labelPrefix() {
        return label != null ? "[" + label + "] " : "";
    }
}
