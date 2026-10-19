package com.lombinaxmasson.gt6logic.gametest;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import com.gregtech.gregtech.api.addon.platform.AddonTools;
import com.gregtech.gregtech.api.signal.GtSignalCarrier;
import com.gregtech.gregtech.api.signal.GtSignalSource;
import com.lombinaxmasson.gt6logic.gametest.SignalProbe.Entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

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
