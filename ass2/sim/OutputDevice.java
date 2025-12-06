import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;

/**
 * Device backed by an OutputStream, used for WD instructions.
 */
public class OutputDevice extends Device {
    private final OutputStream output;

    public OutputDevice(OutputStream output) {
        this.output = output;
    }

    @Override
    public void write(byte value) {
        try {
            output.write(value & 0xFF);
            output.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
