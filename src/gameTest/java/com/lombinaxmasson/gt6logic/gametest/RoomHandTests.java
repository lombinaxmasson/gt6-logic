package com.lombinaxmasson.gt6logic.gametest;

import com.gregtech.gregtech.api.signal.GtSignalReceiver;
import com.gregtech.gregtech.api.signal.GtSignalSource;
import com.lombinaxmasson.gt6logic.Gt6Logic;
import com.lombinaxmasson.gt6logic.block.BigLeverBlock;
import com.lombinaxmasson.gt6logic.block.ChartRecorderBlock;
import com.lombinaxmasson.gt6logic.block.ConsolePanelBlock;
import com.lombinaxmasson.gt6logic.block.DigitDisplayBlock;
import com.lombinaxmasson.gt6logic.block.EmergencyStopBlock;
import com.lombinaxmasson.gt6logic.block.EmergencyStopBlockEntity;
import com.lombinaxmasson.gt6logic.block.FaceSignals;
import com.lombinaxmasson.gt6logic.block.IndicatorBlock;
import com.lombinaxmasson.gt6logic.block.LabeledButtonBlock;
import com.lombinaxmasson.gt6logic.block.LabeledButtonBlockEntity;
import com.lombinaxmasson.gt6logic.block.LogicPlateBlock;
import com.lombinaxmasson.gt6logic.block.PointerGaugeBlock;
import com.lombinaxmasson.gt6logic.block.WarningBeaconBlock;
import com.lombinaxmasson.gt6logic.control.ButtonLabel;
import com.lombinaxmasson.gt6logic.signal.SignalBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Hand controls publish a full signal, one tick later, on every face. */
@GameTestHolder("gt6logic")
@PrefixGameTestTemplate(false)
public final class RoomHandTests {
    private RoomHandTests() {}

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void leverTogglesFullSignal(GameTestHelper helper) {
        BlockPos relative = new BlockPos(2, 1, 2);
        BlockPos leverPos = helper.absolutePos(relative);
        BlockPos wirePos = leverPos.east();
        SignalBlockEntity lever = placeLever(helper, leverPos, Direction.WEST);
        GameTestSupport.placeWire(helper, wirePos, Direction.WEST);
        helper.useBlock(relative);
        helper.runAtTickTime(4, () -> {
            SignalBlockEntity again = lever(helper, leverPos);
            helper.assertTrue(again.getBlockState().getValue(BigLeverBlock.POWERED), "the lever is on");
            helper.assertTrue(GameTestSupport.toward(again, Direction.EAST) == FaceSignals.ON, "the east face is full");
            helper.assertTrue(GameTestSupport.toward(again, Direction.WEST) == FaceSignals.ON, "the west face is full");
            helper.assertTrue(helper.getLevel().getSignal(leverPos, Direction.WEST) == 15, "vanilla redstone sees 15");
            long expected = FaceSignals.ON - GameTestSupport.RED_ALLOY_LOSS;
            helper.assertTrue(GameTestSupport.wire(helper, wirePos) == expected,
                    "the wire sees one hop less, saw " + GameTestSupport.wire(helper, wirePos));
            helper.useBlock(relative);
        });
        helper.runAtTickTime(8, () -> {
            SignalBlockEntity again = lever(helper, leverPos);
            helper.assertTrue(!again.getBlockState().getValue(BigLeverBlock.POWERED), "the second click is off");
            helper.assertTrue(GameTestSupport.toward(again, Direction.EAST) == 0L, "the output is zero");
            helper.assertTrue(GameTestSupport.wire(helper, wirePos) == 0L, "the wire goes quiet");
            helper.succeed();
        });
        helper.assertTrue(lever.getBlockState().is(BigLeverBlock.BLOCK.get()), "the lever stayed");
    }

