import java.io.IOException;
import java.io.Reader;

/**
 * Helper utilities for working with SIC/XE object streams.
 */
public final class Utils {
    private Utils() {
    }

    public static String readString(Reader reader, int length) throws IOException {
        if (length < 0) {
            throw new IllegalArgumentException("Length must be non-negative");
        }
        char[] buffer = new char[length];
        int offset = 0;
        while (offset < length) {
            int read = reader.read(buffer, offset, length - offset);
            if (read < 0) {
                throw new IOException("Unexpected end of stream while reading string");
            }
            offset += read;
        }
        return new String(buffer, 0, length);
    }

    public static int readByte(Reader reader) throws IOException {
        return readHexValue(reader, 2);
    }

    public static int readWord(Reader reader) throws IOException {
        return readHexValue(reader, 6);
    }

    private static int readHexValue(Reader reader, int digits) throws IOException {
        if (digits <= 0) {
            throw new IllegalArgumentException("Digits must be positive");
        }
        char[] buffer = new char[digits];
        int count = 0;
        while (count < digits) {
            int ch = reader.read();
            if (ch < 0) {
                throw new IOException("Unexpected end of stream while reading hex value");
            }
            if (Character.isWhitespace(ch)) {
                continue;
            }
            buffer[count++] = Character.toUpperCase((char) ch);
        }
        String hex = new String(buffer, 0, digits);
        try {
            return Integer.parseInt(hex, 16);
        } catch (NumberFormatException ex) {
            throw new IOException("Invalid hexadecimal value: " + hex, ex);
        }
    }
}
