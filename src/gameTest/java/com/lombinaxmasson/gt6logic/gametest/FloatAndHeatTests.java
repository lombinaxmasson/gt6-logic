package com.lombinaxmasson.gt6logic.gametest;

import com.gregtech.gregtech.api.signal.GtSignalReceiver;
import com.gregtech.gregtech.api.signal.GtSignalSource;
import com.lombinaxmasson.gt6logic.block.ActuatorBlock;
import com.lombinaxmasson.gt6logic.block.AdjustableCoolerBlock;
import com.lombinaxmasson.gt6logic.block.AdjustableHeaterBlock;
import com.lombinaxmasson.gt6logic.block.BatteryStewardBlock;
import com.lombinaxmasson.gt6logic.block.BatteryStewardBlockEntity;
import com.lombinaxmasson.gt6logic.block.ElectricMeterBlock;
import com.lombinaxmasson.gt6logic.block.ElectricMeterBlockEntity;
import com.lombinaxmasson.gt6logic.block.FaceSignals;
import com.lombinaxmasson.gt6logic.block.FloatSwitchBlock;
import com.lombinaxmasson.gt6logic.block.FloatSwitchBlockEntity;
import com.lombinaxmasson.gt6logic.block.LogicPlateBlock;
import com.lombinaxmasson.gt6logic.block.TankBlock;
import com.lombinaxmasson.gt6logic.block.ThermalBlockEntity;
import com.lombinaxmasson.gt6logic.block.ThermostatBlock;
import com.lombinaxmasson.gt6logic.block.ThermostatBlockEntity;
import com.lombinaxmasson.gt6logic.control.HysteresisSwitch.Mode;
import com.lombinaxmasson.gt6logic.control.Temperatures;
import com.lombinaxmasson.gt6logic.control.TenThousandths;
import com.lombinaxmasson.gt6logic.energy.EuStore;
import com.lombinaxmasson.gt6logic.heat.TemperatureSource;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
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

