public class Memory {
    public static final int MAX_ADDRESS = 0xFFFFF; // 20-bitni naslovni prostor (1,048,575)
    public static final int MAX_MEM_SIZE = MAX_ADDRESS + 1; // 1,048,576 bajtov (1 MB)

    byte[] mem;

    public Memory(int size) {
        if (size > MAX_MEM_SIZE) {
            throw new IllegalArgumentException("Memory size exceeds maximum: " + MAX_MEM_SIZE);
        }
        mem = new byte[size];
    }

    int getByte(int addr) {
        if (addr < 0 || addr > MAX_ADDRESS || addr >= mem.length) {
            throw new IllegalArgumentException("Invalid memory address: " + addr);
        }
        return mem[addr] & 0xFF; // & 0xFF pretvori v nepredznačeno število
    }

    void setByte(int addr, int val) {
        if (addr < 0 || addr > MAX_ADDRESS || addr >= mem.length) {
            throw new IllegalArgumentException("Invalid memory address: " + addr);
        }
        mem[addr] = (byte) val;
    }

    int getWord(int addr) {
        if (addr < 0 || addr > MAX_ADDRESS || addr + 2 > MAX_ADDRESS || addr + 2 >= mem.length) {
            throw new IllegalArgumentException("Invalid memory address for word: " + addr);
        }
        int b1 = getByte(addr);
        int b2 = getByte(addr + 1);
        int b3 = getByte(addr + 2);
        return (b1 << 16) | (b2 << 8) | b3; // 3-bajtna beseda
    }

    void setWord(int addr, int val) {
        if (addr < 0 || addr > MAX_ADDRESS || addr + 2 > MAX_ADDRESS || addr + 2 >= mem.length) {
            throw new IllegalArgumentException("Invalid memory address for word: " + addr);
        }
        setByte(addr, (val >> 16) & 0xFF);
        setByte(addr + 1, (val >> 8) & 0xFF);
        setByte(addr + 2, val & 0xFF);
    }
}