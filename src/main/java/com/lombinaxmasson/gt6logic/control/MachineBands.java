package com.lombinaxmasson.gt6logic.control;

import com.lombinaxmasson.gt6logic.control.HysteresisSwitch.Mode;

/** Presets for the three blocks that share a hysteresis band. The first entry is the default. */
public final class MachineBands {
    /** Ten-thousandths of a tank. Fill, a tighter fill, then drain. */
    public static final Band[] FLOAT = {
            new Band(Mode.HEAT, 2_000, 8_000),
            new Band(Mode.HEAT, 1_000, 5_000),
            new Band(Mode.COOL, 2_000, 8_000)
    };

    /** Milli-celsius. Ferment, a cooler room, then dump heat. */
    public static final Band[] THERMOSTAT = {
            new Band(Mode.HEAT, 30_000, 35_000),
            new Band(Mode.HEAT, 20_000, 25_000),
            new Band(Mode.COOL, 35_000, 40_000)
    };

    /** Ten-thousandths of a full meter. 20/90, 50/90, then 20/50. */
    public static final Band[] STEWARD = {
            new Band(Mode.HEAT, 2_000, 9_000),
            new Band(Mode.HEAT, 5_000, 9_000),
            new Band(Mode.HEAT, 2_000, 5_000)
    };

    /** On seconds, off seconds. The first pair is ten and ten. */
    public static final int[][] KITCHEN = {
            {10, 10},
            {5, 5},
            {1, 1},
            {30, 30},
            {10, 50}
    };

    private MachineBands() {}

    public static Band select(Band[] bands, int index) {
        if (index < 0 || index >= bands.length) {
            return bands[0];
        }
        return bands[index];
    }
}
