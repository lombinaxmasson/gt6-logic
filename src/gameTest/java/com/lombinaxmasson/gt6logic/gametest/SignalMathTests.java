package com.lombinaxmasson.gt6logic.gametest;

import com.gregtech.gregtech.api.signal.GtSignalCarrier;
import com.lombinaxmasson.gt6logic.block.FormulaModuleBlock;
import com.lombinaxmasson.gt6logic.block.FormulaModuleBlockEntity;
import com.lombinaxmasson.gt6logic.block.ProgramThermostatBlock;
import com.lombinaxmasson.gt6logic.block.ProgramThermostatBlockEntity;
import com.lombinaxmasson.gt6logic.block.ProportionalControllerBlock;
import com.lombinaxmasson.gt6logic.block.ProportionalControllerBlockEntity;
import com.lombinaxmasson.gt6logic.block.RatioControllerBlock;
import com.lombinaxmasson.gt6logic.block.RatioControllerBlockEntity;
import com.lombinaxmasson.gt6logic.control.Formula;
import com.lombinaxmasson.gt6logic.control.TemperatureSchedule;
import com.lombinaxmasson.gt6logic.control.Temperatures;
import com.lombinaxmasson.gt6logic.signal.SignalBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Ratio, proportional, program thermostat, and formula. Front faces north. */
@GameTestHolder("gt6logic")
@PrefixGameTestTemplate(false)
public final class SignalMathTests {
    private SignalMathTests() {}

