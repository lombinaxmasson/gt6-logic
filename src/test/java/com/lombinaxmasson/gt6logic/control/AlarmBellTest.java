package com.lombinaxmasson.gt6logic.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gregtech.gregtech.api.signal.GtSignalCarrier;

import org.junit.jupiter.api.Test;

class AlarmBellTest {
    @Test
    void ringsOnlyWhenTheCeilingIsStrictlyHigher() {
        long unit = GtSignalCarrier.UNIT;
        assertFalse(AlarmBell.shouldRing(0, 0));
        assertFalse(AlarmBell.shouldRing(-unit, 0));
        assertTrue(AlarmBell.shouldRing(1, 0));
        assertTrue(AlarmBell.shouldRing(unit / 2, 0));
        assertFalse(AlarmBell.shouldRing(unit, 1));
        assertTrue(AlarmBell.shouldRing(unit + 1, 1));
        assertFalse(AlarmBell.shouldRing(14L * unit, 14));
        assertTrue(AlarmBell.shouldRing(14L * unit + 1, 14));
        assertTrue(AlarmBell.shouldRing(15L * unit, 14));
        assertTrue(AlarmBell.shouldRing(15L * unit + unit, 14));
    }

    @Test
    void levelCyclesThroughFourteen() {
        assertEquals(0, AlarmBell.MIN_LEVEL);
        assertEquals(14, AlarmBell.MAX_LEVEL);
        assertEquals(20, AlarmBell.PERIOD_TICKS);
        assertEquals(1, AlarmBell.nextLevel(0));
        assertEquals(14, AlarmBell.nextLevel(13));
        assertEquals(0, AlarmBell.nextLevel(14));
    }

    @Test
    void rejectsALevelOutsideZeroToFourteen() {
        assertThrows(IllegalArgumentException.class, () -> AlarmBell.shouldRing(0, -1));
        assertThrows(IllegalArgumentException.class, () -> AlarmBell.shouldRing(0, 15));
        assertThrows(IllegalArgumentException.class, () -> AlarmBell.nextLevel(-1));
        assertThrows(IllegalArgumentException.class, () -> AlarmBell.nextLevel(15));
    }
}
