package com.lombinaxmasson.gt6logic.control;

import java.util.Locale;

import com.gregtech.gregtech.api.signal.GtSignalCarrier;
import com.lombinaxmasson.gt6logic.signal.SignalLevels;

/**
 * Temperature on a signal wire. 0 is 0 °C and {@code 15 * UNIT} is 150 °C.
 * A machine that already speaks in milli-celsius uses {@link #toSignal(long)} on the face it publishes.
 */
public final class Temperatures {
    public static final int SPAN_CELSIUS = 150;
    public static final long MILLI_SPAN = 150_000L;

    private Temperatures() {}

    public static long toSignal(long milliCelsius) {
        long clamped = Math.max(0L, Math.min(MILLI_SPAN, milliCelsius));
        return SignalLevels.multiplyDivide(clamped, 15L * GtSignalCarrier.UNIT, MILLI_SPAN);
    }

    public static long toMilli(long signal) {
        return SignalLevels.multiplyDivide(GtSignalCarrier.clamp(signal), MILLI_SPAN, 15L * GtSignalCarrier.UNIT);
    }

    /** One decimal, for the magnifying glass. 30000 is {@code 30.0}. */
    public static String format(long milliCelsius) {
        long abs = Math.abs(milliCelsius);
        return String.format(Locale.ROOT, "%d.%d", abs / 1000, (abs % 1000) / 100);
    }
}
