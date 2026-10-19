package com.lombinaxmasson.gt6logic.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.gregtech.gregtech.api.signal.GtSignalCarrier;

import org.junit.jupiter.api.Test;

class HopCompensationTest {
    @Test
    void addsOneHopAndLeavesZeroAlone() {
        long unit = GtSignalCarrier.UNIT;
        assertEquals(0L, HopCompensation.restore(0L, 16));
        assertEquals(0L, HopCompensation.restore(-unit, 16));
        assertEquals(unit + unit / 16, HopCompensation.restore(unit, 16));
        assertEquals(unit + unit / 64, HopCompensation.restore(unit, 64));
        assertEquals(15L * unit, HopCompensation.restore(15L * unit, 16));
        assertEquals(15L * unit, HopCompensation.restore(15L * unit - unit / 16, 16));
    }

    @Test
    void rangesCycleTheWiresGtShips() {
        assertEquals(16, HopCompensation.RANGES[0]);
        assertEquals(64, HopCompensation.nextRange(16));
        assertEquals(16, HopCompensation.nextRange(64));
        assertEquals(16, HopCompensation.nextRange(32));
    }

    @Test
    void rejectsARangeThisModDoesNotKnow() {
        assertThrows(IllegalArgumentException.class, () -> HopCompensation.restore(1L, 32));
        assertThrows(IllegalArgumentException.class, () -> HopCompensation.restore(1L, 0));
    }
}
