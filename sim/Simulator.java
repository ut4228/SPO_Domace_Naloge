import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

/**
 * Simple CLI wrapper around the SIC/XE Machine simulator.
 */
public class Simulator {
    private final Machine machine;
    private boolean quit;
    private int lastMemStart = 0;
    private int lastMemLength = 64;
    private int lastWordStart = 0;
    private int lastWordCount = 8;
    private final Map<Integer, String> labels = new LinkedHashMap<>();

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
                machine.step();
                printStatus();
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
            case "mem":
                dumpMemory(parts);
                break;
            case "memw":
                dumpWords(parts);
                break;
            case "vars":
            case "memvars":
                dumpVariableWords(parts);
                break;
            case "namevars":
                nameLastVars(parts);
                break;
            case "label":
                addLabel(parts);
                break;
            case "unlabel":
                removeLabel(parts);
                break;
            case "labels":
                listLabels();
                break;
            case "clear":
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
        System.out.println("  mem [addr] [len]  Dump memory (defaults to last range)");
        System.out.println("  memw [addr] [n]   Dump n SIC/XE words with hex+decimal output");
        System.out.println("  vars [n]          Dump last n words of loaded program (data area)");
        System.out.println("  namevars <names>  Assign names to the current vars range");
        System.out.println("  label <addr> <name>   Assign a name to a specific address");
        System.out.println("  unlabel <addr|name>   Remove a label by address or name");
        System.out.println("  labels            List all labels");
        System.out.println("  clear             Reset registers and memory");
        System.out.println("  quit/exit         Exit the simulator");
    }

    private void loadProgram(String path) {
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
        labels.clear();
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
        for (int i = 0; i < steps; i++) {
            machine.step();
        }
        printStatus();
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

    private void dumpMemory(String[] parts) {
        int start = lastMemStart;
        int length = lastMemLength;

        if (parts.length >= 2) {
            try {
                start = parseNumber(parts[1]);
            } catch (NumberFormatException ex) {
                System.out.println("Invalid address.");
                return;
            }
        }

        if (parts.length >= 3) {
            try {
                length = parseNumber(parts[2]);
            } catch (NumberFormatException ex) {
                System.out.println("Invalid length.");
                return;
            }
        }

        if (parts.length == 1) {
            System.out.printf("Using last memory range %06X len %d bytes.%n", start, length);
        }
        if (start < 0 || start > Machine.MAX_ADDRESS) {
            System.out.println("Address out of range.");
            return;
        }
        if (length <= 0) {
            System.out.println("Length must be positive.");
            return;
        }
        lastMemStart = start;
        lastMemLength = length;
        int end = Math.min(Machine.MAX_ADDRESS, start + length - 1);
        for (int addr = start; addr <= end; addr += 16) {
            System.out.printf("%06X:", addr);
            int lineEnd = Math.min(end, addr + 15);
            for (int current = addr; current <= lineEnd; current++) {
                int value = machine.getByte(current);
                System.out.printf(" %02X", value);
            }
            System.out.println();
        }
    }

    private void dumpWords(String[] parts) {
        int start = lastWordStart;
        int count = lastWordCount;

        if (parts.length >= 2) {
            try {
                start = parseNumber(parts[1]);
            } catch (NumberFormatException ex) {
                System.out.println("Invalid address.");
                return;
            }
        }

        if (parts.length >= 3) {
            try {
                count = parseNumber(parts[2]);
            } catch (NumberFormatException ex) {
                System.out.println("Invalid word count.");
                return;
            }
        }

        if (parts.length == 1) {
            System.out.printf("Using last word range %06X count %d.%n", start, count);
        }

        if (start < 0 || start > Machine.MAX_ADDRESS) {
            System.out.println("Address out of range.");
            return;
        }
        if (count <= 0) {
            System.out.println("Count must be positive.");
            return;
        }

        lastWordStart = start;
        lastWordCount = count;

        printWordRange(start, count);
    }

    private void dumpVariableWords(String[] parts) {
        int count = lastWordCount;
        int nameIndex = 1;
        if (parts.length >= 2) {
            Integer maybeCount = tryParseNumber(parts[1]);
            if (maybeCount != null) {
                count = maybeCount;
                nameIndex = 2;
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
        lastWordStart = start;
        lastWordCount = count;
        System.out.printf("Data region (last %d words of program):%n", count);
        assignNamesFrom(parts, nameIndex, start, count);
        printWordRange(start, count);
    }

    private void printWordRange(int start, int count) {
        for (int i = 0; i < count; i++) {
            int addr = start + i * 3;
            if (addr > Machine.MAX_ADDRESS - 2) {
                System.out.println("Reached end of memory.");
                break;
            }
            int word = machine.getWord(addr);
            int signed = toSigned24(word);
            String label = labels.get(addr);
            if (label != null) {
                System.out.printf("%06X: %06X (%d) [%s]\n", addr, word, signed, label);
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

    private void assignNamesFrom(String[] parts, int nameIndex, int start, int count) {
        if (parts.length <= nameIndex) {
            return;
        }
        int names = parts.length - nameIndex;
        int assign = Math.min(names, count);
        for (int i = 0; i < assign; i++) {
            int addr = start + i * 3;
            labels.put(addr, parts[nameIndex + i]);
        }
        if (names > count) {
            System.out.println("Warning: extra names ignored.");
        }
    }

    private void nameLastVars(String[] parts) {
        if (parts.length < 2) {
            System.out.println("Usage: namevars <name1> [name2 ...]");
            return;
        }
        if (lastWordCount <= 0) {
            System.out.println("Run vars first to capture a data range.");
            return;
        }
        assignNamesFrom(parts, 1, lastWordStart, lastWordCount);
    }

    private void addLabel(String[] parts) {
        if (parts.length < 3) {
            System.out.println("Usage: label <addr> <name>");
            return;
        }
        int addr;
        try {
            addr = parseNumber(parts[1]);
        } catch (NumberFormatException ex) {
            System.out.println("Invalid address.");
            return;
        }
        labels.put(addr, parts[2]);
        System.out.printf("Label %s assigned to %06X.%n", parts[2], addr);
    }

    private void removeLabel(String[] parts) {
        if (parts.length < 2) {
            System.out.println("Usage: unlabel <addr|name>");
            return;
        }
        String token = parts[1];
        Integer addr = tryParseNumber(token);
        if (addr != null) {
            if (labels.remove(addr) != null) {
                System.out.printf("Removed label at %06X.%n", addr);
            } else {
                System.out.println("No label defined at that address.");
            }
            return;
        }
        boolean removed = false;
        java.util.Iterator<Map.Entry<Integer, String>> iterator = labels.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, String> entry = iterator.next();
            if (entry.getValue().equals(token)) {
                iterator.remove();
                removed = true;
            }
        }
        if (removed) {
            System.out.printf("Removed label(s) named %s.%n", token);
        } else {
            System.out.println("No label with that name.");
        }
    }

    private void listLabels() {
        if (labels.isEmpty()) {
            System.out.println("No labels defined.");
            return;
        }
        labels.forEach((addr, name) -> System.out.printf("%06X -> %s%n", addr, name));
    }
}
