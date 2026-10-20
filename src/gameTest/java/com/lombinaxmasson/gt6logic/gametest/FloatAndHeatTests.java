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
import com.lombinaxmasson.gt6logic.signal.SignalLevels;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Float, thermostat, meter, steward, heater, and cooler. Blocks under test are placed with their items. */
@GameTestHolder("gt6logic")
@PrefixGameTestTemplate(false)
public final class FloatAndHeatTests {
    private FloatAndHeatTests() {}

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void floatFollowsTheTank(GameTestHelper helper) {
        BlockPos tankPos = helper.absolutePos(new BlockPos(2, 1, 2));
        placeTank(helper, tankPos);
        BlockPos platePos = placeOnFace(helper, FloatSwitchBlock.ITEM.get(), tankPos, Direction.EAST);
        IFluidHandler faced = FluidUtil.getFluidHandler(helper.getLevel(), tankPos, Direction.EAST).orElse(null);
        IFluidHandler above = FluidUtil.getFluidHandler(helper.getLevel(), tankPos, Direction.UP).orElse(null);
        IFluidHandler unsided = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, tankPos, null);
        helper.assertTrue(faced != null, "the face toward the switch is a fluid handler");
        helper.assertTrue(above != null, "the top exposes the same tank");
        helper.assertTrue(unsided != null, "a side-less query exposes the same tank");
        helper.assertTrue(FluidUtil.getFluidHandler(helper.getLevel(), platePos, Direction.EAST).isEmpty(),
                "the switch itself holds no fluid");
        // Stone around the dust so the world's own blocks cannot pull a line. The plate stays the only open neighbor.
        helper.getLevel().setBlockAndUpdate(platePos.below(), Blocks.STONE.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(platePos.east().north(), Blocks.STONE.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(platePos.east().east(), Blocks.STONE.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(platePos.east().south(), Blocks.STONE.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(platePos.south().west(), Blocks.STONE.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(platePos.south().south(), Blocks.STONE.defaultBlockState());
        BlockPos away = placeDust(helper, platePos.east());
        BlockPos side = placeDust(helper, platePos.south());
        faced.fill(new FluidStack(Fluids.WATER, 1_600), IFluidHandler.FluidAction.EXECUTE);
        helper.runAtTickTime(3, () -> {
            helper.assertTrue(output(helper, platePos) == FaceSignals.ON, "below 20% opens");
            assertOneFace(helper, platePos, FaceSignals.ON);
            BlockState out = helper.getLevel().getBlockState(away);
            BlockState quiet = helper.getLevel().getBlockState(side);
            helper.assertTrue(out.getValue(RedStoneWireBlock.WEST) != RedstoneSide.NONE, "dust connects on the output face, saw " + out);
            helper.assertTrue(out.getValue(RedStoneWireBlock.NORTH) == RedstoneSide.NONE, "output dust is a line toward the switch, saw " + out);
            helper.assertTrue(out.getValue(RedStoneWireBlock.POWER) == 15, "output dust is on, saw " + out);
            helper.assertTrue(quiet.getValue(RedStoneWireBlock.EAST) != RedstoneSide.NONE, "side dust is not a line into the switch, saw " + quiet);
            helper.assertTrue(quiet.getValue(RedStoneWireBlock.POWER) == 0, "side dust stays dark, saw " + quiet);
        });
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
        helper.getLevel().setBlockAndUpdate(stonePos, Blocks.STONE.defaultBlockState());
        BlockPos platePos = placeOnFace(helper, FloatSwitchBlock.ITEM.get(), stonePos, Direction.EAST);
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
        placePlateFacing(helper, ThermostatBlock.ITEM.get(), pos, Direction.WEST);
        var probe = probeInstead(helper, pos, Direction.WEST);
        probe.setPending(Direction.WEST, Temperatures.toSignal(25_000L));
        probe.finishTick();
        helper.runAtTickTime(3, () -> {
            helper.assertTrue(output(helper, pos) == FaceSignals.ON, "25 °C is below 30");
            assertOneFace(helper, pos, FaceSignals.ON);
        });
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
        placePlateFacing(helper, ThermostatBlock.ITEM.get(), pos, Direction.WEST);
        var probe = probeInstead(helper, pos, Direction.WEST);
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
        placeTank(helper, tankPos);
        BlockPos pos = placeOnFace(helper, ThermostatBlock.ITEM.get(), tankPos, Direction.EAST);
        helper.assertTrue(!(helper.getLevel().getBlockEntity(tankPos) instanceof TemperatureSource),
                "the tank does not report milli-celsius");
        helper.runAtTickTime(3, () -> {
            helper.assertTrue(output(helper, pos) == FaceSignals.ON, "0 °C opens the ferment band");
            assertOneFace(helper, pos, FaceSignals.ON);
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty", timeoutTicks = 20)
    public static void meterPublishesItsCharge(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 8));
        GameTestSupport.placeAimed(helper, ElectricMeterBlock.ITEM.get(), pos, Direction.WEST);
        helper.assertTrue(helper.getLevel().getBlockState(pos).getValue(ElectricMeterBlock.FACING) == Direction.WEST,
                "the meter faces west, so the signal leaves to the east");
        ElectricMeterBlockEntity meter = meter(helper, pos);
        long amps = meter.eu().capacity() / 2 / EuStore.LV;
        helper.assertTrue(meter.doEnergyInjection(EuStore.EU, Direction.UP, EuStore.LV, amps, false) == amps, "the meter takes the packet");
        BlockPos stewardPos = placeOnFace(helper, BatteryStewardBlock.ITEM.get(), pos, Direction.EAST);
        helper.runAtTickTime(4, () -> {
            long signal = GameTestSupport.toward(meter, Direction.EAST);
            int part = TenThousandths.of(meter.eu().stored(), meter.eu().capacity());
            helper.assertTrue(signal == TenThousandths.toSignal(part), "the signal matches the buffer");
            assertOneFace(helper, pos, signal);
            assertOneFace(helper, stewardPos, 0L);
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
        Item item = cold ? AdjustableCoolerBlock.ITEM.get() : AdjustableHeaterBlock.ITEM.get();
        GameTestSupport.placeAimed(helper, item, pos, Direction.WEST);
        helper.assertTrue(helper.getLevel().getBlockState(pos).getValue(ActuatorBlock.FACING) == Direction.WEST,
                "faces the probe");
        helper.assertTrue(helper.getLevel().getBlockEntity(pos) instanceof GtSignalReceiver,
                cold ? "cooler is a receiver" : "heater is a receiver");
    }

    /** Stone under {@code pos}, then the tank item on that stone. */
    private static void placeTank(GameTestHelper helper, BlockPos pos) {
        helper.getLevel().setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
        BlockPos placed = GameTestSupport.placeBlock(helper, TankBlock.ITEM.get(), pos.below(), Direction.UP, Direction.NORTH);
        helper.assertTrue(placed.equals(pos), "tank position");
    }

    /** Clicks {@code face} of a block that is already there. The plate faces the opposite way. */
    private static BlockPos placeOnFace(GameTestHelper helper, Item item, BlockPos support, Direction face) {
        BlockPos placed = GameTestSupport.placeBlock(helper, item, support, face, Direction.NORTH);
        Direction facing = face.getOpposite();
        helper.assertTrue(helper.getLevel().getBlockState(placed).getValue(LogicPlateBlock.FACING) == facing,
                item + " faces " + facing + " but is " + helper.getLevel().getBlockState(placed));
        return placed;
    }

    /** Stone on the faced side, then the plate. The stone can be swapped for a probe afterwards. */
    private static void placePlateFacing(GameTestHelper helper, Item item, BlockPos pos, Direction facing) {
        BlockPos support = pos.relative(facing);
        helper.getLevel().setBlockAndUpdate(support, Blocks.STONE.defaultBlockState());
        BlockPos placed = placeOnFace(helper, item, support, facing.getOpposite());
        helper.assertTrue(placed.equals(pos), "plate position");
    }

    private static SignalProbe.Entity probeInstead(GameTestHelper helper, BlockPos plate, Direction facing) {
        BlockPos support = plate.relative(facing);
        helper.getLevel().setBlockAndUpdate(support, Blocks.AIR.defaultBlockState());
        return GameTestSupport.placeProbe(helper, support);
    }

    private static BlockPos placeDust(GameTestHelper helper, BlockPos pos) {
        helper.getLevel().setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
        return GameTestSupport.placeBlock(helper, Items.REDSTONE, pos.below(), Direction.UP, Direction.NORTH);
    }

    /** The away face carries {@code expected}. The other five stay 0. Wires attach only on the facing axis. */
    private static void assertOneFace(GameTestHelper helper, BlockPos pos, long expected) {
        BlockState state = helper.getLevel().getBlockState(pos);
        Direction facing = state.getValue(BlockStateProperties.FACING);
        BlockEntity entity = helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(entity instanceof GtSignalSource, "signal source");
        GtSignalSource source = (GtSignalSource) entity;
        int vanilla = SignalLevels.ceilingLevel(expected);
        for (Direction side : Direction.values()) {
            boolean away = side == facing.getOpposite();
            long read = GameTestSupport.toward(source, side);
            helper.assertTrue(read == (away ? expected : 0L), side + " signal " + read);
            Direction query = side.getOpposite();
            int weak = state.getSignal(helper.getLevel(), pos, query);
            int direct = state.getDirectSignal(helper.getLevel(), pos, query);
            int want = away ? vanilla : 0;
            helper.assertTrue(weak == want && direct == want, side + " vanilla " + weak + "/" + direct);
            boolean connect = state.getBlock().canConnectRedstone(state, helper.getLevel(), pos, side);
            helper.assertTrue(connect == (side.getAxis() == facing.getAxis()), side + " connect " + connect);
        }
        helper.assertTrue(!state.getBlock().canConnectRedstone(state, helper.getLevel(), pos, null),
                "a wire above does not connect");
    }

    private static long output(GameTestHelper helper, BlockPos pos) {
        BlockState state = helper.getLevel().getBlockState(pos);
        Direction facing = state.getValue(LogicPlateBlock.FACING);
        return GameTestSupport.toward((GtSignalSource) helper.getLevel().getBlockEntity(pos), facing.getOpposite());
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
