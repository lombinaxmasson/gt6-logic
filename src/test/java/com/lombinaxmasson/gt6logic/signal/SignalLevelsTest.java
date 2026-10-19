package com.lombinaxmasson.gt6logic.signal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigInteger;

import com.gregtech.gregtech.api.signal.GtSignalCarrier;

import org.junit.jupiter.api.Test;

class SignalLevelsTest {
    @Test
    void ceilingRoundsUpAndKeepsZero() {
        long unit = GtSignalCarrier.UNIT;
        assertEquals(0, SignalLevels.ceilingLevel(0));
        assertEquals(0, SignalLevels.ceilingLevel(-unit));
        assertEquals(1, SignalLevels.ceilingLevel(1));
        assertEquals(1, SignalLevels.ceilingLevel(unit / 2));
        assertEquals(1, SignalLevels.ceilingLevel(unit));
        assertEquals(2, SignalLevels.ceilingLevel(unit + 1));
        assertEquals(15, SignalLevels.ceilingLevel(15L * unit));
        assertEquals(15, SignalLevels.ceilingLevel(15L * unit + unit));
    }

    @Test
    void fromLevelClampsToFifteen() {
        long unit = GtSignalCarrier.UNIT;
        assertEquals(0L, SignalLevels.fromLevel(0));
        assertEquals(0L, SignalLevels.fromLevel(-2));
        assertEquals(unit, SignalLevels.fromLevel(1));
        assertEquals(15L * unit, SignalLevels.fromLevel(15));
        assertEquals(15L * unit, SignalLevels.fromLevel(16));
        assertEquals(1, SignalLevels.ceilingLevel(unit / 2));
        assertNotEquals(SignalLevels.fromLevel(1), unit / 2);
    }

    @Test
    void scaleMatchesTheFullProduct() {
        long unit = GtSignalCarrier.UNIT;
        long full = 15L * unit;
        long[][] cases = {
                {0, 5, 3},
                {6, 7, 2},
                {1, 1, unit},
                {unit, unit, unit},
                {unit - 1, full, unit},
                {full, full, unit},
                {full, full, 1},
                {unit, 3, 2},
                {Long.MAX_VALUE, 2, 2},
                {Long.MAX_VALUE, 2, 1},
                {Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE},
                {5, 1, 2},
        };
        for (long[] row : cases) {
            assertEquals(reference(row[0], row[1], row[2]),
                    SignalLevels.multiplyDivide(row[0], row[1], row[2]),
                    row[0] + " * " + row[1] + " / " + row[2]);
        }
        assertEquals(225L * unit, SignalLevels.multiplyDivide(full, full, unit));
    }

    @Test
    void scaleRejectsANegativeOrZeroDivisor() {
        assertThrows(IllegalArgumentException.class, () -> SignalLevels.multiplyDivide(-1, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> SignalLevels.multiplyDivide(1, -1, 1));
        assertThrows(IllegalArgumentException.class, () -> SignalLevels.multiplyDivide(1, 1, 0));
    }

    private static long reference(long left, long right, long divisor) {
        BigInteger quotient = BigInteger.valueOf(left)
                .multiply(BigInteger.valueOf(right))
                .divide(BigInteger.valueOf(divisor));
        if (quotient.bitLength() > 63) {
            return Long.MAX_VALUE;
        }
        return quotient.longValue();
    }
}