    @GameTest(template = "test_empty", timeoutTicks = 20)
    public static void ratioDoublesTheFront(GameTestHelper helper) {
        BlockPos pos = place(helper, new BlockPos(2, 1, 2), RatioControllerBlock.ITEM.get());
        var back = GameTestSupport.placeProbe(helper, pos.south());
        back.setPending(Direction.SOUTH, GtSignalCarrier.UNIT);
        back.finishTick();
        helper.runAtTickTime(4, () -> {
            helper.assertTrue(ratio(helper, pos).ratio().text().equals("2"), "the default ratio is 2");
            helper.assertTrue(north(helper, pos) == 2L * GtSignalCarrier.UNIT, "the reference is doubled");
            helper.assertTrue(GameTestSupport.toward(source(helper, pos), Direction.SOUTH) == 0L, "the reference face stays quiet");
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty", timeoutTicks = 20)
    public static void proportionalOpensWithTheError(GameTestHelper helper) {
        BlockPos relative = new BlockPos(2, 1, 5);
        BlockPos pos = place(helper, relative, ProportionalControllerBlock.ITEM.get());
        var front = GameTestSupport.placeProbe(helper, pos.west());
        var back = GameTestSupport.placeProbe(helper, pos.east());
        front.setPending(Direction.WEST, 4L * GtSignalCarrier.UNIT);
        back.setPending(Direction.EAST, GtSignalCarrier.UNIT);
        front.finishTick();
        back.finishTick();
        helper.runAtTickTime(4, () -> {
            helper.assertTrue(proportional(helper, pos).gain() == 1, "gain starts at 1");
            helper.assertTrue(north(helper, pos) == 3L * GtSignalCarrier.UNIT, "cool opens by the error");
            helper.useBlock(relative);
        });
        helper.runAtTickTime(8, () -> {
            helper.assertTrue(proportional(helper, pos).gain() == 2, "a click doubles the gain");
            helper.assertTrue(north(helper, pos) == 6L * GtSignalCarrier.UNIT, "the error is doubled");
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty", timeoutTicks = 20)
    public static void programStartsAtTheMashRest(GameTestHelper helper) {
        BlockPos pos = place(helper, new BlockPos(2, 1, 8), ProgramThermostatBlock.ITEM.get());
        GameTestSupport.placeProbe(helper, pos.west()).finishTick();
        long first = Temperatures.toSignal(52_000L);
        helper.runAtTickTime(3, () -> helper.assertTrue(north(helper, pos) == first, "0 °C against 52 °C opens to that target"));
        helper.runAtTickTime(4, () -> {
            ProgramThermostatBlockEntity program = program(helper, pos);
            CompoundTag saved = program.saveWithoutMetadata(helper.getLevel().registryAccess());
            saved.putInt("Elapsed", TemperatureSchedule.SEGMENT_TICKS);
            program.loadWithComponents(saved, helper.getLevel().registryAccess());
        });
        helper.runAtTickTime(8, () -> {
            helper.assertTrue(program(helper, pos).setpointMilli() == 63_000, "the saved step is 63 °C");
            helper.assertTrue(north(helper, pos) == Temperatures.toSignal(63_000L), "the opening follows that step");
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void formulaCyclesTheFourPresets(GameTestHelper helper) {
        BlockPos relative = new BlockPos(2, 1, 11);
        BlockPos pos = place(helper, relative, FormulaModuleBlock.ITEM.get());
        var front = GameTestSupport.placeProbe(helper, pos.west());
        var back = GameTestSupport.placeProbe(helper, pos.east());
        front.setPending(Direction.WEST, 6L * GtSignalCarrier.UNIT);
        back.setPending(Direction.EAST, 2L * GtSignalCarrier.UNIT);
        front.finishTick();
        back.finishTick();
        long five = 5L * GtSignalCarrier.UNIT;
        helper.runAtTickTime(4, () -> {
            helper.assertTrue(formula(helper, pos).formula().equals(Formula.PRESETS[0]), "the first preset is the half-sum");
            helper.assertTrue(north(helper, pos) == five, "clamp(a * 0.5 + b, 0, 15) is five levels");
            helper.assertTrue(GameTestSupport.toward(source(helper, pos), Direction.WEST) == 0L, "the left stays quiet");
            helper.assertTrue(GameTestSupport.toward(source(helper, pos), Direction.EAST) == 0L, "the right stays quiet");
            helper.useBlock(relative);
        });
        helper.runAtTickTime(8, () -> {
            helper.assertTrue(formula(helper, pos).formula().equals(Formula.PRESETS[1]), "the second preset is min");
            helper.assertTrue(north(helper, pos) == 2L * GtSignalCarrier.UNIT, "min(a, b) is two levels");
            helper.useBlock(relative);
        });
        helper.runAtTickTime(12, () -> {
            helper.assertTrue(formula(helper, pos).formula().equals(Formula.PRESETS[2]), "the third preset keeps a positive difference");
            helper.assertTrue(north(helper, pos) == 4L * GtSignalCarrier.UNIT, "max(a - b, 0) is four levels");
            helper.useBlock(relative);
        });
        helper.runAtTickTime(16, () -> {
            helper.assertTrue(formula(helper, pos).formula().equals(Formula.PRESETS[3]), "the fourth preset doubles");
            helper.assertTrue(north(helper, pos) == 12L * GtSignalCarrier.UNIT, "clamp(a * 2, 0, 15) is twelve levels");
            helper.useBlock(relative);
        });
        helper.runAtTickTime(20, () -> {
            helper.assertTrue(formula(helper, pos).formula().equals(Formula.PRESETS[0]), "the next click returns to the first line");
            helper.assertTrue(north(helper, pos) == five, "the first line is publishing again");
            formula(helper, pos).setFormula("nope");
        });
        helper.runAtTickTime(24, () -> {
            helper.assertTrue(north(helper, pos) == 0L, "a stored line that does not parse outputs zero");
            helper.useBlock(relative);
        });
        helper.runAtTickTime(28, () -> {
            helper.assertTrue(formula(helper, pos).formula().equals(Formula.PRESETS[0]), "a line outside the four presets snaps back");
            helper.assertTrue(north(helper, pos) == five, "right click does not keep the foreign line");
            helper.succeed();
        });
    }

    private static BlockPos place(GameTestHelper helper, BlockPos relative, Item item) {
        return GameTestSupport.placeGate(helper, item, helper.absolutePos(relative), Direction.NORTH);
    }

    private static long north(GameTestHelper helper, BlockPos pos) {
        return GameTestSupport.toward(source(helper, pos), Direction.NORTH);
    }

    private static SignalBlockEntity source(GameTestHelper helper, BlockPos pos) {
        BlockEntity entity = helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(entity instanceof SignalBlockEntity, "signal block");
        return (SignalBlockEntity) entity;
    }

    private static RatioControllerBlockEntity ratio(GameTestHelper helper, BlockPos pos) {
        return (RatioControllerBlockEntity) source(helper, pos);
    }

    private static ProportionalControllerBlockEntity proportional(GameTestHelper helper, BlockPos pos) {
        return (ProportionalControllerBlockEntity) source(helper, pos);
    }

    private static ProgramThermostatBlockEntity program(GameTestHelper helper, BlockPos pos) {
        return (ProgramThermostatBlockEntity) source(helper, pos);
    }

    private static FormulaModuleBlockEntity formula(GameTestHelper helper, BlockPos pos) {
        return (FormulaModuleBlockEntity) source(helper, pos);
    }
}
