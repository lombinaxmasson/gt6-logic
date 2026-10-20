package com.lombinaxmasson.gt6logic.gametest;

import com.gregtech.gregtech.api.signal.GtSignalSource;
import com.lombinaxmasson.gt6logic.block.ActuatorBlock;
import com.lombinaxmasson.gt6logic.block.ElectricValveBlock;
import com.lombinaxmasson.gt6logic.block.ElectricValveBlockEntity;
import com.lombinaxmasson.gt6logic.block.FaceSignals;
import com.lombinaxmasson.gt6logic.block.FloatSwitchBlock;
import com.lombinaxmasson.gt6logic.block.LogicPlateBlock;
import com.lombinaxmasson.gt6logic.block.TankBlock;
import com.lombinaxmasson.gt6logic.block.ThermostatBlock;
import com.lombinaxmasson.gt6logic.block.ThermostatBlockEntity;
import com.lombinaxmasson.gt6logic.control.HysteresisSwitch.Mode;
import com.lombinaxmasson.gt6logic.energy.EuStore;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Float switch on the north and thermostat on the south, into a ProjectRed AND gate facing east.
 * The gate's third input, the back, is switched off with the screwdriver so left and right are enough.
 * The wire in front of the gate feeds the valve from the west. Skipped without ProjectRed.
 */
@GameTestHolder("gt6logic")
@PrefixGameTestTemplate(false)
public final class FirstCircuitTests {
    /** Strictly under the float's 20% open point, and strictly over its 80% close point. The tank holds 16 buckets. */
    private static final int LOW_WATER = 1_000;
    private static final int HIGH_WATER = 16_000;
    /** Cool preset opens above 40 °C and closes below 35 °C. These sit outside that gap. */
    private static final long HOT = 50_000L;
    private static final long COLD = 30_000L;

    private FirstCircuitTests() {}

