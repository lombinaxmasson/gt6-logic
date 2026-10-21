package com.lombinaxmasson.gt6logic.signal;

import com.gregtech.gregtech.api.signal.GtSignalCarrier;

/**
 * Converts one GT signal and one bundled-cable colour.
 * {@code 0} and {@code 255} are the two ends. Values in between round down.
 */
public final class BundledScale {
    public static final int FULL = 255;
    private static final long SPAN = 15L * GtSignalCarrier.UNIT;

    private BundledScale() {}

    /** Bundled level 0–255 for a GT signal. Out-of-range signals are clamped first. */
    public static int toBundled(long signal) {
        long clamped = GtSignalCarrier.clamp(signal);
        if (clamped == 0L || SPAN == 0L) {
            return 0;
        }
        return (int) (clamped * (long) FULL / SPAN);
    }

    /** GT signal for a bundled level. Values outside 0–255 are clamped, then the result is clamped again. */
    public static long toSignal(int bundled) {
        int value = bundled;
        if (value < 0) {
            value = 0;
        } else if (value > FULL) {
            value = FULL;
        }
        if (value == 0) {
            return 0L;
        }
        return GtSignalCarrier.clamp(value * SPAN / (long) FULL);
    }
}
