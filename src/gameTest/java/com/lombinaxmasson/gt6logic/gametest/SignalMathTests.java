package com.lombinaxmasson.gt6logic.gametest;

import com.gregtech.gregtech.api.signal.GtSignalCarrier;
import com.lombinaxmasson.gt6logic.block.FormulaModuleBlock;
import com.lombinaxmasson.gt6logic.block.FormulaModuleBlockEntity;
import com.lombinaxmasson.gt6logic.block.LogicPlateBlock;
import com.lombinaxmasson.gt6logic.block.ProgramThermostatBlock;
import com.lombinaxmasson.gt6logic.block.ProgramThermostatBlockEntity;
import com.lombinaxmasson.gt6logic.block.ProportionalControllerBlock;
import com.lombinaxmasson.gt6logic.block.ProportionalControllerBlockEntity;
import com.lombinaxmasson.gt6logic.block.RatioControllerBlock;
import com.lombinaxmasson.gt6logic.block.RatioControllerBlockEntity;
import com.lombinaxmasson.gt6logic.control.TemperatureSchedule;
import com.lombinaxmasson.gt6logic.control.Temperatures;
import com.lombinaxmasson.gt6logic.signal.SignalBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Ratio, proportional, program thermostat, and formula. Front faces west. */
@GameTestHolder("gt6logic")
@PrefixGameTestTemplate(false)
public final class SignalMathTests {
    private SignalMathTests() {}

    @GameTest(template = "test_empty", timeoutTicks = 20)
    public static void ratioDoublesTheFront(GameTestHelper helper) {
        BlockPos pos = place(helper, new BlockPos(2, 1, 2), RatioControllerBlock.BLOCK.get());
        var front = GameTestSupport.placeProbe(helper, pos.west());
        front.setPending(Direction.WEST, GtSignalCarrier.UNIT);
        front.finishTick();
        helper.runAtTickTime(4, () -> {
            helper.assertTrue(ratio(helper, pos).ratio().text().equals("2"), "the default ratio is 2");
            helper.assertTrue(north(helper, pos) == 2L * GtSignalCarrier.UNIT, "the front is doubled");
            helper.assertTrue(GameTestSupport.toward(source(helper, pos), Direction.WEST) == 0L, "the front stays quiet");
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty", timeoutTicks = 20)
    public static void proportionalOpensWithTheError(GameTestHelper helper) {
        BlockPos relative = new BlockPos(2, 1, 5);
        BlockPos pos = place(helper, relative, ProportionalControllerBlock.BLOCK.get());
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
        BlockPos pos = place(helper, new BlockPos(2, 1, 8), ProgramThermostatBlock.BLOCK.get());
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

    @GameTest(template = "test_empty", timeoutTicks = 20)
    public static void formulaHalvesThenRejectsABadLine(GameTestHelper helper) {
        BlockPos pos = place(helper, new BlockPos(2, 1, 11), FormulaModuleBlock.BLOCK.get());
        var front = GameTestSupport.placeProbe(helper, pos.west());
        front.setPending(Direction.WEST, 4L * GtSignalCarrier.UNIT);
        front.finishTick();
        helper.runAtTickTime(4, () -> {
            helper.assertTrue(north(helper, pos) == 2L * GtSignalCarrier.UNIT, "half of four levels");
            formula(helper, pos).setFormula("nope");
        });
        helper.runAtTickTime(8, () -> {
            helper.assertTrue(north(helper, pos) == 0L, "a line that does not parse stays at zero");
            helper.succeed();
        });
    }

    private static BlockPos place(GameTestHelper helper, BlockPos relative, Block block) {
        BlockPos pos = helper.absolutePos(relative);
        BlockState state = block.defaultBlockState().setValue(LogicPlateBlock.FACING, Direction.WEST);
        helper.getLevel().setBlockAndUpdate(pos, state);
        return pos;
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