    @GameTest(template = "test_empty", timeoutTicks = 140)
    public static void bothSensorsOpenTheValve(GameTestHelper helper) {
        if (!ProjectRedInteropTests.present()) {
            helper.succeed();
            return;
        }
        BlockPos tankRel = new BlockPos(3, 1, 1);
        BlockPos floatRel = new BlockPos(3, 1, 2);
        BlockPos leftWireRel = new BlockPos(3, 1, 3);
        BlockPos gateRel = new BlockPos(3, 1, 4);
        BlockPos rightWireRel = new BlockPos(3, 1, 5);
        BlockPos thermostatRel = new BlockPos(3, 1, 6);
        BlockPos probeRel = new BlockPos(3, 1, 7);
        BlockPos frontWireRel = new BlockPos(4, 1, 4);
        BlockPos valveRel = new BlockPos(5, 1, 4);
        BlockPos outletRel = new BlockPos(5, 1, 3);
        BlockPos inletRel = new BlockPos(5, 1, 5);

        BlockPos tank = helper.absolutePos(tankRel);
        BlockPos plate = helper.absolutePos(floatRel);
        BlockPos gate = helper.absolutePos(gateRel);
        BlockPos thermostat = helper.absolutePos(thermostatRel);
        BlockPos probe = helper.absolutePos(probeRel);
        BlockPos leftWire = helper.absolutePos(leftWireRel);
        BlockPos rightWire = helper.absolutePos(rightWireRel);
        BlockPos frontWire = helper.absolutePos(frontWireRel);
        BlockPos valvePos = helper.absolutePos(valveRel);

        placeTank(helper, tank);
        BlockPos placedFloat = GameTestSupport.placeBlock(helper, FloatSwitchBlock.ITEM.get(), tank, Direction.SOUTH, Direction.EAST);
        helper.assertTrue(placedFloat.equals(plate), "float switch sits on the tank's south face");
        helper.assertTrue(helper.getLevel().getBlockState(plate).getValue(LogicPlateBlock.FACING) == Direction.NORTH,
                "the float faces the tank, so the signal leaves to the south");

        SignalProbe.Entity temperature = GameTestSupport.placeProbe(helper, probe);
        BlockPos placedThermostat = GameTestSupport.placeBlock(
                helper, ThermostatBlock.ITEM.get(), probe, Direction.NORTH, Direction.EAST);
        helper.assertTrue(placedThermostat.equals(thermostat), "thermostat sits on the probe's north face");
        helper.assertTrue(helper.getLevel().getBlockState(thermostat).getValue(LogicPlateBlock.FACING) == Direction.SOUTH,
                "the thermostat faces the probe, so the signal leaves to the north");
        helper.useBlock(thermostatRel);
        helper.useBlock(thermostatRel);
        helper.assertTrue(thermostat(helper, thermostat).band().mode() == Mode.COOL, "two clicks select the hot band");

        GameTestSupport.placeAimed(helper, ElectricValveBlock.ITEM.get(), valvePos, Direction.NORTH);
        helper.assertTrue(helper.getLevel().getBlockState(valvePos).getValue(ActuatorBlock.FACING) == Direction.NORTH,
                "the valve faces north, so its signal side is west");
        BlockPos inletPos = GameTestSupport.placeBlock(helper, TankBlock.ITEM.get(), valvePos, Direction.SOUTH, Direction.EAST);
        BlockPos outletPos = GameTestSupport.placeBlock(helper, TankBlock.ITEM.get(), valvePos, Direction.NORTH, Direction.EAST);
        helper.assertTrue(inletPos.equals(helper.absolutePos(inletRel)), "inlet is south of the valve");
        helper.assertTrue(outletPos.equals(helper.absolutePos(outletRel)), "outlet is north of the valve");

        helper.getLevel().setBlockAndUpdate(gate.below(), Blocks.STONE.defaultBlockState());
        ProjectRedInteropTests.placePart(helper, "projectred_integration:and_gate", gate.below(), Direction.UP, Direction.EAST);
        disableBackInput(helper, gate);
        placeWire(helper, leftWire);
        placeWire(helper, rightWire);
        placeWire(helper, frontWire);

        IFluidHandler measured = handler(helper, tank, Direction.SOUTH);
        IFluidHandler inlet = handler(helper, inletPos, Direction.NORTH);
        IFluidHandler outlet = handler(helper, outletPos, Direction.SOUTH);
        helper.assertTrue(measured.getTankCapacity(0) == HIGH_WATER, "the measured tank holds sixteen buckets");
        measured.fill(new FluidStack(Fluids.WATER, LOW_WATER), IFluidHandler.FluidAction.EXECUTE);
        inlet.fill(new FluidStack(Fluids.WATER, HIGH_WATER), IFluidHandler.FluidAction.EXECUTE);
        temperature.reportMilliCelsius(HOT);
        ElectricValveBlockEntity valve = valve(helper, valvePos);
        helper.assertTrue(valve.doEnergyInjection(EuStore.EU, Direction.UP, EuStore.LV, 8L, false) > 0L, "the valve takes power");

        helper.runAtTickTime(20, () -> {
            helper.assertTrue(away(helper, plate) == FaceSignals.ON, "low water opens the float");
            helper.assertTrue(away(helper, thermostat) == FaceSignals.ON, "50 °C opens the hot band");
            helper.assertTrue(helper.getLevel().getSignal(frontWire, Direction.WEST) > 0, "the gate's front wire is on");
            helper.assertTrue(stored(outlet) > 0, "low water and high temperature send water to the outlet, saw " + stored(outlet));
            temperature.reportMilliCelsius(COLD);
        });
        int[] cooled = {0};
        helper.runAtTickTime(40, () -> {
            helper.assertTrue(away(helper, thermostat) == 0L, "30 °C is under the hot band");
            helper.assertTrue(valve(helper, valvePos).lastRate() == 0, "the valve shut when the temperature fell");
            cooled[0] = stored(outlet);
        });
        helper.runAtTickTime(55, () -> {
            helper.assertTrue(stored(outlet) == cooled[0], "flow stays stopped while the temperature is low");
            temperature.reportMilliCelsius(HOT);
        });
        int[] flowing = {0};
        helper.runAtTickTime(75, () -> {
            helper.assertTrue(away(helper, thermostat) == FaceSignals.ON, "50 °C opens the hot band again");
            helper.assertTrue(stored(outlet) > cooled[0], "water moves again once the temperature recovers");
            flowing[0] = stored(outlet);
            measured.fill(new FluidStack(Fluids.WATER, HIGH_WATER), IFluidHandler.FluidAction.EXECUTE);
        });
        int[] full = {0};
        helper.runAtTickTime(95, () -> {
            helper.assertTrue(away(helper, plate) == 0L, "a full tank closes the float");
            helper.assertTrue(valve(helper, valvePos).lastRate() == 0, "the valve shut when the tank filled");
            full[0] = stored(outlet);
        });
        helper.runAtTickTime(110, () -> {
            helper.assertTrue(stored(outlet) == full[0], "flow stays stopped once the water is above the upper limit");
            helper.assertTrue(full[0] >= flowing[0], "filling the tank did not pull water back out");
            helper.succeed();
        });
    }

