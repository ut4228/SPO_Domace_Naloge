package assembler;

import java.util.Map;

public final class MnemonicOpcodes {
    private static final Map<String, Integer> OPC = Map.ofEntries(
            // subset of SIC/XE opcodes used in samples
            Map.entry("LDA", 0x00),
            Map.entry("LDX", 0x04),
            Map.entry("LDL", 0x08),
            Map.entry("STA", 0x0C),
            Map.entry("STX", 0x10),
            Map.entry("STL", 0x14),
            Map.entry("ADD", 0x18),
            Map.entry("SUB", 0x1C),
            Map.entry("MUL", 0x20),
            Map.entry("DIV", 0x24),
            Map.entry("COMP", 0x28),
        Map.entry("TIX", 0x2C),
        Map.entry("JEQ", 0x30),
        Map.entry("JGT", 0x34),
        Map.entry("JLT", 0x38),
            Map.entry("J", 0x3C),
            Map.entry("JSUB", 0x48),
            Map.entry("RSUB", 0x4C),
            Map.entry("LDCH", 0x50),
            Map.entry("STCH", 0x54),
        Map.entry("STSW", 0xE8),
        Map.entry("LPS", 0xD0),
        Map.entry("STI", 0xD4),
        Map.entry("SSK", 0xEC),
            Map.entry("TD", 0xE0),
            Map.entry("RD", 0xD8),
            Map.entry("WD", 0xDC),
        Map.entry("AND", 0x40),
        Map.entry("OR", 0x44),
        Map.entry("ADDF", 0x58),
        Map.entry("SUBF", 0x5C),
        Map.entry("MULF", 0x60),
        Map.entry("DIVF", 0x64),
        Map.entry("LDB", 0x68),
        Map.entry("LDS", 0x6C),
        Map.entry("LDF", 0x70),
        Map.entry("LDT", 0x74),
        Map.entry("STB", 0x78),
        Map.entry("STS", 0x7C),
        Map.entry("STF", 0x80),
        Map.entry("STT", 0x84),
        Map.entry("COMPF", 0x88),
            // format 1
            Map.entry("FIX", 0xC4),
            Map.entry("FLOAT", 0xC0),
            Map.entry("HIO", 0xF4),
            Map.entry("NORM", 0xC8),
            Map.entry("SIO", 0xF0),
            Map.entry("TIO", 0xF8),
            // format 2
            Map.entry("ADDR", 0x90),
            Map.entry("SUBR", 0x94),
            Map.entry("MULR", 0x98),
            Map.entry("DIVR", 0x9C),
            Map.entry("COMPR", 0xA0),
            Map.entry("CLEAR", 0xB4),
            Map.entry("TIXR", 0xB8),
            Map.entry("SHIFTL", 0xA4),
            Map.entry("SHIFTR", 0xA8),
        Map.entry("SVC", 0xB0),
        Map.entry("RMO", 0xAC)
    );

    public static int opcode(String mnemonic) {
        return OPC.getOrDefault(mnemonic.toUpperCase(), 0);
    }
}