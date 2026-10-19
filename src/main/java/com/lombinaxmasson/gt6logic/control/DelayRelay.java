package com.lombinaxmasson.gt6logic.control;

/**
 * Waits before turning on, or keeps the output on after the input leaves.
 * The caller decides what counts as an input. One update is one tick.
 */
public final class DelayRelay {
    public static final int TICKS_PER_SECOND = 20;
    /** Whole seconds a placed relay can be set to. The first entry is the default. */
    public static final int[] SECONDS = {1, 2, 5, 10, 20, 30, 60};

    public enum Mode {
        ON_DELAY,
        OFF_DELAY;

        public Mode next() {
            Mode[] values = values();
            return values[(ordinal() + 1) % values.length];
        }

        public static Mode byName(String name) {
            for (Mode mode : values()) {
                if (mode.name().equals(name)) {
                    return mode;
                }
            }
            return ON_DELAY;
        }
    }

    private final Mode mode;
    private final int delay;
    private int waited;
    private boolean output;

    public DelayRelay(Mode mode, int delay) {
        if (mode == null) {
            throw new IllegalArgumentException("delay relay mode is missing");
        }
        if (delay < 1) {
            throw new IllegalArgumentException("delay must be at least 1 tick, saw " + delay);
        }
        this.mode = mode;
        this.delay = delay;
    }

    public Mode mode() {
        return mode;
    }

    public int delay() {
        return delay;
    }

    public int waited() {
        return waited;
    }

    public boolean output() {
        return output;
    }

    /**
     * On-delay opens on the {@code delay}-th consecutive tick the input is present.
     * Off-delay opens immediately and then stays open for {@code delay} ticks after the input leaves.
     * A gap in an on-delay, or a return during an off-delay hold, starts that wait over.
     */
    public boolean update(boolean input) {
        if (mode == Mode.ON_DELAY) {
            if (!input) {
                waited = 0;
                output = false;
            } else if (waited < delay) {
                waited++;
                output = waited >= delay;
            } else {
                output = true;
            }
            return output;
        }
        if (input) {
            waited = 0;
            output = true;
        } else if (output) {
            if (waited >= delay) {
                output = false;
            } else {
                waited++;
            }
        }
        return output;
    }

    /** Continues a wait that was saved. Out-of-range waits clamp into {@code 0..delay}. */
    public void restore(int waited, boolean output) {
        if (waited < 0) {
            waited = 0;
        } else if (waited > delay) {
            waited = delay;
        }
        this.waited = waited;
        this.output = output;
    }

    /** Next preset after {@code seconds}. A value that is not in {@link #SECONDS} returns the first preset. */
    public static int nextSeconds(int seconds) {
        for (int i = 0; i < SECONDS.length; i++) {
            if (SECONDS[i] == seconds) {
                return SECONDS[(i + 1) % SECONDS.length];
            }
        }
        return SECONDS[0];
    }

    public static boolean allowedSeconds(int seconds) {
        for (int preset : SECONDS) {
            if (preset == seconds) {
                return true;
            }
        }
        return false;
    }
}
