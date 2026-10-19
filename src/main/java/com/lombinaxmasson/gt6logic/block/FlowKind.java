package com.lombinaxmasson.gt6logic.block;

/** How a flow actuator turns a signal into millibuckets per tick. */
public enum FlowKind {
    /** Enough signal opens the full rate, and the coil costs 1 EU per tick. */
    VALVE(250, false, true),
    /** The rate follows the signal. No coil. */
    CONTROL(250, true, false),
    /** Same proportion as the control valve, four times the rate, and it needs power. */
    PUMP(1000, true, true);

    final int fullRate;
    final boolean proportional;
    final boolean needsPower;

    FlowKind(int fullRate, boolean proportional, boolean needsPower) {
        this.fullRate = fullRate;
        this.proportional = proportional;
        this.needsPower = needsPower;
    }
}
