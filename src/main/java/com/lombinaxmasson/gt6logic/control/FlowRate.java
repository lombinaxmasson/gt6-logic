package com.lombinaxmasson.gt6logic.control;

import com.gregtech.gregtech.api.signal.GtSignalCarrier;
import com.lombinaxmasson.gt6logic.signal.SignalLevels;

/** Millibuckets to move this tick. Binary opens to the full rate once the signal is enough. */
public final class FlowRate {
    private FlowRate() {}

    public static int millibuckets(long signal, int fullOpen, boolean proportional) {
        if (fullOpen <= 0) {
            return 0;
        }
        long clamped = GtSignalCarrier.clamp(signal);
        if (!proportional) {
            return SignalLevels.ceilingLevel(clamped) >= 1 ? fullOpen : 0;
        }
        long rate = SignalLevels.multiplyDivide(clamped, fullOpen, 15L * GtSignalCarrier.UNIT);
        return (int) Math.min(fullOpen, rate);
    }
}
