package com.lombinaxmasson.gt6logic.gametest;

import com.gregtech.gregtech.api.signal.GtSignalCarrier;
import com.lombinaxmasson.gt6logic.block.ChartRecorderBlock;
import com.lombinaxmasson.gt6logic.block.ChartRecorderBlockEntity;
import com.lombinaxmasson.gt6logic.block.PointerGaugeBlock;
import com.lombinaxmasson.gt6logic.block.PointerGaugeBlockEntity;
import com.lombinaxmasson.gt6logic.block.SignalMeterBlock;
import com.lombinaxmasson.gt6logic.block.SignalMeterBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The free face is the only one a meter reads. Dust does not connect. */
@GameTestHolder("gt6logic")
@PrefixGameTestTemplate(false)
public final class ReadoutFaceTests {
    private ReadoutFaceTests() {}

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void signalMeterReadsOnlyTheFreeFace(GameTestHelper helper) {
        BlockPos pos = place(helper, SignalMeterBlock.ITEM.get(), new BlockPos(2, 1, 2));
        feed(helper, pos);
        helper.runAtTickTime(8, () -> {
            long front = GameTestSupport.wire(helper, pos.east());
            long side = GameTestSupport.wire(helper, pos.north());
            SignalMeterBlockEntity meter = entity(helper, pos, SignalMeterBlockEntity.class);
            helper.assertTrue(front == 4L * GtSignalCarrier.UNIT, "front wire is four, saw " + front);
            helper.assertTrue(side == GtSignalCarrier.UNIT, "side wire is one, saw " + side);
            helper.assertTrue(meter.reading() == front, "the meter reads the front wire, saw " + meter.reading());
            helper.getLevel().setBlockAndUpdate(pos.east(), Blocks.AIR.defaultBlockState());
            assertNoRedstone(helper, pos);
        });
        helper.runAtTickTime(14, () -> {
            helper.assertTrue(GameTestSupport.wire(helper, pos.north()) > 0L, "the side wire is still live");
            helper.assertTrue(entity(helper, pos, SignalMeterBlockEntity.class).reading() == 0L, "another face reads as zero");
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void pointerGaugeReadsOnlyTheFreeFace(GameTestHelper helper) {
        BlockPos pos = place(helper, PointerGaugeBlock.ITEM.get(), new BlockPos(2, 1, 2));
        feed(helper, pos);
        helper.runAtTickTime(8, () -> {
            long front = GameTestSupport.wire(helper, pos.east());
            PointerGaugeBlockEntity gauge = entity(helper, pos, PointerGaugeBlockEntity.class);
            helper.assertTrue(front == 4L * GtSignalCarrier.UNIT, "front wire is four, saw " + front);
            helper.assertTrue(gauge.reading() == front, "the gauge reads the front wire, saw " + gauge.reading());
            helper.getLevel().setBlockAndUpdate(pos.east(), Blocks.AIR.defaultBlockState());
            assertNoRedstone(helper, pos);
        });
        helper.runAtTickTime(14, () -> {
            helper.assertTrue(entity(helper, pos, PointerGaugeBlockEntity.class).reading() == 0L, "another face reads as zero");
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty", timeoutTicks = 60)
    public static void chartRecorderReadsOnlyTheFreeFace(GameTestHelper helper) {
        BlockPos pos = place(helper, ChartRecorderBlock.ITEM.get(), new BlockPos(2, 1, 2));
        feed(helper, pos);
        helper.runAtTickTime(8, () -> helper.assertTrue(
                GameTestSupport.wire(helper, pos.east()) == 4L * GtSignalCarrier.UNIT, "front wire is four"));
        // The first sample can land before the wire has a signal. The next one is a second later.
        helper.runAtTickTime(22, () -> {
            long front = GameTestSupport.wire(helper, pos.east());
            helper.assertTrue(entity(helper, pos, ChartRecorderBlockEntity.class).latest() == front,
                    "the chart reads the front wire, saw " + entity(helper, pos, ChartRecorderBlockEntity.class).latest());
            helper.getLevel().setBlockAndUpdate(pos.east(), Blocks.AIR.defaultBlockState());
            assertNoRedstone(helper, pos);
        });
        helper.runAtTickTime(42, () -> {
            helper.assertTrue(entity(helper, pos, ChartRecorderBlockEntity.class).latest() == 0L, "another face reads as zero");
            helper.succeed();
        });
    }

    /** Wall on the west. Facing points at it, so the free face is east. */
    private static BlockPos place(GameTestHelper helper, Item item, BlockPos relative) {
        BlockPos pos = helper.absolutePos(relative);
        BlockPos wall = pos.west();
        helper.getLevel().setBlockAndUpdate(wall, Blocks.STONE.defaultBlockState());
        BlockPos placed = GameTestSupport.placeBlock(helper, item, wall, Direction.EAST, Direction.EAST);
        helper.assertTrue(placed.equals(pos), item + " landed at " + placed);
        helper.assertTrue(helper.getLevel().getBlockState(pos).getValue(BlockStateProperties.FACING) == Direction.WEST,
                "facing points at the wall");
        return pos;
    }

    /** A strong wire on the free face and a weaker wire on the north face. */
    private static void feed(GameTestHelper helper, BlockPos pos) {
        BlockPos front = pos.east();
        BlockPos side = pos.north();
        var frontProbe = GameTestSupport.placeProbe(helper, front.east());
        var sideProbe = GameTestSupport.placeProbe(helper, side.north());
        frontProbe.setPending(Direction.EAST, 4L * GtSignalCarrier.UNIT + GameTestSupport.RED_ALLOY_LOSS);
        sideProbe.setPending(Direction.NORTH, GtSignalCarrier.UNIT + GameTestSupport.RED_ALLOY_LOSS);
        frontProbe.finishTick();
        sideProbe.finishTick();
        GameTestSupport.placeWire(helper, front, Direction.EAST);
        GameTestSupport.placeWire(helper, side, Direction.NORTH);
    }

    private static void assertNoRedstone(GameTestHelper helper, BlockPos pos) {
        BlockState state = helper.getLevel().getBlockState(pos);
        for (Direction side : Direction.values()) {
            helper.assertTrue(!state.getBlock().canConnectRedstone(state, helper.getLevel(), pos, side), side + " connects");
            helper.assertTrue(state.getSignal(helper.getLevel(), pos, side) == 0, side + " getSignal");
        }
        helper.assertTrue(!state.getBlock().canConnectRedstone(state, helper.getLevel(), pos, null), "above connects");
        BlockPos dustPos = pos.south();
        helper.getLevel().setBlockAndUpdate(dustPos.below(), Blocks.STONE.defaultBlockState());
        GameTestSupport.placeBlock(helper, Items.REDSTONE, dustPos.below(), Direction.UP, Direction.NORTH);
        BlockState dust = helper.getLevel().getBlockState(dustPos);
        // A line into the meter would drop the east and west arms. A dot keeps the cross and stays dark.
        helper.assertTrue(dust.getValue(RedStoneWireBlock.EAST) == RedstoneSide.SIDE
                && dust.getValue(RedStoneWireBlock.WEST) == RedstoneSide.SIDE
                && dust.getValue(RedStoneWireBlock.POWER) == 0,
                "dust does not enter the meter, " + dust);
    }

    private static <T> T entity(GameTestHelper helper, BlockPos pos, Class<T> type) {
        BlockEntity entity = helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(type.isInstance(entity), type.getSimpleName());
        return type.cast(entity);
    }
}
