package com.lombinaxmasson.gt6logic.signal;

import com.gregtech.gregtech.api.signal.GtSignalCarrier;

/**
 * Six signal faces. {@link #set} stores the value for the current tick.
 * {@link #get} keeps returning the previous tick until {@link #publish}.
 */
public final class PublishedSignal {
    public static final int FACES = 6;

    private final long[] pending = new long[FACES];
    private final long[] published = new long[FACES];

    public long get(int side) {
        return published[check(side)];
    }

    public void set(int side, long value) {
        pending[check(side)] = GtSignalCarrier.clamp(value);
    }

    /** Copies every pending face into the published faces. Returns whether any face changed. */
    public boolean publish() {
        boolean changed = false;
        for (int side = 0; side < FACES; side++) {
            if (published[side] != pending[side]) {
                published[side] = pending[side];
                changed = true;
            }
        }
        return changed;
    }

    public long[] publishedCopy() {
        return published.clone();
    }

    /**
     * Restores a saved published snapshot and copies it into the pending faces,
     * so a later publish does not wipe a value the block has not rewritten yet.
     * Missing faces become 0. Extra faces are ignored.
     */
    public void loadPublished(long[] values) {
        if (values == null) {
            throw new IllegalArgumentException("published signal is missing");
        }
        for (int side = 0; side < FACES; side++) {
            long value = side < values.length ? GtSignalCarrier.clamp(values[side]) : 0L;
            pending[side] = value;
            published[side] = value;
        }
    }

    private static int check(int side) {
        if (side < 0 || side >= FACES) {
            throw new IllegalArgumentException("side " + side);
        }
        return side;
    }
}
