package com.lombinaxmasson.gt6logic.signal;

import java.util.Arrays;

import com.gregtech.gregtech.api.signal.GtSignalSource;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A signal source with a one-tick delay. {@link #onTick} writes the pending faces,
 * and {@link #finishTick} publishes them afterwards.
 *
 * <p>{@code gtSignal}'s side is the direction from the asking wire toward this block.
 * A wire standing to the east reads {@code gtSignal(WEST)}.</p>
 */
public abstract class SignalBlockEntity extends BlockEntity implements GtSignalSource {
    private static final String PUBLISHED = "Published";

    private final PublishedSignal output = new PublishedSignal();

    protected SignalBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    protected final PublishedSignal output() {
        return output;
    }

    /** Signal on the neighbor across {@code face}. {@code face} points from this block toward that neighbor. */
    protected final long read(Direction face) {
        return SignalInputs.read(level, worldPosition.relative(face), face);
    }

    /** Writes this tick's pending outputs. Must not publish, and must not call {@code gtSignal} on a neighbor. */
    protected abstract void onTick();

    /** Publishes pending outputs and wakes vanilla neighbors when a whole redstone level changed. */
    public final void finishTick() {
        if (level == null || level.isClientSide || isRemoved()) {
            return;
        }
        int[] before = vanillaLevels();
        onTick();
        boolean changed = output.publish();
        if (changed) {
            setChanged();
        }
        if (!Arrays.equals(before, vanillaLevels())) {
            BlockState state = getBlockState();
            level.updateNeighborsAt(worldPosition, state.getBlock());
            level.updateNeighbourForOutputSignal(worldPosition, state.getBlock());
        }
    }

    @Override
    public final long gtSignal(int side) {
        if (side < 0 || side >= PublishedSignal.FACES) {
            return 0L;
        }
        return output.get(side);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLongArray(PUBLISHED, output.publishedCopy());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        output.loadPublished(tag.getLongArray(PUBLISHED));
    }

    private int[] vanillaLevels() {
        int[] levels = new int[PublishedSignal.FACES];
        for (int side = 0; side < levels.length; side++) {
            levels[side] = SignalLevels.ceilingLevel(output.get(side));
        }
        return levels;
    }
}
