import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Locale;
import java.util.Scanner;

/**
 * Simple CLI wrapper around the SIC/XE Machine simulator.
 */
public class Simulator {
    private final Machine machine;
    private boolean quit;
    private int lastWordCount = 8;
    private Machine.Snapshot undoSnapshot;
    private String undoLabel;

    public Simulator() {
        this.machine = new Machine();
    }

    public static void main(String[] args) {
        Simulator simulator = new Simulator();
        simulator.run(args);
    }

    private void run(String[] args) {
        if (args.length > 0) {
            loadProgram(args[0]);
        }

        System.out.println("SIC/XE simulator ready. Type 'help' for a list of commands.");
        try (Scanner scanner = new Scanner(System.in)) {
            while (!quit) {
                System.out.print("sicxe> ");
                if (!scanner.hasNextLine()) {
                    break;
                }
                String line = scanner.nextLine().trim();
                if (line.isEmpty()) {
                    continue;
                }
                handleCommand(line);
            }
        }
        machine.stop();
    }

    private void handleCommand(String line) {
        String[] parts = line.split("\\s+");
        String cmd = parts[0].toLowerCase(Locale.ROOT);
        switch (cmd) {
            case "help":
                printHelp();
                break;
            case "load":
                if (parts.length < 2) {
                    System.out.println("Usage: load <path>");
                } else {
                    loadProgram(parts[1]);
                }
                break;
            case "regs":
                printRegisters();
                break;
            case "status":
                printStatus();
                break;
            case "pc":
                System.out.printf("PC = %06X%n", machine.getPC());
                break;
            case "step":
                performSingleStep();
                break;
            case "run":
                runSteps(parts);
                break;
            case "start":
                machine.start();
                System.out.println("Automatic execution started.");
                break;
            case "stop":
                machine.stop();
                System.out.println("Automatic execution stopped.");
                break;
            case "speed":
                setSpeed(parts);
                break;
            case "vars":
            case "memvars":
                dumpVariableWords(parts);
                break;
            case "undo":
                undoLastChange();
                break;
            case "clear":
                captureUndoPoint("clear");
                resetMachine();
                System.out.println("Machine state cleared.");
                break;
            case "exit":
            case "quit":
                quit = true;
                break;
            default:
                System.out.println("Unknown command. Type 'help' for assistance.");
                break;
        }
    }

    private void printHelp() {
        System.out.println("Commands:");
        System.out.println("  help              Show this help message");
        System.out.println("  load <path>       Load an object file and reset the machine");
        System.out.println("  regs              Show register contents");
        System.out.println("  status            Show execution status and PC");
        System.out.println("  pc                Show current program counter");
        System.out.println("  step              Execute a single instruction");
        System.out.println("  run [n]           Execute n instructions (default 1)");
        System.out.println("  start             Start automatic execution");
        System.out.println("  stop              Stop automatic execution");
        System.out.println("  speed <kHz>       Set automatic execution speed");
        System.out.println("  vars [n]          Dump last n words of loaded program (data area)");
        System.out.println("  undo              Restore the previous machine snapshot");
        System.out.println("  clear             Reset registers and memory");
        System.out.println("  quit/exit         Exit the simulator");
    }

    private void loadProgram(String path) {
        captureUndoPoint("load " + path);
        machine.stop();
        resetMachine();
        try (Reader reader = new FileReader(path)) {
            if (machine.loadSection(reader)) {
                System.out.printf("Loaded %s (PC=%06X)%n", path, machine.getPC());
            } else {
                System.out.printf("Failed to load %s%n", path);
            }
        } catch (IOException ex) {
            System.out.printf("Error loading %s: %s%n", path, ex.getMessage());
        }
    }

    private void resetMachine() {
        machine.stop();
        machine.clearMemory();
        machine.clearLoadInfo();
        machine.setA(0);
        machine.setX(0);
        machine.setL(0);
        machine.setB(0);
        machine.setS(0);
        machine.setT(0);
        machine.setF(0.0);
        machine.setPC(0);
        machine.setSW(0);
    }

