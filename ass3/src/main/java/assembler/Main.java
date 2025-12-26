package assembler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class Main {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java assembler.Main <sourcefile>");
            System.exit(1);
        }
        Path path = Path.of(args[0]);
        String text;
        try {
            text = Files.readString(path);
        } catch (IOException e) {
            System.err.println("Unable to read file: " + path + " (" + e.getMessage() + ")");
            System.exit(2);
            return;
        }

        Parser parser = new Parser(text);
        try {
            Code code = parser.parse();
            code.resolve();

            byte[] raw = code.emitBinary();
            String obj = code.emitObject();
            String lst = code.emitListing();

            Path objPath = changeExtension(path, ".obj");
            Path binPath = changeExtension(path, ".bin");
            Path logPath = changeExtension(path, ".log");
            Path lstPath = changeExtension(path, ".lst");

            Files.write(objPath, obj.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            Files.write(binPath, raw, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            Files.write(lstPath, lst.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            StringBuilder log = new StringBuilder();
            log.append(code.pretty()).append("\n\n");
            log.append(code.dumpSymbols()).append("\n");
            log.append("Object file:\n").append(obj).append("\n");
            log.append("Listing:\n").append(lst).append("\n");
            log.append("Raw dump:\n").append(code.dumpCode(raw));
            Files.writeString(logPath, log.toString(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            System.out.println("Assembled." );
            System.out.println("OBJ: " + objPath);
            System.out.println("BIN: " + binPath);
            System.out.println("LST: " + lstPath);
            System.out.println("LOG: " + logPath);
        } catch (ParseException e) {
            System.err.println("Parse error: " + e.getMessage());
            System.exit(3);
        } catch (IOException e) {
            System.err.println("IO error: " + e.getMessage());
            System.exit(4);
        }
    }

    private static Path changeExtension(Path original, String newExt) {
        String name = original.getFileName().toString();
        int dot = name.lastIndexOf('.');
        if (dot >= 0) {
            name = name.substring(0, dot);
        }
        name += newExt;
        return original.resolveSibling(name);
    }
}
