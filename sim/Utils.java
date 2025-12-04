import java.io.IOException;
import java.io.Reader;

public class Utils {
    // Prebere točno len znakov iz Readerja in vrne kot String
    public static String readString(Reader r, int len) throws IOException {
        char[] buf = new char[len];
        int off = 0;
        while (off < len) {
            int n = r.read(buf, off, len - off);
            if (n < 0)
                throw new IOException("Unexpected EOF while reading string of length " + len);
            off += n;
        }
        return new String(buf);
    }

    // Prebere en bajt zapisan kot dva hex znaka (npr. "AF") in vrne int 0..255
    public static int readByte(Reader r) throws IOException {
        int hi = readHexNibble(r);
        int lo = readHexNibble(r);
        return ((hi << 4) | lo) & 0xFF;
    }

    // Prebere besedo (3 bajte) zapopano v 6 hex znakih in vrne int 0..0xFFFFFF
    public static int readWord(Reader r) throws IOException {
        int b1 = readByte(r);
        int b2 = readByte(r);
        int b3 = readByte(r);
        return (b1 << 16) | (b2 << 8) | b3;
    }

    // Prebere 6-hex-digit 24-bit vrednost
    public static int readWordHex(Reader r) throws IOException {
        int v = 0;
        for (int i = 0; i < 6; i++) {
            v = (v << 4) | readHexNibble(r);
        }
        return v & 0xFFFFFF;
    }

    // Prebere 5-hex-digit 20-bit naslov
    public static int readAddress20(Reader r) throws IOException {
        int v = 0;
        for (int i = 0; i < 5; i++) {
            v = (v << 4) | readHexNibble(r);
        }
        return v & Memory.MAX_ADDRESS;
    }

    // Prebere eno hex polovico bajta
    private static int readHexNibble(Reader r) throws IOException {
        int ch = r.read();
        if (ch < 0)
            throw new IOException("Unexpected EOF while reading hex digit");
        if (ch >= '0' && ch <= '9')
            return ch - '0';
        if (ch >= 'A' && ch <= 'F')
            return 10 + (ch - 'A');
        if (ch >= 'a' && ch <= 'f')
            return 10 + (ch - 'a');
        throw new IOException("Invalid hex digit: " + (char) ch);
    }

    // Preskoči morebitne konce vrstic in presledke
    public static void skipWhitespace(Reader r) throws IOException {
        r.mark(1);
        int ch;
        while ((ch = r.read()) != -1) {
            if (!(ch == '\n' || ch == '\r' || ch == ' ')) {
                r.reset();
                return;
            }
            r.mark(1);
        }
    }
}
