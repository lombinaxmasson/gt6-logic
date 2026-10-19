package com.lombinaxmasson.gt6logic.gametest;

import com.gregtech.gregtech.api.signal.GtSignalCarrier;
import com.gregtech.gregtech.api.signal.GtSignalReceiver;
import com.gregtech.gregtech.api.signal.GtSignalSource;
import com.lombinaxmasson.gt6logic.block.SelectorSwitchBlock;
import com.lombinaxmasson.gt6logic.block.SelectorSwitchBlockEntity;
import com.lombinaxmasson.gt6logic.gametest.SignalProbe.Entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The selected face is copied, fraction included. The other faces repeat it. */
@GameTestHolder("gt6logic")
@PrefixGameTestTemplate(false)
public final class SelectorSwitchTests {
    private SelectorSwitchTests() {}

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void passesTheSelectedFaceUnchanged(GameTestHelper helper) {
        BlockPos relative = new BlockPos(2, 1, 2);
        BlockPos pos = helper.absolutePos(relative);
        SelectorSwitchBlockEntity selector = place(helper, pos, Direction.WEST);
        helper.assertTrue(selector.input() == Direction.WEST, "a new switch listens to the face it was placed against");
        Entity west = GameTestSupport.placeProbe(helper, pos.west());
        Entity east = GameTestSupport.placeProbe(helper, pos.east());
        west.setPending(Direction.WEST, GtSignalCarrier.UNIT / 2);
        east.setPending(Direction.EAST, 3L * GtSignalCarrier.UNIT);
        west.finishTick();
        east.finishTick();
        helper.runAtTickTime(4, () -> {
            SelectorSwitchBlockEntity current = selector(helper, pos);
            helper.assertTrue(GameTestSupport.toward(current, Direction.NORTH) == GtSignalCarrier.UNIT / 2,
                    "the half level is copied, saw " + GameTestSupport.toward(current, Direction.NORTH));
            helper.assertTrue(GameTestSupport.toward(current, Direction.WEST) == 0L, "the selected face stays quiet");
            helper.assertTrue(current.getBlockState().getValue(SelectorSwitchBlock.POWERED), "a present signal lights the face");
            helper.useBlock(relative);
        });
        helper.runAtTickTime(8, () -> {
            SelectorSwitchBlockEntity current = selector(helper, pos);
            helper.assertTrue(current.input() == Direction.EAST, "one click selects east, the next direction after west");
            helper.assertTrue(GameTestSupport.toward(current, Direction.NORTH) == 3L * GtSignalCarrier.UNIT,
                    "the east face is now the one copied");
            helper.assertTrue(GameTestSupport.toward(current, Direction.EAST) == 0L, "the new input face stays quiet");
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty", timeoutTicks = 20)
    public static void unknownDirectionReturnsToFacing(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 5));
        SelectorSwitchBlockEntity selector = place(helper, pos, Direction.NORTH);
        selector.cycleInput();
        CompoundTag saved = selector.saveWithoutMetadata(helper.getLevel().registryAccess());
        selector.loadWithComponents(saved, helper.getLevel().registryAccess());
        helper.assertTrue(selector.input() == Direction.SOUTH, "the selection reloads, saw " + selector.input());
        saved.putString("Input", "nope");
        selector.loadWithComponents(saved, helper.getLevel().registryAccess());
        helper.assertTrue(selector.input() == Direction.NORTH, "an unknown face returns to the front");
        helper.succeed();
    }

    private static SelectorSwitchBlockEntity place(GameTestHelper helper, BlockPos pos, Direction facing) {
        BlockState state = SelectorSwitchBlock.BLOCK.get().defaultBlockState().setValue(SelectorSwitchBlock.FACING, facing);
        helper.getLevel().setBlockAndUpdate(pos, state);
        BlockEntity entity = helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(entity instanceof GtSignalSource, "selector switch is a source");
        helper.assertTrue(!(entity instanceof GtSignalReceiver), "selector switch is not a receiver");
        return selector(helper, pos);
    }

    private static SelectorSwitchBlockEntity selector(GameTestHelper helper, BlockPos pos) {
        BlockEntity entity = helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(entity instanceof SelectorSwitchBlockEntity, "selector switch block entity");
        return (SelectorSwitchBlockEntity) entity;
    }
}
