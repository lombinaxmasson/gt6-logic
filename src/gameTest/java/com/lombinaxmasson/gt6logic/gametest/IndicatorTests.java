package com.lombinaxmasson.gt6logic.gametest;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import com.gregtech.gregtech.api.signal.GtSignalCarrier;
import com.gregtech.gregtech.api.signal.GtSignalReceiver;
import com.gregtech.gregtech.api.signal.GtSignalSource;
import com.lombinaxmasson.gt6logic.Gt6Logic;
import com.lombinaxmasson.gt6logic.block.IndicatorBlock;
import com.lombinaxmasson.gt6logic.block.IndicatorBlockEntity;
import com.lombinaxmasson.gt6logic.gametest.SignalProbe.Entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The first placeable block: it lights, and it does not feed a signal wire. */
@GameTestHolder("gt6logic")
@PrefixGameTestTemplate(false)
public final class IndicatorTests {
    private IndicatorTests() {}

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void redstoneBlockLightsAndRemovalClears(GameTestHelper helper) {
        BlockPos lampPos = helper.absolutePos(new BlockPos(2, 1, 2));
        BlockPos redstonePos = lampPos.west();
        placeLamp(helper, lampPos);
        helper.getLevel().setBlockAndUpdate(redstonePos, Blocks.REDSTONE_BLOCK.defaultBlockState());
        helper.runAtTickTime(2, () -> {
            assertLit(helper, lampPos, true, "a redstone block lights the lamp");
            helper.getLevel().setBlockAndUpdate(redstonePos, Blocks.AIR.defaultBlockState());
        });
        helper.runAtTickTime(5, () -> {
            assertLit(helper, lampPos, false, "removing the redstone block turns the lamp off");
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void halfLevelLightsAndZeroClears(GameTestHelper helper) {
        BlockPos lampPos = helper.absolutePos(new BlockPos(2, 1, 5));
        BlockPos probePos = lampPos.east();
        placeLamp(helper, lampPos);
        Entity probe = placeProbe(helper, probePos);
        // Face index is the direction from the lamp toward the probe.
        probe.setPending(Direction.EAST, GtSignalCarrier.UNIT / 2);
        probe.finishTick();
        helper.runAtTickTime(2, () -> assertLit(helper, lampPos, true,
                "UNIT / 2 ceilings to one level and lights the lamp"));
        helper.runAtTickTime(3, () -> {
            probe.setPending(Direction.EAST, 0);
            probe.finishTick();
        });
        helper.runAtTickTime(6, () -> {
            assertLit(helper, lampPos, false, "publishing 0 turns the lamp off");
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void litLampDoesNotFeedTheWire(GameTestHelper helper) {
        BlockPos stonePos = helper.absolutePos(new BlockPos(2, 1, 8));
        BlockPos torchPos = stonePos.above();
        BlockPos lampPos = torchPos.above();
        BlockPos wirePos = lampPos.east();
        helper.getLevel().setBlockAndUpdate(stonePos, Blocks.STONE.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(torchPos, Blocks.REDSTONE_TORCH.defaultBlockState());
        placeLamp(helper, lampPos);
        BlockState wire = SignalProbe.connect(SignalProbe.redAlloyWire().defaultBlockState(), Direction.WEST);
        helper.getLevel().setBlockAndUpdate(wirePos, wire);
        helper.runAtTickTime(4, () -> {
            assertLit(helper, lampPos, true, "the lamp is lit while the wire is sampled");
            BlockState state = helper.getLevel().getBlockState(lampPos);
            helper.assertTrue(state.getSignal(helper.getLevel(), lampPos, Direction.UP) == 0,
                    "the lamp does not override getSignal");
            int conducted = helper.getLevel().getSignal(lampPos, Direction.UP);
            helper.assertTrue(conducted == 15,
                    "the lamp conducts the torch under it, saw " + conducted);
            BlockEntity wireEntity = helper.getLevel().getBlockEntity(wirePos);
            helper.assertTrue(wireEntity instanceof GtSignalCarrier, "red alloy wire");
            long signal = ((GtSignalCarrier) wireEntity).signal();
            helper.assertTrue(signal == 0L, "the wire skips the lit lamp, saw " + signal);
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void creativeTabOffersNamedIndicator(GameTestHelper helper) {
        CreativeModeTab tab = Gt6Logic.CREATIVE_TAB.get();
        tab.buildContents(new CreativeModeTab.ItemDisplayParameters(
                FeatureFlags.VANILLA_SET, true, helper.getLevel().registryAccess()));
        helper.assertTrue(tab.getIconItem().is(IndicatorBlock.ITEM.get()), "creative tab icon is the indicator");
        boolean listed = false;
        for (ItemStack stack : tab.getDisplayItems()) {
            if (stack.is(IndicatorBlock.ITEM.get())) {
                listed = true;
            }
        }
        helper.assertTrue(listed, "creative tab contains the indicator");
        helper.assertTrue("Indicator".equals(translation("en_us.json")), "english name");
        helper.assertTrue("指示灯".equals(translation("zh_cn.json")), "chinese name");
        helper.succeed();
    }

    private static void placeLamp(GameTestHelper helper, BlockPos pos) {
        helper.getLevel().setBlockAndUpdate(pos, IndicatorBlock.BLOCK.get().defaultBlockState());
        BlockEntity entity = helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(entity instanceof IndicatorBlockEntity, "indicator block entity");
        helper.assertTrue(entity instanceof GtSignalReceiver, "indicator is a receiver");
        helper.assertTrue(!(entity instanceof GtSignalSource), "indicator is not a source");
        helper.assertTrue(!entity.getBlockState().getValue(IndicatorBlock.LIT), "a new lamp is off");
    }

    private static Entity placeProbe(GameTestHelper helper, BlockPos pos) {
        helper.getLevel().setBlockAndUpdate(pos, SignalProbe.BLOCK.defaultBlockState());
        BlockEntity entity = helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(entity instanceof Entity, "probe block entity");
        return (Entity) entity;
    }

    private static void assertLit(GameTestHelper helper, BlockPos pos, boolean lit, String message) {
        BlockState state = helper.getLevel().getBlockState(pos);
        helper.assertTrue(state.getValue(IndicatorBlock.LIT) == lit, message + ", lit=" + state.getValue(IndicatorBlock.LIT));
        int light = state.getLightEmission();
        helper.assertTrue(light == (lit ? 15 : 0), message + ", light=" + light);
    }

    private static String translation(String file) {
        String path = "/assets/gt6logic/lang/" + file;
        try (InputStreamReader reader = new InputStreamReader(
                IndicatorTests.class.getResourceAsStream(path), StandardCharsets.UTF_8)) {
            return GsonHelper.getAsString(GsonHelper.parse(reader), "block.gt6logic.indicator");
        } catch (IOException | NullPointerException e) {
            throw new IllegalStateException("missing " + path, e);
        }
    }
}
