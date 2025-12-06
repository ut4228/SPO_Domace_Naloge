import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Core SIC/XE machine simulator state. Manages registers, memory and devices.
 */
public class Machine {
    public static final int MAX_ADDRESS = (1 << 20) - 1; // 20-bit address space (1 MiB)
    public static final int MEMORY_SIZE = MAX_ADDRESS + 1;
    public static final int DEVICE_COUNT = 256;

    private final byte[] memory = new byte[MEMORY_SIZE];
    private final Device[] devices = new Device[DEVICE_COUNT];
    private final Object executionLock = new Object();

    private int regA;
    private int regX;
    private int regL;
    private int regB;
    private int regS;
    private int regT;
    private double regF;
    private int regPC;
    private int regSW;
    private int lastNi;
    private int lastXbpe;
    private boolean lastExtended;
    private int lastOperand;
    private int lastInstructionLength;
    private int lastLoadStart;
    private int lastLoadLength;

    private Timer timer;
    private volatile boolean running;
    private int speedKHz = 1;

    private static final long TIMER_PERIOD_MS = 1L;
    private static final int CC_LESS = 0x00;
    private static final int CC_EQUAL = 0x40;
    private static final int CC_GREATER = 0x80;

    public Machine() {
        initialiseDevices();
    }

    private void initialiseDevices() {
        devices[0] = new InputDevice(System.in);
        devices[1] = new OutputDevice(System.out);
        devices[2] = new OutputDevice(System.err);
        for (int i = 3; i < DEVICE_COUNT; i++) {
            setDevice(i, new FileDevice(String.format("device%03d.dat", i)));
        }
    }

    public int getA() {
        return regA;
    }

    public void setA(int val) {
        regA = maskWord(val);
    }

    public int getX() {
        return regX;
    }

    public void setX(int val) {
        regX = maskWord(val);
    }

    public int getL() {
        return regL;
    }

    public void setL(int val) {
        regL = maskWord(val);
    }

    public int getB() {
        return regB;
    }

    public void setB(int val) {
        regB = maskWord(val);
    }

    public int getS() {
        return regS;
    }

    public void setS(int val) {
        regS = maskWord(val);
    }

    public int getT() {
        return regT;
    }

    public void setT(int val) {
        regT = maskWord(val);
    }

    public double getF() {
        return regF;
    }

    public void setF(double val) {
        regF = val;
    }

    public int getPC() {
        return regPC;
    }

    public void setPC(int val) {
        regPC = maskAddress(val);
    }

    public int getSW() {
        return regSW;
    }

    public void setSW(int val) {
        regSW = val & 0xFF;
    }

    public int getReg(int reg) {
        switch (reg) {
            case 0:
                return getA();
            case 1:
                return getX();
            case 2:
                return getL();
            case 3:
                return getB();
            case 4:
                return getS();
            case 5:
                return getT();
            case 6:
                throw new UnsupportedOperationException("Use getF() for floating-point register access.");
            case 8:
                return getPC();
            case 9:
                return getSW();
            default:
                throw new IllegalArgumentException("Invalid register index: " + reg);
        }
    }

    public void setReg(int reg, int val) {
        switch (reg) {
            case 0:
                setA(val);
                break;
            case 1:
                setX(val);
                break;
            case 2:
                setL(val);
                break;
            case 3:
                setB(val);
                break;
            case 4:
                setS(val);
                break;
            case 5:
                setT(val);
                break;
            case 6:
                throw new UnsupportedOperationException("Use setF(double) for floating-point register access.");
            case 8:
                setPC(val);
                break;
            case 9:
                setSW(val);
                break;
            default:
                throw new IllegalArgumentException("Invalid register index: " + reg);
        }
    }

    public int getByte(int addr) {
        checkAddressRange(addr, 1);
        return memory[addr] & 0xFF;
    }

    public void setByte(int addr, int val) {
        checkAddressRange(addr, 1);
        memory[addr] = (byte) (val & 0xFF);
    }

    public int getWord(int addr) {
        checkAddressRange(addr, 3);
        int b1 = memory[addr] & 0xFF;
        int b2 = memory[addr + 1] & 0xFF;
        int b3 = memory[addr + 2] & 0xFF;
        return (b1 << 16) | (b2 << 8) | b3;
    }

