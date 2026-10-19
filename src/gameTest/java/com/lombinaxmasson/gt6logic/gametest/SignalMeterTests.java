package com.lombinaxmasson.gt6logic.gametest;

import com.gregtech.gregtech.api.signal.GtSignalCarrier;
import com.gregtech.gregtech.api.signal.GtSignalReceiver;
import com.gregtech.gregtech.api.signal.GtSignalSource;
import com.lombinaxmasson.gt6logic.block.SignalMeterBlock;
import com.lombinaxmasson.gt6logic.block.SignalMeterBlockEntity;
import com.lombinaxmasson.gt6logic.gametest.SignalProbe.Entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The dial keeps the fraction. A wire beside the meter still reads zero. */
@GameTestHolder("gt6logic")
@PrefixGameTestTemplate(false)
public final class SignalMeterTests {
    private SignalMeterTests() {}

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void halfLevelStaysFractional(GameTestHelper helper) {
        BlockPos meterPos = helper.absolutePos(new BlockPos(2, 1, 2));
        BlockPos probePos = meterPos.west();
        place(helper, meterPos, Direction.WEST);
        Entity probe = GameTestSupport.placeProbe(helper, probePos);
        probe.setPending(Direction.WEST, GtSignalCarrier.UNIT / 2);
        probe.finishTick();
        helper.runAtTickTime(4, () -> {
            SignalMeterBlockEntity meter = meter(helper, meterPos);
            helper.assertTrue(meter.reading() == GtSignalCarrier.UNIT / 2,
                    "the meter keeps the half level, saw " + meter.reading());
            helper.assertTrue("0.5000".equals(meter.formatted()), "four decimals, saw " + meter.formatted());
            probe.setPending(Direction.WEST, 0);
            probe.finishTick();
        });
        helper.runAtTickTime(8, () -> {
            helper.assertTrue("0.0000".equals(meter(helper, meterPos).formatted()), "zero reads as 0.0000");
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void redAlloyHopShowsTheMissingSixteenth(GameTestHelper helper) {
        BlockPos redstonePos = helper.absolutePos(new BlockPos(1, 1, 5));
        BlockPos wirePos = redstonePos.east();
        BlockPos meterPos = wirePos.east();
        helper.getLevel().setBlockAndUpdate(redstonePos, Blocks.REDSTONE_BLOCK.defaultBlockState());
        GameTestSupport.placeWire(helper, wirePos, Direction.WEST);
        place(helper, meterPos, Direction.WEST);
        long expected = 15L * GtSignalCarrier.UNIT - GameTestSupport.RED_ALLOY_LOSS;
        helper.runAtTickTime(6, () -> {
            SignalMeterBlockEntity meter = meter(helper, meterPos);
            helper.assertTrue(meter.reading() == expected, "one red-alloy hop, saw " + meter.reading());
            helper.assertTrue("14.9375".equals(meter.formatted()), "the dial shows 14.9375, saw " + meter.formatted());
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void meterDoesNotFeedTheWire(GameTestHelper helper) {
        BlockPos stonePos = helper.absolutePos(new BlockPos(2, 1, 8));
        BlockPos torchPos = stonePos.above();
        BlockPos meterPos = torchPos.above();
        BlockPos wirePos = meterPos.east();
        helper.getLevel().setBlockAndUpdate(stonePos, Blocks.STONE.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(torchPos, Blocks.REDSTONE_TORCH.defaultBlockState());
        place(helper, meterPos, Direction.DOWN);
        BlockState wire = SignalProbe.connect(SignalProbe.redAlloyWire().defaultBlockState(), Direction.WEST);
        helper.getLevel().setBlockAndUpdate(wirePos, wire);
        helper.runAtTickTime(4, () -> {
            helper.assertTrue(helper.getLevel().getSignal(meterPos, Direction.UP) == 15,
                    "the meter conducts the torch under it");
            helper.assertTrue(meter(helper, meterPos).getBlockState().getSignal(
                    helper.getLevel(), meterPos, Direction.UP) == 0, "the meter does not override getSignal");
            helper.assertTrue(GameTestSupport.wire(helper, wirePos) == 0L, "the wire skips the meter");
            helper.succeed();
        });
    }

    private static SignalMeterBlockEntity place(GameTestHelper helper, BlockPos pos, Direction facing) {
        BlockState state = SignalMeterBlock.BLOCK.get().defaultBlockState().setValue(SignalMeterBlock.FACING, facing);
        helper.getLevel().setBlockAndUpdate(pos, state);
        BlockEntity entity = helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(entity instanceof GtSignalReceiver, "signal meter is a receiver");
        helper.assertTrue(!(entity instanceof GtSignalSource), "signal meter is not a source");
        SignalMeterBlockEntity meter = meter(helper, pos);
        helper.assertTrue("0.0000".equals(meter.formatted()), "a new meter reads zero");
        return meter;
    }

    private static SignalMeterBlockEntity meter(GameTestHelper helper, BlockPos pos) {
        BlockEntity entity = helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(entity instanceof SignalMeterBlockEntity, "signal meter block entity");
        return (SignalMeterBlockEntity) entity;
    }
}
