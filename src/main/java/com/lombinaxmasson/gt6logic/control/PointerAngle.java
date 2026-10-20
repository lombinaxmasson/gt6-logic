package com.lombinaxmasson.gt6logic.control;

import com.gregtech.gregtech.api.signal.GtSignalCarrier;

/**
 * Needle position for a 0 to full-scale gauge. A full signal stops just short of a whole turn,
 * so it does not sit on the same mark as zero.
 */
public final class PointerAngle {
    private PointerAngle() {}

    /** Fraction of a turn, clockwise from straight up. */
    public static float turns(long signal) {
        long clamped = GtSignalCarrier.clamp(signal);
        if (clamped <= 0L) {
            return 0F;
        }
        float fraction = clamped / (float) (15L * GtSignalCarrier.UNIT);
        return fraction * (15.5F / 16F);
    }

    public static float radians(long signal) {
        return turns(signal) * ((float) Math.PI * 2F);
    }
}
