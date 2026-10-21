package com.lombinaxmasson.gt6logic.gametest;

import com.gregtech.gregtech.api.signal.GtSignalCarrier;
import com.lombinaxmasson.gt6logic.block.AnalogPlate;
import com.lombinaxmasson.gt6logic.block.ArithmeticBlock;
import com.lombinaxmasson.gt6logic.block.FormulaModuleBlock;
import com.lombinaxmasson.gt6logic.block.FormulaModuleBlockEntity;
import com.lombinaxmasson.gt6logic.block.ProgramThermostatBlock;
import com.lombinaxmasson.gt6logic.block.ProportionalControllerBlock;
import com.lombinaxmasson.gt6logic.block.RatioControllerBlock;
import com.lombinaxmasson.gt6logic.control.Formula;
import com.lombinaxmasson.gt6logic.signal.SignalBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Wires, a direct chain, and a missing formula input. Every plate is placed by a mock player. */
@GameTestHolder("gt6logic")
@PrefixGameTestTemplate(false)
public final class AnalogPlateTests {
    private AnalogPlateTests() {}

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void wiresOnTheLeftAndRightAddOnTheFront(GameTestHelper helper) {
        BlockPos pos = GameTestSupport.placeGate(
                helper, ArithmeticBlock.ITEM.get(), helper.absolutePos(new BlockPos(3, 1, 2)), Direction.NORTH);
        BlockPos leftWire = pos.west();
        BlockPos rightWire = pos.east();
        BlockPos frontWire = pos.north();
        var leftProbe = GameTestSupport.placeProbe(helper, leftWire.west());
        var rightProbe = GameTestSupport.placeProbe(helper, rightWire.east());
        long four = 4L * GtSignalCarrier.UNIT;
        long two = 2L * GtSignalCarrier.UNIT;
        leftProbe.setPending(Direction.WEST, four + GameTestSupport.RED_ALLOY_LOSS);
        rightProbe.setPending(Direction.EAST, two + GameTestSupport.RED_ALLOY_LOSS);
        leftProbe.finishTick();
        rightProbe.finishTick();
        GameTestSupport.placeWire(helper, leftWire, Direction.WEST);
        GameTestSupport.placeWire(helper, rightWire, Direction.EAST);
        GameTestSupport.placeWire(helper, frontWire, Direction.SOUTH);
        helper.runAtTickTime(8, () -> {
            long left = GameTestSupport.wire(helper, leftWire);
            long right = GameTestSupport.wire(helper, rightWire);
            helper.assertTrue(left == four, "left wire is four, saw " + left);
            helper.assertTrue(right == two, "right wire is two, saw " + right);
            long sum = GtSignalCarrier.clamp(left + right);
            SignalBlockEntity plate = source(helper, pos);
            helper.assertTrue(GameTestSupport.toward(plate, Direction.NORTH) == sum, "the front face is the sum");
            long front = GameTestSupport.wire(helper, frontWire);
            helper.assertTrue(front == sum - GameTestSupport.RED_ALLOY_LOSS,
                    "the front wire reads the sum less one hop, saw " + front);
            helper.assertTrue(GameTestSupport.toward(plate, Direction.SOUTH) == 0L, "the back is quiet");
            helper.assertTrue(GameTestSupport.toward(plate, Direction.UP) == 0L, "up is quiet");
            helper.assertTrue(GameTestSupport.toward(plate, Direction.DOWN) == 0L, "down is quiet");
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void arithmeticFrontFeedsProportionalLeft(GameTestHelper helper) {
        BlockPos arithmetic = GameTestSupport.placeGate(
                helper, ArithmeticBlock.ITEM.get(), helper.absolutePos(new BlockPos(3, 1, 3)), Direction.NORTH);
        BlockPos proportional = GameTestSupport.placeGate(
                helper, ProportionalControllerBlock.ITEM.get(), arithmetic.north(), Direction.WEST);
        var left = GameTestSupport.placeProbe(helper, arithmetic.west());
        var right = GameTestSupport.placeProbe(helper, arithmetic.east());
        left.setPending(Direction.WEST, GtSignalCarrier.UNIT);
        right.setPending(Direction.EAST, 2L * GtSignalCarrier.UNIT);
        left.finishTick();
        right.finishTick();
        helper.runAtTickTime(8, () -> {
            long sum = 3L * GtSignalCarrier.UNIT;
            helper.assertTrue(GameTestSupport.toward(source(helper, arithmetic), Direction.NORTH) == sum, "the sum leaves north");
            helper.assertTrue(GameTestSupport.toward(source(helper, proportional), Direction.WEST) == sum,
                    "the proportional opening follows that sum, saw "
                            + GameTestSupport.toward(source(helper, proportional), Direction.WEST));
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void formulaTreatsAMissingInputAsZero(GameTestHelper helper) {
        BlockPos relative = new BlockPos(3, 1, 2);
        BlockPos pos = GameTestSupport.placeGate(
                helper, FormulaModuleBlock.ITEM.get(), helper.absolutePos(relative), Direction.NORTH);
        var left = GameTestSupport.placeProbe(helper, pos.west());
        var right = GameTestSupport.placeProbe(helper, pos.east());
        var back = GameTestSupport.placeProbe(helper, pos.south());
        left.setPending(Direction.WEST, 6L * GtSignalCarrier.UNIT);
        right.setPending(Direction.EAST, 2L * GtSignalCarrier.UNIT);
        back.setPending(Direction.SOUTH, 9L * GtSignalCarrier.UNIT);
        left.finishTick();
        right.finishTick();
        back.finishTick();
        long five = 5L * GtSignalCarrier.UNIT;
        helper.runAtTickTime(4, () -> {
            FormulaModuleBlockEntity formula = formula(helper, pos);
            helper.assertTrue(formula.formula().equals(Formula.PRESETS[0]), "the first preset");
            helper.assertTrue(GameTestSupport.toward(formula, Direction.NORTH) == five,
                    "half of a plus b, the back is unused");
            left.setPending(Direction.WEST, 0L);
            left.finishTick();
        });
        helper.runAtTickTime(8, () -> {
            helper.assertTrue(GameTestSupport.toward(formula(helper, pos), Direction.NORTH) == 2L * GtSignalCarrier.UNIT,
                    "a missing a counts as zero");
            helper.assertTrue(GameTestSupport.toward(formula(helper, pos), Direction.SOUTH) == 0L, "the back output stays quiet");
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty", timeoutTicks = 20)
    public static void fivePlatesFaceTheWayThePlayerLooks(GameTestHelper helper) {
        place(helper, ArithmeticBlock.ITEM.get(), new BlockPos(2, 1, 2), Direction.NORTH);
        place(helper, ProportionalControllerBlock.ITEM.get(), new BlockPos(5, 1, 2), Direction.EAST);
        place(helper, RatioControllerBlock.ITEM.get(), new BlockPos(8, 1, 2), Direction.SOUTH);
        place(helper, FormulaModuleBlock.ITEM.get(), new BlockPos(11, 1, 2), Direction.WEST);
        place(helper, ProgramThermostatBlock.ITEM.get(), new BlockPos(14, 1, 2), Direction.NORTH);
        helper.succeed();
    }

    private static void place(GameTestHelper helper, Item item, BlockPos relative, Direction front) {
        BlockPos pos = GameTestSupport.placeGate(helper, item, helper.absolutePos(relative), front);
        helper.assertTrue(helper.getLevel().getBlockState(pos).getValue(AnalogPlate.INPUT_LEFT)
                || helper.getLevel().getBlockState(pos).getValue(AnalogPlate.INPUT_RIGHT)
                || helper.getLevel().getBlockState(pos).getValue(AnalogPlate.INPUT_BACK),
                "the plate names at least one input");
    }

    private static SignalBlockEntity source(GameTestHelper helper, BlockPos pos) {
        BlockEntity entity = helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(entity instanceof SignalBlockEntity, "signal block");
        return (SignalBlockEntity) entity;
    }

    private static FormulaModuleBlockEntity formula(GameTestHelper helper, BlockPos pos) {
        return (FormulaModuleBlockEntity) source(helper, pos);
    }
}
