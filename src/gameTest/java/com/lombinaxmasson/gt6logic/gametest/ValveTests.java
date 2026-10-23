package com.lombinaxmasson.gt6logic.gametest;

import com.lombinaxmasson.gt6logic.block.ActuatorBlock;
import com.lombinaxmasson.gt6logic.block.AdjustablePumpBlock;
import com.lombinaxmasson.gt6logic.block.ControlValveBlock;
import com.lombinaxmasson.gt6logic.block.FaceSignals;
import com.lombinaxmasson.gt6logic.block.FlowBlockEntity;
import com.lombinaxmasson.gt6logic.energy.EuStore;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The valve pulls from the west tank and pushes into the east tank. The signal sits on the left, which is north. */
@GameTestHolder("gt6logic")
@PrefixGameTestTemplate(false)
public final class ValveTests {
    private ValveTests() {}

    @GameTest(template = "test_empty", timeoutTicks = 20)
    public static void controlValveFollowsHalfASignal(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(3, 1, 5));
        placeActuator(helper, ControlValveBlock.ITEM.get(), pos, Direction.EAST);
        Ends ends = tanks(helper, pos);
        ends.inlet.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
        var probe = GameTestSupport.placeProbe(helper, pos.north());
        probe.setPending(Direction.NORTH, FaceSignals.ON / 2);
        probe.finishTick();
        helper.runAtTickTime(3, () -> {
            FlowBlockEntity valve = (FlowBlockEntity) helper.getLevel().getBlockEntity(pos);
            int amount = stored(ends.outlet);
            helper.assertTrue(valve.lastRate() == 124, "half of 250 mB floors to 124");
            helper.assertTrue(amount > 0 && amount % 124 == 0, "that much water moved, saw " + amount);
            helper.assertTrue(stored(ends.inlet) + amount == 1000, "the inlet lost the same amount");
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty", timeoutTicks = 20)
    public static void pumpMovesABucketPerTick(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(3, 1, 8));
        placeActuator(helper, AdjustablePumpBlock.ITEM.get(), pos, Direction.EAST);
        Ends ends = tanks(helper, pos);
        ends.inlet.fill(new FluidStack(Fluids.WATER, 8_000), IFluidHandler.FluidAction.EXECUTE);
        var probe = GameTestSupport.placeProbe(helper, pos.north());
        probe.setPending(Direction.NORTH, FaceSignals.ON);
        probe.finishTick();
        FlowBlockEntity pump = (FlowBlockEntity) helper.getLevel().getBlockEntity(pos);
        helper.runAtTickTime(3, () -> helper.assertTrue(stored(ends.outlet) == 0, "no power keeps the pump stopped"));
        helper.runAtTickTime(4, () -> pump.doEnergyInjection(EuStore.EU, Direction.UP, EuStore.LV, 8L, false));
        helper.runAtTickTime(8, () -> {
            int amount = stored(ends.outlet);
            helper.assertTrue(pump.lastRate() == 1000, "full signal asks for 1000 mB");
            helper.assertTrue(amount > 0 && amount % 1000 == 0, "the pump moved buckets, saw " + amount);
            helper.assertTrue(stored(ends.inlet) + amount == 8_000, "the inlet lost the same amount");
            helper.succeed();
        });
    }

    private static void placeActuator(GameTestHelper helper, Item item, BlockPos pos, Direction facing) {
        GameTestSupport.placeAimed(helper, item, pos, facing);
        helper.assertTrue(helper.getLevel().getBlockState(pos).getValue(ActuatorBlock.FACING) == facing,
                item + " faces " + facing + " but is " + helper.getLevel().getBlockState(pos));
    }

    /** Inlet on the west, outlet on the east. Both are GT drums, the real transport the valve feeds. */
    private static Ends tanks(GameTestHelper helper, BlockPos machine) {
        Level level = helper.getLevel();
        BlockPos inletPos = machine.west();
        BlockPos outletPos = machine.east();
        GameTestSupport.placeGtBlock(helper, "drum_bronze", inletPos);
        GameTestSupport.placeGtBlock(helper, "drum_bronze", outletPos);
        IFluidHandler inlet = GameTestSupport.fluidHandler(level, inletPos, Direction.EAST);
        IFluidHandler outlet = GameTestSupport.fluidHandler(level, outletPos, Direction.WEST);
        helper.assertTrue(inlet != null, "the inlet face toward the actuator is a fluid handler");
        helper.assertTrue(outlet != null, "the outlet face toward the actuator is a fluid handler");
        for (Direction face : Direction.values()) {
            helper.assertTrue(GameTestSupport.fluidHandler(level, machine, face) == null, "the actuator has no fluid handler");
        }
        helper.assertTrue(level.getCapability(Capabilities.FluidHandler.BLOCK, machine, null) == null,
                "the actuator has no side-less fluid handler");
        return new Ends(inlet, outlet);
    }

    private static int stored(IFluidHandler handler) {
        int amount = 0;
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            amount += handler.getFluidInTank(tank).getAmount();
        }
        return amount;
    }

    private record Ends(IFluidHandler inlet, IFluidHandler outlet) {}
}
