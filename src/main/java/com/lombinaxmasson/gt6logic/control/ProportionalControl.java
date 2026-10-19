package com.lombinaxmasson.gt6logic.control;

import com.gregtech.gregtech.api.signal.GtSignalCarrier;
import com.lombinaxmasson.gt6logic.control.HysteresisSwitch.Mode;
import com.lombinaxmasson.gt6logic.signal.SignalLevels;

/**
 * Opening follows the error. Cool opens when the measurement is above the setpoint.
 * Heat opens when it is below. A zero or negative error stays shut.
 */
public final class ProportionalControl {
    /** First entry is the default. One level of error becomes this many levels of opening. */
    public static final int[] GAINS = {1, 2, 4};

    private ProportionalControl() {}

    public static long opening(Mode mode, long measured, long setpoint, int gain) {
        if (mode == null) {
            throw new IllegalArgumentException("proportional mode is missing");
        }
        if (!allowed(gain)) {
            throw new IllegalArgumentException("gain must be 1, 2, or 4, saw " + gain);
        }
        long measurement = GtSignalCarrier.clamp(measured);
        long target = GtSignalCarrier.clamp(setpoint);
        long error = mode == Mode.COOL ? measurement - target : target - measurement;
        if (error <= 0) {
            return 0L;
        }
        return GtSignalCarrier.clamp(SignalLevels.multiplyDivide(error, gain, 1));
    }

    public static int nextGain(int gain) {
        for (int i = 0; i < GAINS.length; i++) {
            if (GAINS[i] == gain) {
                return GAINS[(i + 1) % GAINS.length];
            }
        }
        return GAINS[0];
    }

    public static boolean allowed(int gain) {
        for (int preset : GAINS) {
            if (preset == gain) {
                return true;
            }
        }
        return false;
    }
}
