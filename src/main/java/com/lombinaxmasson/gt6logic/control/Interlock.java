package com.lombinaxmasson.gt6logic.control;

/**
 * Combinational gate over two booleans. NOT reads only the first input.
 */
public final class Interlock {
    public enum Mode {
        AND,
        OR,
        NOT;

        public Mode next() {
            Mode[] values = values();
            return values[(ordinal() + 1) % values.length];
        }

        public static Mode byName(String name) {
            for (Mode mode : values()) {
                if (mode.name().equals(name)) {
                    return mode;
                }
            }
            return AND;
        }
    }

    private Interlock() {}

    public static boolean output(Mode mode, boolean first, boolean second) {
        if (mode == null) {
            throw new IllegalArgumentException("interlock mode is missing");
        }
        return switch (mode) {
            case AND -> first && second;
            case OR -> first || second;
            case NOT -> !first;
        };
    }
}
