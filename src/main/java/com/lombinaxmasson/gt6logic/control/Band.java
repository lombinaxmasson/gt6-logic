package com.lombinaxmasson.gt6logic.control;

import com.lombinaxmasson.gt6logic.control.HysteresisSwitch.Mode;

/** One hysteresis preset. The numbers are in the caller's unit. */
public record Band(Mode mode, long lower, long upper) {
    public Band {
        if (mode == null) {
            throw new IllegalArgumentException("band mode is missing");
        }
    }

    public HysteresisSwitch open(boolean on) {
        HysteresisSwitch hysteresis = new HysteresisSwitch(mode, lower, upper);
        hysteresis.remember(on);
        return hysteresis;
    }
}
