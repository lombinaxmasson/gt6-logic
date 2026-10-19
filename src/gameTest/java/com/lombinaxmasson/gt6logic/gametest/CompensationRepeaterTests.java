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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
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
        place(helper, repeaterPos, Direction.WEST);
        helper.runAtTickTime(6, () -> {
            CompensationRepeaterBlockEntity repeater = repeater(helper, repeaterPos);
            helper.assertTrue(GameTestSupport.toward(repeater, Direction.EAST) == 15L * GtSignalCarrier.UNIT,
                    "the full level is back, saw " + GameTestSupport.toward(repeater, Direction.EAST));
            helper.assertTrue(GameTestSupport.toward(repeater, Direction.WEST) == 0L, "the input face stays quiet");
            helper.assertTrue(repeater.getBlockState().getValue(CompensationRepeaterBlock.POWERED), "the face lights");
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void zeroStaysZeroAndRangeSixtyFourAddsASmallerHop(GameTestHelper helper) {
        BlockPos relative = new BlockPos(2, 1, 5);
        BlockPos repeaterPos = helper.absolutePos(relative);
        place(helper, repeaterPos, Direction.WEST);
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
