package com.lombinaxmasson.gt6logic.gametest;

import com.gregtech.gregtech.api.signal.GtSignalReceiver;
import com.gregtech.gregtech.api.signal.GtSignalSource;
import com.lombinaxmasson.gt6logic.block.DelayRelayBlock;
import com.lombinaxmasson.gt6logic.block.DelayRelayBlockEntity;
import com.lombinaxmasson.gt6logic.block.FaceSignals;
import com.lombinaxmasson.gt6logic.control.DelayRelay.Mode;
import com.lombinaxmasson.gt6logic.gametest.SignalProbe.Entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** On-delay waits out a second. Off-delay holds for a second. The input face stays quiet. */
@GameTestHolder("gt6logic")
@PrefixGameTestTemplate(false)
public final class DelayRelayTests {
    private DelayRelayTests() {}

    @GameTest(template = "test_empty", timeoutTicks = 80)
    public static void onDelayWaitsThenOpensWithoutFeedingTheInput(GameTestHelper helper) {
        BlockPos relayPos = helper.absolutePos(new BlockPos(2, 1, 2));
        BlockPos probePos = relayPos.west();
        BlockPos wirePos = relayPos.east();
        place(helper, relayPos, Direction.WEST);
        Entity probe = GameTestSupport.placeProbe(helper, probePos);
        probe.setPending(Direction.WEST, FaceSignals.ON);
        probe.finishTick();
        GameTestSupport.placeWire(helper, wirePos, Direction.WEST);
        helper.runAtTickTime(8, () -> {
            helper.assertTrue(GameTestSupport.toward(relay(helper, relayPos), Direction.EAST) == 0L,
                    "still waiting, saw " + GameTestSupport.toward(relay(helper, relayPos), Direction.EAST));
            helper.assertTrue(GameTestSupport.wire(helper, wirePos) == 0L, "the wire is quiet during the wait");
        });
        helper.runAtTickTime(45, () -> {
            DelayRelayBlockEntity relay = relay(helper, relayPos);
            helper.assertTrue(GameTestSupport.toward(relay, Direction.EAST) == FaceSignals.ON,
                    "the far side is on after a second");
            helper.assertTrue(GameTestSupport.toward(relay, Direction.WEST) == 0L, "the input face stays quiet");
            helper.assertTrue(relay.getBlockState().getValue(DelayRelayBlock.POWERED), "the face lights");
            long expected = FaceSignals.ON - GameTestSupport.RED_ALLOY_LOSS;
            helper.assertTrue(GameTestSupport.wire(helper, wirePos) == expected,
                    "the wire sees one hop less, saw " + GameTestSupport.wire(helper, wirePos));
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty", timeoutTicks = 80)
    public static void offDelayHoldsAfterTheInputLeaves(GameTestHelper helper) {
        BlockPos relative = new BlockPos(2, 1, 2);
        BlockPos relayPos = helper.absolutePos(relative);
        BlockPos probePos = relayPos.west();
        DelayRelayBlockEntity relay = place(helper, relayPos, Direction.WEST);
        Player sneaking = helper.makeMockPlayer(GameType.SURVIVAL);
        sneaking.setShiftKeyDown(true);
        helper.useBlock(relative, sneaking);
        helper.assertTrue(relay.mode() == Mode.OFF_DELAY, "sneak selects hold");
        Entity probe = GameTestSupport.placeProbe(helper, probePos);
        probe.setPending(Direction.WEST, FaceSignals.ON);
        probe.finishTick();
        helper.runAtTickTime(4, () -> {
            helper.assertTrue(GameTestSupport.toward(relay(helper, relayPos), Direction.EAST)
                    == FaceSignals.ON, "hold mode opens immediately");
            probe.setPending(Direction.WEST, 0);
            probe.finishTick();
        });
        helper.runAtTickTime(10, () -> helper.assertTrue(
                GameTestSupport.toward(relay(helper, relayPos), Direction.EAST)
                        == FaceSignals.ON,
                "it is still holding"));
        helper.runAtTickTime(50, () -> {
            helper.assertTrue(GameTestSupport.toward(relay(helper, relayPos), Direction.EAST) == 0L,
                    "the hold ends");
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void settingsReloadAndAClickResetsTheWait(GameTestHelper helper) {
        BlockPos relative = new BlockPos(2, 1, 2);
        BlockPos relayPos = helper.absolutePos(relative);
        place(helper, relayPos, Direction.WEST);
        Entity probe = GameTestSupport.placeProbe(helper, relayPos.west());
        probe.setPending(Direction.WEST, FaceSignals.ON);
        probe.finishTick();
        helper.runAtTickTime(6, () -> {
            DelayRelayBlockEntity relay = relay(helper, relayPos);
            helper.assertTrue(relay.waited() > 0 && relay.waited() < 20,
                    "the wait has started, saw " + relay.waited());
            CompoundTag saved = relay.saveWithoutMetadata(helper.getLevel().registryAccess());
            saved.putInt("Waited", 100);
            relay.loadWithComponents(saved, helper.getLevel().registryAccess());
            helper.assertTrue(relay.waited() == 20, "a long wait clamps to the delay, saw " + relay.waited());
            helper.useBlock(relative);
            helper.assertTrue(relay(helper, relayPos).seconds() == 2, "right click selects 2 seconds");
            helper.assertTrue(relay(helper, relayPos).waited() == 0, "changing the time clears the wait");
            Player sneaking = helper.makeMockPlayer(GameType.SURVIVAL);
            sneaking.setShiftKeyDown(true);
            helper.useBlock(relative, sneaking);
            CompoundTag again = relay(helper, relayPos).saveWithoutMetadata(helper.getLevel().registryAccess());
            again.putInt("Seconds", 3);
            again.putString("Mode", "nope");
            relay(helper, relayPos).loadWithComponents(again, helper.getLevel().registryAccess());
            helper.assertTrue(relay(helper, relayPos).seconds() == 1, "an unknown time returns to 1 second");
            helper.assertTrue(relay(helper, relayPos).mode() == Mode.ON_DELAY, "an unknown mode returns to on-delay");
            helper.assertTrue(relay(helper, relayPos).waited() == 0, "a bad save does not keep the wait");
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty", timeoutTicks = 20)
    public static void magnifyingGlassDoesNotChangeTheSetting(GameTestHelper helper) {
        BlockPos relative = new BlockPos(2, 1, 2);
        BlockPos relayPos = helper.absolutePos(relative);
        place(helper, relayPos, Direction.NORTH);
        for (int expected : new int[] {2, 5, 10, 20, 30, 60, 1}) {
            helper.useBlock(relative);
            helper.assertTrue(relay(helper, relayPos).seconds() == expected,
                    "seconds " + expected + ", saw " + relay(helper, relayPos).seconds());
        }
        var glass = GameTestSupport.magnifyingGlass();
        helper.assertTrue(!glass.isEmpty(), "gregtech magnifying glass is present");
        Player reader = helper.makeMockPlayer(GameType.SURVIVAL);
        reader.setItemInHand(InteractionHand.MAIN_HAND, glass);
        helper.useBlock(relative, reader);
        helper.assertTrue(relay(helper, relayPos).seconds() == 1, "the glass leaves the time");
        helper.assertTrue(relay(helper, relayPos).mode() == Mode.ON_DELAY, "the glass leaves the mode");
        helper.succeed();
    }

    private static DelayRelayBlockEntity place(GameTestHelper helper, BlockPos pos, Direction facing) {
        BlockState state = DelayRelayBlock.BLOCK.get().defaultBlockState().setValue(DelayRelayBlock.FACING, facing);
        helper.getLevel().setBlockAndUpdate(pos, state);
        BlockEntity entity = helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(entity instanceof GtSignalSource, "delay relay is a source");
        helper.assertTrue(!(entity instanceof GtSignalReceiver), "delay relay is not a receiver");
        DelayRelayBlockEntity relay = relay(helper, pos);
        helper.assertTrue(relay.seconds() == 1, "a new relay waits one second");
        helper.assertTrue(relay.mode() == Mode.ON_DELAY, "a new relay delays turning on");
        helper.assertTrue(!relay.getBlockState().getValue(DelayRelayBlock.POWERED), "a new relay is dark");
        return relay;
    }

    private static DelayRelayBlockEntity relay(GameTestHelper helper, BlockPos pos) {
        BlockEntity entity = helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(entity instanceof DelayRelayBlockEntity, "delay relay block entity");
        return (DelayRelayBlockEntity) entity;
    }
}
