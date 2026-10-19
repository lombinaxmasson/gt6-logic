package com.lombinaxmasson.gt6logic.block;

import com.gregtech.gregtech.api.signal.GtSignalReceiver;
import com.lombinaxmasson.gt6logic.signal.SignalInputs;
import com.lombinaxmasson.gt6logic.signal.SignalLevels;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Reads six neighbors once per server tick and stores the result on {@link IndicatorBlock#LIT}.
 * Nothing here is synced apart from that block state.
 */
public final class IndicatorBlockEntity extends BlockEntity implements GtSignalReceiver {
    public IndicatorBlockEntity(BlockPos pos, BlockState state) {
        super(IndicatorBlock.TYPE.get(), pos, state);
    }

    public void tickServer() {
        if (level == null || level.isClientSide || isRemoved()) {
            return;
        }
        long strongest = 0L;
        for (Direction face : Direction.values()) {
            strongest = Math.max(strongest, SignalInputs.read(level, worldPosition.relative(face), face));
        }
        boolean lit = SignalLevels.ceilingLevel(strongest) >= 1;
        BlockState state = getBlockState();
        if (!state.is(IndicatorBlock.BLOCK.get()) || state.getValue(IndicatorBlock.LIT) == lit) {
            return;
        }
        level.setBlock(worldPosition, state.setValue(IndicatorBlock.LIT, lit), Block.UPDATE_CLIENTS);
    }
}
