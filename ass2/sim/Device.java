/**
 * Basic abstraction of an I/O device in the SIC/XE simulator.
 */
public class Device {
    public boolean test() {
        return true;
    }

    public byte read() {
        return 0;
    }

    public void write(byte value) {
        // no-op by default
    }
}
