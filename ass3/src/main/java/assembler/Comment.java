package assembler;

public class Comment extends Node {
    private final String content;

    public Comment(String content) {
        super(null, null);
        this.content = content;
    }

    @Override
    public String pretty(int indent) {
        return pad(indent) + "Comment: " + content;
    }
}
