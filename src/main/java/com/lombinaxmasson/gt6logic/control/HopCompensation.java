package com.lombinaxmasson.gt6logic.control;

import com.gregtech.gregtech.api.signal.GtSignalCarrier;

/**
 * Adds back the amount one signal-wire hop subtracts: {@code UNIT / range}.
 * A zero input stays zero, because a hop that has already fallen to zero cannot be told from no signal.
 */
public final class HopCompensation {
    /** Ranges GT6CE actually ships: red alloy and lumium are 16, signalum is 64. The first is the default. */
    public static final int[] RANGES = {16, 64};

    private HopCompensation() {}

    public static long restore(long input, int range) {
        if (!allowedRange(range)) {
            throw new IllegalArgumentException("wire range must be 16 or 64, saw " + range);
        }
        long clamped = GtSignalCarrier.clamp(input);
        if (clamped == 0L) {
            return 0L;
        }
        long loss = GtSignalCarrier.UNIT / range;
        return GtSignalCarrier.clamp(clamped + loss);
    }

    public static int nextRange(int range) {
        for (int i = 0; i < RANGES.length; i++) {
            if (RANGES[i] == range) {
                return RANGES[(i + 1) % RANGES.length];
            }
        }
        return RANGES[0];
    }

    public static boolean allowedRange(int range) {
        for (int preset : RANGES) {
            if (preset == range) {
                return true;
            }
        }
        return false;
    }
}
