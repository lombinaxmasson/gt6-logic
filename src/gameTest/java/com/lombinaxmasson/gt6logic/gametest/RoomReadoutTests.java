package com.lombinaxmasson.gt6logic.gametest;

import com.gregtech.gregtech.api.signal.GtSignalReceiver;
import com.gregtech.gregtech.api.signal.GtSignalSource;
import com.lombinaxmasson.gt6logic.block.ChartRecorderBlock;
import com.lombinaxmasson.gt6logic.block.ChartRecorderBlockEntity;
import com.lombinaxmasson.gt6logic.block.FaceSignals;
import com.lombinaxmasson.gt6logic.block.PointerGaugeBlock;
import com.lombinaxmasson.gt6logic.block.PointerGaugeBlockEntity;
import com.lombinaxmasson.gt6logic.block.ReceiverPlate;
import com.lombinaxmasson.gt6logic.control.PointerAngle;
import com.lombinaxmasson.gt6logic.gametest.SignalProbe.Entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
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
    public static void pointerFollowsTheSignal(GameTestHelper helper) {
        BlockPos gaugePos = helper.absolutePos(new BlockPos(2, 1, 2));
        placeGauge(helper, gaugePos, Direction.EAST);
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
        placeRecorder(helper, recorderPos, Direction.EAST);
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
}
