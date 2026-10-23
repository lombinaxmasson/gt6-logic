package com.lombinaxmasson.gt6logic.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gregtech.gregtech.api.signal.GtSignalCarrier;

import org.junit.jupiter.api.Test;

class RoomControlTest {
    private static final long UNIT = GtSignalCarrier.UNIT;
    private static final long FULL = 15L * UNIT;

    @Test
    void pointerStopsShortOfAFullTurn() {
        assertEquals(0F, PointerAngle.turns(0L), 1.0e-6F);
        assertEquals(15.5F / 32F, PointerAngle.turns(FULL / 2), 1.0e-5F);
        assertEquals(15.5F / 16F, PointerAngle.turns(FULL), 1.0e-5F);
        assertEquals(PointerAngle.turns(FULL), PointerAngle.turns(Long.MAX_VALUE), 1.0e-5F);
        assertTrue(PointerAngle.turns(FULL) < 1F);
        assertEquals(PointerAngle.turns(FULL) * ((float) Math.PI * 2F), PointerAngle.radians(FULL), 1.0e-4F);
    }

    @Test
    void chartKeepsOneMinuteAndDropsTheOldest() {
        ChartTrace trace = new ChartTrace();
        assertEquals(0, trace.count());
        assertTrue(trace.tick(UNIT));
        assertEquals(UNIT, trace.latest());
        for (int i = 0; i < ChartTrace.PERIOD - 1; i++) {
            assertFalse(trace.tick(2 * UNIT));
        }
        assertEquals(1, trace.count());
        assertTrue(trace.tick(2 * UNIT));
        assertEquals(UNIT, trace.lowest());
        assertEquals(2 * UNIT, trace.highest());

        for (int sample = 0; sample < ChartTrace.CAPACITY; sample++) {
            for (int skipped = 0; skipped < ChartTrace.PERIOD - 1; skipped++) {
                assertFalse(trace.tick(0L));
            }
            assertTrue(trace.tick(3 * UNIT + sample));
        }
        assertEquals(ChartTrace.CAPACITY, trace.count());
        assertEquals(3 * UNIT, trace.lowest());
        assertEquals(3 * UNIT + ChartTrace.CAPACITY - 1, trace.latest());
        assertEquals(trace.lowest(), trace.ordered()[0]);

        long[] extra = new long[ChartTrace.CAPACITY + 2];
        for (int i = 0; i < extra.length; i++) {
            extra[i] = i;
        }
        trace.restore(extra, 99);
        assertEquals(ChartTrace.CAPACITY, trace.count());
        assertEquals(2L, trace.ordered()[0]);
        assertEquals(ChartTrace.CAPACITY + 1L, trace.latest());
        assertEquals(ChartTrace.PERIOD, trace.untilNext());
        trace.restore(new long[0], -4);
        assertEquals(0, trace.count());
        assertEquals(0, trace.untilNext());
        assertTrue(trace.tick(UNIT));
    }
}
