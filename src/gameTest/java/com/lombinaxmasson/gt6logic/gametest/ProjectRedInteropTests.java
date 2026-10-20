package com.lombinaxmasson.gt6logic.gametest;

import static com.lombinaxmasson.gt6logic.gametest.GameTestSupport.wire;

import com.gregtech.gregtech.api.signal.GtSignalCarrier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Whether GT signal wire and ProjectRed parts pass signals to each other with no bridge block.
 * ProjectRed parts are placed with their items, the way a player places them. Skipped without ProjectRed.
 */
@GameTestHolder("gt6logic")
@PrefixGameTestTemplate(false)
public final class ProjectRedInteropTests {
    private static final long FULL = 15 * GtSignalCarrier.UNIT;

    private ProjectRedInteropTests() {}

    /** Redstone block, GT wire, NOT gate input. The lamp in front of the gate goes off. */
    @GameTest(template = "test_empty", timeoutTicks = 100, required = false)
    public static void gtWireDrivesProjectRedGate(GameTestHelper helper) {
        if (skip(helper)) {
            return;
        }
        BlockPos gate = helper.absolutePos(new BlockPos(3, 1, 2));
        BlockPos wirePos = gate.west();
        BlockPos sourcePos = wirePos.west();
        BlockPos lamp = gate.east();
        floor(helper, gate);
        helper.getLevel().setBlockAndUpdate(lamp, Blocks.REDSTONE_LAMP.defaultBlockState());
        placePart(helper, "projectred_integration:not_gate", gate.below(), Direction.UP, Direction.EAST);
        GameTestSupport.placeWire(helper, wirePos, Direction.WEST, Direction.EAST);
        helper.runAtTickTime(10, () -> {
            helper.assertTrue(lit(helper, lamp), "with no input the NOT gate lights the lamp in front");
            helper.getLevel().setBlockAndUpdate(sourcePos, Blocks.REDSTONE_BLOCK.defaultBlockState());
        });
        helper.runAtTickTime(25, () -> {
            helper.assertTrue(wire(helper, wirePos) > 0, "the GT wire picks up the redstone block");
            helper.assertTrue(!lit(helper, lamp), "the GT wire switches the NOT gate, wire=" + wire(helper, wirePos));
            helper.succeed();
        });
    }

    /** NOT gate with no input outputs on. A GT wire in front of it reads full signal. */
    @GameTest(template = "test_empty", timeoutTicks = 100, required = false)
    public static void projectRedGateDrivesGtWire(GameTestHelper helper) {
        if (skip(helper)) {
            return;
        }
        BlockPos gate = helper.absolutePos(new BlockPos(3, 1, 5));
        BlockPos wirePos = gate.east();
        floor(helper, gate);
        placePart(helper, "projectred_integration:not_gate", gate.below(), Direction.UP, Direction.EAST);
        GameTestSupport.placeWire(helper, wirePos, Direction.WEST);
        helper.runAtTickTime(15, () -> {
            long value = wire(helper, wirePos);
            helper.assertTrue(value == FULL - GameTestSupport.RED_ALLOY_LOSS,
                    "the GT wire reads the full gate output less one hop, saw " + value);
            helper.succeed();
        });
    }

    /** Redstone block, ProjectRed red alloy wire, GT wire. */
    @GameTest(template = "test_empty", timeoutTicks = 100, required = false)
    public static void projectRedWireFeedsGtWire(GameTestHelper helper) {
        if (skip(helper)) {
            return;
        }
        BlockPos sourcePos = helper.absolutePos(new BlockPos(1, 1, 8));
        BlockPos prWire = sourcePos.east();
        BlockPos gtWire = prWire.east();
        floor(helper, prWire);
        helper.getLevel().setBlockAndUpdate(sourcePos, Blocks.REDSTONE_BLOCK.defaultBlockState());
        placePart(helper, "projectred_transmission:red_alloy_wire", prWire.below(), Direction.UP, Direction.EAST);
        GameTestSupport.placeWire(helper, gtWire, Direction.WEST);
        helper.runAtTickTime(15, () -> {
            long value = wire(helper, gtWire);
            helper.assertTrue(value > 0, "the GT wire reads the ProjectRed wire, saw " + value);
            helper.succeed();
        });
    }

