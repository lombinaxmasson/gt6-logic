package com.lombinaxmasson.gt6logic.block;

import com.lombinaxmasson.gt6logic.control.Interlock;
import com.lombinaxmasson.gt6logic.control.Interlock.Mode;
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

/** Enough means the ceiling is at least one level. NOT ignores the back face. */
public final class InterlockBlockEntity extends SignalBlockEntity {
    private static final String MODE = "Mode";

    private Mode mode = Mode.AND;

    public InterlockBlockEntity(BlockPos pos, BlockState state) {
        super(InterlockBlock.TYPE.get(), pos, state);
    }

    public Mode mode() {
        return mode;
    }

    public Component status() {
        String key = switch (mode) {
            case AND -> "message.gt6logic.interlock.and";
            case OR -> "message.gt6logic.interlock.or";
            case NOT -> "message.gt6logic.interlock.not";
        };
        return Component.translatable(key);
    }

    public void cycleMode() {
        mode = mode.next();
        setChanged();
        show(getBlockState().getValue(InterlockBlock.POWERED));
    }

    @Override
    protected void onTick() {
        BlockState state = getBlockState();
        if (!state.is(InterlockBlock.BLOCK.get())) {
            return;
        }
        Direction facing = state.getValue(InterlockBlock.FACING);
        boolean first = SignalLevels.ceilingLevel(read(facing)) >= 1;
        boolean second = SignalLevels.ceilingLevel(read(facing.getOpposite())) >= 1;
        boolean on = Interlock.output(mode, first, second);
        FaceSignals.send(output(), on ? FaceSignals.ON : 0L, facing, facing.getOpposite());
        show(on);
    }

    private void show(boolean on) {
        Level level = getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        BlockState state = getBlockState();
        BlockState next = state
                .setValue(InterlockBlock.POWERED, on)
                .setValue(InterlockBlock.GATE, InterlockBlock.Gate.of(mode));
        if (next != state) {
            level.setBlock(worldPosition, next, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString(MODE, mode.name());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        String name = tag.getString(MODE);
        mode = Mode.byName(name).name().equals(name) ? Mode.byName(name) : Mode.AND;
    }
}