    public void setWord(int addr, int val) {
        checkAddressRange(addr, 3);
        int masked = maskWord(val);
        memory[addr] = (byte) ((masked >> 16) & 0xFF);
        memory[addr + 1] = (byte) ((masked >> 8) & 0xFF);
        memory[addr + 2] = (byte) (masked & 0xFF);
    }

    public double getFloat(int addr) {
        checkAddressRange(addr, 6);
        long raw = 0L;
        for (int i = 0; i < 6; i++) {
            raw = (raw << 8) | (memory[addr + i] & 0xFF);
        }
        return SicXeFloat.fromRaw(raw);
    }

    public void setFloat(int addr, double val) {
        checkAddressRange(addr, 6);
        long raw = SicXeFloat.toRaw(val);
        for (int i = 5; i >= 0; i--) {
            memory[addr + i] = (byte) (raw & 0xFF);
            raw >>= 8;
        }
    }

    public void clearMemory() {
        Arrays.fill(memory, (byte) 0);
    }

    public Device getDevice(int num) {
        checkDeviceNumber(num);
        return devices[num];
    }

    public void setDevice(int num, Device device) {
        checkDeviceNumber(num);
        devices[num] = device;
    }

    public boolean loadSection(Reader reader) {
        BufferedReader buffered = reader instanceof BufferedReader
                ? (BufferedReader) reader
                : new BufferedReader(reader);

        boolean headerSeen = false;
        int startAddress = 0;
        int entryAddress = -1;
        int programLength = 0;

        try {
            String line;
            while ((line = buffered.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                char recordType = Character.toUpperCase(line.charAt(0));
                String payload = line.length() > 1 ? line.substring(1) : "";
                String[] fields = splitFields(payload);

                switch (recordType) {
                    case 'H': {
                        String startHex = null;
                        String lengthHex = null;
                        if (fields.length >= 3) {
                            startHex = fields[1];
                            lengthHex = fields[2];
                        } else if (payload.replace(" ", "").length() >= 18) {
                            try (StringReader sr = new StringReader(payload.replace("^", ""))) {
                                Utils.readString(sr, 6); // program name ignored for now
                                startHex = Utils.readString(sr, 6);
                                lengthHex = Utils.readString(sr, 6);
                            }
                        }

                        if (startHex == null || lengthHex == null) {
                            System.err.println("Malformed header record: " + line);
                            return false;
                        }

                        startAddress = parseHex(startHex, "header start address");
                        programLength = parseHex(lengthHex, "program length");
                        lastLoadStart = startAddress;
                        lastLoadLength = programLength;
                        if (programLength > 0) {
                            try {
                                checkAddressRange(startAddress, programLength);
                            } catch (IllegalArgumentException ex) {
                                invalidAddressing();
                                return false;
                            }
                        }
                        headerSeen = true;
                        break;
                    }
                    case 'T': {
                        if (!headerSeen) {
                            System.err.println("Text record encountered before header.");
                            return false;
                        }
                        if (fields.length < 3) {
                            System.err.println("Malformed text record: " + line);
                            return false;
                        }

                        int recordAddress = parseHex(fields[0], "text record start address");
                        int byteCount = parseHex(fields[1], "text record length");
                        StringBuilder dataBuilder = new StringBuilder();
                        for (int i = 2; i < fields.length; i++) {
                            dataBuilder.append(fields[i]);
                        }
                        String data = dataBuilder.toString();
                        if (data.length() < byteCount * 2) {
                            System.err.println("Text record shorter than expected: " + line);
                            return false;
                        }

                        try {
                            checkAddressRange(recordAddress, Math.max(byteCount, 1));
                        } catch (IllegalArgumentException ex) {
                            invalidAddressing();
                            return false;
                        }

                        try (StringReader dataReader = new StringReader(data)) {
                            for (int i = 0; i < byteCount; i++) {
                                int value = Utils.readByte(dataReader);
                                try {
                                    setByte(recordAddress + i, value);
                                } catch (IllegalArgumentException ex) {
                                    invalidAddressing();
                                    return false;
                                }
                            }
                        }
                        break;
                    }
                    case 'E': {
                        if (fields.length >= 1 && !fields[0].isEmpty()) {
                            entryAddress = parseHex(fields[0], "entry point");
                        } else {
                            entryAddress = startAddress;
                        }
                        break;
                    }
                    case 'M':
                    case 'R':
                    case 'D':
                    case 'C':
                        // Relocation and other records are ignored by the absolute loader.
                        break;
                    case '.':
                        // Comment line, skip it.
                        break;
                    default:
                        System.err.println("Unknown record type '" + recordType + "': " + line);
                        return false;
                }
            }
        } catch (IOException ex) {
            System.err.println("Failed to load section: " + ex.getMessage());
            return false;
        } catch (IllegalArgumentException ex) {
            System.err.println(ex.getMessage());
            return false;
        }

        if (!headerSeen) {
            System.err.println("Missing header record in object file.");
            return false;
        }

        if (entryAddress < 0) {
            entryAddress = startAddress;
        }

        try {
            checkAddressRange(entryAddress, 1);
        } catch (IllegalArgumentException ex) {
            invalidAddressing();
            return false;
        }

        setPC(entryAddress);
        return true;
    }

    public int getLastLoadStart() {
        return lastLoadStart;
    }

    public int getLastLoadLength() {
        return lastLoadLength;
    }

    public void clearLoadInfo() {
        lastLoadStart = 0;
        lastLoadLength = 0;
    }

    public void notImplemented(String mnemonic) {
        System.err.println("Instruction not implemented: " + mnemonic);
    }

    public void invalidOpcode(int opcode) {
        System.err.printf("Invalid opcode: 0x%02X%n", opcode & 0xFF);
    }

    public void invalidAddressing() {
        System.err.println("Invalid addressing mode encountered.");
    }

    public int fetch() {
        int pc = getPC();
        int value = getByte(pc);
        setPC(pc + 1);
        return value;
    }

    public void execute() {
        synchronized (executionLock) {
            executeInstruction();
        }
    }

    private void executeInstruction() {
        int first = fetch();
        lastNi = 0;
        lastXbpe = 0;
        lastExtended = false;
        lastOperand = 0;
        lastInstructionLength = 1;

        if (isFormat1(first)) {
            boolean handled = execF1(first);
            if (!handled) {
                notImplemented(opcodeToMnemonic(first));
            }
            return;
        }

        if (isFormat2(first)) {
            int operand = fetch();
            lastOperand = operand;
            lastInstructionLength = 2;
            boolean handled = execF2(first, operand);
            if (!handled) {
                notImplemented(opcodeToMnemonic(first));
            }
            return;
        }

        int opcode = first & 0xFC;
        if (!isFormat34(opcode)) {
            invalidOpcode(first);
            return;
        }

        int second = fetch();
        int xbpe = (second >> 4) & 0x0F;
        int third = fetch();
        boolean extended = (xbpe & 0x1) != 0;
        int operandRaw;
        int operandValue;

        if (extended) {
            int fourth = fetch();
            operandRaw = ((second & 0x0F) << 16) | (third << 8) | fourth;
            operandValue = signExtend(operandRaw, 20);
            lastInstructionLength = 4;
        } else {
            operandRaw = ((second & 0x0F) << 8) | third;
            operandValue = signExtend(operandRaw, 12);
            lastInstructionLength = 3;
        }

        lastNi = first & 0x03;
        lastXbpe = xbpe;
        lastExtended = extended;
        lastOperand = operandValue;

        boolean handled = execSICF3F4(opcode, lastNi, operandRaw);
        if (!handled) {
            notImplemented(opcodeToMnemonic(opcode));
        }
    }

    public boolean execF1(int opcode) {
        return false;
    }

    public boolean execF2(int opcode, int operand) {
        int r1 = (operand >> 4) & 0x0F;
        int r2 = operand & 0x0F;

        switch (opcode & 0xFF) {
            case Opcode.ADDR: {
                if (!validateGeneralRegister(r1) || !validateGeneralRegister(r2)) {
                    invalidAddressing();
                    return true;
                }
                int addResult = toSigned24(getReg(r1)) + toSigned24(getReg(r2));
                setReg(r2, addResult);
                return true;
            }
            case Opcode.SUBR: {
                if (!validateGeneralRegister(r1) || !validateGeneralRegister(r2)) {
                    invalidAddressing();
                    return true;
                }
                int diff = toSigned24(getReg(r2)) - toSigned24(getReg(r1));
                setReg(r2, diff);
                return true;
            }
            case Opcode.MULR: {
                if (!validateGeneralRegister(r1) || !validateGeneralRegister(r2)) {
                    invalidAddressing();
                    return true;
                }
                long product = (long) toSigned24(getReg(r1)) * toSigned24(getReg(r2));
                setReg(r2, (int) product);
                return true;
            }
            case Opcode.DIVR: {
                if (!validateGeneralRegister(r1) || !validateGeneralRegister(r2)) {
                    invalidAddressing();
                    return true;
                }
                int divisor = toSigned24(getReg(r1));
                if (divisor == 0) {
                    System.err.println("Division by zero in DIVR instruction.");
                    return true;
                }
                int quotient = toSigned24(getReg(r2)) / divisor;
                setReg(r2, quotient);
                return true;
            }
            case Opcode.COMPR: {
                if (!validateGeneralRegister(r1) || !validateGeneralRegister(r2)) {
                    invalidAddressing();
                    return true;
                }
                int comparison = Integer.compare(toSigned24(getReg(r1)), toSigned24(getReg(r2)));
                setConditionFromComparison(comparison);
                return true;
            }
            case Opcode.RMO: {
                if (!validateGeneralRegister(r1) || !validateGeneralRegister(r2)) {
                    invalidAddressing();
                    return true;
                }
                setReg(r2, getReg(r1));
                return true;
            }
            case Opcode.SHIFTL: {
                if (!validateGeneralRegister(r1)) {
                    invalidAddressing();
                    return true;
                }
                int count = r2 & 0x0F;
                int value = getReg(r1) & 0xFFFFFF;
                int shifted = maskWord(count >= 24 ? 0 : (value << count));
                setReg(r1, shifted);
                return true;
            }
            case Opcode.SHIFTR: {
                if (!validateGeneralRegister(r1)) {
                    invalidAddressing();
                    return true;
                }
                int count = r2 & 0x0F;
                int value = getReg(r1) & 0xFFFFFF;
                int shifted = count >= 24 ? 0 : (value >>> count);
                setReg(r1, shifted);
                return true;
            }
            case Opcode.CLEAR: {
                if (!validateGeneralRegister(r1)) {
                    invalidAddressing();
                    return true;
                }
                setReg(r1, 0);
                return true;
            }
            case Opcode.TIXR: {
                if (!validateGeneralRegister(r1)) {
                    invalidAddressing();
                    return true;
                }
                setX(maskWord(getX() + 1));
                int comparison = Integer.compare(toSigned24(getX()), toSigned24(getReg(r1)));
                setConditionFromComparison(comparison);
                return true;
            }
            case Opcode.SVC:
                return false;
            default:
                return false;
        }
    }

    private boolean unconditionalJump(EffectiveAddress operand) {
        if (operand.immediate) {
            invalidAddressing();
            return true;
        }
        setPC(operand.address);
        return true;
    }

    private boolean conditionalJump(EffectiveAddress operand, boolean shouldJump) {
        if (operand.immediate) {
            invalidAddressing();
            return true;
        }
        if (shouldJump) {
            setPC(operand.address);
        }
        return true;
    }

    private boolean storeWordOperand(EffectiveAddress operand, int value) {
        if (operand.immediate) {
            invalidAddressing();
            return true;
        }
        setWord(operand.address, value);
        return true;
    }

    private boolean executeReadDevice(EffectiveAddress operand) {
        int deviceNum = fetchDeviceNumber(operand);
        Device device;
        try {
            device = getDevice(deviceNum);
        } catch (IllegalArgumentException ex) {
            invalidAddressing();
            return true;
        }
        if (device == null) {
            System.err.println("Device not configured: " + deviceNum);
            return true;
        }
        byte data = device.read();
        setA((getA() & 0xFFFF00) | (data & 0xFF));
        return true;
    }

    private boolean executeWriteDevice(EffectiveAddress operand) {
        int deviceNum = fetchDeviceNumber(operand);
        Device device;
        try {
            device = getDevice(deviceNum);
        } catch (IllegalArgumentException ex) {
            invalidAddressing();
            return true;
        }
        if (device == null) {
            System.err.println("Device not configured: " + deviceNum);
            return true;
        }
        device.write((byte) (getA() & 0xFF));
        return true;
    }

    private boolean executeTestDevice(EffectiveAddress operand) {
        int deviceNum = fetchDeviceNumber(operand);
        Device device;
        try {
            device = getDevice(deviceNum);
        } catch (IllegalArgumentException ex) {
            invalidAddressing();
            return true;
        }
        boolean ready = device != null && device.test();
        setSW(ready ? CC_EQUAL : CC_LESS);
        return true;
    }

    private int fetchDeviceNumber(EffectiveAddress operand) {
        return readByteValue(operand) & 0xFF;
    }

    private int readWordValue(EffectiveAddress operand) {
        if (operand.immediate) {
            return maskWord(operand.value);
        }
        return getWord(operand.address);
    }

    private int readSignedWordValue(EffectiveAddress operand) {
        return toSigned24(readWordValue(operand));
    }

    private int readByteValue(EffectiveAddress operand) {
        if (operand.immediate) {
            return operand.value & 0xFF;
        }
        return getByte(operand.address);
    }

    private EffectiveAddress resolveOperand(int ni, int xbpe, int operandRaw, boolean extended) {
        boolean n = (ni & 0x02) != 0;
        boolean i = (ni & 0x01) != 0;
        if (!n && !i) {
            throw new IllegalArgumentException("Invalid addressing mode flags");
        }

        boolean x = (xbpe & 0x08) != 0;
        boolean b = (xbpe & 0x04) != 0;
        boolean p = (xbpe & 0x02) != 0;

        int baseAddress;
        if (extended) {
            baseAddress = operandRaw & 0xFFFFF;
        } else {
            int disp = operandRaw & 0x0FFF;
            int signedDisp = signExtend(disp, 12);
            if (p) {
                baseAddress = maskAddress(getPC() + signedDisp);
            } else if (b) {
                baseAddress = maskAddress(getB() + signedDisp);
            } else {
                baseAddress = disp;
            }
        }

        if (x) {
            baseAddress = maskAddress(baseAddress + getX());
        }

        if (!n && i) {
            int value;
            if (extended || b || p) {
                value = baseAddress;
            } else {
                int bits = extended ? 20 : 12;
                value = signExtend(operandRaw, bits);
            }
            return EffectiveAddress.immediate(value);
        }

        int address = maskAddress(baseAddress);
        if (n && !i) {
            int pointer = getWord(address);
            address = maskAddress(pointer);
        }
        return EffectiveAddress.direct(address);
    }

    private void setConditionFromComparison(int comparison) {
        if (comparison < 0) {
            setSW(CC_LESS);
        } else if (comparison > 0) {
            setSW(CC_GREATER);
        } else {
            setSW(CC_EQUAL);
        }
    }

    private int getConditionCode() {
        return getSW() & 0xC0;
    }

    private boolean validateGeneralRegister(int reg) {
        return reg >= 0 && reg <= 5;
    }

    private static int toSigned24(int value) {
        int masked = value & 0xFFFFFF;
        if ((masked & 0x800000) != 0) {
            masked -= 1 << 24;
        }
        return masked;
    }

    private static int signExtend(int value, int bits) {
        int mask = (1 << bits) - 1;
        value &= mask;
        int signBit = 1 << (bits - 1);
        if ((value & signBit) != 0) {
            value |= ~mask;
        }
        return value;
    }

    private static final class EffectiveAddress {
        final boolean immediate;
        final int value;
        final int address;

        private EffectiveAddress(boolean immediate, int value, int address) {
            this.immediate = immediate;
            this.value = value;
            this.address = address;
        }

        static EffectiveAddress immediate(int value) {
            return new EffectiveAddress(true, value, -1);
        }

        static EffectiveAddress direct(int address) {
            return new EffectiveAddress(false, 0, address);
        }
    }

    public boolean execSICF3F4(int opcode, int ni, int operandRaw) {
        if (opcode == Opcode.RSUB) {
            setPC(getL());
            return true;
        }

        EffectiveAddress operand;
        try {
            operand = resolveOperand(ni, lastXbpe, operandRaw, lastExtended);
        } catch (IllegalArgumentException ex) {
            invalidAddressing();
            return true;
        }

        switch (opcode & 0xFC) {
            case Opcode.LDA:
                setA(readWordValue(operand));
                return true;
            case Opcode.LDX:
                setX(readWordValue(operand));
                return true;
            case Opcode.LDL:
                setL(readWordValue(operand));
                return true;
            case Opcode.STA:
                return storeWordOperand(operand, getA());
            case Opcode.STX:
                return storeWordOperand(operand, getX());
            case Opcode.STL:
                return storeWordOperand(operand, getL());
            case Opcode.ADD: {
                int result = toSigned24(getA()) + readSignedWordValue(operand);
                setA(result);
                return true;
            }
            case Opcode.SUB: {
                int result = toSigned24(getA()) - readSignedWordValue(operand);
                setA(result);
                return true;
            }
            case Opcode.MUL: {
                long product = (long) toSigned24(getA()) * readSignedWordValue(operand);
                setA((int) product);
                return true;
            }
            case Opcode.DIV: {
                int divisor = readSignedWordValue(operand);
                if (divisor == 0) {
                    System.err.println("Division by zero in DIV instruction.");
                    return true;
                }
                int quotient = toSigned24(getA()) / divisor;
                setA(quotient);
                return true;
            }
            case Opcode.COMP: {
                int comparison = Integer.compare(toSigned24(getA()), readSignedWordValue(operand));
                setConditionFromComparison(comparison);
                return true;
            }
            case Opcode.TIX: {
                setX(maskWord(getX() + 1));
                int comparison = Integer.compare(toSigned24(getX()), readSignedWordValue(operand));
                setConditionFromComparison(comparison);
                return true;
            }
            case Opcode.JEQ:
                return conditionalJump(operand, getConditionCode() == CC_EQUAL);
            case Opcode.JGT:
                return conditionalJump(operand, getConditionCode() == CC_GREATER);
            case Opcode.JLT:
                return conditionalJump(operand, getConditionCode() == CC_LESS);
            case Opcode.J:
                return unconditionalJump(operand);
            case Opcode.AND: {
                int result = getA() & readWordValue(operand);
                setA(result);
                return true;
            }
            case Opcode.OR: {
                int result = getA() | readWordValue(operand);
                setA(result);
                return true;
            }
            case Opcode.JSUB: {
                if (operand.immediate) {
                    invalidAddressing();
                    return true;
                }
                setL(getPC());
                setPC(operand.address);
                return true;
            }
            case Opcode.LDCH: {
                int value = readByteValue(operand);
                setA((getA() & 0xFFFF00) | (value & 0xFF));
                return true;
            }
            case Opcode.STCH: {
                if (operand.immediate) {
                    invalidAddressing();
                    return true;
                }
                setByte(operand.address, getA() & 0xFF);
                return true;
            }
            case Opcode.LDB:
                setB(readWordValue(operand));
                return true;
            case Opcode.LDS:
                setS(readWordValue(operand));
                return true;
            case Opcode.LDT:
                setT(readWordValue(operand));
                return true;
            case Opcode.STB:
                return storeWordOperand(operand, getB());
            case Opcode.STS:
                return storeWordOperand(operand, getS());
            case Opcode.STT:
                return storeWordOperand(operand, getT());
            case Opcode.RD:
                return executeReadDevice(operand);
            case Opcode.WD:
                return executeWriteDevice(operand);
            case Opcode.TD:
                return executeTestDevice(operand);
            case Opcode.STSW:
                return storeWordOperand(operand, getSW());
            default:
                return false;
        }
    }

    public void start() {
        synchronized (this) {
            if (running) {
                return;
            }
            timer = new Timer("sicxe-timer", true);
            running = true;
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    runScheduledStep();
                }
            }, 0L, TIMER_PERIOD_MS);
        }
    }

    public void stop() {
        synchronized (this) {
            if (!running) {
                return;
            }
            running = false;
            if (timer != null) {
                timer.cancel();
                timer = null;
            }
        }
    }

    public boolean isRunning() {
        return running;
    }

    public int getSpeed() {
        return speedKHz;
    }

    public void setSpeed(int kHz) {
        if (kHz <= 0) {
            throw new IllegalArgumentException("Speed must be positive.");
        }
        speedKHz = kHz;
    }

    public void step() {
        execute();
    }

    private void runScheduledStep() {
        try {
            int steps = Math.max(1, speedKHz);
            synchronized (executionLock) {
                for (int i = 0; i < steps; i++) {
                    executeInstruction();
                }
            }
        } catch (RuntimeException e) {
            stop();
            System.err.println("Execution halted due to runtime error: " + e.getMessage());
        }
    }

    public int getLastNi() {
        return lastNi;
    }

    public int getLastXbpe() {
        return lastXbpe;
    }

    public boolean isLastExtended() {
        return lastExtended;
    }

    public int getLastOperand() {
        return lastOperand;
    }

    public int getLastInstructionLength() {
        return lastInstructionLength;
    }

    private static String[] splitFields(String payload) {
        if (payload == null) {
            return new String[0];
        }
        String trimmed = payload.trim();
        if (trimmed.isEmpty()) {
            return new String[0];
        }
        if (trimmed.charAt(0) == '^') {
            trimmed = trimmed.substring(1);
        }
        String[] raw = trimmed.split("\\^");
        int count = 0;
        for (int i = 0; i < raw.length; i++) {
            String field = raw[i].trim();
            if (!field.isEmpty()) {
                raw[count++] = field;
            }
        }
        if (count == raw.length) {
            return raw;
        }
        return Arrays.copyOf(raw, count);
    }

    private static int parseHex(String hex, String context) {
        if (hex == null || hex.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing " + context + ".");
        }
        try {
            return Integer.parseInt(hex.trim(), 16);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid " + context + ": " + hex, ex);
        }
    }

    private static void checkAddressRange(int addr, int length) {
        if (addr < 0 || addr > MAX_ADDRESS || addr + length - 1 > MAX_ADDRESS) {
            throw new IllegalArgumentException("Address out of range: " + addr);
        }
    }

    private static void checkDeviceNumber(int num) {
        if (num < 0 || num >= DEVICE_COUNT) {
            throw new IllegalArgumentException("Invalid device number: " + num);
        }
    }

    private static int maskWord(int value) {
        return value & 0xFFFFFF;
    }

    private static int maskAddress(int value) {
        return value & MAX_ADDRESS;
    }

    private static boolean isFormat1(int opcode) {
        switch (opcode & 0xFF) {
            case Opcode.FIX:
            case Opcode.FLOAT:
            case Opcode.NORM:
            case Opcode.SIO:
            case Opcode.HIO:
            case Opcode.TIO:
                return true;
            default:
                return false;
        }
    }

    private static boolean isFormat2(int opcode) {
        switch (opcode & 0xFF) {
            case Opcode.ADDR:
            case Opcode.SUBR:
            case Opcode.MULR:
            case Opcode.DIVR:
            case Opcode.COMPR:
            case Opcode.RMO:
            case Opcode.SHIFTL:
            case Opcode.SHIFTR:
            case Opcode.SVC:
            case Opcode.CLEAR:
            case Opcode.TIXR:
                return true;
            default:
                return false;
        }
    }

    private static boolean isFormat34(int opcode) {
        switch (opcode & 0xFF) {
            case Opcode.LDA:
            case Opcode.LDX:
            case Opcode.LDL:
            case Opcode.STA:
            case Opcode.STX:
            case Opcode.STL:
            case Opcode.ADD:
            case Opcode.SUB:
            case Opcode.MUL:
            case Opcode.DIV:
            case Opcode.COMP:
            case Opcode.TIX:
            case Opcode.JEQ:
            case Opcode.JGT:
            case Opcode.JLT:
            case Opcode.J:
            case Opcode.AND:
            case Opcode.OR:
            case Opcode.JSUB:
            case Opcode.RSUB:
            case Opcode.LDCH:
            case Opcode.STCH:
            case Opcode.ADDF:
            case Opcode.SUBF:
            case Opcode.MULF:
            case Opcode.DIVF:
            case Opcode.LDB:
            case Opcode.LDS:
            case Opcode.LDF:
            case Opcode.LDT:
            case Opcode.STB:
            case Opcode.STS:
            case Opcode.STF:
            case Opcode.STT:
            case Opcode.COMPF:
            case Opcode.LPS:
            case Opcode.STI:
            case Opcode.RD:
            case Opcode.WD:
            case Opcode.TD:
            case Opcode.STSW:
            case Opcode.SSK:
                return true;
            default:
                return false;
        }
    }

    private static String opcodeToMnemonic(int opcode) {
        switch (opcode & 0xFF) {
            case Opcode.LDA:
                return "LDA";
            case Opcode.LDX:
                return "LDX";
            case Opcode.LDL:
                return "LDL";
            case Opcode.STA:
                return "STA";
            case Opcode.STX:
                return "STX";
            case Opcode.STL:
                return "STL";
            case Opcode.ADD:
                return "ADD";
            case Opcode.SUB:
                return "SUB";
            case Opcode.MUL:
                return "MUL";
            case Opcode.DIV:
                return "DIV";
            case Opcode.COMP:
                return "COMP";
            case Opcode.TIX:
                return "TIX";
            case Opcode.JEQ:
                return "JEQ";
            case Opcode.JGT:
                return "JGT";
            case Opcode.JLT:
                return "JLT";
            case Opcode.J:
                return "J";
            case Opcode.AND:
                return "AND";
            case Opcode.OR:
                return "OR";
            case Opcode.JSUB:
                return "JSUB";
            case Opcode.RSUB:
                return "RSUB";
            case Opcode.LDCH:
                return "LDCH";
            case Opcode.STCH:
                return "STCH";
            case Opcode.ADDF:
                return "ADDF";
            case Opcode.SUBF:
                return "SUBF";
            case Opcode.MULF:
                return "MULF";
            case Opcode.DIVF:
                return "DIVF";
            case Opcode.LDB:
                return "LDB";
            case Opcode.LDS:
                return "LDS";
            case Opcode.LDF:
                return "LDF";
            case Opcode.LDT:
                return "LDT";
            case Opcode.STB:
                return "STB";
            case Opcode.STS:
                return "STS";
            case Opcode.STF:
                return "STF";
            case Opcode.STT:
                return "STT";
            case Opcode.COMPF:
                return "COMPF";
            case Opcode.LPS:
                return "LPS";
            case Opcode.STI:
                return "STI";
            case Opcode.RD:
                return "RD";
            case Opcode.WD:
                return "WD";
            case Opcode.TD:
                return "TD";
            case Opcode.STSW:
                return "STSW";
            case Opcode.SSK:
                return "SSK";
            case Opcode.ADDR:
                return "ADDR";
            case Opcode.SUBR:
                return "SUBR";
            case Opcode.MULR:
                return "MULR";
            case Opcode.DIVR:
                return "DIVR";
            case Opcode.COMPR:
                return "COMPR";
            case Opcode.RMO:
                return "RMO";
            case Opcode.SHIFTL:
                return "SHIFTL";
            case Opcode.SHIFTR:
                return "SHIFTR";
            case Opcode.SVC:
                return "SVC";
            case Opcode.CLEAR:
                return "CLEAR";
            case Opcode.TIXR:
                return "TIXR";
            case Opcode.FIX:
                return "FIX";
            case Opcode.FLOAT:
                return "FLOAT";
            case Opcode.NORM:
                return "NORM";
            case Opcode.SIO:
                return "SIO";
            case Opcode.HIO:
                return "HIO";
            case Opcode.TIO:
                return "TIO";
            default:
                return String.format("0x%02X", opcode & 0xFF);
        }
    }
}
