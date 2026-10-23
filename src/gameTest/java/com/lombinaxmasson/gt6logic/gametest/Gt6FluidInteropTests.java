package com.lombinaxmasson.gt6logic.gametest;

import com.lombinaxmasson.gt6logic.block.ActuatorBlock;
import com.lombinaxmasson.gt6logic.block.ControlValveBlock;
import com.lombinaxmasson.gt6logic.block.FaceSignals;
import com.lombinaxmasson.gt6logic.block.FlowBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Whether a proportional valve can feed GT6's own fluid transport.
 *
 * <p>Layout, with the valve facing south: a GT drum north, a GT fluid pipe south of the valve,
 * and a GT drum south of the pipe. The pipe's north and south ports are opened the way a
 * wrench would, because GT's face handler refuses {@code fill} on a closed port.</p>
 */
@GameTestHolder("gt6logic")
@PrefixGameTestTemplate(false)
public final class Gt6FluidInteropTests {
    private Gt6FluidInteropTests() {}

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void controlValveFeedsGtFluidPipeAndTank(GameTestHelper helper) {
        Level level = helper.getLevel();
        BlockPos valvePos = helper.absolutePos(new BlockPos(3, 1, 2));

        GameTestSupport.placeAimed(helper, ControlValveBlock.ITEM.get(), valvePos, Direction.SOUTH);
        helper.assertTrue(level.getBlockState(valvePos).getValue(ActuatorBlock.FACING) == Direction.SOUTH,
                "the valve faces the pipe");

        BlockPos inletPos = valvePos.north();
        GameTestSupport.placeGtBlock(helper, "drum_bronze", inletPos);
        BlockPos pipePos = valvePos.south();
        BlockPos tankPos = pipePos.south();

        Block pipe = GameTestSupport.gtBlock("pipe_medium_steel");
        BlockState pipeState = open(open(pipe.defaultBlockState(), Direction.NORTH), Direction.SOUTH);
        level.setBlockAndUpdate(pipePos, pipeState);
        GameTestSupport.placeGtBlock(helper, "drum_bronze", tankPos);
        helper.assertTrue(level.getBlockEntity(pipePos) != null, "GT fluid pipe block entity");
        helper.assertTrue(level.getBlockEntity(tankPos) != null, "GT drum block entity");

        IFluidHandler inlet = GameTestSupport.fluidHandler(level, inletPos, Direction.SOUTH);
        IFluidHandler outlet = GameTestSupport.fluidHandler(level, tankPos, Direction.NORTH);
        helper.assertTrue(inlet != null, "the source drum is a fluid handler");
        helper.assertTrue(outlet != null, "the sink drum is a fluid handler toward the pipe");
        inlet.fill(new FluidStack(Fluids.WATER, 8_000), IFluidHandler.FluidAction.EXECUTE);

        var probe = GameTestSupport.placeProbe(helper, valvePos.east());
        probe.setPending(Direction.EAST, FaceSignals.ON);
        probe.finishTick();

        helper.runAtTickTime(4, () -> {
            FlowBlockEntity valve = (FlowBlockEntity) level.getBlockEntity(valvePos);
            helper.assertTrue(valve.lastRate() == 250, "a full signal asks for 250 mB, saw " + valve.lastRate());
        });
        helper.runAtTickTime(20, () -> {
            int stored = stored(outlet);
            helper.assertTrue(stored > 0,
                    "water crossed the valve, the GT pipe and into the GT drum; saw " + stored);
            helper.succeed();
        });
    }

    /** Two identical runs in one tick window: the full signal must carry about twice the half signal. */
    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void signalScalesWhatGtTransportCarries(GameTestHelper helper) {
        IFluidHandler full = buildFlow(helper, new BlockPos(2, 1, 2), FaceSignals.ON);
        IFluidHandler half = buildFlow(helper, new BlockPos(6, 1, 2), FaceSignals.ON / 2);
        helper.runAtTickTime(20, () -> {
            int fullAmount = stored(full);
            int halfAmount = stored(half);
            helper.assertTrue(fullAmount > 0 && halfAmount > 0,
                    "both signals reached the GT drums: full " + fullAmount + ", half " + halfAmount);
            helper.assertTrue(fullAmount > halfAmount * 3 / 2,
                    "a full signal carries more than half: " + fullAmount + " vs " + halfAmount);
            helper.assertTrue(fullAmount < halfAmount * 5 / 2,
                    "and about twice as much: " + fullAmount + " vs " + halfAmount);
            helper.succeed();
        });
    }

    private static IFluidHandler buildFlow(GameTestHelper helper, BlockPos relativeValve, long signal) {
        Level level = helper.getLevel();
        BlockPos valvePos = helper.absolutePos(relativeValve);
        GameTestSupport.placeAimed(helper, ControlValveBlock.ITEM.get(), valvePos, Direction.SOUTH);

        BlockPos inletPos = valvePos.north();
        GameTestSupport.placeGtBlock(helper, "drum_bronze", inletPos);
        BlockPos pipePos = valvePos.south();
        BlockPos tankPos = pipePos.south();

        Block pipe = GameTestSupport.gtBlock("pipe_medium_steel");
        level.setBlockAndUpdate(pipePos, open(open(pipe.defaultBlockState(), Direction.NORTH), Direction.SOUTH));
        GameTestSupport.placeGtBlock(helper, "drum_bronze", tankPos);

        IFluidHandler inlet = GameTestSupport.fluidHandler(level, inletPos, Direction.SOUTH);
        IFluidHandler outlet = GameTestSupport.fluidHandler(level, tankPos, Direction.NORTH);
        helper.assertTrue(inlet != null, "the source drum is a fluid handler");
        helper.assertTrue(outlet != null, "the sink drum is a fluid handler toward the pipe");
        inlet.fill(new FluidStack(Fluids.WATER, 8_000), IFluidHandler.FluidAction.EXECUTE);

        var probe = GameTestSupport.placeProbe(helper, valvePos.east());
        probe.setPending(Direction.EAST, signal);
        probe.finishTick();
        return outlet;
    }

    /** Opens one port the way GT's wrench does. Connections are boolean properties named after the side. */
    private static BlockState open(BlockState state, Direction side) {
        for (Property<?> property : state.getProperties()) {
            if (property instanceof BooleanProperty flag && property.getName().equals(side.getSerializedName())) {
                return state.setValue(flag, true);
            }
        }
        throw new IllegalStateException("no " + side.getSerializedName() + " port on " + state);
    }

    private static int stored(IFluidHandler handler) {
        int amount = 0;
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            amount += handler.getFluidInTank(tank).getAmount();
        }
        return amount;
    }
}
