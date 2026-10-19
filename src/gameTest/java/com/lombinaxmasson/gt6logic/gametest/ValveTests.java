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
import com.lombinaxmasson.gt6logic.block.TankBlockEntity;
import com.lombinaxmasson.gt6logic.energy.EuStore;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
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
        TankBlockEntity inlet = tank(helper, pos.west());
        TankBlockEntity outlet = tank(helper, pos.east());
        inlet.fluids().fill(new FluidStack(Fluids.WATER, 4_000), IFluidHandler.FluidAction.EXECUTE);
        var probe = GameTestSupport.placeProbe(helper, pos.north());
        probe.setPending(Direction.NORTH, FaceSignals.ON);
        probe.finishTick();
        helper.runAtTickTime(3, () -> helper.assertTrue(outlet.fluids().getFluidAmount() == 0, "no power keeps it shut"));
        helper.runAtTickTime(4, () -> valve.doEnergyInjection(EuStore.EU, Direction.UP, EuStore.LV, 8L, false));
        int[] moved = {0};
        helper.runAtTickTime(8, () -> {
            int amount = outlet.fluids().getFluidAmount();
            helper.assertTrue(amount > 0 && amount % 250 == 0, "powered ticks move 250 mB, saw " + amount);
            helper.assertTrue(inlet.fluids().getFluidAmount() + amount == 4_000, "the inlet lost the same amount");
            BlockEntity placed = helper.getLevel().getBlockEntity(pos);
            helper.assertTrue(!(placed instanceof GtSignalSource), "a valve is not a signal source");
            helper.assertTrue(placed instanceof GtSignalReceiver, "a valve is a receiver");
            probe.setPending(Direction.NORTH, 0L);
            probe.finishTick();
        });
        helper.runAtTickTime(12, () -> moved[0] = outlet.fluids().getFluidAmount());
        helper.runAtTickTime(16, () -> {
            helper.assertTrue(outlet.fluids().getFluidAmount() == moved[0], "a quiet signal stops the flow");
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty", timeoutTicks = 20)
    public static void controlValveFollowsHalfASignal(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(3, 1, 5));
        BlockState state = ControlValveBlock.BLOCK.get().defaultBlockState().setValue(ActuatorBlock.FACING, Direction.EAST);
        helper.getLevel().setBlockAndUpdate(pos, state);
        TankBlockEntity inlet = tank(helper, pos.west());
        TankBlockEntity outlet = tank(helper, pos.east());
        inlet.fluids().fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
        var probe = GameTestSupport.placeProbe(helper, pos.north());
        probe.setPending(Direction.NORTH, FaceSignals.ON / 2);
        probe.finishTick();
        helper.runAtTickTime(3, () -> {
            FlowBlockEntity valve = (FlowBlockEntity) helper.getLevel().getBlockEntity(pos);
            int amount = outlet.fluids().getFluidAmount();
            helper.assertTrue(valve.lastRate() == 124, "half of 250 mB floors to 124");
            helper.assertTrue(amount > 0 && amount % 124 == 0, "that much water moved, saw " + amount);
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
        TankBlockEntity inlet = tank(helper, pos.west());
        TankBlockEntity outlet = tank(helper, pos.east());
        inlet.fluids().fill(new FluidStack(Fluids.WATER, 8_000), IFluidHandler.FluidAction.EXECUTE);
        var probe = GameTestSupport.placeProbe(helper, pos.north());
        probe.setPending(Direction.NORTH, FaceSignals.ON);
        probe.finishTick();
        FlowBlockEntity pump = (FlowBlockEntity) helper.getLevel().getBlockEntity(pos);
        pump.doEnergyInjection(EuStore.EU, Direction.UP, EuStore.LV, 8L, false);
        helper.runAtTickTime(3, () -> {
            int amount = outlet.fluids().getFluidAmount();
            helper.assertTrue(pump.lastRate() == 1000, "full signal asks for 1000 mB");
            helper.assertTrue(amount > 0 && amount % 1000 == 0, "the pump moved buckets, saw " + amount);
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

    private static TankBlockEntity tank(GameTestHelper helper, BlockPos pos) {
        helper.getLevel().setBlockAndUpdate(pos, TankBlock.BLOCK.get().defaultBlockState());
        BlockEntity entity = helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(entity instanceof TankBlockEntity, "tank");
        return (TankBlockEntity) entity;
    }
}
