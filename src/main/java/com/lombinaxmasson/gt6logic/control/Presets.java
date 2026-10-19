package com.lombinaxmasson.gt6logic.control;

/** Cycles a saved preset index. An index that is not in range returns to the first entry. */
public final class Presets {
    private Presets() {}

    public static int next(int index, int length) {
        if (length <= 0) {
            return 0;
        }
        int next = index + 1;
        return next >= 0 && next < length ? next : 0;
    }
}