    private void runSteps(String[] parts) {
        int steps = 1;
        if (parts.length >= 2) {
            try {
                steps = parseNumber(parts[1]);
            } catch (NumberFormatException ex) {
                System.out.println("Invalid step count.");
                return;
            }
        }
        if (steps <= 0) {
            System.out.println("Step count must be positive.");
            return;
        }
        if (machine.isRunning()) {
            System.out.println("Stop automatic execution before running manual steps.");
            return;
        }
        captureUndoPoint(steps == 1 ? "single step" : ("run " + steps + " steps"));
        for (int i = 0; i < steps; i++) {
            machine.step();
        }
        printStatus();
    }

    private void performSingleStep() {
        if (machine.isRunning()) {
            System.out.println("Stop automatic execution before stepping manually.");
            return;
        }
        captureUndoPoint("single step");
        int startPC = machine.getPC();
        machine.step();
        int length = machine.getLastInstructionLength();
        if (length <= 0) {
            printStatus();
            return;
        }
        int[] bytes = readInstructionBytes(startPC, length);
        String disasm = formatInstructionDescription(bytes, length, machine.getPC());
        String byteDump = formatInstructionBytes(bytes, length);
        System.out.printf(
                "STEP %06X -> %06X : %s\nbytes %s \n%s%n",
                startPC,
                machine.getPC(),
                disasm,
                byteDump,
                formatRegisterSummary());
        printStatus();
    }

    private int[] readInstructionBytes(int startAddress, int length) {
        int[] data = new int[length];
        for (int i = 0; i < length; i++) {
            int addr = maskAddress(startAddress + i);
            data[i] = machine.getByte(addr);
        }
        return data;
    }

