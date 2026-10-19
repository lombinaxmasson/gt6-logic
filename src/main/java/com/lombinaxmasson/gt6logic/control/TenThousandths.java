package com.lombinaxmasson.gt6logic.control;

import com.gregtech.gregtech.api.signal.GtSignalCarrier;
import com.lombinaxmasson.gt6logic.signal.SignalLevels;

/**
 * A share of a full tank or a full buffer, in ten-thousandths.
 * {@code 10000} is full. The signal form uses the whole 0 to {@code 15 * UNIT} range.
 */
public final class TenThousandths {
    public static final int FULL = 10_000;

    private TenThousandths() {}

    public static int of(long amount, long whole) {
        if (amount <= 0 || whole <= 0) {
            return 0;
        }
        if (amount >= whole) {
            return FULL;
        }
        return (int) Math.min(FULL, amount * FULL / whole);
    }

    /** Whole percent, dropping the fraction. 2000 is {@code 20}. */
    public static int percent(int part) {
        if (part <= 0) {
            return 0;
        }
        if (part >= FULL) {
            return 100;
        }
        return part / 100;
    }

    public static long toSignal(int part) {
        int clamped = Math.max(0, Math.min(FULL, part));
        return SignalLevels.multiplyDivide(clamped, 15L * GtSignalCarrier.UNIT, FULL);
    }

    public static int fromSignal(long signal) {
        long part = SignalLevels.multiplyDivide(GtSignalCarrier.clamp(signal), FULL, 15L * GtSignalCarrier.UNIT);
        return (int) Math.min(FULL, part);
    }
}
