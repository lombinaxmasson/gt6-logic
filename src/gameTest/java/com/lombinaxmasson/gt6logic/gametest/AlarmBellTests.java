package com.lombinaxmasson.gt6logic.gametest;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import com.gregtech.gregtech.api.addon.platform.AddonTools;
import com.gregtech.gregtech.api.signal.GtSignalCarrier;
import com.gregtech.gregtech.api.signal.GtSignalReceiver;
import com.gregtech.gregtech.api.signal.GtSignalSource;
import com.lombinaxmasson.gt6logic.Gt6Logic;
import com.lombinaxmasson.gt6logic.block.AlarmBellBlock;
import com.lombinaxmasson.gt6logic.block.AlarmBellBlock.Timbre;
import com.lombinaxmasson.gt6logic.block.AlarmBellBlockEntity;
import com.lombinaxmasson.gt6logic.block.IndicatorBlock;
import com.lombinaxmasson.gt6logic.gametest.SignalProbe.Entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** A bell that rings above a level, and does not feed a signal wire. */
@GameTestHolder("gt6logic")
@PrefixGameTestTemplate(false)
public final class AlarmBellTests {
    private AlarmBellTests() {}

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void listenedFaceRingsAndTheBackDoesNot(GameTestHelper helper) {
        BlockPos bellPos = helper.absolutePos(new BlockPos(2, 1, 2));
        BlockPos front = bellPos.west();
        BlockPos back = bellPos.east();
        placeBell(helper, bellPos, Direction.WEST);
        helper.getLevel().setBlockAndUpdate(back, Blocks.REDSTONE_BLOCK.defaultBlockState());
        helper.runAtTickTime(2, () -> {
            assertRinging(helper, bellPos, false, "redstone behind the bell stays quiet");
            helper.assertTrue(bell(helper, bellPos).ringsPlayed() == 0, "the back face does not play");
            helper.getLevel().setBlockAndUpdate(back, Blocks.AIR.defaultBlockState());
            helper.getLevel().setBlockAndUpdate(front, Blocks.REDSTONE_BLOCK.defaultBlockState());
        });
        helper.runAtTickTime(5, () -> {
            assertRinging(helper, bellPos, true, "redstone on the listened face rings");
            helper.getLevel().setBlockAndUpdate(front, Blocks.AIR.defaultBlockState());
        });
        helper.runAtTickTime(8, () -> {
            assertRinging(helper, bellPos, false, "removing that redstone stops the bell");
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void halfLevelRingsAndZeroClears(GameTestHelper helper) {
        BlockPos bellPos = helper.absolutePos(new BlockPos(2, 1, 5));
        BlockPos probePos = bellPos.east();
        placeBell(helper, bellPos, Direction.EAST);
        Entity probe = placeProbe(helper, probePos);
        probe.setPending(Direction.EAST, GtSignalCarrier.UNIT / 2);
        probe.finishTick();
        helper.runAtTickTime(2, () -> assertRinging(helper, bellPos, true,
                "UNIT / 2 ceilings to one level and rings at the default threshold"));
        helper.runAtTickTime(3, () -> {
            probe.setPending(Direction.EAST, 0);
            probe.finishTick();
        });
        helper.runAtTickTime(6, () -> {
            assertRinging(helper, bellPos, false, "publishing 0 stops the bell");
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void equalLevelStaysQuiet(GameTestHelper helper) {
        BlockPos bellPos = helper.absolutePos(new BlockPos(2, 1, 8));
        BlockPos probePos = bellPos.west();
        AlarmBellBlockEntity bell = placeBell(helper, bellPos, Direction.WEST);
        bell.cycleThreshold();
        helper.assertTrue(bell.threshold() == 1, "the threshold is one level");
        Entity probe = placeProbe(helper, probePos);
        probe.setPending(Direction.WEST, GtSignalCarrier.UNIT);
        probe.finishTick();
        helper.runAtTickTime(2, () -> {
            assertRinging(helper, bellPos, false, "a ceiling equal to the threshold stays quiet");
            helper.assertTrue(bell(helper, bellPos).ringsPlayed() == 0, "an equal level plays nothing");
            probe.setPending(Direction.WEST, 2L * GtSignalCarrier.UNIT);
            probe.finishTick();
        });
        helper.runAtTickTime(5, () -> {
            assertRinging(helper, bellPos, true, "one level above the threshold rings");
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void ringingBellDoesNotFeedTheWire(GameTestHelper helper) {
        BlockPos stonePos = helper.absolutePos(new BlockPos(2, 1, 11));
        BlockPos torchPos = stonePos.above();
        BlockPos bellPos = torchPos.above();
        BlockPos wirePos = bellPos.east();
        helper.getLevel().setBlockAndUpdate(stonePos, Blocks.STONE.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(torchPos, Blocks.REDSTONE_TORCH.defaultBlockState());
        placeBell(helper, bellPos, Direction.DOWN);
        BlockState wire = SignalProbe.connect(SignalProbe.redAlloyWire().defaultBlockState(), Direction.WEST);
        helper.getLevel().setBlockAndUpdate(wirePos, wire);
        helper.runAtTickTime(4, () -> {
            assertRinging(helper, bellPos, true, "the bell is ringing while the wire is sampled");
            BlockState state = helper.getLevel().getBlockState(bellPos);
            helper.assertTrue(state.getSignal(helper.getLevel(), bellPos, Direction.UP) == 0,
                    "the bell does not override getSignal");
            int conducted = helper.getLevel().getSignal(bellPos, Direction.UP);
            helper.assertTrue(conducted == 15,
                    "the bell conducts the torch under it, saw " + conducted);
            BlockEntity wireEntity = helper.getLevel().getBlockEntity(wirePos);
            helper.assertTrue(wireEntity instanceof GtSignalCarrier, "red alloy wire");
            long signal = ((GtSignalCarrier) wireEntity).signal();
            helper.assertTrue(signal == 0L, "the wire skips the ringing bell, saw " + signal);
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void ringsOncePerSecondAndRestartsImmediately(GameTestHelper helper) {
        BlockPos bellPos = helper.absolutePos(new BlockPos(2, 1, 2));
        BlockPos front = bellPos.west();
        placeBell(helper, bellPos, Direction.WEST);
        helper.getLevel().setBlockAndUpdate(front, Blocks.REDSTONE_BLOCK.defaultBlockState());
        helper.runAtTickTime(4, () -> helper.assertTrue(bell(helper, bellPos).ringsPlayed() == 1,
                "the crossing plays once, saw " + bell(helper, bellPos).ringsPlayed()));
        helper.runAtTickTime(12, () -> helper.assertTrue(bell(helper, bellPos).ringsPlayed() == 1,
                "it does not play again inside the second, saw " + bell(helper, bellPos).ringsPlayed()));
        helper.runAtTickTime(32, () -> {
            helper.assertTrue(bell(helper, bellPos).ringsPlayed() == 2,
                    "the second ring is one second later, saw " + bell(helper, bellPos).ringsPlayed());
            helper.getLevel().setBlockAndUpdate(front, Blocks.AIR.defaultBlockState());
        });
        helper.runAtTickTime(36, () -> {
            assertRinging(helper, bellPos, false, "the bell is quiet after the redstone leaves");
            helper.assertTrue(bell(helper, bellPos).ringsPlayed() == 2, "going quiet plays nothing");
            helper.getLevel().setBlockAndUpdate(front, Blocks.REDSTONE_BLOCK.defaultBlockState());
        });
        helper.runAtTickTime(40, () -> {
            helper.assertTrue(bell(helper, bellPos).ringsPlayed() == 3,
                    "the next crossing plays immediately, saw " + bell(helper, bellPos).ringsPlayed());
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void settingsReloadAndBadValuesClamp(GameTestHelper helper) {
        BlockPos bellPos = helper.absolutePos(new BlockPos(2, 1, 2));
        AlarmBellBlockEntity bell = placeBell(helper, bellPos, Direction.NORTH);
        bell.cycleThreshold();
        bell.cycleThreshold();
        bell.cycleTimbre();
        CompoundTag saved = bell.saveWithoutMetadata(helper.getLevel().registryAccess());
        helper.assertTrue(saved.contains("Threshold"), "threshold is saved");
        helper.assertTrue(saved.contains("Timbre"), "timbre is saved");
        helper.assertTrue(!saved.contains("Played") && !saved.contains("Cooldown"), "cadence is not saved");
        bell.cycleThreshold();
        bell.cycleTimbre();
        bell.loadWithComponents(saved, helper.getLevel().registryAccess());
        helper.assertTrue(bell.threshold() == 2, "the threshold reloads, saw " + bell.threshold());
        helper.assertTrue(bell.timbre() == Timbre.CHIME, "the timbre reloads, saw " + bell.timbre());
        saved.putInt("Threshold", 40);
        saved.putString("Timbre", "nope");
        bell.loadWithComponents(saved, helper.getLevel().registryAccess());
        helper.assertTrue(bell.threshold() == 14, "a high level clamps to 14, saw " + bell.threshold());
        helper.assertTrue(bell.timbre() == Timbre.BELL, "an unknown timbre falls back to the bell");
        saved.putInt("Threshold", -2);
        bell.loadWithComponents(saved, helper.getLevel().registryAccess());
        helper.assertTrue(bell.threshold() == 0, "a negative level clamps to 0, saw " + bell.threshold());
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void rightClickCyclesLevelAndSneakCyclesTimbre(GameTestHelper helper) {
        BlockPos relative = new BlockPos(2, 1, 2);
        BlockPos bellPos = helper.absolutePos(relative);
        placeBell(helper, bellPos, Direction.NORTH);
        for (int expected = 1; expected <= 14; expected++) {
            helper.useBlock(relative);
            helper.assertTrue(bell(helper, bellPos).threshold() == expected,
                    "right click sets level " + expected + ", saw " + bell(helper, bellPos).threshold());
        }
        helper.useBlock(relative);
        helper.assertTrue(bell(helper, bellPos).threshold() == 0, "right click wraps to 0");

        Player sneaking = helper.makeMockPlayer(GameType.SURVIVAL);
        sneaking.setShiftKeyDown(true);
        helper.useBlock(relative, sneaking);
        helper.assertTrue(bell(helper, bellPos).timbre() == Timbre.CHIME, "sneak selects the chime");
        helper.useBlock(relative, sneaking);
        helper.assertTrue(bell(helper, bellPos).timbre() == Timbre.PLING, "sneak selects the pling");
        helper.useBlock(relative, sneaking);
        helper.assertTrue(bell(helper, bellPos).timbre() == Timbre.BELL, "sneak wraps back to the bell");
        helper.assertTrue(bell(helper, bellPos).threshold() == 0, "changing timbre leaves the level alone");

        ItemStack glass = magnifyingGlass();
        helper.assertTrue(!glass.isEmpty(), "gregtech magnifying glass is present");
        helper.useBlock(relative);
        helper.useBlock(relative);
        helper.useBlock(relative);
        Player reader = helper.makeMockPlayer(GameType.SURVIVAL);
        reader.setItemInHand(InteractionHand.MAIN_HAND, glass);
        helper.useBlock(relative, reader);
        helper.assertTrue(bell(helper, bellPos).threshold() == 3,
                "the magnifying glass leaves the level, saw " + bell(helper, bellPos).threshold());
        helper.assertTrue(bell(helper, bellPos).timbre() == Timbre.BELL, "the magnifying glass leaves the timbre");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void creativeTabOffersNamedAlarmBell(GameTestHelper helper) {
        CreativeModeTab tab = Gt6Logic.CREATIVE_TAB.get();
        tab.buildContents(new CreativeModeTab.ItemDisplayParameters(
                FeatureFlags.VANILLA_SET, true, helper.getLevel().registryAccess()));
        helper.assertTrue(tab.getIconItem().is(IndicatorBlock.ITEM.get()), "creative tab icon stays the indicator");
        boolean listed = false;
        for (ItemStack stack : tab.getDisplayItems()) {
            if (stack.is(AlarmBellBlock.ITEM.get())) {
                listed = true;
            }
        }
        helper.assertTrue(listed, "creative tab contains the alarm bell");
        helper.assertTrue("Alarm Bell".equals(translation("en_us.json")), "english name");
        helper.assertTrue("报警铃".equals(translation("zh_cn.json")), "chinese name");
        helper.succeed();
    }

    private static AlarmBellBlockEntity placeBell(GameTestHelper helper, BlockPos pos, Direction facing) {
        BlockState state = AlarmBellBlock.BLOCK.get().defaultBlockState().setValue(AlarmBellBlock.FACING, facing);
        helper.getLevel().setBlockAndUpdate(pos, state);
        BlockEntity entity = helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(entity instanceof GtSignalReceiver, "alarm bell is a receiver");
        helper.assertTrue(!(entity instanceof GtSignalSource), "alarm bell is not a source");
        AlarmBellBlockEntity bell = bell(helper, pos);
        helper.assertTrue(!bell.getBlockState().getValue(AlarmBellBlock.RINGING), "a new bell is quiet");
        helper.assertTrue(bell.threshold() == 0, "a new bell rings above 0");
        helper.assertTrue(bell.timbre() == Timbre.BELL, "a new bell uses the bell timbre");
        return bell;
    }

    private static AlarmBellBlockEntity bell(GameTestHelper helper, BlockPos pos) {
        BlockEntity entity = helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(entity instanceof AlarmBellBlockEntity, "alarm bell block entity");
        return (AlarmBellBlockEntity) entity;
    }

    private static Entity placeProbe(GameTestHelper helper, BlockPos pos) {
        helper.getLevel().setBlockAndUpdate(pos, SignalProbe.BLOCK.defaultBlockState());
        BlockEntity entity = helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(entity instanceof Entity, "probe block entity");
        return (Entity) entity;
    }

    private static void assertRinging(GameTestHelper helper, BlockPos pos, boolean ringing, String message) {
        BlockState state = helper.getLevel().getBlockState(pos);
        helper.assertTrue(state.getValue(AlarmBellBlock.RINGING) == ringing,
                message + ", ringing=" + state.getValue(AlarmBellBlock.RINGING));
    }

    private static ItemStack magnifyingGlass() {
        for (Item item : BuiltInRegistries.ITEM) {
            ItemStack stack = new ItemStack(item);
            if (AddonTools.isMagnifyingGlass(stack)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static String translation(String file) {
        String path = "/assets/gt6logic/lang/" + file;
        try (InputStreamReader reader = new InputStreamReader(
                AlarmBellTests.class.getResourceAsStream(path), StandardCharsets.UTF_8)) {
            return GsonHelper.getAsString(GsonHelper.parse(reader), "block.gt6logic.alarm_bell");
        } catch (IOException | NullPointerException e) {
            throw new IllegalStateException("missing " + path, e);
        }
    }
}
