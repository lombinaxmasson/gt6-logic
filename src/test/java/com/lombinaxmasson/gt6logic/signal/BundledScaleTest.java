package com.lombinaxmasson.gt6logic.signal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gregtech.gregtech.api.signal.GtSignalCarrier;

import org.junit.jupiter.api.Test;

class BundledScaleTest {
    @Test
    void endsAndTheMiddleRoundDown() {
        long full = 15L * GtSignalCarrier.UNIT;
        long half = full / 2;
        assertEquals(0, BundledScale.toBundled(0L));
        assertEquals(0, BundledScale.toBundled(-1L));
        assertEquals(127, BundledScale.toBundled(half));
        assertEquals(255, BundledScale.toBundled(full));
        assertEquals(255, BundledScale.toBundled(Long.MAX_VALUE));
        assertEquals(0L, BundledScale.toSignal(0));
        assertEquals(0L, BundledScale.toSignal(-4));
        assertEquals(full, BundledScale.toSignal(255));
        assertEquals(full, BundledScale.toSignal(400));
    }

    @Test
    void roundTripStaysWithinOne() {
        for (int bundled = 0; bundled <= 255; bundled++) {
            int back = BundledScale.toBundled(BundledScale.toSignal(bundled));
            assertTrue(Math.abs(back - bundled) <= 1, bundled + " came back as " + back);
        }
        assertEquals(0, BundledScale.toBundled(BundledScale.toSignal(0)));
        assertEquals(255, BundledScale.toBundled(BundledScale.toSignal(255)));
    }
}
