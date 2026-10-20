package com.lombinaxmasson.gt6logic.control;

/** Stays tripped until something resets it. A second press does not release it. */
public final class StopLatch {
    private boolean tripped;

    public void trip() {
        tripped = true;
    }

    public void reset() {
        tripped = false;
    }

    public boolean tripped() {
        return tripped;
    }

    public void restore(boolean tripped) {
        this.tripped = tripped;
    }
}
