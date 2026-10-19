package com.lombinaxmasson.gt6logic.control;

import com.lombinaxmasson.gt6logic.signal.SignalLevels;

/**
 * Whether an alarm bell rings, and the next threshold a right-click selects.
 * A level of 15 is not a setting: the ceiling never goes past 15, so nothing could exceed it.
 */
public final class AlarmBell {
    public static final int MIN_LEVEL = 0;
    public static final int MAX_LEVEL = 14;
    /** One second. The first ring happens on the tick the ceiling crosses the threshold. */
    public static final int PERIOD_TICKS = 20;

    private AlarmBell() {}

    /** True when the signal's ceiling is strictly above {@code level}. */
    public static boolean shouldRing(long signal, int level) {
        checkLevel(level);
        return SignalLevels.ceilingLevel(signal) > level;
    }

    /** The next setting after {@code level}, wrapping from {@link #MAX_LEVEL} back to {@link #MIN_LEVEL}. */
    public static int nextLevel(int level) {
        checkLevel(level);
        return level == MAX_LEVEL ? MIN_LEVEL : level + 1;
    }

    private static void checkLevel(int level) {
        if (level < MIN_LEVEL || level > MAX_LEVEL) {
            throw new IllegalArgumentException("alarm level must be 0 through 14, saw " + level);
        }
    }
}
