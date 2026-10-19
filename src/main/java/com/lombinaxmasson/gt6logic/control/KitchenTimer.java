package com.lombinaxmasson.gt6logic.control;

/**
 * On for {@code duration} ticks, then off until {@code period} ticks have passed.
 * Read {@link #output()} and then call {@link #tick()}. Phase starts at 0, so the first read is on.
 */
public final class KitchenTimer {
    private final int period;
    private final int duration;
    private int phase;

    public KitchenTimer(int period, int duration) {
        if (period < 1 || duration < 1) {
            throw new IllegalArgumentException("period and duration must be at least 1");
        }
        this.period = period;
        this.duration = duration;
    }

    public boolean output() {
        return phase % period < duration;
    }

    public void tick() {
        phase++;
        if (phase == period) {
            phase = 0;
        }
    }
}
