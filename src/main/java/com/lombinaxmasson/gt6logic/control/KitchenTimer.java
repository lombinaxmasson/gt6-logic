package com.lombinaxmasson.gt6logic.control;

/**
 * On for {@code duration} ticks, then off until {@code period} ticks have passed.
 * Read {@link #output()} and then call {@link #tick()}. Phase starts at 0, so the first read is on.
 */
public final class KitchenTimer {
    public static final int TICKS_PER_SECOND = 20;

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

    public int period() {
        return period;
    }

    public int duration() {
        return duration;
    }

    public int phase() {
        return phase;
    }

    /** Same clock as the constructor, with a phase restored from a save. A negative phase starts at 0. */
    public static KitchenTimer resumed(int period, int duration, int phase) {
        KitchenTimer timer = new KitchenTimer(period, duration);
        if (phase > 0) {
            timer.phase = phase % period;
        }
        return timer;
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
