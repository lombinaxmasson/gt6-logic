package com.lombinaxmasson.gt6logic.control;

import java.util.Locale;

import com.lombinaxmasson.gt6logic.signal.SignalLevels;

/**
 * Three bands on the strongest neighbor. Zero is dark.
 * One to five is fine, six to ten wants a look, eleven and above is danger.
 */
public enum BeaconBand {
    OFF,
    GREEN,
    YELLOW,
    RED;

    public static BeaconBand of(long signal) {
        int level = SignalLevels.ceilingLevel(signal);
        if (level <= 0) {
            return OFF;
        }
        if (level <= 5) {
            return GREEN;
        }
        if (level <= 10) {
            return YELLOW;
        }
        return RED;
    }

    public static BeaconBand byName(String name) {
        for (BeaconBand band : values()) {
            if (band.name().equals(name)) {
                return band;
            }
        }
        return OFF;
    }

    public String messageKey() {
        return "message.gt6logic.beacon." + name().toLowerCase(Locale.ROOT);
    }
}
