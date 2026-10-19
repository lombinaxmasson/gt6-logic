package com.lombinaxmasson.gt6logic.control;

import com.gregtech.gregtech.api.signal.GtSignalCarrier;

/**
 * One block, six operations. Half and double are the scale operations:
 * the player does not type a factor.
 */
public final class Arithmetic {
    public enum Op {
        ADD,
        SUB,
        HALF,
        DOUBLE,
        MAX,
        MIN;

        public Op next() {
            Op[] values = values();
            return values[(ordinal() + 1) % values.length];
        }

        public static Op byName(String name) {
            for (Op op : values()) {
                if (op.name().equals(name)) {
                    return op;
                }
            }
            return ADD;
        }
    }

    private Arithmetic() {}

    public static long apply(Op op, long left, long right) {
        if (op == null) {
            throw new IllegalArgumentException("arithmetic operation is missing");
        }
        long a = GtSignalCarrier.clamp(left);
        long b = GtSignalCarrier.clamp(right);
        return switch (op) {
            case ADD -> GtSignalCarrier.clamp(a + b);
            case SUB -> GtSignalCarrier.clamp(a - b);
            case HALF -> a / 2;
            case DOUBLE -> GtSignalCarrier.clamp(a * 2);
            case MAX -> Math.max(a, b);
            case MIN -> Math.min(a, b);
        };
    }
}
