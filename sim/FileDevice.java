import java.io.RandomAccessFile;
import java.io.IOException;
import java.io.FileNotFoundException;

public class FileDevice extends Device {
    private RandomAccessFile file;

    public FileDevice(int num, String filename) {
        super(num);
        try {
            this.file = new RandomAccessFile(filename, "rw");
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Cannot open file: " + filename, e);
        }
    }

    @Override
    boolean test() {
        try {
            return file != null && file.getFilePointer() >= 0;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    int read() {
        try {
            int value = file.read();
            return value == -1 ? 0 : value;
        } catch (IOException e) {
            return 0;
        }
    }

    @Override
    void write(int val) {
        try {
            file.write(val & 0xFF);
        } catch (IOException e) {
            // Ignoriramo napako
        }
    }

    public void close() {
        try {
            if (file != null) {
                file.close();
            }
        } catch (IOException e) {
            // Ignoriramo napako
        }
    }
}
