package com.lombinaxmasson.gt6logic.signal;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gregtech.gregtech.api.signal.GtSignalCarrier;

import org.junit.jupiter.api.Test;

class PublishedSignalTest {
    @Test
    void pendingStaysHiddenUntilPublish() {
        PublishedSignal signal = new PublishedSignal();
        signal.set(2, GtSignalCarrier.UNIT / 2);
        assertEquals(0L, signal.get(2));
        assertTrue(signal.publish());
        assertEquals(GtSignalCarrier.UNIT / 2, signal.get(2));
        assertFalse(signal.publish());
    }

    @Test
    void valuesAreClampedOnTheWayIn() {
        PublishedSignal signal = new PublishedSignal();
        signal.set(0, -1);
        signal.set(1, 15L * GtSignalCarrier.UNIT + 9);
        signal.publish();
        assertEquals(0L, signal.get(0));
        assertEquals(15L * GtSignalCarrier.UNIT, signal.get(1));
    }

    @Test
    void aBadSideIsRejected() {
        PublishedSignal signal = new PublishedSignal();
        assertThrows(IllegalArgumentException.class, () -> signal.set(-1, 1));
        assertThrows(IllegalArgumentException.class, () -> signal.get(6));
    }

    @Test
    void aSavedSnapshotRestoresPublishedAndPending() {
        PublishedSignal signal = new PublishedSignal();
        long[] saved = new long[] {1, 2, 3};
        signal.loadPublished(saved);
        assertEquals(1L, signal.get(0));
        assertEquals(0L, signal.get(3));
        signal.set(0, 9);
        assertEquals(1L, signal.get(0));
        signal.publish();
        assertEquals(9L, signal.get(0));
        assertEquals(2L, signal.get(1));
        assertArrayEquals(signal.publishedCopy(), new long[] {9, 2, 3, 0, 0, 0});
    }

    @Test
    void anUnwrittenFaceKeepsItsPendingValue() {
        PublishedSignal signal = new PublishedSignal();
        signal.set(4, 4);
        signal.publish();
        signal.set(5, 5);
        signal.publish();
        assertEquals(4L, signal.get(4));
        assertEquals(5L, signal.get(5));
    }
}
