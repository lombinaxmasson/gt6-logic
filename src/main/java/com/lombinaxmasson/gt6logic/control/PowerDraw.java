package com.lombinaxmasson.gt6logic.control;

import com.gregtech.gregtech.api.signal.GtSignalCarrier;
import com.lombinaxmasson.gt6logic.signal.SignalLevels;

/** EU per tick at a given signal. Full signal draws {@code fullScale}. */
public final class PowerDraw {
    /** One LV packet. A heater at full signal draws this much. */
    public static final long LV_PACKET = 32L;

    private PowerDraw() {}

    public static long eu(long signal, long fullScale) {
        if (fullScale <= 0) {
            return 0L;
        }
        return SignalLevels.multiplyDivide(GtSignalCarrier.clamp(signal), fullScale, 15L * GtSignalCarrier.UNIT);
    }
}
