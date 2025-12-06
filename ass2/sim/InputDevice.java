import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

/**
 * Device backed by an InputStream, used for RD instructions.
 */
public class InputDevice extends Device {
    private final InputStream input;

    public InputDevice(InputStream input) {
        this.input = input;
    }

    @Override
    public byte read() {
        try {
            int value = input.read();
            if (value < 0) {
                return 0;
            }
            return (byte) value;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
