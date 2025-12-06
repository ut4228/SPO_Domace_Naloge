import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;

/**
 * Device backed by a file on disk via RandomAccessFile.
 */
public class FileDevice extends Device {
    private final String path;
    private RandomAccessFile file;

    public FileDevice(String path) {
        this.path = path;
    }

    @Override
    public byte read() {
        RandomAccessFile raf = ensureFile();
        try {
            int value = raf.read();
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
        RandomAccessFile raf = ensureFile();
        try {
            raf.writeByte(value & 0xFF);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public boolean test() {
        return true;
    }

    private RandomAccessFile ensureFile() {
        if (file == null) {
            try {
                file = new RandomAccessFile(path, "rw");
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return file;
    }
}
