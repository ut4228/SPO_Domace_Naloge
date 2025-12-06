import java.util.Arrays;

/**
 * Core SIC/XE machine simulator state. Manages registers, memory and devices.
 */
public class Machine {
    public static final int MAX_ADDRESS = (1 << 20) - 1; // 20-bit address space (1 MiB)
    public static final int MEMORY_SIZE = MAX_ADDRESS + 1;
    public static final int DEVICE_COUNT = 256;

    private final byte[] memory = new byte[MEMORY_SIZE];
    private final Device[] devices = new Device[DEVICE_COUNT];

    private int regA;
    private int regX;
    private int regL;
    private int regB;
    private int regS;
    private int regT;
    private double regF;
    private int regPC;
    private int regSW;

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
}