    @GameTest(template = "test_empty", timeoutTicks = 80)
    public static void buttonPulsesForASecond(GameTestHelper helper) {
        BlockPos relative = new BlockPos(2, 1, 2);
        BlockPos buttonPos = helper.absolutePos(relative);
        BlockPos wirePos = buttonPos.east();
        placeButton(helper, buttonPos, Direction.WEST);
        GameTestSupport.placeWire(helper, wirePos, Direction.WEST);
        helper.useBlock(relative);
        helper.runAtTickTime(4, () -> {
            LabeledButtonBlockEntity button = button(helper, buttonPos);
            helper.assertTrue(button.getBlockState().getValue(LabeledButtonBlock.POWERED), "the button is lit");
            helper.assertTrue(GameTestSupport.toward(button, Direction.EAST) == FaceSignals.ON, "the pulse is full");
            long expected = FaceSignals.ON - GameTestSupport.RED_ALLOY_LOSS;
            helper.assertTrue(GameTestSupport.wire(helper, wirePos) == expected, "the wire sees the pulse");
        });
        helper.runAtTickTime(30, () -> {
            LabeledButtonBlockEntity button = button(helper, buttonPos);
            helper.assertTrue(!button.getBlockState().getValue(LabeledButtonBlock.POWERED), "the second is over");
            helper.assertTrue(GameTestSupport.toward(button, Direction.EAST) == 0L, "the pulse ended");
            helper.assertTrue(GameTestSupport.wire(helper, wirePos) == 0L, "the wire is quiet again");
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void sneakChangesTheLabelWithoutPulsing(GameTestHelper helper) {
        BlockPos relative = new BlockPos(2, 1, 2);
        BlockPos buttonPos = helper.absolutePos(relative);
        placeButton(helper, buttonPos, Direction.NORTH);
        Player sneaking = helper.makeMockPlayer(GameType.SURVIVAL);
        sneaking.setShiftKeyDown(true);
        ButtonLabel[] labels = {ButtonLabel.STOP, ButtonLabel.FEED, ButtonLabel.DRAIN, ButtonLabel.RUN};
        for (ButtonLabel label : labels) {
            helper.useBlock(relative, sneaking);
            helper.assertTrue(button(helper, buttonPos).label() == label, "label " + label);
        }
        CompoundTag saved = button(helper, buttonPos).saveWithoutMetadata(helper.getLevel().registryAccess());
        saved.putString("Label", "nope");
        saved.putInt("Left", 100);
        button(helper, buttonPos).loadWithComponents(saved, helper.getLevel().registryAccess());
        helper.assertTrue(button(helper, buttonPos).label() == ButtonLabel.RUN, "an unknown label returns to run");
        helper.assertTrue(button(helper, buttonPos).remaining() == 20, "a long pulse clamps to one second");
        saved.putInt("Left", 0);
        button(helper, buttonPos).loadWithComponents(saved, helper.getLevel().registryAccess());
        helper.assertTrue(button(helper, buttonPos).remaining() == 0, "the clamped pulse was cleared");
        helper.runAtTickTime(4, () -> {
            LabeledButtonBlockEntity button = button(helper, buttonPos);
            helper.assertTrue(!button.getBlockState().getValue(LabeledButtonBlock.POWERED), "sneak did not pulse");
            helper.assertTrue(GameTestSupport.toward(button, Direction.SOUTH) == 0L, "sneak did not publish");
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void emergencyStopLatchesUntilReset(GameTestHelper helper) {
        BlockPos relative = new BlockPos(2, 1, 2);
        BlockPos stopPos = helper.absolutePos(relative);
        BlockPos wirePos = stopPos.east();
        placeStop(helper, stopPos);
        GameTestSupport.placeWire(helper, wirePos, Direction.WEST);
        helper.useBlock(relative);
        helper.useBlock(relative);
        helper.runAtTickTime(4, () -> {
            EmergencyStopBlockEntity stop = stop(helper, stopPos);
            helper.assertTrue(stop.tripped(), "a second press stays tripped");
            helper.assertTrue(GameTestSupport.toward(stop, Direction.EAST) == FaceSignals.ON, "east is full");
            helper.assertTrue(GameTestSupport.toward(stop, Direction.UP) == FaceSignals.ON, "up is full");
            helper.assertTrue(helper.getLevel().getSignal(stopPos, Direction.WEST) == 15, "vanilla redstone sees 15");
            long expected = FaceSignals.ON - GameTestSupport.RED_ALLOY_LOSS;
            helper.assertTrue(GameTestSupport.wire(helper, wirePos) == expected, "the wire sees the stop");
            CompoundTag saved = stop.saveWithoutMetadata(helper.getLevel().registryAccess());
            Player sneaking = helper.makeMockPlayer(GameType.SURVIVAL);
            sneaking.setShiftKeyDown(true);
            helper.useBlock(relative, sneaking);
            helper.assertTrue(!stop(helper, stopPos).tripped(), "sneak releases it");
            stop(helper, stopPos).loadWithComponents(saved, helper.getLevel().registryAccess());
            helper.assertTrue(stop(helper, stopPos).tripped(), "a saved press comes back");
            helper.assertTrue(GameTestSupport.toward(stop(helper, stopPos), Direction.EAST) == FaceSignals.ON,
                    "the saved output comes back");
        });
        helper.runAtTickTime(8, () -> {
            helper.assertTrue(stop(helper, stopPos).getBlockState().getValue(EmergencyStopBlock.POWERED),
                    "the lamp follows the loaded latch");
            Player sneaking = helper.makeMockPlayer(GameType.SURVIVAL);
            sneaking.setShiftKeyDown(true);
            helper.useBlock(relative, sneaking);
        });
        helper.runAtTickTime(12, () -> {
            EmergencyStopBlockEntity stop = stop(helper, stopPos);
            helper.assertTrue(!stop.tripped(), "sneak releases the loaded latch");
            helper.assertTrue(GameTestSupport.toward(stop, Direction.EAST) == 0L, "the output drops");
            helper.assertTrue(GameTestSupport.wire(helper, wirePos) == 0L, "the wire goes quiet");
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty", timeoutTicks = 20)
    public static void creativeTabListsTheRoom(GameTestHelper helper) {
        CreativeModeTab tab = Gt6Logic.CREATIVE_TAB.get();
        tab.buildContents(new CreativeModeTab.ItemDisplayParameters(
                FeatureFlags.VANILLA_SET, true, helper.getLevel().registryAccess()));
        helper.assertTrue(tab.getIconItem().is(IndicatorBlock.ITEM.get()), "creative tab icon stays the indicator");
        assertListed(helper, tab, DigitDisplayBlock.BLOCK.get(), "block.gt6logic.digit_display", "Digit Display", "数码管");
        assertListed(helper, tab, PointerGaugeBlock.BLOCK.get(), "block.gt6logic.pointer_gauge", "Pointer Gauge", "指针表");
        assertListed(helper, tab, ChartRecorderBlock.BLOCK.get(), "block.gt6logic.chart_recorder", "Chart Recorder", "记录仪");
        assertListed(helper, tab, BigLeverBlock.BLOCK.get(), "block.gt6logic.big_lever", "Big Lever", "大号拉杆");
        assertListed(helper, tab, LabeledButtonBlock.BLOCK.get(), "block.gt6logic.labeled_button", "Labeled Button", "带标签的按钮");
        assertListed(helper, tab, WarningBeaconBlock.BLOCK.get(), "block.gt6logic.warning_beacon", "Warning Beacon", "警示灯塔");
        assertListed(helper, tab, ConsolePanelBlock.BLOCK.get(), "block.gt6logic.console_panel", "Console Panel", "控制台面板");
        assertListed(helper, tab, EmergencyStopBlock.BLOCK.get(), "block.gt6logic.emergency_stop", "Emergency Stop", "紧急停止");
        helper.succeed();
    }

    private static SignalBlockEntity placeLever(GameTestHelper helper, BlockPos pos, Direction facing) {
        BlockState state = BigLeverBlock.BLOCK.get().defaultBlockState().setValue(LogicPlateBlock.FACING, facing);
        helper.getLevel().setBlockAndUpdate(pos, state);
        BlockEntity entity = helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(entity instanceof GtSignalSource, "big lever is a source");
        helper.assertTrue(!(entity instanceof GtSignalReceiver), "big lever is not a receiver");
        helper.assertTrue(!entity.getBlockState().getValue(BigLeverBlock.POWERED), "a new lever is off");
        return lever(helper, pos);
    }

    private static SignalBlockEntity lever(GameTestHelper helper, BlockPos pos) {
        BlockEntity entity = helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(entity instanceof SignalBlockEntity, "big lever block entity");
        return (SignalBlockEntity) entity;
    }

    private static void placeButton(GameTestHelper helper, BlockPos pos, Direction facing) {
        helper.getLevel().setBlockAndUpdate(pos,
                LabeledButtonBlock.BLOCK.get().defaultBlockState().setValue(LogicPlateBlock.FACING, facing));
        BlockEntity entity = helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(entity instanceof GtSignalSource, "labeled button is a source");
        helper.assertTrue(!(entity instanceof GtSignalReceiver), "labeled button is not a receiver");
        helper.assertTrue(button(helper, pos).label() == ButtonLabel.RUN, "a new button says run");
    }

    private static LabeledButtonBlockEntity button(GameTestHelper helper, BlockPos pos) {
        BlockEntity entity = helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(entity instanceof LabeledButtonBlockEntity, "labeled button block entity");
        return (LabeledButtonBlockEntity) entity;
    }

    private static void placeStop(GameTestHelper helper, BlockPos pos) {
        helper.getLevel().setBlockAndUpdate(pos, EmergencyStopBlock.BLOCK.get().defaultBlockState());
        BlockEntity entity = helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(entity instanceof GtSignalSource, "emergency stop is a source");
        helper.assertTrue(!(entity instanceof GtSignalReceiver), "emergency stop is not a receiver");
        helper.assertTrue(!stop(helper, pos).tripped(), "a new stop is ready");
    }

    private static EmergencyStopBlockEntity stop(GameTestHelper helper, BlockPos pos) {
        BlockEntity entity = helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(entity instanceof EmergencyStopBlockEntity, "emergency stop block entity");
        return (EmergencyStopBlockEntity) entity;
    }

    private static void assertListed(
            GameTestHelper helper, CreativeModeTab tab, Block block, String key, String english, String chinese) {
        boolean listed = false;
        for (ItemStack stack : tab.getDisplayItems()) {
            if (stack.is(block.asItem())) {
                listed = true;
            }
        }
        helper.assertTrue(listed, key + " is in the creative tab");
        helper.assertTrue(english.equals(GameTestSupport.translation("en_us.json", key)), "english " + key);
        helper.assertTrue(chinese.equals(GameTestSupport.translation("zh_cn.json", key)), "chinese " + key);
    }
}
