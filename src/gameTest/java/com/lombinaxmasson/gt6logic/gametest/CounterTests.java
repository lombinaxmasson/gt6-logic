package com.lombinaxmasson.gt6logic.gametest;

import com.gregtech.gregtech.api.signal.GtSignalReceiver;
import com.gregtech.gregtech.api.signal.GtSignalSource;
import com.lombinaxmasson.gt6logic.block.CounterBlock;
import com.lombinaxmasson.gt6logic.block.CounterBlockEntity;
import com.lombinaxmasson.gt6logic.block.FaceSignals;
import com.lombinaxmasson.gt6logic.control.PulseCounter.Mode;
import com.lombinaxmasson.gt6logic.gametest.SignalProbe.Entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Pulses on the front, reset on the back. A high level held across a reload is not a new pulse. */
@GameTestHolder("gt6logic")
@PrefixGameTestTemplate(false)
public final class CounterTests {
    private CounterTests() {}

    @GameTest(template = "test_empty", timeoutTicks = 80)
    public static void countsTwoPulsesThenTheBackClears(GameTestHelper helper) {
        BlockPos relative = new BlockPos(2, 1, 2);
        BlockPos counterPos = helper.absolutePos(relative);
        CounterBlockEntity counter = place(helper, counterPos, Direction.WEST);
        for (int i = 0; i < 5; i++) {
            helper.useBlock(relative);
        }
        helper.assertTrue(counter.target() == 2, "five clicks land on 2, saw " + counter.target());
        Entity pulse = GameTestSupport.placeProbe(helper, counterPos.west());
        Entity reset = GameTestSupport.placeProbe(helper, counterPos.east());
        rise(helper, pulse, 4);
        rise(helper, pulse, 12);
        helper.runAtTickTime(10, () -> helper.assertTrue(counter(helper, counterPos).count() == 1,
                "one pulse, saw " + counter(helper, counterPos).count()));
        helper.runAtTickTime(20, () -> {
            CounterBlockEntity done = counter(helper, counterPos);
            helper.assertTrue(done.count() == 2, "two pulses, saw " + done.count());
            helper.assertTrue(done.latched(), "open mode turns on at the target");
            helper.assertTrue(GameTestSupport.toward(done, Direction.NORTH) == FaceSignals.ON, "the side carries the latch");
            helper.assertTrue(GameTestSupport.toward(done, Direction.WEST) == 0L, "the pulse face stays quiet");
            helper.assertTrue(GameTestSupport.toward(done, Direction.EAST) == 0L, "the reset face stays quiet");
            reset.setPending(Direction.EAST, FaceSignals.ON);
            reset.finishTick();
        });
        helper.runAtTickTime(28, () -> {
            CounterBlockEntity cleared = counter(helper, counterPos);
            helper.assertTrue(cleared.count() == 0, "the back face clears the count, saw " + cleared.count());
            helper.assertTrue(!cleared.latched(), "open mode returns to off");
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void shutStartsOnAndAHeldPulseDoesNotCountTwice(GameTestHelper helper) {
        BlockPos relative = new BlockPos(2, 1, 5);
        BlockPos counterPos = helper.absolutePos(relative);
        place(helper, counterPos, Direction.WEST);
        for (int i = 0; i < 5; i++) {
            helper.useBlock(relative);
        }
        Player sneaking = helper.makeMockPlayer(GameType.SURVIVAL);
        sneaking.setShiftKeyDown(true);
        helper.useBlock(relative, sneaking);
        helper.assertTrue(counter(helper, counterPos).mode() == Mode.SHUT, "sneak selects shut");
        Entity pulse = GameTestSupport.placeProbe(helper, counterPos.west());
        helper.runAtTickTime(3, () -> helper.assertTrue(counter(helper, counterPos).latched(),
                "shut mode starts on"));
        helper.runAtTickTime(4, () -> {
            pulse.setPending(Direction.WEST, FaceSignals.ON);
            pulse.finishTick();
        });
        helper.runAtTickTime(8, () -> {
            CounterBlockEntity counter = counter(helper, counterPos);
            helper.assertTrue(counter.count() == 1, "the first edge counts, saw " + counter.count());
            CompoundTag saved = counter.saveWithoutMetadata(helper.getLevel().registryAccess());
            counter.cycleTarget();
            counter.loadWithComponents(saved, helper.getLevel().registryAccess());
            helper.assertTrue(counter.count() == 1, "the count reloads");
            helper.assertTrue(counter.latched(), "the latch reloads");
        });
        helper.runAtTickTime(12, () -> helper.assertTrue(counter(helper, counterPos).count() == 1,
                "a level that stays high is not another pulse"));
        helper.runAtTickTime(14, () -> {
            pulse.setPending(Direction.WEST, 0);
            pulse.finishTick();
        });
        helper.runAtTickTime(18, () -> {
            pulse.setPending(Direction.WEST, FaceSignals.ON);
            pulse.finishTick();
        });
        helper.runAtTickTime(24, () -> {
            CounterBlockEntity counter = counter(helper, counterPos);
            helper.assertTrue(counter.count() == 2, "the next edge finishes the count, saw " + counter.count());
            helper.assertTrue(!counter.latched(), "shut mode turns off at the target");
            savedBad(helper, counter);
            helper.succeed();
        });
    }

    private static void savedBad(GameTestHelper helper, CounterBlockEntity counter) {
        CompoundTag tag = counter.saveWithoutMetadata(helper.getLevel().registryAccess());
        tag.putInt("Target", 7);
        tag.putString("Mode", "nope");
        tag.putInt("Count", 4);
        counter.loadWithComponents(tag, helper.getLevel().registryAccess());
        helper.assertTrue(counter.target() == 10, "an unknown target returns to 10");
        helper.assertTrue(counter.mode() == Mode.OPEN, "an unknown mode returns to open");
        helper.assertTrue(counter.count() == 0, "a bad save clears the count");
    }

    private static void rise(GameTestHelper helper, Entity probe, int tick) {
        helper.runAtTickTime(tick, () -> {
            probe.setPending(Direction.WEST, FaceSignals.ON);
            probe.finishTick();
        });
        helper.runAtTickTime(tick + 3, () -> {
            probe.setPending(Direction.WEST, 0);
            probe.finishTick();
        });
    }

    private static CounterBlockEntity place(GameTestHelper helper, BlockPos pos, Direction facing) {
        BlockState state = CounterBlock.BLOCK.get().defaultBlockState().setValue(CounterBlock.FACING, facing);
        helper.getLevel().setBlockAndUpdate(pos, state);
        BlockEntity entity = helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(entity instanceof GtSignalSource, "counter is a source");
        helper.assertTrue(!(entity instanceof GtSignalReceiver), "counter is not a receiver");
        CounterBlockEntity counter = counter(helper, pos);
        helper.assertTrue(counter.target() == 10, "a new counter aims at 10");
        helper.assertTrue(counter.mode() == Mode.OPEN, "a new counter turns on when full");
        helper.assertTrue(counter.count() == 0, "a new counter has not counted");
        return counter;
    }

    private static CounterBlockEntity counter(GameTestHelper helper, BlockPos pos) {
        BlockEntity entity = helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(entity instanceof CounterBlockEntity, "counter block entity");
        return (CounterBlockEntity) entity;
    }
}
