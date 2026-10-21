package com.lombinaxmasson.gt6logic.gametest;

import com.gregtech.gregtech.api.signal.GtSignalCarrier;
import com.gregtech.gregtech.api.signal.GtSignalReceiver;
import com.gregtech.gregtech.api.signal.GtSignalSource;
import com.lombinaxmasson.gt6logic.block.CompensationRepeaterBlock;
import com.lombinaxmasson.gt6logic.block.CompensationRepeaterBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** One red-alloy hop is put back. Zero stays zero, and the input face stays quiet. */
@GameTestHolder("gt6logic")
@PrefixGameTestTemplate(false)
public final class CompensationRepeaterTests {
    private CompensationRepeaterTests() {}

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void restoresOneRedAlloyHop(GameTestHelper helper) {
        BlockPos redstonePos = helper.absolutePos(new BlockPos(1, 1, 2));
        BlockPos wirePos = redstonePos.east();
        BlockPos repeaterPos = wirePos.east();
        helper.getLevel().setBlockAndUpdate(redstonePos, Blocks.REDSTONE_BLOCK.defaultBlockState());
        GameTestSupport.placeWire(helper, wirePos, Direction.WEST);
        place(helper, repeaterPos, Direction.EAST);
        BlockPos dustPos = repeaterPos.east();
        BlockPos sidePos = repeaterPos.north();
        helper.getLevel().setBlockAndUpdate(dustPos.below(), Blocks.STONE.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(sidePos.below(), Blocks.STONE.defaultBlockState());
        GameTestSupport.placeBlock(helper, Items.REDSTONE, dustPos.below(), Direction.UP, Direction.NORTH);
        GameTestSupport.placeBlock(helper, Items.REDSTONE, sidePos.below(), Direction.UP, Direction.NORTH);
        helper.runAtTickTime(6, () -> {
            CompensationRepeaterBlockEntity repeater = repeater(helper, repeaterPos);
            for (Direction side : Direction.values()) {
                long want = side == Direction.EAST ? 15L * GtSignalCarrier.UNIT : 0L;
                long read = GameTestSupport.toward(repeater, side);
                helper.assertTrue(read == want, side + " is " + read);
            }
            BlockState state = repeater.getBlockState();
            for (Direction query : Direction.values()) {
                boolean connect = state.getBlock().canConnectRedstone(state, helper.getLevel(), repeaterPos, query);
                helper.assertTrue(connect == (query == Direction.WEST), query + " connect " + connect);
            }
            helper.assertTrue(!state.getBlock().canConnectRedstone(state, helper.getLevel(), repeaterPos, null),
                    "a wire above does not connect");
            BlockState dust = helper.getLevel().getBlockState(dustPos);
            BlockState side = helper.getLevel().getBlockState(sidePos);
            helper.assertTrue(dust.getValue(RedStoneWireBlock.WEST) != RedstoneSide.NONE, "dust connects on the output, " + dust);
            helper.assertTrue(dust.getValue(RedStoneWireBlock.POWER) == 15, "output dust is on, " + dust);
            // A line into the repeater would drop the east and west arms. A dot keeps the cross.
            helper.assertTrue(side.getValue(RedStoneWireBlock.EAST) == RedstoneSide.SIDE
                    && side.getValue(RedStoneWireBlock.WEST) == RedstoneSide.SIDE
                    && side.getValue(RedStoneWireBlock.POWER) == 0,
                    "side dust stays off the repeater, " + side);
            helper.assertTrue(repeater.getBlockState().getValue(CompensationRepeaterBlock.POWERED), "the face lights");
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty", timeoutTicks = 20)
    public static void playerPlacesTheOutputAwayFromTheClickedBlock(GameTestHelper helper) {
        BlockPos anchor = helper.absolutePos(new BlockPos(2, 1, 11));
        helper.getLevel().setBlockAndUpdate(anchor, Blocks.STONE.defaultBlockState());
        BlockPos placed = GameTestSupport.placeBlock(
                helper, CompensationRepeaterBlock.ITEM.get(), anchor, Direction.EAST, Direction.EAST);
        helper.assertTrue(helper.getLevel().getBlockState(placed).getValue(CompensationRepeaterBlock.FACING) == Direction.EAST,
                "output continues east, away from the clicked block");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void zeroStaysZeroAndRangeSixtyFourAddsASmallerHop(GameTestHelper helper) {
        BlockPos relative = new BlockPos(2, 1, 5);
        BlockPos repeaterPos = helper.absolutePos(relative);
        place(helper, repeaterPos, Direction.EAST);
        var probe = GameTestSupport.placeProbe(helper, repeaterPos.west());
        probe.setPending(Direction.WEST, 0);
        probe.finishTick();
        helper.runAtTickTime(4, () -> helper.assertTrue(
                GameTestSupport.toward(repeater(helper, repeaterPos), Direction.EAST) == 0L,
                "zero is not boosted"));
        helper.runAtTickTime(5, () -> {
            helper.useBlock(relative);
            helper.assertTrue(repeater(helper, repeaterPos).range() == 64, "right click selects signalum range");
            probe.setPending(Direction.WEST, GtSignalCarrier.UNIT);
            probe.finishTick();
        });
        helper.runAtTickTime(9, () -> {
            long expected = GtSignalCarrier.UNIT + GtSignalCarrier.UNIT / 64;
            helper.assertTrue(GameTestSupport.toward(repeater(helper, repeaterPos), Direction.EAST) == expected,
                    "range 64 adds UNIT / 64, saw " + GameTestSupport.toward(repeater(helper, repeaterPos), Direction.EAST));
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty", timeoutTicks = 20)
    public static void unknownRangeReturnsToRedAlloy(GameTestHelper helper) {
        BlockPos relative = new BlockPos(2, 1, 8);
        BlockPos pos = helper.absolutePos(relative);
        CompensationRepeaterBlockEntity repeater = place(helper, pos, Direction.NORTH);
        helper.useBlock(relative);
        CompoundTag saved = repeater.saveWithoutMetadata(helper.getLevel().registryAccess());
        repeater.loadWithComponents(saved, helper.getLevel().registryAccess());
        helper.assertTrue(repeater.range() == 64, "the range reloads");
        saved.putInt("Range", 32);
        repeater.loadWithComponents(saved, helper.getLevel().registryAccess());
        helper.assertTrue(repeater.range() == 16, "an unknown range returns to 16");
        helper.useBlock(relative);
        helper.assertTrue(repeater.range() == 64, "the next click is 64 again");
        helper.useBlock(relative);
        helper.assertTrue(repeater.range() == 16, "64 wraps back to 16");
        helper.succeed();
    }

    private static CompensationRepeaterBlockEntity place(GameTestHelper helper, BlockPos pos, Direction facing) {
        BlockState state = CompensationRepeaterBlock.BLOCK.get().defaultBlockState()
                .setValue(CompensationRepeaterBlock.FACING, facing);
        helper.getLevel().setBlockAndUpdate(pos, state);
        BlockEntity entity = helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(entity instanceof GtSignalSource, "compensation repeater is a source");
        helper.assertTrue(!(entity instanceof GtSignalReceiver), "compensation repeater is not a receiver");
        CompensationRepeaterBlockEntity repeater = repeater(helper, pos);
        helper.assertTrue(repeater.range() == 16, "a new repeater matches red alloy");
        return repeater;
    }

    private static CompensationRepeaterBlockEntity repeater(GameTestHelper helper, BlockPos pos) {
        BlockEntity entity = helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(entity instanceof CompensationRepeaterBlockEntity, "compensation repeater block entity");
        return (CompensationRepeaterBlockEntity) entity;
    }
}