    /** Two crouching screwdriver clicks. The first disables the right input, the second disables the back. */
    private static void disableBackInput(GameTestHelper helper, BlockPos gate) {
        ResourceLocation id = ResourceLocation.parse("projectred_core:screwdriver");
        helper.assertTrue(BuiltInRegistries.ITEM.containsKey(id), "missing " + id);
        ItemStack screwdriver = new ItemStack(BuiltInRegistries.ITEM.get(id));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.moveTo(Vec3.atBottomCenterOf(gate.above()), Direction.EAST.toYRot(), 90F);
        player.setYHeadRot(Direction.EAST.toYRot());
        player.setPose(Pose.CROUCHING);
        player.setShiftKeyDown(true);
        player.setItemInHand(InteractionHand.MAIN_HAND, screwdriver);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(gate), Direction.UP, gate, false);
        for (int click = 0; click < 2; click++) {
            ItemInteractionResult result = helper.getLevel().getBlockState(gate).useItemOn(
                    screwdriver, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
            helper.assertTrue(result.consumesAction(), "screwdriver click " + (click + 1) + " returned " + result);
        }
    }

    private static void placeWire(GameTestHelper helper, BlockPos pos) {
        helper.getLevel().setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
        ProjectRedInteropTests.placePart(helper, "projectred_transmission:red_alloy_wire", pos.below(), Direction.UP, Direction.EAST);
    }

    private static void placeTank(GameTestHelper helper, BlockPos pos) {
        helper.getLevel().setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
        BlockPos placed = GameTestSupport.placeBlock(helper, TankBlock.ITEM.get(), pos.below(), Direction.UP, Direction.NORTH);
        helper.assertTrue(placed.equals(pos), "tank position");
    }

    /** What the neighbor standing on the free side of a plate reads. */
    private static long away(GameTestHelper helper, BlockPos pos) {
        Direction facing = helper.getLevel().getBlockState(pos).getValue(LogicPlateBlock.FACING);
        return GameTestSupport.toward((GtSignalSource) helper.getLevel().getBlockEntity(pos), facing.getOpposite());
    }

    private static IFluidHandler handler(GameTestHelper helper, BlockPos pos, Direction face) {
        IFluidHandler handler = FluidUtil.getFluidHandler(helper.getLevel(), pos, face).orElse(null);
        helper.assertTrue(handler != null, "fluid handler on " + face + " of " + pos);
        return handler;
    }

    private static int stored(IFluidHandler handler) {
        int amount = 0;
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            amount += handler.getFluidInTank(tank).getAmount();
        }
        return amount;
    }

    private static ThermostatBlockEntity thermostat(GameTestHelper helper, BlockPos pos) {
        BlockEntity entity = helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(entity instanceof ThermostatBlockEntity, "thermostat");
        return (ThermostatBlockEntity) entity;
    }

    private static ElectricValveBlockEntity valve(GameTestHelper helper, BlockPos pos) {
        BlockEntity entity = helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(entity instanceof ElectricValveBlockEntity, "electric valve");
        return (ElectricValveBlockEntity) entity;
    }
}
