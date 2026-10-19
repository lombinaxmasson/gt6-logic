package com.lombinaxmasson.gt6logic.signal;

import com.gregtech.gregtech.api.signal.GtSignalCarrier;

/**
 * Converts between a GT signal and a vanilla redstone level, and scales a signal
 * without dropping the high half of the product.
 */
public final class SignalLevels {
    private SignalLevels() {}

    /** Smallest vanilla level that is at least {@code signal}. Zero stays zero. */
    public static int ceilingLevel(long signal) {
        long clamped = GtSignalCarrier.clamp(signal);
        if (clamped == 0) {
            return 0;
        }
        long unit = GtSignalCarrier.UNIT;
        long level = (clamped + unit - 1) / unit;
        return (int) Math.min(15L, level);
    }

    /** Exact signal for a vanilla level. Values outside 0–15 are clamped. */
    public static long fromLevel(int level) {
        if (level <= 0) {
            return 0;
        }
        if (level >= 15) {
            return 15L * GtSignalCarrier.UNIT;
        }
        return level * (long) GtSignalCarrier.UNIT;
    }

    /**
     * Floor of {@code left * right / divisor}.
     * All three arguments are non-negative and the divisor is positive.
     * A quotient that does not fit in a signed long becomes {@link Long#MAX_VALUE}.
     */
    public static long multiplyDivide(long left, long right, long divisor) {
        if (left < 0 || right < 0 || divisor <= 0) {
            throw new IllegalArgumentException("signal scale needs non-negative values and a positive divisor");
        }
        if (left == 0 || right == 0) {
            return 0;
        }
        long high = Math.multiplyHigh(left, right);
        long low = left * right;
        return divideUnsigned(high, low, divisor);
    }

    /** Unsigned 128-bit dividend over a positive divisor. The high word is the signed high half. */
    private static long divideUnsigned(long high, long low, long divisor) {
        if (high == 0) {
            long quotient = Long.divideUnsigned(low, divisor);
            return quotient < 0 ? Long.MAX_VALUE : quotient;
        }
        if (high < 0 || Long.compareUnsigned(high, divisor) >= 0) {
            return Long.MAX_VALUE;
        }
        long quotient = 0;
        long remainHigh = 0;
        long remainLow = 0;
        for (int bit = 127; bit >= 0; bit--) {
            long next = (bit >= 64 ? high >>> (bit - 64) : low >>> bit) & 1L;
            long shiftedHigh = (remainHigh << 1) | (remainLow >>> 63);
            long shiftedLow = (remainLow << 1) | next;
            boolean take = shiftedHigh != 0 || Long.compareUnsigned(shiftedLow, divisor) >= 0;
            if (take) {
                if (Long.compareUnsigned(shiftedLow, divisor) < 0) {
                    shiftedHigh -= 1;
                }
                shiftedLow -= divisor;
                if (bit >= 64) {
                    return Long.MAX_VALUE;
                }
                quotient |= 1L << bit;
            }
            remainHigh = shiftedHigh;
            remainLow = shiftedLow;
        }
        return quotient < 0 ? Long.MAX_VALUE : quotient;
    }
}
