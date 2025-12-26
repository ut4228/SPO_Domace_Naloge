package assembler;

import java.io.ByteArrayOutputStream;

public class Storage extends Node {
    private final String mnemonic;
    private final Object value;

    public Storage(String label, String mnemonic, Object value) {
        super(label, null);
        this.mnemonic = mnemonic;
        this.value = value;
    }

    public String getMnemonic() {
        return mnemonic;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public String pretty(int indent) {
        String val = value != null ? " " + value : "";
        return pad(indent) + "Storage " + labelPrefix() + mnemonic + val;
    }

    @Override
    public int length() {
        return switch (mnemonic.toUpperCase()) {
            case "WORD" -> 3;
            case "BYTE" -> estimateByteLength(value);
            case "RESW" -> valueAsInt() * 3;
            case "RESB" -> valueAsInt();
            default -> 0;
        };
    }

    @Override
    public void enter(Code code) {
        // no-op: length already accounts for reservation/data sizes
    }

    @Override
    public void emitCode(ByteArrayOutputStream out, Code code) throws ParseException {
        switch (mnemonic.toUpperCase()) {
            case "WORD" -> emitWord(out, valueAsInt());
            case "BYTE" -> emitByteLiteral(out, value);
            case "RESW", "RESB" -> {
                // reservation: caller handles padding/gaps
            }
            default -> {
                // no emission
            }
        }
    }

    private int valueAsInt() {
        if (value instanceof Integer i) return i;
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private void emitWord(ByteArrayOutputStream out, int v) {
        out.write((v >> 16) & 0xFF);
        out.write((v >> 8) & 0xFF);
        out.write(v & 0xFF);
    }

    private void emitByteLiteral(ByteArrayOutputStream out, Object v) throws ParseException {
        if (v == null) return;
        String s = String.valueOf(v).trim();
        if (s.length() >= 3 && s.charAt(1) == '\'' && s.lastIndexOf("'") == s.length() - 1) {
            char type = Character.toUpperCase(s.charAt(0));
            String inside = s.substring(2, s.length() - 1);
            if (type == 'C') {
                for (char c : inside.toCharArray()) out.write((byte) c);
                return;
            } else if (type == 'X') {
                if (inside.length() % 2 != 0) inside = "0" + inside; // pad odd length
                try {
                    for (int i = 0; i < inside.length(); i += 2) {
                        int b = Integer.parseInt(inside.substring(i, i + 2), 16);
                        out.write(b & 0xFF);
                    }
                    return;
                } catch (NumberFormatException ex) {
                    throw new ParseException("Invalid hex literal: " + s);
                }
            }
        }

        try {
            int val = Integer.parseInt(s);
            out.write(val & 0xFF);
        } catch (NumberFormatException ex) {
            throw new ParseException("Unsupported BYTE literal: " + s);
        }
    }

    private int estimateByteLength(Object v) {
        if (v == null) return 0;
        String s = String.valueOf(v).trim();
        // handle C'EOF' style or X'F1' style crudely
        if (s.length() >= 3 && s.charAt(1) == '\'' && s.lastIndexOf("'") == s.length() - 1) {
            char type = Character.toUpperCase(s.charAt(0));
            String inside = s.substring(2, s.length() - 1);
            if (type == 'C') return inside.length();
            if (type == 'X') return (inside.length() + 1) / 2; // hex digits
        }
        return s.length();
    }
}
