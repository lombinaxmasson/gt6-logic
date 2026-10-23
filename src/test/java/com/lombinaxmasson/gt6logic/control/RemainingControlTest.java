package com.lombinaxmasson.gt6logic.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gregtech.gregtech.api.signal.GtSignalCarrier;
import com.lombinaxmasson.gt6logic.control.Arithmetic.Op;
import com.lombinaxmasson.gt6logic.control.HysteresisSwitch.Mode;
import com.lombinaxmasson.gt6logic.energy.EuStore;

import org.junit.jupiter.api.Test;

class RemainingControlTest {
    private static final long UNIT = GtSignalCarrier.UNIT;
    private static final long FULL = 15L * UNIT;

    @Test
    void arithmeticCoversTheSixOperations() {
        assertEquals(3 * UNIT, Arithmetic.apply(Op.ADD, UNIT, 2 * UNIT));
        assertEquals(0L, Arithmetic.apply(Op.SUB, UNIT, 2 * UNIT));
        assertEquals(UNIT, Arithmetic.apply(Op.SUB, 2 * UNIT, UNIT));
        assertEquals(UNIT, Arithmetic.apply(Op.HALF, 2 * UNIT, 0L));
        assertEquals(FULL, Arithmetic.apply(Op.DOUBLE, 8 * UNIT, 0L));
        assertEquals(FULL, Arithmetic.apply(Op.DOUBLE, FULL, 0L));
        assertEquals(4 * UNIT, Arithmetic.apply(Op.MAX, UNIT, 4 * UNIT));
        assertEquals(UNIT, Arithmetic.apply(Op.MIN, UNIT, 4 * UNIT));
        assertEquals(Op.SUB, Op.ADD.next());
        assertEquals(Op.ADD, Op.byName("nope"));
    }

    @Test
    void ratioScalesTheReference() {
        assertEquals(4 * UNIT, RatioControl.desired(2 * UNIT, RatioControl.RATIOS[0]));
        assertEquals(UNIT, RatioControl.desired(4 * UNIT, new RatioControl.Ratio(1, 4)));
        assertEquals("1/2", new RatioControl.Ratio(1, 2).text());
        assertEquals("2", new RatioControl.Ratio(2, 1).text());
    }

    @Test
    void proportionalOpensWithTheError() {
        assertEquals(2 * UNIT, ProportionalControl.opening(Mode.COOL, 5 * UNIT, 3 * UNIT, 1));
        assertEquals(4 * UNIT, ProportionalControl.opening(Mode.COOL, 5 * UNIT, 3 * UNIT, 2));
        assertEquals(0L, ProportionalControl.opening(Mode.COOL, 3 * UNIT, 5 * UNIT, 1));
        assertEquals(2 * UNIT, ProportionalControl.opening(Mode.HEAT, 3 * UNIT, 5 * UNIT, 1));
        assertEquals(FULL, ProportionalControl.opening(Mode.COOL, FULL, 0L, 4));
        assertEquals(2, ProportionalControl.nextGain(1));
        assertEquals(1, ProportionalControl.nextGain(4));
        assertFalse(ProportionalControl.allowed(3));
    }

    @Test
    void formulaSpeaksInLevels() {
        assertEquals(2 * UNIT, Formula.evaluate("clamp(a * 0.5 + b, 0, 15)", 2 * UNIT, UNIT).signal());
        assertEquals(UNIT, Formula.evaluate("min(a, b)", 4 * UNIT, UNIT).signal());
        assertEquals(0L, Formula.evaluate("max(a - b, 0)", UNIT, 4 * UNIT).signal());
        assertEquals(FULL, Formula.evaluate("clamp(a * 2, 0, 15)", 8 * UNIT, 0L).signal());
        assertEquals(5 * UNIT, Formula.evaluate(Formula.PRESETS[0], 6 * UNIT, 2 * UNIT).signal());
        assertEquals(2 * UNIT, Formula.evaluate(Formula.PRESETS[1], 6 * UNIT, 2 * UNIT).signal());
        assertEquals(4 * UNIT, Formula.evaluate(Formula.PRESETS[2], 6 * UNIT, 2 * UNIT).signal());
        assertEquals(12 * UNIT, Formula.evaluate(Formula.PRESETS[3], 6 * UNIT, 2 * UNIT).signal());
        assertEquals(4, Formula.PRESETS.length);
        assertEquals(-UNIT, Formula.evaluate("a - b", UNIT, 2 * UNIT).signal());
        assertFalse(Formula.evaluate("nope", UNIT, UNIT).ok());
        assertFalse(Formula.evaluate("a / ", UNIT, UNIT).ok());
        assertEquals(0, Formula.nextPreset("not a preset"));
        assertEquals(1, Formula.nextPreset(Formula.PRESETS[0]));
        assertEquals(0, Formula.nextPreset(Formula.PRESETS[3]));
    }

    @Test
    void flowAndPortion() {
        assertEquals(250, FlowRate.millibuckets(UNIT, 250, false));
        assertEquals(0, FlowRate.millibuckets(0L, 250, false));
        assertEquals(3, FlowRate.millibuckets(3 * UNIT, 15, true));
        assertEquals(124, FlowRate.millibuckets(FULL / 2, 250, true));
        assertEquals(0, FlowRate.millibuckets(0L, 250, true));
        assertEquals(2_000, TenThousandths.of(2_000, 10_000));
        assertEquals(10_000, TenThousandths.of(11, 10));
        assertEquals(0, TenThousandths.of(5, 0));
        assertEquals(20, TenThousandths.percent(2_000));
        assertEquals(FULL, TenThousandths.toSignal(10_000));
        assertEquals(FULL / 2, TenThousandths.toSignal(5_000));
        assertEquals(4_999, TenThousandths.fromSignal(FULL / 2));
    }

    @Test
    void energyBufferAcceptsLvPacketsAndSpendsThem() {
        assertEquals(32L, EuStore.LV);
        EuStore store = new EuStore(100);
        assertEquals(1L, store.inject(EuStore.LV, 1L, true));
        assertEquals(0L, store.stored());
        assertEquals(1L, store.inject(EuStore.LV, 1L, false));
        assertEquals(32L, store.stored());
        assertEquals(1L, store.consumeUpTo(1L));
        assertEquals(31L, store.stored());
        assertEquals(1L, store.offered(31L));
        assertEquals(1L, store.extract(31L, 1L, false));
        assertEquals(0L, store.stored());
        store.restore(1_000L);
        assertEquals(100L, store.stored());
    }

    @Test
    void rememberedHysteresisSurvivesAReloadShape() {
        HysteresisSwitch hysteresis = new HysteresisSwitch(Mode.HEAT, 20, 80);
        hysteresis.remember(true);
        assertTrue(hysteresis.update(50));
        hysteresis.remember(false);
        assertFalse(hysteresis.update(50));
    }
}
