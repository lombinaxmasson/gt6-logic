package com.lombinaxmasson.gt6logic.control;

/** A press holds the output for one second. Pressing again starts that second over. */
public final class PulseHold {
    public static final int LENGTH = 20;

    private int remaining;

    public void press() {
        remaining = LENGTH;
    }

    public boolean on() {
        return remaining > 0;
    }

    public int remaining() {
        return remaining;
    }

    /** Call after the output for this tick has been taken. */
    public void tick() {
        if (remaining > 0) {
            remaining--;
        }
    }

    public void restore(int remaining) {
        if (remaining < 0) {
            this.remaining = 0;
        } else if (remaining > LENGTH) {
            this.remaining = LENGTH;
        } else {
            this.remaining = remaining;
        }
    }
}
