import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;

/**
 * Device backed by a file on disk via RandomAccessFile.
 */
public class FileDevice extends Device {
    private final RandomAccessFile file;

    public FileDevice(String path) {
        try {
            this.file = new RandomAccessFile(path, "rw");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public byte read() {
        try {
            int value = file.read();
            if (value < 0) {
                return 0;
            }
            return (byte) value;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void write(byte value) {
        try {
            file.writeByte(value & 0xFF);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public boolean test() {
        return true;
    }
}
