package com.lombinaxmasson.gt6logic.control;

/** Mash rest. Each step lasts half a minute, then the last step holds. */
public final class TemperatureSchedule {
    public static final int[] CELSIUS = {52, 63, 72, 78};
    public static final int SEGMENT_TICKS = 20 * 30;

    private TemperatureSchedule() {}

    /** Zero-based step. Past the end stays on the last step. */
    public static int segment(int elapsed) {
        int ticks = Math.max(0, elapsed);
        return Math.min(CELSIUS.length - 1, ticks / SEGMENT_TICKS);
    }

    public static int setpointMilli(int elapsed) {
        return CELSIUS[segment(elapsed)] * 1000;
    }
}
