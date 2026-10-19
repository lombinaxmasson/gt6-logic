package com.lombinaxmasson.gt6logic.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lombinaxmasson.gt6logic.control.Interlock.Mode;

import org.junit.jupiter.api.Test;

class InterlockTest {
    @Test
    void andOrAndNot() {
        assertFalse(Interlock.output(Mode.AND, true, false));
        assertFalse(Interlock.output(Mode.AND, false, true));
        assertTrue(Interlock.output(Mode.AND, true, true));
        assertFalse(Interlock.output(Mode.AND, false, false));

        assertTrue(Interlock.output(Mode.OR, true, false));
        assertTrue(Interlock.output(Mode.OR, false, true));
        assertTrue(Interlock.output(Mode.OR, true, true));
        assertFalse(Interlock.output(Mode.OR, false, false));

        assertFalse(Interlock.output(Mode.NOT, true, true));
        assertFalse(Interlock.output(Mode.NOT, true, false));
        assertTrue(Interlock.output(Mode.NOT, false, true));
        assertTrue(Interlock.output(Mode.NOT, false, false));
    }

    @Test
    void modeCyclesAndUnknownNamesFallBack() {
        assertEquals(Mode.OR, Mode.AND.next());
        assertEquals(Mode.NOT, Mode.OR.next());
        assertEquals(Mode.AND, Mode.NOT.next());
        assertEquals(Mode.OR, Mode.byName("OR"));
        assertEquals(Mode.AND, Mode.byName("nope"));
    }

    @Test
    void rejectsAMissingMode() {
        assertThrows(IllegalArgumentException.class, () -> Interlock.output(null, true, true));
    }
}
