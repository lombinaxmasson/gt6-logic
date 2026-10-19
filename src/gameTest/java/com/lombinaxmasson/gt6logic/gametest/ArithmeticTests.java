package com.lombinaxmasson.gt6logic.gametest;

import com.gregtech.gregtech.api.signal.GtSignalCarrier;
import com.gregtech.gregtech.api.signal.GtSignalReceiver;
import com.gregtech.gregtech.api.signal.GtSignalSource;
import com.lombinaxmasson.gt6logic.block.ArithmeticBlock;
import com.lombinaxmasson.gt6logic.block.ArithmeticBlockEntity;
import com.lombinaxmasson.gt6logic.block.FaceSignals;
import com.lombinaxmasson.gt6logic.block.LogicPlateBlock;
import com.lombinaxmasson.gt6logic.control.Arithmetic.Op;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Front plus back. The two input faces stay quiet. */
@GameTestHolder("gt6logic")
@PrefixGameTestTemplate(false)
public final class ArithmeticTests {
    private ArithmeticTests() {}

    @GameTest(template = "test_empty", timeoutTicks = 30)
    public static void addsTheFrontToTheBack(GameTestHelper helper) {
        BlockPos relative = new BlockPos(2, 1, 2);
        BlockPos pos = helper.absolutePos(relative);
        place(helper, pos, Direction.WEST);
        var front = GameTestSupport.placeProbe(helper, pos.west());
        var back = GameTestSupport.placeProbe(helper, pos.east());
        front.setPending(Direction.WEST, GtSignalCarrier.UNIT);
        back.setPending(Direction.EAST, 2L * GtSignalCarrier.UNIT);
        front.finishTick();
        back.finishTick();
        helper.runAtTickTime(4, () -> {
            helper.assertTrue(side(helper, pos) == 3L * GtSignalCarrier.UNIT, "one plus two is three");
            helper.assertTrue(GameTestSupport.toward(plate(helper, pos), Direction.WEST) == 0L, "the front stays quiet");
            helper.assertTrue(GameTestSupport.toward(plate(helper, pos), Direction.EAST) == 0L, "the back stays quiet");
            helper.useBlock(relative);
            helper.useBlock(relative);
        });
        helper.runAtTickTime(8, () -> {
            helper.assertTrue(plate(helper, pos).op() == Op.HALF, "two clicks reach half");
            helper.assertTrue(side(helper, pos) == GtSignalCarrier.UNIT / 2, "half of the front");
            CompoundTag saved = plate(helper, pos).saveWithoutMetadata(helper.getLevel().registryAccess());
            saved.putString("Op", "nope");
            plate(helper, pos).loadWithComponents(saved, helper.getLevel().registryAccess());
            helper.assertTrue(plate(helper, pos).op() == Op.ADD, "an unknown operation returns to add");
            helper.succeed();
        });
    }

    private static long side(GameTestHelper helper, BlockPos pos) {
        return GameTestSupport.toward(plate(helper, pos), Direction.NORTH);
    }

    private static void place(GameTestHelper helper, BlockPos pos, Direction facing) {
        BlockState state = ArithmeticBlock.BLOCK.get().defaultBlockState().setValue(LogicPlateBlock.FACING, facing);
        helper.getLevel().setBlockAndUpdate(pos, state);
        helper.assertTrue(helper.getLevel().getBlockEntity(pos) instanceof GtSignalSource, "arithmetic is a source");
        helper.assertTrue(!(helper.getLevel().getBlockEntity(pos) instanceof GtSignalReceiver), "arithmetic is not a receiver");
    }

    private static ArithmeticBlockEntity plate(GameTestHelper helper, BlockPos pos) {
        BlockEntity entity = helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(entity instanceof ArithmeticBlockEntity, "arithmetic block entity");
        return (ArithmeticBlockEntity) entity;
    }
}
