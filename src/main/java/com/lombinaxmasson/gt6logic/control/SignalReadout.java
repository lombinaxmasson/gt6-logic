package com.lombinaxmasson.gt6logic.control;

import java.util.Locale;

import com.gregtech.gregtech.api.signal.GtSignalCarrier;

/**
 * A signal as a level with four decimal places, rounded to the nearest digit.
 * One level is {@code UNIT}. A half level prints as {@code 0.5000} even though {@code UNIT} is odd.
 */
public final class SignalReadout {
    public static final int SCALE = 10_000;

    private SignalReadout() {}

    public static String format(long signal) {
        long clamped = GtSignalCarrier.clamp(signal);
        // 15 * UNIT * SCALE still fits in a signed long, so the product does not need a wide multiply.
        long product = clamped * SCALE;
        long unit = GtSignalCarrier.UNIT;
        long scaled = product / unit;
        if ((product % unit) * 2 >= unit) {
            scaled++;
        }
        long whole = scaled / SCALE;
        long fraction = scaled % SCALE;
        return String.format(Locale.ROOT, "%d.%04d", whole, fraction);
    }
}
