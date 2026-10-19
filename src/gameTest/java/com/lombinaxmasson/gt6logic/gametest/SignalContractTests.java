package com.lombinaxmasson.gt6logic.gametest;

import com.gregtech.gregtech.api.signal.GtSignalCarrier;
import com.lombinaxmasson.gt6logic.gametest.SignalProbe.Entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The published-signal contract against a real red-alloy signal wire. */
@GameTestHolder("gt6logic")
@PrefixGameTestTemplate(false)
public final class SignalContractTests {
    /** {@code gregtech:wire_01_redalloy} range is 16 at GT6CE c899462a6. */
    private static final long RED_ALLOY_LOSS = GtSignalCarrier.UNIT / 16;

    private SignalContractTests() {}

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void unpublishedChangeStaysOffTheWire(GameTestHelper helper) {
        BlockPos probePos = helper.absolutePos(new BlockPos(2, 1, 2));
        BlockPos wirePos = probePos.east();
        Entity probe = placeProbe(helper, probePos);
        placeWire(helper, wirePos, Direction.WEST);
        helper.assertTrue(!SignalProbe.isReceiver(probe), "a source probe is not a receiver");

        probe.setPending(Direction.WEST, GtSignalCarrier.UNIT);
        probe.finishTick();
        helper.runAtTickTime(2, () -> helper.assertTrue(
                signal(helper, wirePos) == GtSignalCarrier.UNIT - RED_ALLOY_LOSS,
                "a published level arrives minus one red-alloy hop, saw " + signal(helper, wirePos)));
        helper.runAtTickTime(3, () -> probe.setPending(Direction.WEST, GtSignalCarrier.UNIT / 2));
        helper.runAtTickTime(5, () -> helper.assertTrue(
                signal(helper, wirePos) == GtSignalCarrier.UNIT - RED_ALLOY_LOSS,
                "a pending change is still invisible, saw " + signal(helper, wirePos)));
        helper.runAtTickTime(6, () -> probe.finishTick());
        helper.runAtTickTime(8, () -> {
            long expected = GtSignalCarrier.UNIT / 2 - RED_ALLOY_LOSS;
            helper.assertTrue(signal(helper, wirePos) == expected,
                    "the published half-level arrives minus one hop, saw " + signal(helper, wirePos));
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void aWireIsReadAsACarrier(GameTestHelper helper) {
        BlockPos probePos = helper.absolutePos(new BlockPos(2, 1, 4));
        BlockPos wirePos = probePos.east();
        BlockPos redstonePos = wirePos.east();
        helper.getLevel().setBlockAndUpdate(redstonePos, Blocks.REDSTONE_BLOCK.defaultBlockState());
        placeWire(helper, wirePos, Direction.EAST);
        Entity probe = placeProbe(helper, probePos);
        probe.echoInputs(true);
        long expected = 15L * GtSignalCarrier.UNIT - RED_ALLOY_LOSS;
        helper.runAtTickTime(4, () -> {
            probe.finishTick();
            helper.assertTrue(probe.gtSignal(Direction.EAST.ordinal()) == expected,
                    "the probe keeps the wire's fraction, saw " + probe.gtSignal(Direction.EAST.ordinal()));
            helper.assertTrue(expected != 15L * GtSignalCarrier.UNIT,
                    "the fraction is not a rounded vanilla level");
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void vanillaSignalCeilingsThePublishedValue(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 6));
        Entity probe = placeProbe(helper, pos);
        probe.setPending(Direction.NORTH, 1);
        probe.finishTick();
        helper.assertTrue(helper.getLevel().getSignal(pos, Direction.NORTH) == 1, "one unit ceilings to one level");
        probe.setPending(Direction.NORTH, GtSignalCarrier.UNIT / 2);
        probe.finishTick();
        helper.assertTrue(helper.getLevel().getSignal(pos, Direction.NORTH) == 1, "half a level ceilings to one");
        helper.assertTrue(helper.getLevel().getDirectSignal(pos, Direction.NORTH) == 1, "direct signal matches");
        probe.setPending(Direction.NORTH, 0);
        probe.finishTick();
        helper.assertTrue(helper.getLevel().getSignal(pos, Direction.NORTH) == 0, "zero stays zero");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void publishedSignalReloads(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(4, 1, 6));
        Entity probe = placeProbe(helper, pos);
        long value = GtSignalCarrier.UNIT / 2 + 3;
        probe.setPending(Direction.NORTH, value);
        probe.finishTick();
        CompoundTag saved = probe.saveWithoutMetadata(helper.getLevel().registryAccess());
        probe.setPending(Direction.NORTH, 0);
        probe.finishTick();
        helper.assertTrue(probe.gtSignal(Direction.NORTH.ordinal()) == 0, "the probe was cleared");
        probe.loadWithComponents(saved, helper.getLevel().registryAccess());
        helper.assertTrue(probe.gtSignal(Direction.NORTH.ordinal()) == value, "the published value reloads");
        helper.succeed();
    }

    private static Entity placeProbe(GameTestHelper helper, BlockPos pos) {
        helper.getLevel().setBlockAndUpdate(pos, SignalProbe.BLOCK.defaultBlockState());
        BlockEntity entity = helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(entity instanceof Entity, "probe block entity");
        return (Entity) entity;
    }

    private static void placeWire(GameTestHelper helper, BlockPos pos, Direction toward) {
        BlockState state = SignalProbe.connect(SignalProbe.redAlloyWire().defaultBlockState(), toward);
        helper.getLevel().setBlockAndUpdate(pos, state);
        helper.assertTrue(helper.getLevel().getBlockEntity(pos) instanceof GtSignalCarrier,
                "red alloy wire keeps a fractional signal");
    }

    private static long signal(GameTestHelper helper, BlockPos pos) {
        BlockEntity entity = helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(entity instanceof GtSignalCarrier, "wire still present");
        return ((GtSignalCarrier) entity).signal();
    }
}
