package com.lombinaxmasson.gt6logic.gametest;

import com.gregtech.gregtech.api.signal.GtSignalCarrier;
import com.gregtech.gregtech.api.signal.GtSignalSource;
import com.lombinaxmasson.gt6logic.block.LevelGaugeBlock;
import com.lombinaxmasson.gt6logic.block.LogicPlateBlock;
import com.lombinaxmasson.gt6logic.block.TankBlock;
import com.lombinaxmasson.gt6logic.block.TankBlockEntity;
import com.lombinaxmasson.gt6logic.control.TenThousandths;
import com.lombinaxmasson.gt6logic.signal.SignalLevels;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Empty, half, and full. The free face is the only one that carries the fill. */
@GameTestHolder("gt6logic")
@PrefixGameTestTemplate(false)
public final class LevelGaugeTests {
    private LevelGaugeTests() {}

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void fillScalesAcrossTheFreeFace(GameTestHelper helper) {
        BlockPos tankPos = helper.absolutePos(new BlockPos(2, 1, 2));
        helper.getLevel().setBlockAndUpdate(tankPos.below(), Blocks.STONE.defaultBlockState());
        BlockPos tank = GameTestSupport.placeBlock(helper, TankBlock.ITEM.get(), tankPos.below(), Direction.UP, Direction.NORTH);
        BlockPos gauge = GameTestSupport.placeBlock(helper, LevelGaugeBlock.ITEM.get(), tank, Direction.EAST, Direction.EAST);
        helper.assertTrue(helper.getLevel().getBlockState(gauge).getValue(LogicPlateBlock.FACING) == Direction.WEST,
                "the gauge faces the tank");
        TankBlockEntity fluids = tank(helper, tank);
        helper.runAtTickTime(4, () -> {
            assertFace(helper, gauge, 0L);
            fluids.fluids().fill(new FluidStack(Fluids.WATER, TankBlockEntity.CAPACITY / 2), IFluidHandler.FluidAction.EXECUTE);
        });
        long half = TenThousandths.toSignal(TenThousandths.FULL / 2);
        helper.runAtTickTime(8, () -> {
            long read = GameTestSupport.toward(source(helper, gauge), Direction.EAST);
            helper.assertTrue(read == half, "half tank, saw " + read);
            helper.assertTrue(Math.abs(read - (15L * GtSignalCarrier.UNIT) / 2) <= 1L, "half is within one of 7.5");
            assertFace(helper, gauge, half);
            fluids.fluids().fill(new FluidStack(Fluids.WATER, TankBlockEntity.CAPACITY / 2), IFluidHandler.FluidAction.EXECUTE);
        });
        helper.runAtTickTime(12, () -> {
            assertFace(helper, gauge, 15L * GtSignalCarrier.UNIT);
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty", timeoutTicks = 20)
    public static void stoneReadsAsEmpty(GameTestHelper helper) {
        BlockPos stone = helper.absolutePos(new BlockPos(2, 1, 2));
        helper.getLevel().setBlockAndUpdate(stone, Blocks.STONE.defaultBlockState());
        BlockPos gauge = GameTestSupport.placeBlock(helper, LevelGaugeBlock.ITEM.get(), stone, Direction.EAST, Direction.NORTH);
        helper.runAtTickTime(4, () -> {
            assertFace(helper, gauge, 0L);
            helper.succeed();
        });
    }

    private static void assertFace(GameTestHelper helper, BlockPos pos, long expected) {
        BlockState state = helper.getLevel().getBlockState(pos);
        Direction facing = state.getValue(LogicPlateBlock.FACING);
        GtSignalSource source = source(helper, pos);
        int vanilla = SignalLevels.ceilingLevel(expected);
        for (Direction side : Direction.values()) {
            boolean free = side == facing.getOpposite();
            long read = GameTestSupport.toward(source, side);
            helper.assertTrue(read == (free ? expected : 0L), side + " signal " + read);
            int weak = state.getSignal(helper.getLevel(), pos, side.getOpposite());
            helper.assertTrue(weak == (free ? vanilla : 0), side + " vanilla " + weak);
        }
    }

    private static GtSignalSource source(GameTestHelper helper, BlockPos pos) {
        BlockEntity entity = helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(entity instanceof GtSignalSource, "level gauge");
        return (GtSignalSource) entity;
    }

    private static TankBlockEntity tank(GameTestHelper helper, BlockPos pos) {
        BlockEntity entity = helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(entity instanceof TankBlockEntity, "tank");
        return (TankBlockEntity) entity;
    }
}
