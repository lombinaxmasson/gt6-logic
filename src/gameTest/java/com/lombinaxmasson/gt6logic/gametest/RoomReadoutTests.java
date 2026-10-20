package com.lombinaxmasson.gt6logic.gametest;

import com.gregtech.gregtech.api.signal.GtSignalCarrier;
import com.gregtech.gregtech.api.signal.GtSignalReceiver;
import com.gregtech.gregtech.api.signal.GtSignalSource;
import com.lombinaxmasson.gt6logic.block.ChartRecorderBlock;
import com.lombinaxmasson.gt6logic.block.ChartRecorderBlockEntity;
import com.lombinaxmasson.gt6logic.block.ConsolePanelBlock;
import com.lombinaxmasson.gt6logic.block.ConsolePanelBlockEntity;
import com.lombinaxmasson.gt6logic.block.DigitDisplayBlock;
import com.lombinaxmasson.gt6logic.block.DigitDisplayBlockEntity;
import com.lombinaxmasson.gt6logic.block.FaceSignals;
import com.lombinaxmasson.gt6logic.block.PointerGaugeBlock;
import com.lombinaxmasson.gt6logic.block.PointerGaugeBlockEntity;
import com.lombinaxmasson.gt6logic.block.ReceiverPlate;
import com.lombinaxmasson.gt6logic.block.WarningBeaconBlock;
import com.lombinaxmasson.gt6logic.control.DisplayReading.Unit;
import com.lombinaxmasson.gt6logic.control.PanelName;
import com.lombinaxmasson.gt6logic.control.PointerAngle;
import com.lombinaxmasson.gt6logic.gametest.SignalProbe.Entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Wall readouts listen, and a wire beside them stays quiet. */
@GameTestHolder("gt6logic")
@PrefixGameTestTemplate(false)
public final class RoomReadoutTests {
    private RoomReadoutTests() {}

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void digitCyclesUnitsAndKeepsTheFraction(GameTestHelper helper) {
        BlockPos relative = new BlockPos(2, 1, 2);
        BlockPos displayPos = helper.absolutePos(relative);
        placeDisplay(helper, displayPos, Direction.WEST);
        Entity probe = GameTestSupport.placeProbe(helper, displayPos.west());
        probe.setPending(Direction.WEST, 3L * GtSignalCarrier.UNIT);
        probe.finishTick();
        helper.runAtTickTime(4, () -> {
            DigitDisplayBlockEntity display = display(helper, displayPos);
            helper.assertTrue(display.reading() == 3L * GtSignalCarrier.UNIT, "the display keeps the signal");
            helper.assertTrue("3.0000".equals(display.face()), "signal face, saw " + display.face());
            helper.useBlock(relative);
            helper.assertTrue(display(helper, displayPos).unit() == Unit.TEMPERATURE, "right click selects temperature");
            helper.assertTrue("30.0".equals(display(helper, displayPos).face()), "30 °C, saw " + display(helper, displayPos).face());
            helper.useBlock(relative);
            helper.assertTrue("20%".equals(display(helper, displayPos).face()), "percent, saw " + display(helper, displayPos).face());
            helper.useBlock(relative);
            helper.assertTrue(display(helper, displayPos).unit() == Unit.SIGNAL, "the third click returns to signal");
            CompoundTag saved = display(helper, displayPos).saveWithoutMetadata(helper.getLevel().registryAccess());
            saved.putString("Unit", "nope");
            display(helper, displayPos).loadWithComponents(saved, helper.getLevel().registryAccess());
            helper.assertTrue(display(helper, displayPos).unit() == Unit.SIGNAL, "an unknown unit returns to signal");
            Player reader = helper.makeMockPlayer(GameType.SURVIVAL);
            reader.setItemInHand(InteractionHand.MAIN_HAND, GameTestSupport.magnifyingGlass());
            helper.assertTrue(!reader.getMainHandItem().isEmpty(), "gregtech magnifying glass is present");
            helper.useBlock(relative, reader);
            helper.assertTrue(display(helper, displayPos).unit() == Unit.SIGNAL, "the glass does not change the unit");
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void digitDoesNotFeedTheWire(GameTestHelper helper) {
        BlockPos stonePos = helper.absolutePos(new BlockPos(2, 1, 2));
        BlockPos torchPos = stonePos.above();
        BlockPos displayPos = torchPos.above();
        BlockPos wirePos = displayPos.east();
        helper.getLevel().setBlockAndUpdate(stonePos, Blocks.STONE.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(torchPos, Blocks.REDSTONE_TORCH.defaultBlockState());
        placeDisplay(helper, displayPos, Direction.DOWN);
        helper.getLevel().setBlockAndUpdate(wirePos,
                SignalProbe.connect(SignalProbe.redAlloyWire().defaultBlockState(), Direction.WEST));
        helper.runAtTickTime(4, () -> {
            helper.assertTrue(helper.getLevel().getSignal(displayPos, Direction.UP) == 15,
                    "the display conducts the torch under it");
            helper.assertTrue(display(helper, displayPos).getBlockState().getSignal(
                    helper.getLevel(), displayPos, Direction.UP) == 0, "the display does not override getSignal");
            helper.assertTrue(GameTestSupport.wire(helper, wirePos) == 0L, "the wire skips the display");
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void pointerFollowsTheSignal(GameTestHelper helper) {
        BlockPos gaugePos = helper.absolutePos(new BlockPos(2, 1, 2));
        placeGauge(helper, gaugePos, Direction.WEST);
        Entity probe = GameTestSupport.placeProbe(helper, gaugePos.west());
        probe.setPending(Direction.WEST, FaceSignals.ON);
        probe.finishTick();
        helper.runAtTickTime(4, () -> {
            PointerGaugeBlockEntity gauge = gauge(helper, gaugePos);
            helper.assertTrue(gauge.reading() == FaceSignals.ON, "the gauge stores a full signal");
            float expected = PointerAngle.radians(FaceSignals.ON);
            helper.assertTrue(Math.abs(gauge.angle() - expected) < 1.0e-4F, "the needle uses the gauge angle");
            helper.assertTrue(gauge.angle() < (float) (Math.PI * 2F), "a full signal is not a whole turn");
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty", timeoutTicks = 80)
    public static void chartRemembersLowAndHigh(GameTestHelper helper) {
        BlockPos recorderPos = helper.absolutePos(new BlockPos(2, 1, 2));
        placeRecorder(helper, recorderPos, Direction.WEST);
        Entity probe = GameTestSupport.placeProbe(helper, recorderPos.west());
        probe.setPending(Direction.WEST, FaceSignals.ON);
        probe.finishTick();
        helper.runAtTickTime(4, () -> {
            ChartRecorderBlockEntity recorder = recorder(helper, recorderPos);
            helper.assertTrue(recorder.bars() >= 1, "the first tick is a sample");
            helper.assertTrue(recorder.latest() == FaceSignals.ON, "the trace starts at full, saw " + recorder.latest());
            probe.setPending(Direction.WEST, 0L);
            probe.finishTick();
        });
        helper.runAtTickTime(50, () -> {
            ChartRecorderBlockEntity recorder = recorder(helper, recorderPos);
            helper.assertTrue(recorder.bars() >= 2, "a later second was stored, saw " + recorder.bars());
            helper.assertTrue(recorder.latest() == 0L, "the newest sample is zero");
            helper.assertTrue(recorder.lowest() == 0L, "zero is the low");
            helper.assertTrue(recorder.highest() == FaceSignals.ON, "full is still the high");
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void beaconBandsTheStrongestNeighbor(GameTestHelper helper) {
        BlockPos beaconPos = helper.absolutePos(new BlockPos(2, 1, 2));
        BlockPos neighborPos = beaconPos.west();
        placeBeacon(helper, beaconPos);
        helper.getLevel().setBlockAndUpdate(neighborPos, Blocks.REDSTONE_BLOCK.defaultBlockState());
        helper.runAtTickTime(4, () -> {
            assertBand(helper, beaconPos, WarningBeaconBlock.Color.RED, 15);
            helper.getLevel().setBlockAndUpdate(neighborPos, Blocks.AIR.defaultBlockState());
            Entity probe = GameTestSupport.placeProbe(helper, neighborPos);
            probe.setPending(Direction.WEST, GtSignalCarrier.UNIT / 2);
            probe.finishTick();
        });
        helper.runAtTickTime(8, () -> {
            assertBand(helper, beaconPos, WarningBeaconBlock.Color.GREEN, 8);
            Entity probe = (Entity) helper.getLevel().getBlockEntity(neighborPos);
            probe.setPending(Direction.WEST, 6L * GtSignalCarrier.UNIT);
            probe.finishTick();
        });
        helper.runAtTickTime(12, () -> {
            assertBand(helper, beaconPos, WarningBeaconBlock.Color.YELLOW, 12);
            Entity east = GameTestSupport.placeProbe(helper, beaconPos.east());
            east.setPending(Direction.EAST, 12L * GtSignalCarrier.UNIT);
            east.finishTick();
        });
        helper.runAtTickTime(16, () -> {
            assertBand(helper, beaconPos, WarningBeaconBlock.Color.RED, 15);
            Entity west = (Entity) helper.getLevel().getBlockEntity(neighborPos);
            west.setPending(Direction.WEST, 0L);
            west.finishTick();
            Entity east = (Entity) helper.getLevel().getBlockEntity(beaconPos.east());
            east.setPending(Direction.EAST, 0L);
            east.finishTick();
        });
        helper.runAtTickTime(20, () -> {
            assertBand(helper, beaconPos, WarningBeaconBlock.Color.OFF, 0);
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void beaconDoesNotFeedTheWire(GameTestHelper helper) {
        BlockPos stonePos = helper.absolutePos(new BlockPos(2, 1, 2));
        BlockPos torchPos = stonePos.above();
        BlockPos beaconPos = torchPos.above();
        BlockPos wirePos = beaconPos.east();
        helper.getLevel().setBlockAndUpdate(stonePos, Blocks.STONE.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(torchPos, Blocks.REDSTONE_TORCH.defaultBlockState());
        placeBeacon(helper, beaconPos);
        helper.getLevel().setBlockAndUpdate(wirePos,
                SignalProbe.connect(SignalProbe.redAlloyWire().defaultBlockState(), Direction.WEST));
        helper.runAtTickTime(4, () -> {
            BlockState state = helper.getLevel().getBlockState(beaconPos);
            helper.assertTrue(helper.getLevel().getSignal(beaconPos, Direction.UP) == 15,
                    "the beacon conducts the torch under it");
            helper.assertTrue(state.getSignal(helper.getLevel(), beaconPos, Direction.UP) == 0,
                    "the beacon does not override getSignal");
            helper.assertTrue(GameTestSupport.wire(helper, wirePos) == 0L, "the wire skips the beacon");
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void consoleNamesTheLamp(GameTestHelper helper) {
        BlockPos relative = new BlockPos(2, 1, 2);
        BlockPos panelPos = helper.absolutePos(relative);
        placePanel(helper, panelPos, Direction.WEST);
        Entity probe = GameTestSupport.placeProbe(helper, panelPos.west());
        probe.setPending(Direction.WEST, GtSignalCarrier.UNIT / 2);
        probe.finishTick();
        helper.runAtTickTime(4, () -> {
            ConsolePanelBlockEntity panel = panel(helper, panelPos);
            helper.assertTrue(panel.getBlockState().getValue(ConsolePanelBlock.POWERED), "half a level lights the panel");
            helper.assertTrue(panel.name() == PanelName.POWER, "a new panel says power");
            helper.useBlock(relative);
            helper.assertTrue(panel(helper, panelPos).name() == PanelName.HEAT, "right click selects heat");
            helper.assertTrue(panel(helper, panelPos).getBlockState().getValue(ConsolePanelBlock.POWERED),
                    "renaming leaves the lamp on");
            probe.setPending(Direction.WEST, 0L);
            probe.finishTick();
        });
        helper.runAtTickTime(8, () -> {
            helper.assertTrue(!panel(helper, panelPos).getBlockState().getValue(ConsolePanelBlock.POWERED),
                    "zero turns the lamp off");
            CompoundTag saved = panel(helper, panelPos).saveWithoutMetadata(helper.getLevel().registryAccess());
            saved.putString("Name", "nope");
            panel(helper, panelPos).loadWithComponents(saved, helper.getLevel().registryAccess());
            helper.assertTrue(panel(helper, panelPos).name() == PanelName.POWER, "an unknown name returns to power");
            helper.succeed();
        });
    }

    private static void placeDisplay(GameTestHelper helper, BlockPos pos, Direction facing) {
        BlockState state = DigitDisplayBlock.BLOCK.get().defaultBlockState().setValue(ReceiverPlate.FACING, facing);
        helper.getLevel().setBlockAndUpdate(pos, state);
        BlockEntity entity = helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(entity instanceof GtSignalReceiver, "digit display is a receiver");
        helper.assertTrue(!(entity instanceof GtSignalSource), "digit display is not a source");
    }

    private static DigitDisplayBlockEntity display(GameTestHelper helper, BlockPos pos) {
        BlockEntity entity = helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(entity instanceof DigitDisplayBlockEntity, "digit display block entity");
        return (DigitDisplayBlockEntity) entity;
    }

    private static void placeGauge(GameTestHelper helper, BlockPos pos, Direction facing) {
        helper.getLevel().setBlockAndUpdate(pos,
                PointerGaugeBlock.BLOCK.get().defaultBlockState().setValue(ReceiverPlate.FACING, facing));
        helper.assertTrue(helper.getLevel().getBlockEntity(pos) instanceof GtSignalReceiver, "pointer gauge is a receiver");
        helper.assertTrue(!(helper.getLevel().getBlockEntity(pos) instanceof GtSignalSource), "pointer gauge is not a source");
    }

    private static PointerGaugeBlockEntity gauge(GameTestHelper helper, BlockPos pos) {
        BlockEntity entity = helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(entity instanceof PointerGaugeBlockEntity, "pointer gauge block entity");
        return (PointerGaugeBlockEntity) entity;
    }

    private static void placeRecorder(GameTestHelper helper, BlockPos pos, Direction facing) {
        helper.getLevel().setBlockAndUpdate(pos,
                ChartRecorderBlock.BLOCK.get().defaultBlockState().setValue(ReceiverPlate.FACING, facing));
        helper.assertTrue(helper.getLevel().getBlockEntity(pos) instanceof GtSignalReceiver, "chart recorder is a receiver");
        helper.assertTrue(!(helper.getLevel().getBlockEntity(pos) instanceof GtSignalSource), "chart recorder is not a source");
    }

    private static ChartRecorderBlockEntity recorder(GameTestHelper helper, BlockPos pos) {
        BlockEntity entity = helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(entity instanceof ChartRecorderBlockEntity, "chart recorder block entity");
        return (ChartRecorderBlockEntity) entity;
    }

    private static void placeBeacon(GameTestHelper helper, BlockPos pos) {
        helper.getLevel().setBlockAndUpdate(pos, WarningBeaconBlock.BLOCK.get().defaultBlockState());
        BlockEntity entity = helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(entity instanceof GtSignalReceiver, "warning beacon is a receiver");
        helper.assertTrue(!(entity instanceof GtSignalSource), "warning beacon is not a source");
        helper.assertTrue(helper.getLevel().getBlockState(pos).getValue(WarningBeaconBlock.BAND) == WarningBeaconBlock.Color.OFF,
                "a new beacon is dark");
    }

    private static void assertBand(GameTestHelper helper, BlockPos pos, WarningBeaconBlock.Color color, int light) {
        BlockState state = helper.getLevel().getBlockState(pos);
        helper.assertTrue(state.getValue(WarningBeaconBlock.BAND) == color, "band " + color + ", saw " + state.getValue(WarningBeaconBlock.BAND));
        helper.assertTrue(state.getLightEmission() == light, "light " + light + ", saw " + state.getLightEmission());
    }

    private static void placePanel(GameTestHelper helper, BlockPos pos, Direction facing) {
        helper.getLevel().setBlockAndUpdate(pos,
                ConsolePanelBlock.BLOCK.get().defaultBlockState().setValue(ReceiverPlate.FACING, facing));
        helper.assertTrue(helper.getLevel().getBlockEntity(pos) instanceof GtSignalReceiver, "console panel is a receiver");
        helper.assertTrue(!(helper.getLevel().getBlockEntity(pos) instanceof GtSignalSource), "console panel is not a source");
    }

    private static ConsolePanelBlockEntity panel(GameTestHelper helper, BlockPos pos) {
        BlockEntity entity = helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(entity instanceof ConsolePanelBlockEntity, "console panel block entity");
        return (ConsolePanelBlockEntity) entity;
    }
}
