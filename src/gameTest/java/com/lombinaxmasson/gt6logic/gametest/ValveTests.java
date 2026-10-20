package com.lombinaxmasson.gt6logic.gametest;

import com.gregtech.gregtech.api.signal.GtSignalReceiver;
import com.gregtech.gregtech.api.signal.GtSignalSource;
import com.lombinaxmasson.gt6logic.block.ActuatorBlock;
import com.lombinaxmasson.gt6logic.block.AdjustablePumpBlock;
import com.lombinaxmasson.gt6logic.block.ControlValveBlock;
import com.lombinaxmasson.gt6logic.block.ElectricValveBlock;
import com.lombinaxmasson.gt6logic.block.ElectricValveBlockEntity;
import com.lombinaxmasson.gt6logic.block.FaceSignals;
import com.lombinaxmasson.gt6logic.block.FlowBlockEntity;
import com.lombinaxmasson.gt6logic.block.TankBlock;
import com.lombinaxmasson.gt6logic.energy.EuStore;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The valve pulls from the west tank and pushes into the east tank. The signal sits on the north side. */
@GameTestHolder("gt6logic")
@PrefixGameTestTemplate(false)
public final class ValveTests {
    private ValveTests() {}

    @GameTest(template = "test_empty", timeoutTicks = 30)
    public static void electricValveNeedsSignalAndPower(GameTestHelper helper) {
        BlockPos relative = new BlockPos(3, 1, 2);
        BlockPos pos = helper.absolutePos(relative);
        ElectricValveBlockEntity valve = placeValve(helper, pos);
        Ends ends = tanks(helper, pos);
        ends.inlet.fill(new FluidStack(Fluids.WATER, 4_000), IFluidHandler.FluidAction.EXECUTE);
        var probe = GameTestSupport.placeProbe(helper, pos.north());
        probe.setPending(Direction.NORTH, FaceSignals.ON);
        probe.finishTick();
        helper.runAtTickTime(3, () -> helper.assertTrue(stored(ends.outlet) == 0, "no power keeps it shut"));
        helper.runAtTickTime(4, () -> valve.doEnergyInjection(EuStore.EU, Direction.UP, EuStore.LV, 8L, false));
        int[] moved = {0};
        helper.runAtTickTime(8, () -> {
            int amount = stored(ends.outlet);
            helper.assertTrue(amount > 0 && amount % 250 == 0, "powered ticks move 250 mB, saw " + amount);
            helper.assertTrue(stored(ends.inlet) + amount == 4_000, "the inlet lost the same amount");
            BlockEntity placed = helper.getLevel().getBlockEntity(pos);
            helper.assertTrue(!(placed instanceof GtSignalSource), "a valve is not a signal source");
            helper.assertTrue(placed instanceof GtSignalReceiver, "a valve is a receiver");
            probe.setPending(Direction.NORTH, 0L);
            probe.finishTick();
        });
        helper.runAtTickTime(12, () -> moved[0] = stored(ends.outlet));
        helper.runAtTickTime(16, () -> {
            helper.assertTrue(stored(ends.outlet) == moved[0], "a quiet signal stops the flow");
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty", timeoutTicks = 20)
    public static void controlValveFollowsHalfASignal(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(3, 1, 5));
        BlockState state = ControlValveBlock.BLOCK.get().defaultBlockState().setValue(ActuatorBlock.FACING, Direction.EAST);
        helper.getLevel().setBlockAndUpdate(pos, state);
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
        BlockState state = AdjustablePumpBlock.BLOCK.get()
                .defaultBlockState()
                .setValue(ActuatorBlock.FACING, Direction.EAST);
        helper.getLevel().setBlockAndUpdate(pos, state);
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

    /** A ProjectRed NOT gate north of the valve, front toward it. Skipped without ProjectRed. */
    @GameTest(template = "test_empty", timeoutTicks = 60, required = false)
    public static void projectRedGateDrivesElectricValve(GameTestHelper helper) {
        if (!ProjectRedInteropTests.present()) {
            helper.succeed();
            return;
        }
        BlockPos pos = helper.absolutePos(new BlockPos(3, 1, 11));
        ElectricValveBlockEntity valve = placeValve(helper, pos);
        Ends ends = tanks(helper, pos);
        ends.inlet.fill(new FluidStack(Fluids.WATER, 8_000), IFluidHandler.FluidAction.EXECUTE);
        BlockPos gate = pos.north();
        helper.getLevel().setBlockAndUpdate(gate.below(), Blocks.STONE.defaultBlockState());
        ProjectRedInteropTests.placePart(helper, "projectred_integration:not_gate", gate.below(), Direction.UP, Direction.SOUTH);
        helper.runAtTickTime(5, () -> valve.doEnergyInjection(EuStore.EU, Direction.UP, EuStore.LV, 8L, false));
        helper.runAtTickTime(10, () -> {
            helper.assertTrue(stored(ends.outlet) > 0, "the gate's output opens the valve");
            helper.getLevel().setBlockAndUpdate(gate.north(), Blocks.REDSTONE_BLOCK.defaultBlockState());
        });
        int[] moved = {0};
        helper.runAtTickTime(16, () -> {
            moved[0] = stored(ends.outlet);
            valve.doEnergyInjection(EuStore.EU, Direction.UP, EuStore.LV, 8L, false);
        });
        helper.runAtTickTime(22, () -> {
            helper.assertTrue(stored(ends.outlet) == moved[0], "the gate turned off and the valve shut");
            helper.succeed();
        });
    }

    private static ElectricValveBlockEntity placeValve(GameTestHelper helper, BlockPos pos) {
        BlockState state = ElectricValveBlock.BLOCK.get().defaultBlockState().setValue(ActuatorBlock.FACING, Direction.EAST);
        helper.getLevel().setBlockAndUpdate(pos, state);
        BlockEntity entity = helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(entity instanceof ElectricValveBlockEntity, "electric valve");
        return (ElectricValveBlockEntity) entity;
    }

    /** Inlet on the west, outlet on the east. Both are tanks. The actuator keeps none of the fluid. */
    private static Ends tanks(GameTestHelper helper, BlockPos machine) {
        Level level = helper.getLevel();
        BlockPos inletPos = machine.west();
        BlockPos outletPos = machine.east();
        level.setBlockAndUpdate(inletPos, TankBlock.BLOCK.get().defaultBlockState());
        level.setBlockAndUpdate(outletPos, TankBlock.BLOCK.get().defaultBlockState());
        helper.assertTrue(level.getBlockState(inletPos).is(TankBlock.BLOCK.get()), "the inlet is a tank");
        helper.assertTrue(level.getBlockState(outletPos).is(TankBlock.BLOCK.get()), "the outlet is a tank");
        IFluidHandler inlet = FluidUtil.getFluidHandler(level, inletPos, Direction.EAST).orElse(null);
        IFluidHandler outlet = FluidUtil.getFluidHandler(level, outletPos, Direction.WEST).orElse(null);
        helper.assertTrue(inlet != null, "the inlet face toward the actuator is a fluid handler");
        helper.assertTrue(outlet != null, "the outlet face toward the actuator is a fluid handler");
        for (Direction face : Direction.values()) {
            helper.assertTrue(FluidUtil.getFluidHandler(level, machine, face).isEmpty(), "the actuator has no fluid handler");
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
