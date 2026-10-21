package com.lombinaxmasson.gt6logic.gametest;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import com.gregtech.gregtech.api.addon.platform.AddonTools;
import com.gregtech.gregtech.api.signal.GtSignalCarrier;
import com.gregtech.gregtech.api.signal.GtSignalSource;
import com.lombinaxmasson.gt6logic.block.AnalogPlate;
import com.lombinaxmasson.gt6logic.gametest.SignalProbe.Entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Placement helpers shared by the second-tier block tests. */
public final class GameTestSupport {
    public static final long RED_ALLOY_LOSS = GtSignalCarrier.UNIT / 16;

    private GameTestSupport() {}

    public static Entity placeProbe(GameTestHelper helper, BlockPos pos) {
        helper.getLevel().setBlockAndUpdate(pos, SignalProbe.BLOCK.defaultBlockState());
        BlockEntity entity = helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(entity instanceof Entity, "probe block entity");
        return (Entity) entity;
    }

    public static void placeWire(GameTestHelper helper, BlockPos pos, Direction... toward) {
        BlockState state = SignalProbe.redAlloyWire().defaultBlockState();
        for (Direction side : toward) {
            state = SignalProbe.connect(state, side);
        }
        helper.getLevel().setBlockAndUpdate(pos, state);
        helper.assertTrue(helper.getLevel().getBlockEntity(pos) instanceof GtSignalCarrier, "red alloy wire");
    }

    public static long wire(GameTestHelper helper, BlockPos pos) {
        BlockEntity entity = helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(entity instanceof GtSignalCarrier, "wire still present");
        return ((GtSignalCarrier) entity).signal();
    }

    /** Value the neighbor standing in {@code towardNeighbor} reads from this source. */
    public static long toward(GtSignalSource source, Direction towardNeighbor) {
        return source.gtSignal(towardNeighbor.getOpposite().ordinal());
    }

    public static ItemStack magnifyingGlass() {
        for (Item item : BuiltInRegistries.ITEM) {
            ItemStack stack = new ItemStack(item);
            if (AddonTools.isMagnifyingGlass(stack)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * Uses {@code stack} on {@code face} of {@code target}. {@code look} is where the player looks, {@code pitch} is degrees.
     * The new block is {@code target.relative(face)}.
     */
    public static BlockPos placeItem(
            GameTestHelper helper, ItemStack stack, BlockPos target, Direction face, Direction look, float pitch) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos stand = target.relative(face).relative(look.getOpposite(), 2);
        player.moveTo(Vec3.atBottomCenterOf(stand), look.toYRot(), pitch);
        player.setYHeadRot(look.toYRot());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        Vec3 hit = Vec3.atCenterOf(target).add(Vec3.atLowerCornerOf(face.getNormal()).scale(0.5));
        UseOnContext context = new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, stack,
                new BlockHitResult(hit, face, target, false));
        InteractionResult result = stack.useOn(context);
        BlockPos placed = target.relative(face);
        helper.assertTrue(result.consumesAction(), "placing " + stack.getItem() + " returned " + result);
        helper.assertTrue(!helper.getLevel().getBlockState(placed).isAir(), stack.getItem() + " is in the world");
        return placed;
    }

    /** Places a block item. Pitch stays 0 so an aimed block's facing stays on the horizontal. */
    public static BlockPos placeBlock(GameTestHelper helper, Item item, BlockPos target, Direction face, Direction look) {
        return placeItem(helper, new ItemStack(item), target, face, look, 0F);
    }

    /**
     * Floor plate on the stone under {@code pos}. The player looks {@code front}, and the plate's front follows.
     */
    public static BlockPos placeGate(GameTestHelper helper, Item item, BlockPos pos, Direction front) {
        BlockPos support = pos.below();
        helper.getLevel().setBlockAndUpdate(support, Blocks.STONE.defaultBlockState());
        BlockPos placed = placeBlock(helper, item, support, Direction.UP, front);
        helper.assertTrue(placed.equals(pos), item + " landed at " + placed + " instead of " + pos);
        BlockState state = helper.getLevel().getBlockState(pos);
        helper.assertTrue(state.getValue(AnalogPlate.SUPPORT) == Direction.DOWN, "the plate sits on the floor");
        helper.assertTrue(AnalogPlate.ports(state).front() == front,
                "front follows the look, saw " + AnalogPlate.ports(state).front());
        return placed;
    }

    /** Full block on the stone under {@code pos}. The player looks opposite {@code facing}, so the block points {@code facing}. */
    public static BlockPos placeAimed(GameTestHelper helper, Item item, BlockPos pos, Direction facing) {
        BlockPos support = pos.below();
        helper.getLevel().setBlockAndUpdate(support, Blocks.STONE.defaultBlockState());
        BlockPos placed = placeBlock(helper, item, support, Direction.UP, facing.getOpposite());
        helper.assertTrue(placed.equals(pos), item + " landed at " + placed + " instead of " + pos);
        return placed;
    }

    public static String translation(String file, String key) {
        String path = "/assets/gt6logic/lang/" + file;
        try (InputStreamReader reader = new InputStreamReader(
                GameTestSupport.class.getResourceAsStream(path), StandardCharsets.UTF_8)) {
            return GsonHelper.getAsString(GsonHelper.parse(reader), key);
        } catch (IOException | NullPointerException e) {
            throw new IllegalStateException("missing " + path + " key " + key, e);
        }
    }
}
