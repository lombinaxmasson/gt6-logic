package com.lombinaxmasson.gt6logic.gametest;

import com.gregtech.gregtech.api.signal.GtSignalCarrier;
import com.lombinaxmasson.gt6logic.block.BundledTapBlock;
import com.lombinaxmasson.gt6logic.block.BundledTapBlockEntity;

import mrtjp.projectred.api.ProjectRedAPI;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** One bundled colour. Skipped when ProjectRed is not in the test server. */
@GameTestHolder("gt6logic")
@PrefixGameTestTemplate(false)
public final class BundledTapTests {
    private BundledTapTests() {}

    @GameTest(template = "test_empty", timeoutTicks = 80, required = false)
    public static void halfLevelBecomesRedThenBlack(GameTestHelper helper) {
        if (skip(helper)) {
            return;
        }
        BlockPos relative = new BlockPos(3, 1, 2);
        BlockPos tapPos = helper.absolutePos(relative);
        BlockPos wirePos = tapPos.west();
        BlockPos cablePos = tapPos.east();
        long half = (15L * GtSignalCarrier.UNIT) / 2;
        var probe = GameTestSupport.placeProbe(helper, wirePos.west());
        probe.setPending(Direction.WEST, half + GameTestSupport.RED_ALLOY_LOSS);
        probe.finishTick();
        GameTestSupport.placeWire(helper, wirePos, Direction.WEST);
        BlockPos placed = GameTestSupport.placeBlock(
                helper, BundledTapBlock.ITEM.get(), wirePos, Direction.EAST, Direction.EAST);
        helper.assertTrue(placed.equals(tapPos), "tap sits on the wire's east face");
        helper.assertTrue(helper.getLevel().getBlockState(tapPos).getValue(BundledTapBlock.FACING) == Direction.WEST,
                "facing points at the GT wire");
        sneak(helper, relative, DyeColor.RED.getId());
        helper.useBlock(relative);
        BundledTapBlockEntity tap = tap(helper, tapPos);
        helper.assertTrue(tap.color() == DyeColor.RED.getId(), "sneak reaches red, saw " + tap.color());
        helper.assertTrue(tap.toCable(), "a click sends GT toward the cable");
        placeCable(helper, cablePos);
        helper.runAtTickTime(25, () -> {
            long wire = GameTestSupport.wire(helper, wirePos);
            helper.assertTrue(wire == half, "the GT wire is 7.5, saw " + wire);
            byte[] bundled = cable(helper, tapPos, Direction.EAST);
            int red = channel(bundled, DyeColor.RED);
            helper.assertTrue(red == 127 || red == 128, "red is 127 or 128, saw " + unsigned(bundled));
            assertOthersZero(helper, bundled, DyeColor.RED.getId());
            sneak(helper, relative, 1);
            helper.assertTrue(tap(helper, tapPos).color() == DyeColor.BLACK.getId(), "the next sneak is black");
        });
        helper.runAtTickTime(45, () -> {
            byte[] bundled = cable(helper, tapPos, Direction.EAST);
            helper.assertTrue(channel(bundled, DyeColor.RED) == 0, "red dropped, saw " + unsigned(bundled));
            int black = channel(bundled, DyeColor.BLACK);
            helper.assertTrue(black == 127 || black == 128, "black took the value, saw " + unsigned(bundled));
            helper.useBlock(relative);
            helper.assertTrue(!tap(helper, tapPos).toCable(), "the next click listens to the cable");
        });
        helper.runAtTickTime(65, () -> {
            byte[] bundled = cable(helper, tapPos, Direction.EAST);
            for (int i = 0; i < bundled.length; i++) {
                helper.assertTrue(channel(bundled, DyeColor.byId(i)) == 0, "the tap stopped driving the cable, " + unsigned(bundled));
            }
            helper.assertTrue(GameTestSupport.toward(tap(helper, tapPos), Direction.WEST) == 0L,
                    "an empty cable writes zero onto the GT side");
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty", timeoutTicks = 80, required = false)
    public static void fullRedComesBackOntoAGtWire(GameTestHelper helper) {
        if (skip(helper)) {
            return;
        }
        BlockPos probePos = helper.absolutePos(new BlockPos(1, 1, 6));
        BlockPos sourceRel = new BlockPos(2, 1, 6);
        BlockPos sourcePos = helper.absolutePos(sourceRel);
        BlockPos cablePos = sourcePos.east();
        BlockPos wirePos = helper.absolutePos(new BlockPos(5, 1, 6));
        BlockPos readerRel = new BlockPos(4, 1, 6);
        BlockPos readerPos = helper.absolutePos(readerRel);
        var probe = GameTestSupport.placeProbe(helper, probePos);
        probe.setPending(Direction.WEST, 15L * GtSignalCarrier.UNIT);
        probe.finishTick();
        BlockPos source = GameTestSupport.placeBlock(
                helper, BundledTapBlock.ITEM.get(), probePos, Direction.EAST, Direction.EAST);
        helper.assertTrue(source.equals(sourcePos), "source tap");
        sneak(helper, sourceRel, DyeColor.RED.getId());
        helper.useBlock(sourceRel);
        helper.assertTrue(tap(helper, sourcePos).toCable(), "the source writes the cable");
        placeCable(helper, cablePos);
        GameTestSupport.placeWire(helper, wirePos, Direction.WEST);
        BlockPos reader = GameTestSupport.placeBlock(
                helper, BundledTapBlock.ITEM.get(), wirePos, Direction.WEST, Direction.WEST);
        helper.assertTrue(reader.equals(readerPos), "reader tap");
        helper.assertTrue(helper.getLevel().getBlockState(readerPos).getValue(BundledTapBlock.FACING) == Direction.EAST,
                "the reader faces the GT wire");
        sneak(helper, readerRel, DyeColor.RED.getId());
        helper.assertTrue(!tap(helper, readerPos).toCable(), "the reader still listens");
        helper.assertTrue(tap(helper, readerPos).color() == DyeColor.RED.getId(), "the reader is red");
        helper.runAtTickTime(30, () -> {
            byte[] bundled = cable(helper, sourcePos, Direction.EAST);
            helper.assertTrue(channel(bundled, DyeColor.RED) == 255, "the cable red is full, saw " + unsigned(bundled));
            assertOthersZero(helper, bundled, DyeColor.RED.getId());
            long gt = GameTestSupport.wire(helper, wirePos);
            long full = 15L * GtSignalCarrier.UNIT;
            helper.assertTrue(gt == full - GameTestSupport.RED_ALLOY_LOSS,
                    "the GT wire reads full less one hop, saw " + gt);
            BundledTapBlockEntity readerTap = tap(helper, readerPos);
            helper.assertTrue(GameTestSupport.toward(readerTap, Direction.EAST) == full, "the free face is full");
            for (Direction side : Direction.values()) {
                if (side == Direction.EAST) {
                    continue;
                }
                helper.assertTrue(GameTestSupport.toward(readerTap, side) == 0L, side + " stays quiet");
            }
            helper.succeed();
        });
    }

    private static boolean skip(GameTestHelper helper) {
        if (ProjectRedInteropTests.present()) {
            return false;
        }
        helper.succeed();
        return true;
    }

    private static void sneak(GameTestHelper helper, BlockPos relative, int times) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        for (int i = 0; i < times; i++) {
            helper.useBlock(relative, player);
        }
    }

    private static void placeCable(GameTestHelper helper, BlockPos cablePos) {
        helper.getLevel().setBlockAndUpdate(cablePos.below(), Blocks.STONE.defaultBlockState());
        ProjectRedInteropTests.placePart(
                helper, "projectred_transmission:neutral_bundled_wire", cablePos.below(), Direction.UP, Direction.EAST);
    }

    private static byte[] cable(GameTestHelper helper, BlockPos tapPos, Direction towardCable) {
        byte[] bundled = ProjectRedAPI.transmissionAPI.getBundledInput(helper.getLevel(), tapPos, towardCable);
        helper.assertTrue(bundled != null && bundled.length == 16, "the cable answered");
        return bundled;
    }

    private static int channel(byte[] bundled, DyeColor color) {
        return bundled[color.getId()] & 0xFF;
    }

    private static void assertOthersZero(GameTestHelper helper, byte[] bundled, int used) {
        for (int i = 0; i < bundled.length; i++) {
            if (i == used) {
                continue;
            }
            helper.assertTrue((bundled[i] & 0xFF) == 0, "other colours stay 0, saw " + unsigned(bundled));
        }
    }

    private static String unsigned(byte[] bundled) {
        StringBuilder text = new StringBuilder();
        for (byte value : bundled) {
            text.append(value & 0xFF).append(' ');
        }
        return text.toString();
    }

    private static BundledTapBlockEntity tap(GameTestHelper helper, BlockPos pos) {
        BlockEntity entity = helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(entity instanceof BundledTapBlockEntity, "bundled tap");
        return (BundledTapBlockEntity) entity;
    }
}
