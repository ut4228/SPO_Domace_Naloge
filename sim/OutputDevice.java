import java.io.OutputStream;
import java.io.IOException;

public class OutputDevice extends Device {
    private OutputStream output;

    public OutputDevice(int num, OutputStream output) {
        super(num);
        this.output = output;
    }

    @Override
    void write(int val) {
        try {
            output.write(val & 0xFF);
            output.flush();
        } catch (IOException e) {
            // Ignoriramo napako
        }
    }
}
