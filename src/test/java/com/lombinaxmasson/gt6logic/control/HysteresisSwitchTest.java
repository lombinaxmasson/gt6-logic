package com.lombinaxmasson.gt6logic.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import com.lombinaxmasson.gt6logic.control.HysteresisSwitch.Mode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class HysteresisSwitchTest {
    @Test
    void startsOff() {
        assertFalse(new HysteresisSwitch(Mode.HEAT, 30, 35).output());
        assertFalse(new HysteresisSwitch(Mode.COOL, 30, 35).output());
    }

    @ParameterizedTest
    @MethodSource("rows")
    void followsTheTable(Mode mode, long lower, long upper, long reading, boolean previous, boolean expected) {
        HysteresisSwitch sw = new HysteresisSwitch(mode, lower, upper);
        assertEquals(lower <= upper, sw.valid());
        if (previous) {
            assertTrue(sw.valid());
            long prime = mode == Mode.HEAT ? lower - 1 : upper + 1;
            assertTrue(sw.update(prime));
        } else {
            assertFalse(sw.output());
        }
        assertEquals(expected, sw.update(reading));
    }

    private static Stream<Arguments> rows() {
        return Stream.of(
                row(Mode.HEAT, 30, 35, 29, false, true),
                row(Mode.HEAT, 30, 35, 29, true, true),
                row(Mode.HEAT, 30, 35, 32, false, false),
                row(Mode.HEAT, 30, 35, 32, true, true),
                row(Mode.HEAT, 30, 35, 36, false, false),
                row(Mode.HEAT, 30, 35, 36, true, false),
                row(Mode.HEAT, 30, 35, 30, false, false),
                row(Mode.HEAT, 30, 35, 30, true, true),
                row(Mode.HEAT, 30, 35, 35, false, false),
                row(Mode.HEAT, 30, 35, 35, true, true),
                row(Mode.HEAT, 10, 10, 9, false, true),
                row(Mode.HEAT, 10, 10, 10, false, false),
                row(Mode.HEAT, 10, 10, 10, true, true),
                row(Mode.HEAT, 10, 10, 11, true, false),
                row(Mode.COOL, 30, 35, 36, false, true),
                row(Mode.COOL, 30, 35, 36, true, true),
                row(Mode.COOL, 30, 35, 32, false, false),
                row(Mode.COOL, 30, 35, 32, true, true),
                row(Mode.COOL, 30, 35, 29, false, false),
                row(Mode.COOL, 30, 35, 29, true, false),
                row(Mode.COOL, 30, 35, 35, false, false),
                row(Mode.COOL, 30, 35, 35, true, true),
                row(Mode.COOL, 30, 35, 30, false, false),
                row(Mode.COOL, 30, 35, 30, true, true),
                row(Mode.COOL, 10, 10, 11, false, true),
                row(Mode.COOL, 10, 10, 10, false, false),
                row(Mode.COOL, 10, 10, 10, true, true),
                row(Mode.COOL, 10, 10, 9, true, false),
                row(Mode.HEAT, 5, 4, 3, false, false),
                row(Mode.HEAT, 5, 4, 4, false, false),
                row(Mode.HEAT, 5, 4, 5, false, false),
                row(Mode.HEAT, 5, 4, 100, false, false),
                row(Mode.COOL, 5, 4, 0, false, false),
                row(Mode.COOL, 5, 4, 6, false, false));
    }

    private static Arguments row(Mode mode, long lower, long upper, long reading, boolean previous, boolean expected) {
        return Arguments.of(mode, lower, upper, reading, previous, expected);
    }
}