/** Float, thermostat, meter, steward, heater, and cooler. */
@GameTestHolder("gt6logic")
@PrefixGameTestTemplate(false)
public final class FloatAndHeatTests {
    private FloatAndHeatTests() {}

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void floatFollowsTheTank(GameTestHelper helper) {
        BlockPos tankPos = helper.absolutePos(new BlockPos(2, 1, 2));
        BlockPos platePos = tankPos.east();
        helper.getLevel().setBlockAndUpdate(tankPos, TankBlock.BLOCK.get().defaultBlockState());
        BlockState plate = FloatSwitchBlock.BLOCK.get().defaultBlockState().setValue(LogicPlateBlock.FACING, Direction.WEST);
        helper.getLevel().setBlockAndUpdate(platePos, plate);
        IFluidHandler faced = FluidUtil.getFluidHandler(helper.getLevel(), tankPos, Direction.EAST).orElse(null);
        IFluidHandler above = FluidUtil.getFluidHandler(helper.getLevel(), tankPos, Direction.UP).orElse(null);
        IFluidHandler unsided = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, tankPos, null);
        helper.assertTrue(faced != null, "the face toward the switch is a fluid handler");
        helper.assertTrue(above != null, "the top exposes the same tank");
        helper.assertTrue(unsided != null, "a side-less query exposes the same tank");
        helper.assertTrue(FluidUtil.getFluidHandler(helper.getLevel(), platePos, Direction.EAST).isEmpty(),
                "the switch itself holds no fluid");
        faced.fill(new FluidStack(Fluids.WATER, 1_600), IFluidHandler.FluidAction.EXECUTE);
        helper.runAtTickTime(3, () -> helper.assertTrue(output(helper, platePos) == FaceSignals.ON, "below 20% opens"));
        helper.runAtTickTime(4, () -> faced.fill(new FluidStack(Fluids.WATER, 12_800), IFluidHandler.FluidAction.EXECUTE));
        helper.runAtTickTime(7, () -> helper.assertTrue(output(helper, platePos) == 0L, "above 80% closes"));
        helper.runAtTickTime(8, () -> {
            faced.drain(stored(faced) - 8_000, IFluidHandler.FluidAction.EXECUTE);
            helper.assertTrue(floatSwitch(helper, platePos).latched() == false, "the bit is still off");
        });
        helper.runAtTickTime(11, () -> {
            helper.assertTrue(output(helper, platePos) == 0L, "50% stays off");
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty", timeoutTicks = 20)
    public static void floatOpensWhenTheNeighborHasNoFluidHandler(GameTestHelper helper) {
        BlockPos stonePos = helper.absolutePos(new BlockPos(2, 1, 2));
        BlockPos platePos = stonePos.east();
        helper.getLevel().setBlockAndUpdate(stonePos, Blocks.STONE.defaultBlockState());
        BlockState plate = FloatSwitchBlock.BLOCK.get().defaultBlockState().setValue(LogicPlateBlock.FACING, Direction.WEST);
        helper.getLevel().setBlockAndUpdate(platePos, plate);
        helper.assertTrue(FluidUtil.getFluidHandler(helper.getLevel(), stonePos, Direction.EAST).isEmpty(),
                "stone registers no fluid handler");
        helper.runAtTickTime(3, () -> {
            helper.assertTrue(output(helper, platePos) == FaceSignals.ON, "an empty reading opens the fill band");
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty", timeoutTicks = 30)
    public static void thermostatUsesATemperatureSignal(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 5));
        BlockState state = ThermostatBlock.BLOCK.get().defaultBlockState().setValue(LogicPlateBlock.FACING, Direction.WEST);
        helper.getLevel().setBlockAndUpdate(pos, state);
        var probe = GameTestSupport.placeProbe(helper, pos.west());
        probe.setPending(Direction.WEST, Temperatures.toSignal(25_000L));
        probe.finishTick();
        helper.runAtTickTime(3, () -> helper.assertTrue(output(helper, pos) == FaceSignals.ON, "25 °C is below 30"));
        helper.runAtTickTime(4, () -> {
            probe.setPending(Direction.WEST, Temperatures.toSignal(36_000L));
            probe.finishTick();
        });
        helper.runAtTickTime(7, () -> helper.assertTrue(output(helper, pos) == 0L, "36 °C is above 35"));
        helper.runAtTickTime(8, () -> {
            probe.setPending(Direction.WEST, Temperatures.toSignal(32_000L));
            probe.finishTick();
        });
        helper.runAtTickTime(11, () -> {
            helper.assertTrue(output(helper, pos) == 0L, "32 °C stays off");
            helper.assertTrue(thermostat(helper, pos).band().mode() == Mode.HEAT, "the ferment preset heats");
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty", timeoutTicks = 20)
    public static void thermostatPrefersReportedMilliCelsius(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
        BlockState state = ThermostatBlock.BLOCK.get().defaultBlockState().setValue(LogicPlateBlock.FACING, Direction.WEST);
        helper.getLevel().setBlockAndUpdate(pos, state);
        var probe = GameTestSupport.placeProbe(helper, pos.west());
        probe.setPending(Direction.WEST, Temperatures.toSignal(25_000L));
        probe.reportMilliCelsius(36_000L);
        probe.finishTick();
        helper.runAtTickTime(3, () -> {
            helper.assertTrue(output(helper, pos) == 0L, "36 °C from the neighbor closes, ahead of the 25 °C signal");
            probe.clearMilliCelsius();
        });
        helper.runAtTickTime(7, () -> {
            helper.assertTrue(output(helper, pos) == FaceSignals.ON, "with no reported temperature the signal is used");
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty", timeoutTicks = 20)
    public static void thermostatOpensBesideATank(GameTestHelper helper) {
        BlockPos tankPos = helper.absolutePos(new BlockPos(2, 1, 2));
        BlockPos pos = tankPos.east();
        helper.getLevel().setBlockAndUpdate(tankPos, TankBlock.BLOCK.get().defaultBlockState());
        BlockState state = ThermostatBlock.BLOCK.get().defaultBlockState().setValue(LogicPlateBlock.FACING, Direction.WEST);
        helper.getLevel().setBlockAndUpdate(pos, state);
        helper.assertTrue(!(helper.getLevel().getBlockEntity(tankPos) instanceof TemperatureSource),
                "the tank does not report milli-celsius");
        helper.runAtTickTime(3, () -> {
            helper.assertTrue(output(helper, pos) == FaceSignals.ON, "0 °C opens the ferment band");
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty", timeoutTicks = 20)
    public static void meterPublishesItsCharge(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 8));
        helper.getLevel().setBlockAndUpdate(pos, ElectricMeterBlock.BLOCK.get().defaultBlockState());
        ElectricMeterBlockEntity meter = meter(helper, pos);
        long amps = meter.eu().capacity() / 2 / EuStore.LV;
        helper.assertTrue(meter.doEnergyInjection(EuStore.EU, Direction.UP, EuStore.LV, amps, false) == amps, "the meter takes the packet");
        BlockPos stewardPos = pos.east();
        BlockState stewardState = BatteryStewardBlock.BLOCK.get().defaultBlockState().setValue(LogicPlateBlock.FACING, Direction.WEST);
        helper.getLevel().setBlockAndUpdate(stewardPos, stewardState);
        helper.runAtTickTime(4, () -> {
            long signal = GameTestSupport.toward(meter, Direction.NORTH);
            int part = TenThousandths.of(meter.eu().stored(), meter.eu().capacity());
            helper.assertTrue(signal == TenThousandths.toSignal(part), "the signal matches the buffer");
            helper.assertTrue(steward(helper, stewardPos).latched() == false, "50% is inside 20 to 90");
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty", timeoutTicks = 20)
    public static void heaterAndCoolerSpendLv(GameTestHelper helper) {
        BlockPos heatPos = helper.absolutePos(new BlockPos(5, 1, 2));
        BlockPos coldPos = helper.absolutePos(new BlockPos(5, 1, 5));
        placeThermal(helper, heatPos, false);
        placeThermal(helper, coldPos, true);
        feed(helper, heatPos);
        feed(helper, coldPos);
        helper.runAtTickTime(3, () -> {
            ThermalBlockEntity heater = thermal(helper, heatPos);
            ThermalBlockEntity cooler = thermal(helper, coldPos);
            helper.assertTrue(heater.spent() == Power(), "the heater spent one LV packet");
            helper.assertTrue(heater.getEnergyOffered(EuStore.HEAT, Direction.UP, 1L) == Power(), "that packet is offered as heat");
            helper.assertTrue(cooler.getEnergyOffered(EuStore.COLD, Direction.UP, 1L) == Power(), "the cooler offers cold");
            helper.assertTrue(heater.doExtract(EuStore.HEAT, Direction.UP, 1L, Power(), false) == Power(), "a neighbor can take the heat");
            helper.succeed();
        });
    }

    private static long Power() {
        return EuStore.LV;
    }

    private static void feed(GameTestHelper helper, BlockPos pos) {
        var probe = GameTestSupport.placeProbe(helper, pos.west());
        probe.setPending(Direction.WEST, FaceSignals.ON);
        probe.finishTick();
        ThermalBlockEntity machine = thermal(helper, pos);
        machine.doEnergyInjection(EuStore.EU, Direction.UP, EuStore.LV, 8L, false);
    }

    private static void placeThermal(GameTestHelper helper, BlockPos pos, boolean cold) {
        BlockState state = (cold ? AdjustableCoolerBlock.BLOCK.get() : AdjustableHeaterBlock.BLOCK.get())
                .defaultBlockState()
                .setValue(ActuatorBlock.FACING, Direction.WEST);
        helper.getLevel().setBlockAndUpdate(pos, state);
        helper.assertTrue(helper.getLevel().getBlockEntity(pos) instanceof GtSignalReceiver,
                cold ? "cooler is a receiver" : "heater is a receiver");
    }

    private static long output(GameTestHelper helper, BlockPos pos) {
        return GameTestSupport.toward((GtSignalSource) helper.getLevel().getBlockEntity(pos), Direction.NORTH);
    }

    private static int stored(IFluidHandler handler) {
        int amount = 0;
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            amount += handler.getFluidInTank(tank).getAmount();
        }
        return amount;
    }

    private static FloatSwitchBlockEntity floatSwitch(GameTestHelper helper, BlockPos pos) {
        BlockEntity entity = helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(entity instanceof FloatSwitchBlockEntity, "float switch");
        return (FloatSwitchBlockEntity) entity;
    }

    private static ThermostatBlockEntity thermostat(GameTestHelper helper, BlockPos pos) {
        BlockEntity entity = helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(entity instanceof ThermostatBlockEntity, "thermostat");
        return (ThermostatBlockEntity) entity;
    }

    private static ElectricMeterBlockEntity meter(GameTestHelper helper, BlockPos pos) {
        BlockEntity entity = helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(entity instanceof ElectricMeterBlockEntity, "meter");
        return (ElectricMeterBlockEntity) entity;
    }

    private static BatteryStewardBlockEntity steward(GameTestHelper helper, BlockPos pos) {
        BlockEntity entity = helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(entity instanceof BatteryStewardBlockEntity, "steward");
        return (BatteryStewardBlockEntity) entity;
    }

    private static ThermalBlockEntity thermal(GameTestHelper helper, BlockPos pos) {
        BlockEntity entity = helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(entity instanceof ThermalBlockEntity, "heater or cooler");
        return (ThermalBlockEntity) entity;
    }
}
