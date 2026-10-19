package com.lombinaxmasson.gt6logic.control;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.gregtech.gregtech.api.signal.GtSignalCarrier;

import org.junit.jupiter.api.Test;

class SignalReadoutTest {
    @Test
    void formatsFourDecimalLevels() {
        long unit = GtSignalCarrier.UNIT;
        assertEquals("0.0000", SignalReadout.format(0));
        assertEquals("0.0000", SignalReadout.format(-1));
        assertEquals("0.0000", SignalReadout.format(1));
        assertEquals("0.5000", SignalReadout.format(unit / 2));
        assertEquals("1.0000", SignalReadout.format(unit));
        assertEquals("0.0625", SignalReadout.format(unit / 16));
        assertEquals("14.9375", SignalReadout.format(15L * unit - unit / 16));
        assertEquals("15.0000", SignalReadout.format(15L * unit));
        assertEquals("15.0000", SignalReadout.format(15L * unit + unit));
    }
}
