package com.lombinaxmasson.gt6logic.control;

import com.gregtech.gregtech.api.signal.GtSignalCarrier;
import com.lombinaxmasson.gt6logic.signal.SignalLevels;

/**
 * Three ways to show one signal on a wall. Temperature uses the 0–150 °C encoding.
 * Percent is the share of a full signal, rounded down.
 */
public final class DisplayReading {
    public enum Unit {
        SIGNAL,
        TEMPERATURE,
        PERCENT;

        public Unit next() {
            Unit[] values = values();
            return values[(ordinal() + 1) % values.length];
        }

        public static Unit byName(String name) {
            for (Unit unit : values()) {
                if (unit.name().equals(name)) {
                    return unit;
                }
            }
            return SIGNAL;
        }

        public String messageKey() {
            return switch (this) {
                case TEMPERATURE -> "message.gt6logic.digit.temperature";
                case PERCENT -> "message.gt6logic.digit.percent";
                case SIGNAL -> "message.gt6logic.digit.signal";
            };
        }
    }

    private DisplayReading() {}

    /** Text drawn on the face. Percent includes the sign so the wall can be read without the chat line. */
    public static String face(Unit unit, long signal) {
        return switch (unit) {
            case TEMPERATURE -> Temperatures.format(Temperatures.toMilli(signal));
            case PERCENT -> percent(signal) + "%";
            case SIGNAL -> SignalReadout.format(signal);
        };
    }

    /** Number substituted into the chat line. The percent sign lives in the translation. */
    public static String argument(Unit unit, long signal) {
        return switch (unit) {
            case TEMPERATURE -> Temperatures.format(Temperatures.toMilli(signal));
            case PERCENT -> Integer.toString(percent(signal));
            case SIGNAL -> SignalReadout.format(signal);
        };
    }

    public static int percent(long signal) {
        return (int) SignalLevels.multiplyDivide(GtSignalCarrier.clamp(signal), 100L, 15L * GtSignalCarrier.UNIT);
    }
}
