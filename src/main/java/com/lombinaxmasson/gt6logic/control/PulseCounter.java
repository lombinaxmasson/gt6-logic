package com.lombinaxmasson.gt6logic.control;

/**
 * Counts rising edges until a target, then stays there.
 * A reset is also a rising edge. Reset and a pulse on the same tick: the reset wins and that pulse is not counted.
 */
public final class PulseCounter {
    /** Counts a placed counter can be set to. {@link #DEFAULT_TARGET} is the one a new counter uses. */
    public static final int[] TARGETS = {1, 2, 5, 10, 20, 32, 64};
    public static final int DEFAULT_TARGET = 10;

    public enum Mode {
        /** Starts off. Turns on once the count reaches the target, and stays on. */
        OPEN,
        /** Starts on. Turns off once the count reaches the target, and stays off. */
        SHUT;

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
            return OPEN;
        }
    }

    private final Mode mode;
    private final int target;
    private int count;
    private boolean pulseWas;
    private boolean resetWas;
    private boolean output;

    public PulseCounter(Mode mode, int target) {
        if (mode == null) {
            throw new IllegalArgumentException("counter mode is missing");
        }
        if (target < 1) {
            throw new IllegalArgumentException("counter target must be at least 1, saw " + target);
        }
        this.mode = mode;
        this.target = target;
        this.output = mode == Mode.SHUT;
    }

    public Mode mode() {
        return mode;
    }

    public int target() {
        return target;
    }

    public int count() {
        return count;
    }

    public boolean output() {
        return output;
    }

    public boolean pulseWas() {
        return pulseWas;
    }

    public boolean resetWas() {
        return resetWas;
    }

    public boolean update(boolean pulse, boolean reset) {
        boolean resetEdge = reset && !resetWas;
        resetWas = reset;
        if (resetEdge) {
            count = 0;
            output = mode == Mode.SHUT;
            pulseWas = pulse;
            return output;
        }
        if (pulse && !pulseWas && count < target) {
            count++;
            if (count >= target) {
                output = mode == Mode.OPEN;
            }
        }
        pulseWas = pulse;
        return output;
    }

    /**
     * Restores a saved count. A negative count becomes 0. A count past the target stops on the target.
     * The saved output is kept, so a counter that already tripped stays tripped.
     */
    public void restore(int count, boolean output, boolean pulseWas, boolean resetWas) {
        if (count < 0) {
            count = 0;
        } else if (count > target) {
            count = target;
        }
        this.count = count;
        this.output = output;
        this.pulseWas = pulseWas;
        this.resetWas = resetWas;
    }

    /** Next preset after {@code target}. A value that is not in {@link #TARGETS} returns {@link #DEFAULT_TARGET}. */
    public static int nextTarget(int target) {
        for (int i = 0; i < TARGETS.length; i++) {
            if (TARGETS[i] == target) {
                return TARGETS[(i + 1) % TARGETS.length];
            }
        }
        return DEFAULT_TARGET;
    }

    public static boolean allowedTarget(int target) {
        for (int preset : TARGETS) {
            if (preset == target) {
                return true;
            }
        }
        return false;
    }
}
