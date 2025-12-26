package assembler;

import java.util.Map;

public final class Registers {
    private static final Map<String, Integer> REG = Map.of(
            "A", 0,
            "X", 1,
            "L", 2,
            "B", 3,
            "S", 4,
            "T", 5,
            "F", 6,
            "PC", 8,
            "SW", 9
    );

    public static int reg(String name) {
        Integer v = REG.get(name.toUpperCase());
        return v == null ? -1 : v;
    }
}