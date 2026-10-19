package com.lombinaxmasson.gt6logic.block;

import com.lombinaxmasson.gt6logic.control.HopCompensation;
import com.lombinaxmasson.gt6logic.signal.SignalBlockEntity;
import com.lombinaxmasson.gt6logic.signal.SignalLevels;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Restores {@code UNIT / range} on a positive input. Zero stays zero. */
public final class CompensationRepeaterBlockEntity extends SignalBlockEntity {
    private static final String RANGE = "Range";

    private int range = HopCompensation.RANGES[0];

    public CompensationRepeaterBlockEntity(BlockPos pos, BlockState state) {
        super(CompensationRepeaterBlock.TYPE.get(), pos, state);
    }

    public int range() {
        return range;
    }

    public Component status() {
        return Component.translatable("message.gt6logic.compensation_repeater.status", range);
    }

    public void cycleRange() {
        range = HopCompensation.nextRange(range);
        setChanged();
        show(getBlockState().getValue(CompensationRepeaterBlock.POWERED));
    }

    @Override
    protected void onTick() {
        BlockState state = getBlockState();
        if (!state.is(CompensationRepeaterBlock.BLOCK.get())) {
            return;
        }
        Direction facing = state.getValue(CompensationRepeaterBlock.FACING);
        long restored = HopCompensation.restore(read(facing), range);
        FaceSignals.send(output(), restored, facing);
        show(SignalLevels.ceilingLevel(restored) >= 1);
    }

    private void show(boolean on) {
        Level level = getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        BlockState state = getBlockState();
        BlockState next = state
                .setValue(CompensationRepeaterBlock.POWERED, on)
                .setValue(CompensationRepeaterBlock.SPAN, CompensationRepeaterBlock.Span.of(range));
        if (next != state) {
            level.setBlock(worldPosition, next, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt(RANGE, range);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        int loaded = tag.getInt(RANGE);
        range = HopCompensation.allowedRange(loaded) ? loaded : HopCompensation.RANGES[0];
    }
}