    private String formatInstructionBytes(int[] bytes, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(String.format("%02X", bytes[i] & 0xFF));
        }
        return sb.toString();
    }

    private String formatInstructionDescription(int[] bytes, int length, int nextPC) {
        if (length <= 0) {
            return "";
        }
        int opcode = bytes[0] & 0xFF;
        if (Machine.isFormat1(opcode)) {
            return Machine.opcodeToMnemonic(opcode);
        }
        if (Machine.isFormat2(opcode) && length >= 2) {
            String mnemonic = Machine.opcodeToMnemonic(opcode);
            return String.format("%s %s", mnemonic, formatFormat2Operand(bytes[1], mnemonic));
        }
        int baseOpcode = opcode & 0xFC;
        if (!Machine.isFormat34(baseOpcode) || length < 3) {
            return String.format("BYTE %02X", opcode);
        }
        String mnemonic = Machine.opcodeToMnemonic(baseOpcode);
        if ("RSUB".equals(mnemonic)) {
            return mnemonic;
        }
        return String.format("%s %s", mnemonic, formatFormat34Operand(bytes, length, nextPC));
    }

    private String formatFormat2Operand(int operand, String mnemonic) {
        int r1 = (operand >> 4) & 0x0F;
        int r2 = operand & 0x0F;
        switch (mnemonic) {
            case "CLEAR":
            case "TIXR":
                return registerName(r1);
            case "SHIFTL":
            case "SHIFTR":
                return String.format("%s,%d", registerName(r1), r2);
            case "SVC":
                return String.valueOf(r1);
            default:
                return String.format("%s,%s", registerName(r1), registerName(r2));
        }
    }

    private String registerName(int code) {
        switch (code) {
            case 0:
                return "A";
            case 1:
                return "X";
            case 2:
                return "L";
            case 3:
                return "B";
            case 4:
                return "S";
            case 5:
                return "T";
            case 6:
                return "F";
            case 8:
                return "PC";
            case 9:
                return "SW";
            default:
                return "R" + code;
        }
    }

    private String formatFormat34Operand(int[] bytes, int length, int nextPC) {
        int first = bytes[0] & 0xFF;
        int ni = first & 0x03;
        boolean n = (ni & 0x02) != 0;
        boolean i = (ni & 0x01) != 0;
        int xbpe = (bytes[1] >> 4) & 0x0F;
        boolean x = (xbpe & 0x08) != 0;
        boolean b = (xbpe & 0x04) != 0;
        boolean p = (xbpe & 0x02) != 0;
        boolean e = length == 4;

        int operandRaw;
        if (e) {
            operandRaw = ((bytes[1] & 0x0F) << 16) | ((bytes[2] & 0xFF) << 8) | (bytes[3] & 0xFF);
        } else {
            operandRaw = ((bytes[1] & 0x0F) << 8) | (bytes[2] & 0xFF);
        }

        int baseAddress;
        if (e) {
            baseAddress = operandRaw & 0xFFFFF;
        } else {
            int disp = operandRaw & 0x0FFF;
            int signedDisp = signExtend(disp, 12);
            if (p) {
                baseAddress = maskAddress(nextPC + signedDisp);
            } else if (b) {
                baseAddress = maskAddress(machine.getB() + signedDisp);
            } else {
                baseAddress = disp;
            }
        }

        if (!n && i) {
            int value;
            if (e || b || p) {
                value = baseAddress;
            } else {
                int bits = e ? 20 : 12;
                value = signExtend(operandRaw, bits);
            }
            if (value < 0) {
                return "#" + value;
            }
            int width = e ? 5 : 3;
            if (e || b || p) {
                width = 6;
            }
            return String.format("#%0" + width + "X", value);
        }

        StringBuilder sb = new StringBuilder();
        String prefix = (n && !i) ? "@" : "";
        sb.append(prefix).append(String.format("%06X", baseAddress));

        if (x) {
            int indexed = maskAddress(baseAddress + machine.getX());
            sb.append(",X -> ").append(String.format("%06X", indexed));
        }

        if (n && !i) {
            int pointer = machine.getWord(baseAddress) & 0xFFFFFF;
            sb.append(" -> ").append(String.format("%06X", pointer));
        }

        String flags = formatAddressingFlags(x, b, p, e);
        if (!flags.isEmpty()) {
            sb.append(" [").append(flags).append(']');
        }
        return sb.toString();
    }

    private String formatAddressingFlags(boolean x, boolean b, boolean p, boolean e) {
        StringBuilder sb = new StringBuilder();
        if (p) {
            sb.append("PC");
        }
        if (b) {
            if (sb.length() > 0) {
                sb.append('+');
            }
            sb.append("B");
        }
        if (e) {
            if (sb.length() > 0) {
                sb.append('+');
            }
            sb.append("EXT");
        }
        if (x) {
            if (sb.length() > 0) {
                sb.append('+');
            }
            sb.append("X");
        }
        return sb.toString();
    }

    private int maskAddress(int value) {
        return value & Machine.MAX_ADDRESS;
    }

    private int signExtend(int value, int bits) {
        int mask = (1 << bits) - 1;
        value &= mask;
        int signBit = 1 << (bits - 1);
        if ((value & signBit) != 0) {
            value |= ~mask;
        }
        return value;
    }

    private String formatRegisterSummary() {
        return String.format(
                "A=%06X X=%06X B=%06X L=%06X S=%06X T=%06X SW=%s",
                machine.getA(),
                machine.getX(),
                machine.getB(),
                machine.getL(),
                machine.getS(),
                machine.getT(),
                interpretCondition());
    }

    private void setSpeed(String[] parts) {
        if (parts.length < 2) {
            System.out.println("Usage: speed <kHz>");
            return;
        }
        try {
            int value = parseNumber(parts[1]);
            machine.setSpeed(value);
            System.out.printf("Speed set to %d kHz.%n", value);
        } catch (NumberFormatException ex) {
            System.out.println("Invalid speed value.");
        } catch (IllegalArgumentException ex) {
            System.out.println(ex.getMessage());
        }
    }

    private void dumpVariableWords(String[] parts) {
        int count = lastWordCount;
        int nameStartIndex = 1;
        if (parts.length >= 2) {
            Integer maybeCount = tryParseNumber(parts[1]);
            if (maybeCount != null) {
                count = maybeCount;
                nameStartIndex = 2;
            }
        }

        java.util.List<String> labels = java.util.Collections.emptyList();
        if (parts.length > nameStartIndex) {
            labels = new java.util.ArrayList<>();
            for (int i = nameStartIndex; i < parts.length; i++) {
                labels.add(parts[i]);
            }
        }

        int loadLength = machine.getLastLoadLength();
        if (loadLength <= 0) {
            System.out.println("No program loaded.");
            return;
        }
        if (count <= 0) {
            System.out.println("Count must be positive.");
            return;
        }

        int loadStart = machine.getLastLoadStart();
        long end = (long) loadStart + (long) loadLength;
        int totalBytes = count * 3;
        int start = (int) Math.max(loadStart, end - totalBytes);
        start = Math.max(loadStart, start);
        lastWordCount = count;
        System.out.printf("Data region (last %d words of program):%n", count);
        printWordRange(start, count, labels);
    }

    private void printWordRange(int start, int count, java.util.List<String> labels) {
        for (int i = 0; i < count; i++) {
            int addr = start + i * 3;
            if (addr > Machine.MAX_ADDRESS - 2) {
                System.out.println("Reached end of memory.");
                break;
            }
            int word = machine.getWord(addr);
            int signed = toSigned24(word);
            String label = (labels != null && i < labels.size()) ? labels.get(i) : null;
            if (label != null) {
                System.out.printf("%06X: %06X (%d)  %s%n", addr, word, signed, label);
            } else {
                System.out.printf("%06X: %06X (%d)\n", addr, word, signed);
            }
        }
    }

    private void printRegisters() {
        System.out.printf("A : %06X    X : %06X    L : %06X%n",
                machine.getA(), machine.getX(), machine.getL());
        System.out.printf("B : %06X    S : %06X    T : %06X%n",
                machine.getB(), machine.getS(), machine.getT());
        System.out.printf("PC: %06X    SW: %02X (%s)%n",
                machine.getPC(), machine.getSW() & 0xFF, interpretCondition());
    }

    private void printStatus() {
        System.out.printf("PC=%06X  running=%s  speed=%d kHz  CC=%s%n",
                machine.getPC(), machine.isRunning(), machine.getSpeed(), interpretCondition());
    }

    private String interpretCondition() {
        int sw = machine.getSW() & 0xFF;
        if (sw == 0x00) {
            return "LT";
        }
        if (sw == 0x40) {
            return "EQ";
        }
        if (sw == 0x80) {
            return "GT";
        }
        return String.format("0x%02X", sw);
    }

    private int parseNumber(String token) {
        return Integer.decode(token);
    }

    private static int toSigned24(int value) {
        int masked = value & 0xFFFFFF;
        if ((masked & 0x800000) != 0) {
            masked -= 1 << 24;
        }
        return masked;
    }

    private Integer tryParseNumber(String token) {
        try {
            return Integer.decode(token);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void captureUndoPoint(String label) {
        undoSnapshot = machine.createSnapshot();
        undoLabel = label;
    }

    private void undoLastChange() {
        if (undoSnapshot == null) {
            System.out.println("No undo information available yet.");
            return;
        }
        machine.restoreSnapshot(undoSnapshot);
        System.out.printf("State restored (%s).%n", undoLabel == null ? "previous action" : undoLabel);
        undoSnapshot = null;
        undoLabel = null;
        printStatus();
    }

}
