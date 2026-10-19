package com.lombinaxmasson.gt6logic.gametest;

import com.gregtech.gregtech.api.signal.GtSignalReceiver;
import com.gregtech.gregtech.api.signal.GtSignalSource;
import com.lombinaxmasson.gt6logic.block.FaceSignals;
import com.lombinaxmasson.gt6logic.block.InterlockBlock;
import com.lombinaxmasson.gt6logic.block.InterlockBlockEntity;
import com.lombinaxmasson.gt6logic.control.Interlock.Mode;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Front and back are the two inputs. NOT ignores the back. */
@GameTestHolder("gt6logic")
@PrefixGameTestTemplate(false)
public final class InterlockTests {
    private InterlockTests() {}

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void andOrAndNot(GameTestHelper helper) {
        BlockPos relative = new BlockPos(2, 1, 2);
        BlockPos pos = helper.absolutePos(relative);
        place(helper, pos, Direction.WEST);
        var front = GameTestSupport.placeProbe(helper, pos.west());
        var back = GameTestSupport.placeProbe(helper, pos.east());
        front.setPending(Direction.WEST, FaceSignals.ON);
        front.finishTick();
        helper.runAtTickTime(4, () -> helper.assertTrue(side(helper, pos) == 0L, "and stays off with one input"));
        helper.runAtTickTime(5, () -> {
            back.setPending(Direction.EAST, FaceSignals.ON);
            back.finishTick();
        });
        helper.runAtTickTime(9, () -> helper.assertTrue(side(helper, pos) == FaceSignals.ON, "and opens when both are on"));
        helper.runAtTickTime(10, () -> {
            helper.useBlock(relative);
            back.setPending(Direction.EAST, 0);
            back.finishTick();
        });
        helper.runAtTickTime(14, () -> {
            helper.assertTrue(interlock(helper, pos).mode() == Mode.OR, "right click selects or");
            helper.assertTrue(side(helper, pos) == FaceSignals.ON, "or stays on from the front alone");
            helper.useBlock(relative);
        });
        helper.runAtTickTime(18, () -> {
            helper.assertTrue(interlock(helper, pos).mode() == Mode.NOT, "the next click selects invert");
            helper.assertTrue(side(helper, pos) == 0L, "invert closes while the front is on");
            helper.assertTrue(GameTestSupport.toward(interlock(helper, pos), Direction.WEST) == 0L, "the front face stays quiet");
            helper.assertTrue(GameTestSupport.toward(interlock(helper, pos), Direction.EAST) == 0L, "the back face stays quiet");
            front.setPending(Direction.WEST, 0);
            front.finishTick();
        });
        helper.runAtTickTime(22, () -> {
            helper.assertTrue(side(helper, pos) == FaceSignals.ON, "invert opens when the front is quiet");
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty", timeoutTicks = 20)
    public static void unknownModeReturnsToAnd(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 5));
        InterlockBlockEntity interlock = place(helper, pos, Direction.NORTH);
        interlock.cycleMode();
        CompoundTag saved = interlock.saveWithoutMetadata(helper.getLevel().registryAccess());
        interlock.cycleMode();
        interlock.loadWithComponents(saved, helper.getLevel().registryAccess());
        helper.assertTrue(interlock.mode() == Mode.OR, "the mode reloads");
        saved.putString("Mode", "nope");
        interlock.loadWithComponents(saved, helper.getLevel().registryAccess());
        helper.assertTrue(interlock.mode() == Mode.AND, "an unknown mode returns to and");
        helper.succeed();
    }

    private static long side(GameTestHelper helper, BlockPos pos) {
        return GameTestSupport.toward(interlock(helper, pos), Direction.NORTH);
    }

    private static InterlockBlockEntity place(GameTestHelper helper, BlockPos pos, Direction facing) {
        BlockState state = InterlockBlock.BLOCK.get().defaultBlockState().setValue(InterlockBlock.FACING, facing);
        helper.getLevel().setBlockAndUpdate(pos, state);
        BlockEntity entity = helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(entity instanceof GtSignalSource, "interlock is a source");
        helper.assertTrue(!(entity instanceof GtSignalReceiver), "interlock is not a receiver");
        InterlockBlockEntity interlock = interlock(helper, pos);
        helper.assertTrue(interlock.mode() == Mode.AND, "a new interlock requires both");
        return interlock;
    }

    private static InterlockBlockEntity interlock(GameTestHelper helper, BlockPos pos) {
        BlockEntity entity = helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(entity instanceof InterlockBlockEntity, "interlock block entity");
        return (InterlockBlockEntity) entity;
    }
}
