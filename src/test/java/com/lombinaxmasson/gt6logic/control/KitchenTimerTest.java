package com.lombinaxmasson.gt6logic.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class KitchenTimerTest {
    @Test
    void onTwoOffThreeThenRepeats() {
        KitchenTimer timer = new KitchenTimer(5, 2);
        boolean[] cycle = {true, true, false, false, false};
        for (int lap = 0; lap < 2; lap++) {
            for (boolean expected : cycle) {
                assertEquals(expected, timer.output());
                timer.tick();
            }
        }
    }

    @Test
    void firstReadIsOn() {
        assertTrue(new KitchenTimer(5, 2).output());
        assertTrue(new KitchenTimer(1, 1).output());
    }

    @Test
    void durationCoveringThePeriodStaysOn() {
        assertStaysOn(new KitchenTimer(4, 4));
        assertStaysOn(new KitchenTimer(4, 9));
    }

    @Test
    void rejectsANonPositiveSpan() {
        assertThrows(IllegalArgumentException.class, () -> new KitchenTimer(0, 1));
        assertThrows(IllegalArgumentException.class, () -> new KitchenTimer(1, 0));
        assertThrows(IllegalArgumentException.class, () -> new KitchenTimer(-3, 2));
        assertThrows(IllegalArgumentException.class, () -> new KitchenTimer(2, -1));
    }

    private static void assertStaysOn(KitchenTimer timer) {
        for (int tick = 0; tick < 8; tick++) {
            assertTrue(timer.output());
            timer.tick();
        }
    }
}
