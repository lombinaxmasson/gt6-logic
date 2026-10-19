package com.lombinaxmasson.gt6logic.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lombinaxmasson.gt6logic.control.DelayRelay.Mode;

import org.junit.jupiter.api.Test;

class DelayRelayTest {
    @Test
    void onDelayOpensOnTheNthTickAndAGapRestarts() {
        DelayRelay relay = new DelayRelay(Mode.ON_DELAY, 3);
        assertFalse(relay.output());
        assertEquals("FFTTF", run(relay, "TTT TF"));
        relay = new DelayRelay(Mode.ON_DELAY, 3);
        assertEquals("FFFFT", run(relay, "TFTTT"));
    }

    @Test
    void offDelayHoldsForTheDelayThenDrops() {
        DelayRelay relay = new DelayRelay(Mode.OFF_DELAY, 3);
        assertEquals("TTTTF", run(relay, "TFFFF"));
        relay = new DelayRelay(Mode.OFF_DELAY, 3);
        assertEquals("TTTTTTF", run(relay, "TFTFFFF"));
    }

    @Test
    void oneTickDelayIsTheArrivalItself() {
        assertEquals("TF", run(new DelayRelay(Mode.ON_DELAY, 1), "TF"));
        assertEquals("TTF", run(new DelayRelay(Mode.OFF_DELAY, 1), "TFF"));
    }

    @Test
    void secondsCycleThePresets() {
        assertEquals(1, DelayRelay.SECONDS[0]);
        assertEquals(20, DelayRelay.TICKS_PER_SECOND);
        assertEquals(2, DelayRelay.nextSeconds(1));
        assertEquals(60, DelayRelay.nextSeconds(30));
        assertEquals(1, DelayRelay.nextSeconds(60));
        assertEquals(1, DelayRelay.nextSeconds(3));
        assertFalse(DelayRelay.allowedSeconds(3));
        assertTrue(DelayRelay.allowedSeconds(60));
    }

    @Test
    void restoreClampsTheWait() {
        DelayRelay relay = new DelayRelay(Mode.OFF_DELAY, 3);
        relay.restore(9, true);
        assertEquals(3, relay.waited());
        assertTrue(relay.output());
        assertFalse(relay.update(false));
        relay.restore(-4, false);
        assertEquals(0, relay.waited());
        assertFalse(relay.output());
    }

    @Test
    void rejectsAMissingModeOrAZeroDelay() {
        assertThrows(IllegalArgumentException.class, () -> new DelayRelay(null, 1));
        assertThrows(IllegalArgumentException.class, () -> new DelayRelay(Mode.ON_DELAY, 0));
    }

    private static String run(DelayRelay relay, String inputs) {
        StringBuilder outputs = new StringBuilder();
        for (int i = 0; i < inputs.length(); i++) {
            char bit = inputs.charAt(i);
            if (bit == ' ') {
                continue;
            }
            outputs.append(relay.update(bit == 'T') ? 'T' : 'F');
        }
        return outputs.toString();
    }
}
