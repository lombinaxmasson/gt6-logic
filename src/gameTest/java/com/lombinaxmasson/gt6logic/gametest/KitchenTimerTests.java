package com.lombinaxmasson.gt6logic.gametest;

import com.gregtech.gregtech.api.signal.GtSignalSource;
import com.lombinaxmasson.gt6logic.block.FaceSignals;
import com.lombinaxmasson.gt6logic.block.KitchenTimerBlock;
import com.lombinaxmasson.gt6logic.block.KitchenTimerBlockEntity;
import com.lombinaxmasson.gt6logic.block.LogicPlateBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** One second on, one second off. The first tick is on, and the phase is saved. */
@GameTestHolder("gt6logic")
@PrefixGameTestTemplate(false)
public final class KitchenTimerTests {
    private KitchenTimerTests() {}

    @GameTest(template = "test_empty", timeoutTicks = 50)
    public static void oneSecondOnThenOneSecondOff(GameTestHelper helper) {
        BlockPos relative = new BlockPos(2, 1, 2);
        BlockPos pos = helper.absolutePos(relative);
        place(helper, pos);
        helper.useBlock(relative);
        helper.useBlock(relative);
        helper.assertTrue(timer(helper, pos).onSeconds() == 1, "two clicks reach one second on");
        helper.assertTrue(timer(helper, pos).offSeconds() == 1, "and one second off");
        helper.runAtTickTime(30, () -> {
            int phase = timer(helper, pos).phase();
            helper.assertTrue(phase >= 20 && phase < 40, "the off second is underway, phase " + phase);
            helper.assertTrue(side(helper, pos) == 0L, "off during that second");
        });
        helper.runAtTickTime(50, () -> {
            helper.assertTrue(side(helper, pos) == FaceSignals.ON, "on again once the off second has passed");
            helper.succeed();
        });
    }

    private static long side(GameTestHelper helper, BlockPos pos) {
        return GameTestSupport.toward(timer(helper, pos), Direction.NORTH);
    }

    private static void place(GameTestHelper helper, BlockPos pos) {
        BlockState state = KitchenTimerBlock.BLOCK.get().defaultBlockState().setValue(LogicPlateBlock.FACING, Direction.WEST);
        helper.getLevel().setBlockAndUpdate(pos, state);
        helper.assertTrue(helper.getLevel().getBlockEntity(pos) instanceof GtSignalSource, "timer is a source");
        helper.assertTrue(timer(helper, pos).onSeconds() == 10, "a new timer is on for ten seconds");
    }

    private static KitchenTimerBlockEntity timer(GameTestHelper helper, BlockPos pos) {
        BlockEntity entity = helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(entity instanceof KitchenTimerBlockEntity, "timer block entity");
        return (KitchenTimerBlockEntity) entity;
    }
}