    /** Redstone block, GT wire, ProjectRed red alloy wire, NOT gate input. */
    @GameTest(template = "test_empty", timeoutTicks = 100, required = false)
    public static void gtWireFeedsProjectRedWire(GameTestHelper helper) {
        if (skip(helper)) {
            return;
        }
        BlockPos sourcePos = helper.absolutePos(new BlockPos(1, 1, 11));
        BlockPos gtWire = sourcePos.east();
        BlockPos prWire = gtWire.east();
        BlockPos gate = prWire.east();
        BlockPos lamp = gate.east();
        floor(helper, prWire);
        floor(helper, gate);
        helper.getLevel().setBlockAndUpdate(lamp, Blocks.REDSTONE_LAMP.defaultBlockState());
        placePart(helper, "projectred_transmission:red_alloy_wire", prWire.below(), Direction.UP, Direction.EAST);
        placePart(helper, "projectred_integration:not_gate", gate.below(), Direction.UP, Direction.EAST);
        GameTestSupport.placeWire(helper, gtWire, Direction.WEST, Direction.EAST);
        helper.runAtTickTime(10, () -> {
            helper.assertTrue(lit(helper, lamp), "with no input the NOT gate lights the lamp in front");
            helper.getLevel().setBlockAndUpdate(sourcePos, Blocks.REDSTONE_BLOCK.defaultBlockState());
        });
        helper.runAtTickTime(25, () -> {
            helper.assertTrue(wire(helper, gtWire) > 0, "the GT wire picks up the redstone block");
            helper.assertTrue(!lit(helper, lamp), "the ProjectRed wire carries the GT wire into the gate");
            helper.succeed();
        });
    }

    /** A player looking east puts the NOT gate's input on the west and its front on the east. */
    @GameTest(template = "test_empty", timeoutTicks = 100, required = false)
    public static void notGateFacesAwayFromPlayer(GameTestHelper helper) {
        if (skip(helper)) {
            return;
        }
        BlockPos gate = helper.absolutePos(new BlockPos(3, 1, 14));
        floor(helper, gate);
        placePart(helper, "projectred_integration:not_gate", gate.below(), Direction.UP, Direction.EAST);
        helper.runAtTickTime(10, () -> {
            StringBuilder emits = new StringBuilder();
            for (Direction d : Direction.Plane.HORIZONTAL) {
                emits.append(' ').append(d).append('=').append(emitted(helper, gate, d));
            }
            helper.assertTrue(emitted(helper, gate, Direction.WEST) == 0 && emitted(helper, gate, Direction.EAST) == 15,
                    "input west, front east; the gate emits" + emits);
            helper.succeed();
        });
    }

    /** What a neighbor on {@code side} of {@code pos} reads from it. */
    private static int emitted(GameTestHelper helper, BlockPos pos, Direction side) {
        return helper.getLevel().getSignal(pos, side.getOpposite());
    }

    /** The gate goes down first, then a player puts GT wire on the floor in front of it. */
    @GameTest(template = "test_empty", timeoutTicks = 100, required = false)
    public static void placedGtWireJoinsGateInFront(GameTestHelper helper) {
        if (skip(helper)) {
            return;
        }
        BlockPos gate = helper.absolutePos(new BlockPos(3, 1, 17));
        BlockPos wirePos = gate.east();
        floor(helper, gate);
        floor(helper, wirePos);
        placePart(helper, "projectred_integration:not_gate", gate.below(), Direction.UP, Direction.EAST);
        placePart(helper, "gregtech:wire_01_redalloy", wirePos.below(), Direction.UP, Direction.EAST);
        helper.runAtTickTime(15, () -> {
            helper.assertTrue(wire(helper, wirePos) > 0,
                    "the placed wire reads the gate, state " + helper.getLevel().getBlockState(wirePos));
            helper.succeed();
        });
    }

