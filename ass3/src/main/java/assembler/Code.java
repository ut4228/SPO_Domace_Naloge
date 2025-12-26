package assembler;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

public class Code {
    private String programName;
    private Integer startAddress;
    private final List<Node> nodes = new ArrayList<>();
    private int locctr = 0;
    private int next = 0; // PC-like: address of next instruction
    private Integer base = null;
    private final Map<String, Integer> symbols = new HashMap<>();
    private final Map<String, LiteralEntry> literalPool = new LinkedHashMap<>();
    private boolean resolved = false;

    public void setProgramName(String programName) {
        this.programName = programName;
    }

    public void setStartAddress(Integer startAddress) {
        this.startAddress = startAddress;
    }

    public List<Node> getNodes() {
        return nodes;
    }

    public String dumpSymbols() {
        StringBuilder sb = new StringBuilder();
        sb.append("Symbols:\n");
        symbols.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> sb.append(String.format("%s = %04X%n", e.getKey(), e.getValue())));
        return sb.toString();
    }

    public int getLocctr() {
        return locctr;
    }

    public int getNext() {
        return next;
    }

    public Integer getStartAddress() {
        return startAddress;
    }

    public Integer getBase() {
        return base;
    }

    public void setBase(Integer base) {
        this.base = base;
    }

    public void setLocctr(int loc) {
        this.locctr = loc;
    }

    public void setNext(int next) {
        this.next = next;
    }

    public void defineSymbol(String name, int address) throws ParseException {
        String key = name.toUpperCase();
        if (symbols.containsKey(key)) {
            throw new ParseException("Duplicate symbol: " + name);
        }
        symbols.put(key, address);
    }

    public Integer lookupSymbol(String name) {
        return symbols.get(name.toUpperCase());
    }

    public void begin() {
        locctr = (startAddress != null) ? startAddress : 0;
        next = locctr;
        base = null;
    }

    public void end() {
        // no-op for now; placeholder if cleanup is needed later
    }

    public void advance(int bytes) {
        locctr = next;
        next = locctr + bytes;
    }

    public void resolve() throws ParseException {
        symbols.clear();
        resolved = false;

        // Expand LTORG/pending literals into the node stream
        List<Node> expanded = new ArrayList<>();
        for (Node n : nodes) {
            expanded.add(n);
            if (isLtorg(n)) {
                expanded.addAll(takePendingLiterals());
            }
        }
        expanded.addAll(takePendingLiterals());
        nodes.clear();
        nodes.addAll(expanded);

        begin();
        // First pass: define symbols
        for (Node n : nodes) {
            n.activate(this);
            advance(n.length());
        }

        // Second pass: resolve operands
        begin();
        for (Node n : nodes) {
            n.enter(this);
            n.resolve(this);
            n.leave(this);
            advance(n.length());
        }
        end();
        resolved = true;
    }

    public void registerLiteral(String literalSpec) {
        if (literalSpec == null || literalSpec.isEmpty()) return;
        String key = literalSpec.toUpperCase();
        literalPool.putIfAbsent(key, new LiteralEntry(literalSpec));
    }

    private boolean isLtorg(Node n) {
        return n instanceof Directive d && d.getMnemonic().equalsIgnoreCase("LTORG");
    }

    private List<Node> takePendingLiterals() throws ParseException {
        List<Node> list = new ArrayList<>();
        for (LiteralEntry le : literalPool.values()) {
            if (le.emitted) continue;
            list.add(createLiteralStorage(le.spec));
            le.emitted = true;
        }
        return list;
    }

    private Node createLiteralStorage(String spec) throws ParseException {
        String s = spec.trim();
        String upper = s.toUpperCase();
        if (upper.matches("C'.*'" ) || upper.matches("X'.*'")) {
            return new Storage(s, "BYTE", s);
        }
        try {
            int val = Integer.decode(s);
            return new Storage(s, "WORD", val);
        } catch (NumberFormatException ex) {
            throw new ParseException("Unsupported literal: =" + spec);
        }
    }

    private static class LiteralEntry {
        final String spec;
        boolean emitted = false;
        LiteralEntry(String spec) { this.spec = spec; }
    }

    public int getProgramLength() {
        int origin = startAddress != null ? startAddress : 0;
        return next - origin;
    }

    public byte[] emitBinary() throws ParseException {
        if (!resolved) resolve();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int origin = startAddress != null ? startAddress : 0;
        begin();
        for (Node n : nodes) {
            n.enter(this);
            int targetOffset = getLocctr() - origin;
            while (out.size() < targetOffset) {
                out.write(0);
            }
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            n.emitCode(buf, this);
            out.writeBytes(buf.toByteArray());
            n.leave(this);
            advance(n.length());
        }
        end();
        return out.toByteArray();
    }

    public String emitObject() throws ParseException {
        if (!resolved) resolve();
        StringBuilder sb = new StringBuilder();
        String name = programName != null ? programName : "NONAME";
        if (name.length() > 6) name = name.substring(0, 6);
        name = String.format("%-6s", name);
        int origin = startAddress != null ? startAddress : 0;
        int length = getProgramLength();
        sb.append(String.format("H%s%06X%06X%n", name, origin, length));

        begin();
        int recordStart = -1;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        List<String> mrecords = new ArrayList<>();
        for (Node n : nodes) {
            n.enter(this);
            int addr = getLocctr();
            ByteArrayOutputStream nodeBytes = new ByteArrayOutputStream();
            n.emitCode(nodeBytes, this);
            byte[] bytes = nodeBytes.toByteArray();
            boolean reserves = (n instanceof Storage s) &&
                    (s.getMnemonic().equalsIgnoreCase("RESB") || s.getMnemonic().equalsIgnoreCase("RESW"));

            if (n instanceof InstructionF4 f4 && f4.getTargetSymbol() != null) {
                // M record: start at address+1, length 5 nibbles (20 bits)
                mrecords.add(String.format("M%06X05+%s", addr + 1, f4.getTargetSymbol()));
            }

            if ((bytes.length == 0 && n.length() > 0) || reserves) {
                if (buffer.size() > 0) {
                    appendTextRecord(sb, recordStart, buffer.toByteArray());
                    buffer.reset();
                    recordStart = -1;
                }
            } else if (bytes.length > 0) {
                if (recordStart == -1) {
                    recordStart = addr;
                }
                int expectedAddr = recordStart + buffer.size();
                if (addr != expectedAddr || buffer.size() + bytes.length > 30) {
                    if (buffer.size() > 0) {
                        appendTextRecord(sb, recordStart, buffer.toByteArray());
                        buffer.reset();
                    }
                    recordStart = addr;
                }
                buffer.writeBytes(bytes);
            }
            n.leave(this);
            advance(n.length());
        }
        if (buffer.size() > 0) {
            appendTextRecord(sb, recordStart, buffer.toByteArray());
        }
        int entry = startAddress != null ? startAddress : 0;
        for (String m : mrecords) {
            sb.append(m).append('\n');
        }
        sb.append(String.format("E%06X%n", entry));
        end();
        return sb.toString();
    }

    public String emitListing() throws ParseException {
        if (!resolved) resolve();
        StringBuilder sb = new StringBuilder();
        begin();
        for (Node n : nodes) {
            n.enter(this);
            int addr = getLocctr();
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            n.emitCode(buf, this);
            byte[] bytes = buf.toByteArray();
            String hex = toHex(bytes, Math.min(6, bytes.length));
            sb.append(String.format("%04X %-12s %s%n", addr, hex, n.pretty(0)));
            n.leave(this);
            advance(n.length());
        }
        end();
        return sb.toString();
    }

    private String toHex(byte[] data, int maxBytes) {
        StringBuilder h = new StringBuilder();
        for (int i = 0; i < data.length && i < maxBytes; i++) {
            h.append(String.format("%02X ", data[i] & 0xFF));
        }
        return h.toString().trim();
    }

    private void appendTextRecord(StringBuilder sb, int start, byte[] data) {
        sb.append(String.format("T%06X%02X", start, data.length));
        for (byte b : data) {
            sb.append(String.format("%02X", b & 0xFF));
        }
        sb.append('\n');
    }

    public String dumpCode(byte[] data) {
        StringBuilder sb = new StringBuilder();
        int origin = startAddress != null ? startAddress : 0;
        for (int i = 0; i < data.length; i += 16) {
            sb.append(String.format("%04X: ", origin + i));
            int j = 0;
            for (; j < 16 && (i + j) < data.length; j++) {
                sb.append(String.format("%02X ", data[i + j] & 0xFF));
            }
            for (; j < 16; j++) sb.append("   ");
            sb.append(" | ");
            for (j = 0; j < 16 && (i + j) < data.length; j++) {
                int v = data[i + j] & 0xFF;
                sb.append((v >= 32 && v < 127) ? (char) v : '.');
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    public String pretty() {
        StringBuilder header = new StringBuilder();
        if (programName != null) {
            header.append("Program: ").append(programName);
        }
        if (startAddress != null) {
            if (header.length() > 0) header.append(" | ");
            header.append("Start: ").append(String.format("0x%X", startAddress));
        }
        if (header.length() == 0) {
            header.append("Program");
        }
        String body = nodes.stream().map(n -> n.pretty(2)).collect(Collectors.joining("\n"));
        if (body.isEmpty()) return header.toString();
        return header + "\n" + body;
    }
}
