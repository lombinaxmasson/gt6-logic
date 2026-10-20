package com.lombinaxmasson.gt6logic.control;

import com.gregtech.gregtech.api.signal.GtSignalCarrier;

/**
 * One sample a second, kept for a minute. The first tick records immediately.
 * After that, {@link #PERIOD} ticks pass between samples.
 */
public final class ChartTrace {
    public static final int CAPACITY = 60;
    public static final int PERIOD = 20;

    private final long[] samples = new long[CAPACITY];
    private int count;
    private int next;
    private int wait;

    /** Returns whether this tick stored a sample. */
    public boolean tick(long signal) {
        if (wait > 0) {
            wait--;
            if (wait > 0) {
                return false;
            }
        }
        push(signal);
        wait = PERIOD;
        return true;
    }

    public int count() {
        return count;
    }

    public int untilNext() {
        return wait;
    }

    public long latest() {
        return count == 0 ? 0L : at(count - 1);
    }

    public long lowest() {
        if (count == 0) {
            return 0L;
        }
        long value = Long.MAX_VALUE;
        for (int i = 0; i < count; i++) {
            value = Math.min(value, at(i));
        }
        return value;
    }

    public long highest() {
        if (count == 0) {
            return 0L;
        }
        long value = 0L;
        for (int i = 0; i < count; i++) {
            value = Math.max(value, at(i));
        }
        return value;
    }

    /** Oldest sample is index 0. */
    public float fraction(int oldestIndex) {
        if (oldestIndex < 0 || oldestIndex >= count) {
            return 0F;
        }
        return at(oldestIndex) / (float) (15L * GtSignalCarrier.UNIT);
    }

    /** Oldest first. A save longer than a minute keeps the newest minute. */
    public long[] ordered() {
        long[] values = new long[count];
        for (int i = 0; i < count; i++) {
            values[i] = at(i);
        }
        return values;
    }

    public void restore(long[] oldestFirst, int wait) {
        count = 0;
        next = 0;
        this.wait = Math.max(0, Math.min(PERIOD, wait));
        if (oldestFirst == null || oldestFirst.length == 0) {
            return;
        }
        int start = Math.max(0, oldestFirst.length - CAPACITY);
        for (int i = start; i < oldestFirst.length; i++) {
            push(oldestFirst[i]);
        }
        this.wait = Math.max(0, Math.min(PERIOD, wait));
    }

    private void push(long signal) {
        samples[next] = GtSignalCarrier.clamp(signal);
        next = (next + 1) % CAPACITY;
        if (count < CAPACITY) {
            count++;
        }
    }

    private long at(int oldestIndex) {
        return samples[Math.floorMod(next - count + oldestIndex, CAPACITY)];
    }
}
