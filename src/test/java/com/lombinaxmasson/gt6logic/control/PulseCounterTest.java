package com.lombinaxmasson.gt6logic.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lombinaxmasson.gt6logic.control.PulseCounter.Mode;

import org.junit.jupiter.api.Test;

class PulseCounterTest {
    @Test
    void openTurnsOnAtTheTargetAndStaysThere() {
        PulseCounter counter = new PulseCounter(Mode.OPEN, 2);
        assertFalse(counter.output());
        assertFalse(counter.update(true, false));
        assertEquals(1, counter.count());
        assertFalse(counter.update(true, false));
        assertFalse(counter.update(false, false));
        assertTrue(counter.update(true, false));
        assertEquals(2, counter.count());
        assertTrue(counter.update(false, false));
        assertTrue(counter.update(true, false));
        assertEquals(2, counter.count());
    }

    @Test
    void shutStartsOnAndTurnsOffAtTheTarget() {
        PulseCounter counter = new PulseCounter(Mode.SHUT, 2);
        assertTrue(counter.output());
        assertTrue(counter.update(true, false));
        assertTrue(counter.update(false, false));
        assertFalse(counter.update(true, false));
        assertEquals(2, counter.count());
        assertFalse(counter.output());
    }

    @Test
    void resetClearsAndSwallowsAPulseOnTheSameTick() {
        PulseCounter counter = new PulseCounter(Mode.OPEN, 2);
        counter.update(true, false);
        counter.update(false, false);
        counter.update(true, false);
        assertTrue(counter.output());
        assertFalse(counter.update(true, true));
        assertEquals(0, counter.count());
        assertFalse(counter.update(true, true));
        assertEquals(0, counter.count());
        assertFalse(counter.update(false, true));
        assertFalse(counter.update(true, true));
        assertEquals(1, counter.count());
    }

    @Test
    void heldResetDoesNotClearAgain() {
        PulseCounter counter = new PulseCounter(Mode.SHUT, 1);
        assertFalse(counter.update(true, false));
        assertTrue(counter.update(false, true));
        assertTrue(counter.update(false, true));
        assertEquals(0, counter.count());
    }

    @Test
    void targetsCycleThePresets() {
        assertEquals(10, PulseCounter.DEFAULT_TARGET);
        assertEquals(20, PulseCounter.nextTarget(10));
        assertEquals(1, PulseCounter.nextTarget(64));
        assertEquals(10, PulseCounter.nextTarget(7));
        assertTrue(PulseCounter.allowedTarget(32));
        assertFalse(PulseCounter.allowedTarget(3));
    }

    @Test
    void restoreKeepsATrippedOutput() {
        PulseCounter counter = new PulseCounter(Mode.OPEN, 2);
        counter.restore(9, true, true, false);
        assertEquals(2, counter.count());
        assertTrue(counter.output());
        assertTrue(counter.update(false, false));
        assertEquals(2, counter.count());
        counter.restore(-1, false, false, false);
        assertEquals(0, counter.count());
        assertFalse(counter.output());
    }

    @Test
    void rejectsAMissingModeOrAZeroTarget() {
        assertThrows(IllegalArgumentException.class, () -> new PulseCounter(null, 1));
        assertThrows(IllegalArgumentException.class, () -> new PulseCounter(Mode.OPEN, 0));
    }
}