    /**
     * The GT wire goes down first, then the gate beside it. GT wire only joins its neighbors when it is placed,
     * so this side stays open and the gate hears nothing until the player connects it with a GT tool.
     * If this starts failing, GT6CE now joins later neighbors and the design doc should drop the caveat.
     */
    @GameTest(template = "test_empty", timeoutTicks = 100, required = false)
    public static void gtWireDoesNotJoinALaterGate(GameTestHelper helper) {
        if (skip(helper)) {
            return;
        }
        BlockPos sourcePos = helper.absolutePos(new BlockPos(1, 1, 20));
        BlockPos wirePos = sourcePos.east();
        BlockPos gate = wirePos.east();
        BlockPos lamp = gate.east();
        floor(helper, wirePos);
        floor(helper, gate);
        helper.getLevel().setBlockAndUpdate(sourcePos, Blocks.REDSTONE_BLOCK.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(lamp, Blocks.REDSTONE_LAMP.defaultBlockState());
        placePart(helper, "gregtech:wire_01_redalloy", wirePos.below(), Direction.UP, Direction.EAST);
        placePart(helper, "projectred_integration:not_gate", gate.below(), Direction.UP, Direction.EAST);
        helper.runAtTickTime(15, () -> {
            helper.assertTrue(wire(helper, wirePos) > 0,
                    "the placed wire reads the redstone block, state " + helper.getLevel().getBlockState(wirePos));
            BlockState state = helper.getLevel().getBlockState(wirePos);
            helper.assertTrue(!SignalProbe.connected(state, Direction.EAST), "the wire left its gate side open, " + state);
            helper.assertTrue(lit(helper, lamp), "so the NOT gate still sees no input");
            helper.succeed();
        });
    }

    static boolean present() {
        return ModList.get().isLoaded("projectred_integration") && ModList.get().isLoaded("projectred_transmission");
    }

    private static boolean skip(GameTestHelper helper) {
        if (present()) {
            return false;
        }
        helper.succeed();
        return true;
    }

    private static void floor(GameTestHelper helper, BlockPos above) {
        helper.getLevel().setBlockAndUpdate(above.below(), Blocks.STONE.defaultBlockState());
    }

    /** Uses the item on {@code face} of {@code target}, by a player looking toward {@code facing}. */
    static void placePart(GameTestHelper helper, String id, BlockPos target, Direction face, Direction facing) {
        ResourceLocation key = ResourceLocation.parse(id);
        helper.assertTrue(BuiltInRegistries.ITEM.containsKey(key), "missing item " + id);
        Item item = BuiltInRegistries.ITEM.get(key);
        ItemStack stack = new ItemStack(item);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos stand = target.relative(face).relative(facing.getOpposite(), 2);
        player.moveTo(Vec3.atBottomCenterOf(stand), facing.toYRot(), 45);
        player.setYHeadRot(facing.toYRot());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        Vec3 hit = Vec3.atCenterOf(target).add(Vec3.atLowerCornerOf(face.getNormal()).scale(0.5));
        UseOnContext context = new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, stack,
                new BlockHitResult(hit, face, target, false));
        InteractionResult result = stack.useOn(context);
        helper.assertTrue(result.consumesAction(), "placing " + id + " returned " + result);
        helper.assertTrue(!helper.getLevel().getBlockState(target.relative(face)).isAir(), id + " is in the world");
    }

    private static boolean lit(GameTestHelper helper, BlockPos lamp) {
        return helper.getLevel().getBlockState(lamp).getValue(RedstoneLampBlock.LIT);
    }
}
