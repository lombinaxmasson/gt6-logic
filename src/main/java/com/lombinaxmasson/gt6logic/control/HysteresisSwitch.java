package com.lombinaxmasson.gt6logic.control;

/**
 * One bit of memory between two thresholds. The caller chooses the unit.
 * Heat opens when the reading is strictly below the lower threshold and closes
 * when it is strictly above the upper one. Cool does the opposite.
 * A reading sitting on either threshold keeps the previous output.
 */
public final class HysteresisSwitch {
    public enum Mode {
        HEAT,
        COOL
    }

    private final Mode mode;
    private final long lower;
    private final long upper;
    private boolean on;

    public HysteresisSwitch(Mode mode, long lower, long upper) {
        this.mode = mode;
        this.lower = lower;
        this.upper = upper;
    }

    /** False when the lower threshold is above the upper one. The two numbers are not swapped. */
    public boolean valid() {
        return lower <= upper;
    }

    public Mode mode() {
        return mode;
    }

    public long lower() {
        return lower;
    }

    public long upper() {
        return upper;
    }

    public boolean output() {
        return on;
    }

    /** Puts the remembered bit back after a world load or a preset change. */
    public void remember(boolean on) {
        this.on = on;
    }

    /** Applies one reading. An invalid pair leaves the output unchanged. */
    public boolean update(long reading) {
        if (!valid()) {
            return on;
        }
        if (mode == Mode.HEAT) {
            if (reading < lower) {
                on = true;
            } else if (reading > upper) {
                on = false;
            }
        } else if (reading > upper) {
            on = true;
        } else if (reading < lower) {
            on = false;
        }
        return on;
    }
}
