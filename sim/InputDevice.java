import java.io.InputStream;
import java.io.IOException;

public class InputDevice extends Device {
    private InputStream input;

    public InputDevice(int num, InputStream input) {
        super(num);
        this.input = input;
    }

    @Override
    int read() {
        try {
            int value = input.read();
            return value == -1 ? 0 : value;
        } catch (IOException e) {
            return 0;
        }
    }
}
